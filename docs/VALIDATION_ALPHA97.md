# Validação da alpha.97

- Refatoração visual em `RemoteScreenActivity.kt` e `RemoteMessagesUi.kt`, sem mudanças no transporte, serviços ou permissões.
- Testar em dois aparelhos: cabeçalho Ver tela no modo retrato, controle de mídia e imagem, troca JPEG/H.264, tela cheia, ações de navegação e insets Android 15+.
- Testar SMS/Notificações: grupo, ausência de mensagens, não exibir OTP, botão copiar, Responder/Revisar/Confirmar, escolha de SIM quando presente e revogação.
- Validar tema claro/escuro, fontes grandes, rotação, rede desconectada e falhas.
- Verificador estrutural e análise sintática não substituem :app:compileDebugKotlin nem testes Android reais.
