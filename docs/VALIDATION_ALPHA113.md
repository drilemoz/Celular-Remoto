# Validação da alpha.113 — bateria e previsões (04/11/2026)

Mudanças: corrente líquida preservada mesmo em CHARGING, média por contador em janela válida, previsões conservadoras de sessões comparáveis, diagnóstico na aba Bateria, histórico e metadados de versão.

## Verificações locais
- `python3 tools/check_distribution.py`
- Parser PSI Kotlin sobre fontes + testes.
- Compilação isolada Kotlin/JVM e testes de corrente/contador/previsões.
- XML/YAML/JSON e integridade do ZIP.

## Pendências
- `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:lintDebug`, `:app:assembleRelease` no CI.
- Testes de sinais negativos com carregador fraco, contraprova com multímetro USB, contador de carga indisponível, reconexão e histórico remoto em dois celulares.
- Previsão é estimativa por percentual observável (não medição de capacidade ou geração do painel solar); nenhum ajuste OEM confirmado nesta entrega.

## Resultado local desta entrega
- Verificador estrutural: aprovado, incluindo regressões das versões anteriores e novas regras alpha.113.
- Kotlin PSI: 227 arquivos `app/src` sem erros sintáticos.
- `kotlinc` compilou os módulos de cálculo da bateria; seis módulos centrais também passaram por compilação de tipos com stubs Android/JSON.
- 12 testes JVM executados isoladamente com substitutos de assert JUnit, incluindo os testes legados de sessão; aprovados.
- 55 XML e um YAML válidos; um único workflow.
- O resultado **não** comprova build Android completo nem comportamento em hardware.
