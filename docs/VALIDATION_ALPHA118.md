# Validação da alpha.118 — design compacto

## Mudanças sob verificação

- Início e Configurações reorganizados; todas as ações e destinos preservados.
- Grade responsiva com 2 colunas em tela pequena/fonte ampliada e 3 colunas em celular comum, sem deixar títulos invisíveis por regra fixa de altura.
- Controlar mantém 12 ações autenticadas; Monitorado preserva confirmação explícita para captura/tela; Bateria continua medindo apenas corrente líquida.
- Novidades mostra as mudanças da versão, mas oferece histórico completo separadamente.

## Ensaios que NÃO foram concluídos localmente

- `:app:assembleDebug`, `:app:testDebugUnitTest` e `:app:lintDebug` via Android Gradle (necessita SDK/dependências).
- Renderização nos dispositivos e densidades 320/360/393dp, tamanhos de fonte ampliados, TalkBack, contraste/tema AMOLED e rolagem.
- Testes de QR, Controle, Bateria e atualização com dois aparelhos reais.

Nunca classificar teste estático como APK compilado nem emular o resultado do hardware.
