import { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { loggedOut } from "../store/authSlice.js";
import { fmtClock, initials } from "../utils/dates.js";

const TITLES = { compose: "Compose post", drafts: "Drafts", calendar: "Calendar", admin: "Admin panel" };

export default function Topbar() {
  const dispatch = useDispatch();
  const user = useSelector((s) => s.auth.user);
  const claims = useSelector((s) => s.auth.claims);
  const activeTab = useSelector((s) => s.ui.activeTab);
  const [remaining, setRemaining] = useState(claims ? claims.exp * 1000 - Date.now() : 0);

  useEffect(() => {
    if (!claims) return;
    const expMs = claims.exp * 1000; // JWT `exp` is seconds since epoch
    const iv = setInterval(() => {
      const r = expMs - Date.now();
      setRemaining(r);
      if (r <= 0) { clearInterval(iv); dispatch(loggedOut()); }
    }, 1000);
    return () => clearInterval(iv);
  }, [claims, dispatch]);

  const warn = remaining < 60000;

  return (
    <div className="topbar">
      <div className="tb-title">{TITLES[activeTab] || ""}</div>
      <div className="tb-right">
        <div className={"session-chip" + (warn ? " warn" : "")}>⏱ session {fmtClock(remaining)}</div>
        <div className="user-chip">
          <div className="user-avatar">{initials(user.name)}</div>
          <div>
            <div style={{ fontSize: "12.5px", fontWeight: 600 }}>{user.name}</div>
            <span className={"role-badge role-" + user.role}>{user.role}</span>
          </div>
        </div>
        <button className="btn btn-ghost btn-sm" onClick={() => dispatch(loggedOut())}>Log out</button>
      </div>
    </div>
  );
}
