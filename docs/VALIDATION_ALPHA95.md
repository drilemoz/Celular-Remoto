# Validação local — Monitor Remoto 0.1.0-alpha.95

## Mudanças
- `MonitorActionSheet.kt` centraliza menus inferiores com ícones, títulos, descrições e diferenciação de ações destrutivas. A chamada à operação ocorre somente após o toque, precedida de `dialog.dismiss()`.
- `RemoteFilesActivity` migrou ações, ordenação e checkpoints interrompidos para o mesmo menu.
- `RemoteCatalogActivity` migrou o menu de ações por aplicativo, sem alterar o fluxo de confirmação ADB.
- `SettingsHubUi` ganhou agrupamento visual compacto, mantendo a busca e destinos anteriores.
- Notas de versão, Gradle, AppVersion, validador e Novidades sincronizados com alpha.95.

## Verificações
- `python3 tools/check_distribution.py`: aprovado, com regressões de preservação das confirmações e versão sequencial.
- Parser Kotlin PSI: 195 arquivos sem erro sintático (não equivale à compilação Android, nem verifica tipos e referências da SDK).
- Verificação de XML/JSON e integridade ZIP local: executar antes da entrega.

## Pendências
- Compilar `:app:testDebugUnitTest` e `:app:assembleDebug` no GitHub Actions, testar cliques, foco, scroll e tamanho dos menus em Android, especialmente temas claro/escuro e fontes ampliadas.
- Não houve teste físico de ADB, segurança dos diálogos ou uploads/downloads nesta revisão visual.
