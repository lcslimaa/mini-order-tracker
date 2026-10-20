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

O planejamento está arquivado em [`openspec/changes/archive/2026-10-20-delivery-order-tracking/`](openspec/changes/archive/2026-10-20-delivery-order-tracking/):

- [Proposta](openspec/changes/archive/2026-10-20-delivery-order-tracking/proposal.md)
- [Design técnico](openspec/changes/archive/2026-10-20-delivery-order-tracking/design.md)
- [Autenticação](openspec/changes/archive/2026-10-20-delivery-order-tracking/specs/user-auth/spec.md)
- [Catálogo](openspec/changes/archive/2026-10-20-delivery-order-tracking/specs/product-catalog/spec.md)
- [Pedidos](openspec/changes/archive/2026-10-20-delivery-order-tracking/specs/order-tracking/spec.md)
- [Tarefas](openspec/changes/archive/2026-10-20-delivery-order-tracking/tasks.md)

Com o CLI OpenSpec instalado, valide o planejamento com:

```sh
openspec validate --specs --strict
```

As especificações consolidadas estão em `openspec/specs/`. O arquivamento do planejamento não realiza merge dos PRs.

## Organização do código Java

As classes em `src/main/java/com/delivery/tracker` estão organizadas por camadas:

| Pacote | Responsabilidade |
|---|---|
| `controller` | Endpoints REST e validação das requisições |
| `service` | Casos de uso, regras de negócio e transações |
| `repository` | Acesso aos dados com Spring Data JPA |
| `entity` | Entidades e relacionamentos persistidos |
| `enums` | Perfis de usuário e status de pedido |
| `dto.auth`, `dto.catalog`, `dto.order`, `dto.error` | Requests e responses em arquivos próprios |
| `config` | Configurações de segurança JWT e console H2 |
| `security` | Identidade do usuário autenticado |
| `bootstrap` | Provisionamento inicial do operador |
| `exception` | Exceções e tratamento padronizado de erros |
| `util` | Normalização compartilhada de email |

`MiniOrderTrackerApplication` permanece na raiz para a descoberta dos componentes Spring. Serviços recebem DTOs independentes dos controllers. As conversões de entidades para respostas ficam nos DTOs de resposta. Os testes de integração permanecem em `src/test/java`.

## Desenvolvimento local

Requer JDK 21. O Maven é baixado automaticamente pelo wrapper no primeiro uso.

```sh
# macOS, caso o Java padrão seja diferente
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./mvnw verify
```

O Spring Boot carrega automaticamente o arquivo `.env` na pasta de trabalho, sem plugin do IntelliJ e sem executar `source` ou `export`. Na primeira configuração de um clone:

```sh
cp .env.example .env
```

Preencha `JWT_SECRET` e `OPERATOR_PASSWORD` no `.env` antes de iniciar. Gere um segredo JWT com `openssl rand -hex 32`. O arquivo usa formato Java properties: escreva `CHAVE=valor`, sem `export` e sem aspas delimitadoras. O `.env` contém credenciais locais e é ignorado pelo Git; somente `.env.example` é versionado.

No IntelliJ, use JDK 21 e defina **Working directory** como `$PROJECT_DIR$` na configuração de execução de `MiniOrderTrackerApplication`. Depois, clique em Run. Pelo terminal, execute na raiz:

```sh
./mvnw spring-boot:run
```

O arquivo é opcional para ambientes que fornecem configuração externa. Variáveis de ambiente do sistema e argumentos de linha de comando têm prioridade sobre o `.env`. Os testes mantêm configurações próprias e banco isolado.

O banco padrão é `jdbc:h2:file:./data/tracker`, com migrações Flyway versionadas.
`DATABASE_URL`, `DATABASE_USERNAME` e `DATABASE_PASSWORD` permitem configurar o armazenamento.
Os testes usam H2 isolado em memória e arquivos temporários para verificar reabertura.
O console web H2 pode ser habilitado para desenvolvimento conforme a seção abaixo. Credenciais e arquivos de banco não devem ser versionados.

## Autenticação

A aplicação exige `JWT_SECRET` externo com ao menos 32 bytes. O JWT expira em 3600 segundos por padrão (`JWT_TTL_SECONDS`).
Para provisionar o operador no primeiro início, defina as três variáveis `OPERATOR_NAME`, `OPERATOR_EMAIL` e `OPERATOR_PASSWORD`. A configuração é opcional, mas parcial é rejeitada. Se o operador já existe, reinicializações preservam sua senha; email pertencente a cliente não é promovido.

Os nomes de configuração no `.env` são:

```properties
JWT_SECRET=preencha-com-segredo-aleatorio-de-pelo-menos-32-bytes
JWT_TTL_SECONDS=3600
OPERATOR_NAME=Operador
OPERATOR_EMAIL=operador@example.com
OPERATOR_PASSWORD=preencha-com-sua-senha-local
```

Use os valores definidos no seu arquivo para fazer login do operador. Se o banco já contém esse operador, mudar a senha no `.env` não altera a senha persistida.

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


## Console H2 local

Defina `H2_CONSOLE_ENABLED=true` no `.env` e reinicie a aplicação. Acesse `http://localhost:8080/h2-console/` enquanto a aplicação estiver rodando. Se alterou `SERVER_PORT`, use essa porta.

- Driver: `org.h2.Driver`.
- JDBC URL: `jdbc:h2:file:./data/tracker`, ou o valor de `DATABASE_URL` no `.env`.
- User Name: `sa`, ou o valor de `DATABASE_USERNAME`.
- Password: vazia por padrão, ou o valor de `DATABASE_PASSWORD`.

Clique em **Connect**. O console usa as credenciais do banco, não as do operador, e aceita apenas conexões locais. A flag é desabilitada por padrão e no `.env.example`; mantenha-a desabilitada fora do desenvolvimento local. A liberação das páginas e frames do console não altera a autenticação JWT da API.
