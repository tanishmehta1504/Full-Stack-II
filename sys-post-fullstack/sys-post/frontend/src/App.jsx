import { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { bootstrap } from "./store/authSlice.js";
import LoginScreen from "./components/LoginScreen.jsx";
import Dashboard from "./components/Dashboard.jsx";

export default function App() {
  const dispatch = useDispatch();
  const status = useSelector((s) => s.auth.status);
  const token = useSelector((s) => s.auth.token);

  useEffect(() => { dispatch(bootstrap()); }, [dispatch]);

  if (status === "booting") {
    return (
      <div className="boot-wrap">
        <span className="spinner dark" />
        verifying session…
      </div>
    );
  }

  return token ? <Dashboard /> : <LoginScreen />;
}
