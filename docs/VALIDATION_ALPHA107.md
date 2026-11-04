# Validação da alpha.107 — Celular Remoto

**Escopo:** migração de identidade, Labs aprimorado, sessões automáticas e alertas de metas locais/remotos.

- Fonte base: Monitor Remoto alpha.106 e regras técnicas estudadas do Pulso Bateria alpha.73. Não é uma cópia dos dois APKs; há um único aplicativo.
- Port TCP 37842; o antigo Monitor Remoto usa 37841 para evitar conflito ao manter ambos instalados durante a migração.
- Dados privados do aplicativo anterior **não migram automaticamente**. Exportação manual dos relatórios via versão anterior deve preceder sua desinstalação.
- Segurança: AES-GCM, versionamento exato, pareamento com chaves novas, limites de mensagem e confirmações de ações sensíveis mantidos. Alertas de bateria não podem desativar a proteção térmica crítica.
- Unidade de corrente: mA líquidos na bateria derivados das propriedades Android; W = mV × mA / 1.000.000 representa apenas potência elétrica líquida da bateria, **não geração solar real**.
- Ses. automáticas: máximo 120 registros, até 240 pontos por sessão, apenas um ativo, amostras 60s/mudança de %, preenchimento de lacunas proibido; dados salvos via AtomicFile.
- Alertas: configuração remota somente em operação autenticada, limites de entradas validados, deduplicação de eventos, disponibilidade sujeita às notificações Android e à política de consultas. Sem internet ou socket adicional.
- Critérios de teste físico: dois apps Celular Remoto alpha.107 pareados; iniciar Solar remotamente e verificar 100%, teste com tela apagada 2h, meta 80%, alertas de temperatura, desconexão/reconexão de Wi-Fi, modo duplo, backup de relatórios e compatibilidade Android 16.
- **Pendente:** Gradle/Android em CI e testes instrumentados/físicos. Aprovação de verificações locais não demonstra build instalável.
