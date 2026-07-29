import { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { fetchDrafts, setPlatformFilter } from "../store/draftsSlice.js";
import { selectFilteredDrafts, selectCountsByPlatform, selectPlatformFilter, recomputeCount } from "../store/selectors.js";
import { PLATFORM_LIMITS, PLATFORM_LABELS } from "../constants.js";
import DraftItem from "./DraftItem.jsx";

export default function DraftsView({ toast }) {
  const dispatch = useDispatch();
  const role = useSelector((s) => s.auth.user.role);
  const status = useSelector((s) => s.drafts.status);
  const filter = useSelector(selectPlatformFilter);
  const filtered = useSelector(selectFilteredDrafts);
  const counts = useSelector(selectCountsByPlatform);

  useEffect(() => { if (status === "idle") dispatch(fetchDrafts()); }, [status, dispatch]);

  return (
    <div className="card">
      <div className="card-head">
        <div className="card-title">All posts</div>
        <div className="card-sub">selector recomputed {recomputeCount}× (reselect memoization)</div>
      </div>

      <div className="filter-row">
        <span className="pill">filter:</span>
        <select value={filter} onChange={(e) => dispatch(setPlatformFilter(e.target.value))}>
          <option value="all">All platforms</option>
          {Object.keys(PLATFORM_LIMITS).map((p) => (
            <option key={p} value={p}>{PLATFORM_LABELS[p]} ({counts[p]})</option>
          ))}
        </select>
      </div>

      {status === "loading" && <div className="empty"><span className="spinner dark" />&nbsp;&nbsp;loading drafts…</div>}
      {status === "succeeded" && filtered.length === 0 && <div className="empty">No posts match this filter.</div>}

      <div className="draft-list">
        {filtered.map((d) => <DraftItem key={d.id} draft={d} role={role} toast={toast} />)}
      </div>
    </div>
  );
}
