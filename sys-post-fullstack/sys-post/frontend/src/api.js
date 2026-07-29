const BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:4000";

/**
 * Thin fetch wrapper. Throws an Error with a `.status` property on non-2xx
 * responses so callers (thunks) can branch on 401/403 vs other failures.
 */
export async function apiFetch(path, { method = "GET", body, token } = {}) {
  const res = await fetch(BASE_URL + path, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  let data = null;
  try { data = await res.json(); } catch (_) { /* e.g. 204 No Content */ }

  if (!res.ok) {
    const err = new Error((data && data.message) || `Request failed (${res.status})`);
    err.status = res.status;
    throw err;
  }
  return data;
}
