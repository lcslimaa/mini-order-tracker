import type { OrderStatus } from "../types";
export const statusLabels: Record<OrderStatus, string> = {
  RECEBIDO: "Recebido",
  EM_PREPARO: "Em preparo",
  SAIU_PARA_ENTREGA: "Saiu para entrega",
  ENTREGUE: "Entregue",
  CANCELADO: "Cancelado",
};
export function OrderStatusBadge({ status }: { status: OrderStatus }) {
  return (
    <span className={`badge status-${status.toLowerCase()}`}>
      <span aria-hidden="true">●</span>
      {statusLabels[status] ?? status}
    </span>
  );
}
