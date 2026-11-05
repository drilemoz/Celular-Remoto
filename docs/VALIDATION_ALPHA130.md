# Validação alpha.130 — coleta local contínua sem rede

## Regressões verificáveis

- Preferência `local_battery_monitoring_enabled` começa falsa, independente da autorização do Monitorado.
- Política de três modos: OFF, BATTERY_ONLY e REMOTE. O modo remoto tem precedência quando ambos são autorizados, sem executor ou agenda extra.
- FGS `specialUse` somente no modo local explícito, sem chamar `ensureServerReady()`, `MonitorServer.start()`, nem iniciar câmera, tela, localização, controles físicos ou WakeLocks.
- Trocas de modo preservam histórico e não criam segundo canal/ID de notificação. `Parar bateria` não desliga Monitorado; interromper Monitorado não desativa coleta local autorizada.
- Verificar que boot só tenta retomar acompanhamento se auto-inicialização já estiver autorizada; `START_STICKY` sujeito às restrições do Android.

## Pendências obrigatórias

- GitHub Actions: `:app:compileDebugKotlin`, `:app:testDebugUnitTest`, lint, Debug e Release, assinatura real.
- Em dois Androids: iniciar apenas coleta local; confirmar nenhuma porta TCP 37842 aberta e uma notificação. Habilitar Monitorado, verificar comunicação autorizada e nenhum histórico duplicado. Desativar Monitorado, verificar bateria contínua; desativar bateria, verificar encerramento da notificação.
- Verificar Doze, tela apagada, mudança de rede, notificações negadas e force-stop; sem supor continuidade irrestrita.
- Uso de specialUse pode exigir declaração/revisão adicional no Google Play. Não alegar publicação aprovada apenas pela presença no manifesto.
