import { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { saveDraft } from "../store/draftsSlice.js";
import { selectAllDraftsRaw } from "../store/selectors.js";
import { setEditingId, setTab, setPrefillSchedule } from "../store/uiSlice.js";
import { PLATFORM_LIMITS, PLATFORM_LABELS, can } from "../constants.js";
import { toInputValue } from "../utils/dates.js";

export default function ComposeView({ toast }) {
  const dispatch = useDispatch();
  const role = useSelector((s) => s.auth.user.role);
  const editingId = useSelector((s) => s.ui.editingId);
  const prefillSchedule = useSelector((s) => s.ui.prefillSchedule);
  const drafts = useSelector(selectAllDraftsRaw);
  const editingDraft = editingId ? drafts.find((d) => d.id === editingId) : null;

  const [content, setContent] = useState(editingDraft ? editingDraft.content : "");
  const [platforms, setPlatforms] = useState(editingDraft ? editingDraft.platforms : []);
  const [schedule, setSchedule] = useState(editingDraft && editingDraft.scheduledAt ? toInputValue(editingDraft.scheduledAt) : "");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setContent(editingDraft ? editingDraft.content : "");
    setPlatforms(editingDraft ? editingDraft.platforms : []);
    setSchedule(editingDraft && editingDraft.scheduledAt ? toInputValue(editingDraft.scheduledAt) : "");
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [editingId]);

  // Coming from the calendar's "+ schedule post" action: prefill the date once, then clear it.
  useEffect(() => {
    if (prefillSchedule && !editingId) {
      setSchedule(toInputValue(prefillSchedule));
      dispatch(setPrefillSchedule(null));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [prefillSchedule]);

  const readOnly = !can(role, "create") && !can(role, "edit");

  function togglePlatform(p) {
    setPlatforms((prev) => (prev.includes(p) ? prev.filter((x) => x !== p) : [...prev, p]));
  }

  const overLimit = platforms.some((p) => content.length > PLATFORM_LIMITS[p]);
  const canSubmit = !readOnly && content.trim().length > 0 && platforms.length > 0 && !overLimit && !saving;

  async function handleSave(publishNow) {
    setSaving(true);
    const scheduledAt = schedule ? new Date(schedule).getTime() : null;
    const draftPayload = {
      id: editingId || undefined,
      content: content.trim(),
      platforms,
      scheduledAt: publishNow ? null : scheduledAt,
      status: publishNow ? "published" : (scheduledAt ? "scheduled" : "draft"),
    };
    const result = await dispatch(saveDraft(draftPayload));
    setSaving(false);
    if (saveDraft.fulfilled.match(result)) {
      dispatch(setEditingId(null));
      setContent(""); setPlatforms([]); setSchedule("");
      toast(publishNow ? "Post published." : (scheduledAt ? "Post scheduled." : "Draft saved."), "ok");
      dispatch(setTab("drafts"));
    } else {
      toast(result.payload || "Something went wrong.", "err");
    }
  }

  return (
    <div className="card">
      <div className="card-head">
        <div className="card-title">{editingId ? "Edit draft" : "New post"}</div>
        <div className="card-sub">// dynamic multi-platform composer</div>
      </div>

      {readOnly && <div className="hint">Your role ({role}) has read-only access. Composing is disabled.</div>}

      <textarea
        className="composer"
        placeholder="What do you want to share?"
        value={content}
        disabled={readOnly}
        onChange={(e) => setContent(e.target.value)}
      />

      <div className="platform-grid">
        {Object.keys(PLATFORM_LIMITS).map((p) => {
          const checked = platforms.includes(p);
          const limit = PLATFORM_LIMITS[p];
          const pct = Math.min(100, (content.length / limit) * 100);
          const over = content.length > limit;
          return (
            <div key={p} className={"platform-item" + (checked ? " checked" : "") + (checked && over ? " over" : "")}>
              <div className="platform-row">
                <label>
                  <input type="checkbox" checked={checked} disabled={readOnly} onChange={() => togglePlatform(p)} />
                  {PLATFORM_LABELS[p]}
                </label>
              </div>
              {checked && (
                <div>
                  <div className={"charcount" + (over ? " over" : "")}>{content.length} / {limit}{over ? "  ✕ over limit" : ""}</div>
                  <div className="bar"><div className={"bar-fill" + (over ? " over" : "")} style={{ width: pct + "%" }} /></div>
                </div>
              )}
            </div>
          );
        })}
      </div>

      {!readOnly && platforms.length === 0 && <div className="hint">Select at least one platform to enable saving.</div>}

      {!readOnly && (
        <div className="field" style={{ marginTop: "16px", maxWidth: "260px" }}>
          <label>Schedule (optional)</label>
          <input type="datetime-local" value={schedule} onChange={(e) => setSchedule(e.target.value)} />
        </div>
      )}

      <div className="row-actions">
        <button className="btn btn-ghost" disabled={!canSubmit} onClick={() => handleSave(false)}>
          {saving && <span className="spinner dark" />}
          {schedule ? "Save & schedule" : "Save as draft"}
        </button>
        {can(role, "publish") && (
          <button className="btn btn-primary" style={{ width: "auto" }} disabled={!canSubmit} onClick={() => handleSave(true)}>
            Publish now
          </button>
        )}
        {editingId && (
          <button className="btn btn-danger" onClick={() => { dispatch(setEditingId(null)); setContent(""); setPlatforms([]); setSchedule(""); }}>
            Cancel edit
          </button>
        )}
      </div>
    </div>
  );
}
