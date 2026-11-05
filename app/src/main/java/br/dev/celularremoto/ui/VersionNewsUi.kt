package br.dev.celularremoto.ui

import br.dev.celularremoto.R
import br.dev.celularremoto.core.SettingsStore
import br.dev.celularremoto.transport.AppVersion

/** O modal mostra apenas novidades da versão atual. O histórico longo é opcional. */
internal fun MainActivity.showVersionNewsIfNeeded() {
    if (SettingsStore.seenNewsVersion(this) != AppVersion.NAME) showVersionNews()
}

internal fun MainActivity.showVersionNews() {
    val (wrapper, layout) = dialogWrapper()
    layout.addView(iconSectionHeader(R.drawable.ic_info, "Novidades · ${AppVersion.NAME}", "O que mudou nesta versão"))
    sectionGap(layout, 10)
    val current = listOf(
        "Bateria independente" to "Ative a coleta contínua local sem iniciar compartilhamento remoto, na aba Bateria ou em Ajustes → Bateria local contínua.",
        "Um coletor, dois modos" to "Quando Monitorado e Bateria estão autorizados, ambos usam a mesma agenda; nenhuma segunda conexão ou notificação permanente.",
        "Retomada sem perder histórico" to "Ao parar Monitorado, a coleta local pode continuar. Desligar a coleta local não interrompe o servidor autorizado."
    )
    current.forEach { (title, details) ->
        layout.addView(dialogCardRow(title, details, ""))
        sectionGap(layout, 6)
    }
    val dialog = themedPopup(this).setView(wrapper)
        .setNeutralButton("Histórico completo") { _, _ ->
            SettingsStore.markNewsSeen(this, AppVersion.NAME)
            openSettingsModule()
            openSettingsSection("news")
        }
        .setPositiveButton("Entendi") { _, _ -> SettingsStore.markNewsSeen(this, AppVersion.NAME) }
        .create().apply { setCancelable(false); setCanceledOnTouchOutside(false) }
    showManagedDialog(dialog)
}

internal fun MainActivity.renderVersionHistory(layout: android.widget.LinearLayout) {
    layout.addView(label("Histórico das melhorias anteriores", 14f, strong = true))
    layout.addView(label("As mudanças mais recentes aparecem na primeira abertura de cada versão.", 11.5f, muted = true))
    PREVIOUS_NEWS.forEachIndexed { index, (title, details) ->
        layout.addView(settingsSlimRow(R.drawable.ic_info, title, details, "") {
            showAppNotice(title, details)
        })
        if (index < PREVIOUS_NEWS.lastIndex) compactDivider(layout, 7)
    }
}

private val PREVIOUS_NEWS = listOf(
        "Continuidade e recuperação (alpha.129)" to "Inícios repetidos não recriam agendas; a rede padrão e o registro de continuidade melhoram o diagnóstico.",
        "Conexão estável e WakeLock (alpha.128)" to "Diagnóstico de Doze e locks opcionais de CPU/Wi-Fi, desligados por padrão; liberação ao parar Monitorado.",
        "Preparação remota (alpha.127)" to "Diagnóstico de prontidão local/remota e autorizações individuais; acesso autenticado e permissões locais preservados.",
        "Diagnóstico de alertas (alpha.126)" to "Permissões e canais verificados antes de publicar; registros e notificação de teste em Alertas e proteção.",
        "Notificação local e remota (alpha.125)" to "Percentual, corrente, temperatura, previsão e último contato remoto na notificação do Monitorado, com atualizações limitadas.",
        "Todos os comandos visíveis (alpha.124)" to "Doze comandos sempre disponíveis no Controlar, sem botão de ocultar, com cache visual seguro.",
        "AMOLED e legibilidade (alpha.123)" to "Tema Preto AMOLED, tamanhos de fonte maiores, grades adaptativas e navegação responsiva.",
        "Correção de compilação (alpha.122)" to "Corrigida a altura mínima no histórico de Bateria após o erro do workflow 15.",
        "Configurações compactas (alpha.121)" to "Categorias e permissões reorganizadas, atualizador e diálogos com menos cartões, preservando autorizações locais e instalação por toque.",
        "Bateria compacta (alpha.120)" to "Indicador de percentual, métricas adaptativas e histórico em lista, mantendo diagnósticos e cálculos existentes.",
        "Controlar e Monitorado compactos" to "A alpha.119 trouxe telemetria compacta, painel único de Tela/Câmera/Foto e controles secundários mais densos sem remover confirmações locais.",
        "Design compacto inicial" to "A alpha.118 modernizou Início, navegação, categorias de Ajustes e a organização geral das telas em grades adaptativas.",
        "Estabilidade da conexão" to "Em Controlar ou Configurações, acompanhe latência, tentativas autenticadas, períodos sem medição, falhas e recuperação por aparelho.",
        "Heartbeat e reconexão" to "Nova sonda leve e cifrada, tentativas com espera progressiva e busca apenas em endereços salvos com a mesma chave. Segundo plano pode ser ativado nas configurações.",
        "Correção do build 9" to "A compilação Kotlin passou e 209 dos 210 testes foram aprovados. O teste de versão agora consulta o BuildConfig gerado pelo Gradle, sem exigir o número antigo alpha.112.",
        "Correção do build 8" to "Corrigida a compilação do teste de histórico da bateria. Os recursos de bateria, gráficos, controle remoto e atualizações continuam como antes.",
        "Histórico de bateria com filtros" to "Filtre sessões locais ou do aparelho remoto por tipo (solar, elétrico, consumo, carga/descarga automática) e período de 7, 30, 90 dias ou todo o histórico disponível.",
        "Comparação de testes" to "Compare duas sessões concluídas com taxa em pontos percentuais por hora e cobertura. O app avisa se céu, painel ou condição da tela tornam a comparação inconclusiva.",
        "Gráficos com lacunas" to "Gráficos de percentual, corrente e temperatura mostram eixos, horários e apenas pontos registrados. Intervalos maiores que 90 segundos não recebem linhas inventadas.",
        "Bateria: corrente avançada" to "Corrente instantânea, média Android e média temporal pelo contador, com alerta para sinais contraditórios sem esconder leituras válidas.",
        "Previsão com histórico" to "Previsões de carga/descarga usam tendência recente e histórico comparável quando há dados suficientes; metas de 80%, 90% e 100%.",
        "Sessões e diagnóstico" to "Média líquida da sessão, cobertura de amostras, últimas recargas na tela principal e diagnóstico com valores brutos. Dados remotos continuam autenticados.",
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
