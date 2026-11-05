# Validação da alpha.121 — Configurações, permissões e diálogos

## Verificações locais
- Gradle `versionCode=121`, `versionName=0.1.0-alpha.121`, AppVersion sincronizado.
- `tools/check_distribution.py` e testes Kotlin/JVM disponíveis.
- Analisar sintaxe Kotlin, recursos XML, workflow único e CRC do ZIP; esses checks NÃO substituem build.

## Testes obrigatórios no Android
- CI: `:app:testDebugUnitTest`, `:app:assembleDebug` e `:app:lintDebug`.
- Tela de 320/360/393dp; fonte normal e ampliada; orientação e temas claro/escuro.
- Permissões: concedidas, negadas, negadas permanentemente, sem telefonia; conferir o estado ao retornar do sistema. Solicitação de SMS continua separada.
- Acessibilidade, listener, uso e localização em segundo plano abrem apenas páginas oficiais; nenhuma autorização automática.
- Atualizações: verificar/download/assinatura/instalar no próprio aparelho; comando remoto NÃO instala nem ignora configuração de rede/bateria.
- Novidades: abrir e fechar, acessar histórico, detalhes e retornar; confirmações destrutivas mantêm cancelar.
- Monitorado/Controlar, bateria, pareamento e navegação sem regressões; testes com dois aparelhos.

## Limitações
Sem SDK/Gradle Android local e sem inspeção visual em aparelhos no ambiente desta entrega; testes físicos e build CI permanecem pendentes.
