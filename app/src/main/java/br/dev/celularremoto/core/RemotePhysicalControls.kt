package br.dev.celularremoto.core

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import org.json.JSONObject

/** Pertence ao serviço de monitoramento. Não grava estados em disco nem altera volume/silencioso. */
object RemotePhysicalControls {
    private val main = Handler(Looper.getMainLooper())
    private var sirenTrack: AudioTrack? = null
    private var soundUntilMs = 0L
    private var soundGeneration = 0
    private var torchManager: CameraManager? = null
    private var torchId: String? = null
    private var torchState = "desconhecida"
    private var torchOwned = false
    private var callback: CameraManager.TorchCallback? = null
    private var lastTorchRequestMs = 0L

    /** A câmera selecionada tem flash físico; o callback observa alterações reais do Android. */
    @Synchronized fun start(context: Context) {
        if (callback != null) return
        val manager = context.getSystemService(CameraManager::class.java) ?: return
        val id = try {
            manager.cameraIdList.firstOrNull { candidate ->
                val info = manager.getCameraCharacteristics(candidate)
                info.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK &&
                    info.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (_: Exception) { null }
        torchManager = manager
        torchId = id
        torchState = if (id == null) "indisponível" else "desconhecida"
        if (id == null) return
        val observer = object : CameraManager.TorchCallback() {
            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                synchronized(this@RemotePhysicalControls) {
                    if (cameraId == torchId) {
                        torchState = if (enabled) "ligada" else "desligada"
                        if (!enabled) torchOwned = false
                    }
                }
            }
            override fun onTorchModeUnavailable(cameraId: String) {
                synchronized(this@RemotePhysicalControls) {
                    if (cameraId == torchId) { torchState = "indisponível"; torchOwned = false }
                }
            }
        }
        try {
            manager.registerTorchCallback(observer, main)
            callback = observer
        } catch (_: Exception) {
            torchState = "desconhecida"
        }
    }

    /** Sirene sintética original em dois tons alternados, sem alterar volume do Android.
     * A thread de áudio existe somente durante o alerta e respeita DND e a sessão ativa.
     */
    @Synchronized fun startSound(seconds: Int): String {
        check(seconds in DeviceControlPolicy.durations) { "Duração inválida." }
        stopSound()
        val sampleRate = 22_050
        val blockFrames = 882 // 40 ms; escrita contínua, com fase para evitar estalos.
        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(blockFrames * 2 * 4)
            .build()
        if (track.state != AudioTrack.STATE_INITIALIZED) {
            track.release()
            error("O Android não inicializou o alerta sonoro.")
        }
        try { track.play() } catch (error: Exception) {
            track.release()
            throw IllegalStateException("Não foi possível iniciar a sirene.", error)
        }
        sirenTrack = track
        val generation = ++soundGeneration
        soundUntilMs = SystemClock.elapsedRealtime() + seconds * 1000L
        Thread({
            val pcm = ShortArray(blockFrames)
            var sampleIndex = 0L
            var phase = 0.0
            try {
                while (synchronized(this@RemotePhysicalControls) {
                    generation == soundGeneration && sirenTrack === track &&
                        SystemClock.elapsedRealtime() < soundUntilMs
                }) {
                    for (i in pcm.indices) {
                        // Subida/descida a cada 0,8s: sirene distinguível dos bipes antigos.
                        val cycle = (sampleIndex % (sampleRate * 2L)).toDouble() / (sampleRate * 2.0)
                        val sweep = if (cycle < 0.5) cycle * 2 else (1.0 - cycle) * 2
                        val frequency = 690.0 + 410.0 * sweep
                        phase += 2.0 * PI * frequency / sampleRate
                        if (phase > PI * 2) phase -= PI * 2
                        pcm[i] = (sin(phase) * 0.32 * Short.MAX_VALUE).toInt().toShort()
                        sampleIndex++
                    }
                    val n = track.write(pcm, 0, pcm.size, AudioTrack.WRITE_BLOCKING)
                    if (n <= 0) break
                }
            } catch (_: IllegalStateException) {
                // A parada pode liberar o AudioTrack enquanto a escrita termina.
            } finally {
                synchronized(this@RemotePhysicalControls) {
                    if (generation == soundGeneration && sirenTrack === track) stopSound()
                }
            }
        }, "CelularRemoto-sirene").apply { isDaemon = true }.start()
        return "Sirene solicitada por ${seconds}s. Volume e Não Perturbe podem impedir a audição."
    }

    @Synchronized fun stopSound(): String {
        soundGeneration++
        soundUntilMs = 0L
        val old = sirenTrack
        sirenTrack = null
        runCatching { old?.pause(); old?.flush(); old?.release() }
        return "Sirene interrompida."
    }

    /** Desliga somente o flash ligado por este serviço, não o uso prévio por outro app. */
    @Synchronized fun stopOwnedTorch() {
        if (torchOwned) {
            val id = torchId
            if (id != null) runCatching { torchManager?.setTorchMode(id, false) }
            torchOwned = false
            torchState = "desconhecida" // confirmar somente por TorchCallback
        }
    }

    @Synchronized fun setTorch(enabled: Boolean): String {
        val manager = torchManager ?: error("Câmera não disponível neste aparelho.")
        val id = torchId ?: error("Sem flash traseiro compatível.")
        // Evita spam involuntário e disputa com outros apps.
        val now = SystemClock.elapsedRealtime()
        check(now - lastTorchRequestMs >= 500L) { "Aguarde antes de repetir o comando." }
        lastTorchRequestMs = now
        try { manager.setTorchMode(id, enabled) }
        catch (e: SecurityException) { error("Autorize Câmera nas permissões do Android monitorado.") }
        catch (e: Exception) { error("Flash indisponível ou ocupado por outro aplicativo.") }
        torchOwned = enabled
        // Não inventar sucesso do hardware enquanto o callback não o confirmar.
        torchState = "desconhecida"
        return "Solicitação enviada; consulte o estado confirmado pelo Android."
    }

    @Synchronized fun status(context: Context): JSONObject = JSONObject()
        .put("soundAllowed", SettingsStore.allowRemoteSound(context))
        .put("torchAllowed", SettingsStore.allowRemoteTorch(context))
        .put("lockAllowed", SettingsStore.allowRemoteLock(context))
        .put("lockAdminActive", RemoteScreenLock.adminActive(context))
        .put("lockSecure", RemoteScreenLock.secureLockConfigured(context))
        .put("lockSupported", RemoteScreenLock.supported(context))
        .put("soundActive", sirenTrack != null && SystemClock.elapsedRealtime() < soundUntilMs)
        .put("soundRemainingSeconds", if (sirenTrack == null) 0 else
            ((soundUntilMs - SystemClock.elapsedRealtime() + 999) / 1000).coerceAtLeast(0))
        .put("torchState", torchState)

    @Synchronized fun shutdown() {
        stopSound()
        stopOwnedTorch()
        val listener = callback
        if (listener != null) runCatching { torchManager?.unregisterTorchCallback(listener) }
        callback = null
        torchManager = null
        torchId = null
        torchState = "desconhecida"
        lastTorchRequestMs = 0L
    }
}
