# Celular Remoto — 0.1.0-alpha.111

**Atualizações online integradas:** o app consulta versões publicadas no GitHub, baixa em segundo plano pelo DownloadManager (quando permitido pela rede e bateria) e avisa quando o APK estiver verificado. O usuário autoriza localmente a instalação oficial no Android. A instalação nunca é remota ou silenciosa.

A tela **Configurações → Atualizações** inclui busca manual, download manual, horários aproximados de 6/12/24 h, preferências de rede não tarifada e proteção de bateria. APK só é elegível se a Release possuir o manifesto `celular-remoto-update.json` gerado com a assinatura estável. Verificação de hash SHA-256, pacote, versão e certificado. O workflow de GitHub Actions continua único.

**Limitações:** conexão à internet para verificar/baixar; Wi-Fi de transferência entre aparelhos ainda não implementado. Instalações assinadas com certificados diferentes não atualizam entre si. Necessário testar GitHub Actions completo, DownloadManager e instalador nos aparelhos.

---

# Celular Remoto — 0.1.0-alpha.110

Central de permissões com primeira instalação guiada, solicitações Android e atalhos para acessos especiais, banner de pendências e SMS separado. Não altera o serviço remoto nem permite controle sem autorização local.

**Correção do build 3:** a etapa `:app:testDebugUnitTest` executou 183 testes e falhou em um teste de QR por usar a antiga porta 37841. A versão 109 testa a adulteração da porta real declarada no protocolo (37842), exigindo que os casos negativos sejam de fato diferentes do QR válido. O esquema de QR foi atualizado de `monitorremoto://` para `celularremoto://`; gere novo QR e use esta versão nos dois aparelhos.

Mantidos o workflow único, versão estrita nos dois aparelhos, autenticação AES-GCM, coleta de bateria e controle remoto. Build Gradle completo e uso em dois dispositivos ainda precisam ser confirmados.

---

# Celular Remoto — 0.1.0-alpha.108

**Correção de compilação:** resolve o erro `BatteryScreenUi.kt:506` no compartilhamento de sessão automática de bateria (colisão entre variável `type` e `Intent.type`).

**Um único workflow:** somente `push` na branch `main` aciona a compilação e publicação. Eventos `pull_request` e `workflow_dispatch` deixaram de disparar o mesmo job; uma nova atualização cancela uma execução anterior ainda em curso.

**Sem mudança de dados ou protocolo:** mantém a identidade `br.dev.celularremoto`, porta 37842, autenticação AES-GCM e compatibilidade estrita de versões. Para comunicação, os dois celulares devem usar alpha.108.

**Validação:** os dois logs da alpha.107 apontam a mesma falha no mesmo commit. A alpha.108 precisa ser compilada no GitHub Actions; um teste estrutural local não substitui o APK real.

---

# Celular Remoto — 0.1.0-alpha.107

**Nova identidade:** `br.dev.celularremoto`, APK `CelularRemoto-v0.1.0-alpha.107.apk` gerado pela CI. Esta versão é instalada separadamente do antigo Monitor Remoto; faça o pareamento novamente em ambos os telefones. Não exclua a instalação antiga antes de resgatar relatórios relevantes, pois dados internos e chaves de pareamento não são migrados.

A aba Bateria agora inclui sessões automáticas de carga/descarga, previsões baseadas em percentuais observados, histórico, metas de bateria e alertas de temperatura configuráveis local ou remotamente. Os quatro testes Labs continuam disponíveis e a conclusão de carga em 100% foi fortalecida. Os resultados são medidos pelo próprio aparelho e consultados na rede local autenticada; sem exigir Pulso Bateria. O painel solar só é medido de verdade com instrumentação externa.

**Importante:** a comunicação exige Celular Remoto alpha.107 nos dois celulares. O serviço usa porta TCP 37842; não reutiliza dados e chaves internas do app anterior. Alertas remotos em segundo plano dependem da configuração e de consultas do Android; não há promessa de tempo real após desconexão. Um build completo pelo GitHub Actions e testes físicos ainda precisam ser confirmados.

---

# Celular Remoto v0.1.0-alpha.106 · Bateria e testes remotos

A barra inferior passa a conter cinco destinos: Início, Controlar, Monitorado, Bateria e Ajustes. Bateria alterna entre o aparelho atual e um monitorado pareado.

Com o serviço Monitorado ativo no aparelho de origem, agora é possível iniciar e finalizar testes Solar, Elétrico, Consumo e Carga Automática. As sessões são armazenadas localmente no dispositivo medido; o controlador recebe relatórios e medições mediante pedidos autenticados pela chave de pareamento, não pela instalação do Pulso Bateria. Há gráficos, temperatura, corrente média válida, duração e períodos desconhecidos quando faltam amostras.

Os dados do Android representam **corrente e potência líquidas da bateria**, não a produção elétrica real do painel solar. O campo W do painel é nominal/declarado pelo usuário. Em nenhum caso o aplicativo inventa medições durante interrupções, e os relatórios permanecem no armazenamento privado do monitorado até serem explicitamente compartilhados.

Requer versão alpha.106 instalada em **ambos** os aparelhos. Não foi confirmado APK compilado em hardware nem validação do servidor em rede física neste ambiente.

---

# Celular Remoto v0.1.0-alpha.105 · Interface para smartphones

Nesta versão, a interface Início e Controlar foi redesenhada com foco em celulares. Há uma barra inferior fixa com Início, Controlar, Monitorado e Ajustes; os dados do aparelho e os comandos são organizados em cartões maiores, com quatro acessos rápidos e oito funções expansíveis.

Aparelhos salvos não são apresentados como permanentemente conectados. Permissões locais, autenticação e confirmações dos módulos existentes não foram alteradas.

**Instale a mesma versão nos dois aparelhos.** Validação de compilação Android na CI e teste real de UI continuam necessários antes de considerar esta versão aprovada para uso diário.

# Celular Remoto v0.1.0-alpha.104 · Estabilidade de conexão

- Recuperação do listener fecha sockets em execução e enfileirados.
- Janela de notificações de acesso restringe o estado em memória aos acessos autenticados ainda recentes.
- Respostas recebidas de chave removida ou alterada não atualizam o controlador.
- Regressões locais adicionadas; build Android e testes reais em dois celulares pendentes.

# Celular Remoto v0.1.0-alpha.103 · Desempenho e acabamento

- Modo Monitorado mantém rolagem e cartões estáveis nas consultas em que nada mudou, reduzindo atualizações desnecessárias de layout.
- Corrigida a dupla programação do ciclo de atualização de estado de 5 s e agrupados eventos autenticados consecutivos sem perder a expiração dos acessos.
- Busca de Configurações responde com índice local e atualizações agrupadas (120 ms), sem modificar autorizações; limpar busca responde imediatamente.
- Botões de conexão com áreas de toque maiores e sem alteração de comandos.
- Mesma versão exigida nos dois aparelhos. Testes estáticos locais não substituem compilação e medição reais no Android.

# Celular Remoto v0.1.0-alpha.102 · Configurações e diálogos

- Índice de configurações compacto, sem título duplicado, com ícones, contagem de resultados e busca que pode ser limpa com um toque.
- Estado de permissões Android de câmera, localização e notificações acessível no topo; não presume serviços ativos ou conteúdo compartilhado.
- Escolha de perfil GPS modernizada e claramente associada a bateria/autonomia; não aplica nova seleção até o toque.
- Janelas de confirmação e seleção responsivas, preservando controles de privacidade e autenticação.
- Instale a mesma versão nos dois aparelhos após conferir o build Android no GitHub Actions. Compilação completa e aparência em aparelho físico ainda pendentes.

# Celular Remoto v0.1.0-alpha.101 · Tela inicial e Modo Monitorado

- Novo painel inicial com estado de monitoramento local e atalhos legíveis.
- Aparelhos salvos exibem data de acesso anterior sem sugerir conexão atual.
- No monitorado, tela, câmera e foto têm cartões com ícone, estado e ação separada; permissões aparecem em resumo claro.
- Indicador de acesso recente distingue autenticação ocorrida de conexão permanente.
- Mantidos MediaProjection, autorização local, parada visível, ADB/TLS, AES-GCM, permissões e confirmações existentes.
- Verificações estáticas locais; build Android completo e inspeção física pendentes.

# Celular Remoto v0.1.0-alpha.100 · Correção do build 87

- `MonitorScreenSkin.kt` corrigido em três usos de `foreground`: `TextView`, `Button` e `ImageView` agora usam explicitamente a cor `Int` do kit visual, em vez do `Drawable` herdado de `View`.
- Resolvida a causa dos erros mostrados em `:app:compileDebugKotlin` no workflow "Gerar APK e Release 87".
- Mantidas as mesmas cores e o design; comportamento remoto, AES-GCM, ADB, permissões e confirmações inalterados.
- Adicionada regressão ao verificador. Build Android real ainda depende de GitHub Actions.

# Celular Remoto v0.1.0-alpha.99 · Correção do build 86

- Corrigido o atributo inválido `android:windowDimAmount` nos temas de diálogo, responsável pela falha AAPT2 em `:app:processDebugResources` da alpha.98.
- Os estilos claro e escuro continuam com a mesma cor de fundo, arredondamento e intensidade de escurecimento usando `android:backgroundDimAmount`.
- Adicionada verificação automática de regressão para o recurso XML.
- Funcionalidades, criptografia, permissões e interface das versões anteriores preservadas.
- Validação Gradle Android e teste em dispositivo permanecem pendentes até nova execução do workflow.

# Celular Remoto v0.1.0-alpha.98 · Conexão, pareamento, detalhes e ADB

- Tela de controle com estado de conexão compacto, latência e hora da última consulta já obtidas de respostas reais.
- Pareamento via QR, chave manual e aparelhos salvos com escolhas mais claras; sem alteração de segurança nem de credenciais.
- Detalhes do aparelho e página ADB adotam `MonitorScreenSkin`, cabeçalhos com ícones e cartões mais compactos.
- Fluxos críticos de ADB permanecem protegidos por conexão pareada e confirmação.
- Testes locais não substituem compilação Gradle nem ensaios reais no Android.

# Celular Remoto v0.1.0-alpha.97 · Tela remota, mensagens e notificações

- Ver tela: cabeçalho padronizado, controles de captura mais compactos e painel de sessão com informações de status e canal.
- Mensagens e notificações: apresentação renovada, grupos com identificação melhor, cartões separados e avisos discretos sobre leitura protegida e temporária.
- Edição de resposta: diálogo central com o kit visual, validação de campos, filtro de conteúdo sensível e confirmação explícita antes de envio.
- Preservados MediaProjection, permissões SMS/notificações, AES-GCM, funcionamento local e todos os botões de interrupção.
- Sintaxe Kotlin e verificações locais não substituem Gradle/Android nem testes em aparelhos reais.

# Celular Remoto v0.1.0-alpha.96 · Modernização de captura e localização

- Sistema de aparência compartilhado para telas especializadas, com cabeçalhos, cartões e botões coerentes com o painel principal.
- Fotos: apresentação e estado de captura mais claros, preservando a foto única por solicitação, autorização local de câmera e o salvamento opcional.
- Câmera ao vivo e abertura no monitorado: cores vinculadas ao tema, prévia mais organizada e ações compactas; sem alteração no protocolo JPEG e sem gravação automática.
- Mapa: cabeçalho e botões seguem os mesmos componentes do restante do aplicativo, mantendo precisão, idade da leitura e consultas autenticadas.
- Nenhuma permissão, comando remoto, par de chaves, autorização ou confirmação sensível foi ampliada.
- Verificações locais não equivalem à compilação Gradle nem aos testes físicos no Android.

# Celular Remoto v0.1.0-alpha.95 · Menus e Configurações modernizados

- Novo componente reutilizável de menu inferior com ícones, títulos, subtítulos, contraste claro/escuro e altura adaptada.
- Ações de arquivos, seleção de ordenação e gerenciamento de downloads interrompidos migrados para o novo menu.
- Menu de aplicativos modernizado, identificando ações que dependem de ADB.
- Página Configurações com cabeçalho visual, grupos compactos e ícones destacados.
- Confirmações de exclusão, reinicialização e parada continuam obrigatórias, sem enviar comandos automaticamente.
- Build Android e inspeção de aparência em dispositivo pendentes.

# Celular Remoto v0.1.0-alpha.94 · Identidade visual e energia

- Componentes visuais reutilizáveis (cabeçalhos, ações, chips, avisos, botões e cartões compactos).
- Energia e manutenção redesenhada: diálogo central, aparelho identificado, bloqueio, acender tela e reiniciar Android, com destaque para requisito ADB e aviso de segurança.
- Novos temas dos diálogos nativos no modo claro e escuro. Notificações e confirmações da MainActivity reformuladas.
- Mantidas as autorizações de cada operação, confirmações e regra de nunca desligar completamente o aparelho remotamente.
- Próximas telas candidatas ao novo kit: arquivos, aplicativos, câmera, mapas e configurações. Este release não transforma automaticamente todos os diálogos dessas telas em painéis personalizados.
- Build Gradle Android e visualização no dispositivo pendentes.

# Celular Remoto v0.1.0-alpha.93 · Localização dinâmica

- Consulta autenticada **somente de localização** (`locationSnapshot`), reduzindo trabalho no servidor sem contornar autorização nem forçar leitura GNSS nova.
- Novo painel de localização com indicadores de conectividade e estado do GPS que não confundem coordenada antiga com posição atual.
- Mostra última resposta, idade do fix, precisão e tempo da requisição; modo **Acompanhar** faz pedidos a cada 3 segundos somente com a tela aberta.
- A posição muda apenas quando o Android monitorado dispõe de um novo fix; consulta de 3 s não garante GPS a cada 3 s.
- Compilação Gradle e teste em dois aparelhos pendentes.

# Celular Remoto v0.1.0-alpha.91 · Termometria detalhada

- Janela central ao tocar Bateria ou Temperatura, com leituras reais e fontes identificadas.
- Android: bateria, estado térmico, margem térmica e ambiente se houver sensor.
- ADB opcional para host previamente pareado: enumera as zonas expostas via `dumpsys thermalservice`, sem comando remoto arbitrário, root ou acesso a dados pessoais.
- Leituras indisponíveis não são inferidas; validação Android completa ainda pendente.

# Celular Remoto v0.1.0-alpha.90 · Correção de compilação do menu de arquivos

## Alterações
- Corrige o erro do GitHub Actions `Gerar APK e Release 82`: `RemoteFilesActivity.kt:604-606`, `scroll` indefinido e tentativa de chamar métodos de `AlertDialog?` sem garantir não nulidade.
- O menu inferior passa a possuir uma `ScrollView` local com seu conteúdo; a instância `dialog` de tipo não nulo é usada para mostrar e configurar a janela.
- Mantidos os recursos da alpha.89: Downloads/Celular Remoto, gerenciamento de arquivos próprios, grade de três colunas, novas Configurações, AES-GCM e SHA-256.

## Validação
- Verificação sintática/estrutural local não substitui `:app:compileDebugKotlin` real. Reexecutar a CI e instalar a alpha.90 em ambos os aparelhos.

# Celular Remoto v0.1.0-alpha.89 · Menus modernos e Downloads fixos

## Novidades
- **Downloads/Celular Remoto** criado automaticamente no Android 10+; essa é a única pasta de transferência padrão nos dois aparelhos, sem seletor de pasta.
- Listagem remota restrita a arquivos próprios que o app criou no MediaStore, filtrados por proprietário do pacote e caminho relativo. Não há acesso oculto aos downloads de outros aplicativos.
- Operações autenticadas disponíveis sobre arquivos próprios: download, upload, renomeação, movimentação, cópia, lixeira e exclusão confirmada; diretórios vazios podem ser criados e removidos.
- Modernização de diálogos temáticos em várias telas e menu inferior para ações de arquivo. Mantida grade de 3 colunas.
- Validação e publicação SHA-256, retomada privada de downloads e versão exata nos dois aparelhos preservadas.

## Limitações
- Transferência na modalidade fixa requer Android 10+. Os arquivos que eram acessíveis via árvore SAF na alpha.88 NÃO migram automaticamente. É necessário enviá-los ao Celular Remoto como arquivos específicos.
- Arquivos pessoais de outros apps em Downloads não aparecem na pasta remota sem autorização específica; nenhum bypass de privacidade/segurança do Android.
- Compilação Android completa e testes reais não disponíveis nesta entrega.

# Celular Remoto v0.1.0-alpha.89 · Painel em 3 colunas e Configurações pesquisáveis

## Novidades
- Painel compacto com **12 funções em 3 colunas**, mantendo toque e acessibilidade. Detalhes na barra do aparelho.
- Configurações agrupadas em seis áreas com pesquisa local instantânea (aceita acentos) e navegação para as opções atuais.
- Avisos e confirmações centrais em diálogos temáticos compactos. Permissões e consentimentos não foram relaxados.
- Tempo de requisição autenticada em milissegundos junto à última resposta. O relógio da última observação remota permanece distinto.

## Limites
- O intervalo de atualização ainda é de 15 s; RTT mede a duração total da chamada, não velocidade de rede pura.
- Telas especializadas e alguns menus herdados ainda usam diálogos Android e são candidatos às próximas versões.
- Android Gradle completo e testes visuais reais pendentes. Instalar a mesma versão nos dois celulares.

# Celular Remoto v0.1.0-alpha.87 · Mapa visível e modo recuperação de GPS

## Novidades
- Mapa interno agora renderiza blocos OSM da região de referência mesmo sem resposta GPS, com aviso de **nenhuma posição remota** e sem marcador inventado. Mantém consentimento de blocos online por controlador/aparelho e atribuição.
- Área de status sobe acima do mapa; cartão e textos mais compactos. Consultas automáticas ficam ligadas enquanto a tela estiver aberta e podem ser desligadas.
- No aparelho monitorado, opção **Ativar recuperação automática** com configuração local única. A sessão de localização permanece em primeiro plano com notificação e ação Parar, mesmo após apagar a tela.
- Tentativa de restaurar GPS por boot/reinício do MonitorService SOMENTE quando modo recuperação e permissões Android, inclusive acesso de localização em segundo plano, foram previamente autorizados. O usuário pode abrir Configurações do Android para conceder "Permitir o tempo todo". Serviço pode não ser restaurado conforme Android/fabricante.
- Consulta autenticada não solicita outra autorização do monitorado. Falha de rede pode manter nesta tela um marcador anterior somente com aviso **não ao vivo**.

## Limitações
- O GPS do Android não é ligado remotamente; instalar e autorizar ainda exige acesso físico uma única vez. Nenhum comando escondido ativa GPS.
- O app não tem serviço público de internet: fora da LAN/VPN não poderá localizar o celular desaparecido. O modo não funciona após desinstalação, force-stop, perda de permissão ou sem energia/conectividade.
- Permissão ACCESS_BACKGROUND_LOCATION requer declaração adicional e pode limitar publicação Google Play. Testes Gradle/Android/boot nos Redmi estão pendentes.
- Para transmissão de fotos, as regras separadas de câmera não mudaram.
