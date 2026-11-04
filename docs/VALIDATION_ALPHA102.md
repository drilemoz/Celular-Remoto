# Validação do fonte alpha.102

## Escopo

- Modernização de Configurações, seleção do perfil GPS e apresentação de diálogos centralizados.
- Nenhuma mudança no protocolo, permissões declaradas, captura, ADB, criptografia ou permissões de arquivos.
- Regra de apresentação: autorização de GPS, câmera e notificações é a permissão Android **local**, não prova de conexão ou serviço ativo.

## Verificações locais

- `python3 tools/check_distribution.py` (regressões de alpha.101 mantidas e alpha.102 acrescentada).
- Parser PSI Kotlin em todos os fontes `*.kt` (sintaxe apenas).
- Parse de XML de recursos e Manifest.
- Verificação de integridade ZIP e sincronização de `versionName`/`versionCode`.

## Pendente

- Gradle Android real (`:app:compileDebugKotlin`, `:app:processDebugResources`, `:app:assembleDebug`) em GitHub Actions.
- Verificação visual da abertura de Configurações, permissões, busca e diálogo GPS em Android 8+ e Android recente, nos temas claro/escuro.
- Confirmar atualização do APK com assinatura existente e mesma versão nos dois aparelhos.
