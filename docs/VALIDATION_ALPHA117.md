# Validação alpha.117 — conexão

- Base: ZIP alpha.116, código-fonte original preservado até esta versão.
- Implementação: `heartbeat` AES-GCM no servidor e cliente; fallback para `snapshot` em versões antigas; não transfere dados por sonda moderna.
- Métricas: amostras autenticadas, sucessos/falhas, latência, sequências, intervalos desconhecidos; retenção local limitada por host (2048 amostras / 7 dias, o que vier primeiro).
- Reconexão: no máximo 4 endpoints já salvos, apenas se a credencial local for idêntica; nunca escanear IPs.
- Android: JobScheduler de 15 min apenas quando usuário ativa a opção; Doze e restrições OEM podem adiar, nunca prometer monitoramento ininterrupto.
- Pendente: build Android completo e teste físico de comunicação, quedas, alertas, Wi-Fi/IP e modo Doze nos dois aparelhos.
