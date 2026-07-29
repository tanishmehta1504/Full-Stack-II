export function timeAgo(ts) {
  const s = Math.floor((Date.now() - ts) / 1000);
  if (s < 60) return s + "s ago";
  if (s < 3600) return Math.floor(s / 60) + "m ago";
  if (s < 86400) return Math.floor(s / 3600) + "h ago";
  return Math.floor(s / 86400) + "d ago";
}

export function fmtClock(ms) {
  const total = Math.max(0, Math.floor(ms / 1000));
  const m = String(Math.floor(total / 60)).padStart(2, "0");
  const s = String(total % 60).padStart(2, "0");
  return `${m}:${s}`;
}

export function initials(name) {
  return name.split(" ").map((p) => p[0]).slice(0, 2).join("").toUpperCase();
}

// Local-date (not UTC) key so a post scheduled for "10pm local" lands on the right calendar cell.
export function ymdLocal(ts) {
  const d = new Date(ts);
  return d.getFullYear() + "-" + String(d.getMonth() + 1).padStart(2, "0") + "-" + String(d.getDate()).padStart(2, "0");
}

export function toInputValue(ts) {
  const d = new Date(ts);
  const pad = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export function fmtTime(ts) {
  const d = new Date(ts);
  let hr = d.getHours();
  const min = String(d.getMinutes()).padStart(2, "0");
  const ap = hr >= 12 ? "PM" : "AM";
  hr = hr % 12; if (hr === 0) hr = 12;
  return `${hr}:${min} ${ap}`;
}

export function fmtDayLabel(key) {
  const [y, m, d] = key.split("-").map(Number);
  return new Date(y, m - 1, d).toLocaleDateString(undefined, { weekday: "long", month: "long", day: "numeric" });
}

export const MONTH_NAMES = ["January","February","March","April","May","June","July","August","September","October","November","December"];
export const DOW_NAMES = ["Sun","Mon","Tue","Wed","Thu","Fri","Sat"];
