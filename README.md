# Mini Order Tracker

API REST simplificada para rastreamento de pedidos de delivery.

## Status

API implementada em Java 21 e Spring Boot 4.1.1, com tarefas OpenSpec concluídas e verificação integrada de autenticação, catálogo, pedidos, concorrência e persistência. A integração à main depende da revisão dos PRs.

## Escopo

- Java e Spring Boot, Spring Security, JWT com expiração e BCrypt.
- Cadastro público de clientes e provisionamento inicial controlado do operador.
- Clientes criam e consultam os próprios pedidos; operadores consultam todos e gerenciam seu fluxo.
- Catálogo administrado pelo operador, com edição e disponibilidade de produtos.
- Pedidos preservam preços históricos e calculam o total no servidor, sem taxa de entrega.
- Fluxo: `RECEBIDO` → `EM_PREPARO` → `SAIU_PARA_ENTREGA` → `ENTREGUE`.
- Cliente cancela apenas antes do preparo; operador pode cancelar em qualquer estado ativo.
- Persistência H2 em arquivo e testes de integração de segurança e regras de negócio.

## Planejamento

A mudança está em [`openspec/changes/delivery-order-tracking/`](openspec/changes/delivery-order-tracking/):

- [Proposta](openspec/changes/delivery-order-tracking/proposal.md)
- [Design técnico](openspec/changes/delivery-order-tracking/design.md)
- [Autenticação](openspec/changes/delivery-order-tracking/specs/user-auth/spec.md)
- [Catálogo](openspec/changes/delivery-order-tracking/specs/product-catalog/spec.md)
- [Pedidos](openspec/changes/delivery-order-tracking/specs/order-tracking/spec.md)
- [Tarefas](openspec/changes/delivery-order-tracking/tasks.md)

Com o CLI OpenSpec instalado, valide o planejamento com:

```sh
openspec validate delivery-order-tracking --strict
```

A mudança permanece aberta até a revisão e o arquivamento pelo fluxo OpenSpec.

## Desenvolvimento local

Requer JDK 21. O Maven é baixado automaticamente pelo wrapper no primeiro uso.

```sh
# macOS, caso o Java padrão seja diferente
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./mvnw verify
```

Antes de iniciar com `./mvnw spring-boot:run`, configure o segredo JWT e, se necessário, o operador conforme a seção Autenticação.

O banco padrão é `jdbc:h2:file:./data/tracker`, com migrações Flyway versionadas.
`DATABASE_URL`, `DATABASE_USERNAME` e `DATABASE_PASSWORD` permitem configurar o armazenamento.
Os testes usam H2 isolado em memória e arquivos temporários para verificar reabertura.
O console web H2 está desabilitado. Credenciais e arquivos de banco não devem ser versionados.

## Autenticação

A aplicação exige `JWT_SECRET` externo com ao menos 32 bytes. O JWT expira em 3600 segundos por padrão (`JWT_TTL_SECONDS`).
Para provisionar o operador no primeiro início, defina as três variáveis `OPERATOR_NAME`, `OPERATOR_EMAIL` e `OPERATOR_PASSWORD`. A configuração é opcional, mas parcial é rejeitada. Se o operador já existe, reinicializações preservam sua senha; email pertencente a cliente não é promovido.

```sh
export JWT_SECRET="$(openssl rand -hex 32)"
export OPERATOR_NAME='Operador'
export OPERATOR_EMAIL='operador@example.com'
read -r -s OPERATOR_PASSWORD
export OPERATOR_PASSWORD
./mvnw spring-boot:run
```

Mantenha o mesmo segredo entre reinicializações para preservar a validade dos tokens emitidos.
A senha deve ser não vazia e conter no máximo 72 bytes UTF-8, sem truncamento; nunca é devolvida pela API. Login com senha acima desse limite retorna o mesmo 401 de credenciais inválidas.
O cadastro público sempre cria `CLIENTE` e rejeita campos de perfil. Email é normalizado para minúsculas, sem espaços externos.

```sh
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"name":"Ana","email":"ana@example.com","password":"senha-local"}'
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"ana@example.com","password":"senha-local"}'
```

Login retorna `accessToken`, `tokenType: "Bearer"` e `expiresIn`. Use `Authorization: Bearer <accessToken>` em todas as rotas de catálogo e pedidos. Para login do operador, use seu email e senha de provisionamento. Não há refresh token nem revogação por logout neste MVP.

Erros retornam JSON com `status`, `code` e `message`. Autenticação ausente ou inválida retorna 401; perfil sem permissão retorna 403; entrada inválida retorna 400 e email duplicado retorna 409.

## Catálogo

`GET /api/products` retorna um array ordenado por ID: clientes veem somente produtos disponíveis, operadores veem todos. Cada produto tem `id`, `name`, `price` e `available`.
Somente operadores usam `POST /api/products` e `PUT /api/products/{id}`. O preço deve ser positivo, com no máximo duas casas decimais. Não há exclusão física; use `available: false`.

```sh
# Defina OPERATOR_TOKEN com o accessToken obtido no login do operador.
curl -X POST http://localhost:8080/api/products \
  -H "Authorization: Bearer $OPERATOR_TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"Pizza","price":12.50}'
curl -X PUT http://localhost:8080/api/products/1 \
  -H "Authorization: Bearer $OPERATOR_TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"Pizza","price":15.00,"available":false}'
curl http://localhost:8080/api/products -H "Authorization: Bearer $OPERATOR_TOKEN"
```

A criação retorna 201 e assume `available: true` quando omitido. Edição exige os três campos e retorna 200; produto inexistente retorna 404. Dados inválidos retornam 400 sem alteração persistida.

## Pedidos

Somente clientes criam pedidos em `POST /api/orders`. Envie endereço textual (até 1000 caracteres) e itens com `productId` e `quantity` inteira positiva. Produtos precisam existir e estar disponíveis; IDs repetidos são rejeitados. O cliente vem do token e os valores vêm do catálogo: campos de proprietário, status, preço ou total são rejeitados.

```sh
# Defina CLIENT_TOKEN com o accessToken do cliente e use IDs reais do catálogo.
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $CLIENT_TOKEN" -H 'Content-Type: application/json' \
  -d '{"address":"Rua A, 10","items":[{"productId":1,"quantity":2},{"productId":2,"quantity":1}]}'
curl http://localhost:8080/api/orders -H "Authorization: Bearer $CLIENT_TOKEN"
curl http://localhost:8080/api/orders/1 -H "Authorization: Bearer $CLIENT_TOKEN"
```

Criação retorna 201 com `id`, `customer` (ID, nome e email), `address`, `items`, `total`, `status` e `createdAt`. Cada item contém `productId`, `name`, `unitPrice`, `quantity` e `subtotal`. Duas unidades de 12,50 e uma de 5,00 totalizam 30,00. Não há taxa de entrega.

`GET /api/orders` retorna array ordenado por ID, sem paginação. Clientes veem somente seus pedidos; operadores veem todos. `GET /api/orders/{id}` retorna 404 para ID inexistente ou pedido de outro cliente. Os nomes e preços dos itens e o endereço são preservados no pedido, independentemente de alterações posteriores no catálogo. Dados inválidos retornam 400 sem criar pedido parcial.

## Status e cancelamento

O pedido começa em `RECEBIDO`. Somente o operador avança uma etapa por vez:

```text
RECEBIDO --> EM_PREPARO --> SAIU_PARA_ENTREGA --> ENTREGUE
```

O cliente pode cancelar o próprio pedido somente em `RECEBIDO`. O operador pode cancelar em `RECEBIDO`, `EM_PREPARO` e `SAIU_PARA_ENTREGA`. `ENTREGUE` e `CANCELADO` são finais.

```sh
curl -X PATCH http://localhost:8080/api/orders/1/status \
  -H "Authorization: Bearer $OPERATOR_TOKEN" -H 'Content-Type: application/json' \
  -d '{"status":"EM_PREPARO"}'
curl -X POST http://localhost:8080/api/orders/1/cancel \
  -H "Authorization: Bearer $CLIENT_TOKEN"
```

O operador também pode enviar `{"status":"CANCELADO"}` ao endpoint de status, com as mesmas regras. Alterações válidas retornam 200 com o pedido atualizado; saltos, regressões, repetições, cancelamento fora do estado permitido ou alterações após finalização retornam 409. Status desconhecido retorna 400. Disputas concorrentes preservam uma alteração e retornam 409 para a outra; consulte novamente antes de tentar outra ação.
