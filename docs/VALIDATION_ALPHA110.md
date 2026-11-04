# Validação da alpha.110 — Central de permissões

- Verificar onCreate: primeira instalação inicia em Configurações → Central de permissões e não ativa monitoramento ou coleta.
- Verificar listagem dinâmica: câmera, localização, áudio, notificações, telefonia, SMS, Acessibilidade, listener, estatísticas de uso, admin, GPS de segundo plano, captura de tela e SAF.
- Solicitações comuns e SMS: um toque explícito dispara diálogos Android, respeita negação e resultado; nenhuma opção de compartilhamento remoto é habilitada por conceder permissão.
- Em Android 12+, localização fina e aproximada solicitadas juntas; localização de segundo plano apenas via Configurações, após primeiro plano.
- Se acesso for revogado fora do app, estado atualiza no onResume; banner indica pendências e botão abre a central.
- Confirmar em duas instalações (limpa e atualização), com/sem chip, Android 13+, e UX com fonte ampliada.
- Testes locais e verificações XML/Kotlin **não substituem** CI `:app:testDebugUnitTest :app:assembleDebug` nem validação física.
