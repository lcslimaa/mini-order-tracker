# Tasks

## 1. Identidade e integração do backend

- [x] 1.1 Implementar GET /api/auth/me com identidade do principal e DTO público; verificar testes para CLIENTE, OPERADOR, anonimato, token inválido/expirado e ausência de senha ou hash.
- [x] 1.2 Adicionar CrossOrigin restrito a http://localhost:3000 nos três controllers e integrar CORS à segurança; verificar preflight com Authorization e métodos usados sem liberar operações protegidas.
- [x] 1.3 Documentar /me e configuração CORS no README e executar a suíte Maven existente e os novos testes com sucesso.

## 2. Base do frontend

- [x] 2.1 Inicializar React, TypeScript e Vite em react-app com a organização app/features/shared/styles; verificar instalação e build de produção.
- [x] 2.2 Configurar porta 3000 estrita, proxy /api para localhost:8080 e exemplo de configuração; verificar uma consulta pelo proxy e documentar execução conjunta.
- [x] 2.3 Criar cliente fetch e contratos tipados para autenticação, produtos e pedidos; verificar envio do Bearer, tratamento de erros e ausência de repetição automática de mutações com testes HTTP simulados.
- [x] 2.4 Configurar Vitest e Testing Library; verificar que um teste de comportamento da integração HTTP executa no comando de testes documentado.

## 3. Autenticação e navegação

- [x] 3.1 Implementar cadastro, login, consulta /me e sessão em memória; verificar sucesso, rejeição e falha de /me com testes de interface.
- [x] 3.2 Implementar guards, rotas e layouts de cliente e operador; verificar acesso direto com perfil incorreto, sessão desconhecida e redirecionamento após login.
- [x] 3.3 Implementar saída e limpeza no 401, descartando respostas pendentes de sessões anteriores; verificar com testes de expiração e troca de usuário e documentar novo login após recarga.

## 4. Pedidos do cliente

- [x] 4.1 Implementar listagem com itens, valores, datas, status e estados de carregamento/vazio/erro; verificar que dados exibidos correspondem à resposta autenticada.
- [x] 4.2 Implementar atualização manual e polling de 15 segundos apenas com tela visível, sem sobreposição e com limpeza; verificar alteração de status, falha, retorno à tela e desmontagem com testes.
- [x] 4.3 Implementar formulário de pedido com catálogo, endereço e quantidades; verificar validação, produto único por item, payload restrito, prevenção de envio simultâneo e total confirmado pela API.
- [x] 4.4 Implementar cancelamento em RECEBIDO e recuperação de conflito; verificar resposta aceita e 409 e documentar fluxo do cliente e resultado incerto em falhas de rede.

## 5. Operação de pedidos e produtos

- [x] 5.1 Implementar listagem global com cliente, endereço e atualização compartilhada; verificar dados globais para operador e estados de consulta com testes.
- [x] 5.2 Implementar avanço sequencial e cancelamento de pedidos ativos; verificar ações por status, bloqueio durante envio, estados finais e consulta após 409.
- [x] 5.3 Implementar listagem, cadastro e edição de produtos com disponibilidade; verificar preço/nome, erros, preservação do formulário e atualização após confirmação.
- [x] 5.4 Documentar uso da área do operador, catálogo vazio e provisionamento externo já existente; verificar o percurso descrito com um operador provisionado sem expor credenciais.

## 6. Integração completa

- [x] 6.1 Validar com Java real o percurso cadastro/login, produto criado pelo operador, pedido criado pelo cliente, avanço pelo operador e atualização para cliente; registrar resultados incluindo cancelamentos permitidos e rejeitados.
- [x] 6.2 Verificar navegação por teclado, labels, mensagens e layout em tela pequena nas duas áreas, além da execução por proxy e preflight direto de CORS.
- [x] 6.3 Executar suíte Maven, testes de frontend, checagem de tipos e build final; registrar comandos e resultados e confirmar documentação de execução e configuração de implantação.
