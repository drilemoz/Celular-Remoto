# Validação alpha.132 — atualizações locais e remotas

## Invariantes e verificações locais

- [x] Somente o servidor pareado com AES-GCM processa `appUpdate`. `status` somente leitura; `check`, `checkDownload`, `download` e `cancel` exigem opt-in local.
- [x] Sem instalação remota nem concessão automática de permissões; a Activity local chama `installFromUserTap` após confirmação, com SHA-256, pacote, código e certificado verificados.
- [x] Estado remoto limitado a versão, etapa, progresso e autorização do instalador, sem URL ou credencial. Polling apenas com tela Atualizações ativa por até 3 minutos; janela limitada, sem conexão persistente.
- [x] Transferências pendentes podem ser reconciliadas ou canceladas. Consulta transitória do DownloadManager não apaga a referência. Controles de bateria e rede preservados.
- [x] Testes puros de progresso e estados, parser Kotlin, XML/YAML, verificador estrutural e CRC do ZIP aprovados localmente; ainda não substituem o Gradle.

## Pendências reais

- [ ] `:app:compileDebugKotlin`, `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:lintDebug` no GitHub Actions.
- [ ] Dois aparelhos com a mesma assinatura de APK, DownloadManager operando em Wi-Fi, com dados móveis e sem rede; testar resposta autenticada, negação de opt-in, download, cancelamento e nova tentativa.
- [ ] Instalação exige permissão de fontes desconhecidas e confirmação no aparelho destino; testar interrupção no meio do download, reboot, APK adulterado, certificado trocado e falta de armazenamento.
- [ ] Confirmar UI em 320–393 dp, fonte ampliada e que polling para ao sair de Configurações/Atualizações ou apagar a tela.
