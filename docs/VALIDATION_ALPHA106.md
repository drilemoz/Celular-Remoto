# Validação · Monitor Remoto 0.1.0-alpha.106

## Escopo

Nova aba Bateria, telemetria e sessões locais/remotas, sem segundo serviço e sem novas permissões.

## Verificações

- `python3 tools/check_distribution.py`: invariantes herdadas de versão, autenticação, navegação, permissões, screenshots, 12 ações e os novos contratos alpha.106.
- `kotlinc` com testes matemáticos isolados e cobertura de lacunas, sinal, estados e relógio.
- Compilação `:app:assembleDebug`/`testDebugUnitTest` via GitHub Actions ainda obrigatória; sem Android SDK/Gradle local não há confirmação de APK instalável.
- Testes em dois celulares ainda pendentes: corrente OEM, gráfico em tela pequena, modo com tela apagada, latência Wi-Fi, troca de host, revogação de chave, reinício de processo/serviço, relatório remoto e conservação de histórico.
