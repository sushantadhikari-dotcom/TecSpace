import { useState } from "react";
import { useNavigate } from "react-router-dom";
import api, { errMsg } from "../api.js";
import { useAuth } from "../AuthContext.jsx";

export default function Account() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const forced = !!user.mustChangePassword;
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
      setForm({ currentPassword: "", newPassword: "", confirm: "" });
      if (forced) {
        setOk("Password changed. Please log in again with your new password.");
        setTimeout(() => { logout(); navigate("/login"); }, 1500);
      } else {
        setOk("Password changed.");
      }
    } catch (err) { setError(errMsg(err)); }
  };

  return (
    <>
      <h1 className="page-title">Account</h1>

      {forced && (
        <div className="alert error" role="alert">
          <strong>You are using a temporary password.</strong> Choose your own password now. Nothing else works until you do.
        </div>
      )}

      <section className="panel">
        <h2>{user.firstName} {user.lastName}</h2>
        <p className="muted">{user.universityEmail}</p>
      </section>

      <section className="panel">
        <h2>Change password</h2>
        {error && <div className="alert error" role="alert">{error}</div>}
        {ok && <div className="alert success" role="status">{ok}</div>}
        <form onSubmit={submit} className="stack" style={{ maxWidth: 420 }}>
          <label>{forced ? "Temporary password" : "Current password"}
            <input type="password" value={form.currentPassword} onChange={(e) => setForm({ ...form, currentPassword: e.target.value })} required />
          </label>
          <label>New password
            <input type="password" minLength={8} value={form.newPassword} onChange={(e) => setForm({ ...form, newPassword: e.target.value })} required />
          </label>
          <label>Repeat new password
            <input type="password" minLength={8} value={form.confirm} onChange={(e) => setForm({ ...form, confirm: e.target.value })} required />
          </label>
          <p className="muted small" style={{ margin: 0 }}>At least 8 characters, with a letter and a number.</p>
          <div><button className="btn primary">Change password</button></div>
        </form>
      </section>
    </>
  );
}
