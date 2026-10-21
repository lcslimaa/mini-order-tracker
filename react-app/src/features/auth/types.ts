export type Role = "CLIENTE" | "OPERADOR";
export type User = { id: number; name: string; email: string; role: Role };
export type Credentials = { email: string; password: string };
export type TokenResponse = {
  accessToken: string;
  tokenType: "Bearer";
  expiresIn: number;
};
