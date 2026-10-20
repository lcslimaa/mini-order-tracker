# Mini Order Tracker

API REST simplificada para rastreamento de pedidos de delivery.

## Status

Planejamento OpenSpec concluído e validado. A implementação da API ainda não começou; não há aplicação executável neste repositório.

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

Para iniciar a implementação no Codex, use `$openspec-apply-change delivery-order-tracking`.
