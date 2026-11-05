# Validação alpha.126 — 05/11/2026

## Mudanças validadas em código

- Testes de `AlertDeliveryPolicy`: bloqueios não confirmam eventos; supressão explícita e aceitação do Android confirmam; tentativas somente dentro de 24h.
- `HealthAlertMonitor`: conserva um evento pendente se o Android bloquear e descarta quando estado normaliza.
- `BatteryGoalAlerts`: armazena indicador `posted` apenas em eventos novos, preservando os antigos sem replay retroativo.
- `ControllerHealthAlerts` e `ControllerBatteryEvents`: não confirmam sequência de evento quando a notificação é bloqueada.
- `ConnectionHealthNotifier` e `UpdateController`: envio passa pela mesma verificação de bloqueio e registro de resultado; nenhuma instalação automática é acionada.
- `RemoteConnectionManager` processa metas remotas durante resposta autenticada.
- `AlertDelivery` grava somente categoria, canal, status e horário; sem nomes/IP/credenciais/conteúdo.

## Testes físicos e de integração ainda necessários

- Negar POST_NOTIFICATIONS e depois liberar; desligar todos os avisos e desligar individualmente os canais locais/remotos.
- Atingir meta, retornar a estado normal e conferir que alertas antigos não reaparecem indevidamente.
- Conferir notificação de teste e histórico local sem afirmar leitura do usuário.
- Exercitar respostas remotas, perda de Wi-Fi e intervalo do JobScheduler em dois aparelhos.
- Executar GitHub Actions: `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug`, incluindo build Release com assinatura estável.
