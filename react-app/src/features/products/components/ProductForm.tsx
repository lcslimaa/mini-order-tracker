import { useEffect, useRef, useState, type FormEvent } from "react";
import { useAuth } from "../../auth/AuthProvider";
import { productsApi } from "../api";
import type { Product } from "../types";
import { errorMessage } from "../../../shared/api/client";
export function ProductForm({
  product,
  onSaved,
  onClose,
}: {
  product?: Product;
  onSaved: (product: Product) => void;
  onClose: () => void;
}) {
  const { run } = useAuth();
  const [name, setName] = useState(product?.name ?? "");
  const [price, setPrice] = useState(product?.price.toString() ?? "");
  const [available, setAvailable] = useState(product?.available ?? true);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  const busy = useRef(false);
  const alive = useRef(true);
  const nameInput = useRef<HTMLInputElement>(null);
  useEffect(() => {
    nameInput.current?.focus();
    return () => {
      alive.current = false;
    };
  }, []);
  async function submit(event: FormEvent) {
    event.preventDefault();
    if (busy.current) return;
    const amount = Number(price);
    if (
      !name.trim() ||
      !Number.isFinite(amount) ||
      amount <= 0 ||
      !/^\d+(\.\d{1,2})?$/.test(price)
    ) {
      setError(
        "Informe um nome e um preço positivo com até duas casas decimais.",
      );
      return;
    }
    busy.current = true;
    setPending(true);
    setError("");
    try {
      const result = await run((token, signal) =>
        productsApi.save(
          token,
          { name: name.trim(), price: amount, available },
          product?.id,
          signal,
        ),
      );
      if (alive.current) onSaved(result);
    } catch (e) {
      if (
        alive.current &&
        !(e instanceof DOMException && e.name === "AbortError")
      )
        setError(errorMessage(e));
    } finally {
      busy.current = false;
      if (alive.current) setPending(false);
    }
  }
  return (
    <section
      className="panel product-editor"
      aria-labelledby="product-form-title"
    >
      <div className="panel-heading">
        <h2 id="product-form-title">
          {product ? "Editar produto" : "Novo produto"}
        </h2>
        <button
          type="button"
          className="text-button"
          disabled={pending}
          onClick={onClose}
        >
          Fechar
        </button>
      </div>
      <form onSubmit={submit}>
        <div className="form-row">
          <label>
            Nome do produto
            <input
              ref={nameInput}
              required
              maxLength={160}
              value={name}
              onChange={(e) => setName(e.target.value)}
              disabled={pending}
            />
          </label>
          <label>
            Preço (R$)
            <input
              type="number"
              inputMode="decimal"
              min="0.01"
              max="9999999999.99"
              step="0.01"
              required
              value={price}
              onChange={(e) => setPrice(e.target.value)}
              disabled={pending}
            />
          </label>
        </div>
        <label className="checkbox-label">
          <input
            type="checkbox"
            checked={available}
            onChange={(e) => setAvailable(e.target.checked)}
            disabled={pending}
          />
          Disponível para pedidos
        </label>
        {error && (
          <p role="alert" className="error">
            {error}
          </p>
        )}
        <button className="primary" disabled={pending}>
          {pending ? "Salvando…" : "Salvar produto"}
        </button>
      </form>
    </section>
  );
}
