import { act, renderHook, waitFor } from "@testing-library/react";
import { it, expect, vi, afterEach } from "vitest";
import { useResource } from "./useResource";
afterEach(() => {
  vi.useRealTimers();
  Object.defineProperty(document, "visibilityState", {
    configurable: true,
    value: "visible",
  });
});
it("polls only while visible and refreshes on return without overlapping calls", async () => {
  let resolve: ((value: string[]) => void) | undefined;
  let calls = 0;
  const loader = vi.fn(async () => {
    calls++;
    return calls === 1
      ? ["Recebido"]
      : new Promise<string[]>((done) => {
          resolve = done;
        });
  });
  vi.useFakeTimers();
  const { result, unmount } = renderHook(() => useResource(loader, true));
  await act(async () => {});
  expect(result.current.data).toEqual(["Recebido"]);
  Object.defineProperty(document, "visibilityState", {
    configurable: true,
    value: "hidden",
  });
  await act(() => vi.advanceTimersByTimeAsync(30000));
  expect(calls).toBe(1);
  Object.defineProperty(document, "visibilityState", {
    configurable: true,
    value: "visible",
  });
  act(() => document.dispatchEvent(new Event("visibilitychange")));
  await act(() => vi.advanceTimersByTimeAsync(45000));
  expect(calls).toBe(2);
  await act(async () => resolve?.(["Em preparo"]));
  expect(result.current.data).toEqual(["Em preparo"]);
  await act(() => vi.advanceTimersByTimeAsync(15000));
  expect(calls).toBe(3);
  unmount();
  await act(() => vi.advanceTimersByTimeAsync(30000));
  expect(calls).toBe(3);
});
it("aborts unmounted work and discards stale responses after an explicit update", async () => {
  let resolve: ((value: number[]) => void) | undefined;
  let signal: AbortSignal | undefined;
  const loader = async (s: AbortSignal) => {
    signal = s;
    return new Promise<number[]>((done) => {
      resolve = done;
    });
  };
  const { result, unmount } = renderHook(() => useResource(loader));
  act(() => result.current.update(() => [2]));
  expect(signal?.aborted).toBe(true);
  await act(async () => resolve?.([1]));
  expect(result.current.data).toEqual([2]);
  act(() => {
    void result.current.refresh();
  });
  unmount();
  expect(signal?.aborted).toBe(true);
});
