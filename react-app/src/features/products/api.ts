import { request } from "../../shared/api/client";
import type { Product, ProductInput } from "./types";
export const productsApi = {
  list: (token: string, signal?: AbortSignal) =>
    request<Product[]>("/products", { token, signal }),
  save: (
    token: string,
    body: ProductInput,
    id?: number,
    signal?: AbortSignal,
  ) =>
    request<Product>(id ? `/products/${id}` : "/products", {
      token,
      method: id ? "PUT" : "POST",
      body,
      signal,
    }),
};
