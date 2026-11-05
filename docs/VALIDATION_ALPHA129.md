# Validação alpha.129 — 05/11/2026

## Alterações verificadas estaticamente

- `MonitorContinuityStore`: marca inicialização/última execução/parada e recriações sem término observado, com persistência privada e intervalo de heartbeat >=120 s.
- `MonitorService`: chamada redundante `onStartCommand` não reinicializa a agenda; quando já ativo, somente valida listener e atualiza a notificação.
- Falhas do gerenciador de notificações são capturadas, permitindo nova tentativa, sem marcar a assinatura visual como publicada prematuramente.
- Callback de rede monitora apenas a rede padrão; não confundir alteração de conectividade com sucesso de uma consulta autenticada.
- UI local de energia exibe diagnósticos sem coletar PII e sem alterar WakeLocks automaticamente.

## Ensaios necessários

- Gradle / GitHub Actions: compilar Debug e Release, executar todos os testes unitários, lint e validar APK assinado.
- Android real: executar Monitorado, iniciar serviço duas vezes, verificar um único supervisor/coleção e persistência dos registros.
- Tela apagada: medir serviço parado/retomado, Wi-Fi versus rede celular, sem/ com WakeLock opcional (30 min) e impacto de bateria/temperatura em dois celulares.
- Testar revogação de notificação, ausência de Wi-Fi, encerramento do processo pelo Android e force-stop manual (não há retomada garantida após force-stop).

Resultados do parser Kotlin e testes isolados não equivalem a compilação Android/Gradle.
