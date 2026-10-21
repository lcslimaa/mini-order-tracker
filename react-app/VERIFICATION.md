# Verificação da mudança react-order-tracker

Execução em 20/10/2026, com Java 21 e Node 26, em worktree isolado.

- `./mvnw test`: 28 testes Java passaram, incluindo identidade, CORS/preflight,
  controle por perfil, concorrência e persistência.
- `npm test`: 25 testes passaram. Cobrem contrato HTTP, cadastro/login, falha de
  identidade, logout/401, descarte de resposta antiga, pedidos, conflitos,
  produtos e atualização periódica sem sobreposição, em aba visível.
  As regressões da revisão cobrem troca de editor durante salvamento, catálogo
  inicial pendente, erro de gateway com resultado incerto e pedido confirmado
  preservado quando a consulta falha (inclusive cancelamento nesse estado).
- `npm run typecheck` e `npm run build`: passaram.
- `npm audit --audit-level=high`: nenhuma vulnerabilidade encontrada.
- `npm run test:e2e`, com variáveis de operador de teste: passou em Chromium,
  usando Vite em localhost:3000, Java em 8080 e H2 em memória.

O percurso real verificou cadastro e login do cliente, login do operador,
cadastro/edição/disponibilidade de produto, criação com total calculado pelo
Java, avanço até entrega, atualização na área do cliente, cancelamento pelo
cliente em RECEBIDO e pelo operador em EM_PREPARO. O cancelamento do cliente
após início do preparo recebeu 409. Acesso de cliente à rota de operador foi
redirecionado. Saída e acesso ao login por teclado foram verificados.

Capturas de desktop e celular foram inspecionadas. As duas áreas foram
verificadas com largura de 390 px, sem rolagem horizontal da página. Produtos
podem usar rolagem interna da tabela em telas estreitas. Não houve avaliação
formal de conformidade WCAG.

Os dados de teste foram criados somente no banco em memória de verificação.
Não há migração de esquema nem credenciais de operador embutidas no frontend.
