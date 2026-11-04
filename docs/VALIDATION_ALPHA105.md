# Validação · Monitor Remoto 0.1.0-alpha.105

Data: 20/10/2026. Origem: código-fonte alpha.104 enviado para revisão do layout.

## Alterações
- Barra inferior fora do ScrollView (`MobileNavigationUi.kt`), quatro destinos e conteúdo rolável.
- `HomeScreenUi.kt`: cartões de aparelhos pareados, última resposta real, botão de conexão e acesso ao modo monitorado.
- `ConnectionScreenUi.kt`: seleção, renomeação, status, atualizar e detalhes em linhas dedicadas.
- `RemoteDashboardUi.kt`: bateria/temperatura, rede, dados em cache rotulados, 4 atalhos e 8 funções expansíveis. Todas as 12 ações mantidas com callbacks anteriores; barra de ações somente habilitada para host com chave salva.
- `MonitorScreenUi.kt`: cartões mais legíveis e até 2 colunas para ações do monitorado.
- Atualização sequencial de Gradle, AppVersion, changelog, release notes, README e novidades.

## Verificações realizadas
- `python3 tools/check_distribution.py`: aprovado, incluindo invariante nova da alpha.105 e verificações herdadas (protocolo, criptografia, validação de versão, permissões, captura e arquivos).
- Kotlin PSI: 202 arquivos de `app/src/main/java` e `app/src/test/java` analisados sem erro sintático (não é compilação de tipos).
- XML: 53 arquivos analisados sem erros de parsing.
- ZIP: arquivo de código-fonte fechado e testado por CRC após empacotamento.

## Ainda pendente
- `:app:testDebugUnitTest`, `:app:lintDebug` e `:app:assembleDebug` no Android Gradle. Não há SDK Android/Gradle configurados neste ambiente.
- Teste em hardware Android real nos dois aparelhos (mesma versão), layouts em 320/360/393/411 dp, fontes ampliadas, orientação e teclado.
- Testes funcionais de captura autorizada, rede, dados em cache, troca de aparelho, 12 atalhos e rolagem com barra fixa.

## Escopo
Não alterou protocolo, permissões Android, AES-GCM, fluxo de consentimento, transporte de arquivos ou política existente de sobrescrita/cópia. A opção **Substituir** na cópia de arquivos não faz parte desta alteração visual.
