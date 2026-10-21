import { request } from "../../shared/api/client";
import type { Credentials, TokenResponse, User } from "./types";
export const authApi = {
  login: (body: Credentials, signal?: AbortSignal) =>
    request<TokenResponse>("/auth/login", { method: "POST", body, signal }),
  register: (body: Credentials & { name: string }, signal?: AbortSignal) =>
    request<User>("/auth/register", { method: "POST", body, signal }),
  me: (token: string, signal?: AbortSignal) =>
    request<User>("/auth/me", { token, signal }),
};
