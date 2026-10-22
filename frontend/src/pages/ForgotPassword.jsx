import { useState } from "react";
import { Link } from "react-router-dom";
import api, { errMsg } from "../api.js";
import Logo from "../components/Logo.jsx";

export default function ForgotPassword() {
  const [email, setEmail] = useState("");
  const [done, setDone] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError(""); setBusy(true);
    try {
      const r = await api.post("/public/forgot-password", { email });
      setDone(r.data.message);
    } catch (err) { setError(errMsg(err)); }
    finally { setBusy(false); }
  };

  return (
    <div className="pay-page">
      <Logo size={30} />
      <div className="panel pay-card stack" style={{ maxWidth: 440 }}>
        <h1>Forgot your password?</h1>
        {done ? (
          <>
            <div className="alert success" role="status">{done}</div>
            <p className="muted small">The link works for 30 minutes. If you do not see the email, check your spam folder.</p>
          </>
        ) : (
          <form onSubmit={submit} className="stack">
            <p className="muted">Enter your university email and we will send you a link to choose a new password.</p>
            {error && <div className="alert error" role="alert">{error}</div>}
            <label>University email<input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus /></label>
            <button className="btn primary" disabled={busy}>{busy ? "Sending…" : "Send reset link"}</button>
          </form>
        )}
        <p className="muted small"><Link to="/login">Back to log in</Link></p>
      </div>
    </div>
  );
}
