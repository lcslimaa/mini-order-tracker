# operator-portal Specification

## Purpose

Permitir que operadores autenticados atendam pedidos de todos os clientes e mantenham os produtos oferecidos pelo delivery pelo navegador.

## Requirements

### Requirement: Acesso exclusivo à área do operador
A interface SHALL liberar a área de operação apenas após confirmar perfil OPERADOR pela API. SHALL encaminhar clientes para sua própria área e usuários sem sessão ao login. Não SHALL oferecer cadastro público de operador.

#### Scenario: Operador autenticado
- **WHEN** o login é válido e a consulta da identidade retorna OPERADOR
- **THEN** a interface abre a área de operação

#### Scenario: Cliente acessa rota de operador
- **WHEN** um CLIENTE tenta abrir diretamente uma rota de operação
- **THEN** a interface o encaminha para sua área sem carregar dados exclusivos do operador

### Requirement: Acompanhamento global de pedidos
A interface SHALL listar todos os pedidos retornados pela API, exibindo identificador, cliente, endereço, itens, total, data e status em português. SHALL oferecer atualização manual e periódica e distinguir carregamento, lista vazia e falha de atualização.

#### Scenario: Consulta global
- **WHEN** o operador abre a lista e a API responde com sucesso
- **THEN** consegue consultar os dados dos pedidos de todos os clientes retornados

#### Scenario: Atualização ou falha
- **WHEN** uma consulta periódica recebe novos dados ou falha
- **THEN** a interface atualiza os pedidos ou indica que não conseguiu atualizá-los

### Requirement: Ações do operador sobre pedidos
A interface SHALL oferecer apenas o próximo avanço de RECEBIDO para EM_PREPARO, deste para SAIU_PARA_ENTREGA e deste para ENTREGUE. SHALL permitir cancelar nesses três estados ativos, impedir submissões simultâneas por pedido e usar a resposta da API como confirmação.

#### Scenario: Avanço ou cancelamento aceito
- **WHEN** a API confirma uma ação permitida
- **THEN** a interface apresenta o status retornado e ajusta as ações disponíveis

#### Scenario: Estado final
- **WHEN** um pedido está ENTREGUE ou CANCELADO
- **THEN** não são oferecidos avanço ou cancelamento

#### Scenario: Conflito de atualização
- **WHEN** uma ação recebe 409
- **THEN** a interface informa o conflito e busca o pedido atualizado

### Requirement: Manutenção de produtos
A interface SHALL listar produtos disponíveis e indisponíveis e permitir cadastro e edição de nome, preço e disponibilidade. SHALL validar nome não vazio e preço positivo com até duas casas decimais, informar erros da API e impedir envio simultâneo do mesmo formulário. Não SHALL oferecer exclusão física.

#### Scenario: Produto cadastrado
- **WHEN** o operador envia um produto válido e a API aceita
- **THEN** o produto aparece na listagem com os dados confirmados

#### Scenario: Produto desativado
- **WHEN** o operador altera a disponibilidade e a API confirma
- **THEN** a listagem mostra a nova disponibilidade e pedidos anteriores mantêm seu histórico

#### Scenario: Dados inválidos
- **WHEN** o formulário contém nome vazio ou preço inválido
- **THEN** a interface indica os campos a corrigir antes de enviar

#### Scenario: Rejeição da API
- **WHEN** o backend rejeita uma criação ou edição
- **THEN** a interface informa a falha e preserva o formulário sem apresentar a operação como concluída
