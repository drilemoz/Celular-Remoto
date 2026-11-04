# Validação · Monitor Remoto 0.1.0-alpha.104

Data: 17/10/2026 · versionCode 104

## Correções de estabilização

1. `MonitorServer`: rastreia sockets aceitos (incluindo enfileirados) em coleção concorrente; `stop()` fecha essas conexões antes de interromper o executor. Isso reduz sessões residuais após a perda/recuperação da escuta, mas não substitui testes de TCP reais.
2. `AccessAlertWindow`: mantém somente sessões ativas do período definido por `AuthenticatedAccess.recent()`. Notificação apenas para sessão nova, sem crescimento cumulativo da coleção ao longo de muitas horas.
3. `RemoteConnectionManager`: verifica a chave atual pareada do host antes de aceitar telemetria remota e antes de entregar respostas de SMS e controles. Uma solicitação cuja tela saiu do primeiro plano não mantém estado `loading` indefinidamente.

## Verificações locais

- `python3 tools/check_distribution.py`: aprovado, inclusive regras específicas para alpha.104.
- Parser Kotlin PSI: 201 arquivos, 0 erros de sintaxe.
- Compilação Kotlin/JVM isolada de `AccessAlertWindow`: aprovada.
- Teste executável JVM: sessões novas/duplicadas, duas sessões simultâneas, remoção, dez mil rotações e reinício; todas as verificações passaram.
- `RemoteConnectionManager.kt` compilado com stubs locais de Android e demais dependências para verificação de tipos Kotlin; aprovado. Esses stubs NÃO verificam comportamento real da API Android ou integração em rede.
- Recursos XML: 55 arquivos parseados sem erros.
- Integridade CRC do ZIP: a validar no empacotamento.

## Limites

- **Pendente:** compilação Android/Gradle (`:app:testDebugUnitTest :app:assembleDebug`) no GitHub Actions.
- **Pendente:** reconexão durante cópia de arquivo, perda de rede, reinício do serviço, reemparelhamento e consumo de memória em dois aparelhos reais.
- Esta versão não adiciona permissões, comandos ADB, transmissão oculta ou novas capacidades remotas.
