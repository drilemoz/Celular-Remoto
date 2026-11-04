# Verificação da alpha.108 — falhas de compilação e CI

## Evidências

- `Celular-Remoto-Gerar-APK-e-Release-1-logs.zip` e `...-2-logs.zip`: ambos em `9cc97a1004d69933ffc866817ce5572eafb30f35`, `:app:compileDebugKotlin` falhou em `BatteryScreenUi.kt:506:17` com `'val' cannot be reassigned`.
- Na alpha.107 a variável local `val type` da função externa colidia com a atribuição `type = "text/plain"` dentro de `Intent(...).apply`, fazendo o compilador tentar reatribuir o `val`.
- Corrigido por `this.type = "text/plain"`, referenciando explicitamente a propriedade do Intent, sem alterar o conteúdo compartilhado.
- `.github/workflows/android.yml` era o único arquivo de workflow, mas tinha `push`, `pull_request` e `workflow_dispatch` sem limitação de branch. Agora apenas `push` em `main` e `concurrency` com cancelamento de builds anteriores.

## Aceite necessário na CI

1. Confirmar que **um** `push` para `main` inicia **uma** execução de `Gerar APK e Release`.
2. Verificar `:app:compileDebugKotlin`, `:app:testDebugUnitTest`, `:app:assembleDebug` e publicação do APK na Release.
3. Compartilhar sessão automática de carga/descarga num dispositivo real e verificar MIME `text/plain` e texto correto.
4. Não há alteração em schema de dados, chaves de pareamento ou comandos remotos, mas ambos os aparelhos precisam executar alpha.108 para comunicação.

Os testes estáticos e Kotlin isolado locais não demonstram sucesso do build Android nem publicação no GitHub. 
