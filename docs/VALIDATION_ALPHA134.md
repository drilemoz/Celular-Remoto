# Validação alpha.134 — interface, sirene, notificações e ADB

## Escopo implementado
- Controlar é o destino padrão; antiga Home não é exibida; barras do sistema tratadas por WindowInsets na Activity principal e tela ADB.
- Doze comandos em Funcionalidades. Wi-Fi técnico, Energia em grade, bloqueio somente após autorização do monitorado e confirmação explícita.
- Sirene gerada em PCM por AudioTrack, tempo máximo de 30 s, parada por comando/encerramento. Volume e DND são do Android.
- Alertas remotos opt-in no controlador baseados em snapshots cifrados. Monitorado mantém filtros existentes; SMS só disponibiliza ID/instante sob as autorizações de SMS e monitoramento. Primeira leitura estabelece linha de base, evita replay; consultas de UI ~15s e JobScheduler ≥15 min quando habilitado.
- Notificação persistente enxuta com bateria local e telemetria remota em RAM com expiração (3min). Mensagens agrupadas; Sobre com Pix/site e histórico.

## Validação de fonte
- `python3 tools/check_distribution.py`.
- Parsing Kotlin (sem compilação Android), XML, JSON quando disponíveis.
- CRC/estrutura do ZIP e varredura por credenciais privadas.

## Necessário antes de instalar/distribuir
- Compilar `:app:assembleDebug` e executar `:app:testDebugUnitTest` e `:app:lintDebug` na CI; conferir logs.
- Testar em dois aparelhos, Android 8 e Android 15/16, com navegação de 3 botões e gestos; verificar cortes, rolagem, fontes grandes, permissões SMS/listener e erros de rede.
- Verificar sirene com tela apagada, volume 0, DND, sobreposição de chamadas, encerramento do serviço e troca de conexão.
- Verificar alertas WhatsApp e SMS com opções autorizadas e negadas; impedir duplicações, conteúdo sensível, falso aviso imediato e vazamentos em tela bloqueada.
- Testar que ADB não executa reboot sem pareamento/confirmação e que porta pode mudar após reboot.
