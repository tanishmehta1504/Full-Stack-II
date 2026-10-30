import { describe, it, expect, vi, afterEach } from "vitest";
import { api, setToken, store, refresh, clear } from "./store";
afterEach(() => {
  vi.restoreAllMocks();
  setToken("");
  store.dispatch(clear());
});
describe("Authenticated API and Redux state", () => {
  it("sends a token and serializes a mutation", async () => {
    setToken("token");
    const f = vi
      .spyOn(globalThis, "fetch")
      .mockResolvedValue({ ok: true, json: async () => ({ id: "1" }) });
    expect(await api("/posts", "POST", { title: "Hi" })).toEqual({ id: "1" });
    expect(f.mock.calls[0][1].headers.Authorization).toBe("Bearer token");
    expect(f.mock.calls[0][1].body).toBe('{"title":"Hi"}');
  });
  it("loads workspace state and clears it at logout", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue({
      ok: true,
      json: async () => ({
        posts: [{ id: "1" }],
        activity: [],
        counts: [],
        daily: [],
      }),
    });
    await store.dispatch(refresh());
    expect(store.getState().workspace.posts).toHaveLength(1);
    expect(store.getState().workspace.loading).toBe(false);
    store.dispatch(clear());
    expect(store.getState().workspace.posts).toEqual([]);
  });
  it("surfaces server validation errors", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue({
      ok: false,
      status: 400,
      json: async () => ({ message: "Invalid title" }),
    });
    await expect(api("/posts")).rejects.toThrow("Invalid title");
  });
  it("expires the session when the server rejects authentication", async () => {
    const f = vi.fn();
    window.addEventListener("session-expired", f);
    vi.spyOn(globalThis, "fetch").mockResolvedValue({
      ok: false,
      status: 401,
      json: async () => {
        throw new Error("empty");
      },
    });
    await expect(api("/me")).rejects.toThrow("Request failed (401)");
    expect(f).toHaveBeenCalledOnce();
    window.removeEventListener("session-expired", f);
  });
  it("retains an actionable refresh error", async () => {
    vi.spyOn(globalThis, "fetch").mockRejectedValue(new Error("Offline"));
    await store.dispatch(refresh());
    expect(store.getState().workspace.error).toBe("Offline");
    expect(store.getState().workspace.loading).toBe(false);
  });
});
