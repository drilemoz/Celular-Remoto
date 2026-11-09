# Validação alpha.140 — bateria remota contínua

Base: arquivo-fonte alpha.139 enviado pelo usuário.

## Mudanças verificadas

- Nova operação autenticada `batteryProbe` na porta TCP existente, retornando somente bateria, temperatura, corrente, estado e horário de amostragem. Sem SMS, localização, câmera, lista de apps ou fallback para snapshot completo.
- Acompanhamento remoto opcional e desativado por padrão em Ajustes. Requer pareamento, Monitorado ativo e app alpha.140 no outro aparelho.
- O único `MonitorService` aceita o modo `CONTROLLER_ONLY` por FGS connectedDevice, sem servidor local, ADB, GPS ou histórico de bateria. Compatibilidade do modo com a permissão `CHANGE_NETWORK_STATE` e FGS connectedDevice conferida na documentação Android.
- Retomada apenas quando a Activity está visível; consultas seriadas a cada 60 segundos após sucesso, com backoff 120/180/300 segundos após falhas. Sem wake lock adicional.
- O callback revalida aparelho selecionado e credencial. A leitura persistida é mínima, sem IP/nome/chave, fica restrita ao opt-in e escopo do pareamento. Valores são considerados recentes por 180 segundos; dados antigos são marcados como desatualizados e apagados após prazo de retenção ao serem acessados.

## Validação realizada no ambiente

- `tools/check_distribution.py`: aprovado, incluindo verificações da alpha.140 e regressões estruturais anteriores.
- Parser Kotlin PSI: 288 arquivos `.kt`, zero erros de sintaxe.
- Testes JVM de regras puras de intervalos e modos: 112 verificações passaram; testes JUnit correspondentes incluídos no projeto.
- XML: 56 válidos. YAML: 2 válidos.
- ZIP: integridade CRC verificada ao empacotar.

## Pendências e limitações

- O ambiente atual não disponibiliza Android SDK nem instalação Gradle do projeto. Há wrapper apenas em terceiro pacote legado. **Compilação Android completa, lint e suíte JUnit/Android não executados**.
- Requer validar o serviço FGS nos Androids reais, a entrega da notificação no HyperOS, reconexão após Wi-Fi/Doze, mudança de host/chave, encerramento do serviço e consumo de bateria com tela apagada.
- O Android pode interromper serviços foreground ou impedir o início em segundo plano. A atualização não é garantida em tempo real e não dispensa a autorização local do monitorado.
