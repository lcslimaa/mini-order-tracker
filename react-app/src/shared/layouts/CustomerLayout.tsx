import { PortalLayout } from "./PortalLayout";
import { CreatedOrderProvider } from "../../features/orders/CreatedOrderProvider";
export function CustomerLayout() {
  return (
    <CreatedOrderProvider>
      <PortalLayout />
    </CreatedOrderProvider>
  );
}
