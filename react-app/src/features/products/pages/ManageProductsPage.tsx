import { useCallback, useEffect, useRef, useState } from "react";
import { useAuth } from "../../auth/AuthProvider";
import { productsApi } from "../api";
import { useResource } from "../../../shared/hooks/useResource";
import { money } from "../../../shared/utils/formatters";
import { ProductForm } from "../components/ProductForm";
import type { Product } from "../types";
export function ManageProductsPage() {
  const { run } = useAuth();
  const loader = useCallback(
    (signal: AbortSignal) =>
      run((token, sessionSignal) =>
        productsApi.list(token, AbortSignal.any([signal, sessionSignal])),
      ),
    [run],
  );
  const { data, loading, error, refresh, update } = useResource(loader);
  const [editor, setEditor] = useState<Product | "new" | null>(null);
  const [success, setSuccess] = useState("");
  const newButton = useRef<HTMLButtonElement>(null);
  const previousEditor = useRef(editor);
  useEffect(() => {
    if (editor === null && previousEditor.current !== null)
      newButton.current?.focus();
    previousEditor.current = editor;
  }, [editor]);
  function close() {
    setEditor(null);
  }
  function saved(product: Product) {
    update((old) =>
      [...(old ?? []).filter((item) => item.id !== product.id), product].sort(
        (a, b) => b.id - a.id,
      ),
    );
    close();
    setSuccess("Produto salvo com sucesso.");
  }
  return (
    <>
      <div className="page-heading">
        <div>
          <p className="eyebrow">UM CARDÁPIO SEMPRE EM DIA</p>
          <h1>Produtos</h1>
          <p className="muted">Organize o que seus clientes podem pedir.</p>
        </div>
        <button
          ref={newButton}
          disabled={loading || data === null || editor !== null}
          className="primary"
          onClick={() => {
            setEditor("new");
            setSuccess("");
          }}
        >
          Novo produto
        </button>
      </div>
      <div className="section-toolbar">
        <h2>
          Seu catálogo <span className="count">{data?.length ?? 0}</span>
        </h2>
        <button
          className="secondary"
          disabled={loading || editor !== null}
          onClick={() => void refresh()}
        >
          Atualizar
        </button>
      </div>
      {success && (
        <p role="status" className="success">
          {success}
        </p>
      )}
      {error && (
        <p role="alert" className="error">
          {error} Não foi possível atualizar o catálogo.
        </p>
      )}
      {editor !== null && (
        <ProductForm
          key={editor === "new" ? "new" : editor.id}
          product={editor === "new" ? undefined : editor}
          onSaved={saved}
          onClose={close}
        />
      )}
      {loading && data === null && <p role="status">Carregando produtos…</p>}
      {!loading && data?.length === 0 && (
        <div className="empty panel">
          <h2>Seu catálogo começa aqui</h2>
          <p className="muted">
            Cadastre o primeiro produto para receber pedidos.
          </p>
        </div>
      )}
      {Boolean(data?.length) && (
        <div className="panel product-table">
          <table>
            <thead>
              <tr>
                <th scope="col">Produto</th>
                <th scope="col">Preço unitário</th>
                <th scope="col">Disponibilidade</th>
                <th scope="col">
                  <span className="sr-only">Ações</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {data?.map((p) => (
                <tr key={p.id}>
                  <th scope="row">
                    {p.name}
                    <small>#{p.id}</small>
                  </th>
                  <td>{money(p.price)}</td>
                  <td>
                    <span
                      className={`badge ${p.available ? "available" : "unavailable"}`}
                    >
                      {p.available ? "Disponível" : "Indisponível"}
                    </span>
                  </td>
                  <td>
                    <button
                      className="secondary"
                      aria-label={`Editar ${p.name}`}
                      disabled={editor !== null}
                      onClick={() => {
                        setEditor(p);
                        setSuccess("");
                      }}
                    >
                      Editar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <p className="small muted catalog-note">
        Produtos indisponíveis ficam fora de novos pedidos. O histórico dos
        pedidos anteriores é preservado.
      </p>
    </>
  );
}
