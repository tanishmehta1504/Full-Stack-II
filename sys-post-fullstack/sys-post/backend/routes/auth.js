import { Router } from "express";
import bcrypt from "bcryptjs";
import jwt from "jsonwebtoken";
import { users, publicUser } from "../db.js";
import { authenticate, JWT_SECRET } from "../middleware.js";

const router = Router();
const delay = (ms) => new Promise((res) => setTimeout(res, ms));
const SESSION = "15m";

router.post("/login", async (req, res) => {
  await delay(400); // simulate network latency
  const { username, password } = req.body || {};
  const user = users.find((u) => u.username === username);

  if (!user || !bcrypt.compareSync(String(password || ""), user.passwordHash)) {
    return res.status(401).json({ message: "Invalid username or password." });
  }

  const token = jwt.sign({ sub: user.id, name: user.name, role: user.role }, JWT_SECRET, { expiresIn: SESSION });
  res.json({ token, user: publicUser(user) });
});

router.get("/me", authenticate, (req, res) => {
  const user = users.find((u) => u.id === req.user.sub);
  if (!user) return res.status(404).json({ message: "User not found." });
  res.json({ user: publicUser(user) });
});

export default router;
