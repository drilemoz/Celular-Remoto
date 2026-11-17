# Validação alpha.143 — bateria compacta

- Origem: ZIP da alpha.142. Versão nova: `0.1.0-alpha.143`, `versionCode 143`. `WIRE_COMPAT` permanece na alpha.111.
- Aprovados `python3 tools/check_distribution.py` e a leitura dos 55 arquivos XML de `app/src/main`. O validador cobre a separação das abas, cartão, menu, relatórios e invariantes de distribuição.
- Casos JVM em `BatteryDashboardPolicyTest.kt`: largura/fonte, validade e consistência da sessão, destinos da previsão, duração curta e rótulos dos eixos.
- Roteiro para dois aparelhos: alternar local/remoto; consultar, desconectar e reconectar; conferir que leitura anterior mantém sua indicação e não produz previsão atual; conferir descarga até 0% e carga até 100%; abrir e salvar os três limites no aparelho escolhido; mudar a coleta local pelo menu; abrir teste ativo, histórico e relatório compartilhado; alternar as três métricas em tela estreita e fonte grande; confirmar rótulos distintos em sessão de 30 s e ausência de ligações sobre lacunas.
- `:app:testDebugUnitTest` e `:app:assembleDebug` não executados. O executor não possui Gradle ou Android SDK. Executar o CI existente e o teste físico antes de publicar um APK.
