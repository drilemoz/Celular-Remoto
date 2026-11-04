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
        "Compilação corrigida" to "O compartilhamento do relatório de sessão automática agora define corretamente o tipo de texto; corrigida a falha do build anterior.",
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
    layout.addView(label("A transferência pode ser limitada pelo Wi-Fi ou armazenamento. Atualize os dois aparelhos para a mesma versão antes de copiar.", 12f, muted = true))
    val dialog = themedPopup(this).setView(wrapper)
        .setPositiveButton("Entendi") { _, _ -> SettingsStore.markNewsSeen(this, AppVersion.NAME) }
        .create().apply { setCancelable(false); setCanceledOnTouchOutside(false) }
    showManagedDialog(dialog)
}
