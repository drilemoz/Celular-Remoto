package br.dev.celularremoto.updates

import android.Manifest
import android.app.DownloadManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import br.dev.celularremoto.BuildConfig
import br.dev.celularremoto.R
import br.dev.celularremoto.ui.MainActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.concurrent.thread
import java.util.concurrent.atomic.AtomicBoolean

internal data class UpdateRelease(val version: String, val code: Int, val sha256: String,
    val apkUrl: String, val apkName: String)

/** Verificação online e download durável; a instalação só é acionada pela Activity local. */
object UpdateController {
    private val checkLock = Any()
    private val remoteBusy = AtomicBoolean(false)
    private val remoteThrottle = Any()
    private var lastRemoteRequestAt = 0L
    private const val REMOTE_COOLDOWN_MS = 120_000L
    private const val API = "https://api.github.com/repos/${UpdateRules.REPOSITORY}/releases?per_page=30"
    private const val CHANNEL_ID = "celular_remoto_updates"
    private const val NOTICE_ID = 3312
    private const val MAX_META_BYTES = 1024 * 1024

    fun state(context: Context): String = UpdatePreferences.getString(context, "state").ifBlank { "Aguardando verificação" }
    fun lastCheck(context: Context): Long = UpdatePreferences.getLong(context, "last_check")
    fun availableVersion(context: Context): String = UpdatePreferences.getString(context, "available_version")
    fun prepared(context: Context): Boolean = UpdatePreferences.getString(context, "state_code") == "ready" &&
        UpdatePreferences.getLong(context, "download_code") > BuildConfig.VERSION_CODE &&
        UpdatePreferences.getLong(context, "download_id") > 0L


    /** Estado de atualização limitado e sem URLs ou identificadores sensíveis. */
    fun remoteStatus(context: Context): JSONObject = JSONObject()
        .put("version", BuildConfig.VERSION_NAME)
        .put("versionCode", BuildConfig.VERSION_CODE)
        .put("state", state(context).take(160))
        .put("lastCheck", lastCheck(context))
        .put("availableVersion", availableVersion(context).take(50))
        .put("downloadReady", prepared(context))
        .put("progress", downloadProgress(context).orEmpty().take(80))
        .put("remoteAllowed", UpdatePreferences.remoteRequests(context))
        .put("autoCheck", UpdatePreferences.checking(context))
        .put("autoDownload", UpdatePreferences.autoDownload(context))

    /**
     * Chamada exclusivamente pelo servidor pareado depois de AES-GCM válido.
     * STATUS sempre é de leitura. Operações com internet exigem opt-in local,
     * respeitam preferência de rede/bateria e possuem limitação de frequência.
     * Não há método para instalar APK remotamente.
     */
    fun remoteAction(context: Context, action: String): Pair<Boolean, String> {
        if (action == "status") return true to "Estado consultado"
        if (action !in setOf("check", "checkDownload", "download"))
            return false to "Operação de atualização desconhecida"
        if (!UpdatePreferences.remoteRequests(context))
            return false to "Autorize pedidos de atualização em Configurações → Atualizações no aparelho monitorado"
        if (action == "download") {
            if (remoteBusy.get()) return false to "Já existe uma consulta remota em andamento"
            return true to download(context)
        }
        synchronized(remoteThrottle) {
            if (remoteBusy.get()) return false to "Atualização remota já em andamento"
            val now = android.os.SystemClock.elapsedRealtime()
            if (lastRemoteRequestAt > 0 && now - lastRemoteRequestAt < REMOTE_COOLDOWN_MS)
                return false to "Aguarde dois minutos antes de solicitar outra verificação"
            lastRemoteRequestAt = now
            remoteBusy.set(true)
        }
        thread(name = "CelularRemoto-remote-update") {
            try {
                check(context.applicationContext, automatic = false)
                if (action == "checkDownload" &&
                    (UpdatePreferences.getString(context, "available_code").toIntOrNull() ?: 0) > BuildConfig.VERSION_CODE)
                    download(context.applicationContext)
            } finally { remoteBusy.set(false) }
        }
        return true to "Solicitação recebida. Acompanhe o estado remoto em instantes."
    }

    fun downloadProgress(context: Context): String? {
        val id = UpdatePreferences.getLong(context, "download_id")
        if (id <= 0 || prepared(context)) return null
        val dm = context.getSystemService(DownloadManager::class.java) ?: return null
        return runCatching { dm.query(DownloadManager.Query().setFilterById(id))?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val done = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            when (status) {
                DownloadManager.STATUS_RUNNING -> if (total > 0) "Baixando: ${done * 100 / total}%" else "Baixando…"
                DownloadManager.STATUS_PENDING, DownloadManager.STATUS_PAUSED -> "Na fila (aguardando condições de rede)"
                DownloadManager.STATUS_SUCCESSFUL -> "Download concluído; verificando assinatura…"
                DownloadManager.STATUS_FAILED -> "Falha ao baixar"
                else -> null
            }
        } }.getOrNull()
    }

    /** Reagendamento após atualização/cold start; sem rede na thread principal. */
    fun initialize(context: Context) {
        UpdateScheduler.refresh(context)
        // Primeira abertura ou retorno após várias horas: conferir sem depender
        // da próxima janela imprecisa do JobScheduler. Nunca abrir Activity.
        val now = System.currentTimeMillis()
        if (UpdatePreferences.checking(context) &&
            (lastCheck(context) <= 0L || now - lastCheck(context) >= UpdatePreferences.intervalMinutes(context) * 60_000L)) {
            thread(name = "CelularRemoto-update-cold-start") {
                check(context.applicationContext, automatic = true)
            }
        }
        val code = UpdatePreferences.getLong(context, "download_code")
        if ((UpdatePreferences.getString(context, "available_code").toIntOrNull() ?: 0) <= BuildConfig.VERSION_CODE) {
            UpdatePreferences.putString(context, "available_version", "")
            UpdatePreferences.putString(context, "available_code", "")
        }
        if (code > 0 && code <= BuildConfig.VERSION_CODE) {
            UpdatePreferences.clearDownload(context)
            UpdateVerifyJobService.stopFallback(context)
            UpdatePreferences.putString(context, "state_code", "installed")
            UpdatePreferences.putString(context, "state", "Versão atual instalada")
        }
        val id = UpdatePreferences.getLong(context, "download_id")
        if (id > 0 && code > BuildConfig.VERSION_CODE) thread(name = "CelularRemoto-update-recover") {
            reconcileDownload(context.applicationContext, id)
        }
    }

    /** Verifica tanto prereleases como releases, mas só aceita uma Release com manifesto assinado na CI. */
    fun check(context: Context, automatic: Boolean): String = synchronized(checkLock) {
        if (automatic && !UpdatePreferences.checking(context)) return@synchronized "Verificação desativada"
        UpdatePreferences.putLong(context, "last_check", System.currentTimeMillis())
        UpdatePreferences.putString(context, "state", "Consultando GitHub Releases…")
        try {
            val release = findUpdate()
            if (release == null) {
                if (!prepared(context)) {
                    UpdatePreferences.putString(context, "state_code", "current")
                    UpdatePreferences.putString(context, "state", "Nenhuma atualização assinada mais recente")
                }
            } else {
                UpdatePreferences.putString(context, "available_version", release.version)
                UpdatePreferences.putString(context, "available_code", release.code.toString())
                UpdatePreferences.putString(context, "available_sha", release.sha256)
                UpdatePreferences.putString(context, "available_url", release.apkUrl)
                UpdatePreferences.putString(context, "available_name", release.apkName)
                if (UpdatePreferences.getLong(context, "download_code") == release.code.toLong() &&
                    UpdatePreferences.getLong(context, "download_id") > 0L) {
                    reconcileDownload(context, UpdatePreferences.getLong(context, "download_id"))
                } else {
                    UpdatePreferences.putString(context, "state_code", "available")
                    UpdatePreferences.putString(context, "state", "Nova versão ${release.version} disponível")
                    if (automatic && UpdatePreferences.autoDownload(context)) download(context)
                }
            }
            state(context)
        } catch (e: Exception) {
            // Não apagar uma atualização pronta quando a consulta falhar por falta de internet.
            if (!prepared(context)) {
                UpdatePreferences.putString(context, "state_code", "error")
                UpdatePreferences.putString(context, "state", "Não foi possível verificar: ${e.message?.take(100) ?: "erro de rede"}")
            }
            state(context)
        }
    }

    private fun readLimited(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 12_000
        conn.readTimeout = 15_000
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("User-Agent", "CelularRemoto-Android/${BuildConfig.VERSION_NAME}")
        return try {
            require(conn.responseCode == 200) { "GitHub HTTP ${conn.responseCode}" }
            conn.inputStream.use { stream ->
                val data = readMax(stream, MAX_META_BYTES)
                String(data, Charsets.UTF_8)
            }
        } finally { conn.disconnect() }
    }

    private fun readMax(stream: InputStream, limit: Int): ByteArray {
        val buffer = ByteArray(8192)
        val out = java.io.ByteArrayOutputStream()
        while (true) {
            val n = stream.read(buffer)
            if (n < 0) break
            require(out.size() + n <= limit) { "Resposta GitHub muito grande" }
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }

    private fun findUpdate(): UpdateRelease? {
        val releases = JSONArray(readLimited(API))
        var best: UpdateRelease? = null
        for (i in 0 until releases.length()) {
            val item = releases.optJSONObject(i) ?: continue
            if (item.optBoolean("draft", false)) continue
            val tag = item.optString("tag_name")
            val assets = item.optJSONArray("assets") ?: continue
            var metaUrl = ""
            var apkUrl = ""
            var apkName = ""
            for (j in 0 until assets.length()) {
                val asset = assets.optJSONObject(j) ?: continue
                val name = asset.optString("name")
                val url = asset.optString("browser_download_url")
                if (!UpdateRules.allowedAssetUrl(url, tag, name)) continue
                if (name == UpdateRules.MANIFEST_NAME) metaUrl = url
                else if (name.endsWith(".apk") && name.startsWith("CelularRemoto-v") &&
                    asset.optLong("size", 0L) in 1..UpdateRules.MAX_APK_BYTES) {
                    apkName = name; apkUrl = url
                }
            }
            if (metaUrl.isBlank() || apkUrl.isBlank()) continue
            val meta = runCatching { JSONObject(readLimited(metaUrl)) }.getOrNull() ?: continue
            val version = meta.optString("versionName")
            val code = meta.optInt("versionCode", -1)
            val sha = meta.optString("sha256")
            if (meta.optInt("schema", 0) != 1 || meta.optString("apkName") != apkName ||
                !UpdateRules.isValidRelease(version, code, BuildConfig.VERSION_CODE,
                    sha, meta.optString("packageName"), meta.optBoolean("stableSigned", false), tag, apkName)) continue
            if (best == null || code > best.code) best = UpdateRelease(version, code, sha.lowercase(), apkUrl, apkName)
        }
        return best
    }

    private fun enoughBattery(context: Context): Boolean {
        if (!UpdatePreferences.batteryCare(context)) return true
        val info = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return false
        val level = info.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = info.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val plugged = info.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        return plugged != 0 || (level >= 0 && scale > 0 && level * 100 / scale >= 25)
    }

    /** Enfileira no DownloadManager; persiste ID para acompanhar mesmo após fechar a interface. */
    @Synchronized fun download(context: Context): String {
        val version = UpdatePreferences.getString(context, "available_version")
        val code = UpdatePreferences.getString(context, "available_code").toIntOrNull() ?: return "Sem atualização disponível"
        val sha = UpdatePreferences.getString(context, "available_sha")
        val url = UpdatePreferences.getString(context, "available_url")
        val fileName = UpdateRules.apkName(version) ?: return "Nome de APK inválido"
        if (!UpdateRules.isValidRelease(version, code, BuildConfig.VERSION_CODE, sha, UpdateRules.PACKAGE,
                true, "v$version", fileName) ||
            !UpdateRules.allowedAssetUrl(url, "v$version", fileName)) return "Metadados inválidos"
        if (UpdatePreferences.getLong(context, "download_id") > 0 &&
            UpdatePreferences.getLong(context, "download_code") == code.toLong())
            return state(context)
        if (UpdatePreferences.batteryCare(context) &&
            runCatching { br.dev.celularremoto.core.BatteryLabStore.report(context)
                .optJSONObject("active")?.optBoolean("active", false) == true }.getOrDefault(false)) {
            UpdatePreferences.putString(context, "state", "Aguardando o fim do teste de bateria em andamento")
            return state(context)
        }
        if (!enoughBattery(context)) {
            UpdatePreferences.putString(context, "state", "Aguardando bateria acima de 25% ou carregador")
            return state(context)
        }
        return try {
            val dm = context.getSystemService(DownloadManager::class.java) ?: return "DownloadManager indisponível"
            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: return "Armazenamento externo indisponível"
            val sub = File(downloadDir, "updates")
            if (!sub.exists() && !sub.mkdirs()) return "Pasta de download indisponível"
            val previousId = UpdatePreferences.getLong(context, "download_id")
            if (previousId > 0) {
                dm.remove(previousId)
                UpdatePreferences.clearDownload(context)
            }
            val target = File(sub, fileName)
            if (target.exists() && !target.delete()) return "Não foi possível substituir APK anterior"
            val req = DownloadManager.Request(Uri.parse(url))
                .setTitle("Celular Remoto $version")
                .setDescription("Baixando atualização assinada")
                .setMimeType("application/vnd.android.package-archive")
                .setAllowedOverMetered(!UpdatePreferences.unmetered(context))
                .setAllowedOverRoaming(false)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "updates/$fileName")
            val id = dm.enqueue(req)
            UpdatePreferences.putLong(context, "download_id", id)
            UpdatePreferences.putLong(context, "download_code", code.toLong())
            UpdatePreferences.putString(context, "download_hash", sha.lowercase())
            UpdatePreferences.putString(context, "download_name", fileName)
            UpdatePreferences.putString(context, "download_version", version)
            UpdatePreferences.putString(context, "state_code", "downloading")
            UpdatePreferences.putString(context, "state", "Download enfileirado: $version${if (UpdatePreferences.unmetered(context)) " (rede não tarifada)" else ""}")
            UpdateVerifyJobService.scheduleFallback(context)
            state(context)
        } catch (e: Exception) {
            UpdatePreferences.putString(context, "state", "Falha ao iniciar download: ${e.message?.take(90)}")
            state(context)
        }
    }

    private fun file(context: Context): File? {
        val name = UpdatePreferences.getString(context, "download_name")
        if (UpdateRules.apkName(UpdatePreferences.getString(context, "download_version")) != name) return null
        val parent = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return null
        return File(File(parent, "updates"), name)
    }

    internal fun reconcileDownload(context: Context, id: Long) {
        val dm = context.getSystemService(DownloadManager::class.java) ?: return
        val status = dm.query(DownloadManager.Query().setFilterById(id))?.use { cursor ->
            if (!cursor.moveToFirst()) null else cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
        }
        when (status) {
            DownloadManager.STATUS_SUCCESSFUL -> verifyDownload(context, id)
            DownloadManager.STATUS_FAILED, null -> {
                UpdatePreferences.putString(context, "state_code", "error")
                UpdatePreferences.putString(context, "state", "O download foi interrompido; verifique a rede e tente novamente")
                UpdatePreferences.clearDownload(context)
                UpdateVerifyJobService.stopFallback(context)
            }
        }
    }

    /** Só declara o APK pronto após hash, pacote, versão e certificado Android corresponderem. */
    fun verifyDownload(context: Context, id: Long): Boolean = synchronized(checkLock) {
        if (id <= 0 || id != UpdatePreferences.getLong(context, "download_id")) return@synchronized false
        val dm = context.getSystemService(DownloadManager::class.java) ?: return@synchronized false
        val completed = dm.query(DownloadManager.Query().setFilterById(id))?.use { cursor ->
            cursor.moveToFirst() && cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL
        } ?: false
        if (!completed) return@synchronized false
        val apk = file(context)
        val code = UpdatePreferences.getLong(context, "download_code")
        val expected = UpdatePreferences.getString(context, "download_hash")
        val valid = apk != null && runCatching {
            apk.isFile && apk.length() in 1..UpdateRules.MAX_APK_BYTES &&
                hashFile(apk).equals(expected, ignoreCase = true) &&
                checkPackageSignature(context, apk, code)
        }.getOrDefault(false)
        if (!valid) {
            apk?.delete()
            UpdatePreferences.putString(context, "state_code", "error")
            UpdatePreferences.putString(context, "state", "APK rejeitado: integridade, pacote, versão ou assinatura inválida")
            UpdatePreferences.clearDownload(context)
            UpdateVerifyJobService.stopFallback(context)
            return@synchronized false
        }
        val wasReady = prepared(context)
        UpdatePreferences.putString(context, "state_code", "ready")
        UpdatePreferences.putString(context, "state", "Atualização ${UpdatePreferences.getString(context, "download_version")} pronta para instalar")
        UpdateVerifyJobService.stopFallback(context)
        if (!wasReady) notifyReady(context)
        true
    }

    private fun hashFile(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(32 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                md.update(buffer, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    @Suppress("DEPRECATION")
    private fun checkPackageSignature(context: Context, apk: File, code: Long): Boolean = runCatching {
        val pm = context.packageManager
        val flag = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val archive = pm.getPackageArchiveInfo(apk.absolutePath, flag) ?: return@runCatching false
        if (archive.packageName != UpdateRules.PACKAGE) return@runCatching false
        val archiveCode = if (Build.VERSION.SDK_INT >= 28) archive.longVersionCode else archive.versionCode.toLong()
        if (archiveCode != code || archiveCode <= BuildConfig.VERSION_CODE) return@runCatching false
        val installed = pm.getPackageInfo(context.packageName, flag)
        val oldCerts = if (Build.VERSION.SDK_INT >= 28) installed.signingInfo?.apkContentsSigners else installed.signatures
        val newCerts = if (Build.VERSION.SDK_INT >= 28) archive.signingInfo?.apkContentsSigners else archive.signatures
        !oldCerts.isNullOrEmpty() && !newCerts.isNullOrEmpty() &&
            oldCerts.size == newCerts.size && oldCerts.all { a -> newCerts.any { b -> a.toByteArray().contentEquals(b.toByteArray()) } }
    }.getOrDefault(false)

    private fun notifyReady(context: Context) {
        val activity = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_UPDATES, true)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pending = PendingIntent.getActivity(context, NOTICE_ID, activity,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val version = UpdatePreferences.getString(context, "download_version")
        val note = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle("Celular Remoto $version")
            .setContentText("APK verificado. Toque para confirmar a instalação.")
            .setContentIntent(pending).setAutoCancel(true).build()
        br.dev.celularremoto.core.AlertDelivery.publish(context, CHANNEL_ID,
            "Atualizações do Celular Remoto", "UPDATE_READY", NOTICE_ID, note,
            NotificationManager.IMPORTANCE_DEFAULT)
    }

    /** Chamado exclusivamente por clique local em UI; não por broadcast, rede ou agendador. */
    fun installFromUserTap(context: Context): String {
        val id = UpdatePreferences.getLong(context, "download_id")
        if (!prepared(context) || !verifyDownload(context, id)) return "APK não está pronto ou falhou na verificação"
        if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
            val settings = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"))
            context.startActivity(settings)
            return "Autorize esta origem nas configurações do Android e toque em Instalar novamente"
        }
        val dm = context.getSystemService(DownloadManager::class.java) ?: return "DownloadManager indisponível"
        val uri = dm.getUriForDownloadedFile(id) ?: return "Arquivo não está disponível"
        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            })
            "Instalador do Android aberto. Confirme a atualização no aparelho."
        } catch (e: Exception) { "Não foi possível abrir o instalador: ${e.message?.take(90)}" }
    }
}
