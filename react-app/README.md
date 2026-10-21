# Mesa — interface do Mini Order Tracker

Requer Node compatível com as versões registradas em package-lock.json e o backend Java em execução.

```sh
npm ci
npm run dev
```

Abra http://localhost:3000. O Vite exige a porta 3000 livre e encaminha `/api`
para http://localhost:8080, preservando as rotas da API. Não existe campo `proxy`
no package.json: o proxy está em vite.config.ts. O arquivo .env.example explica a
configuração local. Não coloque credenciais no frontend.

```sh
npm test
npm run typecheck
npm run build
```

A sessão fica apenas em memória. Recarregar a página exige novo login. Sair ou
receber 401 encerra a sessão e limpa seus dados. Cadastro concluído leva ao login.
Os perfis e as permissões são determinados pelo backend, inclusive quando alguém
abre uma URL diretamente.

Para publicar, sirva `dist/` por HTTPS com fallback das rotas SPA para index.html e
reverse proxy de `/api` ao backend na mesma origem. O proxy do Vite é apenas para
desenvolvimento. CORS permite a origem local 3000 para chamadas diretas ao Java;
uma implantação com origem separada exige configuração explícita no backend.

## Usar a aplicação

Cliente: cadastre-se, entre e abra **Novo pedido**. Informe as quantidades e o
endereço para confirmar. O total final é calculado no Java. A lista permite
cancelar apenas pedidos recebidos. Se o envio falhar sem resposta, consulte a
lista antes de reenviar para evitar criar outro pedido. Em falhas com resultado
incerto (rede, timeout ou erro de servidor), o formulário bloqueia novo envio e
oferece um link para consultar os pedidos. A resposta de criação é mantida em
memória durante a sessão para exibir a confirmação mesmo se a listagem falhar.

Operador: use a conta provisionada pelas variáveis `OPERATOR_NAME`,
`OPERATOR_EMAIL` e `OPERATOR_PASSWORD` do backend (veja o README da raiz).
Em **Produtos**, cadastre nome e preço, edite e marque a disponibilidade. Um
catálogo vazio mostra instruções para cadastrar o primeiro produto. Em **Pedidos**,
inicie o preparo, envie para entrega e conclua na sequência; é possível cancelar
antes de um estado final. Produtos ficam indisponíveis em vez de serem excluídos.

As listas de pedidos consultam a API a cada 15 segundos enquanto a aba está
visível e também permitem atualização manual. Um conflito de estado informa o
usuário e busca os dados atuais. Falhas de atualização deixam claro que os dados
exibidos pertencem à última consulta.

## Verificação em navegador

O teste E2E cria produtos, clientes e pedidos: execute apenas com um backend de
teste isolado. Inicie esse backend com banco em memória e operador provisionado,
e mantenha o Vite na porta 3000. Depois:

```sh
npx playwright install chromium
# Defina E2E_OPERATOR_EMAIL e E2E_OPERATOR_PASSWORD no ambiente de teste.
npm run test:e2e
```

Sem essas duas variáveis, o teste é explicitamente ignorado. Use
`http://localhost:3000` para respeitar a origem CORS configurada; `127.0.0.1` é
outra origem. O teste verifica as duas áreas e salva capturas em `/tmp/mesa-*.png`.

Organização: `app/` compõe rotas e providers; `features/auth`, `features/orders`
e `features/products` agrupam contratos, API, componentes e páginas; `shared/`
contém cliente HTTP, consulta com atualização periódica, layouts e formatação.
