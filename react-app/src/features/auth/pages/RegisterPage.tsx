import { useRef, useState, type FormEvent } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { homeFor, useAuth } from "../AuthProvider";
import { authApi } from "../api";
import { errorMessage } from "../../../shared/api/client";
export function RegisterPage() {
  const { session } = useAuth();
  const navigate = useNavigate();
  const busy = useRef(false);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  if (session) return <Navigate to={homeFor(session.user)} replace />;
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy.current) return;
    const data = new FormData(event.currentTarget);
    const name = String(data.get("name")).trim();
    const password = String(data.get("password"));
    if (
      !name ||
      !password.trim() ||
      new TextEncoder().encode(password).length > 72
    ) {
      setError("Informe um nome e uma senha válida de até 72 bytes.");
      return;
    }
    busy.current = true;
    setPending(true);
    setError("");
    try {
      await authApi.register({
        name,
        email: String(data.get("email")).trim(),
        password,
      });
      navigate("/login", { replace: true, state: { registered: true } });
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      busy.current = false;
      setPending(false);
    }
  }
  return (
    <main id="content" tabIndex={-1} className="register-page">
      <Link className="brand" to="/login">
        mesa<span>●</span>
      </Link>
      <div className="panel auth-form">
        <p className="eyebrow">SUA MESA ESTÁ PRONTA</p>
        <h1>Crie sua conta</h1>
        <p className="muted">Comece a acompanhar seus pedidos.</p>
        <form onSubmit={submit}>
          <label>
            Nome
            <input name="name" required maxLength={120} autoComplete="name" />
          </label>
          <label>
            Email
            <input name="email" type="email" required autoComplete="username" />
          </label>
          <label>
            Senha
            <input
              name="password"
              type="password"
              required
              autoComplete="new-password"
            />
          </label>
          {error && (
            <p role="alert" className="error">
              {error}
            </p>
          )}
          <button className="primary full" disabled={pending}>
            {pending ? "Criando…" : "Criar conta"}
          </button>
        </form>
        <p className="auth-switch">
          Já tem conta? <Link to="/login">Entrar</Link>
        </p>
      </div>
    </main>
  );
}
