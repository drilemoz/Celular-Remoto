# Validação alpha.141 — bateria mais clara e corrente coerente

Base: arquivo-fonte alpha.140 enviado pelo usuário.

## Mudanças verificadas

- Confirmada a separação visual entre leitura atual, ferramentas/alertas e resumos na tela Bateria, sem remoção dos dados já existentes.
- Acompanhamento remoto opcional e desativado por padrão em Ajustes. Requer pareamento, Monitorado ativo e app alpha.141 no outro aparelho.
- O único `MonitorService` aceita o modo `CONTROLLER_ONLY` por FGS connectedDevice, sem servidor local, ADB, GPS ou histórico de bateria. Compatibilidade do modo com a permissão `CHANGE_NETWORK_STATE` e FGS connectedDevice conferida na documentação Android.
- Retomada apenas quando a Activity está visível; consultas seriadas a cada 60 segundos após sucesso, com backoff 120/180/300 segundos após falhas. Sem wake lock adicional.
- Confirmada a preservação das grandezas: ritmos e previsões continuam baseados em percentual por hora; corrente/potência permanecem baseadas nas propriedades BatteryManager ou CHARGE_COUNTER.
- Confirmada a normalização apenas visual do sinal em descarga. O valor bruto permanece disponível em Diagnóstico elétrico e não é regravado como dado diferente.

## Validação realizada no ambiente

- `tools/check_distribution.py`: aprovado, incluindo verificações da alpha.141 e regressões estruturais anteriores.
- Parser Kotlin PSI: 288 arquivos `.kt`, zero erros de sintaxe.
- Testes JVM de regras puras de intervalos e modos: 112 verificações passaram; testes JUnit correspondentes incluídos no projeto.
- XML: 56 válidos. YAML: 2 válidos.
- ZIP: integridade CRC verificada ao empacotar.

## Pendências e limitações

- O ambiente atual não disponibiliza Android SDK nem instalação Gradle do projeto. Há wrapper apenas em terceiro pacote legado. **Compilação Android completa, lint e suíte JUnit/Android não executados**.
- Requer validar o serviço FGS nos Androids reais, a entrega da notificação no HyperOS, reconexão após Wi-Fi/Doze, mudança de host/chave, encerramento do serviço e consumo de bateria com tela apagada.
- O Android pode interromper serviços foreground ou impedir o início em segundo plano. A atualização não é garantida em tempo real e não dispensa a autorização local do monitorado.
