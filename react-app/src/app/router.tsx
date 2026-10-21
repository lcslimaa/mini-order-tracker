import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import { useEffect } from "react";
import { homeFor, useAuth } from "../features/auth/AuthProvider";
import { RequireAuth } from "../features/auth/RequireAuth";
import { LoginPage } from "../features/auth/pages/LoginPage";
import { RegisterPage } from "../features/auth/pages/RegisterPage";
import { CustomerLayout } from "../shared/layouts/CustomerLayout";
import { OperatorLayout } from "../shared/layouts/OperatorLayout";
import { CustomerOrdersPage } from "../features/orders/pages/CustomerOrdersPage";
import { OperatorOrdersPage } from "../features/orders/pages/OperatorOrdersPage";
import { CreateOrderPage } from "../features/orders/pages/CreateOrderPage";
import { ManageProductsPage } from "../features/products/pages/ManageProductsPage";
function Home() {
  const { session } = useAuth();
  return <Navigate to={session ? homeFor(session.user) : "/login"} replace />;
}
export function AppRouter() {
  const location = useLocation();
  useEffect(() => {
    document.getElementById("content")?.focus();
  }, [location.pathname]);
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/cadastro" element={<RegisterPage />} />
      <Route element={<RequireAuth role="CLIENTE" />}>
        <Route element={<CustomerLayout />}>
          <Route path="/cliente/pedidos" element={<CustomerOrdersPage />} />
          <Route path="/cliente/pedidos/novo" element={<CreateOrderPage />} />
        </Route>
      </Route>
      <Route element={<RequireAuth role="OPERADOR" />}>
        <Route element={<OperatorLayout />}>
          <Route path="/operador/pedidos" element={<OperatorOrdersPage />} />
          <Route path="/operador/produtos" element={<ManageProductsPage />} />
        </Route>
      </Route>
      <Route path="*" element={<Home />} />
    </Routes>
  );
}
