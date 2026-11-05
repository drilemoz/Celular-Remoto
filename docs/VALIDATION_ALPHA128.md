# Validação alpha.128 — 05/11/2026

## Escopo

- `ConnectionPowerPolicy`: expiração e janela de observação de lacunas.
- `ConnectionPowerController`: controla WakeLock CPU e Wi-Fi Lock apenas enquanto MonitorService está ativo; ambos default OFF e liberados no encerramento.
- Configurações locais: status do PowerManager/ConnectivityManager/ActivityManager; telas do Android abertas sob toque.
- Sem alterações no protocolo de pareamento, notificações, câmera, SMS, assinatura e workflows.

## Limitações

- A análise sintática e os testes JVM isolados não validam o build Android; requer CI.
- WakeLock comum não garante rede no Doze; não testado no Redmi físico.
- A contagem de lacunas não determina se houve suspensão por Doze, restrição OEM ou sobrecarga.
