# Spec Delta

## Purpose

Permitir que clientes acessem o delivery pelo navegador, façam pedidos e acompanhem seus estados com dados fornecidos pela API.

## ADDED Requirements

### Requirement: Acesso e sessão do cliente
A interface SHALL oferecer cadastro com nome, email e senha, login e saída. SHALL consultar a identidade autenticada antes de liberar a área do cliente e encaminhar sessões inválidas ao login, sem exibir dados de uma sessão anterior.

#### Scenario: Cadastro e acesso
- **WHEN** uma pessoa conclui um cadastro válido e autentica suas credenciais
- **THEN** acessa a lista de seus pedidos como CLIENTE

#### Scenario: Credenciais rejeitadas
- **WHEN** o backend rejeita cadastro ou login
- **THEN** a interface informa o erro e não inicia uma sessão indevida

#### Scenario: Sessão encerrada ou expirada
- **WHEN** o cliente sai ou a API retorna 401 em uma operação autenticada
- **THEN** a interface remove a sessão e seus dados e apresenta o login

### Requirement: Consulta e atualização dos pedidos do cliente
A interface SHALL listar pedidos do cliente com identificador, data, itens, total e status em português. SHALL permitir atualização manual e consultar periodicamente os status enquanto a lista estiver aberta. SHALL distinguir carregamento, lista vazia e falha de consulta, sem apresentar dados antigos como atualizados.

#### Scenario: Lista carregada
- **WHEN** a consulta de pedidos é bem-sucedida
- **THEN** são exibidos somente os pedidos retornados para o cliente autenticado

#### Scenario: Status atualizado
- **WHEN** o backend altera um status e a próxima consulta é bem-sucedida
- **THEN** o novo status aparece sem recarregar a página

#### Scenario: Sem pedidos ou serviço indisponível
- **WHEN** a API retorna lista vazia ou falha
- **THEN** a interface apresenta respectivamente um estado vazio ou uma indicação de falha com opção de tentar novamente

### Requirement: Criação de pedido pelo navegador
A interface SHALL permitir selecionar produtos disponíveis, informar quantidades inteiras positivas e endereço não vazio. SHALL enviar apenas endereço e itens com productId e quantity, impedir submissões simultâneas e apresentar o pedido confirmado com valores retornados pelo backend.

#### Scenario: Pedido criado
- **WHEN** o cliente envia dados válidos e a API confirma a criação
- **THEN** o pedido aparece na lista com total oficial e status RECEBIDO

#### Scenario: Entrada inválida
- **WHEN** faltam endereço ou itens ou uma quantidade é inválida
- **THEN** a interface indica os campos a corrigir antes de enviar

#### Scenario: Produto indisponível ou falha no envio
- **WHEN** o backend rejeita a criação ou há falha de comunicação
- **THEN** a interface informa a falha sem confirmar criação e preserva os dados para revisão sem reenviar automaticamente

### Requirement: Cancelamento pelo cliente
A interface SHALL oferecer cancelamento somente para pedidos em RECEBIDO e aplicar o resultado confirmado pela API. Uma rejeição por mudança concorrente de estado SHALL informar o conflito e atualizar o pedido.

#### Scenario: Cancelamento confirmado
- **WHEN** o cliente cancela seu pedido em RECEBIDO e a API aceita
- **THEN** o status exibido passa a CANCELADO

#### Scenario: Preparo iniciado durante a operação
- **WHEN** a API retorna 409 ao cancelar
- **THEN** a interface informa que o pedido mudou e consulta seu estado atual
