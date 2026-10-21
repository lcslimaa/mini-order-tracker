# Proposal

## Why

O backend de delivery já permite autenticar usuários, manter produtos e acompanhar pedidos, mas ainda não existe uma interface para essas operações. Uma aplicação React permitirá que clientes façam pedidos e operadores gerenciem o atendimento e o catálogo.

## What Changes

- Criar frontend em React, TypeScript e Vite dentro de `react-app/`, organizado por funcionalidade.
- Oferecer cadastro e login de clientes e login de operadores, com navegação conforme o perfil autenticado.
- Permitir ao cliente consultar seus pedidos, criar pedidos a partir do catálogo e cancelar pedidos em RECEBIDO.
- Permitir ao operador consultar todos os pedidos, avançar etapas, cancelar pedidos ativos e cadastrar ou editar produtos e sua disponibilidade.
- Adicionar `GET /api/auth/me` para consultar a identidade e o perfil do usuário autenticado.
- Configurar integração local entre frontend na porta 3000 e Java na porta 8080, incluindo proxy do Vite e CORS nos controllers.

## Capabilities

### New Capabilities

- `customer-portal`: experiência web do cliente para cadastro, acesso, catálogo e pedidos.
- `operator-portal`: experiência web do operador para atendimento de pedidos e manutenção do catálogo.

### Modified Capabilities

- `user-auth`: consulta autenticada da própria identidade e perfil por `/api/auth/me`.

## Impact

- Novo projeto e dependências de frontend em `react-app/`.
- Alterações pontuais no controller de autenticação e nos controllers que recebem CORS.
- Consumo dos contratos existentes de catálogo e pedidos, sem modificar regras de preços, propriedade ou transições.
- Novas verificações da identidade autenticada, integração HTTP e fluxos das duas áreas.
- Fora do escopo: cadastro público de operadores, exclusão física de produtos, pagamentos, notificações push e mudanças nas regras de negócio existentes.
