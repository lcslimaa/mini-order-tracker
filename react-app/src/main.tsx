import { createRoot } from "react-dom/client";
import { App } from "./app/App";
import { Providers } from "./app/providers";
import "./styles/globals.css";
createRoot(document.getElementById("root")!).render(
  <Providers>
    <App />
  </Providers>,
);
