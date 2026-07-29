import { Router } from "express";
import { drafts, setDrafts, newId } from "../db.js";
import { authenticate, authorize } from "../middleware.js";

const router = Router();
const delay = (ms) => new Promise((res) => setTimeout(res, ms));

router.use(authenticate);

router.get("/", async (req, res) => {
  await delay(300);
  res.json(drafts);
});

router.post("/", authorize("create"), async (req, res) => {
  await delay(300);
  const { content, platforms, scheduledAt, status } = req.body || {};
  if (!content || !Array.isArray(platforms) || platforms.length === 0) {
    return res.status(400).json({ message: "content and at least one platform are required." });
  }
  const now = Date.now();
  const draft = {
    id: newId(),
    content: String(content).trim(),
    platforms,
    authorId: req.user.sub,
    status: status || (scheduledAt ? "scheduled" : "draft"),
    scheduledAt: scheduledAt || null,
    createdAt: now,
    updatedAt: now,
  };
  setDrafts([...drafts, draft]);
  res.status(201).json(draft);
});

router.put("/:id", authorize("edit"), async (req, res) => {
  await delay(300);
  const idx = drafts.findIndex((d) => d.id === req.params.id);
  if (idx === -1) return res.status(404).json({ message: "Draft not found." });
  const updated = { ...drafts[idx], ...req.body, id: drafts[idx].id, updatedAt: Date.now() };
  const next = [...drafts];
  next[idx] = updated;
  setDrafts(next);
  res.json(updated);
});

router.post("/:id/publish", authorize("publish"), async (req, res) => {
  await delay(300);
  const idx = drafts.findIndex((d) => d.id === req.params.id);
  if (idx === -1) return res.status(404).json({ message: "Draft not found." });
  const updated = { ...drafts[idx], status: "published", scheduledAt: null, updatedAt: Date.now() };
  const next = [...drafts];
  next[idx] = updated;
  setDrafts(next);
  res.json(updated);
});

router.delete("/:id", authorize("delete"), async (req, res) => {
  await delay(250);
  const exists = drafts.some((d) => d.id === req.params.id);
  if (!exists) return res.status(404).json({ message: "Draft not found." });
  setDrafts(drafts.filter((d) => d.id !== req.params.id));
  res.status(204).end();
});

export default router;
