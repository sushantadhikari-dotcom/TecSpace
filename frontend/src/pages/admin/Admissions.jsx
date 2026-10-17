import { useEffect, useState } from "react";
import api, { errMsg } from "../../api.js";

const STATUS = {
  SUBMITTED: ["Under review", "warn"], APPROVED: ["Approved", "ok"], WAITLISTED: ["Waiting list", "warn"],
  REJECTED: ["Rejected", "bad"], ENROLLED: ["Enrolled", "ok"],
};

export default function Admissions() {
  const [tab, setTab] = useState("apps");
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");
  const [secret, setSecret] = useState(null);

  return (
    <>
      <h1 className="page-title">Admissions</h1>
      {error && <div className="alert error" role="alert">{error}</div>}
      {ok && <div className="alert success" role="status">{ok}</div>}
      {secret && (
        <div className="alert success" role="status">
          Student account created for <strong>{secret.who}</strong>. Temporary password (shown once):
          <div className="temp-pass">{secret.password}</div>
          Give it to the student and ask them to change it from the Account page.
        </div>
      )}
      <div className="tabs" role="tablist">
        <button role="tab" aria-selected={tab === "apps"} className={tab === "apps" ? "active" : ""} onClick={() => setTab("apps")}>Applications</button>
        <button role="tab" aria-selected={tab === "seats"} className={tab === "seats" ? "active" : ""} onClick={() => setTab("seats")}>Seats</button>
      </div>
      {tab === "apps" ? <Applications setError={setError} setOk={setOk} setSecret={setSecret} /> : <Seats setError={setError} setOk={setOk} />}
    </>
  );
}

function Applications({ setError, setOk, setSecret }) {
  const [list, setList] = useState(null);
  const [status, setStatus] = useState("");

  const load = () =>
    api.get("/admin/applications", { params: status ? { status } : {} })
      .then((r) => setList(r.data)).catch((e) => setError(errMsg(e)));
  useEffect(() => { load(); }, [status]);

  return (
    <>
      <section className="panel">
        <label style={{ maxWidth: 260 }}>Show
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">All applications</option>
            {Object.entries(STATUS).map(([k, v]) => <option key={k} value={k}>{v[0]}</option>)}
          </select>
        </label>
        <p className="muted small">Applicants apply at <strong>/apply</strong> without logging in and follow their status at <strong>/track</strong>.</p>
      </section>

      {!list ? <p className="muted">Loading…</p> : list.length === 0 ? (
        <section className="panel"><p className="muted">No applications here.</p></section>
      ) : list.map((a) => (
        <AppCard key={a.id} app={a} reload={load} setError={setError} setOk={setOk} setSecret={setSecret} />
      ))}
    </>
  );
}

function AppCard({ app, reload, setError, setOk, setSecret }) {
  const [docs, setDocs] = useState(app.documents.map((d) => ({ ...d })));
  const [remarks, setRemarks] = useState("");
  const [email, setEmail] = useState("");
  const [text, cls] = STATUS[app.status] || ["", ""];
  const closed = app.status === "REJECTED" || app.status === "ENROLLED";

  const run = async (fn, message) => {
    setError(""); setOk("");
    try { await fn(); if (message) setOk(message); reload(); }
    catch (err) { setError(errMsg(err)); }
  };

  const setDoc = (i, patch) => setDocs(docs.map((d, n) => (n === i ? { ...d, ...patch } : d)));

  return (
    <section className="panel app-card">
      <div className="panel-head">
        <div>
          <strong>{app.firstName} {app.lastName}</strong> <span className="pill">{app.faculty} · {app.session}</span>
          <div className="small muted">{app.email} · {app.mobileNumber} · {app.trackingCode}</div>
        </div>
        <span className={`pill ${cls}`}>{text}</span>
      </div>
      <p className="small">
        {app.previousSchool} · last score {app.lastScore}%
        {app.statement && <><br /><span className="muted">“{app.statement}”</span></>}
      </p>

      <table>
        <thead><tr><th>Document</th><th>Check</th><th>Note</th></tr></thead>
        <tbody>
          {docs.map((d, i) => (
            <tr key={d.name}>
              <td>{d.name}</td>
              <td>
                <select value={d.status} disabled={closed} onChange={(e) => setDoc(i, { status: e.target.value })}>
                  <option value="PENDING">Waiting</option><option value="VERIFIED">Verified</option><option value="REJECTED">Problem</option>
                </select>
              </td>
              <td><input value={d.note || ""} disabled={closed} onChange={(e) => setDoc(i, { note: e.target.value })} /></td>
            </tr>
          ))}
        </tbody>
      </table>

      {!closed && (
        <div className="actions" style={{ marginTop: "0.75rem" }}>
          <button className="btn ghost" onClick={() => run(() => api.put(`/admin/applications/${app.id}/documents`, { documents: docs }), "Documents saved.")}>Save document checks</button>
        </div>
      )}

      {app.remarks && <p className="small muted">Remarks: {app.remarks}</p>}

      {(app.status === "SUBMITTED" || app.status === "WAITLISTED" || app.status === "APPROVED") && (
        <div className="inline-form">
          <label style={{ flex: 1 }}>Remarks for the applicant (optional)
            <input value={remarks} onChange={(e) => setRemarks(e.target.value)} />
          </label>
          {app.status !== "APPROVED" && (
            <button className="btn primary" onClick={() => run(() => api.post(`/admin/applications/${app.id}/decision`, { decision: "APPROVE", remarks }), "Decision saved.")}>Approve</button>
          )}
          <button className="btn ghost danger" onClick={() => run(() => api.post(`/admin/applications/${app.id}/decision`, { decision: "REJECT", remarks }), "Application rejected.")}>Reject</button>
        </div>
      )}

      {app.status === "APPROVED" && (
        <div className="inline-form">
          <label style={{ flex: 1 }}>University email for the new student account
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="name@univ.edu" />
          </label>
          <button className="btn primary" disabled={!email}
            onClick={() => run(async () => {
              const r = await api.post(`/admin/applications/${app.id}/enroll`, { universityEmail: email });
              setSecret({ who: r.data.user.universityEmail, password: r.data.temporaryPassword });
            })}>Enroll as student</button>
        </div>
      )}
    </section>
  );
}

function Seats({ setError, setOk }) {
  const [rows, setRows] = useState(null);
  const [edit, setEdit] = useState({});
  const key = (r) => `${r.faculty}|${r.session}`;

  useEffect(() => {
    api.get("/admin/seat-config").then((r) => setRows(r.data)).catch((e) => setError(errMsg(e)));
  }, [setError]);

  const save = async (r) => {
    const value = edit[key(r)];
    if (value === undefined || value === "") return;
    setError(""); setOk("");
    try {
      const res = await api.put("/admin/seat-config", { faculty: r.faculty, session: r.session, seats: Number(value) });
      setRows(res.data);
      setEdit({ ...edit, [key(r)]: undefined });
      setOk(`Seats saved for ${r.faculty} ${r.session}. Waiting applicants were moved up if seats were free.`);
    } catch (err) { setError(errMsg(err)); }
  };

  if (!rows) return <p className="muted">Loading…</p>;

  return (
    <section className="panel">
      <p className="muted small">
        When you approve an application and the seats are full, the applicant goes on the waiting list. When a seat
        frees up or you add seats, the oldest waiting applicant with verified documents is approved automatically.
        A blank limit means no limit.
      </p>
      <table>
        <thead><tr><th>Programme</th><th>Intake</th><th>Seat limit</th><th>Taken</th><th>Waiting</th><th></th></tr></thead>
        <tbody>
          {rows.map((r) => (
            <tr key={key(r)}>
              <td>{r.faculty}</td><td>{r.session}</td>
              <td>
                <input type="number" min="0" style={{ width: 100 }} placeholder="No limit"
                  value={edit[key(r)] ?? (r.seats ?? "")} onChange={(e) => setEdit({ ...edit, [key(r)]: e.target.value })} />
              </td>
              <td>{r.allocated}</td><td>{r.waitlisted}</td>
              <td className="num"><button className="btn ghost" onClick={() => save(r)}>Save</button></td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
