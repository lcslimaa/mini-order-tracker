import { vi } from "vitest";
import type { Order } from "../features/orders/types";
export const sampleOrder: Order = {
  id: 12,
  customer: { id: 2, name: "Ana", email: "ana@example.com" },
  address: "Rua das Flores, 24",
  items: [
    {
      productId: 3,
      name: "Bowl da casa",
      unitPrice: 25,
      quantity: 2,
      subtotal: 50,
    },
  ],
  total: 50,
  status: "RECEBIDO",
  createdAt: "2026-10-20T18:00:00Z",
};
export const sampleProduct = {
  id: 3,
  name: "Bowl da casa",
  price: 25,
  available: true,
};
export function mockApi(
  handler: (path: string, init: RequestInit) => Response | Promise<Response>,
  role = "CLIENTE",
) {
  vi.stubGlobal(
    "fetch",
    vi.fn(async (input: string, init: RequestInit = {}) => {
      if (input === "/api/auth/login")
        return Response.json({
          accessToken: "token",
          tokenType: "Bearer",
          expiresIn: 3600,
        });
      if (input === "/api/auth/me")
        return Response.json({
          id: 2,
          name: "Ana",
          email: "ana@example.com",
          role,
        });
      return handler(input, init);
    }),
  );
}
