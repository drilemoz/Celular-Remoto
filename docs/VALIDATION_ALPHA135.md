# Validação da alpha.135 — alertas remotos

## Alterações
- Filtros independentes e locais para SMS, WhatsApp/Business e outros aplicativos, opt-in global inicialmente desligado; demais apps desligados por padrão.
- Cursores por host+chave autenticada, reinicializados na reativação, evitando replay e mistura entre pareamentos no mesmo IP.
- Janela de 60 min para eventos ainda retidos na RAM, compatível com consultas JobScheduler de no mínimo 15 min mas sem garantia de entrega, e rótulo com atraso medido.
- SMS recebido usa hora de chegada ao telefone para aviso; o texto/conversa mantém a data do SMS informada pelo Android.
- Resultado de consulta em segundo plano descartado caso o controlador tenha mudado de host ou de chave.

## Testes executáveis
- `python3 tools/check_distribution.py`: consistência das versões e invariantes legados.
- `app/src/test/java/br/dev/celularremoto/core/RemoteEventNoticePolicyTest.kt`: categorias, filtros, prazo, escopo criptográfico do cursor e rotulagem.
- Parser Kotlin de todas as fontes, validação XML/JSON quando disponível, estrutura e CRC do ZIP.
- Smoke JVM compilado da política nova, independente de `android.jar`.

## Pendências
- `:app:testDebugUnitTest`, `:app:assembleDebug` e `:app:lintDebug` em ambiente Android com Gradle/SDK disponível.
- Testar em dois dispositivos o opt-in dos avisos, chegada de SMS e WhatsApp, troca de chave durante polling, reinicialização do monitorado, atraso do JobScheduler, permissões negadas, aparelhos offline, notificações no lockscreen e barra de navegação Android 16.
- Verificar coexistência com o aplicativo alpha.134, especialmente que pareamento de protocolo alpha.111 continua funcionando e funcionalidades antigas não regridem.
