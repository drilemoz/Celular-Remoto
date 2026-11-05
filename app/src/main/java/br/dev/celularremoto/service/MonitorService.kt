package br.dev.celularremoto.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.Manifest
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import br.dev.celularremoto.core.MonitorNotificationContent
import br.dev.celularremoto.core.BatteryReader
import br.dev.celularremoto.core.BatterySessionStore
import br.dev.celularremoto.core.RecentDevices
import br.dev.celularremoto.core.MonitorNotificationPolicy
import br.dev.celularremoto.core.LocationShareStore
import br.dev.celularremoto.core.EventJournal
import br.dev.celularremoto.core.SettingsStore
import br.dev.celularremoto.security.SecretVault
import br.dev.celularremoto.transport.AuthenticatedAccess
import br.dev.celularremoto.transport.AccessAlertWindow
import br.dev.celularremoto.transport.MonitorServer
import br.dev.celularremoto.transport.PairingSession
import br.dev.celularremoto.ui.MainActivity
import br.dev.celularremoto.ui.IncomingAccessActivity
import br.dev.celularremoto.ui.MonitorStatusActivity
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.util.Log

class MonitorService : Service() {
    companion object {
        const val CHANNEL = "celular_remoto_ativo"
        const val ACCESS_CHANNEL = "celular_remoto_acessos"
        const val ACCESS_NOTIFICATION_ID = 43
        const val ACTION_STOP = "br.dev.celularremoto.STOP"
        private const val ACTION_REFRESH_POWER = "br.dev.celularremoto.REFRESH_POWER"
        /** Só notifica serviço existente; não o inicia se estiver desligado. */
        fun requestPowerRefresh(context: Context) {
            if (!isServiceActive) return
            runCatching {
                context.startService(Intent(context, MonitorService::class.java).setAction(ACTION_REFRESH_POWER))
            }.onFailure { Log.w("CelularRemotoPower", "Não foi possível atualizar os locks", it) }
        }
        @Volatile var isRunning = false
            private set
        /** Não confundir listener TCP fora do ar com serviço Android encerrado. */
        @Volatile var isServiceActive = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, MonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }
    }

    private var server: MonitorServer? = null
    private val connectionPower by lazy { ConnectionPowerController(applicationContext) }
    private var lastServerTickElapsed = 0L
    private var stoppedExplicitly = false
    @Volatile private var serviceActive = false
    private var consecutiveFailures = 0
    private var nextRecoveryMs = 0L
    private var networkCallbackRegistered = false
    private val networkManager by lazy { getSystemService(ConnectivityManager::class.java) }
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            SettingsStore.recordConnectionNetworkEvent(this@MonitorService, "Rede disponível ao Android")
            mainHandler.post {
                if (serviceActive && SettingsStore.isMonitorEnabled(this@MonitorService)) ensureServerReady()
            }
        }
        override fun onLost(network: Network) {
            SettingsStore.recordConnectionNetworkEvent(this@MonitorService, "Android informou perda de rede")
            mainHandler.post { if (serviceActive) refreshMainNotification() }
        }
    }
    private val serverWatchdog = object : Runnable {
        override fun run() {
            if (!serviceActive || !SettingsStore.isMonitorEnabled(this@MonitorService)) return
            val nowElapsed = SystemClock.elapsedRealtime()
            ConnectionPowerPolicy.schedulingGap(lastServerTickElapsed, nowElapsed)?.let { gap ->
                SettingsStore.recordSupervisorGap(this@MonitorService, gap)
                Log.i("CelularRemotoPower", "Lacuna no supervisor: ${gap}ms; causa não confirmada")
            }
            lastServerTickElapsed = nowElapsed
            connectionPower.sync(active = true)
            ensureServerReady()
            refreshMainNotification()
            mainHandler.postDelayed(this, ConnectionPowerPolicy.WATCHDOG_INTERVAL_MS)
        }
    }
    private val healthWatchdog = object : Runnable {
        override fun run() {
            if (!serviceActive || !SettingsStore.isMonitorEnabled(this@MonitorService)) return
            runCatching { br.dev.celularremoto.core.HealthAlertMonitor.check(this@MonitorService) }
                .onFailure { Log.w("CelularRemotoHealth", "Leitura de saúde indisponível", it) }
            refreshMainNotification()
            mainHandler.postDelayed(this, 30_000L)
        }
    }
    // Labs, sessões automáticas e metas usam um único executor serial do serviço
    // existente. Nenhum segundo serviço, wake lock ou canal de rede é criado.
    private val batteryExecutor = java.util.concurrent.Executors.newSingleThreadExecutor { job ->
        Thread(job, "CelularRemoto-battery-worker").apply { isDaemon = true }
    }
    // Verificações a cada 30 s, sem bloquear a thread principal com escrita em disco.
    @Volatile private var batteryNotification: MonitorNotificationContent.Battery? = null
    private var lastBatteryNotificationReadMs = 0L // tempo monotônico; worker serial, sem nova thread

    /** Aproveita o executor de bateria existente: IO nunca roda na thread principal. */
    private fun refreshBatteryNotificationFromWorker() {
        val elapsed = SystemClock.elapsedRealtime()
        if (lastBatteryNotificationReadMs > 0L && elapsed - lastBatteryNotificationReadMs in 0..54_999L) return
        lastBatteryNotificationReadMs = elapsed
        val now = System.currentTimeMillis()
        val raw = BatteryReader.snapshot(this)
        val active = BatterySessionStore.report(this).optJSONObject("active")
        val charging = raw.optString("state") == "Carregando"
        val discharging = raw.optString("state") == "Descarregando"
        val mode = active?.optString("kind")
        val matchingMode = (charging && mode == "CHARGE") || (discharging && mode == "DISCHARGE")
        fun number(field: String): Double? = if (raw.isNull(field)) null else
            raw.optDouble(field).takeIf { it.isFinite() }
        batteryNotification = MonitorNotificationContent.Battery(
            percent = raw.optInt("levelPercent", -1).takeIf { it in 0..100 },
            state = raw.optString("state", "Desconhecido"),
            temperatureC = number("temperatureC"),
            currentMa = number("currentMa"),
            forecastMinutes = if (matchingMode && active?.isNull("forecastMinutes") == false)
                active.optLong("forecastMinutes", -1L).takeIf { it in 1..10_080 } else null,
            ratePercentHour = if (matchingMode && active?.isNull("forecastRatePercentHour") == false)
                active.optDouble("forecastRatePercentHour").takeIf { it.isFinite() } else null,
            observedAtMs = raw.optLong("updatedMs", now)
        )
        mainHandler.post { refreshMainNotification() }
    }

    private val batteryLabPulse = object : Runnable {
        override fun run() {
            if (!serviceActive || !SettingsStore.isMonitorEnabled(this@MonitorService)) return
            batteryExecutor.execute {
                runCatching {
                    br.dev.celularremoto.core.BatteryLabStore.tick(this@MonitorService)
                    br.dev.celularremoto.core.BatterySessionStore.tick(this@MonitorService)
                    br.dev.celularremoto.core.BatteryGoalAlerts.check(this@MonitorService)
                }.onFailure { Log.w("CelularBattery", "Coleta de bateria indisponível", it) }
                // Uma falha em Labs/alertas não deve impedir a atualização da notificação.
                runCatching { refreshBatteryNotificationFromWorker() }
                    .onFailure { Log.w("CelularBattery", "Resumo para notificação indisponível", it) }
            }
            mainHandler.postDelayed(this, 30_000L)
        }
    }
    private val accessAlerts = AccessAlertWindow()
    private var foregroundStarted = false
    private var lastNotificationSignature: String? = null
    private var lastControllerSeenMs = 0L
    private val mainNotificationId = 42
    private val mainHandler = Handler(Looper.getMainLooper())
    private val accessListener: () -> Unit = {
        mainHandler.post {
            refreshAccessAlert()
            refreshMainNotification()
        }
    }
    private val alertCleanup = object : Runnable {
        override fun run() {
            refreshAccessAlert()
            refreshMainNotification()
            if (serviceActive) mainHandler.postDelayed(this, 10_000L)
        }
    }
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(
            CHANNEL, "Monitoramento ativo", NotificationManager.IMPORTANCE_LOW
        ))
        manager.createNotificationChannel(NotificationChannel(
            ACCESS_CHANNEL, "Acesso remoto detectado", NotificationManager.IMPORTANCE_DEFAULT
        ))
        if (br.dev.celularremoto.core.FixedDownloadsFolder.supported()) {
            kotlin.concurrent.thread(name = "CelularRemoto-prepare-downloads") {
                runCatching {
                    // Revoga apenas o grant legado SAF, não apaga arquivos do usuário.
                    if (SettingsStore.sharedDataTree(applicationContext).isNotBlank())
                        br.dev.celularremoto.core.SharedDataFolder.revoke(applicationContext)
                    br.dev.celularremoto.core.FixedDownloadsFolder.ensure(applicationContext)
                }
                    .onFailure { Log.w("CelularRemotoFiles", "Pasta padrão indisponível", it) }
            }
        }
        br.dev.celularremoto.core.RemotePhysicalControls.start(applicationContext)
        br.dev.celularremoto.core.DeviceStateReader.start(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_REFRESH_POWER && serviceActive && SettingsStore.isMonitorEnabled(this)) {
            connectionPower.sync(active = true)
            return START_STICKY
        }
        if (intent?.action == ACTION_STOP || !SettingsStore.isMonitorEnabled(this)) {
            stoppedExplicitly = true
            serviceActive = false
            isServiceActive = false
            connectionPower.release()
            SettingsStore.setMonitorEnabled(this, false)
            stopSelf()
            return START_NOT_STICKY
        }
        val notification = buildMainNotification(MonitorNotificationPolicy.State(
            serviceActive = true, serverReady = false, recentAccessCount = 0,
            locationActive = false, screenActive = false, cameraActive = false,
            alertsEnabled = SettingsStore.localHealthAlerts(this) && notificationsAllowed()
        ), "")
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(mainNotificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else startForeground(mainNotificationId, notification)
        foregroundStarted = true
        lastNotificationSignature = null
        serviceActive = true
        isServiceActive = true
        stoppedExplicitly = false
        lastServerTickElapsed = SystemClock.elapsedRealtime()
        connectionPower.sync(active = true)
        RecoveryScheduler.refresh(this)
        SettingsStore.recordRecoveryEvent(this, "Serviço iniciado ou restaurado")
        getSystemService(NotificationManager::class.java)?.cancel(98)
        registerConnectivityWatch()
        // Serviço ativo não significa porta disponível; o supervisor valida ambos.
        ensureServerReady()
        // Persistent recovery location is allowed only with owner opt-in + Android background grant.
        LocationRecoveryStarter.tryStart(this)
        refreshMainNotification()
        mainHandler.removeCallbacks(serverWatchdog)
        mainHandler.postDelayed(serverWatchdog, 15_000L)
        mainHandler.removeCallbacks(healthWatchdog)
        mainHandler.post(healthWatchdog)
        mainHandler.removeCallbacks(batteryLabPulse)
        mainHandler.post(batteryLabPulse)
        AuthenticatedAccess.unsubscribe(accessListener)
        AuthenticatedAccess.subscribe(accessListener)
        mainHandler.removeCallbacks(alertCleanup)
        mainHandler.postDelayed(alertCleanup, 10_000L)
        return START_STICKY
    }

    private fun notificationsAllowed(): Boolean = Build.VERSION.SDK_INT < 33 ||
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun buildMainNotification(state: MonitorNotificationPolicy.State, accessLine: String,
        battery: MonitorNotificationContent.Battery? = null, remoteLine: String = ""): Notification {
        val open = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_MONITOR, true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val details = PendingIntent.getActivity(this, 76,
            Intent(this, MonitorStatusActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1,
            Intent(this, MonitorService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val summary = MonitorNotificationPolicy.headline(state)
        val modules = MonitorNotificationPolicy.modules(state)
        val localLine = MonitorNotificationContent.batteryLine(battery)
        val expanded = listOf(localLine, remoteLine, "Monitorado: $summary", accessLine,
            "Recursos: $modules").filter { it.isNotBlank() }.joinToString("\n")
        val privateNotification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(br.dev.celularremoto.R.drawable.ic_stat_shield)
            .setContentTitle(MonitorNotificationContent.title(battery))
            .setContentText("${battery?.state ?: "Monitorando"} · $summary")
            .setStyle(Notification.BigTextStyle().bigText(expanded))
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .addAction(Notification.Action.Builder(null, "Parar", stop).build())
            .addAction(Notification.Action.Builder(null, "Detalhes", details).build())
        // Tela de bloqueio recebe texto genérico, sem nome/IP do controlador ou módulos.
        val publicNotification = Notification.Builder(this, CHANNEL)
            .setSmallIcon(br.dev.celularremoto.R.drawable.ic_stat_shield)
            .setContentTitle("Celular Remoto ativo")
            .setContentText("Serviço de monitoramento")
            .build()
        return privateNotification.setPublicVersion(publicNotification).build()
    }

    private fun refreshMainNotification() {
        if (!serviceActive || !foregroundStarted || !SettingsStore.isMonitorEnabled(this)) return
        val peers = AuthenticatedAccess.recent()
        val recent = peers.firstOrNull()
        if (recent != null && recent.lastSeenMs >= lastControllerSeenMs) {
            lastControllerSeenMs = recent.lastSeenMs
        }
        val ready = isRunning && server?.isHealthy() == true
        val state = MonitorNotificationPolicy.State(
            serviceActive = true, serverReady = ready, recentAccessCount = peers.size,
            locationActive = LocationShareService.running && LocationShareStore.active(),
            screenActive = br.dev.celularremoto.capture.ScreenFrameStore.sharing,
            cameraActive = br.dev.celularremoto.capture.CameraFrameStore.isActive(),
            alertsEnabled = SettingsStore.localHealthAlerts(this) && notificationsAllowed()
        )
        val accessLine = if (recent != null) {
            "Nome informado: ${recent.name.ifBlank { "Não informado" }.take(48)} · Última consulta: " +
                android.text.format.DateFormat.format("HH:mm", recent.lastSeenMs)
        } else if (lastControllerSeenMs > 0L) {
            "Último acesso: " + android.text.format.DateFormat.format("HH:mm", lastControllerSeenMs)
        } else ""
        val now = System.currentTimeMillis()
        val battery = MonitorNotificationContent.valid(batteryNotification, now)
        val selected = SettingsStore.getRemoteHost(this)
        val peer = RecentDevices.list(this).firstOrNull { it.host == selected }
        val remoteLine = MonitorNotificationContent.remoteLine(peer?.name, peer?.lastSuccessMs ?: 0L, now)
        val key = listOf(state, accessLine, battery, remoteLine).joinToString("|")
        if (key == lastNotificationSignature) return
        lastNotificationSignature = key
        getSystemService(NotificationManager::class.java)
            ?.notify(mainNotificationId, buildMainNotification(state, accessLine, battery, remoteLine))
    }

    /** Reinicia somente o listener TCP; não tenta abrir Activities nem revogar consentimento. */
    private fun ensureServerReady() {
        if (!serviceActive || !SettingsStore.isMonitorEnabled(this)) return
        if (server?.isHealthy() == true) {
            isRunning = true
            consecutiveFailures = 0
            nextRecoveryMs = 0L
            SettingsStore.recordServerHealth(this, "ativo", 0)
            return
        }
        isRunning = false
        val now = SystemClock.elapsedRealtime()
        if (!RecoveryPolicy.canRetry(now, nextRecoveryMs)) return
        server?.stop()
        server = null
        try {
            // Acesso local ao Keystore: falha não deve apagar a autorização do usuário.
            SecretVault.getOrCreateMonitorKey(this)
            val candidate = MonitorServer(applicationContext)
            candidate.start()
            server = candidate
            // isHealthy() será confirmado no próximo ciclo se a thread ainda estiver iniciando.
            isRunning = true
            consecutiveFailures = 0
            nextRecoveryMs = 0L
            SettingsStore.recordServerHealth(this, "recuperando", 0)
        } catch (error: Exception) {
            consecutiveFailures = (consecutiveFailures + 1).coerceAtMost(20)
            val delayMs = RecoveryPolicy.nextDelayMs(consecutiveFailures)
            nextRecoveryMs = now + delayMs
            SettingsStore.recordServerHealth(this, "aguardando", consecutiveFailures)
            Log.w("CelularRemotoService", "Servidor indisponível; nova tentativa em ${delayMs}ms", error)
        }
    }

    private fun registerConnectivityWatch() {
        if (networkCallbackRegistered) return
        try {
            networkManager.registerNetworkCallback(NetworkRequest.Builder().build(), networkCallback)
            networkCallbackRegistered = true
        } catch (error: Exception) {
            Log.w("CelularRemotoService", "Sem callback de rede; supervisor periódico continua", error)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Remover a tarefa visual não equivale ao usuário tocar em Parar.
        // START_STICKY mantém a possibilidade de recriação pelo sistema.
        SettingsStore.recordRecoveryEvent(this, "Janela do app removida; serviço deve continuar")
        super.onTaskRemoved(rootIntent)
    }

    private fun refreshAccessAlert() {
        val current = AuthenticatedAccess.recent()
        val fresh = accessAlerts.observe(current.map { "${it.key}:${it.sinceMs}" })
        if (current.isEmpty()) {
            (getSystemService(NotificationManager::class.java))?.cancel(ACCESS_NOTIFICATION_ID)
            return
        }
        if (!fresh) return // alerta apenas quando aparece uma nova sessão de acesso
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val launch = PendingIntent.getActivity(this, 44,
            Intent(this, IncomingAccessActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            ), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val note = Notification.Builder(this, ACCESS_CHANNEL)
            .setSmallIcon(br.dev.celularremoto.R.drawable.ic_stat_shield)
            .setContentTitle("Acesso remoto autorizado")
            .setContentText("Outro aparelho consultou os dados. Toque para ver detalhes.")
            .setContentIntent(launch)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .build()
        getSystemService(NotificationManager::class.java)?.notify(ACCESS_NOTIFICATION_ID, note)
    }

    override fun onDestroy() {
        // Localização nunca permanece ativa após perda do serviço principal.
        LocationShareService.stop(this)
        PhotoReadyService.stop(this)
        br.dev.celularremoto.core.DeviceStateReader.stop()
        foregroundStarted = false
        lastNotificationSignature = null
        batteryNotification = null
        lastBatteryNotificationReadMs = 0L
        lastControllerSeenMs = 0L
        serviceActive = false
        isServiceActive = false
        connectionPower.release()
        lastServerTickElapsed = 0L
        if (!stoppedExplicitly && SettingsStore.isMonitorEnabled(this)) {
            SettingsStore.recordRecoveryEvent(this, "Serviço Android encerrado; aguardando retomada")
            RecoveryScheduler.refresh(this)
        }
        br.dev.celularremoto.capture.ScreenShareService.stop(this)
        br.dev.celularremoto.capture.CameraFrameStore.stop()
        br.dev.celularremoto.core.RemotePhysicalControls.shutdown()
        mainHandler.removeCallbacks(serverWatchdog)
        mainHandler.removeCallbacks(healthWatchdog)
        mainHandler.removeCallbacks(batteryLabPulse)
        batteryExecutor.shutdownNow()
        if (networkCallbackRegistered) {
            runCatching { networkManager.unregisterNetworkCallback(networkCallback) }
            networkCallbackRegistered = false
        }
        mainHandler.removeCallbacks(alertCleanup)
        AuthenticatedAccess.unsubscribe(accessListener)
        getSystemService(NotificationManager::class.java)?.cancel(ACCESS_NOTIFICATION_ID)
        accessAlerts.clear()
        // O processo pode continuar vivo após a interrupção do serviço.
        EventJournal.clear()
        PairingSession.revoke()
        AuthenticatedAccess.clear()
        server?.stop()
        server = null
        isRunning = false
        SettingsStore.recordServerHealth(this, "parado", 0)
        super.onDestroy()
    }
}
