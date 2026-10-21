import { describe, it, expect, vi } from "vitest";
import { ApiError, request } from "./client";
describe("HTTP boundary", () => {
  it("sends a restricted JSON body with bearer and returns server data", async () => {
    const fetcher = vi.fn(
      async () =>
        new Response(JSON.stringify({ id: 7, total: 30 }), { status: 201 }),
    );
    vi.stubGlobal("fetch", fetcher);
    expect(
      await request("/orders", {
        token: "secret",
        method: "POST",
        body: { address: "Rua A", items: [{ productId: 1, quantity: 2 }] },
      }),
    ).toEqual({ id: 7, total: 30 });
    const [url, init] = fetcher.mock.calls[0] as unknown as [
      string,
      RequestInit,
    ];
    expect(url).toBe("/api/orders");
    expect(new Headers(init.headers).get("Authorization")).toBe(
      "Bearer secret",
    );
    expect(JSON.parse(init.body as string)).toEqual({
      address: "Rua A",
      items: [{ productId: 1, quantity: 2 }],
    });
  });
  it("surfaces HTTP errors without retrying a mutation", async () => {
    const fetcher = vi.fn(
      async () =>
        new Response(JSON.stringify({ message: "Produto indisponível" }), {
          status: 400,
        }),
    );
    vi.stubGlobal("fetch", fetcher);
    await expect(
      request("/orders", { method: "POST", body: {} }),
    ).rejects.toMatchObject({ status: 400, message: "Produto indisponível" });
    expect(fetcher).toHaveBeenCalledTimes(1);
  });
  it("handles non-JSON errors and aborted requests", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => new Response("Gateway failure", { status: 502 })),
    );
    await expect(request("/orders")).rejects.toBeInstanceOf(ApiError);
    const error = new DOMException("Aborted", "AbortError");
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => {
        throw error;
      }),
    );
    await expect(request("/orders")).rejects.toBe(error);
  });
});
