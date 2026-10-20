# Design

## Context

O repositório contém somente a estrutura OpenSpec. A motivação e o escopo estão em proposal.md; os contratos estão nas três especificações desta mudança. Este design é necessário por envolver uma aplicação nova, autenticação, autorização por objeto e persistência.

## Goals / Non-Goals

**Goals:** API pequena, executável localmente, com separação entre transporte, regras de negócio e persistência; decisões de segurança verificáveis por testes HTTP reais; transações que preservem o histórico e a consistência dos pedidos.

**Non-Goals:** arquitetura distribuída, frontend, mecanismo de permissões configurável, operação de múltiplas instâncias sobre o mesmo arquivo e administração completa de usuários.

## Decisions

### Aplicação e organização

Usar Java com Spring Boot, Spring Web, Spring Security, Bean Validation e Spring Data JPA. Um monólito organizado por autenticação, catálogo e pedidos, com controllers, serviços transacionais e repositórios, é suficiente. Evitar microserviços e infraestrutura de mensageria por não haver necessidade no MVP. Usar Maven Wrapper; selecionar versões compatíveis e suportadas ao iniciar a implementação, sem fixar versões não verificadas nesta exploração.

### Persistência

Usar H2 em arquivo, conforme confirmado, em vez de SQLite, para manter o ambiente local simples com persistência relacional integrada. Usar migrações SQL versionadas e validação do esquema, sem depender de recriação automática. Testes comuns usam banco isolado; um teste reabre o mesmo arquivo temporário para verificar durabilidade. PostgreSQL fica como alternativa futura se houver necessidade de operação concorrente em múltiplas instâncias.

Modelo:

- Usuário: ID, nome, email normalizado com restrição única, hash BCrypt e perfil CLIENTE/OPERADOR.
- Produto: ID, nome, preço decimal, disponível e versão para concorrência.
- Pedido: ID, cliente, endereço textual, status, total decimal, data de criação e versão.
- Item: ID, pedido, produto, nome na compra, preço na compra, quantidade e subtotal.

Usar BigDecimal e colunas decimais para dinheiro; não usar ponto flutuante. O endereço será um texto não vazio no MVP. Quantidade deve ser inteira positiva; produtos repetidos na mesma requisição serão rejeitados para evitar interpretação ambígua.

### Autenticação e provisionamento

Spring Security sem sessão, com Bearer JWT assinado e validação de assinatura e expiração. Proposta operacional: expiração padrão de 60 minutos configurável; segredo externo obrigatório, sem valor padrão embutido. Usar componentes de validação JWT do framework em vez de implementar criptografia própria. O sujeito identifica o usuário; carregar o usuário persistido para obter o perfil confiável. BCrypt para hashes, nunca expostos em DTOs ou logs. Senhas devem ser não vazias e respeitar o limite em bytes do BCrypt, com rejeição explícita em vez de truncamento.

Cadastro e login são públicos. Rotas funcionais restantes exigem autenticação; o console H2 não será exposto. Não usar cookies de autenticação; desabilitação de CSRF é vinculada ao uso exclusivo de Bearer no header. Não habilitar CORS amplo sem um consumidor definido.

Provisionar operador por email, nome e senha de configuração externa. Configuração ausente não cria usuário; configuração parcial ou colisão com CLIENTE falha claramente. Se o email já pertence a OPERADOR, não sobrescrever credenciais. Cadastro público rejeita campos de perfil. A restrição única no banco também protege contra cadastros concorrentes do mesmo email, traduzindo a colisão em 409.

### Contrato REST

Todas as rotas abaixo usam prefixo `/api`. Respostas usam JSON. Listagens retornam arrays no MVP, ordenados por ID para previsibilidade.

| Método e rota | Acesso | Entrada / resultado |
|---|---|---|
| POST /auth/register | Público | nome, email, senha; 201 com ID, nome, email e perfil |
| POST /auth/login | Público | email, senha; 200 com accessToken, tokenType e expiresIn |
| GET /products | Autenticado | cliente vê disponíveis; operador vê todos |
| POST /products | Operador | nome, preço, disponibilidade opcional; 201 |
| PUT /products/{id} | Operador | nome, preço, disponibilidade; 200 |
| POST /orders | Cliente | endereço e itens com productId e quantity; 201 |
| GET /orders | Autenticado | próprios para cliente, todos para operador; 200 |
| GET /orders/{id} | Autenticado | consulta conforme propriedade/perfil; 200 ou 404 |
| PATCH /orders/{id}/status | Operador | status de destino; 200 |
| POST /orders/{id}/cancel | Autenticado | cancelamento conforme perfil e estado; 200 |

DTOs de pedido retornam ID, identificação do cliente sem credenciais, endereço, itens históricos, total, status e criação. Separar DTOs de entidades para impedir atribuição de campos privilegiados e serialização acidental. Rejeitar campos desconhecidos nos corpos de entrada.

Padrões propostos para fechar pontos não detalhados durante a conversa: somente CLIENTE cria pedidos; endereço textual; listagens sem paginação; cancelamento por endpoint próprio; rejeição de produtos repetidos. São decisões deste planejamento para revisão, não decisões anteriormente confirmadas pelo usuário.

### Regras de pedido e concorrência

Criação determina o proprietário exclusivamente pelo usuário autenticado, consulta produtos disponíveis e captura preços e nomes numa única transação. Travar os produtos consultados em ordem de ID durante a captura para coordenar com alterações do catálogo; evitar pedidos parcialmente persistidos. Nenhum valor monetário é aceito do cliente.

Para consultas do cliente, buscar por ID e proprietário conjuntamente; ausência retorna 404. Atualizações de status e cancelamento usam versão otimista: alterações concorrentes resultam em 409 e rollback. A verificação do estado ocorre na mesma transação da alteração.

| Estado atual | Próximo estado pelo operador | Cancelamento pelo cliente proprietário | Cancelamento pelo operador |
|---|---|---|---|
| RECEBIDO | EM_PREPARO | Sim | Sim |
| EM_PREPARO | SAIU_PARA_ENTREGA | Não | Sim |
| SAIU_PARA_ENTREGA | ENTREGUE | Não | Sim |
| ENTREGUE | Nenhum | Não | Não |
| CANCELADO | Nenhum | Não | Não |

Não permitir saltos, regressões ou repetição do estado. O endpoint de status é exclusivo do operador e aceita também CANCELADO com as mesmas regras do endpoint de cancelamento.

### Erros e testes

Respostas de erro padronizadas com status, código estável e mensagem, sem stack traces ou credenciais. Usar 400 para entrada inválida, 401 para autenticação ausente/inválida, 403 para perfil proibido, 404 para recurso inexistente ou pedido de terceiro e 409 para email duplicado, transição inválida ou conflito concorrente. Aplicar autenticação antes de validar o corpo das rotas protegidas.

Testes de integração devem iniciar o contexto completo e exercitar a cadeia de segurança com tokens obtidos pelo login, repositórios reais e banco isolado. Cobrir cadastro duplicado, injeção de perfil, BCrypt persistido, JWT inválido/expirado, todas as rotas anônimas, matriz de perfis, isolamento com dois clientes, catálogo, cálculos, histórico, matriz de status, cancelamentos, concorrência e persistência após reabertura. Usar testes unitários apenas onde agreguem cobertura clara, como matriz de transições e cálculo.

## Risks / Trade-offs

- Arquivo H2 limita concorrência e operação distribuída → manter uma instância e testar reabertura; planejar banco servidor se a necessidade surgir.
- JWT emitido permanece válido até expirar, sem logout revogável → expiração configurável e ausência de refresh token no MVP.
- Listagens sem paginação crescem com a base → aceitável para o MVP, com paginação como evolução.
- Credenciais de bootstrap mal configuradas impedem acesso do operador → validação de configuração e instruções locais sem segredos no repositório.
- Disputa entre cancelamento e início do preparo → controle de versão e resposta 409 para o perdedor, sem sobrescrever o estado vencedor.

## Migration Plan

Como não existe aplicação anterior, não há migração de dados legados. Na implementação: criar projeto e migração inicial; configurar caminho externo do arquivo H2 e segredos; executar testes; iniciar com configuração do operador; verificar login e fluxo de pedido. Antes de mudanças futuras de esquema, fazer backup do arquivo com a aplicação parada. Para rollback de migração incompatível, parar a aplicação e restaurar backup consistente junto à versão anterior.
