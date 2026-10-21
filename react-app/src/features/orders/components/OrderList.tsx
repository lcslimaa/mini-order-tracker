import { Link, useLocation } from "react-router-dom";
import { money, dateTime } from "../../../shared/utils/formatters";
import { useOrders } from "../useOrders";
import { OrderStatusBadge } from "./OrderStatusBadge";
import type { OrderStatus } from "../types";
import { useCreatedOrder } from "../CreatedOrderProvider";
const nextStep: Partial<
  Record<OrderStatus, { status: OrderStatus; label: string }>
> = {
  RECEBIDO: { status: "EM_PREPARO", label: "Iniciar preparo" },
  EM_PREPARO: { status: "SAIU_PARA_ENTREGA", label: "Enviar para entrega" },
  SAIU_PARA_ENTREGA: { status: "ENTREGUE", label: "Concluir entrega" },
};
export function OrderList({ operator = false }: { operator?: boolean }) {
  const {
    orders: loadedOrders,
    data,
    loading,
    error,
    refresh,
    pending,
    actionError,
    act,
  } = useOrders();
  const location = useLocation();
  const confirmation = useCreatedOrder();
  const confirmed = operator ? null : confirmation?.order;
  const missingConfirmation =
    confirmed && !loadedOrders.some((order) => order.id === confirmed.id);
  const orders = missingConfirmation
    ? [confirmed, ...loadedOrders]
    : loadedOrders;
  return (
    <>
      <div className="page-heading">
        <div>
          <p className="eyebrow">
            {operator ? "CENTRAL DE ATENDIMENTO" : "DO SEU JEITO, NO SEU TEMPO"}
          </p>
          <h1>{operator ? "Todos os pedidos" : "Meus pedidos"}</h1>
          <p className="muted">
            {operator
              ? "Cuide de cada etapa, do preparo à entrega."
              : "Acompanhe cada etapa até chegar à sua porta."}
          </p>
        </div>
        {!operator && (
          <Link className="button primary" to="/cliente/pedidos/novo">
            <span aria-hidden="true">＋</span> Fazer pedido
          </Link>
        )}
      </div>
      <section className="overview" aria-label="Resumo dos pedidos">
        <div>
          <span>Pedidos no total</span>
          <strong>{data === null ? "—" : orders.length}</strong>
        </div>
        <div>
          <span>Em andamento</span>
          <strong>
            {data === null
              ? "—"
              : orders.filter((o) => nextStep[o.status]).length}
          </strong>
        </div>
        <div>
          <span>Entregues</span>
          <strong>
            {data === null
              ? "—"
              : orders.filter((o) => o.status === "ENTREGUE").length}
          </strong>
        </div>
      </section>
      <div className="section-toolbar">
        <h2>{operator ? "Fila de pedidos" : "Seu histórico"}</h2>
        <div>
          <span className="muted refresh-note">Atualização a cada 15 s</span>
          <button
            className="secondary"
            disabled={loading}
            onClick={() => void refresh()}
          >
            {loading ? "Atualizando…" : "Atualizar"}
          </button>
        </div>
      </div>
      {location.state?.created && (
        <p role="status" className="success">
          Pedido criado com sucesso. Confira abaixo o total confirmado.
        </p>
      )}
      {error && (
        <p role="alert" className="error">
          Não foi possível atualizar os pedidos. {error}{" "}
          {data && "Os dados abaixo são da última consulta."}
        </p>
      )}
      {missingConfirmation && (
        <p className="muted small">
          O pedido abaixo foi confirmado na criação. Aguardando atualização da
          lista.
        </p>
      )}
      {actionError && (
        <p role="alert" className="error">
          {actionError}
        </p>
      )}
      {loading && data === null && (
        <p role="status" className="loading">
          Buscando seus pedidos…
        </p>
      )}
      {!loading && !error && data?.length === 0 && orders.length === 0 && (
        <div className="empty panel">
          <div className="empty-icon" aria-hidden="true">
            ▤
          </div>
          <h3>
            {operator
              ? "Nenhum pedido por enquanto"
              : "Seu primeiro pedido começa aqui"}
          </h3>
          <p className="muted">
            {operator
              ? "Os novos pedidos aparecerão nesta lista."
              : "Escolha seus favoritos e acompanhe tudo por aqui."}
          </p>
          {!operator && (
            <Link className="button primary" to="/cliente/pedidos/novo">
              Explorar cardápio
            </Link>
          )}
        </div>
      )}
      <div className="order-grid">
        {orders.map((order) => {
          const next = operator ? nextStep[order.status] : undefined;
          const cancellable = operator
            ? Boolean(nextStep[order.status])
            : order.status === "RECEBIDO";
          return (
            <article className="panel order-card" key={order.id}>
              <div className="order-head">
                <div>
                  <h3>Pedido #{order.id}</h3>
                  <time dateTime={order.createdAt}>
                    {dateTime(order.createdAt)}
                  </time>
                </div>
                <OrderStatusBadge status={order.status} />
              </div>
              {operator && (
                <div className="customer-info">
                  <strong>{order.customer.name}</strong>
                  <span>{order.customer.email}</span>
                  <p>{order.address}</p>
                </div>
              )}
              <ul className="order-items">
                {order.items.map((item) => (
                  <li key={item.productId}>
                    <span>
                      <b>{item.quantity}×</b> {item.name}
                      <small>{money(item.unitPrice)} por unidade</small>
                    </span>
                    <strong>{money(item.subtotal)}</strong>
                  </li>
                ))}
              </ul>
              <div className="order-total">
                <span>Total do pedido</span>
                <strong>{money(order.total)}</strong>
              </div>
              {!operator && (
                <p className="delivery-address">Entrega em {order.address}</p>
              )}
              {(next || cancellable) && (
                <div className="order-actions">
                  {next && (
                    <button
                      className="primary"
                      disabled={pending.includes(order.id)}
                      onClick={() => void act(order, next.status)}
                    >
                      {next.label}
                    </button>
                  )}
                  {cancellable && (
                    <button
                      className="text-button danger"
                      disabled={pending.includes(order.id)}
                      onClick={() => void act(order)}
                    >
                      Cancelar pedido
                    </button>
                  )}
                  {pending.includes(order.id) && (
                    <span role="status">Salvando…</span>
                  )}
                </div>
              )}
            </article>
          );
        })}
      </div>
    </>
  );
}
