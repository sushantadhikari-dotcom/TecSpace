import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../AuthContext.jsx";
import { errMsg } from "../api.js";
import Logo from "../components/Logo.jsx";

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError("");
    setBusy(true);
    try {
      const u = await login(email, password);
      navigate(u.role === "ROLE_ADMIN" ? "/admin" : "/student/dashboard");
    } catch (err) {
      setError(errMsg(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="auth">
      <div className="auth-intro">
        <Logo size={34} />
        <h1>Your fees, grades and library card in one place.</h1>
        <p>
          Sign in with your university email to see where you stand this
          trimester.
        </p>
      </div>
      <form className="auth-form" onSubmit={submit}>
        <h2>Log in</h2>
        {error && (
          <div className="alert error" role="alert">
            {error}
          </div>
        )}
        <label>
          University email
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            autoFocus
          />
        </label>
        <label>
          Password
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </label>
        <button className="btn primary" disabled={busy}>
          {busy ? "Logging in…" : "Log in"}
        </button>
        <p className="muted">
          <Link to="/forgot-password">Forgot your password?</Link>
        </p>
        <p className="muted">
          New student? <Link to="/signup">Create your account</Link>
        </p>
        <p className="muted">
          <Link to="/apply">Apply for admission</Link> ·{" "}
          <Link to="/track">Track your application</Link>
        </p>
      </form>
    </div>
  );  
}
