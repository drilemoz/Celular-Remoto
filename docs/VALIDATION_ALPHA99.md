# Validação da alpha.99 · correção do workflow 86

## Falha recebida

`MonitorRemoto-Gerar-APK-e-Release-86-logs.zip`, etapa `:app:processDebugResources`:

```
error: style attribute 'android:attr/windowDimAmount' not found.
```

Atributo inexistente nos dois temas (`MonitorDialogLight` e `MonitorDialogDark`), causador da interrupção de AAPT2. Alterado para `android:backgroundDimAmount`, declarado na plataforma Android API 26 e 36, preservando os valores `0.58` e `0.66`.

## Checagens

- Verificador `tools/check_distribution.py` cobre os temas corrigidos e bloqueia o atributo inválido.
- Validação local concluída: `tools/check_distribution.py` aprovado, parser Kotlin PSI em 196 arquivos sem erros sintáticos, 54 XML válidos e versão 0.1.0-alpha.99/versionCode 99 sincronizada.
- Gradle/Android SDK não disponível no ambiente local: `:app:processDebugResources`, `:app:testDebugUnitTest`, `:app:assembleDebug` e teste físico precisam ser repetidos no GitHub Actions/Android.
- Nenhuma mudança de código funcional, protocolo, permissões ou operações ADB.
