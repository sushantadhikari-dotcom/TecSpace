import { useState } from "react";
import { Link } from "react-router-dom";
import api, { errMsg } from "../api.js";
import Logo from "../components/Logo.jsx";

export default function Apply() {
  const [form, setForm] = useState({
    firstName: "", lastName: "", email: "", mobileNumber: "", faculty: "BSIT", session: "FALL",
    previousSchool: "", lastScore: "", statement: "",
  });
  const [error, setError] = useState("");
  const [done, setDone] = useState(null);
  const [busy, setBusy] = useState(false);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setError(""); setBusy(true);
    try {
      const r = await api.post("/public/applications", { ...form, lastScore: Number(form.lastScore) });
      setDone(r.data);
    } catch (err) { setError(errMsg(err)); }
    finally { setBusy(false); }
  };

  if (done) {
    return (
      <div className="pay-page">
        <Logo size={30} />
        <div className="panel pay-card">
          <h1>Application received</h1>
          <p>Thank you, {done.name}. Keep this code to follow your application:</p>
          <div className="temp-pass">{done.trackingCode}</div>
          <p className="muted">
            Bring or send your documents to the admissions office: {done.documents.map((d) => d.name).join(", ")}.
            We will verify them and update your status.
          </p>
          <Link to="/track">Track your application</Link>
        </div>
      </div>
    );
  }

  return (
    <div className="pay-page">
      <Logo size={30} />
      <form className="panel pay-card stack" onSubmit={submit} style={{ maxWidth: 560 }}>
        <h1>Apply to TecSpace</h1>
        {error && <div className="alert error" role="alert">{error}</div>}
        <div className="row">
          <label>First name<input value={form.firstName} onChange={set("firstName")} required /></label>
          <label>Last name<input value={form.lastName} onChange={set("lastName")} required /></label>
        </div>
        <label>Email<input type="email" value={form.email} onChange={set("email")} required /></label>
        <label>Mobile number<input value={form.mobileNumber} onChange={set("mobileNumber")} placeholder="+9779800000000" required /></label>
        <div className="row">
          <label>Programme
            <select value={form.faculty} onChange={set("faculty")}><option>BSIT</option><option>BBA</option><option>MSCIT</option></select>
          </label>
          <label>Intake
            <select value={form.session} onChange={set("session")}><option>SPRING</option><option>FALL</option><option>SUMMER</option></select>
          </label>
        </div>
        <div className="row">
          <label>Previous school or college<input value={form.previousSchool} onChange={set("previousSchool")} required /></label>
          <label>Last score (%)<input type="number" min="0" max="100" step="0.01" value={form.lastScore} onChange={set("lastScore")} required /></label>
        </div>
        <label>Why do you want to join? (optional)
          <textarea rows="4" value={form.statement} onChange={set("statement")} />
        </label>
        <button className="btn primary" disabled={busy}>{busy ? "Submitting…" : "Submit application"}</button>
        <p className="muted small"><Link to="/track">Already applied? Track your application</Link> · <Link to="/login">Log in</Link></p>
      </form>
    </div>
  );
}
