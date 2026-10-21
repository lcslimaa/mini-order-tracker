import { createContext, useContext, useState, type ReactNode } from "react";
import type { Order } from "./types";
const Context = createContext<{
  order: Order | null;
  confirm: (order: Order) => void;
} | null>(null);
// Scoped to the authenticated customer layout; unmounting the session clears it.
export function CreatedOrderProvider({ children }: { children: ReactNode }) {
  const [order, confirm] = useState<Order | null>(null);
  return (
    <Context.Provider value={{ order, confirm }}>{children}</Context.Provider>
  );
}
export const useCreatedOrder = () => useContext(Context);
