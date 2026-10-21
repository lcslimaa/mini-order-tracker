import { request } from "../../shared/api/client";
import type { CreateOrder, Order, OrderStatus } from "./types";
export const ordersApi = {
  list: (token: string, signal?: AbortSignal) =>
    request<Order[]>("/orders", { token, signal }),
  get: (token: string, id: number, signal?: AbortSignal) =>
    request<Order>(`/orders/${id}`, { token, signal }),
  create: (token: string, body: CreateOrder, signal?: AbortSignal) =>
    request<Order>("/orders", { token, method: "POST", body, signal }),
  advance: (
    token: string,
    id: number,
    status: OrderStatus,
    signal?: AbortSignal,
  ) =>
    request<Order>(`/orders/${id}/status`, {
      token,
      method: "PATCH",
      body: { status },
      signal,
    }),
  cancel: (token: string, id: number, signal?: AbortSignal) =>
    request<Order>(`/orders/${id}/cancel`, { token, method: "POST", signal }),
};
