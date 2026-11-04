# Validação alpha.115 — correção do build 8 (04/11/2026)

## Evidência do erro

Log `Celular-Remoto-Gerar-APK-e-Release-8-logs.zip`: `:app:compileDebugKotlin` passou e `:app:compileDebugUnitTestKotlin` falhou em `BatteryHistoryMathTest.kt:14:28` porque o retorno `Double?` de `BatteryHistoryMath.rate()` era usado diretamente em `assertEquals` numérico com `Double` obrigatório.

## Correção

- Um teste foi alterado para `assertEquals(20.0, requireNotNull(BatteryHistoryMath.rate(sample())), 0.001)`.
- `requireNotNull` falha expressamente se o resultado da operação for ausente, preservando a semântica do teste.
- Incluída verificação estática contra retorno à forma que não compila.
- `versionName`/`versionCode` atualizados para alpha.115/115 e documentação sincronizada.
- Nenhuma alteração em lógica de bateria, serviço, comunicação, permissões, atualizador, Secrets ou GitHub Actions.

## Validações locais

- `python3 tools/check_distribution.py`: aprovado (contratos, permissões, identidade, workflow único e regressão `Double?`).
- Compilação Kotlin/JVM isolada do arquivo real `BatteryHistoryMath.kt` e seu teste `BatteryHistoryMathTest.kt`: aprovada com stub de API JUnit equivalente para as asserções utilizadas (compilador local Kotlin 1.9).
- Execução isolada via reflexão dos 12 testes em `BatteryHistoryMathTest.kt`: 12/12 aprovados. Os stubs ficam **fora do ZIP** e não substituem os testes oficiais Gradle/JUnit.
- Parser Kotlin PSI: 229 arquivos (app/src), zero erros sintáticos.
- XML: 54 arquivos válidos (`app/src` e `adb-core/src`). YAML: único workflow carregado.
- ZIP: integridade e CRC verificados após empacotamento.
- O build CI original chegou a `:app:compileDebugKotlin` e falhou em `:app:compileDebugUnitTestKotlin`; não se pode considerar a alpha.115 compilada como aplicativo Android sem novo workflow.

## Pendências

- GitHub Actions: `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug`.
- Testes físicos de UI/pareamento/atualização/histórico em dois celulares.
