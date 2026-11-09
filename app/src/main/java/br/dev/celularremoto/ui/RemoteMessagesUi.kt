package br.dev.celularremoto.ui

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import android.content.ClipDescription
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import br.dev.celularremoto.R
import br.dev.celularremoto.core.MessagePolicy
import br.dev.celularremoto.core.RecentDevices
import br.dev.celularremoto.core.RemoteEventNoticePolicy
import br.dev.celularremoto.core.RemoteMessageSessionPolicy
import br.dev.celularremoto.core.SensitiveContent
import br.dev.celularremoto.core.SettingsStore
import org.json.JSONArray
import org.json.JSONObject

/** A identidade da sessão inclui a chave atual, mesmo que dois aparelhos reutilizem o IP. */
internal fun MainActivity.currentMessageScope(host: String): String =
    if (host.isBlank()) "" else RecentDevices.key(applicationContext, host)?.let {
        RemoteEventNoticePolicy.pairingScope(host, it)
    }.orEmpty()

/** Central em página própria. O conteúdo fica SOMENTE na RAM durante esta visita. */
internal fun MainActivity.openRemoteMessagesScreen() {
    // Invalidar antes de renderizar: callbacks de uma sessão antiga não podem preencher a nova.
    messagesRequestGeneration++
    messagesHost = SettingsStore.getRemoteHost(this)
    messagesPairingScope = currentMessageScope(messagesHost)
    messagesBaselineMs = lastMessageViewedMs[messagesPairingScope] ?: 0L
    // Reabrir por notificação com a Activity já ativa não herda a data da sessão anterior.
    messagesLatestShownMs = 0L
    messagesData = null
    messagesError = null
    messagesLoading = false
    messagesRefreshPending = false
    messagesTechnicalExpanded = false
    messagesExpandedSender = null
    messagesTab = 0
    mode = 4
    render()
    // Se aberto por uma notificação durante a retomada, onResume fará a consulta.
    // Não iniciar requisição com Activity invisível: o callback seria descartado.
    if (active) loadRemoteMessages()
}

/** Sai da central, invalidando callbacks e removendo os textos do estado da Activity. */
internal fun MainActivity.clearRemoteMessagesScreen() {
    if (RemoteMessageSessionPolicy.matches(messagesHost, messagesPairingScope,
        SettingsStore.getRemoteHost(this), currentMessageScope(messagesHost))) {
        lastMessageViewedMs[messagesPairingScope] = maxOf(
            lastMessageViewedMs[messagesPairingScope] ?: 0L, messagesLatestShownMs
        )
    }
    messagesRequestGeneration++
    messagesData = null
    messagesError = null
    messagesLoading = false
    messagesRefreshPending = false
    messagesHost = ""
    messagesPairingScope = ""
    messagesExpandedSender = null
    messagesLatestShownMs = 0L
}

internal fun MainActivity.closeRemoteMessagesScreen() {
    clearRemoteMessagesScreen()
    mode = 1
    render()
    (content.parent as? android.widget.ScrollView)?.scrollTo(0, 0)
}

/** Atualização manual: consulta o vínculo autenticado antes da leitura de mensagens. */
internal fun MainActivity.loadRemoteMessages() {
    if (mode != 4 || messagesLoading) return
    val host = messagesHost
    val scope = messagesPairingScope
    if (!RemoteMessageSessionPolicy.matches(host, scope, SettingsStore.getRemoteHost(this),
            currentMessageScope(host))) {
        // A conexão ou o pareamento pode ter mudado com a página ainda aberta.
        // Não conservar qualquer texto da identidade anterior na UI.
        messagesRequestGeneration++
        messagesData = null
        messagesLoading = false
        messagesRefreshPending = false
        messagesError = "A sessão mudou. Volte e abra Mensagens novamente."
        render()
        return
    }
    messagesLoading = true
    messagesError = null
    val request = ++messagesRequestGeneration
    render()
    val validPage = {
        mode == 4 && messagesHost == host &&
            RemoteMessageSessionPolicy.matches(host, scope, SettingsStore.getRemoteHost(this),
                currentMessageScope(host)) &&
            request == messagesRequestGeneration && active && !isFinishing && !isDestroyed
    }
    val obtainMessages: () -> Unit = {
        remoteConnection.loadMessages(host) { result ->
            if (!validPage() && active && mode == 4 && messagesHost == host &&
                request == messagesRequestGeneration && currentMessageScope(host) != scope) {
                // A resposta chegou depois da troca da credencial. Nunca deixar a tela
                // presa em carregando nem exibir dados capturados na sessão anterior.
                messagesLoading = false
                messagesData = null
                messagesRefreshPending = false
                messagesError = "Pareamento alterado. Reabra Mensagens."
                render()
            } else if (validPage()) {
                messagesLoading = false
                result.onSuccess { messagesData = it }
                    .onFailure { messagesError = it.message ?: "Consulta indisponível." }
                render()
                // Se outro evento chegou durante a consulta, consultar novamente UMA vez;
                // o booleano agrega vários eventos sem criar uma fila infinita.
                if (messagesRefreshPending) {
                    messagesRefreshPending = false
                    if (messagesAutoRefresh && validPage()) loadRemoteMessages()
                }
            }
        }
    }
    // Após voltar do segundo plano ou uma queda de rede, revalidar a conexão.
    if (remoteConnection.state == "connected" && remoteConnection.cachedHost == host) {
        obtainMessages()
    } else {
        // Cancela polling anterior da página Controle que ainda possa estar em voo.
        if (remoteConnection.state == "loading") remoteConnection.select(remoteConnection.method)
        remoteConnection.fetch(host, stillVisible = validPage) { finished, fresh ->
            if (finished && validPage()) {
                if (fresh != null) obtainMessages()
                else {
                    messagesLoading = false
                    messagesError = remoteConnection.issue?.title ?: "Sem resposta do aparelho."
                    render()
                }
            }
        }
    }
}

/** Não copia mensagens que deveriam ter sido barradas no monitorado. */
internal fun MainActivity.copyRemoteMessage(text: String) {
    if (MessagePolicy.looksLikeAuthenticationSms(text)) {
        Toast.makeText(this, "Conteúdo sensível bloqueado", Toast.LENGTH_SHORT).show()
        return
    }
    val clip = ClipData.newPlainText("Mensagem remota", text)
    if (android.os.Build.VERSION.SDK_INT >= 33) {
        clip.description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
    }
    (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
    Toast.makeText(this, "Mensagem copiada", Toast.LENGTH_SHORT).show()
}

private fun MainActivity.smallMessageAction(icon: Int, description: String, onClick: () -> Unit): View =
    headerIconButton(icon, description, onClick).apply {
        background = shape(fieldColor, 12, true)
        layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
    }

private fun MainActivity.messageTextAction(text: String, onClick: () -> Unit): TextView =
    label(text, 12f, strong = true).apply {
        setTextColor(highlight)
        background = shape(fieldColor, 12, true)
        gravity = Gravity.CENTER
        setPadding(dp(10), 0, dp(10), 0)
        minHeight = dp(44)
        isClickable = true
        isFocusable = true
        contentDescription = text
        setOnClickListener { onClick() }
    }

private fun MainActivity.messageDivider(parent: LinearLayout) {
    parent.addView(View(this).apply { setBackgroundColor(lineColor) }, LinearLayout.LayoutParams(-1, dp(1)))
}

private fun MainActivity.messageRow(parent: LinearLayout, caption: String, body: String,
    isNew: Boolean, reply: (() -> Unit)? = null) {
    if (MessagePolicy.looksLikeAuthenticationSms(body)) return
    val row = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = shape(fieldColor, 15, true)
        setPadding(dp(11), dp(10), dp(9), dp(10))
    }
    val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    texts.addView(label(if (isNew) "● NOVA · $caption" else caption, 10.5f, muted = true).apply {
        setPadding(0, 0, 0, dp(4))
    })
    texts.addView(label(body, 13f).apply {
        maxLines = 4
        ellipsize = android.text.TextUtils.TruncateAt.END
        setPadding(0, 0, dp(7), 0)
    })
    row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
    val actions = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    actions.addView(smallMessageAction(R.drawable.ic_copy, "Copiar mensagem") { copyRemoteMessage(body) })
    if (reply != null) {
        actions.addView(View(this), LinearLayout.LayoutParams(dp(5), 1))
        actions.addView(messageTextAction("Responder", reply))
    }
    row.addView(actions)
    parent.addView(row, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(7) })
}

/** Tela completa, sem AlertDialog nem botões largos para cada mensagem. */
internal fun MainActivity.renderRemoteMessagesPage() {
    val top = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    top.addView(headerIconButton(R.drawable.ic_back, "Voltar ao controle") { closeRemoteMessagesScreen() },
        LinearLayout.LayoutParams(dp(48), dp(48)))
    val words = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(9), 0, 0, 0) }
    words.addView(label("Mensagens", 21f, strong = true))
    words.addView(label("Conversas e notificações do aparelho pareado", 11.5f, muted = true))
    top.addView(words, LinearLayout.LayoutParams(0, -2, 1f))
    val refresh = headerIconButton(R.drawable.ic_refresh, "Atualizar mensagens") { loadRemoteMessages() }
    refresh.isEnabled = !messagesLoading
    refresh.alpha = if (messagesLoading) 0.4f else 1f
    top.addView(refresh, LinearLayout.LayoutParams(dp(48), dp(48)))
    content.addView(top)
    sectionGap(content, 12)
    val info = panel(14).apply {
        background = shape(fieldColor, 19, true)
        val row = LinearLayout(this@renderRemoteMessagesPage).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row.addView(vectorIcon(R.drawable.ic_shield, 23, highlight))
        val texts = LinearLayout(this@renderRemoteMessagesPage).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(11), 0, 0, 0) }
        texts.addView(label(if (messagesLoading) "Consultando aparelho…" else "Consulta protegida", 13f, strong = true))
        texts.addView(label("Dados temporários · códigos sensíveis filtrados", 11f, muted = true))
        row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))
        addView(row)
    }
    content.addView(info)
    sectionGap(content, 8)
    content.addView(messageTextAction(
        if (messagesAutoRefresh) "Atualização por eventos: ligada (tela aberta)" else "Atualização por eventos: desligada"
    ) {
        messagesAutoRefresh = !messagesAutoRefresh
        if (!messagesAutoRefresh) messagesRefreshPending = false
        eventWatcher.reconsider()
        render()
    })
    content.addView(label("Enquanto esta tela estiver aberta, novos eventos autorizados podem atualizar as conversas. Com o app em segundo plano, vale a consulta periódica do Android.", 11f, muted = true))
    sectionGap(content, 12)

    val root = messagesData
    val state = root?.optJSONObject("messagingPermissions")
    val sms = eligibleSms(root)
    val notifications = eligibleNotifications(root)
    val tabs = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        background = shape(fieldColor, 14, true)
        setPadding(dp(3), dp(3), dp(3), dp(3))
    }
    listOf("SMS (${sms.size})", "Notificações (${notifications.size})").forEachIndexed { index, text ->
        val selected = index == messagesTab
        tabs.addView(TextView(this).apply {
            this.text = text
            textSize = 13.5f
            setTextColor(if (selected) highlightText else ink)
            gravity = Gravity.CENTER
            minHeight = dp(46)
            background = shape(if (selected) highlight else fieldColor, 12)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                messagesTab = index
                messagesTechnicalExpanded = false
                render()
                (content.parent as? android.widget.ScrollView)?.scrollTo(0, 0)
            }
        }, LinearLayout.LayoutParams(0, -2, 1f))
    }
    content.addView(tabs)
    sectionGap(content, 11)
    content.addView(label(when {
        messagesLoading -> "Consultando mensagens autorizadas…"
        root == null -> "Sem consulta recente · toque no ícone Atualizar"
        else -> "Consulta concluída · novas mensagens dependem de nova consulta"
    }, 11f, muted = true))
    if (messagesError != null) {
        sectionGap(content, 9)
        content.addView(panel(12).apply {
            addView(label("Não foi possível atualizar", 13.5f, strong = true))
            addView(label(messagesError.orEmpty(), 12f, muted = true))
            addView(messageTextAction("Tentar novamente") { loadRemoteMessages() })
        })
    }
    if (root == null) {
        sectionGap(content, 18)
        if (!messagesLoading && messagesError == null) content.addView(label("Toque em Atualizar para consultar o aparelho.", 13f, muted = true))
        return
    }
    sectionGap(content, 14)
    if (messagesTab == 0) renderSmsMessages(root, state, sms)
    else renderAppNotifications(state, notifications)
}

private fun eligibleSms(root: JSONObject?): List<JSONObject> {
    if (root == null) return emptyList()
    val list = mutableListOf<JSONObject>()
    val sms = root.optJSONArray("smsInbox")
    for (i in 0 until (sms?.length() ?: 0)) {
        val item = sms?.optJSONObject(i) ?: continue
        if (!MessagePolicy.looksLikeAuthenticationSms(item.optString("text"))) list.add(item)
    }
    val live = root.optJSONArray("smsLive")
    for (i in 0 until (live?.length() ?: 0)) {
        val item = live?.optJSONObject(i) ?: continue
        if (MessagePolicy.looksLikeAuthenticationSms(item.optString("text"))) continue
        if (list.none { it.optString("address") == item.optString("address") &&
                it.optString("text") == item.optString("text") &&
                kotlin.math.abs(it.optLong("whenMs") - item.optLong("whenMs")) < 30_000L }) list.add(item)
    }
    return list.sortedByDescending { it.optLong("whenMs") }
}

private fun eligibleNotifications(root: JSONObject?): List<JSONObject> {
    val events = root?.optJSONArray("events") ?: return emptyList()
    val list = mutableListOf<JSONObject>()
    for (i in events.length() - 1 downTo 0) {
        val item = events.optJSONObject(i) ?: continue
        if (item.optString("type") != "notificacao") continue
        if (SensitiveContent.isSensitive(item.optString("title"), item.optString("preview"))) continue
        if (SensitiveContent.isBlockedFinancialApp(item.optString("app"), item.optString("app"))) continue
        list.add(item)
    }
    return list.sortedByDescending { it.optLong("whenMs") }
}

private fun MainActivity.renderSmsMessages(root: JSONObject, state: JSONObject?, items: List<JSONObject>) {
    content.addView(iconSectionHeader(R.drawable.ic_message, "Conversas SMS", "Recebidas e enviadas · atualizar para consultar"))
    sectionGap(content, 12)
    if (items.isEmpty()) {
        val hint = when {
            state?.optBoolean("smsRead") == true || state?.optBoolean("smsRealtime") == true -> "Nenhum SMS permitido pelo filtro nesta consulta."
            else -> "Ative a leitura de SMS e autorize a permissão no aparelho monitorado."
        }
        content.addView(panel(16).apply {
            background = shape(fieldColor, 19, true)
            addView(iconSectionHeader(R.drawable.ic_message, "Nenhum SMS disponível", hint))
        })
    }
    val subscriptions = root.optJSONArray("smsSubscriptions")
    val bySender = items.groupBy { it.optString("address").ifBlank { "Remetente desconhecido" } }
        .entries.sortedByDescending { group -> group.value.maxOf { it.optLong("whenMs") } }
    bySender.forEach { (address, messages) ->
        val card = panel(13)
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val heading = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        heading.addView(label(address, 14f, strong = true))
        val expanded = messagesExpandedSender == address
        heading.addView(label("${messages.size} mensagem(ns) · ${formatDate(messages.maxOf { it.optLong("whenMs") })}",
            11f, muted = true))
        head.addView(heading, LinearLayout.LayoutParams(0, -2, 1f))
        if (state?.optBoolean("smsReply") == true && MessagePolicy.validReplyAddress(address)) {
            head.addView(messageTextAction("Responder SMS") {
                showRemoteMessageEditor("smsReply", address, "SMS para $address", subscriptions)
            })
        }
        head.addView(label(if (expanded) "⌃" else "⌄", 19f, muted = true))
        head.isClickable = true
        head.isFocusable = true
        head.contentDescription = "${if (expanded) "Recolher" else "Expandir"} conversa com $address"
        head.setOnClickListener {
            messagesExpandedSender = if (expanded) null else address
            render()
        }
        card.addView(head)
        sectionGap(card, 6)
        messageDivider(card)
        messages.forEach { messagesLatestShownMs = maxOf(messagesLatestShownMs, it.optLong("whenMs")) }
        if (expanded) {
            messages.sortedByDescending { it.optLong("whenMs") }.forEach { item ->
                val at = item.optLong("whenMs")
                val direction = if (item.optString("direction") == "enviado") "Enviado" else "Recebido"
                messageRow(card, "$direction · ${formatDate(at)}", item.optString("text"), at > messagesBaselineMs)
                messageDivider(card)
            }
        } else {
            val recent = messages.maxByOrNull { it.optLong("whenMs") }
            card.addView(label(recent?.optString("text").orEmpty(), 12.5f, muted = true).apply {
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(0, dp(7), 0, dp(3))
            })
            card.addView(label("Toque no cabeçalho para ver a conversa", 10.5f, muted = true))
        }
        content.addView(card)
        sectionGap(content, 9)
    }
    val status = root.optJSONArray("smsStatus")
    val mms = root.optJSONArray("mmsMetadata")
    if ((status?.length() ?: 0) > 0 || (mms?.length() ?: 0) > 0) {
        sectionGap(content, 8)
        content.addView(dialogCardRow("Detalhes de SMS e MMS", "Status de envio e assuntos de MMS",
            if (messagesTechnicalExpanded) "⌃" else "⌄", onClick = {
                messagesTechnicalExpanded = !messagesTechnicalExpanded
                render()
            }))
        if (messagesTechnicalExpanded) {
            sectionGap(content, 8)
            val technical = panel(12)
            for (i in (status?.length() ?: 0) - 1 downTo 0) {
                val event = status?.optJSONObject(i) ?: continue
                technical.addView(label("${event.optString("address")} · ${event.optString("state")} · ${formatDate(event.optLong("whenMs"))}", 11.5f))
            }
            for (i in 0 until (mms?.length() ?: 0)) {
                val entry = mms?.optJSONObject(i) ?: continue
                if (!SensitiveContent.isSensitive("", entry.optString("subject")))
                    technical.addView(label("MMS · ${entry.optString("subject")} · ${formatDate(entry.optLong("whenMs"))}", 11.5f))
            }
            content.addView(technical)
        }
    }
}

private fun MainActivity.renderAppNotifications(state: JSONObject?, items: List<JSONObject>) {
    content.addView(iconSectionHeader(R.drawable.ic_bell, "Notificações", if (state?.optBoolean("allNotifications") == true)
        "Todos os aplicativos elegíveis" else "Aplicativos autorizados no monitorado"))
    sectionGap(content, 12)
    if (items.isEmpty()) content.addView(panel(16).apply {
        background = shape(fieldColor, 19, true)
        addView(iconSectionHeader(R.drawable.ic_bell, "Sem notificações", "Nenhum evento autorizado nesta consulta"))
    })
    val byConversation = items.groupBy { "${it.optString("app")}|${it.optString("title")}" }
        .entries.sortedByDescending { group -> group.value.maxOf { it.optLong("whenMs") } }
    byConversation.forEach { (_, notifications) ->
        val mostRecent = notifications.first()
        val card = panel(13)
        val pkg = mostRecent.optString("app")
        val title = mostRecent.optString("title").ifBlank { "Sem título" }
        val app = friendlyNotificationApp(pkg)
        val heading = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val headingText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        headingText.addView(label(title, 14f, strong = true))
        headingText.addView(label("$app · ${notifications.size} notificação(ões)", 11f, muted = true))
        heading.addView(headingText, LinearLayout.LayoutParams(0, -2, 1f))
        heading.addView(visualTag("ALERTA", SettingsBadgeKind.NEUTRAL))
        card.addView(heading)
        sectionGap(card, 6)
        messageDivider(card)
        notifications.forEach { item ->
            val at = item.optLong("whenMs")
            messagesLatestShownMs = maxOf(messagesLatestShownMs, at)
            val token = item.optString("replyToken")
            val reply = if (state?.optBoolean("notificationReply") == true && token.isNotBlank()) {
                { showRemoteMessageEditor("notificationReply", token, "Responder $title ($app)") }
            } else null
            messageRow(card, formatDate(at), item.optString("preview"), at > messagesBaselineMs, reply)
            messageDivider(card)
        }
        content.addView(card)
        sectionGap(content, 9)
    }
}

private fun friendlyNotificationApp(pkg: String): String = when (pkg) {
    "com.whatsapp", "com.whatsapp.w4b" -> "WhatsApp"
    "org.telegram.messenger" -> "Telegram"
    "com.instagram.android" -> "Instagram"
    "com.facebook.orca" -> "Messenger"
    "com.google.android.googlequicksearchbox" -> "Google"
    "com.google.android.apps.messaging" -> "Mensagens"
    else -> pkg.substringAfterLast('.').ifBlank { "Aplicativo" }.take(36)
}

internal fun MainActivity.showRemoteMessageEditor(operation: String, recipient: String,
    recipientLabel: String, subscriptions: JSONArray? = null) {
    val host = SettingsStore.getRemoteHost(this)
    if (remoteConnection.state != "connected" || remoteConnection.cachedHost != host) {
        showAppNotice("Conexão não confirmada", "Atualize a conexão antes de responder mensagens.")
        return
    }
    val (wrapper, layout) = visualDialogContainer()
    layout.addView(visualHeader(R.drawable.ic_message, "Responder mensagem", recipientLabel))
    sectionGap(layout, 11)
    layout.addView(label("A mensagem será enviada pelo celular monitorado. O envio não garante entrega.", 12f, muted = true))
    sectionGap(layout, 11)
    val options = mutableListOf(-1 to "Chip padrão")
    if (operation == "smsReply" && subscriptions != null) {
        for (i in 0 until subscriptions.length()) {
            val entry = subscriptions.optJSONObject(i) ?: continue
            val id = entry.optInt("id", -1)
            if (id != -1 && options.none { it.first == id }) options.add(id to entry.optString("label"))
        }
    }
    val selector = if (operation == "smsReply" && options.size > 1) Spinner(this).apply {
        adapter = ArrayAdapter(this@showRemoteMessageEditor, android.R.layout.simple_spinner_dropdown_item,
            options.map { it.second })
        layout.addView(label("Chip que enviará o SMS", 11.5f, muted = true))
        layout.addView(this)
        sectionGap(layout, 9)
    } else null
    val input = EditText(this).apply {
        hint = "Digite a resposta"
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        minLines = 3; maxLines = 6
        setTextColor(ink); setHintTextColor(dim)
        background = shape(fieldColor, 12, true)
        setPadding(dp(12), dp(10), dp(12), dp(10))
        filters = arrayOf(android.text.InputFilter.LengthFilter(500))
    }
    layout.addView(input, LinearLayout.LayoutParams(-1, -2))
    val dialog = themedPopup(this)
        .setView(wrapper).setNegativeButton("Cancelar", null).setPositiveButton("Revisar envio", null).create()
    showManagedDialog(dialog)
    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
        val text = input.text.toString().trim()
        if (text.isBlank()) { input.error = "Escreva uma mensagem."; return@setOnClickListener }
        if (SensitiveContent.isSensitive("", text)) {
            input.error = "Conteúdo sensível bloqueado."; return@setOnClickListener
        }
        val selected = options.getOrElse(selector?.selectedItemPosition ?: 0) { options.first() }
        dialog.dismiss()
        showAppConfirm("Confirmar envio remoto?", "$recipientLabel\n${if (operation == "smsReply") selected.second + "\n" else ""}\n$text\n\nAção executada no aparelho monitorado.",
            R.drawable.ic_message, "Enviar") {
            remoteConnection.sendMessage(host, operation, recipient, text, { result ->
                result.onSuccess { showAppNotice("Solicitação realizada", it) }
                    .onFailure { showAppNotice("Falha ao responder", it.message ?: "Falha na comunicação.") }
            }, selected.first)
        }
    }
}
