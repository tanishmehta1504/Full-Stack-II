/**
 * Decodes (does NOT verify) a JWT payload so the UI can show claims like
 * role/exp for the session chip. Real verification happens server-side on
 * every request in backend/middleware.js — this is display-only.
 */
export function decodeToken(raw) {
  if (!raw) return null;
  const parts = raw.split(".");
  if (parts.length !== 3) return null;
  try {
    const pad = parts[1].length % 4 ? "=".repeat(4 - (parts[1].length % 4)) : "";
    const base64 = parts[1].replace(/-/g, "+").replace(/_/g, "/") + pad;
    return JSON.parse(decodeURIComponent(escape(atob(base64))));
  } catch (e) {
    return null;
  }
}
