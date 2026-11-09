package br.dev.celularremoto.ui

import android.app.AlertDialog
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import br.dev.celularremoto.R
import br.dev.celularremoto.core.DeviceControlPolicy
import br.dev.celularremoto.core.SettingsStore

/** Bloqueio nunca é disparado por simples abertura do cartão ou consulta de estado. */
internal fun MainActivity.showRemoteLockDialog() {
    val host = SettingsStore.getRemoteHost(this)
    val (wrapper, layout) = dialogWrapper()
    layout.addView(visualHeader(R.drawable.ic_shield, "Bloquear aparelho remoto",
        currentDeviceTitle?.text?.toString().orEmpty().ifBlank { "Aparelho conectado" }))
    sectionGap(layout, 10)
    layout.addView(visualInfoPanel(R.drawable.ic_info, "Atenção antes de bloquear",
        "O monitorado precisa de administrador Android e bloqueio seguro (PIN, padrão ou senha). Com o botão Power quebrado, talvez seja difícil voltar a acessar a tela."))
    sectionGap(layout, 10)
    val status = label("Consultando permissão do monitorado…", 12.5f, strong = true)
    val feedback = label("", 12f, muted = true)
    layout.addView(panel(12).apply {
        background = shape(fieldColor, 14)
        addView(status)
        addView(feedback)
    })
    sectionGap(layout, 10)
    layout.addView(label("Digite BLOQUEAR para confirmar esta operação:", 12.5f, strong = true))
    val confirm = EditText(this).apply {
        hint = "BLOQUEAR"
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        isSingleLine = true
        setTextColor(ink)
        setHintTextColor(dim)
    }
    confirm.background = shape(fieldColor, 14, true)
    confirm.setPadding(dp(13), 0, dp(13), 0)
    layout.addView(confirm, LinearLayout.LayoutParams(-1, dp(52)))
    sectionGap(layout, 9)
    val sendButton = actionButton("Bloquear agora", emphasis = true) { }
    layout.addView(sendButton)
    sectionGap(layout, 8)
    val refreshButton = actionButton("Atualizar autorização") { }
    layout.addView(refreshButton)
    val dialog = themedPopup(this).setView(wrapper).setNegativeButton("Fechar", null).create()
    var busy = false
    var permissionReady = false
    fun setEnabled() {
        sendButton.isEnabled = !busy && permissionReady &&
            confirm.text.toString().trim() == DeviceControlPolicy.LOCK_CONFIRMATION
        sendButton.alpha = if (sendButton.isEnabled) 1f else 0.5f
        refreshButton.isEnabled = !busy
    }
    fun request(action: String, confirmation: String = "") {
        if (busy || !dialog.isShowing) return
        busy = true
        setEnabled()
        remoteConnection.deviceControl(host, action, 0, confirmation) { result ->
            if (!dialog.isShowing) return@deviceControl
            busy = false
            result.fold(onSuccess = { reply ->
                val remoteStatus = reply.optJSONObject("status")
                permissionReady = remoteStatus?.optBoolean("lockAllowed", false) == true &&
                    remoteStatus?.optBoolean("lockAdminActive", false) == true &&
                    remoteStatus?.optBoolean("lockSecure", false) == true &&
                    remoteStatus?.optBoolean("lockSupported", false) == true
                status.text = if (permissionReady) "Autorização e bloqueio seguro disponíveis" else
                    "Bloqueio ainda não autorizado ou indisponível"
                feedback.text = when {
                    !reply.optBoolean("ok", false) -> reply.optString("message").ifBlank { "Pedido recusado." }
                    action == "lockNow" -> reply.optString("message").ifBlank { "Solicitação recebida." }
                    remoteStatus?.optBoolean("lockAllowed", false) != true -> "Ative Bloqueio remoto nas Configurações do monitorado."
                    remoteStatus?.optBoolean("lockAdminActive", false) != true -> "Ative o administrador do Android no monitorado."
                    remoteStatus?.optBoolean("lockSecure", false) != true -> "Configure PIN, padrão ou senha no monitorado."
                    else -> "Digite BLOQUEAR para habilitar a ação."
                }
            }, onFailure = {
                permissionReady = false
                status.text = "Sem confirmação do monitorado"
                feedback.text = it.message ?: "Falha na comunicação."
            })
            setEnabled()
        }
    }
    confirm.addTextChangedListener(object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { setEnabled() }
        override fun afterTextChanged(s: android.text.Editable?) = Unit
    })
    sendButton.setOnClickListener {
        if (confirm.text.toString().trim() != DeviceControlPolicy.LOCK_CONFIRMATION) {
            feedback.text = "Digite exatamente BLOQUEAR (maiúsculas) para confirmar."
        } else {
            confirm.setText("")
            request("lockNow", DeviceControlPolicy.LOCK_CONFIRMATION)
        }
    }
    refreshButton.setOnClickListener { request("status") }
    setEnabled()
    showManagedDialog(dialog)
    request("status")
}
