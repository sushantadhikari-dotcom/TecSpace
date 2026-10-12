import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../AuthContext.jsx";
import { errMsg } from "../api.js";
import Logo from "../components/Logo.jsx";

export default function Signup() {
  const { signup } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({
    firstName: "", lastName: "", universityEmail: "", mobileNumber: "",
    faculty: "BSIT", session: "FALL", password: "",
  });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setError("");
    setBusy(true);
    try {
      await signup(form);
      navigate("/student/financials");
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
        <h1>Create your TecSpace account.</h1>
        <p>Use your university email. You can apply for your library card right after signing up.</p>
      </div>
      <form className="auth-form" onSubmit={submit}>
        <h2>Sign up</h2>
        {error && <div className="alert error" role="alert">{error}</div>}
        <div className="row">
          <label>First name<input value={form.firstName} onChange={set("firstName")} required /></label>
          <label>Last name<input value={form.lastName} onChange={set("lastName")} required /></label>
        </div>
        <label>University email<input type="email" value={form.universityEmail} onChange={set("universityEmail")} required /></label>
        <label>Mobile number<input value={form.mobileNumber} onChange={set("mobileNumber")} placeholder="+1234567890" required /></label>
        <div className="row">
          <label>Faculty
            <select value={form.faculty} onChange={set("faculty")}>
              <option>BSIT</option><option>BBA</option><option>MSCIT</option>
            </select>
          </label>
          <label>Session
            <select value={form.session} onChange={set("session")}>
              <option>SPRING</option><option>FALL</option><option>SUMMER</option>
            </select>
          </label>
        </div>
        <label>Password (8+ characters)
          <input type="password" minLength={8} value={form.password} onChange={set("password")} required />
        </label>
        <button className="btn primary" disabled={busy}>{busy ? "Creating account…" : "Create account"}</button>
        <p className="muted">Already registered? <Link to="/login">Log in</Link></p>
      </form>
    </div>
  );
}
