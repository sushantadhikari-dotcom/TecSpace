import { useState } from "react";
import api, { errMsg } from "../api.js";
import { useAuth } from "../AuthContext.jsx";

export default function Account() {
  const { user } = useAuth();
  const [form, setForm] = useState({ currentPassword: "", newPassword: "", confirm: "" });
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");

  const submit = async (e) => {
    e.preventDefault();
    setError(""); setOk("");
    if (form.newPassword !== form.confirm) {
      setError("The two new passwords do not match");
      return;
    }
    try {
      await api.post("/account/change-password", { currentPassword: form.currentPassword, newPassword: form.newPassword });
      setOk("Password changed.");
      setForm({ currentPassword: "", newPassword: "", confirm: "" });
    } catch (err) { setError(errMsg(err)); }
  };

  return (
    <>
      <h1 className="page-title">Account</h1>
      <section className="panel">
        <h2>{user.firstName} {user.lastName}</h2>
        <p className="muted">{user.universityEmail}</p>
      </section>

      <section className="panel">
        <h2>Change password</h2>
        {error && <div className="alert error" role="alert">{error}</div>}
        {ok && <div className="alert success" role="status">{ok}</div>}
        <form onSubmit={submit} className="stack" style={{ maxWidth: 420 }}>
          <label>Current password
            <input type="password" value={form.currentPassword} onChange={(e) => setForm({ ...form, currentPassword: e.target.value })} required />
          </label>
          <label>New password (8+ characters)
            <input type="password" minLength={8} value={form.newPassword} onChange={(e) => setForm({ ...form, newPassword: e.target.value })} required />
          </label>
          <label>Repeat new password
            <input type="password" minLength={8} value={form.confirm} onChange={(e) => setForm({ ...form, confirm: e.target.value })} required />
          </label>
          <div><button className="btn primary">Change password</button></div>
        </form>
        <p className="muted small">If an administrator gave you a temporary password, change it here now.</p>
      </section>
    </>
  );
}
