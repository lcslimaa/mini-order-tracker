import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../../features/auth/AuthProvider";
export function PortalLayout({ operator = false }: { operator?: boolean }) {
  const { session, logout } = useAuth();
  const root = operator ? "/operador" : "/cliente";
  return (
    <div className="portal">
      <a className="skip-link" href="#content">
        Pular para conteúdo
      </a>
      <aside className="sidebar">
        <NavLink to={`${root}/pedidos`} className="brand">
          mesa<span>●</span>
        </NavLink>
        <p className="sidebar-label">
          {operator ? "ÁREA DO OPERADOR" : "ÁREA DO CLIENTE"}
        </p>
        <nav aria-label="Menu principal">
          <NavLink to={`${root}/pedidos`} end>
            <span aria-hidden="true">▤</span>
            {operator ? "Pedidos" : "Meus pedidos"}
          </NavLink>
          {operator ? (
            <NavLink to="/operador/produtos">
              <span aria-hidden="true">▦</span>Produtos
            </NavLink>
          ) : (
            <NavLink to="/cliente/pedidos/novo">
              <span aria-hidden="true">＋</span>Novo pedido
            </NavLink>
          )}
        </nav>
        <div className="sidebar-bottom">
          <div className="avatar" aria-hidden="true">
            {session?.user.name.charAt(0)}
          </div>
          <div>
            <strong>{session?.user.name}</strong>
            <small>{operator ? "Operador" : "Cliente"}</small>
          </div>
          <button className="logout" onClick={logout}>
            Sair
          </button>
        </div>
      </aside>
      <div className="workspace">
        <header className="topbar">
          <span>Seu delivery, bem acompanhado.</span>
          <span className="environment">
            {operator ? "Painel de operação" : "Espaço do cliente"}
          </span>
        </header>
        <main id="content" tabIndex={-1} className="page">
          <Outlet />
        </main>
        <footer>mesa · Cada pedido tem seu cuidado.</footer>
      </div>
    </div>
  );
}
