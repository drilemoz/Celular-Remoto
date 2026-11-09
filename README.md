<!-- alpha.142 -->
**Correção de compilação (alpha.142):** corrigidos três testes Kotlin em `BatteryPresentationMathTest.kt` usando `requireNotNull()` antes de `assertEquals(Double, Double, delta)`, conforme falha do workflow 32. Coleta, armazenamento e exibição de corrente permanecem os mesmos da alpha.141. A execução integral do build da alpha.142 em GitHub Actions continua pendente.
**Bateria mais clara (alpha.141):** a aba Bateria separa leitura atual, ferramentas/alertas e resumos automáticos para reduzir confusão visual. Corrente e potência continuam líquidas na bateria; quando o Android reportar valor positivo durante descarga, a interface mostra visualmente o sentido de saída, enquanto o diagnóstico preserva a leitura bruta. As taxas de ritmo/autonomia permanecem calculadas por variação de percentual ao longo do tempo. Build e testes físicos pendentes.

# Celular Remoto v0.1.0-alpha.142

**Alpha.91:** toque em Bateria ou Temperatura no painel remoto para abrir diálogo central de termometria. Android: bateria, estado térmico, headroom (indicador sem °C) e sensor ambiente somente quando presente. ADB: leitura adicional de zonas térmicas do mesmo IP previamente pareado via comando fixo `dumpsys thermalservice`, sem exigir ADB na consulta comum. Algumas zonas não são expostas pelo fabricante; valor ausente nunca é estimado. Build Android ainda pendente.

**Nota histórica, correção da alpha.90:** workflow #82 revelou erro de compilação no menu inferior de arquivos: referência `scroll` não declarada e acesso a `AlertDialog?`. Corrigido usando ScrollView local e `dialog` não nulo. Esta versão substitui a alpha.89; funcionalidades e pasta fixa Downloads/Celular Remoto preservadas. Gerar novo APK pelo GitHub Actions antes de considerar o build validado.

# Celular Remoto

**Foto remota (alpha.131):** no monitorado, abra **Ativar + Foto** ou **Fotografia remota** e toque uma vez em **Ativar Monitorado e preparar câmera**; permissões Android continuam locais. No controlador, toque em **Tirar foto**. Apenas um JPEG é capturado por Camera2 (sem prévia/streaming), recebido por AES-GCM e mostrado para salvar. Não funciona se o Android parar a sessão, houver indisponibilidade da câmera ou se o aparelho estiver fora da LAN/VPN. Não rearma por boot nem permite abertura oculta.

**v0.1.0-alpha.142 / versionCode 142** · Android 8.0+ · Kotlin 2.2.21 · AGP 8.13.2 · Gradle 8.13

## Bateria mais clara e coerente (alpha.141)

- **Organização visual:** a tela separa **Leitura atual**, **Ferramentas e alertas** e os blocos de resumo/histórico para reduzir a sensação de mistura entre status, ação e diagnóstico.
- **Sinal visual da corrente:** se o Android informar corrente positiva mesmo com estado de descarga, o painel passa a exibir o sentido visual como **saída da bateria**. O valor bruto continua visível em Diagnóstico elétrico.
- **Taxas preservadas:** previsões e ritmos continuam baseados em variação real de percentual por hora; a corrente em mA segue derivada de `CURRENT_NOW`, `CURRENT_AVERAGE` ou `CHARGE_COUNTER`, sem misturar grandezas.
- **Resumo automático:** rótulos mais claros para janela observada, cobertura das leituras e ritmo observado.
- **Limite:** compilação Android completa e teste real em dois aparelhos ainda pendentes.

## Mensagens isoladas por credencial (alpha.139)

- **Repareamento no mesmo IP:** Mensagens associa sua sessão ativa ao host e ao resumo criptográfico da chave, de modo que o estado/última mensagem vista não passe para outro aparelho que receba o mesmo IP.
- **Resposta atrasada:** quando a chave mudar durante a leitura, textos antigos são descartados e a página solicita reabertura. Abrir a página novamente invalida callbacks anteriores.
- **Avisos com tela aberta:** só atualizam a central se o escopo de pareamento ainda for o mesmo; não envia conteúdo adicional.
- **ADB mDNS:** anúncios devem ter o tipo exato `_adb-tls-connect._tcp` ou `_adb-tls-connect._tcp.local` e corresponder ao IP selecionado, sem varredura.
- **Monitorado:** interromper o serviço acorda a espera `eventWatch` em vez de aguardar os 11 segundos totais. Não há serviço em segundo plano novo.
- **Limite:** ainda é necessário confirmar `:app:assembleDebug`, testes completos no GitHub Actions e comportamento em dois aparelhos.

## Segurança de sessão e ADB (alpha.137)

- **ADB Wi-Fi:** corrigido o uso de host opcional no fluxo de descoberta; é necessário ter um IP definido antes de procurar/selecionar a porta de conexão. O filtro mDNS aceita apenas o nome de serviço TLS ADB de conexão, no IP selecionado, e exclui portas inválidas ou anúncios similares.
- **Mensagens e notificações:** ao abrir a central a partir de uma notificação, os textos da sessão anterior são removidos da RAM antes de renderizar. Não há solicitação de dados sensíveis com Activity invisível.
- **Eventos em primeiro plano:** callbacks de atualização atrasados são descartados se host ou chave mudarem antes da thread principal processá-los. No segundo plano permanece o JobScheduler, sem notificações em tempo real garantidas.
- Não muda protocolo AES-GCM, `WIRE_COMPAT`, comandos ADB, permissões ou consentimentos locais. **Build Android completo e testes nos dois aparelhos ainda pendentes.**

## Eventos mais rápidos na rede local (alpha.136)

- **Avisos ativados no controlador:** enquanto este aplicativo estiver visível, `eventWatch` aguarda por até 11 segundos um evento no monitorado, retornando assim que a captura autorizada mudar. Usa AES-GCM na mesma porta 37842 e ocupa no máximo um dos três workers do servidor.
- Transfere apenas metadados mínimos (ID, hora, tipo e pacote). O conteúdo e o remetente de SMS não são incluídos nesta consulta. SMS e notificações só entram se o monitorado permitir localmente a captura e o compartilhamento; filtros de apps e proteção de dados sensíveis continuam em vigor.
- **Mensagens:** ao abrir a central, a atualização por evento fica ligada somente enquanto a página e o aplicativo estão visíveis; há controle para desligá-la e continua possível atualizar manualmente. Nenhum serviço de atualização permanente é criado.
- **Em segundo plano:** o Android continua usando a consulta agendada de 15 minutos ou mais. O observador é encerrado em `onPause`; a rede pode falhar, o Android pode suspender o monitorado e a primeira conexão estabelece linha de base, portanto **não se promete alerta instantâneo nem entrega garantida**.
- **Energia → Acender tela:** comando autenticado com confirmação explícita, usando a autorização `Acender tela` habilitada no monitorado. Não desbloqueia e o Android pode recusar.
- Segurança: verifica host e chave antes de aceitar respostas, limita um observador por monitorado e não altera `WIRE_COMPAT=0.1.0-alpha.111`. Versões antigas podem não atender `eventWatch`; snapshot e JobScheduler continuam. A compilação Android e o teste em dois celulares estão pendentes.

## Avisos remotos mais confiáveis (alpha.135)

- **Ajustes → Notificações:** o controlador pode escolher individualmente alertas de SMS, WhatsApp/Business e outros apps. As duas primeiras categorias vêm ligadas apenas dentro do alerta geral opt-in; outros apps ficam desativados por padrão. A coleta no monitorado ainda exige as permissões locais.
- A consulta periódica pode ocorrer somente a cada 15 minutos ou sofrer atrasos do Android; eventos novos observados até **60 minutos** antes da consulta, enquanto ainda estiverem no buffer, podem gerar aviso genérico com a idade real. Não há push, aviso imediato garantido ou resgate de notificações descartadas.
- Os cursores agora são isolados por host **e chave de pareamento**. Ao desligar e reativar os avisos, a primeira consulta estabelece nova linha de base para evitar notificações retroativas.
- SMS recebidos em tempo real usam a hora de chegada ao Android para validade do aviso, sem mudar a hora da mensagem mostrada na conversa.
- O JobScheduler revalida aparelho selecionado e chave **após** a resposta cifrada, evitando usar um retorno antigo após re-pareamento. Sem novos serviços, permissões ou portas.
- Protocolo AES-GCM e `WIRE_COMPAT=0.1.0-alpha.111` preservados. **Compilação Android e testes em dois aparelhos seguem pendentes.**

## Interface, sirene e avisos remotos (alpha.134)

- **Controlar** é a tela de entrada. Início foi removida e a barra inferior tem quatro destinos: Controlar, Monitorado, Bateria e Ajustes. A tela principal trata os Insets reais de barras/recortes, inclusive Android 16.
- Os **12 comandos** ficam em **Funcionalidades**, sem separar acesso rápido e outras funções. Rede abre painel de Wi-Fi com SSID, RSSI, IPv4, validação de internet e permissões; Energia usa três ícones lado a lado.
- **Sirene** sintética de dois tons em vez de bipes: 5/10/20/30 segundos, parada manual/automática, sem elevar volume nem ignorar Não Perturbe. Bloquear exige todas as permissões locais e digitar BLOQUEAR.
- Em **Ajustes → Notificações**, habilite **Avisos de mensagens do aparelho remoto** no controlador. O monitorado continua exigindo suas permissões e allowlist; o canal AES-GCM envia eventos autorizados, sem conteúdo de SMS no polling. Notificações genéricas só são emitidas para eventos novos após a primeira leitura. Com Controlar visível, consultas acontecem aproximadamente a cada 15 segundos; no segundo plano o Android pode atrasar a tarefa de mínimo 15 minutos. **Não há entrega instantânea garantida.**
- A notificação permanente apresenta percentual e estado da bateria local, com expansão para temperatura/corrente e última telemetria remota autenticada em RAM (até 3 minutos). Dados remotos antigos são marcados como indisponíveis, sem nome do aplicativo no título.
- A central Mensagens mostra conversas SMS recolhíveis; ADB indica não pareado, pareado sem conexão, reconectando e conectado. Sobre inclui Adriedson Lemos, site, Pix para contribuições e acesso ao histórico de alterações.
- Criptografia, permissões locais, proteção de dados sensíveis, ADB autorizado, fluxo de atualizações e `WIRE_COMPAT=0.1.0-alpha.111` preservados. **Build Android e testes físicos ainda pendentes.**

## Copiar detalhes do aparelho e número da linha (alpha.133)

- **Controlar → Detalhes do aparelho:** toque em qualquer campo com ícone de cópia, incluindo IP, SSID, RSSI, dados da bateria, operadora, armazenamento e sistema. Somente valores informados são copiáveis.
- **Copiar resumo técnico** continua disponível; **Copiar todos os dados disponíveis** reúne todas as seções do snapshot autenticado, sem campos ocultos ou não compartilhados.
- **Número da linha:** no monitorado, habilite **Ajustes → Número do chip** e conceda READ_PHONE_NUMBERS. Mesmo com autorização, algumas operadoras não fornecem o número ao Android; o app informa que está indisponível, sem inventar um valor. Android 8–12 usa TelephonyManager legado autorizado; Android 13+ usa SubscriptionManager.
- O relatório com número pede confirmação adicional; Android 13+ oculta a prévia do clipboard sensível. **O clipboard pode ser lido por aplicativos locais**: compartilhe com cuidado. Nenhuma permissão, endpoint ou protocolo remoto novo foi criado.

## Atualização local e remota reforçada (alpha.132)

- **Ajustes → Atualizações** agora diferencia estado da transferência, percentual quando conhecido, bloqueio por rede/bateria e necessidade de autorizar o instalador no aparelho de destino.
- No controlador pareado: **Verificar e baixar**, **Consultar remoto**, **Acompanhar remoto por 3 minutos** (consulta autenticada a cada 15 s apenas com a tela visível), **Cancelar remoto** durante uma transferência e **Tentar download remoto** em casos recuperáveis.
- O monitorado continua exigindo opt-in **Aceitar pedidos remotos**, inicialmente desativado, e nunca aceita instalar APK ou conceder permissões por comando da rede. Somente o usuário no próprio monitorado confirma a instalação pelo Android.
- No próprio aparelho, é possível **Atualizar estado**, **Cancelar download** e retomar tentativa após falha; a Release pode ser reconsultada sem apagar automaticamente um APK já verificado. Consultas e verificação do download são sincronizadas por um único bloqueio para evitar corridas.
- SHA-256, identidade do pacote, código de versão superior e certificado de assinatura igual ao já instalado continuam obrigatórios. Não houve alteração no protocolo AES-GCM, na câmera, bateria ou nos 12 comandos.
- O monitoramento de progresso remoto termina ao sair da tela ou após 3 minutos; não adiciona serviço/sessão remota permanente nem ativa WakeLock.
- A compilação real do Android e os testes de atualização com dois aparelhos ainda estão pendentes.

## Fotografia remota em um único fluxo (alpha.131)

- No próprio monitorado, use **Monitorado → Ativar + Foto** (quando desativado) ou **Foto** (quando ativo). Na tela Fotografia remota, o botão ativa Monitorado se necessário, aguarda seu servidor e prepara a sessão de câmera enquanto a Activity está visível.
- `CAMERA` e notificações (Android 13+) são solicitadas apenas se faltarem. Negar permissões impede preparar a sessão. A câmera não fica capturando em segundo plano: Camera2 abre somente para cada JPEG e fecha em seguida, sob comando de aparelho pareado.
- No controlador, a tela de Fotografia consulta o estado autenticado uma vez ao abrir e ao tocar em **Verificar câmera no monitorado**. Estados sem sessão ativa orientam preparar localmente; versões anteriores sem campo de diagnóstico permitem tentativa manual.
- Se Android encerrar a câmera, reiniciar o celular, interromper o app ou o usuário parar o serviço, será necessário novo toque local. Não há rearmamento remoto oculto, vídeo contínuo ou alteração no protocolo AES-GCM.

## Bateria contínua sem controle remoto (alpha.130)

- Aba **Bateria → Ativar coleta contínua local**, ou Ajustes → Bateria local contínua: ativa acompanhamento com tela apagada independentemente de Monitorado; desligado por padrão.
- Um único serviço foreground alterna o tipo Android (specialUse para medição local; connectedDevice para monitoramento remoto). O modo local **não abre porta TCP** nem inicializa câmera, tela ou demais comandos remotos.
- Coleta local e Monitorado compartilham a mesma agenda de 30 s, com apenas uma notificação ativa; as preferências são independentes, inclusive ao parar uma delas. Histórico não é apagado.
- Android/Doze pode limitar a coleta; não ativar WakeLock automaticamente. O tipo FGS specialUse pode exigir revisão do caso de uso para distribuição Google Play. Build Android e testes físicos ainda pendentes.

## Continuidade e recuperação do serviço (alpha.129)

- **Ajustes → Conexão estável e WakeLock:** novo resumo de início, última execução, encerramento observado e recriações sem encerramento registrado. A escrita do heartbeat é limitada a uma vez a cada 2 minutos.
- O serviço ativo não reinicializa os agendamentos a cada segundo comando de início. O listener TCP ainda é reparado se necessário; eventos de rede refletem só a rede padrão do Android, não toda interface auxiliar.
- Erros ao atualizar a notificação não encerram o serviço; uma nova tentativa ocorre no ciclo seguinte. Sem mais acessos à rede, permissões, WakeLocks, sessões ou notificações permanentes.
- Uma recriação sem encerramento registrado pode ser causada por encerramento do processo ou energia; não indica necessariamente Doze, falha Wi-Fi ou encerramento pela Xiaomi. Requer ensaio em aparelhos reais e CI Android.

## Conexão estável e WakeLock (alpha.128)

- Em **Ajustes → Conexão estável e WakeLock**, consultar status do Doze, economia de energia, isenção parcial e rede disponível.
- WakeLock de CPU opt-in: desligado por padrão, teste de 30 minutos, ou durante serviço Monitorado. Wi-Fi Lock opcional e separado, ambos liberados na parada do serviço.
- Indicação de lacunas do supervisor acima de 45 s: não prova queda de Wi-Fi, travamento ou desligamento do serviço. Acesso a configurações oficiais Android e orientações sobre Xiaomi.
- Não concede isenção de bateria silenciosamente. Android pode ignorar WakeLock e bloquear rede no Doze. Sem novos serviços ou privilégios remotos.

## Preparação do monitorado e prontidão remota (alpha.127)

- **Monitorado** e **Ajustes → Preparar aparelho monitorado** mostram um diagnóstico único de pré-requisitos de serviço, notificações, atualização, origem de instalação, captura, localização, comandos físicos, SMS e arquivos. Cada item é apresentado como Pronto, Configurar, Por sessão ou Desativado; recursos opcionais não são considerados defeitos.
- O controlador recebe o bloco opcional `readiness` **somente dentro do snapshot autenticado AES-GCM**, vê contagens de prontidão e pode abrir a lista de estados sem acesso a dados sensíveis. Versões anteriores ignoram o campo; a compatibilidade `WIRE_COMPAT` permanece `alpha.111`.
- A autorização de verificação/download remoto permanece opt-in e desativada por padrão. A origem de instalação pode ser aberta na configuração oficial do Android **somente após toque local**; instalar continua exigindo intervenção local. A tela nunca ativa permissões, câmera, acessibilidade, tela, localização ou SMS por conta própria.
- Nenhum serviço, socket, permissão manifest ou workflow adicional. Teste Android completo e validação com dois aparelhos continuam pendentes.

## Diagnóstico e entrega de alertas (alpha.126)

- Central local de publicação para saúde, metas, estabilidade de conexão, atualização pronta e eventos remotos: valida POST_NOTIFICATIONS, permissão global e importância de cada canal, registrando apenas origem genérica, canal, horário e resultado (sem host, mensagens, localização ou credenciais).
- Eventos recentes de saúde e de metas que não foram publicados devido a bloqueio permanecem pendentes por até 24 horas. Não marcar como entregue antes de o Android aceitar a notificação. Alertas antigos e condições já resolvidas não devem reaparecer.
- Alertas remotos de metas são analisados nas respostas autenticadas da tela Controlar, além de Bateria e do JobScheduler de melhor esforço (15 min quando ativado).
- Configurações → Alertas e proteção apresenta diagnóstico de canais, últimas tentativas e ação de notificação de teste. Um resultado "aceito pelo Android" não comprova que foi exibido ou lido.
- Continua com consentimentos e canais anteriores; nenhuma nova permissão, serviço, coleta ou socket. Testes de dispositivos, Doze e compilação Android ainda necessários.

## Notificação ativa com bateria local e estado remoto (alpha.125)

- A notificação persistente do `MonitorService` agora resume percentual, temperatura da **bateria** e estado de carga do próprio aparelho. Inclui corrente líquida em mA quando medida (sem inverter sinais) e previsão de carga ou autonomia **somente** quando a sessão oferece estimativa válida.
- Quando expandida, diferencia **Monitorado** (servidor local, acessos autenticados) e **Remoto** (aparelho selecionado no controlador, com horário da última resposta). Não promete socket permanente nem inventa conexão atual quando a resposta é antiga.
- Aproveita o executor de bateria já existente, com atualização limitada a aproximadamente 1 minuto; não cria serviço ou conexão adicionais. Evita leituras ou armazenamento de sessões na thread principal. Tela bloqueada mantém notificação pública genérica.
- Testes de conteúdo e casos inválidos adicionados. Verificar compilação Android real e comportamento com tela apagada nos dois aparelhos antes de considerar aprovado.

## Controlar: 12 comandos sempre visíveis (alpha.124)

- **Outras funções** mostra sempre os oito comandos adicionais, abaixo dos quatro atalhos rápidos, sem botão para ocultar ou expandir. Isso vale também antes de receber telemetria do aparelho pareado.
- A grade permanece responsiva (1–4 colunas conforme largura e fonte) e o bloqueio de ações sem chave segue encaminhando ao pareamento explícito.
- A UI reutiliza o grupo de comandos nas atualizações de telemetria quando aparelho, autorização, eventos e aparência não mudaram. Alterações nessas condições invalidam o cache; nenhum snapshot, segredo ou dado pessoal é armazenado nesse cache.
- Nenhuma mudança no protocolo, atualizador, notificações, cálculo de bateria ou serviço de monitoramento. Compilação Android e testes físicos pendentes.

## Interface e acessibilidade (alpha.123)

- Tema AMOLED selecionável em Ajustes → Aparência, com superfícies e diálogos próprios; preserva Claro, Escuro e Sistema.
- Botões de configurações deixam de competir por espaço em telas estreitas e com fontes ampliadas; atalhos e barra de navegação ajustam altura e quebra de texto.
- Políticas puras testáveis para cores semânticas, grades e densidade. Não muda autorização, pareamento ou comunicação remota.
- **Atenção:** testes sintáticos/isolados não substituem o build Android e a verificação em dois celulares.

## Correção de compilação (alpha.122)

- **Build 15:** `:app:compileDebugKotlin` falhou em `BatteryCompactUi.kt:114` (`Unresolved reference 'minHeight'`). A linha `LinearLayout` da lista de sessões agora utiliza `minimumHeight = dp(57)`, preservando tamanho e interação.
- Verificador de distribuição inclui regressão estática da propriedade. Nenhuma regra de segurança, rede, atualização, bateria ou coleta foi modificada.
- Build Android e testes de aparência nos aparelhos continuam pendentes; somente um novo GitHub Actions pode comprovar a correção integral.

## Ajustes, permissões e atualizações compactos (alpha.121)

- Telas internas de Configurações sem segundo cartão externo, com retorno à lista de categorias em linha e separador; mantém todos os destinos e a pesquisa por nome.
- Central de permissões dividida em Comuns, SMS, Especiais e Sessões, com estado real e ações explícitas de solicitação; não concede nem habilita acesso remoto ao abrir uma aba. SMS mantém confirmação separada.
- Atualizações com estado resumido, verificação, download/instalação (somente por toque local) e controles em linhas compactas. Os comandos remotos mantêm pareamento e confirmação; nenhum instalador remoto foi introduzido.
- Diálogos mais enxutos e históricos de Novidades navegáveis, com detalhes por toque. Valores de status longos quebram linha, inclusive com fonte ampliada.
- Nenhuma mudança no protocolo, nas chaves, permissões declaradas, medições, fluxo de assinatura ou workflow único. Compilação Android e testes físicos de UI ainda necessários.

## Bateria compacta e analítica (alpha.120)

- **Resumo:** percentual em destaque com barra de progresso e fonte identificada; corrente líquida, temperatura e potência em três indicadores (duas colunas em telas estreitas ou fontes ampliadas). Potência da bateria **não é** potência do painel solar.
- **Sessões:** previsão com destaque, metas de 80%/90% e horário apenas quando houver dados; cobertura, corrente média e evolução real em um painel denso. Últimas recargas e testes aparecem em listas com separadores e acesso aos relatórios completos.
- **Histórico:** filtros por tipo/período preservados e sessões apresentadas em uma lista única, sem um grande card por evento; gráficos por amostras reais continuam nos detalhes, com lacunas respeitadas.
- **Testes:** Solar, Elétrico, Consumo e Carga Automática preservados, com mesmas confirmações e controles local/remoto. Nenhuma nova coleta ou permissão, alteração de protocolo ou fonte de cálculos.
- Layout a validar no Android em 320/360/393 dp, fontes ampliadas e modo claro/escuro. Verificação sintática não comprova execução do APK.

## Telas de controle compactadas (alpha.119)

- **Controlar:** bateria, temperatura e conexão aparecem em três indicadores compactos (em telas estreitas, duas colunas), mantendo os detalhes via toque e a identificação de leitura recente ou armazenada. Os doze comandos permanecem disponíveis em grade; nenhum comando é disparado automaticamente.
- **Monitorado:** serviços e IP apresentados de forma mais condensada; compartilhamento de tela, câmera e foto reunidos em três atalhos com estados visíveis, sem perder as confirmações e a autorização local de cada modo.
- **Diálogos:** controles de som/lanterna e detalhes de manutenção com espaçamentos reduzidos; ações remotas perigosas continuam exigindo confirmação explícita e, quando necessário, ADB autorizado.
- O redesenho não altera credenciais, AES-GCM, protocolo, dados, permissões, serviço Monitorado ou atualizações assinadas. **APK e usabilidade real requerem CI e teste em dois aparelhos.**

## Interface compacta (alpha.118)

- Tela Início redesenhada para mostrar aparelhos salvos em linhas acionáveis, horário da última resposta **sem fingir conexão permanente**, estado do Monitorado e três atalhos adaptativos.
- Configurações reorganizadas em seis categorias com pesquisa preservada; as opções completas só aparecem após escolher uma categoria ou pesquisar. Central de Permissões continua disponível no topo.
- Ações da tela Controlar e do Monitorado em grades de 2/3/4 colunas segundo largura e fonte Android; os 12 comandos e os consentimentos locais são preservados.
- Ajustados indicadores, cartões de telemetria, atalhos dos quatro testes de bateria e tema claro/escuro. Novidades mostram apenas três destaques com histórico separado.
- Nenhuma mudança nos protocolos AES-GCM, credenciais, banco de sessões, serviço Monitorado, permissões ou workflow. 
- **Validação local não substitui compilação Android nem testes de usabilidade e TalkBack em aparelhos físicos.**

## Diagnóstico de conexão (alpha.117)

- Configurações → Estabilidade da conexão (e atalho no Controlar): estado atualizado por idade da resposta, latência, tentativas válidas, falhas consecutivas, lacunas e gráfico de sondagens.
- Novo heartbeat cifrado sem coleta de dados do usuário; fallback compatível com pares anteriores; nenhuma conexão TCP permanente adicionada.
- Monitoramento com o aplicativo visível e sondagens em segundo plano opcionais, via JobScheduler (~15 min, sem garantias de tempo real).
- Reconexão automática limitada a endereços previamente salvos com a mesma chave; notifica após três falhas e na recuperação (se notificações autorizadas).
- O percentual de disponibilidade refere-se somente a tentativas realizadas; períodos sem medição não são falhas nem disponibilidade comprovada.
- Permanece protocolo WIRE_COMPAT alpha.111; validar no GitHub Actions e em dois Androids antes de considerar produção.

## Correção do build 9 (alpha.116)

- A compilação principal e a compilação dos testes passaram na CI da alpha.115; a falha ocorreu na execução do teste `AppVersionTest.apkVersionAndWireVersionAreIndependent` (209/210 aprovados).
- O teste ainda exigia a versão APK `alpha.112`. Agora compara `AppVersion.NAME` com `BuildConfig.VERSION_NAME` e confere `versionCode`; o marcador de protocolo `alpha.111` continua independente.
- Nenhuma mudança em rede, AES-GCM, bateria, atualização online, permissões, assinatura ou workflow único. Testar novamente o Gradle/Android na CI.

## Correção do build 8 (alpha.115)

- `:app:compileDebugKotlin` da alpha.114 passou no GitHub Actions; a falha ocorreu posteriormente em `:app:compileDebugUnitTestKotlin`.
- Em `BatteryHistoryMathTest.kt:14`, `BatteryHistoryMath.rate()` devolve `Double?` (nullable) e o `assertEquals(Double, Double, delta)` exige `Double` não nulo. O teste agora usa `requireNotNull(rate(...))` antes de comparar com a expectativa `20.0`, preservando a falha de teste para amostras inválidas.
- Nenhuma regra de cálculo de bateria, protocolo, permissão, pareamento, assinatura, atualização online ou workflow foi alterada nesta correção.
- Compilação e testes Android completos da alpha.115 ainda dependem do próximo build no GitHub Actions.

## Histórico de bateria e comparação (alpha.114)

- Histórico do aparelho local ou remoto com filtros por tipo e período, integrando sessões dos testes Solar, Elétrico, Consumo, Automático e sessões normais de carga/descarga. O filtro atua somente nos resumos recebidos, sem novas consultas de armazenamento privado do pareado.
- Comparação de dois testes concluídos: variação de percentual, tempo, cobertura, ritmo observado em p.p./h e temperatura máxima. A comparação é marcada como inconclusiva quando tipo, céu, painel nominal ou estado da tela não são equivalentes, ou quando a evidência é insuficiente.
- Gráficos com grades, horários e amostras reais; sem traçado contínuo em lacunas >90s ou com leitura inválida. Dados antigos são preservados, embora algumas sessões legadas possam conter apenas o trecho final.
- Até 24 resumos Labs e 24 automáticos por consulta autenticada; até três gráficos de cada categoria com no máximo 40 pontos por sessão. Retenção local permanece em 80 Labs/120 automáticas, sem migrar banco nem ampliar permissões.
- É preciso validar a compilação Android e a usabilidade em telas pequenas. O Pulso Bateria não é uma dependência do APK.

## Bateria avançada (alpha.113)

- Corrente líquida do Android preservada inclusive negativa no estado CHARGING; leituras instantânea, média e média por CHARGE_COUNTER mostram origem e valores brutos. Correções OEM de escala/sinal não são presumidas.
- Sessões automáticas: média de corrente ponderada apenas por intervalos observados, cobertura das amostras e fonte reportada pelo Android. Histórico anterior mantém-se legível sem migração destrutiva.
- Previsões após tendência contígua válida (≥10 minutos e 2 pontos percentuais) ou ≥2 sessões históricas recentes comparáveis. Fontes USB não distinguem painel solar de carregador; mostrar limitação.
- Resumo com metas estimadas de 80/90/100%, proveniência da estimativa, últimas sessões e diagnóstico elétrico acessível local ou remotamente.
- Sem novo serviço, permissão, segundo workflow ou dependência do APK Pulso Bateria.

## Atualizações mais automáticas (alpha.112)

- Verificação automática a cada 60 minutos por padrão (configurável 30 min/1 h/3 h/6 h/12 h/24 h), sujeita a atraso pelo Android.
- Configurações → Atualizações permite consulta e pedido de baixar APK no aparelho remoto pareado; o monitorado precisa habilitar localmente **Permitir pedidos remotos de atualização**.
- Comandos no canal AES-GCM existente: `appUpdate/status`, `check`, `checkDownload`, `download`; sem permissão para instalação remota.
- Compatibilidade sem exigir número do APK igual: alpha.111 ↔ alpha.112 em operações comuns; operação nova somente entre alpha.112+. Versões anteriores à alpha.111 podem recusar a conexão.
- Download respeita rede não tarifada e proteção de bateria; APK conferido e instalado somente mediante toque local no aparelho.

## Atualizações online (alpha.111)

- Em **Configurações → Atualizações**, o app consulta `drilemoz/Celular-Remoto` via HTTPS e lista inclusive prereleases. A verificação periódica usa JobScheduler (6, 12 ou 24 horas; padrão 24 h) e pode ser atrasada pelo Android.
- Se aparecer uma Release com `celular-remoto-update.json` gerado pela CI **com assinatura estável**, o DownloadManager pode baixar o APK com tela apagada. Por padrão usa apenas rede não tarifada, verifica bateria >= 25% ou conectado à fonte e evita downloads durante testes Labs ativos.
- Antes de oferecer instalação, confere SHA-256, nome do pacote `br.dev.celularremoto`, `versionCode` e assinatura do APK instalada. Instalação somente após toque local e confirmação do Android. Configurações de fontes desconhecidas continuam sendo autorizadas no próprio aparelho.
- O workflow **único** acrescenta manifesto JSON somente às Releases com os quatro Secrets válidos. Builds Debug não podem ser ofertados como atualizações automáticas. A transferência de APK entre aparelhos por Wi-Fi **ainda não foi implementada**.
- Mais detalhes e limitações em `docs/UPDATES.md`.

## Central de permissões (alpha.110)

- Nova tela de **Configurações → Central de permissões**, aberta uma vez na primeira instalação do `br.dev.celularremoto`; acompanha pendências reais de câmera, GPS, notificações, áudio, chips, SMS, Acessibilidade, leitor de notificações, uso e localização em segundo plano.
- Um banner discreto no topo informa pendências locais e abre a central; pode ser fechado até a próxima sessão. A solicitação em lote só ocorre por toque explícito e separa SMS sensível, sem ativar compartilhamento de dados.
- Acesso a arquivos/fotos usa Storage Access Framework, sem `READ_MEDIA_IMAGES` nem acesso geral ao armazenamento. MediaProjection solicita autorização a cada captura de tela. Acessos especiais devem ser concedidos nas telas oficiais Android.
- `READ_SMS`/`RECEIVE_SMS` são restritos por instalador em muitos Androids; a UI explica a limitação. A central nunca concede permissões automaticamente nem ativa o monitoramento remoto.
- Android 13+ exige `POST_NOTIFICATIONS` para publicar notificações; localização em segundo plano permanece etapa separada.

## Correção do teste de pareamento QR (alpha.109)

- O build 3 chegou aos testes Kotlin e falhou em `PairingCodeTest.rejectsUnexpectedQr`: o teste tentava substituir a antiga porta 37841, mas a porta atual é 37842. Erro no teste, não na validação dos QRs.
- Casos de adulteração agora usam `WireProtocol.PORT` e verificam que o QR realmente foi modificado antes de exigir sua rejeição.
- O esquema QR passa a `celularremoto://pair` para acompanhar a identidade atual; QRs das versões anteriores não devem ser reutilizados. É necessário atualizar os dois dispositivos e gerar novo QR.
- Mantidos o AES-GCM, chave temporária, proteção contra replay, serviço único, permissões e um único workflow na `main`. Compilação Android e testes físicos ainda precisam ser confirmados.

## Correção de compilação e execução única do workflow (alpha.108)

- Corrigida a colisão entre variável local `type` e `Intent.type` no compartilhamento de sessões automáticas de bateria (`BatteryScreenUi.kt:506`). A intenção recebe explicitamente `this.type = "text/plain"`.
- A CI passou a executar **um único workflow automaticamente no push para `main`**, sem acionamento separado para pull request ou manual. O `concurrency` cancela a execução anterior da mesma branch, evitando compilações concorrentes e releases em disputa.
- Mantidas as demais funções e integrações da alpha.107. A correção está sujeita à confirmação pelo build Android completo do GitHub Actions.

## Nova identidade e integração (alpha.107)

- **Aplicativo único:** `Celular Remoto` / `br.dev.celularremoto`, Android 8+ (minSdk 26), compile/target Android 16/API 36. Classes internas e protocolos mantêm nomes históricos quando não fazem parte da identidade pública.
- **Instalação separada** do antigo Monitor Remoto: o Android não migra preferências privadas, históricos ou chaves não exportáveis do Keystore entre IDs de aplicativos. Preserve a instalação anterior até compartilhar os relatórios desejados. Os dois aparelhos deverão ser pareados novamente usando o Celular Remoto.
- Porta de serviço alterada de `37841` para `37842` para coexistir durante migração sem disputar o socket TCP. Protocolo permanece AES-GCM; desde a alpha.112 a exigência de igualdade textual foi substituída por compatibilidade de protocolo (111↔112).
- Sessões **automáticas** de carregamento e descarga, acompanhadas pelo serviço Monitorado a cada 30 s com pontos a cada 60 s ou mudança de percentual; história privada isolada de Labs com limite de 120 sessões e 240 pontos por sessão.
- Estimativas conservadoras de chegada a 100% ou autonomia até 0%: exigem pelo menos dez minutos de observação válida e dois pontos percentuais de variação no mesmo sentido; tempo ausente nunca é apresentado como medido.
- Alertas configuráveis por aparelho: meta de carga (50–100%), bateria baixa (5–40%), temperatura (38–55 °C). Eventos são mantidos localmente, e no controlador apenas quando opção de alertas remotos estiver habilitada. Não garantem tempo real quando desconectado; não substituem proteção térmica do sistema.
- Encerramento dos testes de carga após **duas leituras confirmadas de 100%**, inclusive quando o firmware continua informando CHARGING; relatórios preservam o motivo de conclusão.
- Interface da aba Bateria revisada: origem de corrente, fonte não identificada, potência solar nominal não informada, sessões automáticas e configurações de alertas locais/remotas.
- Preservadas as permissões locais e autorizações existentes para projeção de tela, câmera, SMS, localização, ADB, bloqueio de tela e acesso a arquivos. **Não foi executado build Android completo neste ambiente; use GitHub Actions e teste em dois aparelhos.**

## Bateria e testes locais/remotos (alpha.106)

- Quinta aba fixa **Bateria**, com opção para leituras deste aparelho ou de um aparelho pareado, sem instalar Pulso Bateria.
- Corrente Android (instantânea ou média), estado, temperatura, tensão e potência elétrica **líquida na bateria**, sem confundi-la com a geração solar do painel.
- Testes Solar (condição do céu e potência nominal informadas), Elétrico, Consumo e Carga Automática. A Carga Automática encerra com bateria cheia ou após três minutos de desconexão confirmada; outros testes aceitam duração de 1 a 1440 min e encerramento manual.
- Amostragem a cada 30 s **somente com teste ativo e MonitorService em execução**, com até 80 sessões privadas, 480 pontos enquanto ativas e retenção compactada após encerrar; relógio monotônico e lacunas até 90 s classificadas de modo conservador. Recuperação após reinício do processo é limitada pelo ciclo de vida do serviço.
- Resumos e relatórios com períodos confirmados, desconhecidos, média válida da corrente, temperaturas e gráfico de percentual baseado em leituras reais. Compartilhamento de texto do relatório exige ação explícita.
- Comandos `batteryLab` de status/iniciar/parar trafegam pelo AES-GCM/pareamento e revisão de protocolo existentes; respostas só são aceitas se host/chave continuarem válidos. Novas funções só são chamadas quando suportadas.
- Não mede potência solar real, capacidade útil ou autonomia com precisão metrológica; requer medidor externo para geração do painel. Compilação e testes em dois dispositivos permanecem necessários.

## Interface mobile-first (alpha.105)

- Nova navegação inferior fixa com Início, Controlar, Monitorado e Ajustes. O conteúdo principal é rolável, inclusive em smartphones compactos.
- Início com aparelhos pareados, último acesso real, botão de conexão e acesso ao monitoramento local.
- Controlar com cabeçalho próprio, botões separados e informações de bateria, temperatura e rede em cartões legíveis; dados antigos são identificados.
- Acesso rápido com Tela, Arquivos, Câmeras e Localização em duas colunas; as outras oito funções permanecem no grupo expansível.
- Sem alterações na criptografia nem nas permissões de captura. A alpha.112 mantém compatibilidade com a alpha.111 nas funções já existentes.
- **Pendente:** compilação Android na CI e teste de navegação/layout no dispositivo real.

## Estabilidade de conexões e proteção de sessão (alpha.104)

- O encerramento/reinício do listener fecha sockets em execução e aguardando, liberando transferências antigas e conexões pendentes; a fila continua limitada.
- Notificações de acesso preservam o aviso de nova sessão autenticada, mas não acumulam milhares de identificadores de sessões expiradas em RAM.
- No controlador, snapshots, mensagens e confirmações de comandos atrasados deixam de ser aceitos se a credencial pareada do host tiver sido removida ou trocada enquanto a resposta trafegava.
- O aplicativo preserva todos os fluxos atuais de autorização local, câmera, GPS, rede e ADB. **Testes físicos e build completo ainda pendentes.**

## Responsividade e economia de redesenhos (alpha.103)

- O Modo Monitorado só reconstrói os cartões após alterações observáveis de serviço, transmissão, foto, câmera, IP local ou conjunto de aparelhos autenticados; consultas idênticas não deslocam o conteúdo rolado.
- Uma notificação de atividade autenticada é agrupada por 180 ms, e o temporizador periódico agenda apenas um próximo ciclo, sem duplicar tarefas de interface. O prazo de expiração de 45 s permanece verificado a cada 5 s.
- A busca de Configurações reutiliza um índice local, agrupa digitação rápida em 120 ms e limpa resultados imediatamente; não altera nenhuma preferência ao pesquisar.
- Alvos de toque dos ícones de conexão/renomear e detalhes passaram a 44 dp. Nenhuma modificação em protocolos, captura, permissões ou frequência das consultas de rede.
- As melhorias reduzem renderizações desnecessárias por projeto; não representam medições de desempenho ou consumo em aparelhos. Build completo e testes físicos pendentes.

## Configurações e diálogos padronizados (alpha.102)

- A aba Configurações agora usa um único cabeçalho, índice com grupos de opções em linhas mais leves, campo de busca com limpeza acessível e contagem real de resultados.
- Visão geral mostra as autorizações **locais** de câmera, GPS e notificações conforme o Android; tocar abre a respectiva categoria, sem solicitar permissão automaticamente.
- Seleção de perfil GPS usa diálogo central visual com descrições de autonomia, indicador de escolha atual e cancelamento seguro.
- Diálogos gerenciados usam largura responsiva e botões menos agressivos; ações continuam exigindo confirmação explícita.
- Nenhuma permissão, chave, protocolo de conexão ou captura foi ampliada. Compilação Gradle/Android e validação visual real pendentes.

## Modernização da tela inicial e Modo Monitorado (alpha.101)

- Tela inicial: painel de identidade e status local, atalhos claros para pareamento e monitoramento, histórico de aparelhos com indicação explícita de chave salva, sem confundir com conexão online.
- Modo Monitorado: status do serviço destacado, cartão compacto de permissões, bloco padronizado de captura com ícones, descrição curta e estado da tela/câmera/foto, deixando a confirmação explícita no fluxo já existente.
- Acessos autenticados: indicador de atividade recente, sem prometer sessão de rede permanente. Componente compartilhado `statePill` e `monitorFeatureCard` para uniformidade visual.
- As permissões, captura consentida, pareamento, protocolo AES-GCM, comandos ADB e controles de parada permaneceram sem mudanças. Não há nova permissão.
- Compilação Android completa e teste de toque/tema em aparelhos físicos continuam pendentes.

## Correção do workflow 87 (alpha.100)

O workflow #87 passou pela compilação de recursos, mas falhou em `:app:compileDebugKotlin` no
`MonitorScreenSkin.kt` (linhas 43, 53 e 73). Dentro de `TextView.apply`, `Button.apply` e
`ImageView.apply`, o nome `foreground` era capturado por `View.foreground: Drawable`, quando
o código precisava da propriedade `MonitorScreenSkin.foreground: Int` (cor ARGB).
As três ocorrências agora usam `this@MonitorScreenSkin.foreground`; não há alteração de cores,
de captura, credenciais, ADB, transporte ou permissões. Acrescentada regressão estrutural.
O APK ainda requer nova execução da CI.

## Correção do workflow 86 (alpha.99)

A alpha.98 não compilou devido a `android:windowDimAmount` nas folhas de estilo de diálogos. A alpha.99 usa o atributo válido `android:backgroundDimAmount` em claro e escuro sem alterar os diálogos ou seus comandos. Antes de instalar, confirme que o novo workflow concluiu `:app:assembleDebug` e os testes; mantenha a mesma versão nos dois celulares.

## Interface de conexão e pareamento alpha.98

- **Conexão:** indicador de estado compacto, com leitura e latência real quando disponível, sem alterar consulta a cada 15 segundos.
- **Pareamento:** QR Code, conexão manual e aparelhos salvos mantidos, com apresentação resumida e orientação mais clara.
- **Detalhes do aparelho:** cabeçalho e resumo da idade dos dados reutilizam `MonitorScreenSkin`, mantendo as seções técnicas e a proteção de informações sensíveis.
- **ADB:** cabeçalho visual compartilhado, cartões compactos, identidade de conexão mais legível, ações avançadas organizadas. Nenhum comando, permissão ou pareamento mudou.
- Build Android completo e inspeção em dois aparelhos pendentes.

## Nova apresentação de tela remota, SMS e notificações alpha.97

- **Ver tela** ganha cabeçalho do kit `MonitorScreenSkin`, barra de ações separada e cartão de sessão com origem autenticada, evitando apertar o título em aparelhos pequenos. Controles de navegação e tela cheia continuam presentes.
- **Mensagens e notificações** recebem cabeçalho e painel de privacidade próprios, cartões individuais mais legíveis, estados vazios contextualizados e títulos de grupos mais claros.
- A janela **Responder mensagem** usa o recipiente central modernizado e ainda exige revisão, filtragem de conteúdo sensível e confirmação separada para enviar.
- Nenhum protocolo, permissão Android, regra de captura de tela, permissão de mensagem ou confirmação foi alterado. Build Android e revisão visual no dispositivo ainda pendentes.

## Modernização de captura e mapas alpha.96

- `ui/MonitorScreenSkin.kt` introduz cabeçalho com ícone, área de retorno acessível, cartões de estado, indicadores e botões com o mesmo estilo em telas nativas independentes.
- Foto remota: título e identificação compactos, estado destacado e prévia escura; captura JPEG única, zoom, salvar e compartilhar preservados.
- Preparação da foto e câmera compartilhada do monitorado: instruções e status padronizados, mantendo consentimento local obrigatório.
- Câmera ao vivo do controlador: sai do visual escuro rígido e passa a usar tema do aplicativo, tela de imagem e comandos consistentes; não altera o streaming JPEG e não grava automaticamente.
- Localização: cabeçalho e controles harmonizados com as telas de captura; mantém o mapa, os estados GPS e o acompanhamento seguro já existentes.
- Sem mudanças em protocolos, armazenamento, permissões Android nem comandos remotos. Build Android e aparência real ainda dependem do GitHub Actions e dos aparelhos.

## Melhorias visuais alpha.95

Ações de arquivos, ordenação, downloads interrompidos e detalhes de aplicativos usam `MonitorActionSheet`, um menu inferior comum com ícones e estados destrutivos. Configurações ganham agrupamento mais compacto, mantendo pesquisa e consentimentos existentes. Nenhuma ampliação de permissões ou comandos remotos. A modernização dos diálogos especializados restantes seguirá por etapas, após validar a alpha.95 no hardware.

## Modernização visual: alpha.94

- Visual compartilhado em `ui/MonitorVisualKit.kt`, incluindo cabeçalhos com ícones, linhas de ação, chips e avisos.
- Dialogo **Energia e manutenção** reformulado e visual dos avisos e confirmações modernizado.
- Diálogos legados passam por um tema Android arredondado nas variantes clara/escura.
- Não foram modificados protocolos nem permissões; testes de compilação Android e conferência visual continuam necessários.

## Localização: alpha.93

O painel de mapa mostra status real da conexão, idade/precisão do fix e modo Acompanhar durante a tela aberta. A consulta `locationSnapshot` transporta exclusivamente a posição já autorizada, sem ler os demais módulos. O intervalo rápido de 3 s não acelera o GNSS; o custo de rede e bateria pode aumentar quando Acompanhar está ativado. Não supor localização instantânea nem conectividade fora da LAN/VPN.


**Novidades alpha.89:** a pasta de transferências é fixa em **Downloads/Celular Remoto** nos dois aparelhos (Android 10+), criada automaticamente quando o aplicativo abre ou inicia o monitoramento. O envio e download não exigem escolher diretório: ao baixar, salva nessa pasta, com retomada e hash SHA-256, e ao enviar, o controlador escolhe os arquivos individuais, não a pasta remota. O monitorado expõe **somente arquivos que pertencem ao Celular Remoto** no MediaStore, não todo Downloads nem arquivos de outros aplicativos. O antigo seletor de árvore SAF não aparece na Configuração. A chave pareada autentica operações; excluir exige confirmação no controlador. Menus de arquivos como painel inferior e diálogos temáticos mais coerentes nas telas. Requer Android 10+ neste novo modo; aparelhos antigos não acessam a pasta fixa sem seletor. **Compilação Android e testes físicos pendentes.**

**Novidades alpha.88:** o Painel de controle agora exibe 12 ações em **3 colunas**, e Detalhes fica no cabeçalho do aparelho. Configurações ganham uma página inicial de grupos pesquisáveis (GPS, câmera, arquivos, segurança e outras seções), com rótulos objetivos e sem mudar permissões. Os diálogos centrais de aviso/confirmar seguem o tema e usam botões compactos; confirmação explícita permanece obrigatória. A telemetria mostra o tempo real de ida e volta (RTT) da última requisição autenticada, distinto do horário da informação observada. A tela não usa streaming TCP persistente: os 15 s são o intervalo de consulta, não latência. Sem novas permissões, sem alteração de protocolo nem coleta de dados. Compilação Android/validação em aparelhos ainda pendentes.

**Novidades alpha.87:** **Recuperação automática da localização** configurada uma única vez em Configurações no aparelho monitorado, com notificação visível, acesso Android e revogação explícita. Enquanto a sessão estiver ativa, os controladores pareados consultam sem novo toque no monitorado. A restauração após reinício só é tentada quando monitoramento/autostart e permissão de **localização o tempo todo** já foram autorizados; Android/fabricante podem bloquear. Na tela Localização, o mapa de referência aparece mesmo sem fix remoto (NÃO é marcador do aparelho), GPS autenticado recentraliza automaticamente, consultas periódicas ligadas com a tela aberta, status mais compacto e último ponto da tela marcado como antigo se a conexão cair. Sem servidor remoto: rede local/VPN ainda exigida; sem rastreamento secreto, sem ativação silenciosa de câmera, sem GPS ligado por comando do controlador. Compilação e hardware pendentes.

**Novidades alpha.86:** Downloads para **Downloads/Celular Remoto** (Android 10+) têm checkpoints privados no `noBackupFilesDir` e retomada após fechamento/reabertura da tela. O usuário precisa abrir novamente o mesmo arquivo remoto e escolher Salvar em Downloads; a cópia NÃO continua em segundo plano. A retomada volta ao último quadro completo e o servidor relê a origem inteira para validar SHA-256 integral; alterações no arquivo remoto causam rejeição. Uma barra na lista mostra parciais e permite apagá-los, com limpeza automática após sete dias. Destinos SAF arbitrários e upload **não possuem retomada persistente**. Espaço livre necessário no controlador pode chegar a 2× o arquivo ao publicar em Downloads. A velocidade ainda precisa de testes reais.

**Novidades alpha.85:** Recuperar dados ganhou histórico local de até 48 operações/30 dias por aparelho, sem nomes nem caminhos; painel remoto tem grade em duas colunas e diagnóstico de conexão; mapa destaca posição antiga e precisão.

**Novidades alpha.82:** módulo Localização com mapa interno nativo, posição remota obtida do snapshot cifrado, precisão, zoom, centralizar/seguir e consultas automáticas opcionais. Blocos OpenStreetMap somente com autorização por aparelho pareado, cache dos blocos visualizados e atribuição; sem download de áreas offline nem histórico. Perfil GPS intermediário de 30 segundos ativado somente pelo próprio monitorado. **Mantém integralmente o trabalho da alpha.81 de UI e transferência binária.** Build Android e teste físico pendentes.

**Novidades alpha.81:** tela Recuperar dados redesenhada com informações grandes e separadas, área superior corrigida para barras do Android e leitura de taxa em janela móvel. Download SAF agora usa quadros **binários** com AES-256-GCM por bloco, desafio específico por sessão e SHA-256 completo, eliminando a dupla codificação Base64/JSON da alpha.80. Ainda é um socket e um arquivo SAF aberto por vez. **Velocidade não medida em aparelhos nem build Android validado nesta entrega**; uploads, autorização local, limites e pausa só nesta Activity seguem como antes.

**Novidades alpha.79:** Recuperar dados passa a oferecer gerenciamento autorizado da árvore SAF: upload em lotes via cache privado e validação SHA-256, criar pastas, renomear, mover/copy (quando permitido pelo provedor), lixeira interna opcional e exclusão com confirmação. A permissão de escrita é independente da leitura e desligada por padrão. Seleção mostra checkboxes e barra persistente; menu usa ícones, Downloads pode receber arquivos pelo MediaStore (Android 10+), arquivos de até 2 GiB e blocos de leitura de 192 KiB com velocidade e previsão. Pausar/retomar só vale enquanto a tela está aberta; não existe retomada persistente após terminar o processo. Aplicativos com a chave de pareamento compartilham os mesmos direitos de escrita concedidos localmente. **Build Android e dispositivos físicos pendentes.**

**Correção alpha.77:** os logs do workflow #72 registraram falha em `:app:compileDebugKotlin`, `MonitorService.kt:308`, pela referência inexistente `lastControllerName` no encerramento do serviço. Essa atribuição residual foi removida, preservando a limpeza de `lastControllerSeenMs`, as notificações e o registro de acessos. O verificador de regressão impede retornar à referência inexistente. Nenhuma mudança de permissão, GPS, criptografia ou protocolo. **Reexecutar CI para confirmar build Android.**

**Atualização alpha.78:** aba Aplicativos com ícones reais solicitados em lotes de até 12, favoritos por aparelho, busca, detalhes, paginação e filtro do último app observado (não é confirmação de execução contínua). Abrir por API normal apenas com opt-in local e Celular Remoto visível no monitorado, com fallback manual para ADB TLS. Forçar parada somente com ADB autorizado, verificação de pacote e confirmação digitada, nunca automaticamente. Energia e Manutenção reúne bloqueio, instruções para acender tela e reinicialização ADB sem desligamento total. Não adiciona integração direta com Minha Câmera. **Compilação Android e testes nos dois aparelhos pendentes.**

**Alterações alpha.76:** ícone adaptativo próprio de escudo para o launcher e glifo monocromático de proteção nas notificações. A notificação permanente do serviço distingue aguardando controlador, acessos autenticados recentes (expiram após 45 s) e reconexão, com resumo de localização, tela, câmera e alertas conforme estados locais. A ação **Detalhes** abre uma tela dedicada; **Parar** permanece. Atualização deduplicada sem avisos repetidos; versão pública genérica na tela bloqueada. Nomes de controladores são autodeclarados, não identidades verificadas. Nenhuma nova permissão, rastreamento ou alteração do transporte AES-GCM. **Compilação Android/testes físicos pendentes.**

**Alterações alpha.75:** módulo **Localização** utilizável em LAN/VPN sem exigir internet para obter/transmitir coordenadas. Somente o monitorado pode iniciar uma sessão explícita em Configurações → Localização, com permissões Android e notificação permanente com Parar. `LocationShareService` (FGS location) usa GPS/rede com perfis Econômico (5 min), Equilibrado (60 s) e Preciso (10 s), cadências mínimas não garantidas. `LocationShareStore` mantém última posição **em memória**, sem histórico; snapshot AES-GCM contém latitude/longitude, precisão, fonte, idade e estados conservadores apenas durante sessão autorizada. No controlador, Localização abre uma tela de consulta, permite copiar coordenadas ou enviá-las para aplicativo de mapas instalado. **Sem mapa interno ou promessa de mapas offline**, sem rastreamento invisível, sem acionamento remoto de GPS, sem autorização individual entre controladores que compartilham uma chave. Se o monitoramento parar, a sessão de GPS também para. A versão alpha.75 exigia igualdade entre os aparelhos; a alpha.77 também exige ambos atualizados. **Build Android e ensaios físicos ainda pendentes.**

**Mudanças alpha.72:** cartões de Bateria, Temperatura e Rede mais compactos; estado da tela e bloqueio, disponibilidade aproximada da câmera e aplicativo em primeiro plano (acesso ao uso opcional), SSID/RSSI de Wi-Fi após autorização Android e cores orientativas por temperatura da bateria. Recuperar dados: cópia limitada de pastas, limite correto de 12 níveis, rejeição de nomes duplicados, dois blocos criptografados por requisição, repetição curta em erros transitórios e SHA-256 integral após copiar. **Sem retomada persistente após reinicialização, nem worker foreground para cópia**. Ainda exige CI/Android e testes físicos.

**Mudanças alpha.71 (histórico):** corrigida falha `:app:compileDebugKotlin` do workflow #66 no visualizador de tela: o `Runnable` de polling agora possui tipo explícito e reagenda o mesmo objeto (`this`) no callback, sem referenciar a própria propriedade durante a inicialização. Mantidas todas as correções da alpha.70 (selos de Configurações, sessão da câmera e executor único). **A correção ainda precisa de nova execução do Gradle/Android e testes físicos.**

**Correção anterior (alpha.69):** a execução #64 do GitHub Actions falhou na compilação Kotlin da alpha.68 por quatro atribuições inválidas a `TextView.lineSpacingExtra` e quatro acessos a `MainActivity.darkTheme()` privado. Corrigido com `setLineSpacing(extraPx, 1f)` e função de tema `internal`; o visual de Configurações foi preservado. **O build Android completo da alpha.69 ainda precisa ser executado na CI.**

**Novidade da alpha.68:** a área de Configurações recebeu uma revisão de interação para reduzir confusão ao ativar, desativar, escolher e confirmar ações. Os cartões agora usam tipografia maior, cartões mais altos, estados visuais em formato de botão (`ATIVADO`, `DESATIVADO`, `ABRIR`, `REVOGAR`) e diálogos próprios com botões grandes do app para confirmação. Nenhuma permissão nova foi adicionada.


**Correção alpha.67:** no workflow GitHub Actions #62, a compilação Kotlin passou, mas os testes unitários falharam em 5 cenários de `CameraFrameStoreGenerationTest` por usarem `android.os.SystemClock` no ambiente JVM local. O armazenamento volátil da câmera agora utiliza relógio monotônico do processo, compatível com Android e JVM, sem modificar permissões/protocolo. O build completo da alpha.67 ainda exige validação pela CI.

**Atualização alpha.65:** Câmera remota agora pode pedir troca frontal/traseira apenas durante uma captura visível e iniciada no monitorado, após marcar a autorização temporária **na tela de câmera**. A preferência não persiste; o comando AES-GCM exige token da sessão e não pode iniciar captura. O botão remoto só aparece habilitado quando a autorização real é informada. Sem vídeo gravado, microfone ou novas permissões. Build Android e testes em aparelhos reais ainda pendentes.

## Câmera JPEG: orientação e estabilidade (alpha.63)

- Quadros são rotacionados efetivamente em NV21 para seguir orientação Camera2 e rotação da tela; apenas a imagem remota/salva recebe essa correção. A prévia local pode variar conforme dispositivo e não é garantido que tenha a mesma transformação.
- Tokens de sessão evitam publicar JPEG atrasado ao trocar câmeras. Encerrar monitoramento/serviço revoga quadros; callbacks de erro limpam recursos da câmera.
- **Sem alterações de consentimento:** somente iniciar câmera na tela do monitorado, ativa em primeiro plano; sem iniciar captura à distância, gravar áudio/vídeo ou incluir serviços de câmera em segundo plano. Limites 110 KB e até 1 quadro/s permanecem.
- **Pendente:** compilação completa Android e testes físicos de orientação, troca de lentes, latency/consumo e falhas. Todos os dispositivos pareados devem usar a mesma alpha.63.

## Câmera remota JPEG com início local (alpha.62)

- No **aparelho monitorado** (monitoramento ativo), abra **Modo monitorado > Abrir câmera neste aparelho**, confirme a explicação e conceda a permissão Câmera solicitada pelo Android. A prévia local indica que a câmera está transmitindo; fechar a Activity, usar Voltar ou bloquear a tela interrompe a captura e remove os quadros da RAM.
- No controlador pareado, abra o cartão **Câmeras** para acompanhar JPEG de baixa taxa (alvo máximo **1 quadro/segundo**, sem promessa de latência). Os quadros via Camera2 são consultados pela operação `cameraFrame` já cifrada por AES-GCM e por versão exata; não há nova porta ou comando ADB.
- A câmera frontal/traseira é escolhida **localmente no monitorado** pela tecla Trocar câmera. O controlador **não pode abrir a câmera à distância**, alternar sua lente ou ativar captura oculta. **Salvar foto** salva um quadro recente somente após toque explícito no controlador; nenhuma imagem é salva automaticamente no monitorado.
- A imagem é limitada a 110 KB, permanece somente em RAM e expira após 3 segundos. Não há microfone, gravação de vídeos, áudio, câmera em segundo plano, nova permissão invasiva ou serviço de câmera no manifesto.
- Limitações: JPEG de prévia sem correção automática da orientação/espelhamento em todos os fabricantes; não é streaming H.264 de câmera. Câmera ocupada/ausente, permissão negada ou saída da tela encerram a visualização. **Build Android completo e testes em dois dispositivos pendentes**.

## Bloquear aparelho remoto (alpha.61)

- Em **Configurações > Bloqueio remoto** no **celular monitorado**: ative manualmente o administrador de dispositivo pela tela oficial do Android, configure PIN/padrão/senha e depois autorize **Permitir bloqueio remoto**. A autorização remota é desativada por padrão, independente do administrador do Android.
- O único privilégio solicitado ao Android é `<force-lock />`, via `DevicePolicyManager.lockNow()`. Não há permissão para apagar dados, trocar senha, burlar biometria ou criar comandos ADB livres. O Android permite revogar o administrador nas configurações de segurança; a opção remota pode ser revogada no app.
- No controlador pareado, abra **Bloquear** e digite literalmente `BLOQUEAR` antes de tocar em **Bloquear agora**. A operação usa AES-GCM, versão exata e proteção contra repetição do protocolo existente, com intervalo mínimo de 15 s entre pedidos.
- O comando responde **solicitação aceita**, não **aparelho bloqueado**: a API não confirma conclusão. A tentativa é agendada com breve atraso para permitir resposta pela rede. A perda de conexão após desligar a tela é possível. Não usar se não for possível acordar/desbloquear aparelho sem botão Power.
- O pedido é recusado se o administrador estiver inativo, faltar bloqueio seguro (PIN/padrão/senha), a autorização local estiver desligada ou o Android não tiver suporte. **Build Android completo e testes em aparelhos continuam pendentes**.

## Controle remoto de som e lanterna (alpha.60)

- Acesse **Configurações > Som e lanterna** no **celular monitorado**. Ambas as autorizações vêm desativadas e devem ser concedidas separadamente no aparelho. O monitoramento precisa continuar ativo, os dois dispositivos pareados e instalados na mesma versão do aplicativo.
- No painel do controlador, **Tocar som** abre opções de 5, 10, 20 ou 30 segundos, Iniciar/Interromper e Atualizar estado. Usa bipes temporários pelo volume ALARME, sem forçar o volume. Modo silencioso, Não Perturbe e configurações do fabricante podem impedir audição.
- **Lanterna** usa a câmera traseira com flash, `CameraManager.setTorchMode()` e `TorchCallback`. A permissão Câmera do Android pode ser exigida; veja a permissão nas configurações do app no aparelho monitorado. Se câmera/flash estiverem ocupados ou ausentes, o app exibe falha e não inventa estado. A confirmação do estado pode atrasar.
- Desativar permissão de som ou monitoramento local encerra alerta. Desativar a permissão de lanterna ou parar o serviço tenta desligar a lanterna **ligada pelo Celular Remoto**, sem se apropriar de flash iniciado por outro aplicativo. Se o processo morrer inesperadamente, o Android poderá desligá-la por conta própria, mas não há garantia em todos os fabricantes.
- As ações usam somente a operação cifrada de lista fechada `deviceControl`; sem novos privilégios ADB, serviços ocultos, novas permissões manifest ou aumento de volume do aparelho.
- **Validação**: executar build Gradle no GitHub Actions e testar ambos os aparelhos antes de assumir que o som será audível ou a lanterna compatível com determinado modelo.

## Bateria no painel (alpha.59)

- **Painel Controle:** porcentagem reportada pelo Android, ícone com ⚡ quando há informação confirmada de carregamento ou bateria cheia alimentada, e texto curto com estado e fonte (USB, Tomada, Sem fio ou Base).
- Estados provenientes de `ACTION_BATTERY_CHANGED`: **Carregando, Descarregando, Cheia, Não carregando** e **Desconhecido**. O Android distingue `NOT_CHARGING` de `DISCHARGING`, por isso o app também os distingue.
- O monitorado envia `state`, `statusCode`, `powerSource`, `pluggedCode` e `chargingIndicator` junto ao snapshot autenticado; temperatura continua ao lado e na tela Detalhes. Sem permissões novas nem consulta de bateria fora do polling existente.
- Valores ausentes permanecem desconhecidos, não se presume USB/tomada a partir de corrente, porcentagem ou cabo. Exige a mesma versão nos dois aparelhos.
- **Alpha.59:** Tocar som e Lanterna foram adiados nessa versão e implementados na alpha.60, com autorização local individual.

## Notificações e aplicativos (alpha.58)

- **Notificações:** tela independente em cartões, atualização sob demanda, diagnóstico de listener/permissões/filtros e recuperação apenas dos avisos ainda ativos após reativação do listener Android. Eventos permanecem voláteis e filtrados.
- **Aplicativos instalados:** inventário remoto de apps visíveis no lançador, com busca, nomes e IDs, consultável somente após autorização nas Configurações do monitorado. Não acessa dados internos dos apps nem usa `QUERY_ALL_PACKAGES`.
- **Limites conhecidos:** o Android pode não entregar notificações removidas, silenciosas ou protegidas; reinícios do processo apagam o histórico em RAM. A lista de apps não inclui necessariamente todos os pacotes instalados.

## Recuperar dados e manutenção (alpha.57)

- **Recuperar dados**: tela de gerenciador SAF remoto com topo compacto (Voltar/Ajuda/Atualizar), pasta e caminho, lista ou grade, busca local, ordenação por nome/data/tamanho/tipo e informações de arquivos.
- **Copiar individual**: toque em um arquivo → Copiar e escolha o destino pelo seletor de documentos do Android.
- **Copiar múltiplos**: toque em Selecionar, marque até 20 arquivos e escolha Baixar, Downloads, Mover/Copiar na origem ou Excluir, conforme o opt-in de gerenciamento no monitorado. O lote tem limite de 2 GiB; arquivos são transmitidos em sequência com hash e sem sobrescrever nomes existentes. A leitura não concede escrita automaticamente.
- **Ajuda (?)** explica a autorização obrigatória no celular monitorado; não há acesso a arquivos excluídos, privados ou protegidos por outros apps.
- **Manutenção** tem pop-up padronizado, cartão com identificação do alvo, alerta para celulares sem botão Power e reinício somente via ADB do mesmo host, com confirmação digitada REINICIAR. A depuração pode não voltar após o reinício.
- Ainda sem prévia local de imagem/PDF/vídeo, compartilhamento automático de cópias, escrita, exclusão ou renomeação no aparelho remoto. Validar no GitHub Actions e com dois celulares.


## Melhorias alpha.56: painel ADB e tela remota

- Cartão ativo com botões em duas colunas e espaçamento, sem duplicar o aparelho em Outros celulares salvos. O histórico não é redesenhado continuamente. Heartbeat e comandos ADB são serializados, com tolerância maior em Wi-Fi instável.
- Botões **Renomear**, **Desconectar**, **Controles** e **Manutenção**; Controles ADB em grade compacta, com Renomear disponível.
- Nome do aparelho preserva apelidos e modelo reconhecido quando o `getprop` falhar. O IP continua diagnóstico, não identificação de pessoa/aparelho.
- **Ver tela:** seleção JPEG (padrão de menor atraso observado) ou H.264 (experimental), com estados visuais claros.
- Ao bloquear/apagar a tela do monitorado, avisos de toque diferenciam a causa e orientam nova autorização local quando necessário. Android 15 QPR1+ pode encerrar MediaProjection ao bloquear.
- Não foram adicionados desbloqueio remoto de PIN/biometria, nova permissão, serviço de captura oculto ou comandos ADB arbitrários.


## Correção do workflow 53 (alpha.55)

- CI chegou a `:app:testDebugUnitTest`, com **67 testes e 1 falha**: `AdbPolicyTest.commandAllowlist`. A asserção de tamanho da allowlist estava desatualizada (4 versus 7 comandos disponíveis).
- O teste agora verifica todos os nomes/comandos shell explicitamente autorizados, incluindo diagnóstico, heartbeat, navegação e volume, e rejeita comandos de alto risco não previstos.
- Nenhum novo comando ADB foi habilitado nesta correção. O reinício de manutenção continua isolado e exige confirmação digitada. **O build Android ainda precisa ser reexecutado.**

## Correção do workflow 52 (alpha.54)

- Corrigido o erro Kotlin `Unresolved reference 'lastRecoveryAttemptMs'` na `MainActivity`, incluindo a propriedade que limita tentativas de recuperação do serviço a uma por minuto usando `SystemClock.elapsedRealtime()`.
- Preservadas as funcionalidades da alpha.53, incluindo manutenção com reinício ADB mediante confirmação, acesso remoto não intrusivo e tela Recuperar dados com Voltar.
- A análise dos logs comprovou falha de compilação em `:app:compileDebugKotlin`, não falha do Gradle ou da assinatura. O build Android da alpha.54 precisa ser executado no CI.

## Manutenção e navegação sem bloqueios (alpha.53)

- O painel Controle traz o cartão **Manutenção**, com pop-up oferecendo somente **Reiniciar aparelho · ADB**. A operação só fica disponível após (re)conectar a Depuração sem fios do mesmo aparelho; não depende apenas da conexão normal AES-GCM.
- O comando ADB é exclusivamente `reboot:` (reinício do sistema). Antes de executar, há aviso sobre aparelhos sem botão Power e confirmação digitando **REINICIAR**. Nenhum comando poweroff, shutdown ou recovery e nenhuma repetição automática da reinicialização em caso de falha.
- A tela **Acesso remoto detectado** não abre mais por cima do aplicativo: os acessos permanecem visíveis no cartão Monitorado e na notificação do Android. Detalhes só abrem ao tocar na notificação, com botão **Voltar ao aplicativo**.
- **Recuperar dados** tem botão Voltar visível, aparência ajustada e instruções para autorizar a pasta SAF no próprio monitorado quando a listagem falhar. Continua sem acesso oculto aos arquivos.
- A Depuração sem fios pode não voltar depois de reiniciar; aparelho sem botão Power pode ficar inacessível. Testes físicos e compilação Android completa pendentes.

## Ver tela com controle ADB (alpha.52)

- No **Modo avançado ADB**, após conectar, aparece **Ver tela remota • controle ADB**. Só abre se o mesmo IP também possuir uma chave normal do Celular Remoto salva no controlador. Sem ela, explica como autorizar a captura no monitorado.
- Ao abrir o visualizador por esse botão, a sessão ADB é transferida **somente enquanto Ver tela está em primeiro plano**, sem serviço oculto ou reconexão em segundo plano. Sair da visualização encerra a sessão ADB; ao retornar ao Modo avançado ADB, a reconexão da alpha.51 pode tentar restabelecê-la se o dispositivo ainda estiver selecionado.
- A tela **Ver tela** informa os canais separadamente: **Navegação: ADB conectado** e **Imagem: Celular Remoto (MediaProjection)**. O indicador nunca afirma que a imagem foi transportada por ADB.
- **Voltar/Início/Recentes** utilizam comandos ADB restritos, somente após resposta autenticada do canal de imagem para o mesmo endereço. **Toque**, **Notificações**, **Áudio**, **JPEG/H.264** e captura permanecem no protocolo normal e exigem as autorizações anteriores. Se ADB cair, a navegação pode voltar ao canal normal quando a Acessibilidade estiver autorizada.
- Continuam pendentes o streaming de imagem via ADB, medições de latência e testes em dois celulares. A reconexão automática ADB da alpha.51 atua na tela ADB, não dentro do visualizador nesta versão.

## Reconexão ADB com verificação de resposta (alpha.51)

- Depois de conectar a um dispositivo autorizado, se o socket ADB cair, o controlador tenta reconectar **automaticamente enquanto o Modo avançado ADB estiver visível**.
- O app envia verificação de resposta ADB a cada 30 segundos; se não responder em até 8 segundos, fecha a sessão e tenta última porta ou descoberta mDNS no mesmo IP. Backoff: 2, 4, 8, 15 e até 30 segundos entre tentativas.
- **Cancelar reconexão automática** interrompe as tentativas. Ao sair da tela, a conexão é encerrada por segurança; ao reabri-la na mesma Activity, a retomada escolhida anteriormente pode continuar.
- **Comandos ADB** estão disponíveis no resumo após conexão: Voltar, Início, Recentes e volume +/−. **ADB não substitui transmissão de tela**; vídeo continua exigindo MediaProjection e consentimento no monitorado.
- Se Depuração sem fios for desligada, o app não pode ativá-la sozinho em Android comum. Na mesma rede, pode reconectar quando o Android/usuário a reativar. Mudança de IP, rede sem mDNS, autorização revogada e sistema bloqueando conexões ainda exigem intervenção.

## Modo ADB simplificado (alpha.50)

- **Tela principal reduzida:** estado real, celulares salvos com Reconectar/Desconectar, **+ Adicionar outro celular** e **Configurações avançadas** recolhidas. Os comandos Voltar/Início/Recentes ficam em **Controles ADB**, no menu avançado.
- **Pop-ups centralizados:** escolhas de papel (controlar/permitir), método (QR/código), QR temporário, formulário de seis dígitos e conexão manual são janelas curtas, não páginas longas.
- **Após o QR ou código:** o app tenta descobrir a porta `_adb-tls-connect` por mDNS **no IP do aparelho que acabou de parear**, conecta por TLS e só mostra Conectado após sucesso real. Rede sem mDNS ou portas ambíguas exibem fallback manual.
- **Reconexão rápida:** tenta o IP/porta anteriores e, quando falha, procura porta atual no mesmo IP; se a identificação mudar, exige confirmação. Mudanças de IP ainda podem exigir entrada manual.
- **Identificação:** consulta o modelo e, quando disponível, o nome comercial do Android automaticamente; opção Renomear fica em Opções do aparelho.
- **Segurança:** não desbloqueia PIN/biometria, nem utiliza ADB para capturar tela. QR protegido por `FLAG_SECURE` enquanto visível; encerrar a Activity encerra a sessão. O protocolo normal AES-GCM/MediaProjection permanece independente.
- **Validação:** verificações estáticas não equivalem a build Gradle nem teste ADB em dois celulares.

## Correção da compilação (alpha.46)

- Corrigidas as duas chamadas `setTextColor(foreground)` de `RemoteFilesActivity.kt` (linhas 59 e 66 da alpha.45): `foreground` dentro de `TextView.apply` e `Button.apply` era a propriedade da `View` (`Drawable`), e não a cor (`Int`) definida na Activity. O receptor agora é explícito: `this@RemoteFilesActivity.foreground`.
- Sem mudança de permissão, protocolo, pasta SAF autorizada, política de transferência nem fluxo de cópia. Regressão adicionada ao verificador.
- CI #45 falhou na compilação Kotlin antes de gerar APK; alpha.46 requer novo workflow para validar a compilação Android completa.

## Recuperar dados (alpha.45)

- **Monitorado:** Configurações → Arquivos e recuperação → Pasta autorizada → Escolher pasta no seletor SAF do Android. Conceda leitura somente da pasta necessária. O servidor precisa estar ativado; a autorização fica salva até você revogar no mesmo menu.
- **Controlador:** conecte o telefone com QR/chave e toque em **Recuperar dados**. Navegue nas subpastas autorizadas, escolha o arquivo e use o seletor Android **Salvar como** para criar uma cópia no controlador. Não copia nem altera a origem.
- Tráfego pelo protocolo existente AES-GCM autenticado, com verificação SHA-256 por bloco e do arquivo completo, até dois blocos de 48 KiB por solicitação, proteção de tamanho/caminho e tentativa de remover destino parcial se falhar ou cancelar. Apresenta progresso; controle de cancelamento e mensagens de falha.
- Limites na alpha.79: 2 GiB por arquivo e lote, 150 entradas por diretório e até 12 níveis. Downloads não requer seleção da raiz pelo SAF quando o controlador usa MediaStore. Arquivos de tamanho desconhecido e arquivo vazio na cópia de leitura ainda não são suportados; pasta recursiva limitada a 20 arquivos e 200 itens. Operações dependem do DocumentsProvider e da permissão local de escrita. Não há acesso irrestrito, root ou ADB para arquivos.
- O Android 11+ restringe escolha de raiz e determinadas pastas. A capacidade depende do provedor SAF e permissões persistidas. Se a rede cair, poderá haver arquivo parcial no destino; revise antes de usar.
- **Compilação Android e testes entre aparelhos ainda pendentes** nesta entrega; validar no GitHub Actions antes de instalar.

## Correção alpha.44

- Corrigidos quatro pontos de compilação na interface ADB: o Kotlin associava lambdas ao parâmetro de retorno (`String`) e não à operação (`AdbClient`), causando `Unresolved reference pair/execute`. Agora todas as chamadas de `runOperation` têm `operation = { adb -> ... }`.
- O comportamento do ADB, consentimento e modos JPEG/H.264 foi preservado. Esta é uma correção de compilação, não um novo benchmark de latência.

## Melhorias alpha.43

- Em Configurações → Modo avançado ADB: a tela comum permite screenshots, mas o QR temporário permanece protegido contra captura enquanto visível. O guia explica claramente qual telefone gera e qual lê o QR e distingue porta de pareamento da de conexão.
- Após conectar via ADB, o controlador mostra o IP/porta e, quando disponível, o modelo/Android declarados pelo outro aparelho (não é prova criptográfica de identidade física).
- A tela Ver tela inicia usando JPEG. O H.264 permanece disponível por escolha manual como experimental, e o servidor evita drenar um acúmulo de quadros AVC antigos. Ainda são necessários testes reais de latência.

## Modo avançado ADB por Wi-Fi (alpha.42)

Configurações → Modo avançado ADB oferece QR de 2 minutos, pareamento por código, descoberta mDNS, IP/portas manuais, conexão TLS, diagnóstico de modelo/Android e comandos Voltar/Início/Recentes com confirmação.
A Depuração sem fios deve ser habilitada manualmente no monitorado (Android 11+ ou ROM compatível). O controlador continua Android 8+. O QR é do protocolo ADB (`WIFI:T:ADB`), distinto do QR normal do Celular Remoto.
A identidade RSA é protegida pelo cofre AES-GCM/Android Keystore. Não é preciso Termux/PC em tempo de uso. Ao sair da tela, a conexão é encerrada; a identidade permanece. Portas não são persistidas e devem ser redescobertas ou informadas novamente.
Esta versão **não implementa espelhamento via ADB, tela física apagada via ADB, desbloqueio, reconexão em segundo plano ou shell livre**. Leia [arquitetura, dependências e testes ADB](docs/ADB.md). Build Android Debug completo e 54 testes JUnit aprovados; validação física permanece pendente.

## Controles flutuantes em tela cheia (alpha.41)

- Em **Ver tela → Tela cheia**, a imagem ocupa o espaço disponível com duas barras pequenas e semitransparentes sobrepostas: na parte superior Sair, Ajustar/Preencher, Print, Áudio e Foto HD; embaixo Voltar, Início, Recentes, Notificações, Toque e Acender tela. Os ícones permanecem legíveis e não interceptam gestos fora da área dos botões.
- As ações usam a mesma autenticação AES-GCM, autorizações e estado da sessão já existentes. **Voltar/Início/Recentes/Notificações** são independentes do modo Toque, mas exigem Acessibilidade habilitada e captura consentida. Despertar a tela depende de autorização local separada, desligada por padrão.
- O indicador compacto mostra o estado real do codec, a latência de rede informada pela consulta e FPS calculados a partir dos quadros efetivamente apresentados. O valor só aparece após pelo menos dois quadros; não é uma medição ponta a ponta. As barras respeitam recortes do display; a imagem mantém o mapeamento correto de toques em Ajustar e Preencher.
- O modo tela cheia é recuperado após recriação da Activity. Para ver em aparelhos reais, conferir rotação, acessibilidade, impressão, áudio, início e saída do modo imersivo. Nenhuma melhoria de codec ou FPS foi inventada nesta versão.

## H.264 experimental e acender tela (alpha.40)

- **Fluxo H.264 experimental:** `MediaCodec` no monitorado codifica amostras YUV_420_888 de quadros autorizados em AVC e o controlador decodifica com `MediaCodec`. Alvo máximo de ~10 FPS, não garantia; resolução limitada a 432x896, bitrate de 1 Mbps e fila em RAM de 30 amostras. A consulta TCP autenticada continua sendo uma por quadro: é uma primeira etapa rumo a transporte de vídeo persistente e pode não melhorar a latência em todos os aparelhos.
- **Fallback JPEG:** quando codec AVC não funciona ou a decodificação falha, retorna ao JPEG; a fotografia HD permanece baseada na mesma sessão autorizada pelo Android e não grava vídeo.
- **Acender tela:** opção explícita em Configurações > Tela e controle remoto (desligada por padrão), botão Acender tela no controlador, opção adicional de solicitar ao abrir Ver tela (OFF por padrão), wake lock temporário de até 1,5 s com limite de tentativas de 12 s. Só tenta ativar o display com comando autenticado; **não remove PIN, senha, biometria ou proteção `FLAG_SECURE`**, não reinicia MediaProjection encerrada. Pode ser bloqueado pelo Android/ROM, inclusive segundo plano.
- **Permissões:** `WAKE_LOCK` e `TURN_SCREEN_ON` no Manifest, com autenticação AES-GCM e chave pareada. Sem novos serviços exportados, captura oculta, gravação ou abertura de porta pública.
- **Validação:** testes locais de estrutura/sintaxe e limite de buffer não substituem compilação Android nem ensaio de codec no hardware. Benchmark de FPS, delay, temperatura e bateria pendente.

## Controle e visualização corrigidos (alpha.39)

- **Navegação remota:** Voltar, Início, Recentes e Notificações agora executam ações globais do Android via Acessibilidade, sem depender do botão "Toque na imagem". Cada comando segue autenticado e sujeito à autorização de Acessibilidade e à sessão de tela vigente. "Toque" habilita apenas gestos na imagem.
- **Topo protegido:** página Ver tela usa `WindowInsets` da barra de status, recorte de câmera e navegação em Android 15/16. Os botões deixam de competir com relógio e indicadores.
- **Tela cheia real:** modo imersivo refeito, com saída visível, opção Ajustar (FIT_CENTER) ou Preencher (CENTER_CROP) e ajuste de coordenadas de toque para cada escala. Preencher pode cortar parte da imagem.
- **Menor atraso em JPEG:** intervalo de captura alvo 330 ms com viewer ativo e consultas a cada 300 ms após resposta; o frame mais recente substitui o anterior, sem enfileirar vídeos. Em economia de energia, intervalo de captura volta a 700 ms. Exibe duração da consulta e idade do frame na origem. NÃO é H.264 e não garante FPS/latência em qualquer rede.
- **Tela apagada/bloqueada:** diagnóstico distingue tela inativa, bloqueio seguro e projeção encerrada; navegação não tenta contornar PIN, biometria ou novo consentimento do Android. Android 15 QPR1+ encerra projeção após bloqueio.
- **Validação pendente:** build Android no GitHub Actions, UI com Android 15/16, rotação, toque, bateria e latência em aparelhos reais.

## Recuperação programada do serviço (alpha.38)

- **Configurações → Serviço e recuperação:** ativar **Recuperação agendada (15 min)**, com confirmação de autorização no próprio monitorado.
- Serviço foreground normal continua com `START_STICKY` e supervisor da porta a cada 15 segundos. JobScheduler opcional pode tentar reabrir o serviço somente se a autorização persistir, sujeito às restrições Android 12+ e Doze. Se o sistema bloquear, notificação orienta retomada manual.
- A tela Monitorado tenta retomada ao permanecer aberta, mesmo sem novo `onResume`; Diagnóstico mostra serviço Android, listener e último evento.
- **Parar** cancela o agendador. Não reabre MediaProjection nem burla force-stop; início após reiniciar continua sendo permissão separada.

## Correção de compilação do workflow #37 (alpha.37)

- Corrigida a referência `Base64` em `RemoteClient.kt` com importação de `android.util.Base64` na decodificação da Foto HD.
- Corrigida a propriedade `ScrollView.isFillViewport` em `RemoteScreenActivity.kt`, que impedia `compileDebugKotlin`.
- Adicionadas verificações de regressão e sincronizado `versionName/versionCode/AppVersion`. Nenhuma alteração em permissões, captura, criptografia ou protocolo além da exigência de versão exata.
- **Status:** o workflow #37 falhou ao compilar a alpha.36; a compilação Android da alpha.37 ainda depende de nova execução no GitHub Actions.

## Ver tela: nova interface e capturas manuais (alpha.36)

- **Interface redesenhada:** cabeçalho compacto Voltar, Print, Áudio, Tela cheia; painel de imagem arredondado; controles Toque, Voltar remoto, Início e **Foto HD**. Respeita modo claro/escuro.
- **Print:** toque no ícone de câmera para salvar um quadro JPEG recente da prévia no controlador. Não salva um frame antigo de sessão caída. Pressionar e segurar a câmera também aciona Foto HD.
- **Foto HD:** solicita uma única imagem mais nítida (até 1080 px de largura, com redução progressiva para respeitar 480 KiB) pela sessão MediaProjection *já* autorizada. Não é captura furtiva nem vídeo, e pode falhar quando não houver novos quadros ou em telas protegidas.
- **Salvar:** Android 10+ usa MediaStore em `Imagens/Celular Remoto`, sem permissão ampla; Android 8–9 usa seletor do sistema para o usuário escolher destino.
- **Tela cheia:** oculta barras do sistema temporariamente, mantém aspecto da imagem e mapeamento correto de toques. Botão visível permite sair. Não há gravação automática, nem gravação de áudio.
- **Segurança:** solicitações são autenticadas no mesmo canal AES-GCM; foto fica temporariamente na RAM do monitorado e só é gravada no controlador mediante ação explícita.
- **Validação:** checagens locais não equivalem à compilação do APK e aos testes reais no Android.

## Controle remoto e áudio interno (alpha.35)

- **Monitorado → Configurações → Tela e controle remoto**: ative **Controle por toque** e habilite manualmente **Celular Remoto - controle por toque** no serviço de Acessibilidade do Android. O serviço **não coleta árvores de interface nem textos**; só despacha gestos enquanto houver projeção ativa e opção local ligada. Desative no mesmo menu a qualquer momento.
- **Áudio interno**: nessa categoria, autorize RECORD_AUDIO e ative a opção. Depois pare e recomece a sessão de tela para inicializar a captura de áudio. Apenas reprodução de aplicativos que permitem `AudioPlaybackCapture` (Android 10+) pode ser transmitida. Microfone e chamadas protegidas **não** são capturados.
- **Captura de tela**: ao iniciar **Compartilhar tela** no monitorado e aprovar no diálogo oficial Android, a sessão continua mesmo que o controlador perca Wi-Fi, permitindo reconexão sem novo consentimento **enquanto a projeção ainda estiver ativa**. Encerrar a captura, bloquear tela quando o Android a encerra, reiniciar, ou forçar parada exige aprovação nova. Não é possível permitir captura invisível automaticamente.
- **Controlador → Ver tela**: os botões **Toque**, **Ouvir áudio**, **Voltar remoto** e **Início remoto** operam somente quando o monitorado autorizar cada recurso. Tocar, manter pressionado e arrastar na imagem enviam comandos normalizados, respeitando barras laterais. Selecionar **tela inteira** no diálogo do Android oferece melhor correspondência de toques do que compartilhar uma janela isolada.
- Se não houver projeção ativa, **Ver tela** envia **uma solicitação de autorização visível ao monitorado** (notificação limitada a uma por minuto). Apenas o próprio monitorado pode iniciar a captura; não há execução oculta, gravação nem reinício após encerramento.
- **Áudio experimental**: PCM 16 kHz mono, até 500 ms por bloco efêmero em RAM, consulta periódica criptografada pela rede. Pode haver atraso, silêncio e cortes; a prévia usa ~1-2 quadros/s, com codificação JPEG somente quando há visualizador conectado, portanto toque remoto será limitado. Não há streaming H.264 nem audio em alta fidelidade.
- Ao desativar o monitoramento, a sessão de captura é parada. Pareamento e AES-GCM são preservados; aparelhos com a **mesma chave** têm acesso durante sessão explicitamente autorizada. Proteja e revogue a chave quando necessário. **Instale alpha.37 nos dois aparelhos.**

## Ver tela autorizada (alpha.34)

- No aparelho **monitorado**: com o monitoramento ativo, toque em **Compartilhar tela** e confirme no diálogo oficial de captura do Android. A captura só começa após o consentimento local e mostra notificação permanente com ação **Parar tela**. A seção Monitorado também oferece parada explícita.
- No **controlador**: abra aparelho conectado → **Ver tela**. A tela remota é apresentada em uma página própria e consultada pelo canal AES-GCM já pareado. Controle por toque e som interno são opcionais na alpha.35; há exportação manual de capturas, sem gravação contínua.
- Prévia de baixo consumo: aproximadamente **1-2 quadros JPEG/s enquanto houver visualizador**, largura comprimida a até **540 px**, até 110 KiB de imagem por quadro, limite de idade de 3 segundos, sem persistir em disco. O controlador consulta enquanto a página está visível e limpa a visualização ao sair. Uso contínuo ainda aumenta o consumo de bateria.
- **Condições do Android**: consentimento para **cada sessão**; Android 14+ impede reutilização do token. Android 15 QPR1+ exibe indicação destacada e encerra projeção ao bloquear a tela. É necessário reautorizar depois da parada. O app nunca reinicia a captura sozinho, nem após boot, nem após queda do serviço.
- Uma chave compartilhada permite que aparelhos anteriormente pareados assistam durante uma sessão autorizada. Rotacione a chave se quiser revogar todos os controladores. Não abra a porta TCP diretamente para a internet pública.
- A funcionalidade é experimental e requer compilação Android e teste em dois aparelhos. A interface não indica transmissão enquanto a autorização do sistema não estiver efetiva.

## Central de Mensagens (alpha.33)

- **Mensagens** abre tela própria com abas **SMS** e **Notificações**. Não existe mais uma única janela alta com dezenas de botões grandes.
- Mensagens agrupadas por remetente/app, com ícone **Copiar** por item e botão **Responder** apenas quando autorizado e tecnicamente disponível. Envio mantém revisão e confirmação.
- **Voltar** limpa o conteúdo da RAM. **Atualizar** solicita dados sob demanda e revalida a conexão. Após reconectar pode ser necessário solicitar atualização novamente.
- **Correção de privacidade**: SMS de autenticação em inglês, inclusive “authentication code”, são bloqueados antes da transmissão. Eventos capturados anteriormente são revalidados na transmissão, e a interface filtra novamente. Filtros de OTP não são perfeitos.
- Status de envio e metadados de MMS ficam recolhidos em Detalhes; captura de tela liberada apenas onde já era permitida.

## Configurações e recuperação do serviço (alpha.32)

A engrenagem abre uma **página de Configurações**, não uma janela modal. Escolha Notificações, SMS e telefonia, Serviço e recuperação, Diagnóstico, Segurança, Aparência ou Sobre. Cada categoria é uma página separada, com retorno às categorias pelo botão Voltar. Acessos a permissões continuam autorizados exclusivamente pelo Android no aparelho monitorado. Capturas de SMS continuam permitidas; telas de chave e QR continuam protegidas.

No monitorado, **Configurações → Serviço e recuperação** mostra retomada manual, preferência para iniciar após reiniciar e link para as opções de bateria do Android. Enquanto o serviço foreground estiver ativo e autorizado, um supervisor a cada 15 segundos verifica se o servidor TCP realmente escuta e recria o listener se falhar; em erro de bind aguarda 2 s, 5 s, 10 s, 30 s, 60 s e depois até 5 min para novas tentativas. O retorno da rede gera verificação adicional. **Configurações → Diagnóstico** mostra último estado/horário verificado e falhas consecutivas. O monitoramento só volta automaticamente após morte do processo quando o Android permitir a recriação pelo `START_STICKY`; "Forçar parada" nas configurações do Android e restrições de fabricante não podem ser anuladas.


Dois modos no mesmo APK: **Monitorado** (servidor) e **Controle** (cliente).

**Alpha.28:** diagnósticos amigáveis de conexão, detecção criptografada de versão diferente para aparelhos autenticados, seleção de apps de notificação com pesquisa e diálogos padronizados que não se sobrepõem.

## Alpha.27: identidade visual unificada

A Home, o Modo Monitorado, a escolha de conexão, os submenus de Configurações, o diagnóstico, as credenciais e o pareamento QR seguem o mesmo estilo de cartões, ícones vetoriais e espaços compactos da tela Detalhes do aparelho. Esta versão não acrescenta permissões nem amplia o acesso remoto; preserva todas as ações existentes.


> **Alpha.25 (histórico):** correção Kotlin do acesso recebido após o workflow 25; correção localizada de `Context` em Views. A reorganização da tela de Detalhes do aparelho e o futuro atalho Recuperar dados continuam planejados, não estão nesta correção.

> **Alpha.23 (histórico):** continuação da refatoração da interface e correção da tela **Acesso remoto detectado**, que agora mostra as informações completas já na primeira tela, sem o pop-up “Quem está?”.

## Alpha.24: segunda etapa da refatoração

- `MainActivity.kt` foi reduzida de 1.000 para aproximadamente 322 linhas e passou a coordenar principalmente ciclo de vida, navegação e callbacks de permissões.
- `RemoteConnectionManager.kt` agora concentra estados, cache, consulta criptografada, cancelamento de respostas obsoletas e aviso de desconexão.
- `ConnectionScreenUi.kt` e `ConnectionController.kt` separam visual do painel conectado e suas ações.
- `PairingScreenUi.kt`, `PairingQrUi.kt` e `PairingController.kt` separam os diálogos, scanner/gerador de QR e pareamento de rede.
- `MonitorCredentialsUi.kt` preserva a proteção e a exibição sob demanda de IP/chave; os diálogos de notificação foram realocados para Configurações.
- Mantidos o protocolo de acesso, a versão exata entre aparelhos, as permissões e o estilo visual. O IP selecionado por QR/manual agora é definido explicitamente antes da primeira consulta.
- Necessária compilação Android e teste físico nos dois aparelhos. Código-fonte ZIP não inclui APK.


## Novidades da alpha.26: painel remoto e detalhes técnicos

- Painel conectado redesenhado em três indicadores visuais (bateria, temperatura e rede) com ícones vetoriais e seis funções remotas em cartões.
- Detalhes do aparelho agora abre uma tela própria com seções de bateria, Wi-Fi, rede móvel/SIM, armazenamento interno (total, usado, livre), RAM e sistema Android.
- Servidor autenticado envia `deviceInfo`, `telephony` e informações de rede observadas, sem expor a chave de pareamento; recursos ainda não implementados mostram **Em breve**.
- Número de linha é opcional, desativado inicialmente e compartilhado apenas após permissão `READ_PHONE_NUMBERS` **e** aceite em **Configurações → Notificações e permissões → Número do chip**, sempre no aparelho monitorado. O número pode não existir no registro da operadora.
- O atalho **Recuperar dados** estava apenas visual nas versões anteriores à alpha.45; agora transfere arquivos e pastas de forma limitada a partir da árvore SAF autorizada.
- Atualize os dois aparelhos para alpha.27: a conexão exige versão exata igual.

## Recursos já implementados no código-fonte

- Snapshot de nível da bateria, carregamento, temperatura, tensão, corrente instantânea e média (quando o hardware/Android fornecem as propriedades).
- Captura de notificações **novas** autorizadas pelo usuário por `NotificationListenerService`. Não lê automaticamente o histórico de WhatsApp; resposta apenas por ação `RemoteInput` vigente.
- Captura opcional de notificações de aplicativos selecionados; a alpha.30 oferece leitura SMS recebidos sob demanda (READ_SMS) e respostas SMS (SEND_SMS), ambos desativados por padrão e sujeitos às restrições do Android.
- Filtro por conteúdo e bloqueio preventivo de nomes/pacotes financeiros conhecidos (não são infalíveis; o Android ainda concede acesso geral ao serviço).
- Histórico volátil limitado a 100 eventos no aparelho monitorado; mostra os últimos 30. Nada salvo em banco ou nuvem.
- Servidor TCP autenticado com chave de 256 bits e payload protegido por AES-256-GCM, timeout, nonce aleatório, janela temporal de 5 min e cache de rejeição de repetição por nonce.
- Segredos armazenados protegidos por Android Keystore no aparelho.
- Cliente com atualização ao abrir e polling de 15 segundos enquanto a tela do Controle estiver aberta.
- Pareamento por QR Code temporário no mesmo Wi-Fi/hotspot, com leitura pela câmera e confirmação do usuário antes do pareamento; conexão manual por IPv4/nome na mesma LAN ou entre redes por VPN privada (Tailscale/WireGuard). Porta **37841/TCP**.
- Notificação persistente do serviço foreground `connectedDevice`; parar por UI ou pela ação na notificação.
- Inicialização após reboot opcional (tentativa, sujeita a política do SO/fabricante).

## Como utilizar

1. Execute GitHub Actions > **Gerar APK e Release** > Run workflow. Para baixar **APK sem ZIP**, use Releases > `v0.1.0-alpha.91` > Assets > **`CelularRemoto-v0.1.0-alpha.91.apk`**. Em Actions > Artifacts, um APK de contingência (quando presente) continua vindo em ZIP por comportamento padrão do GitHub. Instale o mesmo APK nos dois aparelhos.
2. No telefone secundário: na nova **Home**, toque em **Monitorar este aparelho**; autorize a notificação de serviço e ative o monitoramento. Em Configurações, conceda apenas as permissões necessárias e selecione os aplicativos cujas notificações deseja compartilhar.
3. Na página Monitorado, toque no cartão **Gerar QR**. Escolha o IP privado acessível. O QR expira em 2 minutos e é válido para um único pareamento.
4. No telefone principal: na **Home**, toque em **Conectar novo aparelho > QR Code**, aponte a câmera na janela compacta e confirme os dados do aparelho antes do pareamento. Para um aparelho salvo, use **Aparelhos anteriores > Conectar**. A digitação manual de IP e chave continua disponível.
5. Na internet: configure **Tailscale/WireGuard** em ambos, conecte-os à mesma rede privada VPN e, em **Conectar novo aparelho > Manualmente**, informe o IP/nome acessível na VPN. Não abra a porta no roteador à internet pública.
6. Com o secundário bloqueado, consulte os dados no principal. Depois de desligar/reiniciar, o serviço pode precisar ser reiniciado dependendo do Android, ROM, economia de bateria e permissões.

**Nota:** a versão alpha usa QR e/ou IP/hostname manual, mas não faz descoberta automática NSD/mDNS de aparelhos. O segundo aparelho requer preparação inicial presencial. O app não acessa dados sem consentimento/permissões. A notificação de monitoramento permanece visível.

## Considerações importantes

- **Distribuição particular**: a alpha.31 declara `READ_SMS`, `SEND_SMS`, `RECEIVE_SMS` e `READ_PHONE_STATE`. Cada permissão precisa de autorização local do Android e da opção correspondente no app; `READ_SMS` pode continuar restrita mesmo fora da Play Store. O app não é o gerenciador padrão SMS; MMS só expõe assunto/data quando possível. Responder SMS pode gerar custos e a confirmação de entrega depende da operadora.
- **WhatsApp**: só texto de notificações capturadas e filtradas; resposta apenas se houver ação `RemoteInput` ativa, permitida e válida, com confirmação no controlador. Não acessa histórico interno, mensagens apagadas ou chats sem notificação. A entrega da resposta não pode ser confirmada pela API.
- **Dados sensíveis**: mensagens e OTP podem escapar dos filtros; não se deve tratar o bloqueio por expressões como infalível. SMS com padrões de autenticação são descartados da consulta. A chave permite acesso aos recursos expressamente autorizados; revogue a chave quando necessário.
- **Privacidade**: código não inclui nuvem nem telemetria. A VPN (se utilizada) é um serviço externo e tem políticas próprias.
- **Precisão da bateria**: a corrente `BATTERY_PROPERTY_CURRENT_NOW` é líquida e pode ter sinal ou unidade incorreta em alguns fabricantes. Não confundir com corrente no carregador. Medidas indisponíveis aparecem como indisponíveis.
- **Rede**: comunicação cifrada na aplicação mesmo em LAN, porém o endereço IP/porta ainda é observável. Não há TLS/mTLS. Sem firewall de rede, um atacante na mesma LAN pode tentar sobrecarregar o serviço; limite conexões e evite redes públicas.
- **Android 17**: como `targetSdk=36`, não usa a permissão de rede local introduzida para apps que miram SDK 37. Essa decisão exigirá revisão na atualização de target.

## Compilação

Requer Android SDK API 36, Java 17, Gradle 8.13. O repositório ainda não inclui wrapper binário. Execute no Android Studio ou terminal com Gradle instalado:

```bash
gradle :app:testDebugUnitTest :app:assembleDebug
```

APK local de teste: `app/build/outputs/apk/debug/app-debug.apk`. O workflow copia esse arquivo ou o Release assinado como **`CelularRemoto-v0.1.0-alpha.91.apk`**. Baixe diretamente na aba GitHub **Releases**. **O APK não está incluído neste ZIP de código-fonte.** Veja `docs/PAIRING_QR.md` para segurança do QR.

Opcional: configure os quatro Secrets de assinatura para gerar APK Release assinado e AAB. Ver `docs/APK_DOWNLOAD.md`. Preservar a mesma keystore entre builds é essencial para atualizações sem reinstalação.

## Privacidade e robustez (alpha.6)

- Ao desligar o monitoramento ou reativar uma nova sessão, o buffer de notificações na memória é apagado. Ao retirar um aplicativo da lista permitida, seus eventos previamente coletados são removidos. Antes de responder ao Controle, a lista atual é verificada novamente para evitar a transmissão de eventos de permissões revogadas.
- O servidor limita a 3 o número de requisições simultâneas e a 12 a fila de conexões pendentes; conexões excedentes são fechadas sem fila ilimitada. Isso reduz o risco de consumo de recursos na rede local, mas não substitui um firewall nem testes de estresse.

## Interface Controle (alpha.11)

- Visual claro por padrão, com opções em **⚙ Configurações > Aparência**: claro, escuro e seguir sistema; escolha guardada apenas neste aparelho.
- **Histórico:** a tela inicial já foi Controle (alpha.11) e Monitorado (alpha.16). **Desde a alpha.17**, a tela inicial é a **Home**, com Conectar novo aparelho, Aparelhos anteriores e Monitorar este aparelho; o ícone de casa sempre retorna à Home.
- Cabeçalho com **⌂ Home** e **⚙ Configurações**; no cartão do aparelho, botão compacto **Conectar/Reconectar/Desconectar** junto ao nome e **↻ Atualizar** colorido à direita; chips de versão e estado do monitorado; atualização manual e polling automático a cada 15 s.
- A etapa inicial **Conectar aparelho** oferece QR Code, Manualmente, aparelhos salvos com botões Conectar/Renomear/Esquecer. Somente Manualmente abre os blocos IP e chave com Colar/Limpar.
- Até cinco aparelhos recentes com renomear/esquecer. Cada chave é protegida individualmente pelo Android Keystore, com migração da credencial antiga sem novo pareamento. O histórico registra somente nome/IP e último sucesso, **não** mensagens.
- A informação "Conectado" representa a **última consulta autenticada** e não um socket persistentemente conectado; em falha, status troca para "Sem resposta" e dados anteriores permanecem rotulados.
- QR continua com Camera2 (alpha.9); teste físico e compilação completa ainda pendentes.

## Mensagens remotas (alpha.31)

No **aparelho monitorado**, ative Monitoramento; em **Configurações > Notificações**, conceda acesso ao listener e autorize os apps individualmente ou ative **Todas as notificações elegíveis**. Separadamente, habilite **Responder notificações**, **Ler caixa de SMS**, **Receber SMS em tempo real** e **Responder SMS** conforme desejar. A seleção manual de SIM exige READ_PHONE_STATE. O Android pode negar READ_SMS mesmo em distribuição particular; não é pedido o papel de app SMS padrão. Notificações sigilosas, apps financeiros conhecidos e conteúdos detectados como OTP continuam filtrados em melhor esforço.

No **controlador**, instale a mesma alpha.31, conecte-se, abra **Mensagens** e toque em **Atualizar mensagens**. São apresentados até 60 SMS recebidos e enviados recentes, notificações autorizadas e, quando disponíveis, eventos SMS ao vivo e metadados MMS. **Copiar** copia o texto para a área de transferência. **Responder** aparece quando o SMS possui número regular já presente na caixa recebida ou quando a notificação oferece `RemoteInput` válido. Para SMS, escolha o chip disponível, escreva, revise e confirme. Status *solicitado, enviado, entregue, falhou* ou *entrega não confirmada* refletem callbacks locais na RAM, não garantem leitura pelo destinatário. A ação de responder a uma notificação pode expirar.

A janela **Mensagens** não define `FLAG_SECURE`, permitindo capturas de tela. Janelas com QR e credenciais continuam protegidas. Novas mensagens não são enviadas automaticamente ao controlador: ele precisa abrir/atualizar a tela. Ao desativar o monitoramento, os buffers voláteis são limpos.

As opções estão **desativadas após atualização** e podem ser revogadas no próprio monitorado. Todos os acessos dependem da chave AES-GCM e da versão idêntica em ambos os telefones. Mensagens SMS são obtidas sem salvar banco ou arquivo; os eventos de notificação continuam num buffer volátil. O histórico de WhatsApp e mensagens criptografadas internamente não é acessível pelo Celular Remoto.

## Próximas evoluções

Descoberta de dispositivos na LAN, reconexão ao mudar IP, permissões por categoria/app, bloqueio mais robusto de OTP, persistência local opcional criptografada, atualização em segundo plano no Controlador via canal push e testes físicos em ROMs diferentes.

## Estado de validação

Vide `docs/VALIDATION.md`. A presença do código não equivale a funcionalidade comprovada em hardware: executar build CI e ensaios com aparelhos reais antes de depender deste app.


## Alterações alpha.104 · estabilidade de sessão

- Encerramento de sockets antigos, alertas limitados e descarte de resposta de chave alterada.

## Alterações alpha.4 (versionCode 4)
- QR Code local com segredo temporário (2 minutos, uso único), confirmação antes da conexão e armazenamento protegido da chave permanente.
- Leitor nativo Camera2 com ZXing Core para decodificação offline e permissões de câmera apenas no momento da leitura. Suporta os mesmos dois modos com um APK.
- Servidor confirma bind da porta antes de indicar início do serviço.
- GitHub Actions evita duplicidade de APK entre Artifact e Release bem-sucedida; cria Artifact reserva se a Release falhar ou for ignorada.
- Alerta explícito: Play Protect classificou APK anterior como nocivo; QR não altera a classificação, e precisa de investigação/contestação. Não desativar Play Protect.

## Alterações alpha.2
- Corrigido GitHub Actions: `setup-android@v4` não solicita o pacote descontinuado `tools`.
- Corrigido erro no campo de texto da interface (`setTextColor(ink)`).
- Removido recebimento direto de SMS e o receiver para preparar um possível envio à Play.
- Notificações somente de aplicativos escolhidos pelo usuário, com aviso de consentimento e bloqueio conservador de apps financeiros/conteúdo sensível.
- Seleção de apps com `<queries>` de launcher, sem permissão `QUERY_ALL_PACKAGES`.
- `versionCode` 2; nome do artifact sincronizado; documentação de Google Play em `docs/PLAY_STORE.md`.

## Alterações alpha.3 (versionCode 3)
- Arquivo APK publicado diretamente em GitHub Releases com nome `CelularRemoto-v0.1.0-alpha.3.apk`; Artifacts continuam gerando ZIP como backup.
- Assinatura Release opcional com quatro Secrets e geração do AAB para Google Play.
- Verificação de sincronização da versão e empacotamento, SHA-256 do binário, docs e instruções permanentes em `AGENTS.md`.
- Mantidas funcionalidades e filtros de segurança da alpha.2. Build completo depende do GitHub Actions.

### Correção da alpha.5

A execução GitHub Actions #4 falhou na compilação do leitor QR. A chamada ao ZXing foi corrigida para a constante `IntentIntegrator.QR_CODE`, mantendo pareamento temporário e configuração manual. Faça novo build pela ação **Gerar APK e Release**, baixando o APK diretamente em **Releases > Assets** após sucesso.

## Correções alpha.7 (build 6 e leitura de QR)

- Build #6 compilou o Kotlin principal, mas falhou em três testes `EventJournalTest` que acionavam os métodos `org.json` do `android.jar` mockado na JVM. Agora o teste usa `EventJournal.latest()` e testa a lógica de filtro sem serialização Android. `latestJson()` permanece disponível para transmissão real.
- Scanner alpha.9 agora usa **Camera2 nativo**, sem `CaptureActivity`, `IntentIntegrator` ou bibliotecas de câmera ZXing Embedded. O ZXing Core (3.5.4) fica exclusivamente na codificação/decodificação local. Se a câmera falhar, permaneça a alternativa Manual com IP e chave.
- O fechamento do leitor informado em aparelho físico ainda **não possui stacktrace Android/logcat**; não se pode confirmar que a causa original foi eliminada sem nova instalação e teste. Caso persista, capturar `adb logcat -b crash -d` ou logcat com `FATAL EXCEPTION` e `CelularRemotoQR`.
- Secrets de assinatura não acompanham o projeto nem devem ser adicionados ao GitHub. Para atualizar sem reinstalar é necessário **usar sempre o mesmo certificado** e manter o `applicationId`. APK Debug antigo e nova chave Release possuem assinaturas diferentes: talvez seja necessária uma única reinstalação ao migrar, com perda de dados locais sem backup.

## Correção alpha.8 (workflow #7)

- O log do GitHub Actions da alpha.7 falhou em `:app:compileDebugKotlin` porque `MainActivity.kt` declarava **duas vezes** `onRequestPermissionsResult` (linhas 422 e 580), com a mesma assinatura; o Kotlin relatou `Conflicting overloads`.
- Agora existe **um único callback** para os dois pedidos separados: `CAMERA` (abre o leitor QR quando concedida) e `POST_NOTIFICATIONS` (inicia o monitoramento quando concedida). Nenhuma das duas autorizações habilita a outra e a recusa continua tratada.
- O Artifact APK de contingência só é solicitado após o passo de nomear APK concluir com sucesso. Falhas de compilação não geram tentativa inútil de upload de arquivo inexistente.
- O build de GitHub Actions e o teste físico de QR da alpha.8 ainda precisam ser confirmados. A mensagem anterior sobre possível encerramento do QR não foi diagnosticada por logcat.
- Mantenha os mesmos quatro Secrets de assinatura da alpha.7. **Não gere outra keystore**; isso comprometeria a capacidade de atualizar APKs já assinados.

## alpha.9: nova abordagem para o fechamento do leitor QR

- A alpha.8 ainda abria a câmera usando a Activity do ZXing Embedded; as mudanças de manifesto e permissões não resolveram o encerramento relatado. Sem logcat do celular, a causa original específica **não foi comprovada**.
- A alpha.9 substitui integralmente essa tela por `QrScanActivity` com **Android Camera2** e `ImageReader YUV_420_888`, pré-visualização via `TextureView` e leitura ZXing Core em memória, sem Play Services/internet.
- Exibe estado e erros da câmera na própria tela (em vez de encerrá-la deliberadamente), com logs `CelularRemotoQR` sem incluir o conteúdo confidencial do QR. Permissão continua sob autorização explícita.
- A opção antiga de colar o URI temporário do QR foi retirada na alpha.18; utilize QR pela câmera ou conexão Manual com IP e chave.
- Nenhuma foto ou quadro é salvo/transmitido. O limite de leitura é de ~2,5 tentativas por segundo, economizando CPU em relação à decodificação de todos os frames.
- Necessário confirmar build Android `:app:testDebugUnitTest :app:assembleDebug`, leitura física entre dois aparelhos, orientação/câmera de fabricantes diferentes e upgrade APK assinado. Não afirmar correção comprovada sem esses testes.

## Revisão visual de diálogos e painel (alpha.14)

A tela Monitorado apresenta o estado real do serviço e IP local. Ativar, gerar QR e interromper são ações contextuais; notificações, segurança, inicialização e diagnóstico agora ficam em ⚙ Configurações.
- Menu, Configurações, Aparência, Aparelhos recentes, Diagnóstico e demais diálogos receberam cartões com subtítulo, melhor hierarquia visual e menos sensação de lista crua.
- O cartão principal do Painel Controle foi reorganizado para priorizar nome do aparelho, estado, ação principal e atualização manual, reduzindo textos duplicados.


## Fluxo de conexão e cartões (alpha.15)

- Conectar/Reconectar abre a escolha de método, em vez do formulário grande. No modo conectado, Desconectar apenas limpa a seleção, preservando os aparelhos pareados.
- Na alpha.15 havia as opções de QR Code, Manual e Inserir código QR. Na alpha.18 restaram QR e Manual lado a lado, mais cartões de aparelhos anteriores.
- Cada cartão salvo traz os botões verticais **Conectar**, **Renomear** e **Esquecer**. Esquecer requer confirmação e apaga a chave do aparelho no Android Keystore.
- QR escaneado **não conecta automaticamente**: apresenta endereço e nome local se o aparelho já estiver salvo; se for novo, apresenta «Não identificado (novo)» até ser autenticado. **Confirmar** inicia o pareamento e **Cancelar** o interrompe.
- No painel, botões menores ficam alinhados à direita: ação de conexão junto ao título e **↻ Atualizar** em azul. As consultas continuam a cada 15 s com a tela aberta.

## Tela inicial e informações manuais (alpha.16)

- O aplicativo inicia no modo Monitorado; **⌂ Home** no cabeçalho retorna a esse modo sem interromper o serviço. O modo Controle abre ao conectar por QR, manualmente ou por dispositivo salvo.
- Com o monitor ativo, as ações **Gerar QR Code**, **Mostrar minhas informações** e **Interromper monitoramento** ficam juntas.
- **Mostrar minhas informações** abre uma janela protegida por `FLAG_SECURE` com o IP privado local atual (preferindo Wi-Fi) e a chave guardada no Android Keystore, oculta até o usuário solicitar.
- **Copiar informações** copia IP e chave, marcando conteúdo como sensível em Android 13+. **Compartilhar** exige confirmação e abre o menu nativo do Android. Evite enviar chaves a terceiros; apague o clipboard após utilização.
- Abaixo das ações, **Conectados recentemente** mostra atalhos compactos de até dois aparelhos, botão **Ver todos** (escolha QR/Manual/Salvos) e **Conectar outro aparelho**.
- **Sobre o aplicativo** agora está dentro de **Configurações**. O diálogo de conexão existente da alpha.15 permanece disponível.
- A compilação Gradle/Android e teste real de interface e compartilhamento permanecem pendentes de CI e aparelho.

## Home e monitoramento separados (alpha.17)

- **Home** é a primeira página e continua acessível sem Wi-Fi. Oferece **Conectar novo aparelho** (QR/manual), os **Aparelhos anteriores** e a opção **Monitorar este aparelho**.
- Cada aparelho anterior mostra nome, IP e **Última conexão**. Essa data é derivada de `RecentDevices.lastSuccessMs`, atualizada **somente após uma consulta autenticada bem-sucedida**; se não houve sucesso, informa isso claramente. O botão **Conectar** é imediato; a **engrenagem** abre Renomear e Esquecer, com confirmação e revogação do segredo.
- Na página Monitorado, os botões **Gerar QR**, **Mostrar info** e **Interromper** aparecem empilhados à direita do resumo deste aparelho. Não se trata de uma nova página inicial.
- O ícone **Home** e a ação **Voltar** nas telas de monitoramento/controle retornam à escolha inicial sem encerrar serviços, apagar dados ou revogar credenciais.
- As telas detalhadas de controle remoto e de aparelho monitorado serão refinadas em versão posterior; a alpha.17 só reorganiza a navegação e as ações locais.


## Alpha.22: primeira etapa da refatoração da interface

- A `MainActivity.kt` foi reduzida de ~1.795 para ~1.005 linhas; agora coordena ciclo de vida, permissões, conexão, pareamento e navegação.
- A construção visual da **Home**, da tela **Monitorado**, de **Configurações** e do **Painel remoto** foi separada em `ui/HomeScreenUi.kt`, `ui/MonitorScreenUi.kt`, `ui/SettingsScreenUi.kt` e `ui/RemoteDashboardUi.kt`.
- Componentes Android Views comuns (botões, cartões, campos, chips, diálogos e espaçamento) foram extraídos para `ui/UiComponents.kt`.
- Esta etapa não introduz recursos ou permissões novos, não modifica o protocolo criptografado nem os tempos de monitoramento. Mantém atalhos compactos e o fluxo atual dos dois aparelhos.
- Como a aplicação exige versões idênticas para se conectar, **instale alpha.23 nos dois dispositivos**. Ainda falta compilação Gradle/Android em CI e teste físico de regressão visual.

## Correção da alpha.21: compilação Android

- No workflow **Gerar APK e Release 21**, o compilador Kotlin apontou incompatibilidade de tipo em `IncomingAccessActivity.kt` na configuração do `ScrollView`: o nome `background` foi resolvido como `Drawable`, mas `setBackgroundColor()` exige `Int` de cor.
- A chamada foi corrigida com uma referência explícita a `this@IncomingAccessActivity.background`. O protocolo e as telas permanecem iguais à alpha.20; a versão muda para **alpha.21 nos dois aparelhos**, pois o aplicativo exige igualdade exata da versão.
- A compilação real deve ser repetida no GitHub Actions. A validação estática e o teste sintático local não substituem um build Android completo.

## Novidades da alpha.20: aviso de acesso, compatibilidade e telas renovadas

- O telefone monitorado exibe uma **tela dedicada** sempre que recebe uma consulta autenticada enquanto o aplicativo está em primeiro plano, inclusive se as Configurações estiverem abertas. A tela informa nome e método *declarados* pelo controlador (não verificados), IP real, horário da última consulta, botão **Quem está?** e **Parar acesso** (interrompe o serviço para todos). A captura continua sendo apenas de dados autorizados; não há controle de tela.
- Após **Desconectar** no controlador, um aviso autenticado de encerramento remove o registro de acesso correspondente no monitorado, que volta automaticamente à página **Modo monitorado**, pronta para gerar um novo QR; o serviço continua ativo. Caso a rede falhe antes do aviso, o registro expira após 45 segundos.
- Com o aplicativo monitorado **em segundo plano**, o Android mostra uma notificação de acesso recente, sem forçar a abertura de uma tela sobre outros aplicativos.
- Desde a alpha.20, exigida **versão exata igual nos dois aparelhos** (para esta entrega, alpha.27): QR, pareamento e consultas autenticadas não aceitam versões anteriores. Os dois dispositivos devem ser atualizados antes de reconectar; isso **não** é uma identidade individual verificada.
- A interface Configurações foi reconstruída com grade 2x3, ícones vetoriais e cartões adaptados ao tema; a tela de aviso recebido usa layout próprio de destaque. A chave fica oculta no diálogo de informações, com proteção contra capturas.
- Painel remoto: cartões da bateria mais compactos, ações remotas maiores, redução de informações duplicadas e exibição do tipo de rede do próprio celular monitorado (sem inventar SSID ou rotas).
- Se um serviço autorizado estiver parado, o aplicativo pode tentar retomá-lo **ao voltar ao primeiro plano**, sem contrariar uma parada explícita do usuário. Restrições do Android/ROM podem impedir recuperação. Sem promessa de reinício persistente após qualquer falha.

## Mudanças da alpha.19: painel remoto compacto

- Tela do aparelho conectado reorganizada como painel compacto, com nome, status, IP, método e ações principais no topo.
- Resumo remoto em grade com ícones para bateria, temperatura, corrente atual, corrente média, tensão e quantidade de eventos.
- Grade de funções remotas com atalhos compactos: Notificações, Copiar, Detalhes, Mensagens, Ver tela e Localização.
- Notificações autorizadas e detalhes técnicos passaram a abrir em pop-ups; copiar monta um resumo sensível do aparelho atual.

## Mudanças da alpha.18: conectividade e acesso recebido

- Na janela Conectar aparelho, **QR Code | Manual** dividem a mesma linha. A opção de inserir texto de QR foi retirada do menu (a conexão manual por IP/chave continua).
- Em Aparelhos anteriores, os três controles **Conectar | Renomear | Esquecer** dividem uma única linha em vez de empilharem.
- O pop-up **Minhas informações** exibe os botões **Mostrar | Copiar | Compartilhar** lado a lado, sem mostrar a chave até o usuário solicitar.
- Em Controle, o botão **Desconectar** retorna automaticamente à Home e cancela a seleção do host.
- O aparelho Monitorado exibe um quadro de **acesso recebido** assim que recebe resposta autenticada com sucesso, com IP de origem, nome/método declarados pelo cliente, primeiro e último acesso e botão Ações. A tela Home também sinaliza acessos recentes. A interface expira indicadores após 45 segundos sem consulta autenticada; atualiza via evento e temporizador de 5 segundos.
- **Não existe sessão permanente**: cliente consulta a cada 15 segundos com tela Controle aberta. Método/nome enviados pelo cliente não provam identidade, pois os controladores autorizados compartilham a mesma chave. Encerrar serviço ou trocar chave revoga acesso globalmente; não existe controle individual nesta versão.
- Controladores antigos, que não enviam nome/método, aparecem como **não identificados**. Não inventar método ou atribuir identidade com base apenas no IP.
