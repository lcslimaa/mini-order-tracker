export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}
export type RequestOptions = {
  token?: string;
  method?: string;
  body?: unknown;
  signal?: AbortSignal;
};
export async function request<T>(
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const headers = new Headers({ Accept: "application/json" });
  if (options.body !== undefined)
    headers.set("Content-Type", "application/json");
  if (options.token) headers.set("Authorization", `Bearer ${options.token}`);
  const response = await fetch(`/api${path}`, {
    method: options.method ?? "GET",
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
    signal: options.signal,
  });
  const data = await response.json().catch(() => null);
  if (!response.ok)
    throw new ApiError(
      response.status,
      data?.message ??
        "Não foi possível concluir a solicitação. Tente novamente.",
    );
  return data as T;
}
export const errorMessage = (error: unknown) =>
  error instanceof Error
    ? error.message
    : "Não foi possível concluir a solicitação.";
