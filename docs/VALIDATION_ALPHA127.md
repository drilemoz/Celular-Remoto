# Validação alpha.127 — 05/11/2026

## Escopo
- Pré-requisitos locais: estados de serviço, notificações, atualização, instalação, câmera, tela, toque, localização e opções voluntárias.
- UI de preparação abre Configurações/Android apenas após toque; `remote_requests` continua false inicialmente.
- Snapshot AES-GCM opcional: `readiness` contém schema, contagens e itens genéricos; mensagens e dados sensíveis ficam de fora.
- Matriz de testes JVM de política: IDs únicos, estados restritos e consentimento por sessão distinto de autorização.

## Segurança e regressão
- A instalação só pode ser disparada localmente por `UpdateController.installFromUserTap` já existente; novo módulo não o chama.
- Sem novas permissões, sem novos canais de rede, sem novos serviços ou troca de chaves.
- Os comandos de câmera/tela continuam exigindo autorização real e sessão local.

## Pendências
- Build Gradle/Android e testes instrumentados/visuais em aparelhos reais, inclusive Android 13–16.
- Confirmar versão antiga sem bloco `readiness` e versão atual com bloco autenticado.
