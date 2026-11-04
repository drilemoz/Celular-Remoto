# Validação da alpha.98

- Atualização de interface para conexão, pareamento, detalhes e ADB; nenhuma operação de rede, permissão, captura ou comando foi modificada.
- Verificar `python3 tools/check_distribution.py`, XML, JSON, parse Kotlin e integridade do ZIP.
- Pendente: compilar `:app:assembleDebug`/`:app:lintDebug` no GitHub Actions e verificar visual e fluxos reais nos dois aparelhos.
