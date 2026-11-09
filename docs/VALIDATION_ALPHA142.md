# Validação alpha.142 — correção do workflow 32

Base: código-fonte alpha.141. Log: `Celular-Remoto-Gerar-APK-e-Release-32-logs.zip`.

## Falha comprovada no CI

- O código principal da alpha.141 compilou, mas `:app:compileDebugUnitTestKotlin` falhou com três incompatibilidades `Double?` / `Double` nas linhas 10, 15 e 21 de `BatteryPresentationMathTest.kt`.
- A alpha.142 adicionou `requireNotNull()` nessas três verificações, preservando os valores esperados e tolerâncias dos testes.
- Nenhuma alteração nos arquivos de execução do módulo de bateria, na persistência ou na rede foi necessária.

## Validação local e limitações

- `tools/check_distribution.py`: aprovado, incluindo a nova regra dos três `requireNotNull()` e as regressões herdadas.
- Compilação isolada dos arquivos `BatteryCurrentMath.kt`, `BatteryPresentationMath.kt` e `BatteryPresentationMathTest.kt` aprovada pelo Kotlin local; os 3 métodos de teste executaram sem falhas com uma implementação mínima local das assertivas JUnit, sem substituir o runner oficial.
- XML: 56 arquivos válidos; YAML: 2 arquivos válidos. Integridade CRC do ZIP confirmada na etapa de empacotamento.
- A compilação Android/Gradle e a suíte JUnit completa da alpha.142 **não foram executadas neste ambiente**; requer novo GitHub Actions para confirmar `:app:testDebugUnitTest` e `:app:assembleDebug`.
- Após gerar APK, validar as telas de bateria nos dois aparelhos e o diagnóstico de corrente bruta.
