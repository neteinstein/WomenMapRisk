<!--
  AUTHORITATIVE SOURCE. Transcribed verbatim from the client's functional spec (v1.0 draft),
  as pasted into the project kickoff on 2026-10-04. Only Markdown formatting (headings, lists,
  the edge-case table) was added. The text is unchanged. Section 2 is missing in the original;
  that gap is intentional and must not be "fixed".
  Do NOT edit wording here. Product decisions taken later live in docs/spec/decisions.md and
  docs/spec/addendum-invites.md.
-->

# Especificação funcional: app de mapa de segurança para mulheres

Versão: 1.0 (rascunho) · Plataforma: telemóvel (iOS e Android) · Âmbito: MVP com cidade piloto

## 1. Objetivo

Uma app de telemóvel com um mapa onde mulheres assinalam ruas e locais onde se sentiram inseguras ou desconfortáveis, para que outras mulheres que viajam sozinhas saibam que zonas evitar. A informação vem de outras mulheres e é anónima.

## 3. Utilizadoras

- Visitante (sem conta): vê o mapa e os alertas. Não pode reportar nem confirmar.
- Utilizadora registada: reporta, confirma reportes, guarda locais, gere o seu perfil.
- Moderadora: revê reportes sinalizados, remove conteúdo, bloqueia contas

## 4. Ecrãs

### Ecrã 1: Boas-vindas

Logótipo, uma frase sobre a app e dois botões: "Criar conta" e "Explorar sem conta".
Aviso curto de privacidade: "Os teus reportes são sempre anónimos."

### Ecrã 2: Registo e login

- Campos: email, palavra-passe, país.
- Caixa obrigatória: aceito os termos e a política de privacidade.
- Confirmação por email antes de poder reportar.
- Login com Google (desejável).
- Erros: email já registado, palavra-passe fraca, email não confirmado (mensagens claras, em português).

### Ecrã 3: Mapa (ecrã principal)

- Mapa a ocupar o ecrã todo, centrado na localização atual (se permitida) ou na cidade piloto.
- Zonas coloridas: verde (sem reportes), amarelo (alguns), vermelho (muitos). A cor deve ter também um símbolo ou legenda para ser percetível a quem tem daltonismo.
- Barra de pesquisa no topo (rua, morada ou local).
- Botão de filtros ao lado da pesquisa.
- Botão "Reportar" fixo, em destaque, em baixo.
- Botão "Centrar em mim".
- Menu em baixo: Mapa · Guardados · Perfil.
- Estado vazio (cidade sem dados): "Ainda sem informação nesta zona. Sê a primeira a contribuir."

### Ecrã 4: Detalhe de uma zona

Abre ao tocar numa zona colorida (painel que sobe a partir de baixo).

- Nome da rua ou zona aproximada.
- Número total de reportes e quantas mulheres os confirmaram.
- Lista de reportes, do mais recente para o mais antigo: tipo, quando aconteceu, período do dia (dia/noite), descrição, número de confirmações.
- Em cada reporte: botão "Também senti isto" e menu "Denunciar reporte".
- Botão "Guardar zona".
- Botão "Reportar aqui".

### Ecrã 5: Fazer um reporte

- Localização: por defeito, a posição atual; pode mover o pino no mapa ou pesquisar uma rua.
- Tipo de situação (escolha única): assédio verbal, seguida, rua mal iluminada, zona deserta, roubo, agressão, outro.
- Quando aconteceu: agora, hoje, esta semana, há mais tempo.
- Período do dia: dia ou noite.
- Descrição (opcional, máx. 300 caracteres), com aviso: "Não incluas nomes, matrículas nem dados que identifiquem pessoas."
- Botão "Enviar".
- Confirmação: "Obrigada, o teu reporte ajuda outras mulheres."

Se o tipo for "agressão", mostrar depois da confirmação os contactos de emergência do país e uma mensagem de apoio.
Pode editar ou apagar o reporte durante 24 horas.

### Ecrã 6: Filtros

- Tipo de situação (seleção múltipla).
- Período: última semana, último mês, últimos 6 meses, últimos 12 meses.
- Período do dia: dia, noite, ambos.
- Botões "Aplicar" e "Limpar filtros".

### Ecrã 7: Guardados

- Lista das zonas guardadas, com nome e cor atual.
- Tocar abre o mapa nessa zona. Deslizar para apagar.

### Ecrã 8: Perfil

- Email, pseudónimo (opcional), país.
- "Os meus reportes": lista com data, tipo e estado (pendente, publicado, removido).
- Definições, ajuda, termos e privacidade, terminar sessão.

### Ecrã 9: Definições e privacidade

- Permissão de localização (explicar porquê é necessária).
- Ativar ou desativar o histórico de localização (desativado por defeito).
- Exportar os meus dados.
- Apagar a minha conta e todos os meus dados.

### Ecrã 10: Painel de moderação (web, uso interno)

- Lista de reportes sinalizados e de reportes de estabelecimentos por rever.
- Ações: aprovar, remover, bloquear utilizadora, com motivo registado.
- Contadores: reportes novos, denúncias pendentes.

## 5. Fluxos principais

**Reportar:** Mapa → "Reportar" → Ecrã 5 → Enviar → confirmação → o reporte entra no mapa (ou fica pendente, se for sobre um estabelecimento).

**Consultar uma zona:** Mapa → tocar na zona → Ecrã 4 → confirmar ou denunciar reportes.

**Confirmar um reporte:** "Também senti isto" → o contador sobe 1. Cada utilizadora só pode confirmar o mesmo reporte uma vez, e não pode confirmar o seu.

## 6. Regras de negócio

### Cor das zonas

- Verde: 0 reportes ativos. Amarelo: 1 a 2. Vermelho: 3 ou mais (a definir com dados reais da cidade piloto).
- Só contam reportes dos últimos 12 meses.
- Reportes mais recentes ou com mais confirmações pesam mais no cálculo.

### Privacidade

- Reportes sempre anónimos para as outras utilizadoras.
- Nunca se mostra o ponto exato: a localização é arredondada para uma zona aproximada (raio de cerca de 50 m).
- O histórico de localização só é guardado se a utilizadora ativar.

### Prevenção de abusos

- Máximo de 5 reportes por utilizadora por dia.
- Reporte repetido da mesma utilizadora, no mesmo sítio e no mesmo dia, é bloqueado.
- Um reporte com 3 ou mais denúncias fica oculto até revisão.
- Reportes sobre estabelecimentos (bares, lojas, hotéis) são revistos antes de publicar.
- Proibido: nomes de pessoas, matrículas, fotografias de pessoas, insultos.

### Contas

- Só utilizadoras com email confirmado podem reportar.
- Contas com abusos repetidos são bloqueadas pela moderação.
- Verificação de que a utilizadora é mulher: decisão em aberto (ver secção 9).

## 7. Casos limite

| Situação | Comportamento esperado |
|---|---|
| Duas utilizadoras reportam o mesmo sítio ao mesmo tempo | Ambos os reportes são aceites e somam para a zona. |
| Sem internet | Mostra o mapa e os dados da última sessão, com aviso "Dados desatualizados". Botão de reportar desativado. |
| Localização recusada | O mapa abre na cidade piloto e a pesquisa continua a funcionar. |
| Reporte feito no sítio errado | Editável ou apagável durante 24 horas. |
| Reporte falso ou malicioso | Denúncias de outras utilizadoras e revisão pela moderação. |
| Utilizadora apaga a conta | Os reportes ficam no mapa, sem qualquer ligação à conta. |
| Zona sem dados | Mensagem de convite a contribuir (ecrã 3). |

## 8. Dados a guardar

- Utilizadora: email, pseudónimo, país, data de registo, estado (ativa, bloqueada).
- Reporte: localização aproximada, tipo, data do acontecimento, período do dia, descrição, número de confirmações, número de denúncias, estado (pendente, publicado, removido), data de criação.
- Confirmação: reporte, utilizadora, data.
- Denúncia: reporte, utilizadora, motivo, data.
- Zona guardada: utilizadora, localização, nome.

## 9. Decisões em aberto (para o cliente resolver)

- Como se verifica que a utilizadora é mulher? Opções: confiança e termos de uso; moderação a posteriori; verificação por documento (mais fiável, mas fere a privacidade e reduz adesão).
- Idiomas: só português na V1, ou português e inglês desde o início?
- Cidade piloto: Lisboa ou outra?
- Quem faz a moderação e com que disponibilidade?
- Modelo de negócio: gratuita, financiada, parcerias?
- Aconselhamento jurídico sobre responsabilidade por reportes e difamação antes do lançamento.

## 10. Fora da versão 1

Sugestão automática de trajetos seguros, alertas ao aproximar-se de zonas, botão de emergência, partilha de localização com contactos, lugares positivos, fotografias, pagamentos, versão web para utilizadoras, várias cidades.

## 11. Fases sugeridas

- Fase 1 (MVP): registo, mapa, reportar, ver zona, confirmar, denunciar, filtros, painel de moderação simples. Uma cidade.
- Fase 2: guardados, lugares positivos, mais idiomas, mais cidades.
- Fase 3: alertas de proximidade, trajetos mais seguros, botão de emergência, partilha de localização.

## 12. Critérios de aceitação (MVP)

- Uma utilizadora consegue fazer um reporte em menos de 1 minuto e no máximo 6 toques.
- Um reporte publicado aparece no mapa de outras utilizadoras em menos de 1 minuto.
- Nenhum ecrã mostra a localização exata de um reporte.
- Apagar a conta remove todos os dados pessoais.
- O mapa carrega em menos de 3 segundos com ligação 4G.
- A app funciona em iOS e Android nas duas últimas versões do sistema.
