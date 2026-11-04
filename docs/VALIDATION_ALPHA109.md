# Validação planejada — 0.1.0-alpha.109 (04/11/2026)

## Log da execução 3

- Task `:app:testDebugUnitTest` apresentou 183 testes, 1 falha: `PairingCodeTest.rejectsUnexpectedQr` linha 46, com `java.lang.AssertionError`.
- Código do teste fazia `qr.replace("port=37841", "port=8080")`, mas `PairingCode.encode` já emitia `port=37842`, logo o QR de teste permanecia inalterado.
- Compilação dos fontes Debug Kotlin ocorreu, mas o fluxo falhou antes de `:app:assembleDebug`. Não há evidência de APK da execução 3.

## Ajustes

- Mutação da porta baseada em `WireProtocol.PORT`, e `assertNotEquals` para cada QR modificado.
- Esquema de QR sincronizado com o nome do aplicativo (`celularremoto://pair`) e documentação corrigida. Exigir nova versão e novo QR no pareamento.
- Sem alteração na criptografia AES-GCM, na porta 37842 ou no servidor remoto.

## Aceite na CI

1. Confirmar `:app:testDebugUnitTest` sem falhas, incluindo `PairingCodeTest`.
2. Confirmar `:app:assembleDebug` e publicação de APK na Release, respeitando assinatura configurada.
3. Instalar a alpha.109 nos dois celulares, gerar novo QR e verificar QR válido, porta errada, IP externo e rejeição de QRs antigos.
4. Validar os testes de bateria local/remota e alertas entre os dois aparelhos.

Testes locais não equivalem ao build Android completo.
