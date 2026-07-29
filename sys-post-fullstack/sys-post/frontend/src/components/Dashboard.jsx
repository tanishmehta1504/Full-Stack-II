import { useRef, useState } from "react";
import { useSelector } from "react-redux";
import Sidebar from "./Sidebar.jsx";
import Topbar from "./Topbar.jsx";
import ComposeView from "./ComposeView.jsx";
import DraftsView from "./DraftsView.jsx";
import CalendarView from "./CalendarView.jsx";
import AdminView from "./AdminView.jsx";

function useToast() {
  const [toast, setToast] = useState(null);
  const timerRef = useRef(null);
  function show(message, type) {
    clearTimeout(timerRef.current);
    setToast({ message, type: type || "ok" });
    timerRef.current = setTimeout(() => setToast(null), 2600);
  }
  return [toast, show];
}

export default function Dashboard() {
  const activeTab = useSelector((s) => s.ui.activeTab);
  const [toast, showToast] = useToast();

  let view;
  if (activeTab === "compose") view = <ComposeView toast={showToast} />;
  else if (activeTab === "drafts") view = <DraftsView toast={showToast} />;
  else if (activeTab === "calendar") view = <CalendarView />;
  else view = <AdminView />;

  return (
    <div className="shell">
      <Sidebar />
      <div>
        <Topbar />
        <div className="main">{view}</div>
      </div>
      {toast && <div className={"toast" + (toast.type === "err" ? " err" : "")}>{toast.message}</div>}
    </div>
  );
}
