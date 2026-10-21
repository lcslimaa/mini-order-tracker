import { test, expect, type Page } from "@playwright/test";
const operatorEmail = process.env.E2E_OPERATOR_EMAIL;
const operatorPassword = process.env.E2E_OPERATOR_PASSWORD;
test.skip(
  !operatorEmail || !operatorPassword,
  "Configure E2E_OPERATOR_EMAIL e E2E_OPERATOR_PASSWORD para um backend de teste isolado.",
);
async function login(page: Page, email: string, password: string) {
  await page.goto("/login");
  await page.getByLabel("Email", { exact: true }).fill(email);
  await page.getByLabel("Senha", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Entrar", exact: true }).click();
}
test("customer and operator complete real delivery flows through Vite and Java", async ({
  browser,
}) => {
  const op = await browser.newPage();
  const customer = await browser.newPage();
  const stamp = Date.now();
  const email = `cliente-${stamp}@web.test`;
  const password = "test-only-password";
  const product = `Bowl fresco ${stamp}`;
  await login(op, operatorEmail!, operatorPassword!);
  await expect(
    op.getByRole("heading", { name: "Todos os pedidos" }),
  ).toBeVisible();
  await op.getByRole("link", { name: "Produtos", exact: true }).click();
  await op.getByRole("button", { name: "Novo produto", exact: true }).click();
  await op.getByLabel("Nome do produto").fill(product);
  await op.getByLabel("Preço (R$)").fill("12.50");
  await op.getByRole("button", { name: "Salvar produto" }).click();
  await expect(op.getByRole("status")).toContainText("Produto salvo");
  await op
    .getByRole("button", { name: `Editar ${product}`, exact: true })
    .click();
  await op.getByLabel("Disponível para pedidos").uncheck();
  await op.getByRole("button", { name: "Salvar produto" }).click();
  await expect(op.getByRole("row").filter({ hasText: product })).toContainText(
    "Indisponível",
  );
  await op
    .getByRole("button", { name: `Editar ${product}`, exact: true })
    .click();
  await op.getByLabel("Disponível para pedidos").check();
  await op.getByRole("button", { name: "Salvar produto" }).click();
  await expect(op.getByRole("row").filter({ hasText: product })).toContainText(
    "Disponível",
  );
  await customer.goto("/cadastro");
  await customer.getByLabel("Nome", { exact: true }).fill("Ana de teste");
  await customer.getByLabel("Email", { exact: true }).fill(email);
  await customer.getByLabel("Senha", { exact: true }).fill(password);
  await customer.getByRole("button", { name: "Criar conta" }).click();
  await expect(
    customer.getByRole("heading", { name: "Entre na sua conta" }),
  ).toBeVisible();
  await login(customer, email, password);
  await expect(
    customer.getByRole("heading", { name: "Meus pedidos" }),
  ).toBeVisible();
  async function createOrder() {
    await customer
      .getByRole("link", { name: "Novo pedido", exact: true })
      .click();
    await customer
      .getByLabel(`Quantidade de ${product}`, { exact: true })
      .fill("2");
    await customer
      .getByLabel("Endereço de entrega")
      .fill("Rua das Flores, 24 — Centro");
    await customer.getByRole("button", { name: "Confirmar pedido" }).click();
    const card = customer.locator("article").first();
    await expect(card).toContainText(product);
    await expect(card).toContainText("R$ 25,00");
    return Number(
      (await card.getByRole("heading").innerText()).match(/#(\d+)/)![1],
    );
  }
  const id = await createOrder();
  await op.getByRole("link", { name: "Pedidos", exact: true }).click();
  await op.getByRole("button", { name: "Atualizar", exact: true }).click();
  const opCard = op.locator("article").filter({
    has: op.getByRole("heading", { name: `Pedido #${id}`, exact: true }),
  });
  await opCard.getByRole("button", { name: "Iniciar preparo" }).click();
  await expect(opCard).toContainText("Em preparo");
  await customer
    .getByRole("button", { name: "Atualizar", exact: true })
    .click();
  await expect(customer.locator("article").first()).toContainText("Em preparo");
  await expect(
    customer.getByRole("button", { name: "Cancelar pedido" }),
  ).toHaveCount(0);
  const auth = await customer.request.post("/api/auth/login", {
    data: { email, password },
  });
  const token = (await auth.json()).accessToken;
  const refused = await customer.request.post(`/api/orders/${id}/cancel`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  expect(refused.status()).toBe(409);
  await opCard.getByRole("button", { name: "Enviar para entrega" }).click();
  await expect(opCard).toContainText("Saiu para entrega");
  await opCard.getByRole("button", { name: "Concluir entrega" }).click();
  await expect(opCard).toContainText("Entregue");
  await expect(opCard.getByRole("button")).toHaveCount(0);
  const cancelId = await createOrder();
  const own = customer.locator("article").filter({
    has: customer.getByRole("heading", {
      name: `Pedido #${cancelId}`,
      exact: true,
    }),
  });
  await own.getByRole("button", { name: "Cancelar pedido" }).click();
  await expect(own).toContainText("Cancelado");
  const operatorCancelId = await createOrder();
  await op.getByRole("button", { name: "Atualizar", exact: true }).click();
  const active = op.locator("article").filter({
    has: op.getByRole("heading", {
      name: `Pedido #${operatorCancelId}`,
      exact: true,
    }),
  });
  await active.getByRole("button", { name: "Iniciar preparo" }).click();
  await expect(active).toContainText("Em preparo");
  await active.getByRole("button", { name: "Cancelar pedido" }).click();
  await expect(active).toContainText("Cancelado");
  await customer
    .getByRole("button", { name: "Atualizar", exact: true })
    .click();
  await customer.screenshot({
    path: "/tmp/mesa-customer-desktop.png",
    fullPage: true,
  });
  await op.screenshot({
    path: "/tmp/mesa-operator-desktop.png",
    fullPage: true,
  });
  for (const page of [customer, op]) {
    await page.setViewportSize({ width: 390, height: 844 });
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= window.innerWidth,
      ),
    ).toBe(true);
    await expect(
      page.getByRole("navigation", { name: "Menu principal" }),
    ).toBeVisible();
  }
  await customer.screenshot({
    path: "/tmp/mesa-customer-mobile.png",
    fullPage: true,
  });
  await op.getByRole("link", { name: "Produtos", exact: true }).click();
  expect(
    await op.evaluate(() => document.documentElement.scrollWidth <= innerWidth),
  ).toBe(true);
  await op.screenshot({
    path: "/tmp/mesa-products-mobile.png",
    fullPage: true,
  });
  // A customer cannot open the operator area, even by changing the SPA URL.
  await customer.evaluate(() => {
    history.pushState({}, "", "/operador/produtos");
    dispatchEvent(new PopStateEvent("popstate"));
  });
  await expect(
    customer.getByRole("heading", { name: "Meus pedidos" }),
  ).toBeVisible();
  await expect(
    customer.getByRole("link", { name: "Produtos", exact: true }),
  ).toHaveCount(0);
  await customer.getByRole("button", { name: "Sair", exact: true }).click();
  await expect(
    customer.getByRole("heading", { name: "Entre na sua conta" }),
  ).toBeVisible();
  await customer.keyboard.press("Tab");
  await expect(customer.getByLabel("Email", { exact: true })).toBeFocused();
  await customer.screenshot({
    path: "/tmp/mesa-login-mobile.png",
    fullPage: true,
  });
  await customer.close();
  await op.close();
});
