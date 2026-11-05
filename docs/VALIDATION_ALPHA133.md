# Validação alpha.133 — cópia dos detalhes do aparelho

## Escopo

- Interface de Detalhes permite copiar campos realmente disponíveis; placeholders desabilitados e ícones de ação/descrições acessíveis.
- Número da linha só é enviado se o usuário do monitorado habilitou compartilhar e concedeu READ_PHONE_NUMBERS; leitura legada Android 8–12 e SubscriptionManager Android 13+.
- Relatório com número solicita confirmação; prévia de clipboard redigida no Android 13+; sem alterações em WIRE_COMPAT ou endpoints.

## Testes de código-fonte

- Executar `python3 tools/check_distribution.py` e testes de `DetailCopyPolicy`.
- Analisar sintaxe Kotlin, validar recursos XML e YAML, testar CRC do ZIP e ausência de Secrets/keystore.

## Pendências

- Compilação Gradle/Android completa no GitHub Actions e instalação física.
- Testar clipboard em Android 8–16, leitura opcional de número, dual-SIM/sem número, fonte ampliada, campos longos e resposta da alpha.132.
