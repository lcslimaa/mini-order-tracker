import { useCallback, useEffect, useRef, useState } from "react";
import { useAuth } from "../auth/AuthProvider";
import { useResource } from "../../shared/hooks/useResource";
import { ApiError, errorMessage } from "../../shared/api/client";
import { ordersApi } from "./api";
import type { Order, OrderStatus } from "./types";
import { useCreatedOrder } from "./CreatedOrderProvider";
export function useOrders() {
  const { run } = useAuth();
  const confirmation = useCreatedOrder();
  const loader = useCallback(
    (signal: AbortSignal) =>
      run((token, sessionSignal) =>
        ordersApi.list(token, AbortSignal.any([signal, sessionSignal])),
      ),
    [run],
  );
  const resource = useResource(loader, true);
  const locks = useRef(new Set<number>());
  const mounted = useRef(true);
  const [pending, setPending] = useState<number[]>([]);
  const [actionError, setActionError] = useState("");
  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);
  async function act(order: Order, status?: OrderStatus) {
    if (locks.current.has(order.id)) return;
    locks.current.add(order.id);
    setPending([...locks.current]);
    setActionError("");
    try {
      const updated = await run((token, signal) =>
        status
          ? ordersApi.advance(token, order.id, status, signal)
          : ordersApi.cancel(token, order.id, signal),
      );
      if (mounted.current) {
        if (confirmation?.order?.id === updated.id)
          confirmation.confirm(updated);
        resource.update((previous) =>
          (previous ?? []).map((item) =>
            item.id === updated.id ? updated : item,
          ),
        );
      }
    } catch (e) {
      if (
        !mounted.current ||
        (e instanceof DOMException && e.name === "AbortError")
      )
        return;
      if (e instanceof ApiError && e.status === 409) {
        setActionError(
          "O pedido mudou desde a última consulta. Confira o status atualizado.",
        );
        await resource.refresh(true);
      } else setActionError(errorMessage(e));
    } finally {
      locks.current.delete(order.id);
      if (mounted.current) setPending([...locks.current]);
    }
  }
  return {
    ...resource,
    orders: [...(resource.data ?? [])].sort((a, b) => b.id - a.id),
    pending,
    actionError,
    act,
  };
}
