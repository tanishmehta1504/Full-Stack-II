import jwt from "jsonwebtoken";
import { can } from "./db.js";

const JWT_SECRET = process.env.JWT_SECRET || "dev-secret-change-me";

/** Verifies the Bearer token on every protected request and attaches the decoded claims to req.user. */
export function authenticate(req, res, next) {
  const header = req.headers.authorization || "";
  const token = header.startsWith("Bearer ") ? header.slice(7) : null;
  if (!token) return res.status(401).json({ message: "Missing authentication token." });

  try {
    req.user = jwt.verify(token, JWT_SECRET); // { sub, name, role, iat, exp }
    next();
  } catch (err) {
    return res.status(401).json({ message: "Invalid or expired session. Please log in again." });
  }
}

/** Route-level RBAC guard — call as authorize('publish'), authorize('manageUsers'), etc. */
export function authorize(action) {
  return (req, res, next) => {
    if (!req.user || !can(req.user.role, action)) {
      return res.status(403).json({ message: `Forbidden — the '${req.user ? req.user.role : "unknown"}' role cannot '${action}'.` });
    }
    next();
  };
}

export { JWT_SECRET };
