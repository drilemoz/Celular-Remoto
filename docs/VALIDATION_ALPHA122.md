# Validação da alpha.122 — correção do workflow 15

## Causa comprovada

`Celular-Remoto-Gerar-APK-e-Release-15-logs.zip`: `:app:compileDebugKotlin` falhou em `BatteryCompactUi.kt:114` por `Unresolved reference 'minHeight'`. A variável `row` é um `android.widget.LinearLayout`, que expõe a propriedade `minimumHeight`. O nome `minHeight` se aplica a outros widgets (por exemplo, `TextView`), mas não deve ser usado aqui.

## Correção

Substituição localizada de `minHeight = dp(57)` por `minimumHeight = dp(57)` em `batterySessionListRow`. Nenhuma outra mudança funcional além da sincronização da versão. Novo assert em `tools/check_distribution.py` impede a regressão nesse componente.

## Verificações locais

- `python tools/check_distribution.py` (invariantes de segurança, protocolo, versão e regressões)
- Varredura da sintaxe Kotlin e dos recursos XML/YAML, quando disponível
- Integridade CRC do ZIP e ausência de chaves/Secrets

## Verificações pendentes no GitHub Actions e dispositivos

- `:app:compileDebugKotlin`, `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:lintDebug`, `:app:assembleRelease` com assinatura estável.
- Histórico da Bateria, listas, alternância local/remoto, temas, fonte ampliada e controles remotos em aparelhos físicos.

**Não presumir build Android aprovado com base em validações locais.**
