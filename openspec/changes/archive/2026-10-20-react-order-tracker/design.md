# Design

## Context

Ver `proposal.md` para motivação e escopo. A pasta `react-app/` está vazia. O Java já oferece autenticação JWT, catálogo e pedidos com autorização por perfil. O login retorna token, tipo e validade, mas não o perfil; o token contém a identidade, e a segurança consulta o usuário no banco. O backend usa por padrão a porta 8080 e não possui configuração de CORS identificada.

Este design se aplica porque introduz um frontend, dependências e integração entre módulos, além de decisões sobre sessão e autorização.

## Goals / Non-Goals

**Goals:** concentrar contratos HTTP, compartilhar componentes de pedidos entre perfis e manter o backend como autoridade sobre identidade, valores e transições.

**Non-Goals:** replicar regras de negócio no cliente como mecanismo de segurança, introduzir estado global de catálogo/pedidos sem necessidade, modificar persistência ou exigir alterações de esquema do banco.

## Decisions

### Base e organização

Usar React, TypeScript e Vite, com React Router para rotas. TypeScript ajuda a representar DTOs e os cinco status existentes. Vite permite proxy explícito e evita depender do campo proxy específico do Create React App.

Estrutura planejada:

```text
react-app/
  public/
  src/
    app/
      App.tsx
      router.tsx
      providers.tsx
    features/
      auth/
        api.ts
        types.ts
        AuthProvider.tsx
        RequireAuth.tsx
        pages/
      orders/
        api.ts
        types.ts
        components/
        pages/
      products/
        api.ts
        types.ts
        components/
        pages/
    shared/
      api/client.ts
      components/
      layouts/
      utils/formatters.ts
    styles/globals.css
    main.tsx
  .env.example
  package.json
  tsconfig.json
  vite.config.ts
```

Páginas específicas para cliente e operador permanecem em `features/orders`, compartilhando apresentação e contratos. Evitar árvores completas separadas por perfil, que duplicariam DTOs e status. Criar pastas apenas quando tiverem conteúdo; testes ficam próximos dos módulos verificados.

### Sessão e perfil

Adicionar GET `/api/auth/me`, utilizando o principal autenticado e o usuário do banco para devolver o DTO público existente (id, name, email, role). A rota permanece protegida pela regra atual; não aceitar identidade ou perfil do navegador como autoridade.

Manter o token em memória no provider de autenticação como padrão inicial. Recarregar a página exige novo login, uma limitação deliberada para não persistir credenciais no navegador. Persistência entre recargas exigiria decisão futura sobre sessão. O login só libera a navegação após `/me` confirmar o perfil; falha nessa consulta não deixa uma sessão parcialmente autorizada. Sair ou receber 401 limpa token e dados associados. Guards melhoram navegação, mas a autorização continua no Java. Cadastro bem-sucedido encaminha ao login.

Alternativas: inferir perfil do token não atende ao contrato atual; login devolver perfil não elimina a utilidade de consultar a identidade vigente; armazenamento persistente de token aumenta exposição e não é necessário ao MVP.

### Integração HTTP local

Usar chamadas relativas `/api` por um cliente HTTP baseado em fetch, com serialização JSON, Bearer e erros centralizados. Configurar Vite com porta 3000 estrita e proxy `/api` para `http://localhost:8080`; não adicionar campo proxy ao package.json.

Aplicar `@CrossOrigin(origins = "http://localhost:3000")` em AuthController, OrderController e ProductController, conforme solicitado, e integrar CORS à cadeia Spring Security para permitir preflight antes da autenticação. A origem é específica de desenvolvimento, sem credenciais por cookie. Verificar preflight e cabeçalhos reais; a anotação isolada não é evidência suficiente de integração.

Proxy resolve o acesso local habitual. CORS também permite chamadas diretas da origem autorizada ao backend. Em produção, preferir mesma origem com reverse proxy e fallback das rotas SPA; não presumir que o servidor de desenvolvimento faz parte da implantação.

### Rotas e experiência

Rotas públicas `/login` e `/cadastro`. Rotas de cliente `/cliente/pedidos` e `/cliente/pedidos/novo`. Rotas de operador `/operador/pedidos` e `/operador/produtos`. A raiz e rotas desconhecidas encaminham conforme sessão e perfil. Layouts distintos compartilham componentes básicos, mensagens e saída.

Exibir os pedidos mais recentes primeiro no frontend, pois a API hoje retorna ID crescente. Mostrar valores em BRL e datas no fuso do navegador. Formularios com labels, mensagens acessíveis, foco e estados de carregamento; apresentação utilizável em telas pequenas.

### Dados e atualizações

Usar hooks locais para consultas e mutações e Context apenas para autenticação, sem Redux ou biblioteca de cache na primeira versão. Atualizar pedidos a cada 15 segundos enquanto a tela estiver visível, ao voltar à tela e por botão manual. Encerrar consultas ao desmontar ou sair, evitar sobreposição e descartar respostas de sessões anteriores. Mostrar falhas sem marcar os dados antigos como atuais.

Mutações são confirmadas pela resposta do servidor; não repetir automaticamente criação de pedidos. Após 409 em transição ou cancelamento, consultar novamente o pedido. Impedir envios duplicados enquanto pendentes. Na criação, agrupar cada produto em um único item e não enviar preço, total, cliente ou status. Estimativas de total são apenas informativas; a resposta do backend define o valor confirmado.

### Verificação

Cobrir `/me` com testes de integração do backend para cliente, operador e tokens ausentes/inválidos, além de verificar CORS/preflight sem enfraquecer autorização. No frontend, usar Vitest e Testing Library para fluxos com respostas HTTP simuladas: guards, expiração, criação, conflitos e edição de produto. Verificar os percursos principais com a API Java real e executar build/checagem de tipos.

## Risks / Trade-offs

- Token em memória exige login após recarga → documentar o comportamento e manter retorno ao login previsível.
- Polling apresenta atraso de até um intervalo → atualização manual e consulta após ações; push fica fora deste escopo.
- Mudanças simultâneas no catálogo ou pedido → tratar rejeições e consultar novamente, preservando formulários para revisão.
- Falha de rede após POST pode deixar resultado incerto → não reenviar automaticamente; orientar consulta da lista antes de nova tentativa.
- Instalação vazia pode não ter produtos → estado vazio e gestão do catálogo pelo operador; sem criar dados fictícios automaticamente.
- Configuração CORS e porta local são específicas do ambiente → documentar proxy e configuração de implantação, sem wildcard de origem.

## Migration Plan

Não há migração de dados. Implementar e verificar primeiro `/me` e CORS, depois disponibilizar frontend e documentação de execução. Validar com contas reais de cliente e operador provisionado externamente. Para rollback, retirar a publicação do frontend e reverter as adições de integração; pedidos e catálogo permanecem no banco existente.
