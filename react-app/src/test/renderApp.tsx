import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { App } from "../app/App";
import { AuthProvider } from "../features/auth/AuthProvider";
export async function openApp() {
  render(
    <MemoryRouter>
      <AuthProvider>
        <App />
      </AuthProvider>
    </MemoryRouter>,
  );
  const user = userEvent.setup();
  await user.type(screen.getByLabelText("Email"), "ana@example.com");
  await user.type(screen.getByLabelText("Senha"), "senha");
  await user.click(screen.getByRole("button", { name: "Entrar" }));
  return user;
}
