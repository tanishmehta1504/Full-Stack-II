import { useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { login } from "../store/authSlice.js";

export default function LoginScreen() {
  const dispatch = useDispatch();
  const status = useSelector((s) => s.auth.status);
  const error = useSelector((s) => s.auth.error);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");

  function submit(e) {
    e.preventDefault();
    dispatch(login({ username, password }));
  }
  function fillDemo(u, p) { setUsername(u); setPassword(p); }

  return (
    <div className="login-wrap">
      <div className="login-card">
        <div className="login-brand"><span className="dot" />SYS.POST — SECURE LOGIN</div>
        <div className="login-title">Sign in</div>
        <div className="login-sub">JWT-authenticated session with role-based access control.</div>

        {error && <div className="login-error">✕ {error}</div>}

        <form onSubmit={submit}>
          <div className="field">
            <label>Username</label>
            <input value={username} onChange={(e) => setUsername(e.target.value)} placeholder="e.g. editor" autoComplete="username" />
          </div>
          <div className="field">
            <label>Password</label>
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="••••••••" autoComplete="current-password" />
          </div>
          <button className="btn btn-primary" type="submit" disabled={status === "loading"}>
            {status === "loading" && <span className="spinner" />}
            {status === "loading" ? "Verifying…" : "Sign in"}
          </button>
        </form>

        <div className="demo-creds">
          <div><span>Admin</span><b>admin / admin123</b><button onClick={() => fillDemo("admin", "admin123")}>use</button></div>
          <div><span>Editor</span><b>editor / editor123</b><button onClick={() => fillDemo("editor", "editor123")}>use</button></div>
          <div><span>Viewer</span><b>viewer / viewer123</b><button onClick={() => fillDemo("viewer", "viewer123")}>use</button></div>
        </div>
      </div>
    </div>
  );
}
