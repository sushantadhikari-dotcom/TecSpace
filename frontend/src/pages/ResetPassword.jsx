import { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import api, { errMsg } from "../api.js";
import Logo from "../components/Logo.jsx";

export default function ResetPassword() {
  const [params] = useSearchParams();
  const token = params.get("token") || "";
  const [pw, setPw] = useState("");
  const [again, setAgain] = useState("");
  const [error, setError] = useState("");
  const [done, setDone] = useState(false);
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError("");
    if (pw !== again) { setError("The two passwords do not match"); return; }
    setBusy(true);
    try {
      await api.post("/public/reset-password", { token, newPassword: pw });
      setDone(true);
    } catch (err) { setError(errMsg(err)); }
    finally { setBusy(false); }
  };

  return (
    <div className="pay-page">
      <Logo size={30} />
      <div className="panel pay-card stack" style={{ maxWidth: 440 }}>
        <h1>Choose a new password</h1>
        {!token && <div className="alert error" role="alert">This link is incomplete. Ask for a new one.</div>}
        {done ? (
          <>
            <div className="alert success" role="status">Your password was changed.</div>
            <Link className="btn primary" to="/login" style={{ textAlign: "center", textDecoration: "none" }}>Log in</Link>
          </>
        ) : (
          <form onSubmit={submit} className="stack">
            {error && <div className="alert error" role="alert">{error}</div>}
            <label>New password
              <input type="password" minLength={8} value={pw} onChange={(e) => setPw(e.target.value)} required autoFocus />
            </label>
            <label>Repeat the new password
              <input type="password" minLength={8} value={again} onChange={(e) => setAgain(e.target.value)} required />
            </label>
            <p className="muted small">At least 8 characters, with a letter and a number.</p>
            <button className="btn primary" disabled={busy || !token}>{busy ? "Saving…" : "Change password"}</button>
          </form>
        )}
        <p className="muted small"><Link to="/forgot-password">Ask for a new link</Link></p>
      </div>
    </div>
  );
}
