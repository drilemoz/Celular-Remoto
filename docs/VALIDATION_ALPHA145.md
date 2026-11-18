# Validação alpha.145 — chamadas e avisos

- Base `0.1.0-alpha.144`; versão `0.1.0-alpha.145` / versionCode 145. Android SDK 36; `WIRE_COMPAT` alpha.111.
- Roteiro em dois celulares: negar/autorizar READ_CALL_LOG; consultar com e sem histórico; chamada perdida, recebida e efetuada; receber SMS com a tela do controlador apagada; conferir filtro de SMS sensível, aviso sem remetente/texto e chamada sem número; desligar cada filtro; testar reconexão, troca de pareamento, revogação e parada pelo serviço. Testar também bateria local + avisos e Monitorado + avisos.
- Confirmar que não há repetição entre consulta periódica e espera, que dados de histórico não aparecem após troca de par e que as funções antigas continuam em um aparelho pré-alpha.145.
- Validador estrutural `tools/check_distribution.py`: aprovado.
- AAPT2 e manifesto processados contra SDK 36. Compilação direta do Java e de 221 arquivos Kotlin com Kotlin 2.2.21/JVM 17: aprovada. Foram usados o Android SDK e as bibliotecas da verificação local, sem empacotar APK de release.
- Suíte JVM JUnit: 291 testes aprovados, incluindo as regressões sobre chamadas ocultas, linha de base, chamadas antigas, duplicação e filtros de chamadas e SMS.
- Pendente: workflow completo do Gradle para gerar APK de release e teste de entrega/recebimento em dois celulares reais. O validador local não verifica a concessão de READ_CALL_LOG, Doze ou latência da rede real.
