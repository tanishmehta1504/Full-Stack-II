import { useDispatch, useSelector } from "react-redux";
import { setTab } from "../store/uiSlice.js";
import { can } from "../constants.js";

export default function Sidebar() {
  const dispatch = useDispatch();
  const activeTab = useSelector((s) => s.ui.activeTab);
  const role = useSelector((s) => s.auth.user && s.auth.user.role);
  const logs = useSelector((s) => s.log.entries);

  const items = [
    { id: "compose", label: "Compose", ic: ">_", show: can(role, "create") || can(role, "view") },
    { id: "drafts", label: "Drafts", ic: "≡", show: true },
    { id: "calendar", label: "Calendar", ic: "▦", show: true },
    { id: "admin", label: "Admin panel", ic: "#", show: true, locked: role !== "admin" },
  ];

  return (
    <aside className="sidebar">
      <div className="sb-brand"><span className="dot" />SYS.POST</div>
      <nav className="sb-nav">
        {items.filter((i) => i.show).map((i) => (
          <button
            key={i.id}
            className={"sb-link" + (activeTab === i.id ? " active" : "") + (i.locked ? " sb-lock" : "")}
            onClick={() => dispatch(setTab(i.id))}
          >
            <span className="ic">{i.ic}</span>{i.label}
            {i.locked && <span style={{ marginLeft: "auto", fontSize: "10px" }}>🔒</span>}
          </button>
        ))}
      </nav>
      <div className="sb-foot">
        <div className="log-panel">
          <div className="lh">// system log</div>
          {logs.length === 0
            ? <div className="entry">idle</div>
            : logs.map((l, idx) => <div className="entry" key={idx}><span className="t">› </span>{l.type}</div>)}
        </div>
      </div>
    </aside>
  );
}
