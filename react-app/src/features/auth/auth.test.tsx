import { useState } from "react";
import { act, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { it, expect, vi } from "vitest";
import { AuthProvider, useAuth } from "./AuthProvider";
import { App } from "../../app/App";

function mount(path = "/login") {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AuthProvider>
        <App />
      </AuthProvider>
    </MemoryRouter>,
  );
}
function serve(role = "CLIENTE", failMe = false) {
  vi.stubGlobal(
    "fetch",
    vi.fn(async (url: string) => {
      if (url === "/api/auth/login")
        return Response.json({
          accessToken: "token",
          tokenType: "Bearer",
          expiresIn: 3600,
        });
      if (url === "/api/auth/register")
        return Response.json(
          { id: 2, name: "Ana", email: "ana@example.com", role: "CLIENTE" },
          { status: 201 },
        );
      if (url === "/api/auth/me")
        return failMe
          ? Response.json({ message: "Sessão inválida" }, { status: 401 })
          : Response.json({
              id: 2,
              name: "Ana",
              email: "ana@example.com",
              role,
            });
      return Response.json([]);
    }),
  );
}
async function signIn() {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText("Email"), "ana@example.com");
  await user.type(screen.getByLabelText("Senha"), "senha");
  await user.click(screen.getByRole("button", { name: "Entrar" }));
  return user;
}
it("confirms identity before opening customer area and clears it on logout", async () => {
  serve();
  mount();
  const user = await signIn();
  expect(
    await screen.findByRole("heading", { name: "Meus pedidos" }),
  ).toBeInTheDocument();
  await user.click(screen.getByRole("button", { name: "Sair" }));
  expect(
    await screen.findByRole("heading", { name: "Entre na sua conta" }),
  ).toBeInTheDocument();
  expect(screen.queryByText("Ana")).not.toBeInTheDocument();
});
it("routes an operator to the global area and denies anonymous direct access", async () => {
  serve("OPERADOR");
  mount("/operador/produtos");
  expect(
    screen.getByRole("heading", { name: "Entre na sua conta" }),
  ).toBeInTheDocument();
  await signIn();
  expect(
    await screen.findByRole("heading", { name: "Todos os pedidos" }),
  ).toBeInTheDocument();
});
it("does not accept a partially authenticated session when me fails", async () => {
  serve("CLIENTE", true);
  mount();
  await signIn();
  expect(await screen.findByRole("alert")).toHaveTextContent("Sessão inválida");
  expect(
    screen.getByRole("heading", { name: "Entre na sua conta" }),
  ).toBeInTheDocument();
});
it("shows rejected credentials without entering the application", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn(async () =>
      Response.json({ message: "Email ou senha inválidos" }, { status: 401 }),
    ),
  );
  mount();
  await signIn();
  expect(await screen.findByRole("alert")).toHaveTextContent(
    "Email ou senha inválidos",
  );
});
it("registers a customer then presents login", async () => {
  serve();
  mount("/cadastro");
  const user = userEvent.setup();
  await user.type(screen.getByLabelText("Nome"), "Ana");
  await user.type(screen.getByLabelText("Email"), "ana@example.com");
  await user.type(screen.getByLabelText("Senha"), "senha");
  await user.click(screen.getByRole("button", { name: "Criar conta" }));
  expect(
    await screen.findByRole("heading", { name: "Entre na sua conta" }),
  ).toBeInTheDocument();
});
it("redirects a customer away from operator routes", async () => {
  serve();
  mount();
  const user = await signIn();
  await screen.findByRole("heading", { name: "Meus pedidos" });
  // Use a real navigation within the router to test role guards.
  await user.click(screen.getByRole("link", { name: "Meus pedidos" }));
  expect(
    screen.queryByRole("link", { name: "Produtos" }),
  ).not.toBeInTheDocument();
});
function Probe() {
  const auth = useAuth();
  return (
    <>
      <button
        onClick={() =>
          void auth.login({ email: "ana@example.com", password: "x" })
        }
      >
        Login probe
      </button>
      <button
        onClick={() =>
          void auth
            .run((token, signal) =>
              import("../../shared/api/client").then((m) =>
                m.request("/orders", { token, signal }),
              ),
            )
            .catch(() => {})
        }
      >
        Consulta
      </button>
      <span>{auth.session?.user.name ?? "Sem sessão"}</span>
    </>
  );
}
it("clears an established session when a protected call returns 401", async () => {
  serve();
  const user = userEvent.setup();
  render(
    <AuthProvider>
      <Probe />
    </AuthProvider>,
  );
  await user.click(screen.getByText("Login probe"));
  await screen.findByText("Ana");
  vi.stubGlobal(
    "fetch",
    vi.fn(async () => Response.json({ message: "Expirou" }, { status: 401 })),
  );
  await user.click(screen.getByText("Consulta"));
  await waitFor(() =>
    expect(screen.getByText("Sem sessão")).toBeInTheDocument(),
  );
});
function StaleProbe() {
  const auth = useAuth();
  const [value, setValue] = useState("Nenhum dado");
  return (
    <>
      <button
        onClick={() =>
          void auth.login({ email: "ana@example.com", password: "x" })
        }
      >
        Entrar no probe
      </button>
      <button
        onClick={() =>
          void auth
            .run((token, signal) =>
              import("../../shared/api/client").then((m) =>
                m.request<string>("/orders", { token, signal }),
              ),
            )
            .then(setValue)
            .catch(() => {})
        }
      >
        Buscar
      </button>
      <button onClick={auth.logout}>Sair do probe</button>
      <span>{auth.session?.user.name ?? "Sem sessão"}</span>
      <p>{value}</p>
    </>
  );
}
it("discards a pending response after logout even if the network ignores abort", async () => {
  serve();
  const user = userEvent.setup();
  render(
    <AuthProvider>
      <StaleProbe />
    </AuthProvider>,
  );
  await user.click(screen.getByText("Entrar no probe"));
  await screen.findByText("Ana");
  let resolve: ((value: Response) => void) | undefined;
  vi.stubGlobal(
    "fetch",
    vi.fn(
      () =>
        new Promise<Response>((done) => {
          resolve = done;
        }),
    ),
  );
  await user.click(screen.getByText("Buscar"));
  await user.click(screen.getByText("Sair do probe"));
  await act(async () => resolve?.(Response.json("Dados da sessão antiga")));
  expect(screen.getByText("Sem sessão")).toBeInTheDocument();
  expect(screen.getByText("Nenhum dado")).toBeInTheDocument();
});
