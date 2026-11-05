# Plano de verificação da alpha.119

## Alterações de layout
- Verificar a faixa de três indicadores com texto legível em 360/393dp e reorganização em duas colunas com fonte ampliada ou 320dp.
- Conferir os doze comandos remotos, incluindo menu de funções adicionais e os detalhes completos de Wi-Fi, sem mudar os dados transmitidos.
- No Monitorado, confirmar o painel único Tela/Câmera/Foto, estado real e operação de parar, controles de ativar/interromper e acessos autenticados.
- Confirmar que Tela e Câmera ainda pedem autorização local e Foto só abre preparo local; não usar rede para iniciar captura sem consentimento.
- Checar o tema escuro, toque acessível, TalkBack, rolagem, fontes 1.0 e 1.3 e diálogos de manutenção/som/lanterna.

## Pendências de execução
- Não foi possível executar Gradle Android completo nem um emulador/aparelho no ambiente de entrega. A CI deve executar `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:lintDebug` e build assinado.
- Não confundir validação sintática/estrutural com APK compilado ou funcionamento real em dois celulares.
