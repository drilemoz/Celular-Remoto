# Atualizador online do Celular Remoto (alpha.132)

## Como usar

1. No celular com `br.dev.celularremoto`, abra **Configurações → Atualizações**.
2. Deixe **Verificação automática** e **Download automático** ligados (padrão), ou toque **Verificar agora**.
3. Escolha rede não tarifada (padrão), proteção de bateria (padrão) e periodicidade 30 min/1 h/3 h/6 h/12 h/24 h (1 h por padrão).
4. Quando o download terminar, o Android executará o verificador. O estado ficará **Pronto para instalar** e, se as notificações estiverem liberadas, será emitido um aviso.
5. Toque **Instalar APK verificado** e confirme a instalação pelo instalador oficial do Android. Se necessário, autorize instalação desta origem nas configurações e volte para tocar novamente em Instalar.

## Fonte dos arquivos

- Repositório público: `https://github.com/drilemoz/Celular-Remoto`.
- Endpoint REST: `GET https://api.github.com/repos/drilemoz/Celular-Remoto/releases?per_page=30`.
- São aceitas Releases e prereleases publicadas, nunca rascunhos ou tags sem Release.
- A Release precisa conter exatamente `celular-remoto-update.json` com versão, código, `br.dev.celularremoto`, APK correspondente, SHA-256 e `stableSigned=true`.
- O único workflow `.github/workflows/android.yml` anexa o manifesto **somente** após `:app:assembleRelease` com quatro Secrets de assinatura configurados. Um APK Debug publicado sem Secrets não recebe manifesto.
- Antes de mostrar Pronto, o app confirma SHA-256 dos bytes baixados, `packageName`, `versionCode` mais novo e certificado de assinatura idêntico ao instalado.

## Restrições de execução

- A verificação usa JobScheduler periódico com rede, sem garantia de horário exato (Doze e restrições do fabricante podem adiar).
- O download usa DownloadManager, que pode continuar com a tela apagada e ao trocar de rede, sem despertar a Activity. O receptor de conclusão agenda a validação; uma checagem de contingência de 30 minutos evita depender exclusivamente da entrega do broadcast Android.
- Com proteção de bateria ligada, abaixo de 25% sem carregador ou durante teste Labs ativo, o download é adiado. Ao acabar essa condição, a próxima verificação ou busca manual poderá iniciar a transferência.
- Mesmo com o APK pronto, não há instalação silenciosa. O usuário do aparelho decide quando instalar; a instalação pode reiniciar o serviço e interromper transmissão/medição.
- Android 8.0+ pode exigir `REQUEST_INSTALL_PACKAGES` e habilitar a fonte específica. O app não aprova essa tela automaticamente.
- Se o celular está com um APK Debug de certificado diferente, não existe atualização direta pela Release nova. Preserve históricos antes de reinstalar.
- A conexão deixa de exigir versionName idêntico para alpha.111↔112: ambos usam a revisão de protocolo 111. Comandos de atualização remota exigem servidor alpha.112+, autorização local própria e pareamento. O APK não é transferido por LAN nesta entrega.

## Privacidade

Nenhum token GitHub é embutido e nenhuma keystore fica no aplicativo. O atualizador usa apenas informações públicas da Release. Acesso à internet é necessário somente para pesquisa e download. O APK fica numa pasta de downloads particular do app. Nenhum comando remoto pode acionar a instalação.

## Controle por aparelho pareado (alpha.112)

Configurações → Atualizações → Permitir pedidos remotos de atualização habilita apenas verificação/download, não instalação. O controlador pode consultar estado ou solicitar verificação e download no monitorado pelo canal autenticado; o aparelho de destino precisa ter internet. Pedidos são limitados para evitar abuso; o monitorado aplica restrições de rede não tarifada, bateria e Labs.

## Reforços na alpha.132

- O painel local mostra progresso de download, estado da origem do instalador, opções para atualizar o status e cancelar apenas uma transferência não verificada. Falhas não alteram o APK instalado; após nova consulta é possível recomeçar.
- O controlador pareado mostra data da última consulta, versão remota, etapa, percentual quando disponível e se a autorização de instalação está pendente. O botão **Acompanhar remoto por 3 minutos** consulta a cada 15 s apenas com a tela visível.
- `checkDownload` inicia verificação e download no monitorado, sujeito à autorização `remote_requests` configurada nele, bateria mínima e política de rede. `cancel` apenas remove download pendente; **não existe comando remoto de instalar**. Uma instância antiga pode não suportar o cancelamento.
- A origem de APK é verificada antes de instalar; o usuário deve conceder permissão e confirmar instalação na interface oficial do Android. Atualização de um APK assinado com outra chave continua impossível diretamente.
- As consultas remotas não expõem URL do APK, token, caminho do arquivo nem chave de pareamento. Um resumo antigo é rotulado como último estado consultado, não como conexão ativa.
- Validações locais cobrem progresso e critérios de download; build Android real, liberação pelo instalador e testes cruzados entre versões continuam pendentes.
