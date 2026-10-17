import { useEffect, useState } from "react";
import api, { errMsg } from "../../api.js";

const TYPES = { TRANSCRIPT: "Official transcript", ID_REISSUE: "ID card re-issue", BONAFIDE: "Bonafide certificate", LEAVE: "Leave application" };
const STATUS = { PENDING: ["Waiting", "warn"], APPROVED: ["Approved", "ok"], REJECTED: ["Declined", "bad"] };

export default function Requests() {
  const [list, setList] = useState(null);
  const [status, setStatus] = useState("PENDING");
  const [notes, setNotes] = useState({});
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");

  const load = () =>
    api.get("/admin/requests", { params: status ? { status } : {} }).then((r) => setList(r.data)).catch((e) => setError(errMsg(e)));
  useEffect(() => { load(); }, [status]);

  const decide = async (q, approve) => {
    setError(""); setOk("");
    try {
      await api.post(`/admin/requests/${q.id}/decision`, { approve, note: notes[q.id] || "" });
      setOk(approve ? "Request approved. The student was notified." : "Request declined. The student was notified.");
      load();
    } catch (err) { setError(errMsg(err)); }
  };

  return (
    <>
      <h1 className="page-title">Student requests</h1>
      {error && <div className="alert error" role="alert">{error}</div>}
      {ok && <div className="alert success" role="status">{ok}</div>}

      <section className="panel">
        <label style={{ maxWidth: 260 }}>Show
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="PENDING">Waiting for a decision</option><option value="APPROVED">Approved</option>
            <option value="REJECTED">Declined</option><option value="">Everything</option>
          </select>
        </label>

        {!list ? <p className="muted">Loading…</p> : list.length === 0 ? <p className="muted">Nothing here.</p> : (
          <table>
            <thead><tr><th>Student</th><th>Request</th><th>Status</th><th>Decision</th></tr></thead>
            <tbody>
              {list.map((q) => {
                const [text, cls] = STATUS[q.status] || STATUS.PENDING;
                return (
                  <tr key={q.id}>
                    <td>{q.studentName}<div className="small muted">{q.faculty} · {new Date(q.createdAt).toLocaleDateString()}</div></td>
                    <td>
                      <strong>{TYPES[q.type]}</strong>
                      {q.type === "LEAVE" && <div className="small">{q.leaveFrom} to {q.leaveTo}</div>}
                      {q.details && <div className="small muted">{q.details}</div>}
                    </td>
                    <td><span className={`pill ${cls}`}>{text}</span>{q.adminNote && <div className="small muted">{q.adminNote}</div>}</td>
                    <td>
                      {q.status === "PENDING" ? (
                        <div className="stack" style={{ gap: "0.4rem" }}>
                          <input placeholder="Note (optional)" value={notes[q.id] || ""} onChange={(e) => setNotes({ ...notes, [q.id]: e.target.value })} />
                          <div>
                            <button className="btn primary" onClick={() => decide(q, true)}>Approve</button>{" "}
                            <button className="btn ghost danger" onClick={() => decide(q, false)}>Decline</button>
                          </div>
                        </div>
                      ) : <span className="small muted">by {q.decidedBy}</span>}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </section>
    </>
  );
}
