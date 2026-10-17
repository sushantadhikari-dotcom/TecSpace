import { useEffect, useState } from "react";
import api, { errMsg } from "../../api.js";

export default function Audit() {
  const [rows, setRows] = useState(null);
  const [q, setQ] = useState("");
  const [error, setError] = useState("");

  const load = () =>
    api.get("/admin/audit-logs", { params: q ? { q } : {} }).then((r) => setRows(r.data)).catch((e) => setError(errMsg(e)));
  useEffect(() => { load(); }, []);

  return (
    <>
      <h1 className="page-title">Audit log</h1>
      {error && <div className="alert error" role="alert">{error}</div>}
      <section className="panel">
        <form className="inline-form" onSubmit={(e) => { e.preventDefault(); load(); }}>
          <label style={{ flex: 1 }}>Search by person, action or page
            <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="e.g. admin@tecspace.edu or /api/admin/grades" />
          </label>
          <button className="btn primary">Search</button>
        </form>
        <p className="muted small">Shows the latest 300 changes: who did what, when, and whether it worked. Passwords and form contents are never stored.</p>

        {!rows ? <p className="muted">Loading…</p> : rows.length === 0 ? <p className="muted">Nothing found.</p> : (
          <table>
            <thead><tr><th>When</th><th>Who</th><th>Action</th><th>Result</th><th>From</th></tr></thead>
            <tbody>
              {rows.map((l) => (
                <tr key={l.id}>
                  <td className="small">{new Date(l.timestamp).toLocaleString()}</td>
                  <td>{l.actor}<div className="small muted">{(l.role || "").replace("ROLE_", "")}</div></td>
                  <td><span className="pill">{l.method}</span> <span className="mono small">{l.path}</span></td>
                  <td><span className={`pill ${l.status < 400 ? "ok" : "bad"}`}>{l.status}</span></td>
                  <td className="small muted">{l.ip}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </>
  );
}
