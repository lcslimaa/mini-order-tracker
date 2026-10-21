export type Product = {
  id: number;
  name: string;
  price: number;
  available: boolean;
};
export type ProductInput = Omit<Product, "id">;
