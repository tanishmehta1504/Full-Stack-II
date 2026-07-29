import { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { fetchDrafts } from "../store/draftsSlice.js";
import { selectCalendarMap } from "../store/selectors.js";
import { setTab, setPrefillSchedule } from "../store/uiSlice.js";
import { can, PLATFORM_LABELS } from "../constants.js";
import { ymdLocal, fmtDayLabel, fmtTime, MONTH_NAMES, DOW_NAMES } from "../utils/dates.js";

export default function CalendarView() {
  const dispatch = useDispatch();
  const role = useSelector((s) => s.auth.user.role);
  const status = useSelector((s) => s.drafts.status);
  const calMap = useSelector(selectCalendarMap);

  const today = new Date();
  const todayKey = ymdLocal(Date.now());
  const [cursor, setCursor] = useState({ y: today.getFullYear(), m: today.getMonth() });
  const [selectedKey, setSelectedKey] = useState(todayKey);

  useEffect(() => { if (status === "idle") dispatch(fetchDrafts()); }, [status, dispatch]);

  function shiftMonth(delta) {
    setCursor((c) => {
      const d = new Date(c.y, c.m + delta, 1);
      return { y: d.getFullYear(), m: d.getMonth() };
    });
  }
  function goToday() {
    setCursor({ y: today.getFullYear(), m: today.getMonth() });
    setSelectedKey(todayKey);
  }

  const firstOfMonth = new Date(cursor.y, cursor.m, 1);
  const startOffset = firstOfMonth.getDay();
  const daysInMonth = new Date(cursor.y, cursor.m + 1, 0).getDate();
  const daysInPrevMonth = new Date(cursor.y, cursor.m, 0).getDate();

  const cells = [];
  for (let i = 0; i < startOffset; i++) {
    const dayNum = daysInPrevMonth - startOffset + 1 + i;
    const d = new Date(cursor.y, cursor.m - 1, dayNum);
    cells.push({ key: ymdLocal(d.getTime()), dayNum, inMonth: false });
  }
  for (let dayNum = 1; dayNum <= daysInMonth; dayNum++) {
    const d = new Date(cursor.y, cursor.m, dayNum);
    cells.push({ key: ymdLocal(d.getTime()), dayNum, inMonth: true });
  }
  while (cells.length % 7 !== 0 || cells.length < 42) {
    const idx = cells.length - (startOffset + daysInMonth);
    const d = new Date(cursor.y, cursor.m + 1, idx + 1);
    cells.push({ key: ymdLocal(d.getTime()), dayNum: idx + 1, inMonth: false });
    if (cells.length >= 42) break;
  }

  const selectedEvents = calMap[selectedKey] || [];

  function scheduleOnSelectedDay() {
    const [y, m, d] = selectedKey.split("-").map(Number);
    const target = new Date(y, m - 1, d, 12, 0, 0).getTime();
    dispatch(setPrefillSchedule(target));
    dispatch(setTab("compose"));
  }

  return (
    <div>
      <div className="card">
        <div className="cal-head">
          <div className="card-title">Post calendar</div>
          <div className="cal-nav">
            <button className="cal-navbtn" onClick={() => shiftMonth(-1)}>‹</button>
            <div className="cal-month">{MONTH_NAMES[cursor.m]} {cursor.y}</div>
            <button className="cal-navbtn" onClick={() => shiftMonth(1)}>›</button>
            <button className="btn btn-ghost btn-sm" onClick={goToday}>Today</button>
          </div>
        </div>

        <div className="cal-grid">
          {DOW_NAMES.map((dow) => <div className="cal-dow" key={dow}>{dow}</div>)}
          {cells.map((cell) => {
            const events = calMap[cell.key] || [];
            const shown = events.slice(0, 2);
            const overflow = events.length - shown.length;
            return (
              <div
                key={cell.key}
                className={"cal-cell" + (cell.inMonth ? "" : " out") + (cell.key === todayKey ? " today" : "") + (cell.key === selectedKey ? " selected" : "")}
                onClick={() => setSelectedKey(cell.key)}
              >
                <div className="cal-daynum">{cell.dayNum}</div>
                {shown.map((ev) => <div className={"cal-chip " + ev.status} key={ev.id}>{ev.content.slice(0, 18)}</div>)}
                {overflow > 0 && <div className="cal-more">+{overflow} more</div>}
              </div>
            );
          })}
        </div>
      </div>

      <div className="card">
        <div className="card-head">
          <div className="card-title">{fmtDayLabel(selectedKey)}</div>
          {can(role, "create") && <button className="btn btn-cyan btn-sm" onClick={scheduleOnSelectedDay}>+ Schedule post</button>}
        </div>
        {selectedEvents.length === 0
          ? <div className="empty">Nothing scheduled or published on this day.</div>
          : selectedEvents.map((ev) => (
            <div className="day-panel-item" key={ev.id}>
              <div className="day-panel-time">{fmtTime(ev.scheduledAt || ev.updatedAt)}</div>
              <div className="draft-content">{ev.content}</div>
              <div className="tag-row">
                <span className={"status-tag status-" + ev.status}>{ev.status}</span>
                {ev.platforms.map((p) => <span className="tag" key={p}>{PLATFORM_LABELS[p]}</span>)}
              </div>
            </div>
          ))}
      </div>
    </div>
  );
}
