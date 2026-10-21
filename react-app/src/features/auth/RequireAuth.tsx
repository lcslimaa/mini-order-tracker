import { Navigate, Outlet } from "react-router-dom";
import { homeFor, useAuth } from "./AuthProvider";
import type { Role } from "./types";
export function RequireAuth({ role }: { role: Role }) {
  const { session } = useAuth();
  if (!session) return <Navigate to="/login" replace />;
  if (session.user.role !== role)
    return <Navigate to={homeFor(session.user)} replace />;
  return <Outlet key={session.token} />;
}
