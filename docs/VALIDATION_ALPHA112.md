# Validação alpha.112 — atualizações remotas e compatibilidade (04/11/2026)

## Mudanças
- `versionName=0.1.0-alpha.112` e `versionCode=112`, preservado único workflow.
- Protocolo AES-GCM igual; marcador `appVersion=0.1.0-alpha.111` no fio para conectar alpha.111 e alpha.112. VersionName real disponível em `actualVersion` e `appUpdate/status`.
- Novos comandos `appUpdate` limitados ao peer alpha.112+. Autorização local separada para consultas/download remotos, limitação de requisições e nenhuma instalação por comando.
- Agendamento JobScheduler padrão 60 min, opções 30/60/180/360/720/1440 min e consulta após abrir quando estiver vencido. Execução real sujeita ao Android.

## Testes a executar na CI
- `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:lintDebug` e `:app:assembleRelease` com assinatura estável.
- Pareamento e consulta 111↔112; 110 pode recusar por protocolo antigo.
- Solicitação remota sem opt-in: rejeitada; com opt-in: verifica GitHub e baixa respeitando conexão não tarifada, temperatura e bateria.
- Confirmar arquivo SHA-256 e certificado antes de solicitar instalação no próprio aparelho.
- Validar Doze, conexão interrompida, Wi-Fi sem internet, teste Labs ativo, chaves revogadas e dois celulares simultâneos.

## Limites
O parser Kotlin e as verificações estáticas não comprovam compilação Android. O recebimento de APK por Wi-Fi local entre aparelhos está fora desta entrega.
