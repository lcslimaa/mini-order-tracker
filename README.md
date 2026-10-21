# Mini Order Tracker

Sistema de rastreamento de pedidos de delivery com API REST em Java 21 e Spring Boot 4.1.1 e interface **Mesa**, desenvolvida em React e TypeScript.

Clientes se cadastram, criam pedidos e acompanham as entregas. Operadores administram o catálogo e atualizam os pedidos. O backend usa Spring Security, JWT, BCrypt e banco H2 em arquivo com migrações Flyway.

## Pré-requisitos

- **JDK 21**, com `JAVA_HOME` apontando para essa instalação.
- **Node.js 24.15.0 ou superior da série 24**, com npm, compatível com as dependências do lockfile.
- **Git**, para clonar o repositório, e acesso à internet no primeiro uso para baixar dependências.
- Portas **8080** (backend) e **3000** (frontend) livres.

Não é necessário instalar Maven ou um servidor de banco: o projeto inclui Maven Wrapper e H2. OpenSSL pode ser usado para gerar o segredo JWT.

Depois de clonar o repositório, abra um terminal na pasta `mini-order-tracker`. Confira as ferramentas:

```sh
java -version
node --version
npm --version
```

Os comandos deste guia usam macOS/Linux ou Git Bash/WSL. No PowerShell, execute `Copy-Item .env.example .env` para copiar a configuração e `./mvnw.cmd` no lugar de `./mvnw`.

## Configuração inicial

Na raiz do projeto, copie o arquivo de exemplo:

```sh
cp .env.example .env
```

Gere um segredo e copie o resultado para `JWT_SECRET`:

```sh
openssl rand -hex 32
```

Edite o `.env`, preenchendo o segredo e a senha local do operador. Mantenha ou personalize o nome e o email:

```properties
JWT_SECRET=cole-aqui-o-segredo-gerado
JWT_TTL_SECONDS=3600
OPERATOR_NAME=Operador
OPERATOR_EMAIL=operador@example.com
OPERATOR_PASSWORD=defina-sua-senha-local
```

Esses valores são exemplos: substitua o segredo e a senha antes de iniciar. O segredo deve ter ao menos 32 bytes e a senha deve ser não vazia, com no máximo 72 bytes UTF-8.

A configuração do projeto faz o Spring Boot carregar o `.env` da pasta de trabalho automaticamente. Use formato Java properties (`CHAVE=valor`), sem `export` e sem aspas delimitadoras. Não é necessário executar `source` nem instalar plugin no IntelliJ. O arquivo contém credenciais locais e é ignorado pelo Git.

O operador é criado no primeiro início. Os três campos `OPERATOR_*` devem estar preenchidos para provisioná-lo. É possível executar apenas com clientes deixando os três vazios; configuração parcial é rejeitada. Alterar `OPERATOR_PASSWORD` depois não muda a senha de uma conta já persistida.

## Executar o projeto

### 1. Iniciar o backend

No primeiro terminal, na raiz:

```sh
./mvnw spring-boot:run
```

O wrapper baixa o Maven e as dependências no primeiro uso. Aguarde a inicialização do servidor na porta **8080**. A API fica em `http://localhost:8080/api`; a interface é servida separadamente pelo frontend.

No macOS, se o Java padrão não for o 21, ajuste antes de executar:

```sh
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

Para executar pelo IntelliJ, selecione JDK 21, defina **Working directory** como `$PROJECT_DIR$` e execute `MiniOrderTrackerApplication`.

### 2. Iniciar o frontend

Em outro terminal, também partindo da raiz:

```sh
cd react-app
npm ci
npm run dev
```

Acesse **http://localhost:3000**. Mantenha os dois terminais em execução. O Vite encaminha as chamadas `/api` para `http://localhost:8080`; não é necessário criar `.env` no frontend.

Use `Ctrl+C` em cada terminal para encerrar os servidores. Nas próximas execuções, basta iniciar backend e frontend; repita `npm ci` quando as dependências mudarem.

## Primeiro uso

1. Entre na interface com o email e a senha do operador definidos no `.env`.
2. Em **Produtos**, cadastre pelo menos um produto e mantenha-o disponível.
3. Clique em **Sair**, abra o cadastro e crie uma conta de cliente. O cadastro público sempre cria clientes.
4. Entre como cliente, abra **Novo pedido**, informe as quantidades e o endereço e confirme.
5. Consulte **Meus pedidos**. Para atualizar o status, saia e entre novamente como operador.

O total é calculado no servidor, sem taxa de entrega. Nomes e preços dos itens são preservados no pedido mesmo que o catálogo mude depois. O cliente cancela apenas em `RECEBIDO`; o operador pode cancelar em qualquer estado ativo.

**A sessão fica somente na memória do navegador: recarregar a página exige novo login.** Sair, expirar o token ou receber 401 também encerra a sessão. As listas de pedidos são atualizadas a cada 15 segundos enquanto a aba está visível e permitem atualização manual.

## Testes e build

Na raiz, execute os testes e verificações do backend:

```sh
./mvnw verify
```

Os testes usam configuração própria e bancos isolados, incluindo H2 em memória e arquivos temporários para verificar persistência.

Para o frontend, a partir da raiz:

```sh
cd react-app
npm ci
npm test
npm run typecheck
npm run build
```

O build gera `react-app/dist/`. Os detalhes de implantação e dos testes E2E com Playwright estão no [README do frontend](react-app/README.md). Os registros de verificação estão em [VERIFICATION.md](react-app/VERIFICATION.md).

## Configurações adicionais

| Variável | Uso | Padrão |
|---|---|---|
| `JWT_SECRET` | Segredo da assinatura JWT, ao menos 32 bytes | Obrigatório |
| `JWT_TTL_SECONDS` | Validade do token em segundos, positiva | `3600` |
| `OPERATOR_NAME`, `OPERATOR_EMAIL`, `OPERATOR_PASSWORD` | Provisionamento inicial do operador | Sem provisionamento se os três estiverem vazios |
| `SERVER_PORT` | Porta HTTP do backend | `8080` |
| `DATABASE_URL` | URL JDBC do banco H2 | `jdbc:h2:file:./data/tracker` |
| `DATABASE_USERNAME` | Usuário do banco | `sa` |
| `DATABASE_PASSWORD` | Senha do banco | Vazia |
| `H2_CONSOLE_ENABLED` | Habilita o console local H2 | `false` |

O banco em arquivo mantém os dados entre reinicializações na pasta `data/`, relativa à pasta de execução. As migrações Flyway são aplicadas na inicialização. Credenciais e arquivos de banco não devem ser versionados.

Variáveis de ambiente do sistema e argumentos de linha de comando têm prioridade sobre o `.env`. O arquivo é opcional quando a configuração é fornecida externamente. Mantenha o mesmo segredo JWT entre reinicializações para preservar a validade dos tokens emitidos.

Se mudar `SERVER_PORT`, ajuste também o destino do proxy em `react-app/vite.config.ts` e as URLs dos exemplos da API. O Vite exige a porta 3000 livre; a origem CORS de desenvolvimento está configurada no backend como `http://localhost:3000`.

### Solução de problemas

| Sintoma | Verificação ou ação |
|---|---|
| Java incompatível ou erro de compilação | Confira `java -version`, `JAVA_HOME` e o JDK do IntelliJ; use Java 21. |
| Erro de versão do Node ao instalar ou testar | Use Node 24.15.0 ou superior da série 24 e execute novamente `npm ci`. |
| Falha por `JWT_SECRET` | Preencha um segredo com ao menos 32 bytes e execute a partir da raiz, onde está o `.env`. |
| Falha no provisionamento do operador | Preencha os três campos `OPERATOR_*` ou deixe os três vazios. |
| Senha do operador alterada no `.env` não funciona | Use a senha originalmente persistida; o provisionamento não redefine senhas existentes. |
| Porta 8080 ou 3000 ocupada | Encerre o processo que usa a porta; se alterar a do backend, ajuste o proxy do Vite. |
| Interface abre, mas chamadas à API falham | Confira se o backend iniciou na porta esperada e se o proxy aponta para ela. Use `http://localhost:3000`. |
| Catálogo vazio para o cliente | Entre como operador e cadastre produtos disponíveis. |
| Login solicitado após recarregar | Comportamento previsto: a sessão é mantida somente em memória. |
| Página raiz do backend não abre a interface | Acesse o Vite em `http://localhost:3000`; o Java atende a API. |

## Console H2 local

Defina `H2_CONSOLE_ENABLED=true` no `.env` e reinicie a aplicação. Acesse `http://localhost:8080/h2-console/` enquanto a aplicação estiver rodando. Se alterou `SERVER_PORT`, use essa porta.

- Driver: `org.h2.Driver`.
- JDBC URL: `jdbc:h2:file:./data/tracker`, ou o valor de `DATABASE_URL` no `.env`.
- User Name: `sa`, ou o valor de `DATABASE_USERNAME`.
- Password: vazia por padrão, ou o valor de `DATABASE_PASSWORD`.

Clique em **Connect**. O console usa as credenciais do banco, não as do operador, e aceita apenas conexões locais. A flag é desabilitada por padrão e no `.env.example`; mantenha-a desabilitada fora do desenvolvimento local. A liberação das páginas e frames do console não altera a autenticação JWT da API.

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

Os comandos a seguir usam um terminal com shell compatível com sh. No Windows, use Git Bash ou WSL.

```sh
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"name":"Ana","email":"ana@example.com","password":"senha-local"}'
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"ana@example.com","password":"senha-local"}'
```

Para obter o token do operador, faça outro login com o email e a senha definidos no `.env` (substitua os valores abaixo):

```sh
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"operador@example.com","password":"sua-senha-local"}'
```

Copie o `accessToken` de cada resposta e defina as variáveis no mesmo terminal em que executará os exemplos:

```sh
CLIENT_TOKEN='cole-o-accessToken-do-cliente'
OPERATOR_TOKEN='cole-o-accessToken-do-operador'
```

Login retorna `accessToken`, `tokenType: "Bearer"` e `expiresIn`. Use `Authorization: Bearer <accessToken>` em todas as rotas de catálogo e pedidos. Para login do operador, use seu email e senha de provisionamento. Não há refresh token nem revogação por logout neste MVP.

Erros retornam JSON com `status`, `code` e `message`. Autenticação ausente ou inválida retorna 401; perfil sem permissão retorna 403; entrada inválida retorna 400 e email duplicado retorna 409.

`GET /api/auth/me` exige `Authorization: Bearer <token>` e retorna somente
`id`, `name`, `email` e `role` do usuário autenticado. Não aceita identidade ou
perfil escolhido pelo navegador. Tokens inválidos ou expirados recebem 401.

Os controllers permitem CORS da origem de desenvolvimento `http://localhost:3000`,
incluindo preflight com `Authorization` e `Content-Type`. As regras de autorização
continuam aplicadas às requisições reais; CORS não concede permissões de operador.

## Catálogo

`GET /api/products` retorna um array ordenado por ID: clientes veem somente produtos disponíveis, operadores veem todos. Cada produto tem `id`, `name`, `price` e `available`.
Somente operadores usam `POST /api/products` e `PUT /api/products/{id}`. O preço deve ser positivo, com no máximo duas casas decimais. Não há exclusão física; use `available: false`.

```sh
# Use OPERATOR_TOKEN definido após o login.
curl -X POST http://localhost:8080/api/products \
  -H "Authorization: Bearer $OPERATOR_TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"Pizza","price":12.50}'
curl -X POST http://localhost:8080/api/products \
  -H "Authorization: Bearer $OPERATOR_TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"Suco","price":5.00}'
# Substitua 1 pelo ID retornado ao criar a pizza.
curl -X PUT http://localhost:8080/api/products/1 \
  -H "Authorization: Bearer $OPERATOR_TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"Pizza","price":12.50,"available":true}'
curl http://localhost:8080/api/products -H "Authorization: Bearer $OPERATOR_TOKEN"
```

Os próximos exemplos usam os IDs 1 e 2 para os produtos e 1 para o pedido. Substitua-os pelos IDs reais retornados pela API se o banco já tiver dados.

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
```

Como alternativa ao avanço, o cliente pode cancelar um pedido que ainda esteja em `RECEBIDO`. Não execute o exemplo abaixo no mesmo pedido após colocá-lo em `EM_PREPARO`, pois receberá 409:

```sh
curl -X POST http://localhost:8080/api/orders/1/cancel \
  -H "Authorization: Bearer $CLIENT_TOKEN"
```

O operador também pode enviar `{"status":"CANCELADO"}` ao endpoint de status, com as mesmas regras. Alterações válidas retornam 200 com o pedido atualizado; saltos, regressões, repetições, cancelamento fora do estado permitido ou alterações após finalização retornam 409. Status desconhecido retorna 400. Disputas concorrentes preservam uma alteração e retornam 409 para a outra; consulte novamente antes de tentar outra ação.

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

O planejamento da interface React está em [`openspec/changes/archive/2026-10-20-react-order-tracker/`](openspec/changes/archive/2026-10-20-react-order-tracker/), com as 21 tarefas concluídas.

As especificações consolidadas estão em [`openspec/specs/`](openspec/specs/). O CLI OpenSpec é necessário apenas para validar e manter as especificações; não é requisito para executar a aplicação.
