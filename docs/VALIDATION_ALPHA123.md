# Alpha.123 — melhoria visual e acessibilidade

Base: alpha.122. Mudanças: novo tema AMOLED com superfícies/diálogos próprios, ações de configurações responsivas, rótulos sem truncamento obrigatório e navegação adaptativa.

## Verificações locais

- `python3 tools/check_distribution.py` — executar após empacotar.
- Parse dos arquivos Kotlin, validação XML/YAML, testes puros `AppearancePolicyTest` e `CompactDesignPolicyTest`, integridade ZIP.
- Não contém keystore/Secrets; um workflow GitHub Actions.

## Pendências

- Build completo Gradle/Android em GitHub Actions.
- Testes visuais reais de temas claro, escuro, AMOLED e sistema com fontes 1,0x / 1,3x / 1,7x e 320/360/393dp.
- Conferência do contraste em AlertDialog, teclado, seletor e Activities secundárias, especialmente telas de arquivos e visualizador remoto.
