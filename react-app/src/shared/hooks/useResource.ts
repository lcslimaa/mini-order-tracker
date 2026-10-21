import { useCallback, useEffect, useRef, useState } from "react";
import { errorMessage } from "../api/client";
export function useResource<T>(
  loader: (signal: AbortSignal) => Promise<T>,
  poll = false,
) {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const active = useRef<AbortController | null>(null);
  const mounted = useRef(false);
  const refresh = useCallback(
    async (force = false) => {
      if (active.current && !force) return;
      active.current?.abort();
      const controller = new AbortController();
      active.current = controller;
      setLoading(true);
      setError("");
      try {
        const value = await loader(controller.signal);
        if (
          mounted.current &&
          active.current === controller &&
          !controller.signal.aborted
        )
          setData(value);
      } catch (e) {
        if (
          mounted.current &&
          active.current === controller &&
          !controller.signal.aborted
        )
          setError(errorMessage(e));
      } finally {
        if (mounted.current && active.current === controller) {
          active.current = null;
          setLoading(false);
        }
      }
    },
    [loader],
  );
  useEffect(() => {
    mounted.current = true;
    void refresh();
    const onVisible = () => {
      if (document.visibilityState === "visible") void refresh();
    };
    if (poll) document.addEventListener("visibilitychange", onVisible);
    const timer = poll
      ? setInterval(() => {
          if (document.visibilityState === "visible") void refresh();
        }, 15000)
      : undefined;
    return () => {
      mounted.current = false;
      active.current?.abort();
      active.current = null;
      clearInterval(timer);
      document.removeEventListener("visibilitychange", onVisible);
    };
  }, [refresh, poll]);
  const update = useCallback((updater: (previous: T | null) => T) => {
    active.current?.abort();
    active.current = null;
    setLoading(false);
    setData(updater);
  }, []);
  return { data, loading, error, refresh, update };
}
