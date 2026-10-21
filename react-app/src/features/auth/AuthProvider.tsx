import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { ApiError } from "../../shared/api/client";
import { authApi } from "./api";
import type { Credentials, User } from "./types";
type Session = {
  token: string;
  user: User;
  controller: AbortController;
  expiresAt: number;
};
type Auth = {
  session: Session | null;
  login: (body: Credentials) => Promise<void>;
  logout: () => void;
  run: <T>(
    operation: (token: string, signal: AbortSignal) => Promise<T>,
  ) => Promise<T>;
};
const Context = createContext<Auth | null>(null);
export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(null);
  const current = useRef<Session | null>(null);
  const loginAttempt = useRef<AbortController | null>(null);
  const logout = useCallback(() => {
    loginAttempt.current?.abort();
    loginAttempt.current = null;
    current.current?.controller.abort();
    current.current = null;
    setSession(null);
  }, []);
  const login = useCallback(
    async (body: Credentials) => {
      logout();
      const attempt = new AbortController();
      loginAttempt.current = attempt;
      const token = await authApi.login(body, attempt.signal);
      const user = await authApi.me(token.accessToken, attempt.signal);
      if (attempt.signal.aborted)
        throw new DOMException("Sessão encerrada", "AbortError");
      if (!["CLIENTE", "OPERADOR"].includes(user.role))
        throw new Error("Perfil de acesso desconhecido.");
      const next = {
        token: token.accessToken,
        user,
        controller: attempt,
        expiresAt: Date.now() + token.expiresIn * 1000,
      };
      current.current = next;
      setSession(next);
    },
    [logout],
  );
  const run = useCallback(
    async <T,>(
      operation: (token: string, signal: AbortSignal) => Promise<T>,
    ) => {
      const active = current.current;
      if (!active) throw new DOMException("Sessão encerrada", "AbortError");
      try {
        const result = await operation(active.token, active.controller.signal);
        if (current.current !== active || active.controller.signal.aborted)
          throw new DOMException("Sessão encerrada", "AbortError");
        return result;
      } catch (error) {
        if (current.current !== active || active.controller.signal.aborted)
          throw new DOMException("Sessão encerrada", "AbortError");
        if (error instanceof ApiError && error.status === 401) logout();
        throw error;
      }
    },
    [logout],
  );
  useEffect(() => {
    if (!session) return;
    const timer = setTimeout(
      logout,
      Math.max(0, session.expiresAt - Date.now()),
    );
    return () => clearTimeout(timer);
  }, [session, logout]);
  useEffect(
    () => () => {
      loginAttempt.current?.abort();
      current.current?.controller.abort();
    },
    [],
  );
  return (
    <Context.Provider value={{ session, login, logout, run }}>
      {children}
    </Context.Provider>
  );
}
export function useAuth() {
  const auth = useContext(Context);
  if (!auth) throw new Error("AuthProvider ausente");
  return auth;
}
export const homeFor = (user: User) =>
  user.role === "OPERADOR" ? "/operador/pedidos" : "/cliente/pedidos";
