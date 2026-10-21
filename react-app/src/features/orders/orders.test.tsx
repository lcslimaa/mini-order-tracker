import { screen, waitFor } from "@testing-library/react";
import { it, expect } from "vitest";
import { mockApi, sampleOrder, sampleProduct } from "../../test/fixtures";
import { openApp } from "../../test/renderApp";
it("lists real order values and only allows customer cancellation before preparation", async () => {
  mockApi(() =>
    Response.json([
      sampleOrder,
      { ...sampleOrder, id: 13, status: "EM_PREPARO" },
    ]),
  );
  await openApp();
  expect(await screen.findByText("Pedido #12")).toBeInTheDocument();
  expect(screen.getAllByText(/Bowl da casa/)).toHaveLength(2);
  expect(screen.getAllByText("R$ 50,00").length).toBeGreaterThan(0);
  expect(screen.getByText("Em preparo")).toBeInTheDocument();
  expect(
    screen.getAllByRole("button", { name: "Cancelar pedido" }),
  ).toHaveLength(1);
});
it("distinguishes empty state and failed refresh, allowing recovery", async () => {
  let failed = false;
  mockApi(() =>
    failed
      ? Response.json({ message: "Serviço indisponível" }, { status: 503 })
      : Response.json([]),
  );
  const user = await openApp();
  expect(
    await screen.findByText("Seu primeiro pedido começa aqui"),
  ).toBeInTheDocument();
  failed = true;
  await user.click(screen.getByRole("button", { name: "Atualizar" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Serviço indisponível",
  );
  failed = false;
  await user.click(screen.getByRole("button", { name: "Atualizar" }));
  await waitFor(() =>
    expect(screen.queryByRole("alert")).not.toBeInTheDocument(),
  );
});
it("refreshes a changed order after cancellation conflict", async () => {
  let conflict = false;
  mockApi((path, init) => {
    if (path.endsWith("/cancel")) {
      conflict = true;
      return Response.json({ message: "Alterado" }, { status: 409 });
    }
    return Response.json(
      path === "/api/orders"
        ? [{ ...sampleOrder, status: conflict ? "EM_PREPARO" : "RECEBIDO" }]
        : { ...sampleOrder, status: "EM_PREPARO" },
    );
  });
  const user = await openApp();
  await screen.findByText("Pedido #12");
  await user.click(screen.getByRole("button", { name: "Cancelar pedido" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(/mudou/);
  expect(await screen.findByText("Em preparo")).toBeInTheDocument();
  expect(
    screen.queryByRole("button", { name: "Cancelar pedido" }),
  ).not.toBeInTheDocument();
});
it("creates a restricted order payload and displays the official total", async () => {
  let sent: unknown;
  mockApi((path, init) => {
    if (path === "/api/products") return Response.json([sampleProduct]);
    if (path === "/api/orders" && init.method === "POST") {
      sent = JSON.parse(init.body as string);
      return Response.json({ ...sampleOrder, total: 49.9 }, { status: 201 });
    }
    return Response.json(sent ? [{ ...sampleOrder, total: 49.9 }] : []);
  });
  const user = await openApp();
  await user.click(await screen.findByRole("link", { name: "Novo pedido" }));
  await user.type(
    await screen.findByLabelText("Quantidade de Bowl da casa"),
    "2",
  );
  await user.type(
    screen.getByLabelText("Endereço de entrega"),
    "Rua das Flores, 24",
  );
  await user.click(screen.getByRole("button", { name: "Confirmar pedido" }));
  expect(await screen.findByText("Pedido #12")).toBeInTheDocument();
  expect(screen.getByText("R$ 49,90")).toBeInTheDocument();
  expect(sent).toEqual({
    address: "Rua das Flores, 24",
    items: [{ productId: 3, quantity: 2 }],
  });
});
it("preserves the order form when creation is rejected and does not retry", async () => {
  let sends = 0;
  mockApi((path, init) => {
    if (path === "/api/products") return Response.json([sampleProduct]);
    if (init.method === "POST") {
      sends++;
      return Response.json(
        { message: "Produto indisponível" },
        { status: 400 },
      );
    }
    return Response.json([]);
  });
  const user = await openApp();
  await user.click(await screen.findByRole("link", { name: "Novo pedido" }));
  await user.type(
    await screen.findByLabelText("Quantidade de Bowl da casa"),
    "1",
  );
  await user.type(screen.getByLabelText("Endereço de entrega"), "Rua A");
  await user.click(screen.getByRole("button", { name: "Confirmar pedido" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Produto indisponível",
  );
  expect(screen.getByLabelText("Endereço de entrega")).toHaveValue("Rua A");
  expect(sends).toBe(1);
});
it("exposes operator next step and removes actions for final states", async () => {
  mockApi(
    () =>
      Response.json([
        sampleOrder,
        { ...sampleOrder, id: 13, status: "ENTREGUE" },
      ]),
    "OPERADOR",
  );
  await openApp();
  await screen.findByText("Pedido #12");
  expect(screen.getAllByText("Rua das Flores, 24")).toHaveLength(2);
  expect(
    screen.getAllByRole("button", { name: "Iniciar preparo" }),
  ).toHaveLength(1);
  expect(
    screen.getAllByRole("button", { name: "Cancelar pedido" }),
  ).toHaveLength(1);
});
