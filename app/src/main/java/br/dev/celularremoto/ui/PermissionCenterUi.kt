package br.dev.celularremoto.ui

import android.Manifest
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import br.dev.celularremoto.R
import br.dev.celularremoto.capture.RemoteTouchService
import br.dev.celularremoto.core.LocationShareStore
import br.dev.celularremoto.core.MonitorLockAdminReceiver

/** Permissões reais do manifesto; nenhum acesso sensível é habilitado por obter o grant do Android. */
internal data class PermissionAccess(
    val id: String,
    val label: String,
    val reason: String,
    val icon: Int,
    val kind: String, // runtime, sms, special, session
    val perms: List<String> = emptyList(),
    val guidance: String = "",
)

/** Permissões normal e runtime não incluem acesso a arquivos: SAF/MediaStore não exige READ_MEDIA_*. */
internal object PermissionCenter {
    private const val PREFS = "permission_welcome_v1"
    private const val WELCOME_SHOWN = "welcome_shown"
    const val REQUEST_CODE = 9691

    fun needsWelcome(context: Context): Boolean =
        !context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(WELCOME_SHOWN, false)

    fun markWelcome(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(WELCOME_SHOWN, true).apply()
    }

    fun entries(): List<PermissionAccess> = listOf(
        PermissionAccess("camera", "Câmera e fotografias", "QR, foto e câmera local com indicação visível.",
            R.drawable.ic_remote_camera, "runtime", listOf(Manifest.permission.CAMERA)),
        PermissionAccess("location", "Localização (durante o uso)", "GPS, Wi-Fi e localização remota autorizada.",
            R.drawable.ic_pin, "runtime", listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)),
        PermissionAccess("alerts", "Publicar notificações", "Alertas de bateria, testes e monitoramento no dispositivo.",
            R.drawable.ic_bell, "runtime", listOf(Manifest.permission.POST_NOTIFICATIONS)),
        PermissionAccess("audio", "Áudio", "Captura de áudio interno apenas em sessão de tela consentida.",
            R.drawable.ic_monitor, "runtime", listOf(Manifest.permission.RECORD_AUDIO)),
        PermissionAccess("phone_state", "Informações dos chips", "Identificar o chip usado por funções de SMS autorizadas.",
            R.drawable.ic_phone, "runtime", listOf(Manifest.permission.READ_PHONE_STATE)),
        PermissionAccess("phone_number", "Número da linha", "Consultar número da linha se o Android e a operadora disponibilizarem.",
            R.drawable.ic_phone, "runtime", listOf(Manifest.permission.READ_PHONE_NUMBERS)),
        PermissionAccess("sms_read", "Leitura de SMS", "Mensagens recentes só são compartilhadas após uma segunda autorização local.",
            R.drawable.ic_message, "sms", listOf(Manifest.permission.READ_SMS)),
        PermissionAccess("sms_receive", "Receber SMS", "Eventos de novos SMS só são compartilhados se habilitados localmente.",
            R.drawable.ic_message, "sms", listOf(Manifest.permission.RECEIVE_SMS)),
        PermissionAccess("sms_send", "Enviar SMS", "O controlador solicita cada resposta; o aparelho local deve autorizar o recurso.",
            R.drawable.ic_message, "sms", listOf(Manifest.permission.SEND_SMS)),
        PermissionAccess("listener", "Ler notificações recebidas", "Acesso especial: veja alertas de outros apps somente com consentimento local.",
            R.drawable.ic_bell, "special", guidance = "Ative Celular Remoto em Acesso às notificações do Android."),
        PermissionAccess("accessibility", "Controle por toque", "Acessibilidade: controle remoto exige ativação manual e opção local.",
            R.drawable.ic_shield, "special", guidance = "Abra Acessibilidade e ative Celular Remoto somente se desejar controle por toque."),
        PermissionAccess("usage", "Estatísticas de uso", "Acesso especial para diagnóstico de aplicativos.",
            R.drawable.ic_activity, "special", guidance = "Ative Acesso ao uso para Celular Remoto nas configurações Android."),
        PermissionAccess("background_location", "Localização em segundo plano", "Acesso excepcional apenas para recuperação GPS previamente armada.",
            R.drawable.ic_pin, "special", guidance = "Primeiro autorize localização durante uso. Depois escolha Permitir o tempo todo no Android, se disponível."),
        PermissionAccess("admin", "Administrador para bloquear tela", "Acesso especial: permite somente bloquear o aparelho, se ativado localmente.",
            R.drawable.ic_shield, "special", guidance = "Abra Bloqueio remoto para escolher a autorização de administrador do Android."),
        PermissionAccess("media_projection", "Compartilhar tela", "Consentimento do Android é exigido a cada sessão; não pode ser pré-liberado.",
            R.drawable.ic_monitor, "session"),
        PermissionAccess("documents", "Fotos e arquivos", "Seletor de arquivos do Android (SAF): não exige acesso geral às fotos ou armazenamento.",
            R.drawable.ic_folder_download, "session"),
    )

    fun grantStatus(activity: MainActivity, item: PermissionAccess): String {
        val pm = activity.packageManager
        if (item.id in listOf("phone_state", "phone_number", "sms_read", "sms_receive", "sms_send") &&
            !pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) return "Indisponível"
        if (item.id == "alerts" && Build.VERSION.SDK_INT < 33) return "Automática"
        return when (item.kind) {
            "runtime", "sms" -> {
                if (item.id == "location") {
                    val fine = activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    val coarse = activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    when { fine -> "Autorizada"; coarse -> "Aproximada"; else -> "Pendente" }
                } else if (item.perms.all { activity.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED })
                    "Autorizada" else "Pendente"
            }
            "special" -> when (item.id) {
                "listener" -> if (activity.hasListener()) "Autorizada" else "Pendente"
                "accessibility" -> if (hasAccessibility(activity)) "Autorizada" else "Pendente"
                "usage" -> if (hasUsageAccess(activity)) "Autorizada" else "Pendente"
                "background_location" -> if (LocationShareStore.hasBackgroundPermission(activity)) "Autorizada" else "Pendente"
                "admin" -> {
                    val service = activity.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
                    if (service?.isAdminActive(ComponentName(activity, MonitorLockAdminReceiver::class.java)) == true) "Autorizada" else "Pendente"
                }
                else -> "Pendente"
            }
            else -> "Por sessão"
        }
    }

    private fun hasAccessibility(context: Context): Boolean = try {
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        val expected = ComponentName(context, RemoteTouchService::class.java)
        enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
    } catch (_: Exception) { false }

    private fun hasUsageAccess(context: Context): Boolean = try {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName) == AppOpsManager.MODE_ALLOWED
    } catch (_: Exception) { false }

    fun pending(activity: MainActivity): List<PermissionAccess> = entries().filter {
        grantStatus(activity, it) == "Pendente"
    }
    fun runtimeBatch(activity: MainActivity, includeSms: Boolean): MutableList<List<String>> {
        val eligible = entries().filter { it.kind == "runtime" || (includeSms && it.kind == "sms") }
        return eligible.filter { grantStatus(activity, it) == "Pendente" }.map { it.perms }.toMutableList()
    }
}

/** Primeiro uso: abre a página sem solicitar permissões secretamente ou ativar transmissão. */
internal fun MainActivity.renderPermissionCenter(page: LinearLayout) {
    page.addView(iconSectionHeader(R.drawable.ic_shield, "Central de permissões",
        "Autorize no próprio aparelho o que deseja usar"))
    sectionGap(page, 8)
    page.addView(label("As autorizações do Android não ativam compartilhamento remoto por si só. Câmera, áudio, SMS e notificações ainda dependem das opções locais e de sessões autorizadas.", 12.5f, muted = true))
    val entries = PermissionCenter.entries()
    val pending = PermissionCenter.pending(this)
    val granted = entries.count { PermissionCenter.grantStatus(this, it) in listOf("Autorizada", "Aproximada", "Automática") }
    page.addView(label("$granted autorizações disponíveis · ${pending.size} pendentes · ${entries.size - granted - pending.size} por sessão/indisponíveis", 12.5f, strong = true))
    sectionGap(page, 7)
    page.addView(button("Solicitar permissões comuns pendentes", emphasis = true) {
        startPermissionCenterRequests(includeSms = false)
    })
    page.addView(actionButton("Solicitar todas as permissões, incluindo SMS") {
        showAppConfirm("Permissões de SMS e dados sensíveis",
            "Serão exibidos os pedidos do Android para câmera, localização, notificações, áudio, telefonia e SMS que estiverem pendentes. SMS pode ser bloqueado pelo Android/instalador e, na Google Play, exige requisitos adicionais. Nenhum acesso remoto será ativado por essa ação. Acessibilidade, leitura de notificações e localização em segundo plano exigem etapas manuais separadas.",
            R.drawable.ic_shield, "Prosseguir") { startPermissionCenterRequests(includeSms = true) }
    })
    page.addView(actionButton("Abrir permissões nas Configurações do Android") {
        openAppPermissionSettings()
    })
    sectionGap(page, 8)
    val groups = listOf(
        "PERMISSÕES DO SISTEMA" to entries.filter { it.kind == "runtime" },
        "SMS (ACESSO SENSÍVEL)" to entries.filter { it.kind == "sms" },
        "ACESSOS ESPECIAIS" to entries.filter { it.kind == "special" },
        "AUTORIZAÇÃO POR SESSÃO" to entries.filter { it.kind == "session" }
    )
    groups.forEach { (title, items) ->
        page.addView(label(title, 11f, strong = true, muted = true))
        sectionGap(page, 3)
        items.forEach { item ->
            val state = PermissionCenter.grantStatus(this, item)
            val summary = "${item.reason}  •  $state"
            page.addView(iconMenuRow(item.icon, item.label, summary,
                badge = if (state == "Autorizada" || state == "Automática" || state == "Aproximada") SettingsBadgeKind.ACTIVE
                else if (state == "Pendente") SettingsBadgeKind.INACTIVE else SettingsBadgeKind.ACTION,
                actionLabel = when (state) {
                    "Autorizada", "Automática", "Aproximada" -> "OK"
                    "Indisponível" -> "N/D"
                    "Por sessão" -> "INFO"
                    else -> "ABRIR"
                }) { openPermissionAccess(item) })
            sectionGap(page, 5)
        }
        sectionGap(page, 9)
    }
    page.addView(label("A concessão depende do Android. Permissões negadas permanentemente podem exigir Configurações do app. READ_SMS/RECEIVE_SMS são restritas pelo instalador; fotos e arquivos usam o seletor seguro, sem acesso irrestrito.", 11.5f, muted = true))
    page.addView(actionButton("Concluir e abrir o início") { navigateMainTab(2) })
}

internal fun MainActivity.startPermissionCenterRequests(includeSms: Boolean) {
    permissionCenterQueue.clear()
    permissionCenterQueue.addAll(PermissionCenter.runtimeBatch(this, includeSms))
    requestNextPermissionCenterGroup()
}

internal fun MainActivity.requestNextPermissionCenterGroup() {
    if (permissionCenterQueue.isEmpty()) {
        if (mode == 3 && settingsSection == "permissions") render()
        refreshPermissionsNotice()
        return
    }
    val group = permissionCenterQueue.removeAt(0)
    val needed = group.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
    if (needed.isEmpty()) {
        requestNextPermissionCenterGroup()
    } else {
        val marker = PermissionCenter.entries().firstOrNull { it.perms == group }
        if (marker != null) getSharedPreferences("permission_welcome_v1", Context.MODE_PRIVATE)
            .edit().putBoolean("requested_${marker.id}", true).apply()
        requestPermissions(needed.toTypedArray(), PermissionCenter.REQUEST_CODE)
    }
}

internal fun MainActivity.openPermissionAccess(item: PermissionAccess) {
    val state = PermissionCenter.grantStatus(this, item)
    if (state == "Indisponível") {
        showAppNotice(item.label, "Este dispositivo não oferece esse recurso de telefonia.")
        return
    }
    if (item.kind == "runtime" || item.kind == "sms") {
        if (state != "Pendente") {
            showAppNotice(item.label, "Permissão do Android: $state. O compartilhamento remoto continua sujeito às opções locais de cada recurso.")
            return
        }
        val ask = { permissionCenterQueue.clear(); permissionCenterQueue.add(item.perms); requestNextPermissionCenterGroup() }
        if (item.kind == "sms") {
            showAppConfirm("Autorizar ${item.label}?", "${item.reason} O Android pode restringir SMS mesmo após a solicitação. Permitir a permissão não ativa seu compartilhamento remoto.",
                R.drawable.ic_message, "Solicitar") { ask() }
        } else if (!shouldShowRequestPermissionRationale(item.perms.first()) &&
            getSharedPreferences("permission_welcome_v1", Context.MODE_PRIVATE)
                .getBoolean("requested_${item.id}", false)) {
            showAppConfirm("Revisar ${item.label}",
                "O Android pode ter bloqueado novas solicitações após uma negação. Abra a página de permissões do app para autorizar manualmente.",
                item.icon, "Abrir Android") { openAppPermissionSettings() }
        } else ask()
        return
    }
    when (item.id) {
        "listener" -> confirmSpecialPermission(item, Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        "accessibility" -> confirmSpecialPermission(item, Settings.ACTION_ACCESSIBILITY_SETTINGS)
        "usage" -> confirmSpecialPermission(item, Settings.ACTION_USAGE_ACCESS_SETTINGS)
        "background_location" -> {
            if (!LocationShareStore.hasPermission(this)) {
                showAppNotice("Localização necessária", "Autorize primeiro a localização durante o uso. O acesso em segundo plano é opcional para recuperação armada.")
            } else confirmSpecialPermission(item, Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        }
        "admin" -> openSettingsSection("lock")
        "media_projection" -> openSettingsSection("screen")
        "documents" -> openSettingsSection("files")
    }
}

internal fun MainActivity.openAppPermissionSettings() {
    try {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:$packageName")))
    } catch (_: Exception) {
        showAppNotice("Configurações indisponíveis", "Abra manualmente Configurações → Aplicativos → Celular Remoto → Permissões.")
    }
}

private fun MainActivity.confirmSpecialPermission(item: PermissionAccess, action: String) {
    showAppConfirm(item.label, item.guidance + " A autorização precisa ser feita manualmente neste aparelho e pode ser revogada a qualquer momento.",
        item.icon, "Abrir Android") {
        try {
            val intent = Intent(action)
            if (action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS) {
                intent.data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
    }
}

/** Aviso fixo no topo. Contagem refere-se somente aos acessos efetivamente pendentes. */
internal fun MainActivity.refreshPermissionsNotice() {
    permissionsNoticeHost.removeAllViews()
    val pending = PermissionCenter.pending(this)
    if (pending.isEmpty() || hidePermissionNoticeForSession) {
        permissionsNoticeHost.visibility = View.GONE
        return
    }
    permissionsNoticeHost.visibility = View.VISIBLE
    permissionsNoticeHost.setPadding(dp(12), dp(4), dp(12), dp(4))
    val line = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = shape(fieldColor, 13, true)
        setPadding(dp(10), dp(5), dp(5), dp(5))
    }
    val headline = if (pending.size == PermissionCenter.entries().count { it.kind != "session" })
        "Nenhuma autorização configurada" else "${pending.size} acessos pendentes neste aparelho"
    line.addView(label(headline, 12f, strong = true).apply {
        setOnClickListener { openPermissionCenter() }
    }, LinearLayout.LayoutParams(0, -2, 1f))
    line.addView(actionButton("Revisar") { openPermissionCenter() })
    line.addView(actionButton("×") {
        hidePermissionNoticeForSession = true
        refreshPermissionsNotice()
    })
    permissionsNoticeHost.addView(line)
}

internal fun MainActivity.openPermissionCenter() {
    openSettingsModule()
    openSettingsSection("permissions")
}
