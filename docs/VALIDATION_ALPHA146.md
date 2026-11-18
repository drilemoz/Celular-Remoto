# Validação alpha.146 — pesquisa global de funções

- Base `0.1.0-alpha.145`. Versão `0.1.0-alpha.146` / versionCode 146, SDK 36, WIRE_COMPAT alpha.111.
- Roteiro em aparelho: localizar e abrir a lupa em Controlar, Monitorado, Bateria, Mensagens, início e subseções de Ajustes. Conferir retrato, paisagem, teclado aberto, botões de voltar, tema escuro e fonte grande. Os últimos itens devem rolar acima da lupa.
- Buscar `painel solar`, `chamdas`, `Qr code`, `GPS`, `zap`, `modo escuro` e consulta sem resultado. Confirmar que o resultado abre a seção/ferramenta certa, que um atalho remoto sem chave abre o pareamento e que tocar no resultado não concede permissão, dispara comando ou mexe em preferências.
- Validador estrutural, AAPT2 API 36 e compilação Java/Kotlin 2.2.21 de 224 fontes Kotlin aprovados.
- Suíte JVM JUnit com 295 testes aprovados. Inventário executado com 172 atalhos. Foram conferidos os primeiros resultados para `painel solar`, `chamdas`, `GPS`, `download automático`, `tema escuro` e `zap`.
- APK de release e verificação visual em celulares com teclado, fontes e temas diferentes pendentes.
