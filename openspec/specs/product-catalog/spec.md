# product-catalog Specification

## Purpose

Disponibilizar um catálogo autenticado de produtos do delivery e permitir que operadores mantenham seus dados e disponibilidade.

## Requirements

### Requirement: Consulta do catálogo
O sistema SHALL permitir que clientes autenticados consultem produtos disponíveis e que operadores consultem também os indisponíveis. Cada produto SHALL expor identificador, nome, preço unitário e disponibilidade.

#### Scenario: Consulta pelo cliente
- **WHEN** um CLIENTE lista o catálogo
- **THEN** recebe 200 e apenas produtos disponíveis, ou lista vazia se não houver produtos disponíveis

#### Scenario: Consulta pelo operador
- **WHEN** um OPERADOR lista o catálogo
- **THEN** recebe 200 e todos os produtos com suas disponibilidades

### Requirement: Manutenção exclusiva pelo operador
O sistema SHALL permitir exclusivamente ao OPERADOR criar e editar produtos com nome não vazio, preço positivo com no máximo duas casas decimais e disponibilidade explícita. Produtos SHALL ser criados disponíveis por padrão. Identificador inexistente SHALL retornar 404 e dados inválidos SHALL retornar 400 sem alteração persistida.

#### Scenario: Criação de produto
- **WHEN** um OPERADOR cadastra nome e preço válidos
- **THEN** recebe 201 com identificador e produto disponível

#### Scenario: Edição de produto
- **WHEN** um OPERADOR altera nome, preço ou disponibilidade de um produto existente
- **THEN** recebe 200 com os dados atualizados

#### Scenario: Produto inválido
- **WHEN** um OPERADOR informa nome vazio, preço zero ou negativo ou mais de duas casas decimais
- **THEN** recebe 400 e nenhum dado do produto é alterado

#### Scenario: Produto inexistente
- **WHEN** um OPERADOR tenta editar um identificador inexistente
- **THEN** recebe 404

#### Scenario: Cliente tenta manter catálogo
- **WHEN** um CLIENTE tenta criar ou editar produto
- **THEN** recebe 403 e o catálogo permanece inalterado

### Requirement: Indisponibilidade preserva histórico
O sistema SHALL impedir a inclusão de produtos indisponíveis em novos pedidos e preservar produtos referenciados por pedidos anteriores. Edições do catálogo SHALL preservar os nomes e preços históricos dos itens desses pedidos. O MVP SHALL oferecer desativação em vez de exclusão física pública.

#### Scenario: Produto desativado
- **WHEN** um OPERADOR marca como indisponível um produto usado em pedido anterior
- **THEN** o produto deixa de ser elegível para novos pedidos e o pedido anterior mantém seus dados originais

#### Scenario: Preço alterado
- **WHEN** um OPERADOR altera o preço de um produto já comprado
- **THEN** pedidos anteriores mantêm o preço original e pedidos posteriores usam o novo preço
