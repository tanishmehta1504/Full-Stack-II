import bcrypt from "bcryptjs";
import crypto from "crypto";

/**
 * In-memory "database" for the experiment. Replace with a real database
 * (Postgres/Mongo/etc.) in a production build — the route handlers only
 * touch this module, so swapping storage means editing this file alone.
 */

export const PERMISSIONS = {
  admin: ["create", "edit", "delete", "publish", "manageUsers", "view"],
  editor: ["create", "edit", "delete", "publish", "view"],
  viewer: ["view"],
};

export function can(role, action) {
  return !!(PERMISSIONS[role] && PERMISSIONS[role].includes(action));
}

export const users = [
  { id: "u1", username: "admin", passwordHash: bcrypt.hashSync("admin123", 10), name: "Tanish Mehta", role: "admin" },
  { id: "u2", username: "editor", passwordHash: bcrypt.hashSync("editor123", 10), name: "Priya Singh", role: "editor" },
  { id: "u3", username: "viewer", passwordHash: bcrypt.hashSync("viewer123", 10), name: "Sam Lee", role: "viewer" },
];

export function publicUser(u) {
  return { id: u.id, username: u.username, name: u.name, role: u.role };
}

const now = Date.now();
export let drafts = [
  { id: "d1", content: "Shipping v2 of our onboarding flow this week — smoother, faster, fewer clicks.", platforms: ["twitter", "linkedin"], authorId: "u2", status: "draft", scheduledAt: null, createdAt: now - 86400000 * 2, updatedAt: now - 86400000 * 2 },
  { id: "d2", content: "Behind the scenes of our design system rebuild. Swipe through to see the before/after.", platforms: ["instagram"], authorId: "u2", status: "published", scheduledAt: null, createdAt: now - 86400000, updatedAt: now - 3600000 },
  { id: "d3", content: "Hiring: looking for a frontend engineer who loves state machines as much as we do.", platforms: ["linkedin", "twitter", "facebook"], authorId: "u1", status: "draft", scheduledAt: null, createdAt: now - 3600000 * 5, updatedAt: now - 3600000 * 5 },
  { id: "d4", content: "New feature drop next week — sneak peek of the dashboard redesign.", platforms: ["twitter", "instagram"], authorId: "u2", status: "scheduled", scheduledAt: now + 86400000 * 2 + 3600000 * 3, createdAt: now - 3600000, updatedAt: now - 3600000 },
  { id: "d5", content: "Quarterly product roundup — everything we shipped this quarter, in one thread.", platforms: ["linkedin"], authorId: "u1", status: "scheduled", scheduledAt: now + 86400000 * 5 + 3600000 * 6, createdAt: now - 7200000, updatedAt: now - 7200000 },
];

export function setDrafts(next) { drafts = next; }
export function newId() { return crypto.randomUUID(); }
