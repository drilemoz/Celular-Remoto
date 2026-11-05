# Validação alpha.131 — fotografia remota com preparação local única

## Invariantes inspecionados

- Iniciar preparação requer toque local em `RemotePhotoSetupActivity`; permissões `CAMERA` e `POST_NOTIFICATIONS` são verificadas antes de iniciar serviço. A espera é limitada a 10 segundos e cancelada quando Activity deixa de ficar visível.
- A foto continua dependendo de `PhotoReadyService.armed`, `MonitorService.isRunning` e pareamento autenticado AES-GCM; Camera2 é aberta por fotografia e fechada em `finally`.
- Snapshot opcional de prontidão usa `readiness.items` sem operação nova ou campo sensível. Estados `setup/session` bloqueiam nova tentativa no controlador até reconsulta, enquanto `unknown` mantém compatibilidade com servidor anterior.
- Parada ou falha de permissão informa nova preparação local e nunca dispara rearmamento remoto. Os serviços, manifesto, eventos de rede, permissões e workflow permanecem os mesmos.

## Pendências

- GitHub Actions real: `:app:compileDebugKotlin`, testes Android/JVM, lint, APK e assinatura; análise de sintaxe não substitui o build.
- Testar em dois telefones Android, incluindo Monitorado inativo → Ativar + Foto, câmera autorizada, tela apagada, várias fotos, câmera/serviço encerrados, bloqueio de notificações, permissão CAMERA negada, Wi-Fi perdido e versões antigas compatíveis.
- Confirmar estado de prontidão no controlador, indicação de falhas e ausência de polling contínuo ou JPEG salvo automaticamente no monitorado.
