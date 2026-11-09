# Validação da alpha.136 — eventos locais e Energia

Data: 09/11/2026. Base: ZIP alpha.135 fornecido anteriormente.

## Verificações locais executadas

- `python3 tools/check_distribution.py`: **aprovado**, incluindo regras das versões anteriores e regressões da alpha.136 sobre autenticação, consentimento local, metadados mínimos, limite de espera e acionamento de Acender tela.
- Parser PSI Kotlin: **282 arquivos Kotlin**, **zero erros sintáticos**. Não realiza resolução completa das dependências Android.
- Teste Kotlin/JVM isolado da `RemoteEventSignal`: **sete verificações aprovadas**, cobrindo revisão, sinalização, término antecipado, timeout, limite máximo e interrupção do worker. Os testes JUnit equivalentes foram adicionados ao projeto, mas a suíte do Gradle não foi executada neste ambiente.
- XML: **56 arquivos válidos**; YAML: **2 arquivos válidos**; JSON: nenhum arquivo na árvore.
- Integridade CRC/estrutura do arquivo ZIP: verificar após a criação do pacote final.

## Mudanças revisadas

- `eventWatch` autenticado em uma única porta AES-GCM com espera máxima de 11 s e um observador simultâneo (`Semaphore(1)`), preservando dois workers para comandos.
- O servidor revalida se o monitorado permanece ativo e se a chave não mudou antes de transmitir eventos; SMS exige consentimento e permissão. A resposta contém apenas id, hora, tipo e pacote, além de revisão/epoch.
- Controlador revalida host/chave, encerra o socket em `onPause`, ignora respostas de sessões anteriores, aplica backoff em erros e mantém o JobScheduler em segundo plano.
- Mensagens pode atualizar por evento com página aberta; existe opção de desligar e coalescência de eventos recebidos durante consultas em andamento.
- Acender tela do menu Energia usa `wakeScreen` já existente, exige confirmação e consentimento configurado no monitorado; não desbloqueia.

## Pendente e riscos

- **Não foi possível executar a compilação completa Gradle/Android**, pois este ambiente não dispõe de SDK Android/Gradle Wrapper baixado. Portanto, não há APK compilado ou prova de integração entre servidor e controlador.
- Falta testar dois aparelhos com o mesmo APK, ADB autorizado, redes diferentes, troca de host/chave, Doze, permissões revogadas, SMS e notificações WhatsApp reais, disputas por slots, reconexão e consumo de energia.
- Enquanto o controlador não estiver visível, o Android pode adiar o JobScheduler em 15 minutos ou mais. Nem `eventWatch` nem as consultas periódicas garantem entrega imediata/contínua.
