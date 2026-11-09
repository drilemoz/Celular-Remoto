# Celular Remoto v0.1.0-alpha.142 — correção de compilação de testes

- Próxima versão alpha.143/versionCode 143. Manter Gradle, AppVersion, README, CHANGELOG, docs/RELEASE_NOTES, arquitetura, AGENTS, Novidades e validação sincronizados.
- O workflow `Gerar APK e Release 32` compilou o código principal da alpha.141, mas falhou em três comparações `Double?` vs `Double` nos testes `BatteryPresentationMathTest.kt`, corrigidas agora com `requireNotNull()`.
- Lógica de leitura e apresentação de corrente permanece a da alpha.141. Não forçar sinal em dados persistidos, nem remover diagnósticos da leitura bruta.
- Testes locais não comprovam build Android completo da alpha.142, que deverá ser verificado no GitHub Actions e nos aparelhos reais.

# Celular Remoto v0.1.0-alpha.141 — bateria mais clara e corrente coerente

- Próxima versão: alpha.142/versionCode 142. Manter metadados, notas, novidades, validação e testes sincronizados a cada mudança.
- A tela Bateria foi reorganizada para separar leitura atual, ferramentas/alertas e resumos históricos, reduzindo a confusão visual sem remover dados.
- Quando o Android informar corrente positiva em estado de descarga, a interface pode exibir o sentido visual como saída da bateria; o valor bruto continua preservado no diagnóstico.
- As taxas de bateria continuam distintas da corrente elétrica: previsões e ritmos usam variação de percentual por hora, enquanto a corrente em mA permanece baseada nas propriedades BatteryManager/contador.

# Celular Remoto v0.1.0-alpha.139 — notificação e isolamento de telemetria

- Próxima versão alpha.141/versionCode 141.
- Não mostrar corrente de sinal conflitante; previsão somente de sessão observada e válida.
- Telemetria remota permanece em RAM, expira em 3 minutos e é vinculada a host+chave. Não prometer coleta contínua em segundo plano.
- Preservar AES-GCM, WIRE_COMPAT, consentimento e única porta. Build Android e testes físicos pendentes.

# Celular Remoto v0.1.0-alpha.138 — vínculo de Mensagens por chave e encerramento rápido (09/11/2026)

- Base alpha.137; próxima versão alpha.139/versionCode 139. Incrementar Gradle, AppVersion, README, CHANGELOG, RELEASE_NOTES, arquitetura, novidades, testes e validação em todas as alterações.
- A tela Mensagens vincula seu contexto a host+chave (escopo SHA-256 truncado), não somente IP. Mensagens, callbacks e marcos de leitura em RAM da credencial anterior nunca devem aparecer após re-pareamento com o mesmo endereço.
- Cada reabertura da página invalida a geração anterior. Se a chave mudar durante a consulta, limpar conteúdo, cancelar atualização automática da sessão inválida e exigir nova abertura.
- mDNS ADB só aceita `_adb-tls-connect._tcp` ou `_adb-tls-connect._tcp.local`, no host selecionado. Não ampliar varredura de portas nem autorizações.
- `MonitorServer.stop` deve acordar `RemoteEventSignal.await` para liberar o worker sem 11 segundos adicionais; preservar porta única, WIRE_COMPAT alpha.111, AES-GCM e consentimento do monitorado.
- Parser/JVM/validador estrutural não comprovam compilação Android nem entrega efetiva; CI/2 aparelhos pendentes.

# Celular Remoto v0.1.0-alpha.137 — correções ADB e isolamento de sessões (09/11/2026)

- Base alpha.136; próxima alteração alpha.138/versionCode 138. Sincronizar Gradle, AppVersion, README, CHANGELOG, RELEASE_NOTES, arquitetura, novidades, testes e validações.
- Na descoberta ADB, endereço de conexão é obrigatório e não nulo; mDNS pode sugerir somente serviço `_adb-tls-connect._tcp`, host já selecionado e porta válida. Não adicionar varredura nem conectar a IP de terceiros.
- Abrir Mensagens via notificação deve zerar todos os dados temporários da sessão anterior antes de renderizar; se a Activity estiver invisível, esperar o `onResume` para consultar.
- Callback de eventos deve revalidar host, chave e escopo criptográfico no momento da entrega à UI. Não ampliar coleta, permissões, serviços de segundo plano nem dados transferidos.
- Requer confirmação de compilação Gradle/Android e teste real em dois aparelhos antes de instalar/distribuir como validado.

# Celular Remoto v0.1.0-alpha.136 — eventos locais e despertar de tela (09/11/2026)

- Base alpha.135; próxima versão alpha.137/versionCode 137. Sincronizar Gradle, AppVersion, README, CHANGELOG, RELEASE_NOTES, arquitetura, Novidades, testes e validações.
- `eventWatch` é opcional, somente em tela visível, na porta AES-GCM existente, com um slot de longa espera; nunca transferir conteúdo de SMS/WhatsApp no resumo de eventos. Continuar verificando consentimentos locais, allowlist e pareamento antes de enviar/consumir respostas.
- Encerrar socket ao pausar Activity, preservar fallback JobScheduler em segundo plano, sem serviço persistente novo. Não prometer entrega imediata sob Doze/sem rede.
- Acender tela exige autorização já concedida no próprio monitorado e não remove bloqueio. Testar via CI e em dois celulares antes de considerar funcionalidade homologada.

# Celular Remoto v0.1.0-alpha.135 — alertas e pareamento (09/11/2026)

- Base alpha.134; próxima versão alpha.136/versionCode 136. Toda edição sincroniza Gradle, AppVersion, README, CHANGELOG, RELEASE_NOTES, arquitetura, Novidades, testes e validações.
- Alertas remotos continuam opt-in e genéricos: filtros independentes SMS/WhatsApp/outros apps, com confirmação local do monitorado. Sem capturar aplicativos novos automaticamente e sem novo serviço push; Android pode adiar JobScheduler.
- Janela de avisos = 60 minutos para recuperar eventos que continuem no buffer, usando data real de chegada ao sistema e exibindo atraso; nunca notificar um replay após reativação nem conteúdo de SMS/WhatsApp na notificação do sistema.
- Cursores vinculados ao host+chave, e validação posterior à consulta no Job para ignorar resultado de pareamento removido/trocado. Não registrar segredos, remetentes ou mensagens em preferências.
- Preservar WIRE_COMPAT alpha.111, canal AES-GCM, tela Controlar padrão, sirene, 12 comandos, espaços Android, ADB, controles locais e workflow único. Build Android e teste real em dois celulares ainda obrigatórios antes de distribuição.

# Celular Remoto v0.1.0-alpha.134 — interface e avisos genéricos (09/11/2026)

- Versão atual alpha.134, baseada na alpha.133. Próxima versão alpha.135/versionCode 135. Sincronizar Gradle, AppVersion, README, CHANGELOG, RELEASE_NOTES, arquitetura, Novidades, testes e validações.
- Controlar substitui Início como tela inicial; quatro destinos e WindowInsets reais; não reativar Home ou elementos sob as barras do Android.
- Manter os 12 comandos em Funcionalidades; sirene sintetizada deve parar após prazo, comando Parar ou encerramento do serviço, sem mexer no volume/DND.
- Avisos remotos separados são opt-in no controlador; monitorado exige autorizações locais, filtragem de conteúdo, canal AES-GCM e dados somente mínimos do SMS. Sem serviços adicionais, push oculto, novas permissões ou prometer instantaneidade.
- Notificação foreground simples: local no título, local+remoto quando expandida, apenas bateria/temperatura/corrente. Leitura remota em RAM expira após 3 min; nunca inventar valores nem estado online.
- Preservar WIRE_COMPAT alpha.111, sequência de versões, autenticação, permissões de câmera, ADB, atualizador e workflows. Compilação Gradle/Android e teste físico em dois aparelhos ainda pendentes.

# Celular Remoto v0.1.0-alpha.133 — cópia individual e relatório completo em Detalhes (05/11/2026)

- Base alpha.132. Próxima versão alpha.134/versionCode 134. Toda alteração sincroniza Gradle, AppVersion, README, CHANGELOG, RELEASE_NOTES, arquitetura, Novidades, testes e validações.
- Detalhes do aparelho: todos os campos disponíveis dos tiles e linhas individuais aceitam toque para copiar; ícone de cópia visível e descrição acessível. Valores ausentes/bloqueados não são copiados. Relatório completo reúne os campos realmente disponíveis por seção; resumo técnico compacto antigo permanece.
- Número da linha: compartilhar APENAS com `sharePhoneNumber` explicitamente ativado no próprio monitorado e permissão READ_PHONE_NUMBERS concedida. Android 13+ usa `SubscriptionManager.getPhoneNumber`, Android 8–12 consulta `TelephonyManager.line1Number` com permissão. Número pode continuar indisponível por operadora/Android. Não criar campo persistido, coleta contínua, solicitação automática ou novo endpoint.
- Clipboard é ação local explícita no controlador, marcado sensível em Android 13+. Relatório que incluir telefone pede confirmação adicional; não registrar dados pessoais em logs. A cópia do número pode continuar acessível a apps que leiam o clipboard no aparelho; não alegar proteção completa.
- Não alterar WIRE_COMPAT=alpha.111, criptografia, autenticação, 12 comandos, atualizações, permissões de câmera/SMS ou único workflow. Testes isolados e parser não equivalem a build Android; executar CI e teste físico.

# Celular Remoto v0.1.0-alpha.132 — atualização local/remota robusta (05/11/2026)

- Base alpha.131, próxima versão alpha.133 / versionCode 133. Sincronizar Gradle, AppVersion, README, CHANGELOG, RELEASE_NOTES, arquitetura, Novidades e testes em toda alteração.
- `appUpdate` só pode operar dentro do canal pareado AES-GCM: status sempre consultável; check/download/cancel apenas com opt-in local `remote_requests=false` por padrão. Sem permissão/instalação automática, sem comando de instalar, sem ativação por alarme/boot.
- Estado remoto transmite apenas versão, etapa, percentual e pré-requisito de instalador; sem URLs, tokens, caminhos, IP, dados pessoais ou chaves. Ao mudar de par, descartar respostas antigas. Polling opcional de status a cada 15 s limitado a 3 min enquanto a tela Atualizações estiver visível; sem serviço persistente.
- SHA-256, `packageName`, `versionCode` superior e assinatura compatível com instalada são obrigatórios antes de disponibilizar `Instalar`. Confirmar no próprio Android. Nunca enfraquecer o verificador, tolerar APK com certificado diferente ou aceitar URLs de hosts arbitrários.
- Verificação/download/reconciliação/cancelamento usam uma disciplina única de sincronização; evitar deadlocks, condições de corrida e limpeza prematura de downloads válidos. Preservar preferências de rede não tarifada, nível de bateria e Labs.
- WIRE_COMPAT permanece `0.1.0-alpha.111`, 12 comandos, câmera, Bateria e único workflow inalterados. Verificações estáticas/JVM não substituem o build Gradle/Android e o teste físico em dois aparelhos.

# Celular Remoto v0.1.0-alpha.131 — fotografia remota preparada em um passo (05/11/2026)

- Base alpha.130; próxima versão alpha.132/versionCode 132. Sincronizar Gradle, AppVersion, README, CHANGELOG, RELEASE_NOTES, arquitetura, Novidades e validação.
- `RemotePhotoSetupActivity`: APENAS após toque local e Activity visível, pedir CAMERA e POST_NOTIFICATIONS quando necessário, habilitar Monitorado explicitamente, aguardar o servidor ficar pronto e iniciar `PhotoReadyService` de câmera FGS ainda com Activity visível. Nunca ativar sessão de câmera por comando remoto, boot, alarm ou sem gesto.
- `RemotePhotoReadiness` classifica estados do snapshot autenticado já existente (`readiness.items[id=photo]`) sem alterar `WIRE_COMPAT=alpha.111`, protocolo AES-GCM ou endpoints. Versões antigas sem campo de prontidão permanecem consultáveis, mas nunca recebem autorização implícita.
- `PhotoReadyService`: não deixar câmera capturando continuamente, manter uma notificação de câmera com ação Parar e fechar Camera2/ImageReader após cada JPEG. Encerramento do serviço exige novo consentimento local; nenhum rearmamento automático.
- Não criar novos serviços, permissões, sockets, sessões de vídeo, polling remoto ou novo workflow; WakeLock permanece opt-in e os modos de bateria da alpha.130 independentes. Testar CI Android e dois aparelhos, tela apagada e retorno após interrupção.

# Celular Remoto v0.1.0-alpha.130 — bateria independente (05/11/2026)

- Base alpha.129; próxima versão obrigatória alpha.131/versionCode 131. Sincronizar Gradle, AppVersion, README, CHANGELOG, validações e Novidades.
- Um único MonitorService expõe dois modos de trabalho distintos: REMOTE (servidor TCP/FGS connectedDevice) e BATTERY_ONLY (sem porta, FGS specialUse). Se ambos forem autorizados, o modo remoto compartilha a **mesma** agenda de 30 s de bateria. Nunca criar segunda amostragem ou segunda notificação contínua.
- Coleta local contínua começa DESLIGADA e deve ser habilitada no próprio aparelho. Interromper Monitorado não cancela coleta local; desativar coleta local não revoga Monitorado. O ícone de notificação local possui Parar bateria independente.
- FGS specialUse declarado para medição local contínua explicitamente iniciada; caso de uso sujeito à revisão Google Play. Android pode interromper serviços e atrasar coletas; não prometer tempo real. Sem novos WakeLocks por padrão.
- Manter segurança AES-GCM, WIRE_COMPAT alpha.111, autenticação, consentimentos por sessão, acesso a arquivos e um único workflow inalterados. Validar CI Android e dois aparelhos antes de release estável.

# Celular Remoto v0.1.0-alpha.129 — continuidade e robustez (05/11/2026)

- Base alpha.128; próxima versão obrigatória alpha.130/versionCode 130. Sincronizar Gradle, AppVersion, documentos, testes e Novidades.
- `MonitorContinuityStore` mantém somente carimbos locais, último início/parada, heartbeat limitado a 2 min e contagem de recriações sem encerramento observado. Isso NÃO comprova Doze, perda do Wi-Fi nem ação da Xiaomi. Nenhum dado remoto, IP, token ou leitura pessoal.
- Serviços já ativos não devem reiniciar rotinas por chamadas repetidas de start; socket TCP só é recuperado se necessário. Notificação com falha não deve derrubar serviço.
- `registerDefaultNetworkCallback` monitora somente a rede padrão informada pelo Android; manter supervisor e reavaliação do listener. Não adicionar sockets ou sondagens, nem prometer rede sob Doze.
- WakeLocks continuam totalmente opt-in, inclusive Wi-Fi. Capturas e confirmações locais, AES-GCM, WIRE_COMPAT alpha.111 e workflow único intactos.
- Compilação Android completa e testes físicos em dois aparelhos são obrigatórios antes de classificar a versão como estável.

# Celular Remoto v0.1.0-alpha.128 — conexão estável (05/11/2026)

- Base alpha.127; próxima versão obrigatória alpha.129/versionCode 129, sincronizando todos os metadados.
- WakeLock e Wi-Fi Lock somente no serviço Monitorado existente, desativados por padrão, nunca remotamente habilitados. Libere no onDestroy/Parar. WakeLock CPU de teste com expiração em 30 min e leases de no máximo 10 minutos.
- Não prometer rede ativa durante Doze; isenção é parcial e o app apenas abre tela de Configurações oficial após toque local. Nunca abrir tela automaticamente, nem requerer nova permissão sensível.
- Lacuna do supervisor indica tempo sem execução observada, não prova queda de rede. Preservar protocolo cifrado, um único workflow, permissões de captura e versionamento WIRE_COMPAT.
- Verificar build real no GitHub Actions e dois aparelhos, medindo consumo térmico/elétrico durante o WakeLock. Validações locais não substituem compilação completa.

# Celular Remoto v0.1.0-alpha.127 — preparação para controle remoto (05/11/2026)

- Base alpha.126; próxima versão obrigatória alpha.128/versionCode 128. Sincronizar Gradle, AppVersion, README, CHANGELOG, documentação, testes e tela Novidades.
- `RemotePreparation.snapshot()` envia somente `schema=1`, contagens e estados genéricos de funções dentro de `snapshot` autenticado AES-GCM. Nunca expor sem chave, criar um endpoint público, compartilhar IPs, coordenadas, nomes de apps, SMS ou credenciais.
- `RemotePreparationPolicy` distingue `ready`, `setup`, `session`, `off`; nenhum desses valores concede permissão. A UI local não solicita grant ao abrir; a autorização de atualizações remotas permanece OFF por padrão e instalação exige toque local no Android.
- Sem novos serviços, sockets, permissões, captura automática ou alteração do marcador `WIRE_COMPAT=alpha.111`. Versões antigas ignoram o campo novo.
- Verificar CI Android, fonte ampliada/320–393 dp, resposta cifrada e autorizações em dois dispositivos. Sintaxe não substitui build completo.

# Celular Remoto v0.1.0-alpha.126 — diagnóstico de alertas (05/11/2026)

- Base alpha.125. Próxima versão obrigatória alpha.127/versionCode 127; sempre sincronizar Gradle, AppVersion, documentação, validações e tela Novidades.
- `AlertDelivery` valida permissão POST_NOTIFICATIONS, estado geral e canal ANTES de confirmar eventos. `POSTED` significa aceito pelo gerenciador Android, não notificação exibida ou lida. Registro privado e limitado a 48 tentativas sem mensagens nem identificadores de aparelhos.
- Eventos recentes bloqueados não devem ter sequência confirmada; repetir apenas enquanto válidos (até 24h). O alerta de saúde resolvido não deve reaparecer. Metas locais têm compatibilidade com registros anteriores.
- `ControllerBatteryEvents` e `ControllerHealthAlerts` recebem dados apenas via resposta autenticada. O JobScheduler continua opt-in; não criar rede extra/serviço/socket ou ampliar acesso à câmera/tela/SMS.
- Verificar build Android completo, funcionamento em aparelhos reais, negação de notificação geral e de canal, teste manual e privacidade antes de considerar publicação validada.

# Celular Remoto v0.1.0-alpha.125 — notificação local e remota (05/11/2026)

- Base alpha.124. Próxima versão obrigatória alpha.126 / versionCode 126. Sincronizar Gradle, AppVersion, README, CHANGELOG, docs, testes e Novidades.
- `MonitorNotificationContent` formata somente valores observados e previsões válidas. Não chamar última resposta de conexão persistente; mostrar tempo aproximado desde a última autenticação, sem IP e sem dados privados na tela bloqueada.
- `MonitorService` aproveita **o mesmo executor serial** do tick de bateria para obter leitura e estimativa, sem novo serviço, conexão remota ou IO de histórico na thread principal. Limitar a frequência das notificações; manter ações Parar e Detalhes.
- Não modificar monitoramento, autorização, protocolo AES-GCM, `WIRE_COMPAT=alpha.111`, downloads/instalação ou workflow único. Build Android CI e validação física de bateria e notificação ainda necessários.

# Celular Remoto v0.1.0-alpha.124 — comandos sempre visíveis (05/11/2026)

- Base: alpha.123. Próxima versão obrigatória **alpha.125/versionCode 125**. Sincronizar app/build.gradle.kts, AppVersion, README, CHANGELOG, docs, teste de versão e Novidades.
- `RemoteDashboardUi.kt`: **4 atalhos e 8 outras funções devem ser mostrados SEMPRE** em Controlar, mesmo antes da primeira leitura. Não reintroduzir botão de ocultar/expandir ou `expandedRemoteFunctions`. Todos os 12 callbacks/rotas originais permanecem e ainda exigem chave/consentimentos correspondentes.
- `RemoteActionsRenderKey` invalida cache visual quando aparelho, presença da chave, quantidade de notificações, tema, escala de fonte ou largura da tela mudar. Cache mantém somente Views do controlador, nunca credenciais ou snapshots.
- Não alterar AES-GCM, `WIRE_COMPAT=alpha.111`, fluxo de atualizações, serviço único, permissões e gravações. Preservar um único workflow.
- Validar Android/Gradle real no GitHub Actions e UI em 320, 360 e 393dp, fontes ampliadas, temas e dois aparelhos antes de tratar o APK como aprovado.

# Celular Remoto v0.1.0-alpha.123 — regras de aparência e acessibilidade (05/11/2026)

- Base alpha.122; próxima versão obrigatória **alpha.124/versionCode 124**. Sincronizar Gradle, AppVersion, documentação, testes, Novidades e notas a cada alteração.
- `AppearancePolicy.kt` controla resolução de modo escuro/AMOLED e decisões de densidade de UI, sem acesso a rede ou permissão.
- Modo AMOLED deve manter fundo preto e diálogos correspondentes, reconhecido nas Activities secundárias. Não presumir economia em telas LCD.
- Grades e botões devem se ajustar a fontes maiores e largura estreita sem esconder ações; controles continuam acionáveis por toque e acessibilidade.
- Não alterar AES-GCM, WIRE_COMPAT alpha.111, consentimentos, captura, monitoramento, bateria, atualizador nem workflow único.
- Validar compilação Gradle/CI real e UI em 320/360/393 dp, fontes ampliadas e aparelhos antes de considerar release testada.

# Regras da alpha.122 — correção de compilação do build 15 (05/11/2026)

- Base alpha.121; versão obrigatória `0.1.0-alpha.122`, `versionCode 122`; próxima versão alpha.123/123, mantendo Gradle, AppVersion, documentação, testes e Novidades sincronizados.
- `BatteryCompactUi.kt`: o histórico usa `LinearLayout.minimumHeight` (57 dp). **Não** trocar por `minHeight` (propriedade de `TextView`, não válida para `LinearLayout`). Verificar isso em `tools/check_distribution.py`.
- Não alterar telemetria, algoritmos de bateria, sessões, histórico, arquivos e comandos remotos; pareamento AES-GCM, `WIRE_COMPAT` alpha.111, permissão local, assinatura e workflow único permanecem.
- Testes estáticos não são build Android completo; validar no GitHub Actions, UI em telas pequenas e telefones reais.

# Regras da alpha.121 — Configurações e diálogos compactos (04/11/2026)

- Base alpha.120; versão obrigatória `0.1.0-alpha.121`, `versionCode 121`. Próxima versão: alpha.122/122; sincronizar Gradle, AppVersion, docs, README, CHANGELOG, Novidades, testes e notas de release.
- `SettingsCompactUi.kt` fornece somente componentes visuais. Não introduzir efeitos de rede, permissões, instalação ou compartilhamento ao desenhar componentes.
- Central de Permissões tem categorias Comuns/SMS/Especiais/Sessões, com estados medidos pelo Android. Solicitar permissões é uma ação explícita, SMS é sensível, acessos especiais exigem ação manual e MediaProjection é por sessão.
- Atualizações mantêm validação de APK, assinatura e download sob demanda/automático. Instalar somente após toque local e confirmação Android. Não permitir instalação remota silenciosa.
- Diálogos devem manter as confirmações e a distinção de ações destrutivas. Listas compactas devem conservar descrições acessíveis, texto de fonte ampliada e área de toque ≥48dp.
- Preservar protocolo AES-GCM, pareamento, WIRE_COMPAT alpha.111, serviço e workflow únicos. Validar CI Android e aparelhos reais antes de afirmar APK estável.

# Regras da alpha.120 — Bateria compacta e analítica (04/11/2026)

- Base alpha.119; versão `0.1.0-alpha.120`, `versionCode 120`; próxima alteração alpha.121/121. Gradle, AppVersion, README, CHANGELOG, notas, documentação e Novidades sincronizados.
- `BatteryCompactUi.kt` só contém componentes de interface. Nunca inferir produção solar, ajustar sinal/unidade de bateria sem evidência, ocultar origem de medições ou criar previsões sem dados.
- A aba Bateria deve respeitar fonte ampliada e 320/360/393 dp; usar 2 colunas em telas estreitas e 3 onde couber, com conteúdo acessível e controles mínimos de 43+ dp nos seletores, 48+ nas ações.
- Preservar compatibilidade `WIRE_COMPAT=alpha.111`, AES-GCM, pareamento, limites de histórico remoto, relatórios completos, testes e controle local/remote, assinatura, atualizador e workflow único.
- Executar CI Android real e testes físicos antes de declarar APK funcional; a validação de parser não substitui compilação.

# Regras da alpha.119 — Controlar e Monitorado compactos (04/11/2026)

- Base alpha.118. Versão obrigatória **0.1.0-alpha.119** e `versionCode 119`. Próxima alteração será alpha.120/120, com Gradle, `AppVersion`, README, CHANGELOG, RELEASE_NOTES, docs e Novidades sincronizados.
- O painel Controlar utiliza indicadores compactos para bateria, temperatura e rede. Nunca remover detalhes via toque, hora da resposta autenticada, status de cache ou diagnóstico da rede.
- O Monitorado mostra três atalhos de Tela, Câmera e Foto: MediaProjection exige consentimento do Android por sessão, câmera só inicia localmente após confirmação, e fotografia deve ser preparada localmente. Estados e ação de parar precisam permanecer visíveis.
- Preservar 12 ações remotas, TLS/pareamento AES-GCM, compatibilidade entre versões, chaves, notificações, bateria, atualizador assinado, um único serviço e um único workflow. Nenhuma operação remota nova.
- Manter botões acionáveis com mínimo 48dp, rotulagem acessível e grade que se adapta à fonte ampliada. Validar build Android real e testes físicos em 320/360/393dp antes de afirmar APK aprovado.

# Regras da alpha.118 — reformulação visual (04/11/2026)

- Base: alpha.117, nova versão **0.1.0-alpha.118** e `versionCode 118`. Próxima alteração obrigatoriamente alpha.119/119. Mantenha Gradle, `AppVersion`, README, CHANGELOG, RELEASE_NOTES, docs e tela Novidades sincronizados.
- `CompactDesignUi.kt` agrega atalhos responsivos: 2 colunas em telas menores que 360dp ou fonte acima de 1,20; 3 em celulares comuns; 4 em tablets. Nunca reduzir tamanho físico mínimo de toque abaixo de 48dp para controles.
- Configurações devem abrir por categorias e busca; preservar todos os destinos existentes, status reais e botões de consentimento. Novidades na abertura exibe só os destaques da versão e oferece histórico separado.
- Preservar criptografia AES-GCM, opt-in local para câmera/tela, permissão de SMS/ADB, sockets, versões do protocolo, controle e diagnóstico de conexão, limites de bateria e atualização assinada. Redesenho não autoriza coleta/remoto extra.
- Validar compilação Android **real** no GitHub Actions e teste físico em 320/360/393dp, escala de fonte grande, temas claro/escuro, 2 celulares pareados antes de alegar release aprovada.

# Celular Remoto — 0.1.0-alpha.117 — Diagnóstico de conexão

- Marcador `WIRE_COMPAT` permanece alpha.111, `versionName/versionCode` passam para alpha.117/117.
- Heartbeat de estado deve ser cifrado, não consultar dados do aparelho, não gerar janelas invasivas e não alterar autenticação existente.
- Diferenciar disponibilidade de tentativas realizadas de uptime real; tratar lacunas sem sondagem como desconhecidas.
- JobScheduler em segundo plano é opcional, mínimo 15 min, sem prometer tempo real. Sem serviço persistente adicional nem varredura geral de LAN.
- Reconexão só em host salvo com credencial idêntica, nunca replicar credenciais a novos IPs.
- Alertas após três falhas autenticadas e recuperação com permissão de notificação. Não armazenar chaves em métricas.
- Próxima versão sequencial alpha.118/versionCode 118. Sempre executar CI e testes físicos; testes estáticos não são APK aprovado.

# Celular Remoto — 0.1.0-alpha.116 — Correção build 9

- O build 9 da alpha.115 compilou código e testes Kotlin, mas falhou em 1/210 testes (`AppVersionTest.apkVersionAndWireVersionAreIndependent`) por expectativa antiga de alpha.112.
- O teste agora compara a versão APK com `BuildConfig.VERSION_NAME` e `BuildConfig.VERSION_CODE`, mantendo o protocolo separado (`WIRE_COMPAT=alpha.111`); nunca reintroduzir igualdade textual rígida de versões no pareamento.
- O validador de distribuição exige sincronização entre nome/code do Gradle sem número hardcoded por release; futuras versões ainda devem incrementar Gradle e `AppVersion.NAME` e atualizar docs, novidades e teste.
- Preservar único workflow, serviço, AES-GCM, bateria, configuração de permissões e atualizador remoto; sem modificar Secrets ou assinatura.
- Próxima versão sequencial: alpha.117/versionCode 117. CI Android e testes em aparelhos pendentes até comprovação real.

# Celular Remoto — 0.1.0-alpha.115 — Correção build 8

- O workflow 8 da alpha.114 concluiu `:app:compileDebugKotlin` e falhou em `:app:compileDebugUnitTestKotlin`: `BatteryHistoryMathTest.kt:14` passou `Double?` para `assertEquals` que requer `Double`.
- A alpha.115 usa `requireNotNull(BatteryHistoryMath.rate(sample()))` no teste numérico, sem mascarar retorno nulo. Não alterar regras de medição somente para resolver erro do teste.
- Preservar o protocolo autenticado, o único workflow, a compatibilidade com alpha.111 e as funcionalidades de bateria existentes.
- Próxima versão sequencial: alpha.116/versionCode 116. Compilação Android CI e testes físicos continuam pendentes até confirmação real.

# Celular Remoto — 0.1.0-alpha.114 — Regras ativas

- Aba Bateria: histórico filtrável do aparelho selecionado, com testes Labs e sessões automáticas organizadas por tipo e data. Comparações somente entre testes concluídos; condições distintas ou amostras insuficientes impedem conclusões de superioridade.
- Taxa de carga/descarga calculada em pontos percentuais por hora do período decorrido, com cobertura >=70%, duração >=10 min e variação >=2 pontos percentuais. Isto não é energia solar gerada nem medição de capacidade.
- Relatórios remotos autenticados retornam no máximo 24 resumos por categoria (testes e automáticas), com gráficos em amostras recentes selecionadas e payload limitado. Históricos privados continuam com limites existentes; não expandir socket ou permissões indiscriminadamente.
- Preservar o serviço Monitorado único, protocolo AES-GCM e WIRE_COMPAT alpha.111; não interferir no sistema de atualização online nem na confirmação local de instalação.
- Versão seguinte alpha.115/versionCode 115. É obrigatório testar compilação Android CI e funcionamento real de UI/pareamento em dois aparelhos. Verificações estáticas não equivalem ao build completo.

# Celular Remoto — 0.1.0-alpha.113 — Regras ativas

- A nova leitura de corrente respeita o sinal líquido reportado pelo Android, mesmo quando CHARGING e corrente negativa. CURRENT_NOW/AVERAGE são µA; CHARGE_COUNTER µAh. Não inferir potência solar, nem corrigir escala/sinal OEM sem evidência.
- Previsões só com tendência observada sem lacunas, ou histórico comparável e recente; distinguir ausência de leitura. Evitar extrapolar resultados de painel USB para outros carregadores.
- Sessões automáticas preservadas no arquivo atômico e expostas no protocolo autenticado existente; não criar serviço paralelo ou nova permissão.
- Próxima alteração será alpha.114/versionCode 114; manter protocolo WIRE_COMPAT alpha.111 para interoperar com alpha.111/112, até mudança documentada e validada.
- Executar testes locais, compilação Android no GitHub Actions e testes físicos em dois aparelhos antes de declarar funcionalidade validada.

# Celular Remoto — 0.1.0-alpha.112 — Regras ativas

- A versão do APK e o marcador de protocolo são distintos: `AppVersion.NAME=alpha.112`, `WIRE_COMPAT=alpha.111`. Pares 111 e 112 interoperam somente em operações já disponíveis; novos endpoints exigem alpha.112+.
- Operação autenticada `appUpdate` só consulta, verifica e enfileira DownloadManager. A permissão local `remote_requests` é necessária para ações de rede. NUNCA iniciar instalador a partir de comando remoto ou serviço em segundo plano.
- Verificações 30/60/180/360/720/1440 min, default 60, via JobScheduler; Android pode atrasar. Download apenas após Release assinada verificada, guardando restrições de rede/bateria.
- Preserve apenas um workflow, um serviço Monitorado e o protocolo AES-GCM; transporte de APK por Wi-Fi offline permanece pendente.
- Próxima versão alpha.113 / versionCode 113. Sincronizar Gradle, README, CHANGELOG, notas, testes e validador; exigir build Android na CI e testes em 2 aparelhos.

# Celular Remoto 0.1.0-alpha.111 — Regras ativas

- Alpha.111 disponibiliza atualizações online pelo GitHub Releases do repositório `drilemoz/Celular-Remoto`; inclui prereleases, ignora builds Debug e só aceita APK com mesmo pacote, certificado e SHA-256 válido.
- Atualizações usam JobScheduler e DownloadManager nativos, com bateria/rede configuráveis; **nunca** instalar automaticamente ou via comando remoto. A instalação depende de toque local e confirmação no sistema Android.
- O workflow `android.yml` continua único; anexar `celular-remoto-update.json` apenas quando há assinatura Release estável. Não colocar keystore/Secrets no ZIP nem no Git.
- A passagem offline do APK por Wi-Fi entre celulares está planejada e NÃO integra a alpha.111. Manter a comunicação remota existente e exigir pares na mesma versão.
- Próxima versão sequencial: `0.1.0-alpha.112`, `versionCode 112`, com sincronização de Gradle, AppVersion, README, CHANGELOG, Release Notes, validador e tela Novidades.
- Build Gradle Android, download real em aparelhos e atualização com certificado estável ainda dependem de validação em CI/dispositivos.

# Celular Remoto 0.1.0-alpha.110 — Regras ativas

- Alpha.110 cria Central de permissões com boas práticas: não pedir grants automaticamente nem habilitar compartilhamento de SMS, áudio, GPS ou notificações só porque o Android autorizou.
- Acessos especiais (Acessibilidade, leitor de notificações, uso, localização em segundo plano, administrador) devem abrir configuração local oficial, com consentimento contextual.
- Câmera, áudio, localização e SMS são permissões distintas; READ_SMS/RECEIVE_SMS podem ser bloqueadas pelo instalador. Fotos/arquivos dependem SAF, não inventar READ_MEDIA_IMAGES. MediaProjection sempre por sessão.
- Próxima versão 0.1.0-alpha.111/versionCode 111; sincronize validações, Kotlin, docs e novidades em cada alteração.
- Compilação Gradle completa e testes de primeira instalação em aparelhos físicos continuam obrigatórios.

# Celular Remoto 0.1.0-alpha.109 — Regras ativas

- A alpha.109 resolve falha em `PairingCodeTest.rejectsUnexpectedQr`: não usar portas literais obsoletas em mutações de QR. Construir usando `WireProtocol.PORT` e exigir QR alterado antes de testar rejeição.
- A identidade QR vigente é `celularremoto://pair`, porta 37842; preserve a validação estrita de esquema, versão, parâmetros, IP privado e chave temporária. QR antigo é incompatível; renovar pareamento entre as mesmas versões.
- Preserve um único workflow `.github/workflows/android.yml` executado em `push` na `main`, com `concurrency` e APK direto em Release.
- Próxima versão `0.1.0-alpha.110` / versionCode 110; sincronizar documentação, Novidades, testes, Gradle, AppVersion e validador em qualquer alteração.
- Build Android na CI e testes em dois aparelhos são obrigatórios antes de declarar o APK validado.

# Celular Remoto 0.1.0-alpha.108 — Regras ativas

- Alpha.108 corrige colisão de `type` com `Intent.type` no `BatteryScreenUi.kt`; em `Intent(...).apply { ... }`, qualificar explicitamente a propriedade como `this.type`, principalmente quando existir uma variável `type` no escopo externo.
- O **único** workflow `.github/workflows/android.yml` é acionado automaticamente **apenas** em `push` para a branch `main`. Sem `pull_request` e sem `workflow_dispatch`; manter `concurrency` para cancelar builds antigos da mesma branch e evitar Releases concorrentes.
- Próxima versão sequencial: **0.1.0-alpha.109/versionCode 109**. Sincronizar Gradle, AppVersion, README, CHANGELOG, notas e validador.
- Preservar autenticação AES-GCM, porta 37842, controle pareado, dados do Lab e de sessões, permissões, notificações, serviço único e identificação de que potência da bateria não mede a potência do painel solar.
- Exigir build Gradle Android e validação em dois dispositivos. Check de sintaxe, XML/YAML e testes isolados não comprovam `:app:assembleDebug`.

# Celular Remoto 0.1.0-alpha.107 — Regras ativas

- A partir da alpha.107 o aplicativo unificado chama-se **Celular Remoto**, `applicationId`/namespace `br.dev.celularremoto` e raiz de pacotes Kotlin correspondente. Próxima versão **alpha.108/versionCode 108**. Preservar sincronização Gradle, AppVersion, documentação, UI, testes e workflow.
- O antigo Monitor Remoto e o Pulso Bateria são referências históricas apenas. Não reintroduzir dependências de outro APK; novo ID não herda Keystore/preferências nem autorizações do usuário. Exigir novo pareamento e guardar os históricos do antigo até exportar.
- Coleta dos testes Labs, sessões automáticas e alertas usa o serviço MonitorService existente e seu worker serial. Nunca iniciar segundo serviço, socket remoto sem autenticação ou abrir captura sem consentimento.
- A porta TCP atual é 37842 (diferente de 37841 da instalação anterior), protocolo AES-GCM e versão estrita continuam. Alertas remotos não são garantidos em tempo real. Preservar proteção térmica existente e confidencialidade das notificações.
- Término de testes por 100% exige confirmação repetida. As previsões de autonomia exigem 10 min e 2 pontos percentuais reais; lacunas, valores sem origem ou potência nominal não podem ser mostrados como medidos.
- Executar `python3 tools/check_distribution.py`, testes Kotlin/JVM e build Android completo na CI antes de considerar o APK validado. Testar local/remoto e controle simultâneo em dois celulares físicos.

# Monitor Remoto 0.1.0-alpha.106

- Alpha.106 integra aba Bateria, BatteryLabStore/Math e endpoint autenticado `batteryLab`. Próxima alpha.107/versionCode 107. Os dois aparelhos devem usar a MESMA versão para qualquer operação remota.
- NÃO substituir a corrente líquida por uma afirmação de potência gerada no painel solar. Dados com lacuna >90 s e leituras de corrente inválidas devem permanecer desconhecidos. Preservar relógio monotônico, atomicidade do histórico e limites de retenção.
- Coleta somente durante testes ativos pelo MonitorService; não criar segundo listener/serviço, exigir permissão nova ou abrir canais sem autenticação. UI não deve mostrar dados de host/chave antigos após mudança ou revogação.
- Confirmar compilação real Android no CI e testes com dois celulares, especialmente após reinício do serviço, antes de qualquer instalação definitiva.

# Monitor Remoto 0.1.0-alpha.105

- Alpha.105: navegação inferior fixa em `MobileNavigationUi.kt`, com layout principal em `MainActivity.kt`. Não recriar a barra inferior dentro da ScrollView.
- Os atalhos remotos são duas colunas, com Tela/Arquivos/Câmeras/Localização destacados; manter todas as 12 ações e seus consentimentos.
- Dados `RemoteConnectionManager.cached` não são prova de conexão permanente. Não expandir permissões, acesso a arquivos ou operações remotas durante ajustes visuais.
- Preservar o comportamento alpha.104 dos sockets, do serviço e da revogação de chaves. Próxima versão alpha.106/versionCode 106, com sincronização de documentação e validador.
- Executar `tools/check_distribution.py`, compilação Gradle/CI e testes em telas pequenas/fontes ampliadas antes de instalar.

# Monitor Remoto 0.1.0-alpha.104

- A alpha.104 fecha todos os sockets aceitos ao parar/reiniciar o listener; manter cancelamento de tarefas e limite de fila, e não permitir respostas após troca/revogação da chave de pareamento.
- `AccessAlertWindow` guarda apenas IDs das sessões recentes; preservar o aviso inicial, não alertar a cada requisição e manter memória limitada. Testes JVM em `AccessAlertWindowTest`.
- Próxima versão alpha.105/versionCode 105, mesmo APK nos dois aparelhos. Confirmar Gradle/CI Android e teste de recuperação real antes de instalar.

# Monitor Remoto 0.1.0-alpha.103

- Alpha.103: a tela Monitorado redesenha somente quando `MonitorUiRefreshPolicy.State` muda; não agendar dois ticks de 5 segundos ao chamar `render()` de dentro de `monitorStatusPoll`.
- O token visual de clientes ignora horário para não renderizar a cada consulta, MAS o acesso deve expirar após 45 segundos via `AuthenticatedAccess.recent()`; o diálogo busca último acesso fresco ao abrir. Não sugerir conexão TCP permanente.
- A busca de Configurações faz debounce de 120 ms apenas para a apresentação; operações de configuração e limpeza manual continuam síncronas. Cancelar atualização pendente quando o campo sai da tela.
- Preservar autenticação AES-GCM, autorização local para MediaProjection/Camera, parada, ADB pareado, privacidade e estrutura de arquivos. Próxima alpha.104 / versionCode 104, mesmo aplicativo nos dois aparelhos. Confirmar CI Android antes de instalar.

# Monitor Remoto 0.1.0-alpha.102

- Alpha.102 moderniza `SettingsHubUi.kt`, `SettingsScreenUi.kt`, `AppDialogUi.kt` e acrescenta `ic_search.xml`. Indicadores na página de Configurações mostram **somente permissões Android locais** e não significam serviços ativos ou compartilhamento remoto.
- O seletor de perfil GPS deve exigir toque explícito para gravar a preferência. Preservar o bloqueio de alteração durante sessões ativas, autorização por Android e interrupção de compartilhamento.
- Manter confirmação explícita em ações destrutivas/privadas, mesma versão nos dois aparelhos e nenhuma ampliação de permissões ou comandos.
- Próxima versão sequencial: 0.1.0-alpha.103/versionCode 103. Executar `tools/check_distribution.py`, validar Kotlin/XML e confirmar compilação Android na CI.

# Monitor Remoto 0.1.0-alpha.101

- Alpha.101 moderniza `HomeScreenUi.kt`, `MonitorScreenUi.kt` e adiciona os componentes `statePill` e `monitorFeatureCard` em `UiComponents.kt`. Preservar indicadores como estado *local* ou acesso *recente*, nunca representá-los como sessão permanente.
- Preservar os pedidos de consentimento MediaProjection, Activity da câmera, preparação local de fotografia e controles de parada; os novos cartões nunca podem executar a captura somente por exibir a tela.
- Não modificar pareamento ou protocolo remoto nesta fase. Validar `tools/check_distribution.py`, sintaxe Kotlin e build Android em CI antes de afirmar que o APK compila.
- Próxima versão: 0.1.0-alpha.102 / versionCode 102. Ambos os aparelhos precisam de exatamente a mesma versão.

# Monitor Remoto 0.1.0-alpha.100

- Correção do workflow "Gerar APK e Release 87" (alpha.99): `:app:compileDebugKotlin` falhou por colisão de nome em `MonitorScreenSkin.kt` entre a cor `foreground: Int` e `View.foreground: Drawable` nos blocos `.apply`. Qualificar as três referências como `this@MonitorScreenSkin.foreground`.
- Manter o teste regressivo `tools/check_distribution.py`. Depois de editar UI com `.apply`, verificar colisões entre os nomes de propriedades das views Android e os nomes do kit visual.
- Preservar modernização, dados do monitorado, autenticação, pareamento, fluxos de consentimento e ações ADB. Próxima versão sequencial: 0.1.0-alpha.101 / versionCode 101.
- Este ZIP requer build Android completo em CI para validação final.

# Monitor Remoto 0.1.0-alpha.99

- Corrigido workflow "Gerar APK e Release 86" (alpha.98): falha `:app:processDebugResources` por uso inválido de `android:windowDimAmount` em `res/values/monitor_dialog_styles.xml`. Usar `android:backgroundDimAmount` nos temas `MonitorDialogLight` e `MonitorDialogDark`.
- O verificador `tools/check_distribution.py` deve proteger a regressão. Não alterar temas para atributos de plataforma sem confirmar a existência no SDK usado pelo projeto.
- Preservadas as mudanças visuais até a alpha.98 e os controles de segurança. Próxima versão: alpha.100 / versionCode 100. Exigir ambos os aparelhos com mesma versão.
- Ainda pendente reexecutar Android/Gradle na CI; a validação de XML e PSI local não substitui o build completo.

# Monitor Remoto 0.1.0-alpha.98

- Alpha.98 refina visualmente ConnectionScreenUi, PairingScreenUi, RemoteDetailsActivity e AdbActivity. Preservar a criptografia, QR temporário, pareamento manual, credenciais salvas, comandos ADB fixos, confirmações e FLAG_SECURE.
- Todas as telas independentes devem reutilizar MonitorScreenSkin; a próxima versão sequencial é alpha.99 / versionCode 99.
- Sincronizar Gradle, AppVersion, README, CHANGELOG, docs/RELEASE_NOTES, docs/ARCHITECTURE, VersionNewsUi e validador em toda mudança. Não confundir sintaxe Kotlin com compilação Android.

# Monitor Remoto 0.1.0-alpha.97

- Alpha.97 moderniza `RemoteScreenActivity` e `RemoteMessagesUi` reaproveitando `MonitorScreenSkin`, `MonitorVisualKit` e os diálogos temáticos; nenhum comando, permissão, token MediaProjection, regra SMS ou acesso remoto deve ser alterado ao evoluir o visual.
- O modal de resposta mantém o duplo passo (revisar e confirmar envio), valida destinatário/conteúdo e não persiste dados sensíveis. As notificações mantêm filtros e conteúdos apenas na RAM durante a visita.
- Versão seguinte alpha.98/versionCode 98; atualizar Gradle, `AppVersion`, README, CHANGELOG, docs, tela Novidades e validador. Dois aparelhos exigem a mesma versão; build Android completo deve ser confirmado no GitHub Actions.

# Monitor Remoto 0.1.0-alpha.96

- Alpha.96 moderniza cinco telas independentes usando `ui/MonitorScreenSkin.kt`: fotos no controlador, preparo da foto, câmera ao vivo, câmera local monitorada e localização. Novas telas devem reutilizar o mesmo padrão em vez de controles Android sem tema.
- NÃO alterar captura, autorização local, gravação, protocolo, transporte ou revogação ao refatorar a UI. Fotografia só durante sessão previamente armada no monitorado; câmera ao vivo exige Activity local visível.
- Próxima versão sequencial: alpha.97/versionCode 97; sincronizar Gradle, AppVersion, docs, README, CHANGELOG, tela Novidades, validador e artefato. Exigir mesma versão nos dois aparelhos. A validação local não garante build Gradle/Android.

# Monitor Remoto 0.1.0-alpha.95

- Alpha.95 adiciona `ui/MonitorActionSheet.kt` com ações tipadas; usar ícone, texto curto e subtítulo; ações destrutivas sempre destacadas e SEM execução automática. Não substituir as confirmações existentes do ADB, exclusão ou bloqueio.
- Arquivos, ordenação, downloads interrompidos e catálogo de aplicativos utilizam a folha visual compartilhada. `themedPopup` ainda atende confirmações próprias que exigem escolha consciente.
- Configurações mantém pesquisa e autorização local. Não ampliar permissões nem acessar arquivos fora de `Downloads/Monitor Remoto` pertencentes ao aplicativo.
- Incrementar para alpha.96 na próxima alteração, sincronizando Gradle, AppVersion, README, CHANGELOG, RELEASE_NOTES, docs e tela Novidades. Sempre usar mesma versão nos dois aparelhos e validar GitHub Actions antes de afirmar que APK compila.

# Monitor Remoto 0.1.0-alpha.94

- Alpha.94 cria o kit reutilizável `ui/MonitorVisualKit.kt` para padronizar gradualmente telas e diálogos Android sem introduzir dependência Material3/Compose. Ao criar ou refatorar diálogos, preservar comandos e confirmações de cada módulo, usar os temas `MonitorDialogLight/Dark` e testes de regressão.
- Em Energia e manutenção, **bloquear** exige administrador/autorização e confirmação textual, **acender** apenas encaminha à orientação de Ver tela, **reiniciar** continua dependendo de conexão ADB TLS ao mesmo aparelho e confirmação dentro de `AdbActivity`. Não adicionar desligamento remoto.
- Incrementar versão sequencial para alpha.95 na próxima alteração. Compilar Android via GitHub Actions antes de afirmar que o APK funciona.

# Monitor Remoto 0.1.0-alpha.93

- A consulta `locationSnapshot` é autenticada com a chave AES-GCM já pareada, controle de nonce, versão exata e registro de acesso. Ela só retorna o último fix permitido pelo `LocationShareStore`, nunca inicia GNSS por comando remoto.
- O modo Acompanhar só solicita atualização enquanto a Activity está visível, a cada 3 segundos; não cria novo serviço, localização oculta nem altera a frequência do GPS do monitorado. Diferenciar latência de rede, última resposta e idade real do fix.
- Versionamento sequencial; próxima alpha.94. Executar `tools/check_distribution.py` e CI Gradle antes de instalar.

# Monitor Remoto 0.1.0-alpha.91

- Alpha.91: manter diálogo central ao tocar Bateria e Temperatura; consulta `thermalDiagnostics` usa AES-GCM e a mesma versão em ambos. BatteryManager e PowerManager são confiáveis apenas no que o fabricante oferece; não inventar CPU/GPU. ADB é opcional e só permitido quando o host/IP selecionado corresponde a um histórico ADB já pareado, usando comando fixo `dumpsys thermalservice`, com cliente TLS separado. Não transformar em shell arbitrário, não adicionar leitura sysfs por exceção de permissão e não ampliar acesso a dispositivos desconhecidos. Versionamento sequencial, próximo alpha.92.

# Monitor Remoto 0.1.0-alpha.90

- Alpha.90: workflow `Gerar APK e Release 82` falhou em `RemoteFilesActivity.kt:604–606` porque `scroll` não existia no escopo de `openActions`, seguido de erros com receptor `AlertDialog?`. Manter `val scroll = ScrollView(this).apply { addView(holder) }` e `val dialog = modernBuilder().setView(scroll).create()` para chamadas `dialog.show()` e `dialog.window`. `tools/check_distribution.py` protege esta regressão. Não modificar protocolo nem permissões para corrigir erro de UI; compilar no GitHub Actions. Próxima alpha.91.

## Histórico e convenções anteriores da alpha.89
# Convenções permanentes do projeto Monitor Remoto

- Toda alteração de código incrementa sequencialmente `versionCode` e `versionName` (ex.: `0.1.0-alpha.8` -> `0.1.0-alpha.9`). Sincronizar tela, README, CHANGELOG, RELEASE_NOTES e documentação.
- O artefato instalável da CI deve ter **nome real de arquivo** `MonitorRemoto-v<versionName>.apk`, sem `.zip` no final.
- O GitHub Actions `upload-artifact` sempre entrega ZIP ao baixar um Artifact. Publicar Artifact APK só se Release falhar ou for ignorada, evitando duas fontes para a mesma execução. Para **APK direto**, o workflow **também deve publicar o `.apk` como arquivo de uma GitHub Release** (em workflow_dispatch e push ao branch padrão). Não confundir o pacote ZIP de código-fonte com o APK.
- Opcional: com KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS e KEY_PASSWORD, gerar APK Release assinado estável e AAB Release; nunca armazenar keystore ou segredos no repositório nem em arquivos públicos.
- Sem Secrets, somente APK Debug para testes, com assinatura temporária; avisar que o APK pode não atualizar sem reinstalação.
- Não acrescentar serviços de acessibilidade, sobreposição ou vigilância oculta. Captura de tela só por MediaProjection, com consentimento oficial por sessão e indicação/controle visíveis no monitorado. A alpha.30 inclui leitura opcional de SMS somente após autorização local explícita e READ_SMS, que pode ser bloqueada pelo Android; não contornar permissões restritas. Consentimento expresso; lista positiva de notificações vazia por padrão.
- Funcionar primeiro na rede local; não expor a porta na internet pública. Preservar criptografia, parada visível e conexão autenticada.
- Ao entregar versão, reportar status verdadeiro do build Android e disponibilizar ZIP do fonte e, somente após compilação bem-sucedida, o APK; priorizar cópia do fonte no Google Drive.

- QR local: utilizar credencial de curta duração/uso único, proteger a janela com `FLAG_SECURE`, não salvar temporária em preferências e manter pareamento manual disponível. Código QR inclui apenas IP privado da LAN.
- Alertas `App nocivo bloqueado` do Google Play Protect são sinais reais e exigem auditoria e eventual contestação; não mascarar, não prometer que gerar novo APK/QR resolve e não instruir a desligar Play Protect.

- Regressão alpha.8: manter **uma única** implementação `onRequestPermissionsResult` em `MainActivity`; ao adicionar nova permissão, ampliar o `when`, não criar outro override. Executar `python3 tools/check_distribution.py` antes do pacote.

- Alpha.9: leitura QR usa **Camera2 nativo** (`ui/QrScanActivity.kt`) + `ui/QrFrameDecoder.kt` (ZXing Core). Não voltar a `IntentIntegrator`, `CaptureActivity` do ZXing Embedded ou permissões ocultas sem diagnóstico e testes.
- Para problemas de abertura: conferir o logcat `FATAL EXCEPTION` e tag `MonitorRemotoQR`, ciclo de vida Activity/SurfaceTexture/CameraDevice/ImageReader. Não registrar conteúdo do QR (segredo temporário). Oferecer sempre inserção manual do URI.

- Alpha.10: manter tema **claro padrão**, as opções da engrenagem, janela Conectar, botão de atualização compacto, lista de até 5 aparelhos recentes e os dados do Controle no topo. Não reintroduzir formulário longo na tela principal.
- Credenciais por dispositivo usam `RecentDevices` + `SecretVault.profileKeyName`, com migração única da alpha.9. Não reaproveitar `CONTROLLER_KEY` antigo para IPs novos nem gravar segredo em preferências sem criptografia. Esquecer apaga o segredo correspondente.
- Respostas HTTP/TCP não são conexão persistente. Exibir "Conectado" apenas após consulta autenticada bem-sucedida e "Sem resposta" após erro. Não inventar tempos de atualização.
- Próxima versão sequencial: `0.1.0-alpha.90`, `versionCode 90`.

- Alpha.15: preservar a seleção Conectar → QR/Manual/Salvos, confirmação do QR antes de parear, cartões salvos com três ações e o scanner em Activity com tema Dialog.

- Alpha.16: Home retorna ao modo Monitorado sem interromper serviço; tela inicial reúne QR, informações manuais e recentes. Sobre fica em Configurações. Chave oculta no pop-up `FLAG_SECURE`, cópia explícita marcada sensível e compartilhamento somente com confirmação prévia. Preservar.

- Alpha.17: Home (modo 2) é a tela inicial, acessível offline; Monitorado (modo 0) e Controle (modo 1) são páginas separadas. Exibir última conexão real persistida por `RecentDevices.markSuccess`; não tratar tentativa como sucesso. Engrenagem de cada aparelho na Home oferece Renomear/Esquecer, nunca expor chave. Botões compactos no cartão Monitorado à direita. Home/Voltar não interrompem serviço. Na alpha.18, priorizar avaliação das telas de dados do aparelho remoto e do aparelho monitorado, sem alterar navegação silenciosamente.

- Alpha.18: seleção QR/Manual compacta na mesma linha; dispositivos salvos com ações horizontais Conectar/Renomear/Esquecer; Minhas informações tem Mostrar/Copiar/Compartilhar lado a lado. Desconectar volta à Home.
- Alpha.18: `AuthenticatedAccess` registra SOMENTE consultas respondidas após AES-GCM autenticado. Servidor não possui sessão TCP permanente: registro em memória expira após 45 s sem novas consultas. Nome/método são declarados pelo controlador e podem ser falsificados por outro portador da chave. Nunca apresentá-los como identidade verificada. Sem revogação individual enquanto a chave é compartilhada: interromper serviço ou rotacionar chave afeta todos os aparelhos.

- Alpha.19: tela do aparelho conectado deve priorizar cabeçalho compacto com ações principais no topo, resumo visual em grade e atalhos compactos lado a lado. Funções futuras podem aparecer como “Em breve”, mas sem simular implementação.

- Alpha.20: `AppVersion.NAME` deve seguir o `versionName` do Gradle rigorosamente. QR, snapshots e `disconnect` autenticado exigem **igualdade exata** de versão em ambos os aparelhos. Não confundir versão do aplicativo com protocolo `v=1`.
- Alpha.20: `IncomingAccessActivity` em primeiro plano alerta em qualquer área do app e volta ao Monitorado quando o acesso termina; em segundo plano usar apenas notificação Android, sem iniciar tela automaticamente. Identidade/método informados não são verificados; consultas têm expiração de 45s, não conexão permanente. Parada explícita deve permanecer parada.
- Alpha.20: configurações em grade com ícones vetoriais, bateria compacta e ações remotas com toque adequado; não copiar diálogos antigos em lista simples. Rede remota não pode inventar SSID.

- Alpha.21: correção de compilação do workflow 21: no `ScrollView.apply` de `IncomingAccessActivity`, qualificar `this@IncomingAccessActivity.background` para impedir colisão com `View.background: Drawable`; exigir teste de regressão no validador antes de distribuir. Sem alteração de protocolo além da versão obrigatoriamente sincronizada.

- Alpha.22: interface dividida em `UiComponents`, `HomeScreenUi`, `MonitorScreenUi`, `SettingsScreenUi` e `RemoteDashboardUi`, usando extensões internas de `MainActivity`. Manter uma implementação por função e expandir `tools/check_distribution.py` para verificar também módulos externos. Não reintroduzir funções visuais extraídas na Activity sem justificativa. Preservar identidade visual, protocolos, consentimento e comportamento de acessos antes de mudanças arquiteturais mais profundas.


- Alpha.23: `IncomingAccessActivity` deve permanecer enxuta; a UI da tela de acesso recebido fica em `IncomingAccessScreenUi.kt`. Não reintroduzir o pop-up “Quem está?” se as informações puderem aparecer na tela principal; preferir lista/stack inline para acessos adicionais.

- Alpha.24: `MainActivity` coordena ciclo de vida/navegação; estados/consultas ficam em `RemoteConnectionManager`, callbacks em `ConnectionController`, diálogos de pareamento em `PairingScreenUi` e QR em `PairingQrUi`, com worker separado em `PairingController`. Credenciais locais ficam em `MonitorCredentialsUi`.
- Preservar a invalidação de requisições obsoletas ao trocar/desconectar aparelhos. Ao parear por QR ou conectar manualmente, sempre salvar o host selecionado antes de consultar. Não mover chaves para preferências em texto puro, nem autorizar operações remotas sem consentimento.

- Alpha.25: Em `IncomingAccessScreenUi.kt`, dentro de `LinearLayout.apply`, construtores `View` e `LinearLayout` devem receber `this@infoMetric`, não `this` implicitamente. Rodar `tools/check_distribution.py` e compilar o Android após refatorações.

- Alpha.26: preservar painel compacto de três cartões e seis funções remotas com ícones vetoriais nativos. Detalhes técnicos devem ficar em `RemoteDetailsActivity` e usar apenas dados realmente retornados pelo monitorado. Não fingir acesso a arquivos: Recuperar dados permanece "Em breve" até implementar fluxo autorizado de transferência. Nunca coletar/transmitir número de chip sem opção explícita e permissão `READ_PHONE_NUMBERS`. Não adicionar `READ_PHONE_STATE` ou outros acessos implícitos por conveniência.

- Alpha.27: manter a linguagem visual da tela Detalhes do aparelho (cartões claros/escuros, ícones vetoriais, estados curtos e ações lado a lado) também em Home, Monitorado, Conectar e Configurações. `UiComponents.kt` é o ponto central da padronização. Não retroceder para menus nativos sem necessidade funcional; não comprometer o QR temporário, FLAG_SECURE nem a cópia explícita e sensível de credenciais.

- Alpha.28: usar `showManagedDialog` ou `showAppNotice`/`showAppConfirm` para diálogos de `MainActivity`, nunca abrir dois diálogos internos simultaneamente. Erro em campo manual deve ser exibido inline sem descartar campos.
- Mensagem de versão incompatível autenticada do servidor deve permanecer cifrada com a chave já validada. Nunca revelar versão a clientes sem chave válida; não afirmar que EOF prova incompatibilidade. `ConnectionIssue` descreve hipóteses e o diagnóstico copiado não inclui segredo.

- Alpha.29: `MonitorScreenUi.showIncomingAccessActions` cria o `AlertDialog` uma única vez e chama `showManagedDialog(dialog)` uma única vez. Evitar a terminação `.create().let { showManagedDialog(it) }` em conjunto com novo `.create()`. O validador exige essa invariável após o erro `:app:compileDebugKotlin` no workflow 29.

- Alpha.30: compartilhar SMS, permitir SMS enviados e responder notificações são três opções independentes inicialmente desligadas. A consulta SMS é sob demanda (`includeSms=true` só quando Mensagens é aberta), sem RECEIVE_SMS e sem persistência; a leitura depende de READ_SMS, que pode ser restrita por instalador. O comando de resposta SMS usa SEND_SMS e número já visto na inbox, confirma no controlador, limita taxa/corpo e não garante entrega. RemoteInput somente em ação ativa e permitida, token único com expiração, e confirmação do controlador. Comandos cifrados no mesmo protocolo e chave, com proteção de replay e versão exata. Bloquear códigos, apps financeiros e parar ao revogar; nenhum acesso furtivo ou bypass das proteções Android.

- Alpha.31: novas permissões SMS (`RECEIVE_SMS`, `READ_PHONE_STATE`) e opção ampla de notificações são consentidas separadamente no monitorado e ficam desativadas por padrão. Nunca tentar contornar hard restricted READ_SMS. Mensagens SMS ao vivo são temporárias e consultadas quando Mensagens abre/atualiza, não transmitidas em push oculto. Limite de snapshots, filtros financeiros/OTP, escolha validada de SIM ativo, botão Copiar explícito. Não usar `FLAG_SECURE` em Mensagens, mas **sempre** proteger credenciais e QR; envio exige revisão e confirmação no controlador, sem comandos em lote. MMS inclui apenas metadados quando provider concede. Resultados de entrega SMS podem ser incertos quando operador não confirma. Conservar chave autenticada e versão exata entre aparelhos.

- Alpha.32: Configurações ocupam um módulo de navegação (`mode=3`) com categorias; não devolver um `AlertDialog` único longo. Toda seção retorna às categorias antes de sair do módulo, preservando tela anterior e preferências. O serviço foreground monitora o listener TCP efetivo e recria apenas o servidor em caso de falha, com backoff e diagnóstico. `START_STICKY` cobre recriação oportunista após morte de processo, mas nunca prometer reinício após force-stop pelo usuário ou políticas do fabricante. Parada explícita permanece parada; não criar alarmes agressivos nem novos acessos silenciosos. Conservar o protocolo criptografado e versão exata.

- Alpha.33: Mensagens é uma página de controle (`mode=4`) com abas SMS/Notificações, sem AlertDialog gigante. Não persistir mensagem recebida em Bundle/disco. Respostas continuam exigindo confirmação. A fonte deve filtrar OTP em PT/EN (incluindo “authentication code”), EventJournal revalida antes de transmitir e a UI filtra novamente antes de mostrar/copiar. Não remover FLAG_SECURE de QR/credenciais.

- Alpha.34: `Ver tela` somente após consentimento do Android no dispositivo monitorado. Um token MediaProjection por sessão, foregroundServiceType `mediaProjection`, notificação e botão Parar local. Sem capturar/retomar no boot ou depois de parada; não adicionar Accessibility nem injeção de toque. `ScreenFrameStore` é volátil e limitado a 1 quadro/s / 110 KiB / expiração 3 s; `screenFrame` usa a mesma chave, AES-GCM, nonce antirreplay e versão exata. O controlador só consulta enquanto a Activity estiver visível. Sem áudio, gravação ou persistência. Testar no Android 14+ (token único e consentimento por sessão), rotação e bloqueio da tela.

- Alpha.35: permissão de toque pelo AccessibilityService somente depois de habilitação **manual** e preferência explícita, sem varredura de UI. A sessão MediaProjection aprovada permanece entre reconexões, mas nunca se reinicia após ser encerrada ou bloqueada. AudioPlaybackCapture interno opt-in, RECORD_AUDIO, sem microfone ou captura de chamadas, em chunks PCM em RAM. A notificação de pedido de tela não substitui consentimento e é limitada por taxa. Comandos por AES-GCM+nonce/replay e versão exata, sem nova porta aberta. Parada local encerra a projeção. Testes físicos e build Android ainda necessários.

- Alpha.36: tela Ver tela segue conceito enviado, com cabeçalho compacto e ícones, painel arredondado e controles compactos. Botão de print salva apenas após ação do usuário e com frame recente; Foto HD requer MediaProjection ativa consentida, é autenticada pelo mesmo canal e limitada a 480 KiB, com captura única em memória. Imagens são persistidas apenas no controlador. Modo imersivo nunca remove saída e deve preservar mapeamento FIT_CENTER dos toques. Testar rotações, Android 8–9 e Android 10+, tempo limite, autorizações e tema escuro.

- Alpha.37: o workflow 37 falhou em `:app:compileDebugKotlin`. `RemoteClient.kt` deve importar explicitamente `android.util.Base64` antes de decodificar capturas; `RemoteScreenActivity.kt` deve usar `ScrollView.isFillViewport = true` (e não `fillViewport`). `tools/check_distribution.py` contém regressões para ambos; teste estático não substitui compilação Gradle.

- Alpha.38: preservar a recuperação agendada SOMENTE após habilitação explícita no monitorado; parada deve cancelar o Job. JobScheduler não isenta do bloqueio Android 12+ para iniciar FGS em segundo plano. Evitar relançar MediaProjection, áudio ou acessibilidade automaticamente; sem promessas de reinício após force-stop.

- Alpha.39: botões Voltar/Início/Recentes/Notificações disparam ações globais independentes do toggle de toque na imagem; exigem projeção consentida e AccessibilityService autorizado no monitorado. Sempre validar no servidor e não contornar bloqueio seguro. A tela Ver tela aplica WindowInsets systemBars/displayCutout fora da tela cheia e imersão restaurável quando fullscreen. Preencher usa CENTER_CROP com transformação inversa das coordenadas; ajustar usa FIT_CENTER. Prévia JPEG com alvo 330 ms (700 ms no battery saver), consultas sem fila e diagnóstico do tempo; não declarar H.264 nem garantia de FPS. Preservar concessão MediaProjection por sessão, chaves AES-GCM, print manual e ausência de captura oculta.

- Alpha.40: AVC via MediaCodec somente após sessão MediaProjection visível e autorizada. Ring de no máximo 30 quadros AVC em RAM, pacotes máximos de 128 KiB, alvo 10 FPS sem promessa de desempenho ou transporte persistente, retorno a JPEG em codecs incompatíveis. Uma única VirtualDisplay por projeção; imagens HD sob demanda e áudio mantidos. `wakeScreen` somente com chave AES-GCM pareada, opção `allowRemoteWake` local OFF, 12 s entre tentativas e wake lock 1,5 s; nunca remover Keyguard, PIN, biometria ou consentimento. Build Android/ensaio físico pendentes.

- Alpha.41: tela cheia de Ver tela mantém barra flutuante superior (Sair, Ajustar/Preencher, Print, Áudio, Foto HD) e inferior (Voltar, Início, Recentes, Notificações, Toque, Acender), fundo translúcido sem bloquear gestos fora do controle. Ações reutilizam autorização local e AES-GCM; exibir status de codec e rede real sem fingir FPS, restaurar fullscreen após rotação, respeitar cutout/insets e preservar toque coordenado em FIT_CENTER/CENTER_CROP. Compilar e testar em aparelho antes de afirmar fluidez/compatibilidade.

- Alpha.42: ADB é cliente TLS separado do protocolo AES-GCM. Usar somente IPv4 privado e Depuração sem fios consentida localmente. QR ADB leva serviço e segredo, expira em 2 minutos e não é o QR normal do app. Nunca habilitar fallback A_AUTH/CNXN sem TLS. Não persistir códigos/QR nem registrar segredos; identidade RSA cifrada pelo SecretVault. Preservar licenças e alterações do adb-core. Sem shell livre, boot, captura ADB ou bypass de bloqueio. Ao sair da tela, encerrar sessão; teste físico obrigatório antes de declarar pareamento validado.

- Alpha.43: telas de diagnóstico ADB permitem screenshots sem QR temporário; proteger QR com FLAG_SECURE apenas enquanto visível. Instruir direção de leitura (monitorado lê QR do controlador), diferenciar portas de pareamento/conexão e mostrar modelo reportado pelo telefone após ADB conectado. Em Ver tela, JPEG é padrão de menor atraso, H.264 é opção experimental; ao perder fila AVC, saltar para IDR recente sem drenar muitos P-frames antigos. Não declarar latência garantida sem testes em aparelhos.

- Alpha.44: erro `:app:compileDebugKotlin` no workflow 43 por `runOperation("...") { ... }`. Como `onSuccess` é o último parâmetro lambda da função, a lambda final foi inferida como `(String) -> Unit`, e `pair/execute` não pertencem a String. Usar sempre `operation = { adb -> ... }`, inclusive com callback nomeado `onSuccess = { ... }`. Validador confere a regressão; verificar Gradle/Android em CI.

- Alpha.45: Recuperar dados é leitura SAF por pasta autorizada **no próprio monitorado**, com permissão persistida e opção de revogação. Nunca usar `MANAGE_EXTERNAL_STORAGE`, shell ADB, bypass do Android ou varredura de dados de apps. Só controlar arquivos dentro da árvore SAF previamente escolhida, com caminho relativo validado por reenumeração de filhos, AES-GCM/autenticação e versão exata. Transferir blocos de 48 KiB, até 32 MiB por arquivo, máximo 150 itens por pasta/12 níveis; cliente escolhe destino com ACTION_CREATE_DOCUMENT. Verificar integridade dos blocos e alterações do arquivo, cancelar sem deixar gravação intencional no monitorado. Não prometer recuperar dados apagados; build e testes físicos pendentes.

- Alpha.46: ao chamar `TextView.setTextColor` dentro de `TextView.apply` ou `Button.apply`, nunca usar o getter `foreground` implicitamente: `View.foreground` é `Drawable`. Em `RemoteFilesActivity` usar `this@RemoteFilesActivity.foreground` (`Int`). Manter regressão no verificador estrutural e compilar Kotlin no GitHub Actions.

- Alpha.47: Modo avançado ADB abre assistente por papel: controlador gera QR/código, pareia e só depois conecta; monitorado deve autorizar a Depuração sem fios no Android. Não confundir `pair` com sessão `connect` nem alegar que o monitorado consegue verificar outros clientes ADB por esta tela. Estado Conectado somente se `AdbClient.connected()`, indicativo de modelo é declarado pelo Android, não prova criptográfica de identidade. `onSuccess` de runOperation ocorre quando a operação concluiu sem exceção, inclusive `pair` sem conexão ainda. Ao sair, limpar QR, cancelar sessões e proteger segredo com FLAG_SECURE somente enquanto QR visível.
- Alpha.49: aplicar visual moderno no Modo avançado ADB, com resumo inicial em cartão, chip de estado, ação principal dinâmica e seção **Mostrar/Ocultar configurações avançadas** recolhida por padrão. No fluxo “Permitir controle deste celular”, preferir instruções curtas em passos numerados e oferecer atalho para abrir Configurações/Opções do desenvolvedor. Manter a distinção entre pareamento e conexão ativa e não esconder a natureza experimental do ADB.

- Alpha.49: manter a lista **Conectados anteriormente** no Modo avançado ADB, salvando apenas aparelhos com conexão ADB confirmada. Reconexão rápida deve reutilizar último IP/porta salvos sem fingir identidade verificada; o histórico é conveniência local, não prova criptográfica. Preservar ação **+ Adicionar outro celular**, painel **Configurações avançadas** recolhido por padrão, toque longo para esquecer aparelho salvo e distinção clara entre pareamento, reconexão e sessão ativa.

- Alpha.50: tela ADB deve manter apenas dispositivos salvos, Reconectar, Adicionar outro celular e Configurações avançadas recolhidas. Escolher papel, método, QR, código e orientação do monitorado devem abrir pop-ups centralizados. Depois de pareamento QR/código, buscar somente `_adb-tls-connect` no IP proveniente do pareamento. Em mDNS ambíguo/ausente oferecer conexão manual, não escolher serviço de outro IP. Pareamento não equivale a conexão, nunca apresentar identidade criptograficamente confirmada pelo nome/IPv4. Proteger QR com FLAG_SECURE e manter sessão encerrada ao sair.

- Alpha.51: reconexão automática ADB apenas para a sessão escolhida, com Activity visível, heartbeat 30 s e timeout 8 s, espera 2/4/8/15/30 s; não reativar Depuração sem fios desligada, não varrer IPs nem assumir identidade por mDNS. Botão Cancelar interrompe. `onStop` fecha conexão; não alegar serviço em segundo plano nem melhoria de FPS. Atalho Comandos ADB limitado a navegação e volume. Testar perda/reentrada Wi-Fi, porta dinâmica, revogação, multi-aparelhos, economia de bateria e parada do usuário.

- Alpha.52: entrada "Ver tela remota • controle ADB" só depois de ADB TLS ativo e chave AES-GCM normal presente para o mesmo host; sessão ADB explicitamente transferida por `AdbScreenBridge` apenas enquanto o visualizador estiver em primeiro plano e encerrada em onPause. `AdbScreenRouting` permite ADB somente para Voltar/Início/Recentes após resposta autenticada do canal de imagem. Indicar claramente **Controle ADB** versus **Imagem Monitor Remoto/MediaProjection**. Toque, áudio, vídeo e notificações continuam dependentes dos caminhos consentidos anteriores. Não alegar streaming via ADB nem auto-reconexão dentro do visualizador nesta revisão; testar hardware antes de prometer uso pleno.

- Alpha.53: Manutenção nos cartões principais abre diálogo com apenas Reiniciar via ADB. Exigir ADB ativo para o mesmo host, confirmação digitada REINICIAR; serviço `reboot:` sem modo recovery/shutdown e sem repetição em falha/EOF. Nunca reiniciar por conexão AES-GCM comum nem prometer auto-reconexão após reboot. MainActivity não pode abrir IncomingAccessActivity automaticamente: notificação e cartão Monitorado mostram acessos autenticados, detalhes apenas por toque; botão Voltar deve estar sempre disponível na tela manual. Recuperar dados mantém autorização SAF explícita e botão Voltar/erro com instruções.

- Alpha.54: o workflow 52 falhou em `:app:compileDebugKotlin`, `MainActivity.kt:192:9 Unresolved reference lastRecoveryAttemptMs`. Manter uma única declaração `private var lastRecoveryAttemptMs = 0L` junto ao estado da Activity, com `SystemClock.elapsedRealtime()` para limitar retomadas do serviço previamente autorizado. O verificador estrutural deve exigir esta declaração para evitar regressão. Preservar demais funcionalidades da alpha.53.

- Alpha.55: `AdbPolicyTest.commandAllowlist` deve validar a enumeração e os comandos shell exatos dos 7 membros atuais de `AdbPolicy.Command`, em vez de confiar na contagem antiga de 4; preservar proibição de `tcpip`, `screencap`, `poweroff`, `shutdown`, `reboot` via shell e outras operações não previstas. Reinício normal continua disponível apenas pelo serviço ADB `reboot:` com confirmação explícita e sem repetição. Workflow 53 chegou aos testes e falhou nesse teste unitário; novo build é necessário para validar a correção.

- Alpha.56: manter espaçamento mínimo vertical de 9dp e horizontal de 10dp em pares de ações ADB e diálogos. Não duplicar o celular conectado no histórico; renomear o ativo diretamente e preservar apelido quando diagnóstico não trouxer modelo legível. Reconstruir cartões apenas ao mudar os dados, não no polling de 2s. Visualizador oferece seletor segmentado JPEG (padrão) / H.264 (experimental); mensagens de toque diferenciam captura encerrada, tela apagada, bloqueio e Acessibilidade. ADB não remove PIN/biometria nem substitui consentimento MediaProjection. Não afirmar que ajustes de interface resolvem quedas de Wi-Fi sem testes físicos.

- Alpha.57: Manutenção do painel remoto mostra cartão do aparelho e alerta compacto; a confirmação final ADB ainda exige digitar REINICIAR e usa somente `reboot:`. Aviso ADB não configurado segue a linguagem visual do app e nunca envia comandos por conexão comum. Recuperar dados é um mini gerenciador SAF SOMENTE LEITURA: topo com voltar/atualizar/ajuda, pasta, lista/grade, busca/ordem/infos, seleção múltipla e cópia para árvore de destino do controlador escolhida pelo usuário. Limites 20 arquivos por lote / 128 MiB agregado / 32 MiB individual; evitar sobrescrever com sufixo, tentar excluir cópia parcial em erro; nada de apagar/mover/renomear origem. Preservar AES-GCM, hashes, permissões locais, proteção de caminhos. Build real e ensaios SAF pendentes.

- Alpha.58: Notificações passam a ter tela própria com cartões no estilo Detalhes, atualização autenticada sob demanda e indicadores separados de permissão, listener e filtros. `NotificationCaptureService` recarrega somente notificações **ainda ativas** após o listener reconectar e solicita rebind apenas quando monitoramento autorizado. Não criar armazenamento histórico persistente nem incluir notificações sensíveis no snapshot.
- Alpha.58: novo catálogo de aplicativos **visíveis no lançador**, sob autorização local desligada por padrão. Endpoint `appsList` cifrado/authenticated, somente leitura, até 120 nomes e pacotes, sem `QUERY_ALL_PACKAGES`, nem iniciar, desinstalar ou extrair dados dos apps.

- Alpha.59: `BatteryReader` envia estados e fonte de `ACTION_BATTERY_CHANGED` com indicadores explícitos em snapshot autenticado; não inferir descarga a partir de `NOT_CHARGING` nem inferir fonte a partir de corrente. `BatteryTelemetry` é puro e possui testes JVM; manter `charging`/`full` antigos. Cartão compacto mostra estado/fonte e ⚡ somente com confirmação. `RemoteDetailsActivity` não deve usar fallback falso “Descarregando”. Tocar som e Lanterna continuam na fila; não introduzir comando remoto sem controle local, consentimento e testes.

- Alpha.60: Tocar som e Lanterna usam somente operações AES-GCM `deviceControl` explicitamente permitidas, com `status`, `soundStart`, `soundStop`, `torchOn`, `torchOff`. Nenhum comando ADB, câmera aberta, gravação ou permissão invasiva. Duas autorizações locais independentes, OFF por padrão. Alerta em STREAM_ALARM por 5/10/20/30 segundos, parar manual e ao revogar/desativar serviço, sem subir volume ou ignorar Não Perturbe. Lanterna via CameraManager.setTorchMode/CameraManager.TorchCallback, estado real `ligada/desligada/indisponível/desconhecida` sem assumir sucesso imediato; ao parar serviço desligar apenas se este serviço a tiver ativado. Falhas de CAMERA e câmera ocupada devem ser comunicadas; teste de hardware e build Android são requisitos antes de afirmar compatibilidade plena. Próxima alpha.61.

- Alpha.61: bloquear tela somente com `DevicePolicyManager.lockNow` após `ACTION_ADD_DEVICE_ADMIN` concedido localmente para Receiver estável com SOMENTE `<force-lock/>`, senha/PIN/padrão configurados e preferência `remote_lock=false` por padrão. O controlador exige digitar BLOQUEAR; endpoint `deviceControl` exige confirmação exata e rate-limit 15s. É solicitação aceita, não comprovação do bloqueio. Não adicionar resetPassword, wipeData, shell ADB ou ativação automática de administrador. Usuário pode revogar no sistema e no app. Build/hardware pendentes. Próxima alpha.62.

- Alpha.62: transmissão de câmera JPEG somente via Activity `CameraShareActivity` visível no próprio monitorado, iniciada localmente após confirmação e permissão CAMERA. Não adicionar captura em segundo plano, serviço FGS de câmera, boot, comando remoto de ativar câmera, microfone ou gravação. Camera2 usa prévia local e ImageReader YUV, JPEG reduzido em RAM, limite de 110 KB e 1 quadro/s. `CameraFrameStore` fecha ao sair/revogar, e `cameraFrame` aceita apenas consulta autenticada AES-GCM com versão exata. No controlador, `RemoteCameraActivity` consulta quadros apenas enquanto visível e só salva foto após toque. Troca frontal/traseira ocorre localmente; câmera indisponível/ocupada e permissão recusada não geram imagem inventada. Android 14+ exige respeitar restrições de câmera while-in-use. Build completo/hardware pendentes. Próxima alpha.63.

- Alpha.63: correção determinística de orientação dos quadros NV21, rodando Y/VU 0/90/180/270 conforme SENSOR_ORIENTATION, Surface.ROTATION_* e lente frontal/traseira antes de comprimir JPEG. Não espelhar selfies silenciosamente nem adicionar metadados falsos. CameraFrameStore exige token por abertura e rejeita callback tardio da geração anterior; fechar Activity revoga sem re-stop tardio em onDestroy. MonitorService.onDestroy apaga o frame store e MonitorServer.cameraFrame exige serviço em execução além de monitoramento autorizado. Limites JPEG 110 KB/1 quadro por segundo, origem local e somente RAM mantidos. Testes Kotlin puros, PSI e análise estrutural não substituem build Gradle nem teste em celulares reais. Próxima alpha.64.

- Alpha.64: conversão JPEG da câmera somente com `CameraFrameStore.hasRecentViewer()` após `viewerPing` por pedido de `cameraFrame` autenticado no servidor; a demanda expira em 4,5s e é zerada em `begin/stop`. Nunca usar esse ping para abrir Camera2 remotamente. Cliente envia `afterSequence`, servidor omite `jpeg` se o número de sequência for o mesmo, e retorna `active/lens/age/sequence` de snapshot sincronizado. UI apresenta duração real das consultas, tamanho do último JPEG e intervalo entre quadros novos, sem chamar essas métricas de latência ponta a ponta ou FPS de hardware. Manter troca de lente exclusivamente no monitorado; não adicionar câmera em background, notificações ocultas ou controles remotos sem revisão de segurança. Próxima alpha.65.

- Alpha.65: novo `cameraSwitch` **somente** troca frontal/traseira em sessão `CameraShareActivity` já visível, após opt-in local temporário OFF por padrão no CheckBox da própria Activity. Nenhuma permissão persistente, serviço de câmera em background ou comando de abertura remota. `CameraFrameStore` exige sessionToken retornado por `cameraFrame`, lente em allowlist, controlador recentemente consultando, opt-in ativo e rate-limit. O comando remoto indica solicitação aceita, **não confirmação de troca**; o controlador deve verificar o novo `lens`/`sessionToken` no polling. Limpar pedidos em `begin/stop/revoke`, controlar callbacks obsoletos e revogar ao pausar/parar monitoramento. Na alpha.66, priorizar build real/testes físicos e correção de regressões antes de gravação de vídeo.

- Alpha.66: GitHub Actions run 61 (tentativa 2, 15/10/2026) falhou em `:app:compileDebugKotlin`, `CameraShareActivity.kt:83:70 Assignment type mismatch`: `TextureView(this).apply { surfaceTextureListener = this }` resolve `this` ao próprio TextureView, não ao `TextureView.SurfaceTextureListener`. Corrigido para `this@CameraShareActivity`, seguindo o padrão de `QrScanActivity`. `tools/check_distribution.py` agora verifica o receiver explícito. Sem novas permissões/protocolo. Antes de declarar build bem-sucedido, reexecutar CI com alpha.66.

- Alpha.67: execução 62 do workflow `Gerar APK e Release` compilou `:app:compileDebugKotlin` (corrigindo o erro alpha.66), mas falhou em `:app:testDebugUnitTest` com 105 testes e 5 falhas em `CameraFrameStoreGenerationTest`: chamadas a `android.os.SystemClock.elapsedRealtime()` em JVM sem API Android executável. `CameraFrameStore` usa agora relógio monotônico relativo ao processo com `System.nanoTime()` e sentinela positivo, preservando expiração, tokens, autenticação e bloqueio de troca de câmera sem opt-in. Os testes JUnit reais via Gradle devem ser reexecutados na alpha.67 antes de afirmar aprovação CI.


- Alpha.68: a experiência de Configurações deve manter textos mais legíveis, cartões altos e selos de estado/ação visíveis (`ATIVADO`, `DESATIVADO`, `ABRIR`, `REVOGAR`). Confirmações locais devem usar diálogos próprios do app com botões grandes; evitar depender somente de subtítulos pequenos ou pares “(ativar)/(desativar)” para explicar o estado.

- Alpha.69: corrigida compilação da alpha.68 no workflow 64, `:app:compileDebugKotlin`, envolvendo atribuição proibida a `TextView.lineSpacingExtra` em quatro pontos e `MainActivity.darkTheme()` privado chamado por extensões fora da classe. Usar `setLineSpacing(extraPx, 1f)` e `internal fun darkTheme()`; garantir verificador de regressão em `tools/check_distribution.py`. Nenhuma mudança em permissões, criptografia ou comandos remotos. Não afirmar build até CI executar a alpha.69.

- Alpha.70: selos de Configurações usam exclusivamente `SettingsBadgeKind` com estado explícito, e não inferência de palavras nos subtítulos. Ao adicionar nova opção booleana, passar `badge = SettingsBadgeKind.forEnabled(valorReal)`. Revogação perigosa mantém prioridade visual. `CameraShareActivity` deve fechar o token que possui com `CameraFrameStore.stop(ownedSession)` e não chamar `CameraFrameStore.stop()` global em callbacks ou na pausa; somente serviço/ações globais revogam tudo. O polling da tela deve permanecer com executor reutilizável, um quadro de cada vez, resultado de visita antiga descartado. Compilação Gradle e testes físicos pendentes; próxima alpha.71.

- Alpha.71: workflow GitHub Actions `Gerar APK e Release` #66 (15/10/2026) falhou em `:app:compileDebugKotlin`, `RemoteScreenActivity.kt:472:107` com "Type checking has run into a recursive problem". A causa foi `main.post(poll)` durante a inferência do tipo da propriedade `private val poll = object : Runnable`. Declarar `private val poll: Runnable` e reagendar a instância capturada `main.post(this)` dentro do callback. Não reintroduzir referência autorrecursiva no inicializador. Verificador estrutural mantém esta regressão; nenhuma mudança em consentimento, criptografia, rede, câmera ou captura. Nova compilação CI obrigatória antes de afirmar APK funcional. Próxima alpha.72.

- Alpha.72: snapshots agora incluem `deviceState` (tela/bloqueio, câmera apenas como indício, app visível só com Usage Access), e `network` opcionalmente inclui SSID/RSSI quando ACCESS_FINE_LOCATION e localização estão ativas. Não alegar gravação do Minha Câmera a partir de CameraManager. As faixas de temperatura são indicativas. Transferência de arquivos preserva SAF e autorização local, verifica SHA-256 completo, evita ambiguidade por nomes repetidos, limita cópia de pastas e reduz sockets com operação `dataChunks` (dois blocos por solicitação). Sem retomada persistente/serviço de transferência ainda. Próxima versão alpha.75.

- Alpha.73: cartão de telemetria clicável informa permissões mas não concede direitos remotamente; célula local Diagnóstico leva às autorizações Android no próprio monitorado. `VersionNewsUi` marca versão reconhecida por preferência apenas após toque Entendi. `HealthAlertPolicy` usa limiares orientativos/histerese; `HealthAlertMonitor` notifica localmente e comunica último evento por snapshot AES-GCM. `ControllerHealthAlerts` deduplica por host e sequência, `RemoteHealthJobService` opcional usa JobScheduler não garantido a cada 15 min. `thermalProtection` OFF por padrão somente interrompe capturas próprias em estado severo; não controlar nem afirmar gravação/pausa do Minha Câmera. Permissão POST_NOTIFICATIONS permanece explícita, monitoramento NÃO contorna Doze nem force-stop. Renomear atua apenas em RecentDevices e preserva credencial. Build Android e testes reais obrigatórios antes de afirmar funcionamento. Próxima alpha.74.

- Alpha.75: Localização somente após sessão iniciada localmente na tela Configurações > Localização; serviço `LocationShareService` FGS do tipo location, notificação persistente e ação Parar. Nunca acionar GPS a partir do controlador, receptor de boot ou polling. As coordenadas ficam só em RAM e a sessão desaparece ao encerrar serviço/monitoramento, revogar permissão ou processo. `LocationShareStore` distingue sem permissão, Android sem localização, aguardando, leitura antiga e expirada; não fingir precisão, atualização instantânea, histórico ou mapa offline embutido. O snapshot AES-GCM leva posição apenas durante sessão válida. Conceder chave a vários controladores lhes concede a mesma autorização enquanto a sessão estiver ativa; não afirmar autorização individual. Android 14+ exige FGS location iniciado de Activity visível, e Android 13+ exige POST_NOTIFICATIONS no fluxo local. Próxima alpha.76; build Gradle e testes físicos obrigatórios.


- Alpha.76: ícone do app deve ser adaptativo, com escudo e notificação monocromática própria. O MonitorService apresenta apenas acessos autenticamente consultados nos 45 segundos anteriores como **acessos recentes**, nunca conexões permanentes. Nomes de controladores são autodeclarados e jamais podem ser apresentados como identidade verificada; tela bloqueada usa versão pública genérica. Estado dos módulos deve vir de ScreenFrameStore, CameraFrameStore, LocationShareService/Store e preferências reais. Atualizações da notificação precisam ser deduplicadas, sem reiniciar FGS ou alertar repetidamente. Ações Parar e Detalhes devem continuar funcionais. Sem alteração de protocolos/permissões. Próxima alpha.77.

- Alpha.77: logs do workflow GitHub Actions #72, `:app:compileDebugKotlin`, revelaram referência inexistente `lastControllerName` no `MonitorService.onDestroy()`, após refatoração da notificação na alpha.76. Remover somente essa atribuição; manter `lastControllerSeenMs = 0L` e `AuthenticatedAccess.clear()`. `tools/check_distribution.py` deve impedir retorno da referência inexistente. Sem mudar política de autenticação, notificações, GPS, privacidade, criptografia ou permissões. Build Gradle completo deve ser validado em CI antes de declarar APK. Próxima alpha.78.

- Alpha.78: aba Aplicativos usa `appsIcons` em lote de no máximo 12 com origem PackageManager e tamanho limitado; `appLaunch` exige consentimento separado no monitorado + MainActivity visível, sem burlar limites de inicialização em segundo plano. ADB para aplicativos usa somente `RemoteAppsPolicy.command` (pacote estrito, ações `open`/`stop`/`settings`), mesmo host e confirmação digitada na AdbActivity. Parada forçada nunca segue fallback automático, nem afeta apps críticos/Monitor Remoto. UI não deve afirmar confirmação de abertura/parada ao receber ACK. Os favoritos não salvam conteúdo de aplicativos. Próxima alpha.79.

- Alpha.79: SharedDataFolder.writable requer permissão persistida READ+WRITE mais SettingsStore.allowRemoteFileWrite opt-in OFF por padrão. Somente operações allowlist dataManage/dataUpload* cifradas e caminhos relativos resolvidos dentro da árvore SAF. Não estender a raízes Download, Android/data ou pastas privadas. Upload usa arquivo em cache privado com digest SHA-256 e não sobrescreve nomes; abortar/apagar estágio após fim e limpar ao parar serviço. Operações destrutivas exigem confirmação no controlador; a chave compartilhada não individualiza permissões. Pausar só enquanto Activity existe; retomada persistente e FGS de transferência ainda não implementadas. Arquivos de até 2 GiB; leitura em blocos de 192 KiB, sem alegar ganho medido nem compilação/hardware validados. Próxima versão alpha.80.

- Alpha.80: download SAF `dataStream` mantém uma única sessão TCP autenticada, enviando mensagens AES-GCM por blocos de até 192 KiB com offset e SHA-256 final. Não reabrir o SAF por chunk nem recriar o socket. Limitar fluxos longos simultâneos; recusar caminho fora da árvore e copiar sem acesso permitido. O novo StreamFrameReader conserva bytes pré-lidos entre frames e lança EOF quando a transferência interrompe; sempre rejeitar parcial sem digest final. Upload ainda é separado e limitado, e pausas vinculadas à Activity não são retomadas após fechar o app. Build Android e teste de throughput reais ainda necessários. Próxima alpha.81.

- Alpha.81: download `dataStream` recebe ACK JSON autenticado e quadros binários AES-GCM do `FastFileStream`, removendo duas camadas de Base64; cada quadro usa nonce aleatório, desafio de 16 bytes por sessão no AAD, offset sequencial, limites de tamanho e digest SHA-256 final. Não simplificar para TCP puro nem dispensar integridade. Arquivo SAF e socket únicos, apenas uma transferência em paralelo. Tela de cópia maior e com insets do Android; atualizar métricas sem exigir evento por byte nem carregar arquivo todo. Pausa vinculada à tela, sem resume persistente. Novos tests FastFileStreamTest. Não alegar aceleração numérica nem APK até testes/CI. Próxima alpha.82.

- Alpha.82: integra a alpha.81 de transferência binária já publicada com o mapa interno (não sobrescrever uma linha paralela). Tiles OSM só com opt-in local por aparelho, cache e atribuição, nunca bulk/offline. Nova consulta periódica não inicia GPS; não alterar autenticação/versão exata, parar ao sair da Activity. Nova versão sequencial alpha.83.

- Alpha.83: o endpoint `photoTake` requer par autorizado AES-GCM, versão exata e PhotoReadyService armada antes da captura via `RemotePhotoSetupActivity` visível no monitorado. Câmera Camera2 JPEG única sem stream e sem armazenamento remoto, notificação pública com ação Parar, OFF após parada, reinício/force-stop, NUNCA iniciar por boot/polling ou request remota. Não tentar contornar while-in-use do Android 14+, nem prometer resposta após furto sem VPN/LAN e serviço ativo. Próxima alpha.84.

- Alpha.84: UI da câmera no controlador segue tema, tem status de captura, zoom local, botões Salvar/Compartilhar (compartilhar salva foto local somente após toque), decodificação de prévia em escala e no worker. A foto única e o opt-in pré-armado no monitorado continuam idênticos; não introduzir auto-save, histórico oculto, câmera iniciada remotamente, novas permissões ou polling de vídeo. Controle de salvar, compartilhar e lens não deve disparar segundo pedido enquanto captura em andamento. Testes Android e em aparelhos permanecem pendentes. Próxima alpha.85.

- Alpha.85: o histórico de arquivos é estritamente local, limitado a 48 operações dos últimos 30 dias; não gravar nomes, caminho, IP, tokens nem conteúdo. Registrar término/parcial/falha sem afirmar sucesso por mera requisição. Painel remoto diferencia explicitamente dados de cache de consulta autenticada; GPS antigo/indisponível não finge leitura recente. Nenhuma retomada após fechamento da Activity foi implementada; não a anunciar nem abrir transferências paralelas. Preservar 2 colunas de funções e as permissões remotas existentes. Próxima alpha.86.

- Alpha.86: retomada persistente SOMENTE para Downloads MediaStore no controlador, por arquivos parciais em noBackupFilesDir com 7 dias de retenção. Usuário inicia novamente ao escolher o mesmo arquivo; nenhuma transferência invisível continua após fechar Activity. Servidor recebe offset múltiplo de 192 KiB, mas calcula hash SHA-256 DO ARQUIVO INTEIRO (inclusive prefixo não retransmitido); cliente calcula hash de prefixo local, verifica digest integral antes de publicar em MediaStore (IS_PENDING). Não confiar apenas em tamanho/data. Não retomar escrita em provedores SAF arbitrários nem em uploads sem protocolo adequado. Distinguir progresso do transporte e publicação final; avisar sobre duplicação transitória de armazenamento. Compilação Android/testes físicos pendentes. Próxima alpha.87.

- Alpha.87: autorização prévia e local de **Recuperação automática da localização** (OFF por padrão) permite coordenadas em sessões subsequentes sem toque a cada consulta. Reboot/recriação só tenta GPS se ACCESS_BACKGROUND_LOCATION e monitoramento/autostart foram concedidos; nunca iniciar GPS por requisição remota ou ignorar limites do Android. Notificação permanente + ação Parar desarmam recuperação, assim como encerrar monitoramento. GPS e transmissão não funcionam sem permissão, Android Location ON, FGS e rede LAN/VPN. `RemoteMapView` sempre pode desenhar mapa de referência sem fingir posição do remoto, com OSM somente após autorização de blocos online. Status de rede pode preservar último ponto na mesma tela somente se identificado como ANTIGO; nunca mostrar como atual. Compilação Android/teste físico pendentes; próxima alpha.88.

- Alpha.88: o painel usa 12 funções em três colunas, com Detalhes no cabeçalho; Configurações têm índice pesquisável por categorias sem alterar o caminho de concessão de permissões. Avisos/confirmacões centrais devem preservar botão Cancelar nas ações sensíveis e mensagens legíveis nos temas. RTT da consulta autenticada é medido com relógio monotônico, não substitui a hora da leitura remota e não confirma conexão persistente. Nunca voltar à grade de duas colunas no painel de funções.

- Alpha.89: pasta fixa Downloads/Monitor Remoto no Android 10+ para arquivos próprios do app via MediaStore. `FixedDownloadsFolder` nunca deve enumerar arquivos sem `OWNER_PACKAGE_NAME` correspondente nem ler Downloads de terceiros. Pareamento AES-GCM obrigatório, operações destructive confirmadas no controlador, e nenhuma escolha remota de pasta SAF. Upload usa staging em cache e SHA-256; download valida integridade antes de publicar. Não afirmar acesso universal a Downloads. No Android 8–9, indicar ausência de suporte ao modo fixo. Diálogos seguem temas e menus de arquivos usam painel inferior. Versão seguinte alpha.90.
