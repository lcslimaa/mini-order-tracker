import { useRef, useState, type FormEvent } from "react";
import { Link, Navigate, useLocation } from "react-router-dom";
import { homeFor, useAuth } from "../AuthProvider";
import { errorMessage } from "../../../shared/api/client";
export function LoginPage() {
  const { session, login } = useAuth();
  const location = useLocation();
  const [error, setError] = useState("");
  const [pending, setPending] = useState(false);
  const busy = useRef(false);
  if (session) return <Navigate to={homeFor(session.user)} replace />;
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy.current) return;
    const data = new FormData(event.currentTarget);
    busy.current = true;
    setPending(true);
    setError("");
    try {
      await login({
        email: String(data.get("email")).trim(),
        password: String(data.get("password")),
      });
    } catch (e) {
      if (!(e instanceof DOMException && e.name === "AbortError"))
        setError(errorMessage(e));
    } finally {
      busy.current = false;
      setPending(false);
    }
  }
  return (
    <div className="auth-page">
      <aside className="auth-story">
        <Link to="/login" className="brand">
          mesa<span>●</span>
        </Link>
        <div>
          <p className="eyebrow">DO PRIMEIRO PEDIDO À ÚLTIMA ENTREGA</p>
          <h1>
            Bom atendimento
            <br />
            começa por aqui.
          </h1>
          <p>Peça, acompanhe e deixe o resto com a gente.</p>
        </div>
        <p className="story-footer">
          Feito para aproximar quem pede de quem prepara.
        </p>
      </aside>
      <main id="content" tabIndex={-1} className="auth-main">
        <div className="auth-form">
          <p className="eyebrow">BEM-VINDO DE VOLTA</p>
          <h2>Entre na sua conta</h2>
          <p className="muted">Seus pedidos, sempre por perto.</p>
          {location.state?.registered && (
            <p role="status" className="success">
              Conta criada. Entre para fazer seu primeiro pedido.
            </p>
          )}
          <form onSubmit={submit}>
            <label>
              Email
              <input
                name="email"
                type="email"
                required
                autoComplete="username"
                placeholder="voce@exemplo.com"
              />
            </label>
            <label>
              Senha
              <input
                name="password"
                type="password"
                required
                autoComplete="current-password"
                placeholder="Sua senha"
              />
            </label>
            {error && (
              <p role="alert" className="error">
                {error}
              </p>
            )}
            <button className="primary full" disabled={pending}>
              {pending ? "Entrando…" : "Entrar"}
            </button>
          </form>
          <p className="auth-switch">
            Ainda não tem conta? <Link to="/cadastro">Cadastre-se</Link>
          </p>
        </div>
        <p className="auth-note">Um só lugar para acompanhar cada etapa.</p>
      </main>
    </div>
  );
}
