package br.dev.celularremoto.ui
import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.os.SystemClock
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import br.dev.celularremoto.R
import br.dev.celularremoto.capture.NotificationCaptureService
import br.dev.celularremoto.core.RecentDevices
import br.dev.celularremoto.core.SettingsStore
import br.dev.celularremoto.security.SecretVault
import br.dev.celularremoto.service.MonitorService
import br.dev.celularremoto.transport.AppVersion
import br.dev.celularremoto.transport.AuthenticatedAccess
import java.net.Inet4Address
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
/** Interface Android Views sem dependências adicionais; ambos os modos no mesmo APK. */
class MainActivity : Activity() {
    internal var activeUiDialog: AlertDialog? = null
    internal fun darkTheme(): Boolean = when (SettingsStore.theme(this)) {
        "dark" -> true
        "system" -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        else -> false
    }
    internal val bg get() = if (darkTheme()) Color.rgb(16, 18, 22) else Color.rgb(252, 252, 252)
    internal val surface get() = if (darkTheme()) Color.rgb(29, 31, 35) else Color.WHITE
    internal val ink get() = if (darkTheme()) Color.rgb(241, 242, 244) else Color.rgb(23, 23, 23)
    internal val dim get() = if (darkTheme()) Color.rgb(175, 179, 185) else Color.rgb(103, 103, 103)
    internal val highlight get() = if (darkTheme()) Color.rgb(90, 214, 188) else Color.rgb(24, 166, 146)
    internal val highlightText get() = if (darkTheme()) Color.rgb(14, 22, 35) else Color.WHITE
    internal val lineColor get() = if (darkTheme()) Color.rgb(58, 60, 63) else Color.rgb(232, 232, 232)
    internal val secondaryAccent get() = if (darkTheme()) Color.rgb(54, 116, 171) else Color.rgb(38, 113, 174)
    internal val fieldColor get() = if (darkTheme()) Color.rgb(39, 41, 45) else Color.rgb(242, 242, 242)
    internal lateinit var content: LinearLayout
    internal lateinit var bottomNavigation: LinearLayout
    internal lateinit var permissionsNoticeHost: LinearLayout
    internal var hidePermissionNoticeForSession = false
    internal val permissionCenterQueue = mutableListOf<List<String>>()
    internal val handler = Handler(Looper.getMainLooper())
    internal val remoteConnection by lazy { RemoteConnectionManager(applicationContext) }
    internal val pairingController = PairingController()
    internal val lastMessageViewedMs = mutableMapOf<String, Long>() // Apenas carimbo temporal em RAM
    // Dados temporários: não persistir mensagens em Bundle, preferências ou disco.
    internal var messagesData: org.json.JSONObject? = null
    internal var messagesHost = ""
    internal var messagesTab = 0 // 0: SMS; 1: notificações
    internal var messagesBaselineMs = 0L
    internal var messagesLatestShownMs = 0L
    internal var messagesLoading = false
    internal var messagesError: String? = null
    internal var messagesTechnicalExpanded = false
    internal var messagesRequestGeneration = 0
    internal var expandedRemoteFunctions = false
    internal var mode = 2 // 2: início, 0: monitorado, 1: controle, 3: ajustes, 4: mensagens, 5: bateria
    internal var batteryRemote = false
    internal var batteryData: org.json.JSONObject? = null
    internal var batteryDataHost = ""
    internal var batteryLoading = false
    internal var batteryError: String? = null
    internal var batteryRequestGeneration = 0
    internal var batterySection = "resumo"
    internal var settingsSection = "home"
    private var settingsReturnMode = 2
    internal val cameraPermissionRequest = 4317
    internal val qrScanRequest = 4318
    internal val screenCaptureRequest = 4319
    private val notificationPermissionRequest = 101
    internal val phoneNumberPermissionRequest = 103
    internal val smsReadPermissionRequest = 104
    internal val smsSendPermissionRequest = 105
    internal val smsReceivePermissionRequest = 106
    internal val smsSimPermissionRequest = 107
    internal val audioPermissionRequest = 108
    internal var active = false
    // Tempo monotônico em RAM da última tentativa de retomar o serviço autorizado.
    // Não sobreviver à Activity evita aplicar um timestamp antigo após recriação.
    private var lastRecoveryAttemptMs = 0L
    // Mantém o conteúdo e a posição de rolagem intactos quando nenhum estado mudou.
    internal var displayedMonitorState: MonitorUiRefreshPolicy.State? = null
    private var firstResume = true
    private val batteryPoller = object : Runnable {
        override fun run() {
            if (active && mode == 5) {
                if (batteryRemote) queryBatteryReport() else render()
                handler.postDelayed(this, 30_000L)
            }
        }
    }
    private val monitorUiInvalidation = Runnable {
        if (active && !isFinishing && !isDestroyed && mode == 0) refreshMonitorIfChanged()
    }
    internal var resultBox: LinearLayout? = null
    companion object { const val EXTRA_OPEN_MONITOR = "br.dev.celularremoto.OPEN_MONITOR" }
    // Alpha.53: acesso autenticado permanece visível no cartão Monitorado e na
    // notificação do sistema; nunca abre Activity por cima das tarefas do usuário.
    private val incomingListener: () -> Unit = monitorAccessListener(monitorUiInvalidation)
    private val monitorStatusPoll = monitorStatusPoller { recoverAuthorizedServiceIfStopped() }
    internal var currentStatus: TextView? = null
    internal var currentDeviceTitle: TextView? = null
    internal var currentUpdated: TextView? = null
    internal var currentConnectButton: Button? = null
    private var systemBackCallback: android.window.OnBackInvokedCallback? = null
    private data class OptionItem(val title: String, val subtitle: String = "", val action: () -> Unit)
    private val poller = object : Runnable {
        override fun run() {
            if (active && mode == 1) {
                refreshRemote(silent = true)
                handler.postDelayed(this, 15_000L)
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(if (darkTheme()) android.R.style.Theme_Material_NoActionBar else android.R.style.Theme_Material_Light_NoActionBar)
        super.onCreate(savedInstanceState)
        RecentDevices.migrateLegacy(this)
        br.dev.celularremoto.service.RemoteHealthScheduler.refresh(this)
        val firstPermissionWelcome = savedInstanceState == null && PermissionCenter.needsWelcome(this)
        mode = if (intent.getBooleanExtra(EXTRA_OPEN_MONITOR, false)) 0
            else savedInstanceState?.getInt("active_mode", 2)?.takeIf { it in 0..5 } ?: 2
        settingsSection = savedInstanceState?.getString("settings_section", "home") ?: "home"
        permissionCenterQueue.addAll(savedInstanceState?.getStringArrayList("pending_permission_groups")
            ?.map { it.split('|') } ?: emptyList())
        if (firstPermissionWelcome) {
            mode = 3
            settingsSection = "permissions"
        }
        batteryRemote = savedInstanceState?.getBoolean("battery_remote", false) ?: false
        batterySection = savedInstanceState?.getString("battery_section", "resumo") ?: "resumo"
        messagesTab = savedInstanceState?.getInt("messages_tab", 0)?.coerceIn(0, 1) ?: 0
        expandedRemoteFunctions = savedInstanceState?.getBoolean("expanded_remote_functions", false) ?: false
        if (mode == 4) {
            messagesHost = SettingsStore.getRemoteHost(this)
            messagesBaselineMs = lastMessageViewedMs[messagesHost] ?: 0L
        }
        settingsReturnMode = savedInstanceState?.getInt("settings_return_mode", 2)?.takeIf { it in setOf(0, 1, 2, 5) } ?: 2
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = if (darkTheme()) 0 else
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }
        val scroll = ScrollView(this).apply {
            setBackgroundColor(bg)
            isFillViewport = false
            clipToPadding = false
        }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(25))
        }
        scroll.addView(content)
        permissionsNoticeHost = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(permissionsNoticeHost, LinearLayout.LayoutParams(-1, -2))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        bottomNavigation = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = shape(bg, 0)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            elevation = dp(5).toFloat()
        }
        root.addView(bottomNavigation, LinearLayout.LayoutParams(-1, dp(82)))
        setContentView(root)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val callback = android.window.OnBackInvokedCallback {
                if (mode == 3) navigateBackFromSettings()
                else if (mode == 4) closeRemoteMessagesScreen()
                else if (mode == 2) finish() else navigateHome()
            }
            systemBackCallback = callback
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback
            )
        }
        render()
        if (firstPermissionWelcome) PermissionCenter.markWelcome(this)
        if (savedInstanceState == null && !firstPermissionWelcome) handler.post {
            if (!isFinishing && !isDestroyed) showVersionNewsIfNeeded()
        }
    }
    override fun onDestroy() {
        activeUiDialog?.dismiss()
        activeUiDialog = null
        handler.removeCallbacks(monitorUiInvalidation)
        handler.removeCallbacks(monitorStatusPoll)
        handler.removeCallbacks(poller)
        handler.removeCallbacks(batteryPoller)
        batteryRequestGeneration++
        if (mode == 4) clearRemoteMessagesScreen()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            systemBackCallback?.let { onBackInvokedDispatcher.unregisterOnBackInvokedCallback(it) }
            systemBackCallback = null
        }
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("active_mode", mode)
        outState.putBoolean("battery_remote", batteryRemote)
        outState.putString("battery_section", batterySection)
        outState.putInt("messages_tab", messagesTab)
        outState.putBoolean("expanded_remote_functions", expandedRemoteFunctions)
        outState.putString("settings_section", settingsSection)
        outState.putStringArrayList("pending_permission_groups", ArrayList(permissionCenterQueue.map { it.joinToString("|") }))
        outState.putInt("settings_return_mode", settingsReturnMode)
        super.onSaveInstanceState(outState)
    }
    override fun onResume() {
        br.dev.celularremoto.core.VisibleMonitorUi.changed(true)
        super.onResume()
        active = true
        AuthenticatedAccess.subscribe(incomingListener)
        // A primeira renderização já ocorreu no onCreate; evitar montar tudo duas vezes.
        if (firstResume) firstResume = false else render()
        refreshPermissionsNotice()
        recoverAuthorizedServiceIfStopped()
        if (mode == 4) loadRemoteMessages()
        handler.removeCallbacks(monitorStatusPoll)
        if (mode == 0) handler.postDelayed(monitorStatusPoll, 5_000L)
        handler.removeCallbacks(poller)
        handler.postDelayed(poller, 15_000L)
        handler.removeCallbacks(batteryPoller)
        if (mode == 5) handler.postDelayed(batteryPoller, 30_000L)
    }
    /** Recuperação conservadora somente com o app visível; nunca após parada explícita. */
    private fun recoverAuthorizedServiceIfStopped() {
        if (!SettingsStore.isMonitorEnabled(this) || MonitorService.isServiceActive) return
        val now = SystemClock.elapsedRealtime()
        if (lastRecoveryAttemptMs != 0L && now - lastRecoveryAttemptMs < 60_000L) return
        lastRecoveryAttemptMs = now
        handler.postDelayed({
            if (active && !isFinishing && !isDestroyed &&
                SettingsStore.isMonitorEnabled(this) && !MonitorService.isServiceActive) {
                runCatching { MonitorService.start(this) }.onFailure {
                    SettingsStore.recordRecoveryEvent(this, "Retomada negada pelo sistema")
                }
                handler.postDelayed({ if (active && mode == 0) render() }, 1_000L)
            }
        }, 800L)
    }
    override fun onPause() {
        br.dev.celularremoto.core.VisibleMonitorUi.changed(false)
        active = false
        if (mode == 4) {
            messagesRequestGeneration++
            messagesLoading = false
        }
        AuthenticatedAccess.unsubscribe(incomingListener)
        handler.removeCallbacks(monitorStatusPoll)
        handler.removeCallbacks(poller)
        handler.removeCallbacks(batteryPoller)
        batteryRequestGeneration++
        handler.removeCallbacks(monitorUiInvalidation)
        // Uma resposta anterior não significa que ainda haja conexão ao voltar ao app.
        if ((mode == 1 || mode == 4) && remoteConnection.state == "connected") {
            remoteConnection.pause()
            updateConnectionLabels()
        }
        super.onPause()
    }

    internal fun render() {
        content.removeAllViews()
        if (mode != 0) displayedMonitorState = null
        if (mode == 4) { renderBottomNavigation(); refreshPermissionsNotice(); renderRemoteMessagesPage(); return }
        handler.removeCallbacks(monitorStatusPoll)
        if (active && mode == 0) handler.postDelayed(monitorStatusPoll, 5_000L)
        renderBottomNavigation()
        refreshPermissionsNotice()
        renderMobileHeader()
        when (mode) {
            0 -> renderMonitor()
            5 -> renderBatteryScreen()
            3 -> renderSettingsModule()
            1 -> {
                renderController()
                content.post { if (active && mode == 1) refreshRemote(silent = true) }
            }
            else -> renderHome()
        }
        if (mode == 0 || mode == 3) {
            gap(17)
            addText("Somente em aparelhos próprios ou autorizados. Dados compartilhados exigem permissões locais.", 11f, muted = true)
        }
        if (mode == 0) displayedMonitorState = currentMonitorState()
    }

    /** Navegação principal: não toca em chaves, sockets ou permissões do Android. */
    internal fun navigateMainTab(destination: Int) {
        if (destination !in listOf(0, 1, 2, 3, 5)) return
        if (mode == destination && (destination != 3 || settingsSection == "home")) return
        if (mode == 4) clearRemoteMessagesScreen()
        if (destination == 3) {
            openSettingsModule()
            return
        }
        batteryRequestGeneration++
        handler.removeCallbacks(batteryPoller)
        if (destination == 5 && mode != 5) {
            batteryData = null
            batteryError = null
            batteryLoading = false
        }
        mode = destination
        settingsSection = "home"
        render()
        if (destination == 5 && active) handler.postDelayed(batteryPoller, 30_000L)
        (content.parent as? ScrollView)?.scrollTo(0, 0)
    }

    /** Home retorna à escolha inicial, preservando serviço e chaves de pareamento. */
    private fun navigateHome() {
        if (mode != 2) { if (mode == 4) clearRemoteMessagesScreen(); mode = 2; settingsSection = "home"; render() }
    }

    internal fun openSettingsModule() {
        if (mode != 3) settingsReturnMode = if (mode == 4) 1 else mode
        mode = 3
        settingsSection = "home"
        render()
        (content.parent as? ScrollView)?.scrollTo(0, 0)
    }

    internal fun openSettingsSection(section: String) {
        settingsSection = section
        render()
        (content.parent as? ScrollView)?.scrollTo(0, 0)
    }

    private fun navigateBackFromSettings() {
        if (settingsSection != "home") openSettingsSection("home")
        else { mode = settingsReturnMode; render() }
    }

    @Deprecated("Android Activity back callback")
    override fun onBackPressed() {
        if (mode == 3) navigateBackFromSettings()
        else if (mode == 4) closeRemoteMessagesScreen()
        else if (mode != 2) navigateHome() else super.onBackPressed()
    }

    /** Resposta centralizada: cada permissão mantém o fluxo específico e independente. */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        when (requestCode) {
            PermissionCenter.REQUEST_CODE -> {
                // A concessão NÃO altera as opções locais de compartilhar SMS/áudio/posição.
                handler.post { requestNextPermissionCenterGroup() }
            }
            cameraPermissionRequest -> {
                if (granted) launchQrCapture()
                else message("Câmera não autorizada. Permita o acesso para ler o QR ou use IP/chave manual.")
            }
            notificationPermissionRequest -> {
                if (granted) startServiceExplicitly()
                else message("Autorize notificações para exibir o monitoramento ativo.")
            }
            9268 -> {
                message(if (granted) "Notificações de saúde autorizadas." else "Permissão de notificações negada; consulte alertas no painel.")
            }
            9275, 9276 -> onLocationPermissionsResult(requestCode, grantResults)
            smsReadPermissionRequest -> {
                SettingsStore.setShareSmsInbox(this, granted)
                message(if (granted) "Leitura de SMS ativada no monitorado. Mensagens de autenticação serão filtradas."
                    else "READ_SMS não autorizada. Alguns instaladores/versões do Android restringem essa permissão.")
            }
            smsSendPermissionRequest -> {
                SettingsStore.setAllowSmsReplies(this, granted)
                message(if (granted) "Respostas SMS habilitadas. Cada envio deverá ser solicitado no controlador."
                    else "SEND_SMS não autorizada no aparelho monitorado.")
            }
            smsReceivePermissionRequest -> {
                SettingsStore.setReceiveSmsEvents(this, granted)
                message(if (granted) "Recepção SMS habilitada no monitorado." else "RECEIVE_SMS não autorizada pelo Android.")
            }
            audioPermissionRequest -> {
                SettingsStore.setCaptureInternalAudio(this, granted)
                message(if (granted) "Áudio interno habilitado. Inicie uma nova sessão de compartilhamento de tela."
                    else "RECORD_AUDIO não autorizado. O áudio interno continuará desligado.")
            }
            smsSimPermissionRequest -> {
                message(if (granted) "Seleção de chip disponível para SMS." else "READ_PHONE_STATE negada. Será usado o chip padrão.")
            }
            phoneNumberPermissionRequest -> {
                SettingsStore.setSharePhoneNumber(this, granted)
                message(if (granted) "Número da linha autorizado para consultas remotas autenticadas. Pode desativar em Permissões."
                    else "Número da linha não será compartilhado.")
            }
        }
    }
    @Deprecated("Compatibilidade com a Activity Android nativa")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 4321) return handleRecoveryPickerResult(resultCode, data)
        if (requestCode == screenCaptureRequest) {
            if (resultCode == Activity.RESULT_OK && data != null &&
                SettingsStore.isMonitorEnabled(this) && MonitorService.isRunning) {
                try {
                    val serviceIntent = Intent(this, br.dev.celularremoto.capture.ScreenShareService::class.java)
                        .setAction(br.dev.celularremoto.capture.ScreenShareService.ACTION_START)
                        .putExtra(br.dev.celularremoto.capture.ScreenShareService.EXTRA_RESULT_CODE, resultCode)
                        .putExtra(br.dev.celularremoto.capture.ScreenShareService.EXTRA_RESULT_DATA, data)
                    if (Build.VERSION.SDK_INT >= 26) startForegroundService(serviceIntent)
                    else startService(serviceIntent)
                    message("Autorização recebida. Compartilhamento indicado na barra de notificações.")
                } catch (error: Exception) { message("Não foi possível iniciar a captura: ${error.message}") }
            } else message("Compartilhamento de tela não autorizado.")
            return
        }
        if (requestCode == qrScanRequest && resultCode == Activity.RESULT_OK) {
            val scanned = data?.getStringExtra(QrScanActivity.EXTRA_QR_RESULT) ?: return
            confirmQrCode(scanned)
        }
    }

    internal fun requestStart() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), notificationPermissionRequest)
        } else startServiceExplicitly()
    }

    internal fun startServiceExplicitly() {
        try {
            SecretVault.getOrCreateMonitorKey(this)
            SettingsStore.setMonitorEnabled(this, true)
            MonitorService.start(this)
            render()
        } catch (error: Exception) {
            SettingsStore.setMonitorEnabled(this, false)
            message("Não foi possível iniciar o serviço: ${error.message}")
        }
    }


    internal fun hasListener(): Boolean = try {
        val setting = Settings.Secure.getString(contentResolver, "enabled_notification_listeners").orEmpty()
        setting.split(':').any {
            ComponentName.unflattenFromString(it) == ComponentName(this, NotificationCaptureService::class.java)
        }
    } catch (_: Exception) { false }

    internal fun localIps(): String = try {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { iface -> iface.inetAddresses.toList().filterIsInstance<Inet4Address>()
                .map { "${iface.name}: ${it.hostAddress}" } }
            .joinToString(" · ")
    } catch (_: Exception) { "" }

    internal fun formatDate(timestamp: Long): String = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

    internal fun message(message: String) {
        if (mode == 3) render()
        showAppNotice("Celular Remoto", message)
    }
}
