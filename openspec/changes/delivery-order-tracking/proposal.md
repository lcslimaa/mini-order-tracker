# Proposal

## Why

Construir um MVP de rastreamento de pedidos de delivery com catálogo e acesso autenticado. Clientes precisam acompanhar apenas seus pedidos, enquanto o operador administra produtos e o fluxo de todos os pedidos.

## What Changes

- Cadastro público de clientes e login com email e senha, BCrypt e JWT com expiração.
- Provisionamento inicial controlado do operador, sem escolha de perfil no cadastro público.
- Catálogo com consulta autenticada e criação, edição e disponibilidade administradas pelo operador.
- Pedidos com itens do catálogo, endereço, preços históricos e total calculado no servidor, sem taxa de entrega.
- Consulta restrita ao proprietário para clientes e global para operadores; avanço sequencial de status e cancelamento conforme perfil.
- Persistência H2 em arquivo e testes de integração de segurança, regras de negócio e persistência.
- Fora do escopo: interface gráfica, pagamentos, estoque, geolocalização, notificações, refresh tokens e cadastro público de operadores.

## Capabilities

### New Capabilities

- `user-auth`: cadastro, login, autenticação JWT, perfis e provisionamento do operador.
- `product-catalog`: consulta de produtos e manutenção do catálogo pelo operador.
- `order-tracking`: criação, isolamento por cliente, cálculo, consulta e ciclo de vida dos pedidos.

### Modified Capabilities

Nenhuma; o projeto ainda não possui especificações.

## Impact

Nova API REST Java/Spring Boot com Spring Security, validação, persistência relacional em H2 em arquivo e testes de integração. Não há código ou consumidores existentes a migrar. Os contratos de autenticação, catálogo e pedidos serão novos.
