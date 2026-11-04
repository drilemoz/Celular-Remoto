# Validação alpha.116 — correção do build 9 (04/11/2026)

## Diagnóstico comprovado

A execução `Celular-Remoto-Gerar-APK-e-Release-9-logs.zip` passou por compilação Kotlin principal e de testes, e falhou na tarefa `:app:testDebugUnitTest`: **210 testes, 1 falha** em `AppVersionTest.apkVersionAndWireVersionAreIndependent` (`AppVersionTest.kt:8`). O teste esperava o literal antigo `0.1.0-alpha.112`, enquanto `AppVersion.NAME` era `0.1.0-alpha.115`.

## Alterações

- `AppVersionTest.kt`: usa `br.dev.celularremoto.BuildConfig` gerado pelo plugin Android para conferir `VERSION_NAME` e que o sufixo numérico corresponde ao `VERSION_CODE`.
- `AppVersion.NAME` e Gradle para alpha.116/116; `WIRE_COMPAT` alpha.111 não foi alterado.
- Verificador da distribuição impede retorno à asserção fixa obsoleta e valida sincronização `versionName`/`versionCode` sem valor estático do release.
- README, CHANGELOG, notas de Release, arquitetura, Novidades, AGENTS e documentação de validação sincronizados.

## Limites

Validações estáticas e execução isolada dos testes não equivalem a build Android real. Necessário executar `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug` no GitHub Actions e testar a comunicação entre os dispositivos.
