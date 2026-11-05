package br.dev.celularremoto.ui

import br.dev.celularremoto.R

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import br.dev.celularremoto.security.SecretVault
import br.dev.celularremoto.core.SettingsStore
import br.dev.celularremoto.service.MonitorService
import br.dev.celularremoto.service.PhotoReadyService

/** O armamento da câmera exige gesto no aparelho monitorado, em Activity visível. */
class RemotePhotoSetupActivity : Activity() {
    private val permissionCode = 6280
    private val notificationsCode = 6282
    private var preparing = false
    private var permissionGrantedAt = 0L
    private var prepareStartedAt = 0L
    private val main = Handler(Looper.getMainLooper())
    private var visible = false
    private lateinit var status: TextView
    private lateinit var arm: Button
    private lateinit var stop: Button
    private var latestMessage: String? = null
    private val waitUntilMonitorReady = object : Runnable {
        override fun run() {
            if (!preparing || !visible || isFinishing || isDestroyed) return
            if (MonitorService.isRunning && SettingsStore.isMonitorEnabled(this@RemotePhotoSetupActivity)) {
                preparing = false
                begin()
            } else if (SystemClock.elapsedRealtime() - prepareStartedAt >= 10_000L) {
                preparing = false
                updateStatus("Monitorado não ficou disponível. Revise a rede e toque novamente para tentar.")
            } else {
                updateStatus("Iniciando Monitorado; aguardando o servidor ficar disponível…")
                main.postDelayed(this, 350L)
            }
        }
    }
    private val updater: Runnable = object : Runnable {
        override fun run() {
            if (!visible) return
            updateStatus()
            main.postDelayed(this, 1_000L)
        }
    }
    private fun dp(v: Int) = (resources.displayMetrics.density * v).toInt()
    private val skin by lazy { MonitorScreenSkin(this) }
    private val bg get() = skin.background
    private val surface get() = skin.surface
    private val ink get() = skin.foreground
    private val muted get() = skin.muted
    private fun rounded(color: Int) = skin.rounded(color, 20, true)
    private fun text(value: String, size: Float = 14f, title: Boolean = false): TextView =
        skin.label(value, size, title)
    private fun section(content: LinearLayout, heading: String, explanation: String) {
        val card = skin.card()
        card.addView(text(heading, 15f, true))
        card.addView(text(explanation, 13f).apply { setPadding(0, dp(7), 0, 0) })
        content.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
    }
    private fun action(label: String, highlighted: Boolean, click: () -> Unit): Button =
        skin.button(label, highlighted, click)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        skin.applyWindow()
        val scroller = ScrollView(this).apply { setBackgroundColor(bg); isFillViewport = true; fitsSystemWindows = true }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(24))
        }
        scroller.addView(content)
        content.addView(skin.header(R.drawable.ic_remote_camera, "Fotografia remota", "Preparação da câmera neste aparelho") { finish() })
        val current = skin.card()
        current.addView(skin.badge("● STATUS DA SESSÃO", true), LinearLayout.LayoutParams(-2, -2).apply {
            bottomMargin = dp(10)
        })
        current.addView(text("Estado da câmera", 16f, true))
        status = text("Verificando…", 13f).apply { setPadding(0, dp(7), 0, 0) }
        current.addView(status)
        content.addView(current, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(13) })
        arm = action("Ativar Monitorado e preparar câmera", true) { authorize() }
        stop = action("Parar câmera remota", false) {
            PhotoReadyService.stop(this)
            latestMessage = "Solicitação de parada enviada."
            updateStatus()
        }
        content.addView(arm, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(12) })
        content.addView(stop, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(8) })
        section(content, "Como usar", "1. Toque em Ativar Monitorado e preparar câmera neste celular.\n" +
            "2. Autorize Câmera e Notificações, se o Android solicitar.\n" +
            "3. Apague a tela; o controlador pareado poderá pedir fotos enquanto a sessão estiver ativa.")
        section(content, "Privacidade e controle", "A sessão é sinalizada por uma notificação permanente com ação Parar. " +
            "Somente aparelhos que possuam a chave de pareamento podem solicitar fotografias, enviadas por AES-GCM. " +
            "Nenhuma foto é gravada automaticamente neste celular.")
        section(content, "Limites do Android", "A câmera não fica gravando ou capturando continuamente; é aberta apenas para cada fotografia. " +
            "Não é possível rearmar a câmera automaticamente depois de reiniciar, " +
            "forçar a parada ou se o Android encerrar o serviço. O monitorado precisa continuar conectado pela LAN " +
            "ou por uma rede privada previamente configurada.")
        setContentView(scroller)
        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        visible = true
        if (permissionGrantedAt != 0L) {
            val elapsed = SystemClock.elapsedRealtime() - permissionGrantedAt
            permissionGrantedAt = 0L
            if (elapsed in 0..15_000L) main.post { if (visible) authorize() }
            else latestMessage = "Permissão concedida. Toque em Preparar câmera para continuar."
        }
        main.removeCallbacks(updater)
        main.post(updater)
    }
    override fun onPause() {
        visible = false
        main.removeCallbacks(updater)
        main.removeCallbacks(waitUntilMonitorReady)
        if (preparing) {
            preparing = false
            latestMessage = "Preparação interrompida; mantenha esta tela visível e tente novamente."
        }
        super.onPause()
    }

    private fun updateStatus(message: String? = null) {
        if (message != null) latestMessage = message
        val armed = PhotoReadyService.armed
        status.text = when {
            armed -> "● Ativa · Pronta para fotografias autenticadas com a tela apagada"
            preparing -> latestMessage ?: "Preparando serviço e câmera…"
            else -> latestMessage ?: PhotoReadyService.statusMessage()
        }
        arm.text = if (SettingsStore.isMonitorEnabled(this) && MonitorService.isRunning)
            "Preparar câmera" else "Ativar Monitorado e preparar câmera"
        arm.isEnabled = !armed && !preparing
        stop.isEnabled = armed
        if (armed) latestMessage = null
    }

    /** Um único gesto local inicia o Monitorado e prepara a sessão; nunca chamado de comando remoto. */
    private fun authorize() {
        if (!visible || preparing || PhotoReadyService.armed) return
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), permissionCode)
            return
        }
        // A notificação visível de câmera é parte do consentimento informado.
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), notificationsCode)
            return
        }
        beginMonitoringAndArm()
    }

    private fun beginMonitoringAndArm() {
        val wasEnabled = SettingsStore.isMonitorEnabled(this)
        try {
            if (!wasEnabled) {
                SecretVault.getOrCreateMonitorKey(this)
                SettingsStore.setMonitorEnabled(this, true)
            }
            if (!MonitorService.isRunning) MonitorService.start(this)
            preparing = true
            prepareStartedAt = SystemClock.elapsedRealtime()
            updateStatus("Preparando Monitorado e câmera…")
            main.removeCallbacks(waitUntilMonitorReady)
            main.post(waitUntilMonitorReady)
        } catch (ex: Exception) {
            preparing = false
            if (!wasEnabled) SettingsStore.setMonitorEnabled(this, false)
            updateStatus("Não foi possível ativar Monitorado: ${ex.message ?: "falha do Android"}")
        }
    }

    private fun begin() {
        try {
            val request = Intent(this, PhotoReadyService::class.java).setAction(PhotoReadyService.ACTION_START)
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(request) else startService(request)
            updateStatus("Preparando sessão… Confira a notificação do Android.")
        } catch (ex: Exception) {
            updateStatus("Android não autorizou armar a câmera: ${ex.message ?: "falha"}")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionCode) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                if (visible) main.post { authorize() }
                else permissionGrantedAt = SystemClock.elapsedRealtime()
            } else updateStatus("Permissão CAMERA recusada. Nenhuma sessão foi iniciada.")
        } else if (requestCode == notificationsCode) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                if (visible) main.post { authorize() }
                else permissionGrantedAt = SystemClock.elapsedRealtime()
            }
            else updateStatus("Autorize as notificações para exibir claramente a sessão de câmera. Revise as permissões do app.")
        }
    }
}
