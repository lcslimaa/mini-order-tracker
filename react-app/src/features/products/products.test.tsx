import { screen, waitFor } from "@testing-library/react";
import { it, expect } from "vitest";
import { mockApi, sampleProduct } from "../../test/fixtures";
import { openApp } from "../../test/renderApp";
it("creates a product and edits its availability using server confirmation", async () => {
  let products = [sampleProduct];
  let saved: unknown;
  mockApi((path, init) => {
    if (path === "/api/orders") return Response.json([]);
    if (init.method === "POST" || init.method === "PUT") {
      saved = JSON.parse(init.body as string);
      const value = { id: 3, ...(saved as object) };
      products = [value as typeof sampleProduct];
      return Response.json(value, {
        status: init.method === "POST" ? 201 : 200,
      });
    }
    return Response.json(products);
  }, "OPERADOR");
  const user = await openApp();
  await user.click(await screen.findByRole("link", { name: "Produtos" }));
  expect(await screen.findByText("Bowl da casa")).toBeInTheDocument();
  await user.click(screen.getByRole("button", { name: "Novo produto" }));
  await user.type(screen.getByLabelText("Nome do produto"), "Suco natural");
  await user.type(screen.getByLabelText("Preço (R$)"), "8.50");
  await user.click(screen.getByRole("button", { name: "Salvar produto" }));
  expect(await screen.findByText("Suco natural")).toBeInTheDocument();
  expect(saved).toEqual({ name: "Suco natural", price: 8.5, available: true });
  await user.click(screen.getByRole("button", { name: "Editar Suco natural" }));
  await user.click(screen.getByLabelText("Disponível para pedidos"));
  await user.click(screen.getByRole("button", { name: "Salvar produto" }));
  expect(await screen.findByText("Indisponível")).toBeInTheDocument();
  expect(saved).toEqual({ name: "Suco natural", price: 8.5, available: false });
});
it("keeps a rejected product form and reports the API error", async () => {
  mockApi(
    (path, init) =>
      init.method === "POST"
        ? Response.json({ message: "Conflito no catálogo" }, { status: 409 })
        : Response.json([]),
    "OPERADOR",
  );
  const user = await openApp();
  await user.click(await screen.findByRole("link", { name: "Produtos" }));
  await user.click(await screen.findByRole("button", { name: "Novo produto" }));
  await user.type(screen.getByLabelText("Nome do produto"), "Suco");
  await user.type(screen.getByLabelText("Preço (R$)"), "8");
  await user.click(screen.getByRole("button", { name: "Salvar produto" }));
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Conflito no catálogo",
  );
  expect(screen.getByLabelText("Nome do produto")).toHaveValue("Suco");
});
