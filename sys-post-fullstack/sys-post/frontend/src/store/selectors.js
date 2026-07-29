import { createSelector } from "reselect";
import { draftsAdapter } from "./draftsSlice.js";
import { ymdLocal } from "../utils/dates.js";

export let recomputeCount = 0; // exposed on-screen (Drafts view) to prove memoization skips recompute

const draftAdapterSelectors = draftsAdapter.getSelectors((state) => state.drafts);
export const selectAllDraftsRaw = draftAdapterSelectors.selectAll;
export const selectPlatformFilter = (state) => state.drafts.platformFilter;

export const selectFilteredDrafts = createSelector(
  [selectAllDraftsRaw, selectPlatformFilter],
  (drafts, filter) => {
    recomputeCount += 1;
    return filter === "all" ? drafts : drafts.filter((d) => d.platforms.includes(filter));
  }
);

export const selectCountsByPlatform = createSelector([selectAllDraftsRaw], (drafts) => {
  const counts = { twitter: 0, instagram: 0, linkedin: 0, facebook: 0 };
  drafts.forEach((d) => d.platforms.forEach((p) => { if (counts[p] !== undefined) counts[p] += 1; }));
  return counts;
});

// Groups posts by local calendar day (scheduled date, or publish date if published without a schedule).
export const selectCalendarMap = createSelector([selectAllDraftsRaw], (drafts) => {
  const map = {};
  drafts.forEach((d) => {
    const ts = d.scheduledAt || (d.status === "published" ? d.updatedAt : null);
    if (!ts) return;
    const key = ymdLocal(ts);
    (map[key] = map[key] || []).push(d);
  });
  Object.values(map).forEach((list) => list.sort((a, b) => (a.scheduledAt || a.updatedAt) - (b.scheduledAt || b.updatedAt)));
  return map;
});

export function getRecomputeCount() { return recomputeCount; }
