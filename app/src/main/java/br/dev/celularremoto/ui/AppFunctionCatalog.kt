package br.dev.celularremoto.ui

/** Caminhos do produto. Entradas dependentes de estado abrem a seção mesmo quando a opção ainda não está visível. */
internal object AppFunctionCatalog {
    val entries: List<AppFunction> by lazy {
        val result = buildList {
            fun addEntry(id: String, area: FunctionArea, title: String, path: String, aliases: String,
                destination: FunctionDestination) {
                add(AppFunction(id, area, title, path, aliases, destination))
            }
            fun shortcut(target: SearchShortcut, area: FunctionArea, title: String, path: String, aliases: String) =
                addEntry("shortcut-${target.name}", area, title, path, aliases, FunctionDestination.Shortcut(target))
            fun tool(tool: ControllerTool, title: String, aliases: String, suffix: String = "", path: String = "Funcionalidades") =
                addEntry("tool-${tool.name}$suffix", FunctionArea.CONTROL, title, path, aliases, FunctionDestination.Controller(tool))
            fun battery(id: String, title: String, page: BatterySearchPage, aliases: String,
                focus: String? = null, remote: Boolean? = null) =
                addEntry("battery-$id", FunctionArea.BATTERY, title,
                    when (page) {
                        BatterySearchPage.TESTS -> "Testes"
                        BatterySearchPage.HISTORY -> "Histórico"
                        else -> "Resumo"
                    }, aliases, FunctionDestination.Battery(page, focus, remote))
            fun setting(section: String, id: String, category: String, title: String, aliases: String,
                vararg anchors: String) = addEntry("setting-$section-$id", FunctionArea.SETTINGS, title, category,
                aliases, FunctionDestination.Settings(section, listOf(title) + anchors))
            fun monitor(id: String, title: String, path: String, aliases: String, vararg anchors: String) =
                addEntry("monitor-$id", FunctionArea.MONITOR, title, path, aliases,
                    FunctionDestination.MonitorRow(anchors.toList()))

            shortcut(SearchShortcut.PAIR, FunctionArea.CONTROL, "Conectar ou parear aparelho", "Aparelhos",
                "adicionar celular telefone QR Code escanear câmera manual endereço IP chave trocar selecionar aparelhos salvos esquecer")
            shortcut(SearchShortcut.CALLS, FunctionArea.CONTROL, "Histórico de chamadas", "Mensagens",
                "registro chamadas ligações ligação perdidas recebidas efetuadas telefonia telefone")
            tool(ControllerTool.MESSAGES, "Mensagens e SMS", "caixa entrada sms conversas responder mensagens recebidas notificações")
            battery("solar", "Teste solar", BatterySearchPage.TESTS, "painel placa sol potência watts céu testar carregamento solar", "Teste Solar")
            battery("history", "Histórico da bateria", BatterySearchPage.HISTORY, "sessões registros relatórios gráficos exportar dados cargas descargas")
            shortcut(SearchShortcut.MESSAGE_NOTICES, FunctionArea.CONTROL, "Avisos em tempo real", "Mensagens",
                "push sms chamada ligação notificações tela apagada receber aviso controlador")
            tool(ControllerTool.LOCATION, "Localização do aparelho", "gps mapa localizar endereço coordenadas posição encontrar celular")
            tool(ControllerTool.FILES, "Arquivos remotos", "baixar download transferir copiar pasta downloads upload enviar navegar ordenar renomear excluir arquivos")
            tool(ControllerTool.SCREEN, "Ver tela do aparelho", "espelhar espelhamento compartilhar ver vídeo display remoto áudio toque controle tela cheia")
            tool(ControllerTool.CAMERAS, "Câmeras ao vivo", "câmera câmera remota frontal traseira lente vídeo transmissão prévia")
            tool(ControllerTool.PHOTO, "Foto remota", "fotografia tirar foto capturar salvar imagem jpg jpeg")
            tool(ControllerTool.FILES, "Histórico de transferências", "consultar downloads envios arquivo duração progresso", "-history", "Arquivos remotos")
            tool(ControllerTool.FILES, "Downloads interrompidos", "retomar parciais recuperação pausar cancelar download transferência", "-partial", "Arquivos remotos")
            tool(ControllerTool.FILES, "Enviar arquivos e criar pasta", "upload selecionar arquivos lotes nova pasta", "-upload", "Arquivos remotos")
            tool(ControllerTool.FILES, "Mover, copiar ou excluir arquivos", "gerenciar lixeira excluir apagar renomear organizar pastas ordenar grade lista", "-manage", "Arquivos remotos")
            tool(ControllerTool.CAMERAS, "Trocar câmera ou salvar quadro", "mudar lente câmera frente trás fotograma zoom", "-controls", "Câmeras ao vivo")
            tool(ControllerTool.SCREEN, "Tela cheia e controles de exibição", "fullscreen girar paisagem zoom qualidade gesto áudio", "-controls", "Ver tela do aparelho")
            tool(ControllerTool.LOCATION, "Acompanhar mapa e copiar GPS", "rastreamento acompanhar centralizar mapa mapa externo abrir automático coordenadas", "-controls", "Localização do aparelho")

            tool(ControllerTool.ALERTS, "Alertas do aparelho", "notificações eventos aviso aplicativos apps whatsapp")
            tool(ControllerTool.APPS, "Aplicativos do aparelho", "apps instalados abrir aplicativo app launch")
            tool(ControllerTool.SIREN, "Sirene remota", "som tocar alarme alerta sonoro parar encontrar telefone")
            tool(ControllerTool.TORCH, "Lanterna remota", "flash luz ligar desligar localizar")
            tool(ControllerTool.LOCK, "Bloquear aparelho remoto", "bloqueio trava travar senha pin tela")
            tool(ControllerTool.POWER, "Energia e manutenção", "acender tela acordar wake reiniciar reboot desligamento adb")
            tool(ControllerTool.POWER, "Acender tela remota", "acordar wake screen ligar tela", "-wake", "Energia e manutenção")
            tool(ControllerTool.POWER, "Reiniciar aparelho pelo ADB", "reboot restart reinicializar", "-restart", "Energia e manutenção")
            shortcut(SearchShortcut.CONTROL, FunctionArea.CONTROL, "Painel do aparelho remoto", "Controlar",
                "status conexão conectar desconectar atualizar resposta internet rede wi-fi celular remoto")
            shortcut(SearchShortcut.DEVICE_DETAILS, FunctionArea.CONTROL, "Detalhes do aparelho remoto", "Painel",
                "informações hardware versão android armazenamento memória ram processador cpu sensores rede wifi termometria temperatura bateria diagnóstico copiar relatório")
            shortcut(SearchShortcut.RENAME_DEVICE, FunctionArea.CONTROL, "Renomear aparelho", "Painel",
                "nome apelido editar dispositivo salvo")
            shortcut(SearchShortcut.CONNECTION_DIAGNOSTICS, FunctionArea.CONTROL, "Diagnóstico da conexão", "Painel",
                "rede latência ping atraso indisponível offline queda reconexão")
            shortcut(SearchShortcut.ADB, FunctionArea.SETTINGS, "Modo avançado ADB", "Segurança e permissões",
                "depuração sem fio depurar usb pareamento porta manutenção gestor de pacotes reiniciar")

            shortcut(SearchShortcut.MONITOR, FunctionArea.MONITOR, "Monitoramento local", "Este aparelho",
                "aparelho monitorado hospedar servidor acompanhar estado iniciar desativar ativar")
            shortcut(SearchShortcut.SHOW_MONITOR_INFO, FunctionArea.MONITOR, "IP e chave do monitorado", "Este aparelho",
                "mostrar info credencial pareamento ip endereço senha chave qr copiar")
            shortcut(SearchShortcut.GENERATE_QR, FunctionArea.MONITOR, "Gerar QR Code", "Este aparelho",
                "mostrar código qrcode qr parear credencial conexão escanear")
            shortcut(SearchShortcut.PREPARE_PHOTO, FunctionArea.MONITOR, "Preparar fotografia remota", "Captura",
                "ativar foto armar desarmar jpeg câmera sessão capturar permissão")
            monitor("start", "Ativar, retomar ou interromper monitoramento", "Este aparelho",
                "ligar desligar pausar parar servidor autorização serviço local", "Ativar", "Retomar", "Interromper", "Monitoramento local")
            monitor("screen", "Compartilhar tela do monitorado", "Captura e compartilhamento",
                "espelhar transmissão iniciar parar captura gravar tela media projection", "Tela", "Captura e compartilhamento", "Monitoramento local")
            monitor("camera", "Compartilhar câmera do monitorado", "Captura e compartilhamento",
                "câmera ao vivo prévia iniciar parar lente frontal traseira", "Câmera", "Captura e compartilhamento", "Monitoramento local")
            monitor("access", "Acessos autenticados ao monitorado", "Conexões",
                "controladores conectados histórico últimas consultas revisar permissões revogar acesso", "Acessos autenticados", "Monitoramento local")

            battery("summary", "Resumo da bateria", BatterySearchPage.SUMMARY, "percentual carga bateria nível consumo corrente potência temperatura voltagem autonomia previsão")
            battery("local", "Bateria deste aparelho", BatterySearchPage.SUMMARY, "bateria local percentual estado nível carga", remote = false)
            battery("remote", "Bateria do aparelho remoto", BatterySearchPage.SUMMARY, "bateria remota monitorado atualização leitura consulta nível remoto", remote = true)
            battery("tests", "Testes de energia e consumo", BatterySearchPage.TESTS, "medição testar carga consumo painel elétrica solar")
            battery("electric", "Teste elétrico", BatterySearchPage.TESTS, "testar carregador tomada usb potência carga elétrica", "Teste Elétrico")
            battery("drain", "Teste de consumo", BatterySearchPage.TESTS, "medir descarga descarregar bateria gasto autonomia", "Teste de Consumo")
            battery("auto", "Carga automática", BatterySearchPage.TESTS, "teste automático desligar ao cem porcentagem", "Carga Automática")
            battery("active", "Teste em andamento", BatterySearchPage.TESTS, "finalizar terminar parar teste ativo sessão relatório", "Finalizar teste")
            battery("compare", "Comparar sessões de bateria", BatterySearchPage.HISTORY, "comparação comparar testes desempenho", "Comparar duas sessões registradas")
            battery("reports", "Relatórios e gráficos de bateria", BatterySearchPage.HISTORY, "relatório gráfico nível corrente temperatura compartilhar copiar")
            battery("filter", "Filtros do histórico de bateria", BatterySearchPage.HISTORY, "tipo período 7 30 90 dias pesquisar todos solar elétrico consumo")
            battery("diagnostic", "Diagnóstico elétrico", BatterySearchPage.DIAGNOSTIC, "corrente média valores brutos potência temperatura ma volts")
            battery("goals", "Metas e alertas da bateria", BatterySearchPage.GOALS, "percentual meta carga baixa temperatura limite alerta")
            battery("session", "Sessão automática da bateria", BatterySearchPage.SESSION, "ver sessão atual autonomia previsão últimos relatórios")

            // A entrada de cada categoria é a mesma que o índice já oferece em Ajustes.
            settingGroups.forEachIndexed { groupIndex, group ->
                group.entries.forEachIndexed { entryIndex, item ->
                    if (item.section != "adb") addEntry("category-$groupIndex-$entryIndex", FunctionArea.SETTINGS,
                        item.title, group.title.lowercase(java.util.Locale.ROOT), item.hint, FunctionDestination.Settings(item.section))
                }
            }
            PermissionCenter.entries().forEach { access ->
                setting("permissions", access.id, "Permissões do Android", access.label, "permissão ${access.reason} ${access.guidance}")
            }
            setting("notifications", "listener", "Notificações", "Acesso às notificações", "leitor acesso listener conceder android")
            setting("notifications", "diagnostic", "Notificações", "Diagnóstico da captura", "listener serviço notificações conexão")
            setting("notifications", "scope", "Notificações", "Todas as notificações elegíveis", "todos apps autorizar lista seleção")
            setting("notifications", "allowed", "Notificações", "Aplicativos autorizados", "allowlist apps selecionar notificação")
            setting("notifications", "reply", "Notificações", "Responder notificações", "ação resposta remota whatsapp")
            setting("notifications", "master", "Notificações", "Avisos do aparelho remoto", "ativar notificações controlador")
            setting("notifications", "live", "Notificações", "Avisos em tempo real", "push tela apagada serviço controle")
            setting("notifications", "calls", "Notificações", "Novas chamadas", "avisos chamadas ligação perdidas push", "Avisos do aparelho remoto")
            setting("notifications", "sms", "Notificações", "Novos SMS", "avisos sms mensagens chegada", "Avisos do aparelho remoto")
            setting("notifications", "wa", "Notificações", "WhatsApp e WhatsApp Business", "watsap zap whatsapp avisos mensagem", "Avisos do aparelho remoto")
            setting("notifications", "other", "Notificações", "Outros aplicativos", "avisos outros apps", "Avisos do aparelho remoto")

            setting("sms", "share_calls", "SMS e telefonia", "Compartilhar registro de chamadas", "permissão android read_call_log permitir chamadas ligações histórico")
            setting("sms", "share_sms", "SMS e telefonia", "Ler caixa de SMS", "mensagem recebida compartilhar histórico SMS")
            setting("sms", "receive_sms", "SMS e telefonia", "Receber SMS em tempo real", "confirmar permitir compartilhar mensagens avisos sms")
            setting("sms", "send_sms", "SMS e telefonia", "Responder SMS", "enviar permitir texto sms resposta")
            setting("sms", "sim", "SMS e telefonia", "Escolher chip para SMS", "sim 1 sim 2 dual sim operadora")
            setting("sms", "number", "SMS e telefonia", "Número do chip", "linha telefone celular autorizar sim")

            setting("screen", "share", "Tela e controle remoto", "Compartilhamento de tela", "sessão de captura gravar espelhar tela")
            setting("screen", "touch", "Tela e controle remoto", "Controle por toque", "acessibilidade toque gesto deslizar remoto")
            setting("screen", "wake", "Tela e controle remoto", "Permitir acender tela remotamente", "wake acordar ligar display autorização")
            setting("screen", "auto_wake", "Tela e controle remoto", "Acender automaticamente ao abrir Ver tela", "auto acordar tela remota")
            setting("screen", "audio", "Tela e controle remoto", "Capturar áudio interno", "som mídia transmissão microfone áudio")

            setting("lock", "admin", "Bloqueio remoto", "Administrador do Android", "device admin permitir bloqueio")
            setting("lock", "pin", "Bloqueio remoto", "Bloqueio seguro do Android", "senha pin padrão tela")
            setting("lock", "allow", "Bloqueio remoto", "Permitir bloqueio remoto", "autorizar bloqueio travar tela")

            setting("physical", "sound", "Som e lanterna", "Permitir alerta sonoro remoto", "sirene alarme permissão tocar")
            setting("physical", "light", "Som e lanterna", "Permitir lanterna remota", "luz flash permissão")
            setting("physical", "camera", "Câmera e fotografia", "Permissão Câmera do Android", "autorizações captura câmera foto")

            setting("service", "resume", "Serviço e recuperação", "Verificar / retomar agora", "revisar recuperar serviço servidor iniciar")
            setting("service", "boot", "Serviço e recuperação", "Iniciar depois de reiniciar o celular", "boot ligar iniciar automático autostart")
            setting("service", "schedule", "Serviço e recuperação", "Recuperação agendada (15 min)", "agenda reiniciar serviço 15 minutos")
            setting("service", "android_battery", "Serviço e recuperação", "Ajustar bateria no Android", "limite bateria android otimização")

            setting("power", "battery", "Conexão estável", "Otimização de bateria do Android", "doze economia xiaomi segundo plano")
            setting("power", "vendor", "Conexão estável", "Inicialização automática / Xiaomi", "redmi miui hyperos serviço tela apagada")
            setting("power", "cpu", "Conexão estável", "WakeLock de CPU", "cpu ativa manter acordado wakelock", "Teste de 30 minutos", "Enquanto Monitorado estiver ativo")
            setting("power", "wifi", "Conexão estável", "Manter rádio Wi-Fi ativo (opcional)", "wifi lock wakelock roteador")
            setting("power", "log", "Conexão estável", "Ver histórico de conexão", "diagnóstico quedas registros reconexão")
            setting("connection", "foreground", "Estabilidade da conexão", "Diagnóstico com o app aberto", "medir latência rtt falhas monitorar")
            setting("connection", "background", "Estabilidade da conexão", "Verificar em segundo plano", "controle de rede polling reconexão")
            setting("connection", "alerts", "Estabilidade da conexão", "Avisos de queda e recuperação", "rede caiu voltou notificar queda")

            setting("security", "show_key", "Segurança", "Mostrar/copiar chave", "ip qr credencial código senha pareamento")
            setting("security", "rotate", "Segurança", "Trocar chave e revogar acessos", "revogar dispositivo desconectar segurança rotacionar")
            setting("diagnostics", "wifi", "Diagnóstico", "Nome e sinal da rede Wi-Fi", "ssid rssi dbm wifi")
            setting("diagnostics", "usage", "Diagnóstico", "Aplicativo visível", "estatísticas acesso ao uso app atual")
            setting("files", "folder", "Arquivos e recuperação", "Pasta padrão de transferências", "downloads salvar caminho arquivos root padrão")
            setting("files", "security", "Arquivos e recuperação", "Segurança da pasta", "permissões compartilhadas arquivos android")
            setting("apps", "list", "Aplicativos instalados", "Permitir consulta de aplicativos", "listar compartilhar apps programas")
            setting("apps", "launch", "Aplicativos instalados", "Permitir abrir aplicativos", "executar launch programa remoto")

            setting("health", "local", "Alertas e proteção", "Alertas Android neste aparelho", "alerta bateria temperatura notificação local")
            setting("health", "heat", "Alertas e proteção", "Proteger contra aquecimento severo", "superaquecimento proteção térmica thermal")
            setting("health", "remote", "Alertas e proteção", "Receber alertas de aparelho remoto", "temperatura bateria saúde alertas segundo plano")
            setting("health", "test", "Alertas e proteção", "Testar notificações do Android", "teste canal de notificação permissões")
            setting("battery_local", "collect", "Bateria local contínua", "Coleta contínua local de bateria", "medir em segundo plano ativar pausar tela desligada",
                "Ativar coleta local", "Desativar coleta local", "Ver medições e histórico")
            setting("battery_remote", "collect", "Bateria remota contínua", "Acompanhamento contínuo da bateria remota", "ligar desativar segundo plano polling coleta remoto",
                "Ativar acompanhamento", "Desativar acompanhamento", "Última telemetria")
            setting("location", "recover", "Localização", "Recuperação automática por GPS", "achar celular localização em segundo plano rastrear mapa gps", "Sessão")
            setting("location", "profile", "Localização", "Perfil de localização", "precisão alta economia gps intervalo distância")
            setting("location", "android", "Localização", "Abrir localização do Android", "permissão gps permitir o tempo todo", "Permitir localização o tempo todo no Android")

            setting("updates", "check", "Atualizações", "Verificar agora", "atualizar buscar versão nova github release")
            setting("updates", "automatic", "Atualizações", "Verificação automática", "abrir buscar atualização versão segundo plano intervalo")
            setting("updates", "download", "Atualizações", "Download automático", "baixar apk automaticamente atualizar transferir")
            setting("updates", "install", "Atualizações", "Instalar atualização", "apk instalador origem desconhecida confirmar instalação nova versão", "Instalar APK", "Baixar APK", "Verificar agora")
            setting("updates", "requests", "Atualizações", "Aceitar pedidos remotos", "autorizar atualização remota consultar baixar pelo controlador")
            setting("updates", "poll", "Atualizações", "Acompanhar remoto por 3 minutos", "progresso atualização outro celular monitorado")
            setting("updates", "interval", "Atualizações", "Intervalo em segundo plano", "agenda frequência github")
            setting("preparation", "installer", "Preparação do monitorado", "Permissão do instalador Android", "apk instalar fonte origem apps desconhecidos")
            setting("preparation", "readiness", "Preparação do monitorado", "Prontidão dos comandos", "permissões necessário preparo preparar atualização foto telemetria")
            setting("appearance", "theme", "Aparência", "Tema claro, escuro e AMOLED", "tema dark night modo noturno oled preto automático sistema", "Preto AMOLED", "Escuro", "Claro", "Seguir o sistema")
            setting("appearance", "amoled", "Aparência", "Preto AMOLED", "tema fundo preto oled noturno")
            setting("appearance", "dark", "Aparência", "Tema escuro", "dark mode modo noturno grafite", "Escuro")
            setting("appearance", "light", "Aparência", "Tema claro", "fundo branco light mode", "Claro")
            setting("appearance", "system", "Aparência", "Seguir o sistema", "tema automático android")
            setting("updates", "wifi", "Atualizações", "Somente Wi-Fi não tarifado", "baixar apk só por wifi rede dados móveis")
            setting("updates", "battery", "Atualizações", "Economia de bateria", "adiar download atualização bateria baixa")
            setting("connection", "reconnect", "Estabilidade da conexão", "Verificar agora e tentar reconectar", "atualizar sondar dispositivo celular conectar")
            setting("connection", "clear", "Estabilidade da conexão", "Limpar histórico deste aparelho", "apagar histórico diagnóstico conexão")
            setting("battery_remote", "telemetry", "Bateria remota contínua", "Última telemetria", "dados último percentual remoto medição")
            setting("preparation", "update_remote", "Preparação do monitorado", "Aceitar verificação/download remotos", "autorizar baixar apk remoto")

            setting("news", "today", "Novidades", "Ver novidades da versão atual", "changelog mudanças notas lançamento")
            setting("about", "dev", "Sobre", "Site do desenvolvedor", "iguatama site informações ajuda privacidade")
            setting("about", "pix", "Sobre", "Copiar chave Pix", "apoio doação contribuição pix")
        }
        require(result.map { it.id }.distinct().size == result.size)
        result
    }

    val search by lazy { AppFunctionSearch(entries) }
}
