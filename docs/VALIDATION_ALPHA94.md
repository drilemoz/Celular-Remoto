# Validação alpha.94

- Mudanças: kit visual Android nativo, temas de diálogos, manutenção, avisos e cartões.
- Checks automáticos: `python3 tools/check_distribution.py`; XML parse; Kotlin PSI local; `unzip -t`.
- Pendente: `:app:assembleDebug`, lint/test Gradle e inspeção visual no Redmi A3/Redmi Note 11 Pro+.
- Segurança: sem comandos silenciosos; bloqueio e reinicialização continuam explicitamente confirmados.
