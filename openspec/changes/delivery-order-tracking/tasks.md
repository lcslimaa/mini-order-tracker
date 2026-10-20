# Tasks

## 1. Fundação e persistência

- [x] 1.1 Criar projeto Java/Spring Boot com Maven Wrapper e dependências Web, Security, Validation, JPA, JWT, H2, migrações e testes; verificar compilação com `./mvnw test` e inicialização do contexto.
- [x] 1.2 Criar migração inicial de usuários, produtos, pedidos e itens, incluindo email único, referências, valores decimais e versões; verificar aplicação da migração em banco vazio e validação do esquema na reabertura.
- [x] 1.3 Configurar H2 em arquivo e perfil de testes isolado; documentar execução e caminho dos dados no README e verificar inicialização usando a configuração documentada.

## 2. Autenticação e segurança

- [x] 2.1 Implementar cadastro CLIENTE com normalização de email, BCrypt, DTOs e rejeição de campos privilegiados; testar cadastro válido, email duplicado inclusive concorrente, entrada inválida, perfil forjado e ausência de senha/hash nas respostas.
- [x] 2.2 Implementar login e JWT com assinatura, expiração configurável e segredo externo; testar credenciais válidas, credenciais inválidas, token expirado, adulterado e assinado com outra chave.
- [x] 2.3 Configurar cadeia stateless, autorização por perfil e erros JSON uniformes; testar 401 nas rotas protegidas sem token e 403 em ações de operador solicitadas por cliente.
- [x] 2.4 Implementar provisionamento idempotente do operador; testar primeira criação, reinicialização sem sobrescrita, configuração parcial e colisão com CLIENTE; documentar variáveis de configuração e obter token seguindo o exemplo documentado.

## 3. Catálogo

- [x] 3.1 Implementar criação e edição de produtos pelo operador com validação de nome, preço decimal e disponibilidade; testar 201/200, dados inválidos, ID inexistente e bloqueio do cliente.
- [x] 3.2 Implementar listagem autenticada com disponíveis para clientes e catálogo completo para operador; testar catálogo vazio, produto indisponível e diferença entre perfis.
- [x] 3.3 Documentar endpoints e exemplos de manutenção e consulta do catálogo; verificar exemplos pelos testes HTTP equivalentes.

## 4. Criação e consulta de pedidos

- [x] 4.1 Implementar criação transacional pelo cliente autenticado com captura de dados do catálogo, endereço textual e total em BigDecimal; testar preços históricos, cálculo 30,00 do cenário, estado RECEBIDO e vínculo ao proprietário.
- [x] 4.2 Validar itens, produtos, quantidades, endereço e campos proibidos; testar produtos repetidos, inexistentes e indisponíveis, dados forjados, perfil OPERADOR e rollback sem pedido parcial.
- [x] 4.3 Implementar listagem e consulta por ID com escopo por proprietário e consulta global do operador; testar dois clientes, lista vazia, IDs inexistentes e 404 sem vazamento para pedido de terceiro.
- [x] 4.4 Documentar contratos de criação e consulta com exemplos de token, itens e valores retornados; verificar exemplos com cenários HTTP equivalentes.

## 5. Status e cancelamento

- [x] 5.1 Implementar avanço sequencial exclusivo do operador e rejeição de transições inválidas; testar matriz completa de estados, status desconhecido, repetição, regressão, saltos e 403 para cliente.
- [x] 5.2 Implementar cancelamento pelo cliente somente em RECEBIDO e pelo operador em qualquer estado ativo, com CANCELADO também aceito no endpoint de status; testar ambos os endpoints, propriedade, estados finais e 404 para pedido de terceiro.
- [x] 5.3 Implementar controle de concorrência em alterações de pedido e coordenação transacional da captura de produtos; testar disputa entre preparo e cancelamento com um vencedor e 409 para o concorrente, além de edição de catálogo durante criação sem dados misturados.
- [x] 5.4 Documentar fluxo, cancelamentos e códigos de erro; verificar correspondência entre documentação, matriz de transições e testes de integração.

## 6. Verificação integrada

- [ ] 6.1 Executar cenário completo com operador e dois clientes: catálogo, compra, consulta isolada, preparo, entrega e cancelamentos; verificar respostas HTTP e dados persistidos através da cadeia real de autenticação.
- [ ] 6.2 Testar encerramento e reabertura do contexto com o mesmo H2 temporário em arquivo; verificar login, catálogo, histórico de preços, vínculos e status preservados.
- [ ] 6.3 Executar `./mvnw verify` e conferir README contra os fluxos integrados; concluir somente com suíte aprovada e sem tarefas de implementação pendentes.
