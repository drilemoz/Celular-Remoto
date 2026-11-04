package br.dev.celularremoto.ui

import android.app.AlertDialog
import br.dev.celularremoto.R
import br.dev.celularremoto.core.SettingsStore
import br.dev.celularremoto.transport.AppVersion

/** Exibido somente após a primeira abertura de cada versão instalada. */
internal fun MainActivity.showVersionNewsIfNeeded() {
    if (SettingsStore.seenNewsVersion(this) == AppVersion.NAME) return
    showVersionNews()
}
internal fun MainActivity.showVersionNews() {
    val (wrapper, layout) = dialogWrapper()
    layout.addView(iconSectionHeader(R.drawable.ic_info, "Novidades · ${AppVersion.NAME}", "Bem-vindo à atualização"))
    sectionGap(layout, 10)
    val entries = listOf(
        "Versões diferentes conectam" to "A alpha.112 conversa com a alpha.111 nas operações existentes, usando marcador de protocolo compatível, sem exigir mesmo número do APK.",
        "Atualização mais frequente" to "Padrão de 1 hora, com opções de 30 minutos a 24 horas. O Android poderá atrasar a consulta para economizar energia.",
        "Atualizar aparelho remoto" to "Configurações → Atualizações agora consulta estado remoto e solicita verificação/download no aparelho pareado, após autorização local. Instalação sempre no próprio celular.",
        "Atualizações online" to "Busca novas versões nas Releases do GitHub com o app em segundo plano e opção de baixar o APK automaticamente. Instalar continua exigindo ação local no Android.",
        "Download seguro" to "APK publicado com assinatura estável, hash SHA-256, pacote e versão conferidos antes de aparecer como pronto. Sem novas dependências nem segundo workflow.",
        "Central de permissões" to "Na primeira instalação, confira e solicite os acessos do Android. Também disponível em Configurações, com alertas de pendência no topo.",
        "SMS e acessos especiais" to "SMS requer confirmação separada e pode ser restrito pelo instalador. Acessibilidade e notificações recebidas exigem autorização manual no Android.",
        "Pareamento QR corrigido" to "O protocolo mantém QR celularremoto e porta 37842. Versões 111/112 podem se conectar usando o mesmo protocolo.",
        "GitHub Actions simplificado" to "Um push na branch main inicia um único workflow de APK e Release. Builds anteriores são cancelados quando chega nova alteração.",
        "Agora: Celular Remoto" to "Nova identidade e pacote br.dev.celularremoto. É uma instalação separada: a versão antiga não transfere dados ou chaves automaticamente.",
        "Sessões automáticas" to "Carregamento e descarga são registrados pelo serviço Monitorado, além dos testes manuais Solar, Elétrico, Consumo e Automático.",
        "Previsões conservadoras" to "Autonomia e previsão de carga aparecem somente após variação real suficiente do percentual; dados incertos são identificados.",
        "Alertas local e remoto" to "Meta de carga, bateria baixa e limite de temperatura configuráveis para cada aparelho, com eventos remotos no canal autenticado.",
        "100% confirmado" to "O teste de carga encerra após duas leituras consecutivas de 100%, mesmo se o Android ainda exibir CHARGING."
    )
    entries.forEach { (title, details) ->
        layout.addView(dialogCardRow(title, details, ""))
        sectionGap(layout, 7)
    }
    layout.addView(label("A transferência pode ser limitada pelo Wi-Fi ou armazenamento. Versões anteriores à alpha.111 podem não suportar o novo modo de compatibilidade.", 12f, muted = true))
    val dialog = themedPopup(this).setView(wrapper)
        .setPositiveButton("Entendi") { _, _ -> SettingsStore.markNewsSeen(this, AppVersion.NAME) }
        .create().apply { setCancelable(false); setCanceledOnTouchOutside(false) }
    showManagedDialog(dialog)
}
