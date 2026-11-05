# Validação planejada da alpha.120 — Bateria

## Verificações locais
- Confirmar versionCode/versionName/AppVersion 120 e um workflow único.
- Executar `python3 tools/check_distribution.py`, parser PSI Kotlin, validação XML e YAML, CRC/ZIP.
- Confirmar que `BatteryCompactUi.kt` só altera apresentação e que `BatteryLabStore` e `RemoteClient` não foram modificados.
- Examinar telas local/remoto com e sem dados, erros de rede e fonte de leitura não identificada.

## Validação de dispositivo pendente
- Build real Android CI: `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:lintDebug`.
- Testar 320/360/393 dp, escala de fonte 1.0/1.3, tema claro/escuro, seleção remoto/local e rolagem.
- Testar sessões, alertas, comparação, gráficos e leitura com lacunas em dois celulares pareados.
- Confirmar que a UI não representa corrente da bateria como corrente do painel solar.
