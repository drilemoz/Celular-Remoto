# Validação — alpha.124 (05/11/2026)

## Escopo

- Controlar mostra 4 atalhos e 8 comandos adicionais **sempre visíveis**, mesmo sem snapshot do remoto.
- `expandedRemoteFunctions` e seus dados salvos foram removidos; não há interação adicional para abrir/ocultar comandos.
- `RemoteActionsRenderKey` diferencia host, chave autorizada, contagem de notificações, tema, fontScale e largura para que a grade seja reconstruída apenas quando necessário.
- Cache visual em memória não armazena snapshots, segredos nem dados sensíveis. Resposta autenticada e telemetria continuam atualizadas normalmente.
- Corrigida remoção redundante da árvore visual na ausência de dados de bateria.

## Verificações locais

- `python tools/check_distribution.py`: valida contagem de comandos, ausência de dobra, fluxo de atualização e invariantes existentes.
- Teste Kotlin/JVM isolado do fingerprint: igualdade sem mudanças, invalidação por aparelho, pareamento, notificações, tema, fonte e largura.
- Kotlin PSI parser, XML, YAML, integridade CRC do ZIP.

## Pendente

- Compilação Android completa pelo GitHub Actions, execução do JUnit Android, revisão em 320, 360 e 393 dp e fontes ampliadas.
- Teste físico de atualização automática das métricas, troca de remoto, perda/reconcessão de chave, novos eventos e mudança de tema.
- Verificar consumo e uso de memória com Android Profiler; menos criações de Views não comprova redução mensurável de energia.
