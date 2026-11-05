# Validação da alpha.125 (05/11/2026)

## Objetivo

A notificação foreground deve mostrar o aparelho local (percentual, temperatura, estado e corrente) e, quando disponível, previsão conservadora. Expandida, distingue serviço Monitorado de status do controlador remoto, baseado em última resposta autenticada.

## Regressões prioritárias

- Carregando + corrente negativa: não inverter o sinal nem assumir potência solar.
- Sem sessão confiável: não inventar previsão.
- Dados com mais de três minutos: omitir como atuais.
- Aparelho remoto sem resposta: indicar ausência, não afirmar conexão.
- Android bloqueado: versão pública da notificação não expõe detalhes.
- Nenhuma thread ou serviço novos, nenhuma consulta TCP adicional, nenhuma mudança de chaves.

## Pendências

- Build Android completo no GitHub Actions.
- Validação visual e de bateria em aparelhos reais, com tela apagada/Doze e rede intermitente.
