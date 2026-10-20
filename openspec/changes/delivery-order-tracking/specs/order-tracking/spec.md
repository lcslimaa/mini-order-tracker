# Spec Delta

## Purpose

Permitir que clientes façam e acompanhem seus pedidos e que operadores conduzam o preparo, a entrega e os cancelamentos com acesso controlado.

## ADDED Requirements

### Requirement: Criação de pedido pelo cliente
O sistema SHALL permitir ao CLIENTE criar pedido para si, com endereço de entrega não vazio e ao menos um item com produto disponível e quantidade inteira positiva. SHALL obter o cliente da autenticação e preços do catálogo, recusando campos de proprietário, preço, total ou status enviados pelo cliente. O pedido SHALL iniciar em RECEBIDO e ser criado atomicamente.

#### Scenario: Pedido válido
- **WHEN** um CLIENTE envia endereço e itens válidos
- **THEN** recebe 201 com ID, seu vínculo como cliente, itens, total e status RECEBIDO

#### Scenario: Pedido inválido
- **WHEN** o endereço está vazio, a lista está vazia, há produto repetido ou quantidade não é um inteiro positivo
- **THEN** recebe 400 e nenhum pedido ou item é persistido

#### Scenario: Produto não elegível
- **WHEN** um item referencia produto inexistente ou indisponível
- **THEN** recebe 400 e nenhum pedido parcial é criado

#### Scenario: Tentativa de forjar dados
- **WHEN** o cliente envia proprietário, preço, total ou status na criação
- **THEN** recebe 400 e nenhum pedido é criado

#### Scenario: Operador tenta criar pedido
- **WHEN** um OPERADOR tenta criar pedido
- **THEN** recebe 403, pois a criação no MVP pertence ao perfil CLIENTE

### Requirement: Valores e histórico do pedido
O sistema SHALL registrar nome e preço unitário de cada produto no momento da compra, calcular subtotais e total com precisão decimal e retornar esses valores nas consultas. O total SHALL ser exclusivamente a soma de quantidade multiplicada pelo preço unitário de cada item, sem taxa de entrega. O endereço SHALL ser preservado com o pedido.

#### Scenario: Cálculo do total
- **WHEN** um pedido contém duas unidades de 12,50 e uma unidade de 5,00
- **THEN** o total persistido e retornado é 30,00

#### Scenario: Alteração posterior do catálogo
- **WHEN** nome ou preço de um produto é editado após a criação do pedido
- **THEN** as consultas do pedido mantêm nomes, preços, subtotais e total da compra

### Requirement: Consulta com isolamento entre clientes
O sistema SHALL listar e buscar por ID apenas pedidos do CLIENTE autenticado e permitir ao OPERADOR consultar todos. Consultas de cliente a pedido de outro cliente SHALL retornar 404, assim como um ID inexistente, sem expor dados do pedido ou seu proprietário.

#### Scenario: Listagem do cliente
- **WHEN** um CLIENTE lista pedidos
- **THEN** recebe 200 e somente seus pedidos, ou lista vazia quando não possui pedidos

#### Scenario: Pedido de terceiro
- **WHEN** um CLIENTE consulta um pedido de outro cliente
- **THEN** recebe 404 sem dados do pedido

#### Scenario: Consulta global do operador
- **WHEN** um OPERADOR lista pedidos ou consulta um ID existente
- **THEN** recebe pedidos independentemente do cliente proprietário

#### Scenario: Pedido inexistente
- **WHEN** um usuário autenticado consulta um ID inexistente
- **THEN** recebe 404

### Requirement: Avanço sequencial pelo operador
O sistema SHALL permitir exclusivamente ao OPERADOR avançar RECEBIDO para EM_PREPARO, EM_PREPARO para SAIU_PARA_ENTREGA e SAIU_PARA_ENTREGA para ENTREGUE. Saltos, regressões, repetição do status e alterações após ENTREGUE ou CANCELADO SHALL retornar 409. Status desconhecido SHALL retornar 400. Alterações concorrentes SHALL preservar uma transição válida.

#### Scenario: Avanço válido
- **WHEN** um OPERADOR solicita a próxima etapa de um pedido não finalizado
- **THEN** recebe 200 e o novo status é persistido

#### Scenario: Transição inválida
- **WHEN** um OPERADOR pula etapas, regride, repete status ou altera um estado final
- **THEN** recebe 409 e o status permanece inalterado

#### Scenario: Status desconhecido
- **WHEN** um OPERADOR envia um valor fora dos status definidos
- **THEN** recebe 400 sem alteração

#### Scenario: Cliente tenta avançar
- **WHEN** um CLIENTE tenta avançar o status de um pedido
- **THEN** recebe 403 sem alteração

#### Scenario: Atualizações concorrentes
- **WHEN** duas atualizações disputam o mesmo estado inicial
- **THEN** apenas uma é aplicada a esse estado e a outra recebe 409

### Requirement: Cancelamento conforme perfil e estado
O sistema SHALL permitir ao CLIENTE cancelar somente seu pedido em RECEBIDO e ao OPERADOR cancelar qualquer pedido em RECEBIDO, EM_PREPARO ou SAIU_PARA_ENTREGA. Cancelamento SHALL definir CANCELADO. Estado não elegível SHALL retornar 409. Tentativa de cliente cancelar pedido de terceiro SHALL retornar 404.

#### Scenario: Cliente cancela antes do preparo
- **WHEN** o proprietário cancela seu pedido em RECEBIDO
- **THEN** recebe 200 e o pedido passa a CANCELADO

#### Scenario: Cliente cancela após início do preparo
- **WHEN** o proprietário tenta cancelar em EM_PREPARO ou SAIU_PARA_ENTREGA
- **THEN** recebe 409 e o pedido permanece inalterado

#### Scenario: Operador cancela pedido ativo
- **WHEN** um OPERADOR cancela em RECEBIDO, EM_PREPARO ou SAIU_PARA_ENTREGA
- **THEN** recebe 200 e o pedido passa a CANCELADO

#### Scenario: Cancelamento de estado final
- **WHEN** cliente proprietário ou operador tenta cancelar um pedido ENTREGUE ou CANCELADO
- **THEN** recebe 409 sem alteração

#### Scenario: Cancelamento de terceiro
- **WHEN** um CLIENTE tenta cancelar pedido de outro cliente
- **THEN** recebe 404 sem alteração

### Requirement: Persistência entre reinicializações
O sistema SHALL preservar usuários, produtos, pedidos e seus itens após reinicializações usando armazenamento em arquivo. Uma operação rejeitada SHALL preservar o estado persistido anterior.

#### Scenario: Reabertura do armazenamento
- **WHEN** o sistema encerra e inicia novamente com o mesmo arquivo de dados
- **THEN** credenciais, catálogo, vínculos, itens, valores e status previamente confirmados continuam disponíveis
