# Validação alpha.111 — Atualização online

Checklist estático executado antes do envio:

- [x] VersionName, versionCode, AppVersion, README, documentação e tela Novidades coerentes.
- [x] Único workflow GitHub Actions com manifest JSON somente para Release estável.
- [x] Configuração Android do JobScheduler, DownloadManager e receiver no manifesto.
- [x] Política de versões, nome de APK, hash, pacote, certificado e origem HTTPS restrita.
- [x] Novas classes Kotlin analisadas por parser, teste JVM das regras determinísticas.
- [x] YAML e XML válidos e ZIP íntegro.
- [ ] Compilação real Gradle/Android (pendente se SDK/dependências indisponíveis).
- [ ] Teste de rede e instalação no Android 16, DownloadManager e notificações (pendente).

Resultado técnico: `python tools/check_distribution.py` aprovado; 223 arquivos Kotlin (main e testes) sem erros sintáticos segundo parser Kotlin PSI; 11 verificações Kotlin/JVM de `UpdateRules` aprovadas; 55 XMLs válidos; YAML único com 15 etapas validado e shells `bash -n` aprovados; geração do manifesto JSON testada com APK sintético (sem publicação). Esses testes não substituem a compilação Android completa.
