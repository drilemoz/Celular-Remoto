# Validação — 0.1.0-alpha.144

Base: ZIP 0.1.0-alpha.143. Escopo: atualizador do Celular Remoto; não modifica o protocolo nem o workflow.

- Validador de distribuição: executado com verificações de integração com onResume, JobScheduler, preferência de download e ação remota explícita.
- JUnit isolado: 13 casos do atualizador passaram (gatilhos, intervalo mínimo, limites de progresso, critérios de Release e leitura de tag alpha).
- Compilação direta aprovada com Android SDK 36, Java 17 e Kotlin 2.2.21: recursos vinculados, 21 fontes Java (incluindo R e BuildConfig gerados) e 216 fontes Kotlin compiladas. Os testes isolados também passaram no Kotlin 2.2.21.
- Não foi executado o empacotamento Gradle/Actions. Teste em aparelho e publicação da Release ainda pendentes.

## Teste após a publicação

1. Em uma instalação com assinatura estável, habilitar verificação e download automáticos, publicar uma Release alpha de versionCode maior pela CI com o mesmo certificado, e retornar ao app após pelo menos um minuto da consulta anterior.
2. Abrir Configurações → Atualizações. Confirmar início do download conforme Wi-Fi/bateria, atualização do progresso sem reabrir a página e botão Instalar somente após verificar o APK.
3. Desligar download automático e tocar Verificar agora. Confirmar estado de versão disponível e botão Baixar APK, sem transferir até o toque. Repetir com download automático ligado e verificação automática desligada: apenas o toque deve consultar e baixar.
4. Simular Release alpha mais recente sem manifesto ou sem APK elegível. Não deve sugerir instalação nem afirmar que o build é uma atualização válida.
5. Retomar o app várias vezes em menos de um minuto. Sem requisições automáticas duplicadas. Confirmar que não há instalação sem aceite no Android e que o controle pareado ainda exige autorização local.
