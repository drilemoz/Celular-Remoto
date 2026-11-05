package br.dev.celularremoto.core

import android.Manifest
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import br.dev.celularremoto.capture.CameraFrameStore
import br.dev.celularremoto.capture.RemoteTouchService
import br.dev.celularremoto.capture.ScreenFrameStore
import br.dev.celularremoto.service.MonitorService
import br.dev.celularremoto.service.PhotoReadyService
import br.dev.celularremoto.updates.UpdatePreferences
import org.json.JSONArray
import org.json.JSONObject

/** Somente estados genéricos, derivados de grants e opções locais. Sem IP, credencial,
 * SMS, nomes de outros apps, coordenadas, arquivos ou identificadores do aparelho.
 * Enviado apenas dentro de snapshot autenticado; não ativa nada ao ser consultado. */
object RemotePreparation {
    private val ready get() = RemotePreparationPolicy.READY
    private val setup get() = RemotePreparationPolicy.SETUP
    private val session get() = RemotePreparationPolicy.SESSION
    private val off get() = RemotePreparationPolicy.OFF

    private fun granted(context: Context, permission: String): Boolean =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    private fun touchServiceEnabled(context: Context): Boolean = runCatching {
        val expected = ComponentName(context, RemoteTouchService::class.java)
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            .orEmpty().split(':').any { ComponentName.unflattenFromString(it) == expected }
    }.getOrDefault(false)

    private fun notificationReady(context: Context): Boolean = runCatching {
        (Build.VERSION.SDK_INT < 33 || granted(context, Manifest.permission.POST_NOTIFICATIONS)) &&
            (context.getSystemService(NotificationManager::class.java)?.areNotificationsEnabled() == true)
    }.getOrDefault(false)

    private fun installerReady(context: Context): Boolean =
        Build.VERSION.SDK_INT >= 26 && runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)

    fun installerAuthorized(context: Context): Boolean = installerReady(context)

    fun entries(context: Context): List<RemotePreparationPolicy.Item> {
        val monitorActive = SettingsStore.isMonitorEnabled(context) && MonitorService.isRunning
        val notifications = notificationReady(context)
        val locGranted = LocationShareStore.hasPermission(context)
        val locEnabled = LocationShareStore.locationEnabled(context)
        val cameraGranted = granted(context, Manifest.permission.CAMERA)
        val touchGranted = touchServiceEnabled(context)
        val admin = runCatching {
            (context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager)
                .isAdminActive(ComponentName(context, MonitorLockAdminReceiver::class.java))
        }.getOrDefault(false)
        fun item(id: String, title: String, state: String, text: String) =
            RemotePreparationPolicy.Item(id, title, state, text)
        return listOf(
            item("monitor", "Serviço Monitorado", if (monitorActive) ready else setup,
                if (monitorActive) "Servidor ativo" else "Inicie ou recupere o Monitorado neste celular"),
            item("alerts", "Alertas locais", if (notifications) ready else setup,
                if (notifications) "Notificações permitidas pelo Android; canais podem variar" else "Revise permissão e canais de notificação"),
            item("update_requests", "Atualização solicitada remotamente",
                if (UpdatePreferences.remoteRequests(context)) ready else off,
                if (UpdatePreferences.remoteRequests(context)) "Consulta/download autorizados; instalação sempre local" else "Ative em Atualizações se confiar no controlador pareado"),
            item("installer", "Solicitar instalação do APK", if (installerReady(context)) ready else setup,
                if (installerReady(context)) "Origem autorizada; Android ainda exige confirmação" else "Conceda autorização ao instalador deste app no Android"),
            item("location", "Localização", when { !locGranted || !locEnabled -> setup; LocationShareStore.active() && monitorActive -> ready; else -> off },
                when { !locGranted -> "Permissão de localização pendente"; !locEnabled -> "Ative a localização do Android";
                    LocationShareStore.active() && monitorActive -> "Sessão de localização ativa"; else -> "Exige ativação local do compartilhamento" }),
            item("screen", "Compartilhar tela", if (ScreenFrameStore.sharing) ready else session,
                if (ScreenFrameStore.sharing) "Sessão autorizada em andamento" else "Android exige consentimento por sessão"),
            item("touch", "Controle por toque", when {
                !SettingsStore.allowRemoteTouch(context) -> off; !touchGranted -> setup; !ScreenFrameStore.sharing -> session; else -> ready
            }, "Exige opção local, Acessibilidade e sessão de tela ativa"),
            item("camera", "Câmera ao vivo", if (CameraFrameStore.isActive()) ready else if (cameraGranted) session else setup,
                if (!cameraGranted) "Autorize Câmera no Android" else "Abra localmente a câmera para iniciar uma sessão"),
            item("photo", "Fotografia sob demanda", if (PhotoReadyService.armed) ready else if (cameraGranted) session else setup,
                "Preparação local explícita; não arma câmera remotamente"),
            item("sound", "Alerta sonoro", if (SettingsStore.allowRemoteSound(context)) ready else off,
                "Autorize separadamente em Som e lanterna"),
            item("torch", "Lanterna", if (SettingsStore.allowRemoteTorch(context) && cameraGranted) ready else if (SettingsStore.allowRemoteTorch(context)) setup else off,
                "Exige opção local e permissão de câmera quando aplicável"),
            item("lock", "Bloqueio remoto", if (SettingsStore.allowRemoteLock(context) && admin) ready else if (SettingsStore.allowRemoteLock(context)) setup else off,
                "Exige autorização local e Administrador do dispositivo"),
            item("apps", "Aplicativos instalados", if (SettingsStore.shareInstalledApps(context)) ready else off,
                "Lista só é compartilhada após opção local"),
            item("files", "Arquivos próprios do app", if (monitorActive && Build.VERSION.SDK_INT >= 29) ready else setup,
                if (Build.VERSION.SDK_INT < 29) "Pasta fixa depende de Android 10 ou posterior" else
                    "Somente arquivos do app e escopo permitido; não dá acesso geral ao armazenamento"),
            item("sms", "Consulta de SMS", when {
                !SettingsStore.shareSmsInbox(context) -> off
                !granted(context, Manifest.permission.READ_SMS) -> setup
                else -> ready
            }, "Recurso opcional e sensível; exige permissão e autorização local separadas")
        )
    }

    fun snapshot(context: Context): JSONObject {
        val data = entries(context)
        check(RemotePreparationPolicy.valid(data))
        val list = JSONArray()
        data.forEach { list.put(JSONObject().put("id", it.id).put("title", it.title)
            .put("state", it.state).put("note", it.note)) }
        return JSONObject().put("schema", 1)
            .put("readyCount", RemotePreparationPolicy.readyCount(data))
            .put("setupCount", RemotePreparationPolicy.pendingCount(data))
            .put("total", data.size).put("items", list)
    }
}
