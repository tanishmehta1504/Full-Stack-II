export const PLATFORM_LIMITS = { twitter: 280, instagram: 2200, linkedin: 3000, facebook: 5000 };
export const PLATFORM_LABELS = { twitter: "X / Twitter", instagram: "Instagram", linkedin: "LinkedIn", facebook: "Facebook" };

// Mirrors backend/db.js — used for permission-based UI rendering. The server
// is still the source of truth and re-checks every request; this copy only
// controls what the UI shows/hides.
export const PERMISSIONS = {
  admin: ["create", "edit", "delete", "publish", "manageUsers", "view"],
  editor: ["create", "edit", "delete", "publish", "view"],
  viewer: ["view"],
};

export function can(role, action) {
  return !!(PERMISSIONS[role] && PERMISSIONS[role].includes(action));
}
