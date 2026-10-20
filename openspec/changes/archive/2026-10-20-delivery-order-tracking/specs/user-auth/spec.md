# Spec Delta

## Purpose

Autenticar clientes e operadores e proteger o acesso à API de delivery conforme a identidade e o perfil do usuário.

## ADDED Requirements

### Requirement: Cadastro público de clientes
O sistema SHALL cadastrar nome, email e senha e atribuir exclusivamente o perfil CLIENTE. SHALL validar campos obrigatórios e formato do email, normalizar emails com remoção de espaços externos e conversão para minúsculas e impedir duplicação. SHALL armazenar somente hashes de senha e nunca devolver senhas ou hashes na API.

#### Scenario: Cadastro válido
- **WHEN** uma pessoa envia nome, email válido e senha não vazios
- **THEN** recebe 201 e um usuário CLIENTE, sem senha nem hash na resposta

#### Scenario: Email duplicado
- **WHEN** alguém cadastra um email já existente, inclusive com diferenças de caixa ou espaços externos
- **THEN** recebe 409 e nenhum segundo usuário é criado

#### Scenario: Cadastro inválido ou privilegiado
- **WHEN** uma requisição contém campos inválidos ou tenta definir o perfil OPERADOR
- **THEN** recebe 400 e nenhum usuário é criado

### Requirement: Login e validade do token
O sistema SHALL autenticar email e senha e emitir um JWT assinado com expiração. Credenciais inválidas SHALL retornar 401. O token SHALL identificar o usuário; tokens expirados, adulterados ou com assinatura inválida SHALL ser recusados.

#### Scenario: Login válido
- **WHEN** um usuário informa credenciais corretas
- **THEN** recebe 200, accessToken, tokenType Bearer e expiresIn em segundos

#### Scenario: Login inválido
- **WHEN** o email não existe ou a senha está incorreta
- **THEN** recebe 401 com a mesma mensagem genérica

#### Scenario: Token inválido
- **WHEN** uma rota protegida recebe token expirado, adulterado ou com assinatura inválida
- **THEN** retorna 401 sem executar a operação

### Requirement: Acesso autenticado e autorização por perfil
O sistema SHALL exigir autenticação válida em todas as rotas de catálogo e pedidos. Ausência de autenticação SHALL retornar 401; operação proibida pelo perfil SHALL retornar 403. Apenas cadastro e login SHALL ser públicos entre os endpoints funcionais do MVP.

#### Scenario: Acesso anônimo bloqueado
- **WHEN** uma requisição sem token tenta consultar catálogo ou criar, listar, consultar ou alterar pedidos
- **THEN** recebe 401 e nenhum dado é exposto ou alterado

#### Scenario: Operação exclusiva do operador
- **WHEN** um cliente autenticado tenta manter o catálogo ou avançar o status de um pedido
- **THEN** recebe 403 e nenhum dado é alterado

### Requirement: Operador inicial controlado
O sistema SHALL permitir provisionar um operador inicial por configuração externa, sem cadastro público de operadores e sem credenciais padrão embutidas. Reinicializações SHALL preservar o operador existente sem duplicá-lo ou sobrescrever sua senha. Colisão com email de CLIENTE SHALL impedir promoção automática e indicar falha de provisionamento.

#### Scenario: Primeiro provisionamento
- **WHEN** a configuração inicial válida é fornecida e o email ainda não existe
- **THEN** um OPERADOR é criado com senha armazenada como hash

#### Scenario: Reinicialização
- **WHEN** o sistema reinicia com um operador já provisionado
- **THEN** o usuário e sua senha persistidos são preservados

#### Scenario: Colisão de perfil
- **WHEN** o email configurado para operador pertence a um CLIENTE
- **THEN** o provisionamento falha sem alterar o perfil desse cliente
