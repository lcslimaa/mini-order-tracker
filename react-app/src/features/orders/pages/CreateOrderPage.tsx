import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type FormEvent,
} from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../../auth/AuthProvider";
import { productsApi } from "../../products/api";
import { useResource } from "../../../shared/hooks/useResource";
import { money } from "../../../shared/utils/formatters";
import { ApiError, errorMessage } from "../../../shared/api/client";
import { ordersApi } from "../api";
import { useCreatedOrder } from "../CreatedOrderProvider";
export function CreateOrderPage() {
  const { run } = useAuth();
  const navigate = useNavigate();
  const confirmation = useCreatedOrder();
  const loader = useCallback(
    (signal: AbortSignal) =>
      run((token, sessionSignal) =>
        productsApi.list(token, AbortSignal.any([signal, sessionSignal])),
      ),
    [run],
  );
  const { data, loading, error, refresh } = useResource(loader);
  const [quantities, setQuantities] = useState<Record<number, string>>({});
  const [address, setAddress] = useState("");
  const [submitError, setSubmitError] = useState("");
  const [pending, setPending] = useState(false);
  const [uncertain, setUncertain] = useState(false);
  const busy = useRef(false);
  const alive = useRef(true);
  useEffect(
    () => () => {
      alive.current = false;
    },
    [],
  );
  const products = (data ?? []).filter((p) => p.available);
  const total = products.reduce(
    (sum, p) => sum + p.price * (Number(quantities[p.id]) || 0),
    0,
  );
  async function submit(event: FormEvent) {
    event.preventDefault();
    if (busy.current || uncertain) return;
    const items = Object.entries(quantities)
      .filter(([, q]) => q !== "" && Number(q) !== 0)
      .map(([id, q]) => ({ productId: Number(id), quantity: Number(q) }));
    if (
      !address.trim() ||
      items.length === 0 ||
      items.some((i) => !Number.isSafeInteger(i.quantity) || i.quantity <= 0)
    ) {
      setSubmitError(
        "Informe o endereço e selecione ao menos um produto com quantidade inteira positiva.",
      );
      return;
    }
    busy.current = true;
    setPending(true);
    setSubmitError("");
    try {
      const created = await run((token, signal) =>
        ordersApi.create(token, { address: address.trim(), items }, signal),
      );
      if (alive.current) {
        confirmation?.confirm(created);
        navigate("/cliente/pedidos", {
          replace: true,
          state: { created: true },
        });
      }
    } catch (e) {
      if (
        alive.current &&
        !(e instanceof DOMException && e.name === "AbortError")
      ) {
        const ambiguous =
          !(e instanceof ApiError) || e.status >= 500 || e.status === 408;
        setUncertain(ambiguous);
        setSubmitError(
          ambiguous
            ? "Não foi possível confirmar o envio. Consulte seus pedidos antes de tentar novamente."
            : errorMessage(e),
        );
      }
    } finally {
      busy.current = false;
      if (alive.current) setPending(false);
    }
  }
  return (
    <>
      <Link className="back-link" to="/cliente/pedidos">
        ← Voltar para pedidos
      </Link>
      <div className="page-heading">
        <div>
          <p className="eyebrow">UMA BOA ESCOLHA COMEÇA AQUI</p>
          <h1>Novo pedido</h1>
          <p className="muted">Escolha o que vai à sua mesa hoje.</p>
        </div>
      </div>
      {error && (
        <p role="alert" className="error">
          {error}{" "}
          <button onClick={() => void refresh()}>Tentar novamente</button>
        </p>
      )}
      {loading && <p role="status">Carregando cardápio…</p>}
      {data && products.length === 0 && (
        <div className="empty panel">
          <h2>Cardápio indisponível no momento</h2>
          <p>Volte mais tarde para conferir os produtos.</p>
        </div>
      )}
      {products.length > 0 && (
        <form className="checkout" onSubmit={submit}>
          <section className="panel catalog">
            <div className="panel-heading">
              <h2>Cardápio</h2>
              <span className="muted">
                {products.length} opções disponíveis
              </span>
            </div>
            {products.map((p, index) => (
              <div className="catalog-row" key={p.id}>
                <div
                  className={`product-symbol tint-${index % 3}`}
                  aria-hidden="true"
                >
                  {p.name.charAt(0)}
                </div>
                <div className="product-info">
                  <h3>{p.name}</h3>
                  <strong>{money(p.price)}</strong>
                </div>
                <label className="quantity-label">
                  Quantidade<span className="sr-only"> de {p.name}</span>
                  <input
                    aria-label={`Quantidade de ${p.name}`}
                    type="number"
                    inputMode="numeric"
                    min="0"
                    step="1"
                    value={quantities[p.id] ?? ""}
                    placeholder="0"
                    onChange={(e) =>
                      setQuantities({ ...quantities, [p.id]: e.target.value })
                    }
                    disabled={pending}
                  />
                </label>
              </div>
            ))}
          </section>
          <aside className="panel checkout-summary">
            <h2>Seu pedido</h2>
            <p className="muted">Quase pronto para preparar.</p>
            <label>
              Endereço de entrega
              <textarea
                required
                maxLength={1000}
                rows={3}
                value={address}
                onChange={(e) => setAddress(e.target.value)}
                placeholder="Rua, número, bairro e complemento"
                disabled={pending}
              />
            </label>
            <div className="order-total">
              <span>Total estimado</span>
              <strong>{money(total)}</strong>
            </div>
            <p className="small muted">
              O valor final será confirmado pelo servidor ao criar o pedido. Sem
              taxa de entrega.
            </p>
            {submitError && (
              <p role="alert" className="error">
                {submitError}
                {uncertain && (
                  <>
                    {" "}
                    <Link to="/cliente/pedidos">Consultar meus pedidos</Link>
                  </>
                )}
              </p>
            )}
            <button className="primary full" disabled={pending || uncertain}>
              {pending ? "Enviando…" : "Confirmar pedido"}
            </button>
          </aside>
        </form>
      )}
    </>
  );
}
