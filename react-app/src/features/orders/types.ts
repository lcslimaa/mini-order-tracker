export type OrderStatus =
  "RECEBIDO" | "EM_PREPARO" | "SAIU_PARA_ENTREGA" | "ENTREGUE" | "CANCELADO";
export type Order = {
  id: number;
  customer: { id: number; name: string; email: string };
  address: string;
  items: {
    productId: number;
    name: string;
    unitPrice: number;
    quantity: number;
    subtotal: number;
  }[];
  total: number;
  status: OrderStatus;
  createdAt: string;
};
export type CreateOrder = {
  address: string;
  items: { productId: number; quantity: number }[];
};
