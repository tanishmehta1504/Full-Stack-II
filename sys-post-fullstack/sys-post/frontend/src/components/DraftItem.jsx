import { useState } from "react";
import { useDispatch } from "react-redux";
import { deleteDraft, publishDraft, saveDraft } from "../store/draftsSlice.js";
import { setEditingId, setTab } from "../store/uiSlice.js";
import { can, PLATFORM_LABELS } from "../constants.js";
import { timeAgo, toInputValue, fmtDayLabel, fmtTime, ymdLocal } from "../utils/dates.js";

export default function DraftItem({ draft, role, toast }) {
  const dispatch = useDispatch();
  const [scheduling, setScheduling] = useState(false);
  const [scheduleVal, setScheduleVal] = useState(draft.scheduledAt ? toInputValue(draft.scheduledAt) : toInputValue(Date.now() + 3600000));

  async function handleDelete() {
    const result = await dispatch(deleteDraft(draft.id));
    if (deleteDraft.fulfilled.match(result)) toast("Draft deleted.", "ok");
    else toast(result.payload || "Could not delete draft.", "err");
  }
  async function handlePublish() {
    const result = await dispatch(publishDraft(draft.id));
    if (publishDraft.fulfilled.match(result)) toast("Post published.", "ok");
    else toast(result.payload || "Could not publish post.", "err");
  }
  function handleEdit() {
    dispatch(setEditingId(draft.id));
    dispatch(setTab("compose"));
  }
  async function confirmSchedule() {
    if (!scheduleVal) return;
    const result = await dispatch(saveDraft({ ...draft, scheduledAt: new Date(scheduleVal).getTime(), status: "scheduled" }));
    setScheduling(false);
    if (saveDraft.fulfilled.match(result)) toast("Post scheduled.", "ok");
    else toast(result.payload || "Could not schedule post.", "err");
  }
  async function unschedule() {
    const result = await dispatch(saveDraft({ ...draft, scheduledAt: null, status: "draft" }));
    if (saveDraft.fulfilled.match(result)) toast("Post unscheduled.", "ok");
    else toast(result.payload || "Could not unschedule post.", "err");
  }

  return (
    <div className="draft-item">
      <div className="draft-top">
        <div>
          <div className="draft-content">{draft.content}</div>
          <div className="tag-row">
            <span className={"status-tag status-" + draft.status}>{draft.status}</span>
            {draft.platforms.map((p) => <span className="tag" key={p}>{PLATFORM_LABELS[p]}</span>)}
          </div>
          <div className="draft-meta">
            {draft.status === "scheduled" && draft.scheduledAt
              ? "scheduled for " + fmtDayLabel(ymdLocal(draft.scheduledAt)) + " · " + fmtTime(draft.scheduledAt)
              : "updated " + timeAgo(draft.updatedAt)}
          </div>
          {scheduling && (
            <div className="schedule-inline">
              <input type="datetime-local" value={scheduleVal} onChange={(e) => setScheduleVal(e.target.value)} />
              <button className="btn btn-cyan btn-sm" onClick={confirmSchedule}>Set</button>
              <button className="btn btn-ghost btn-sm" onClick={() => setScheduling(false)}>Cancel</button>
            </div>
          )}
        </div>
        <div className="draft-actions">
          {can(role, "edit") && <button className="btn btn-ghost btn-sm" onClick={handleEdit}>Edit</button>}
          {can(role, "edit") && draft.status !== "published" && !scheduling && (
            <button className="btn btn-ghost btn-sm" onClick={() => setScheduling(true)}>{draft.status === "scheduled" ? "Reschedule" : "Schedule"}</button>
          )}
          {can(role, "edit") && draft.status === "scheduled" && <button className="btn btn-ghost btn-sm" onClick={unschedule}>Unschedule</button>}
          {can(role, "publish") && draft.status !== "published" && <button className="btn btn-cyan btn-sm" onClick={handlePublish}>Publish</button>}
          {can(role, "delete") && <button className="btn btn-danger btn-sm" onClick={handleDelete}>Delete</button>}
        </div>
      </div>
    </div>
  );
}
