import { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import { apiFetch } from "../api.js";
import { PERMISSIONS } from "../constants.js";

export default function AdminView() {
  const role = useSelector((s) => s.auth.user.role);
  const token = useSelector((s) => s.auth.token);
  const [users, setUsers] = useState([]);
  const [status, setStatus] = useState("idle");

  useEffect(() => {
    if (role !== "admin") return;
    setStatus("loading");
    apiFetch("/api/users", { token })
      .then((data) => { setUsers(data); setStatus("succeeded"); })
      .catch(() => setStatus("failed"));
  }, [role, token]);

  // The server also re-checks this on every request (backend/middleware.js) —
  // this client-side check only controls what renders, it's not the real gate.
  if (role !== "admin") {
    return (
      <div className="denied">
        <div className="code">403</div>
        <p>Access denied — this route requires the admin role.</p>
      </div>
    );
  }

  return (
    <div>
      <div className="card">
        <div className="card-head">
          <div className="card-title">Users</div>
          <div className="card-sub">// GET /api/users — RBAC-protected on the server</div>
        </div>
        {status === "loading" && <div className="empty"><span className="spinner dark" />&nbsp;&nbsp;loading users…</div>}
        {status === "succeeded" && (
          <table className="admin-table">
            <thead><tr><th>Name</th><th>Username</th><th>Role</th></tr></thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id}>
                  <td>{u.name}</td>
                  <td>{u.username}</td>
                  <td><span className={"role-badge role-" + u.role}>{u.role}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div className="card">
        <div className="card-head"><div className="card-title">Permission matrix</div></div>
        <div className="perm-grid">
          {Object.entries(PERMISSIONS).map(([r, perms]) => (
            <div className="perm-card" key={r}>
              <h4>{r}</h4>
              <ul>{perms.map((p) => <li key={p}>{p}</li>)}</ul>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
