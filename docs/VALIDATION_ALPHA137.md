# Validação alpha.137 — ADB e sessões de notificações

Data: 09/11/2026. Base: ZIP alpha.136. Compatibilidade mantida (`WIRE_COMPAT=0.1.0-alpha.111`).

## Correções

- `AdbActivity.beginDiscovery`: o host de conexão é verificado e fixado como `String` não nula antes da consulta `connectCandidates`; callback também valida `discoveryHost` e geração do descobrimento.
- `AdbAutoConnectPolicy`: aceita só `_adb-tls-connect._tcp` (com possível sufixo DNS), no IP escolhido, com porta TCP válida; rejeita nomes semelhantes e duplicações.
- `MainActivity.onNewIntent`: abertura de Mensagens reaproveita o fluxo de inicialização limpo; dados anteriores não permanecem visíveis. Se não estiver ativa, a consulta começa no `onResume`.
- `ForegroundEventWatcher`: atualização da tela verifica host+escopo de chave novamente no `Handler` da UI, evitando sinalizar Mensagens de outro aparelho após troca.

## Verificações realizadas

- `python3 tools/check_distribution.py`: aprovado, incluindo regressões alpha.137 e versões anteriores.
- **13 testes JVM isolados aprovados**: política de mDNS e reconexão ADB, sinalização de eventos, filtros e cursores de alertas. Utilizado stub mínimo de `AdbDiscovery.Endpoint`; não é um teste de sistema Android.
- Kotlin PSI: **282 arquivos Kotlin, zero erros sintáticos**. Parser Kotlin 1.9; não resolve APIs Android ou tipos completos da UI.
- XML: **56 arquivos válidos**; YAML: **2 arquivos válidos**; JSON: nenhum na árvore.
- Conferir CRC do arquivo ZIP de distribuição após empacotar.

## Limites da validação

- Não é possível comprovar `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug` sem Gradle/Android SDK; confirmar pelo CI antes de distribuir.
- Testes físicos pendentes: QR/código, mDNS quando IP não existe, porta ADB rotativa, retomada do aplicativo via notificação, mudança de aparelho/chave durante evento, SMS/WhatsApp reais, Android 16, Doze e consumo de bateria.
