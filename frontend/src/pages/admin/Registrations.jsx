import { useEffect, useState } from "react";
import api, { errMsg } from "../../api.js";

const STATUS = { PENDING: ["Waiting", "warn"], ENROLLED: ["Approved", "ok"], REJECTED: ["Declined", "bad"] };

export default function Registrations() {
  const [status, setStatus] = useState("PENDING");
  const [rows, setRows] = useState(null);
  const [notes, setNotes] = useState({});
  const [override, setOverride] = useState({});
  const [students, setStudents] = useState([]);
  const [courses, setCourses] = useState([]);
  const [direct, setDirect] = useState({ studentId: "", courseCode: "", overridePrerequisites: false });
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");

  const load = () =>
    api.get("/admin/enrollment-requests", { params: status ? { status } : {} })
      .then((r) => setRows(r.data)).catch((e) => setError(errMsg(e)));
  useEffect(() => { load(); }, [status]);

  useEffect(() => {
    api.get("/admin/students").then((r) => {
      setStudents(r.data);
      if (r.data[0]) setDirect((d) => ({ ...d, studentId: r.data[0].id }));
    }).catch(() => {});
    api.get("/admin/courses").then((r) => {
      setCourses(r.data);
      if (r.data[0]) setDirect((d) => ({ ...d, courseCode: r.data[0].code }));
    }).catch(() => {});
  }, []);

  const decide = async (r, approve) => {
    setError(""); setOk("");
    try {
      await api.post(`/admin/enrollment-requests/${r.id}/decision`, {
        approve, note: notes[r.id] || "", overridePrerequisites: !!override[r.id],
      });
      setOk(approve ? `${r.studentName} is now registered for ${r.courseCode}.` : "Request declined. The student was notified.");
      load();
    } catch (err) { setError(errMsg(err)); }
  };

  const enrollDirect = async (e) => {
    e.preventDefault();
    setError(""); setOk("");
    try {
      await api.post("/admin/enrollment-requests/direct", direct);
      setOk("Student registered.");
      load();
    } catch (err) { setError(errMsg(err)); }
  };

  return (
    <>
      <h1 className="page-title">Course registrations</h1>
      {error && <div className="alert error" role="alert">{error}</div>}
      {ok && <div className="alert success" role="status">{ok}</div>}

      <section className="panel">
        <label style={{ maxWidth: 280 }}>Show
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="PENDING">Waiting for approval</option>
            <option value="ENROLLED">Approved</option>
            <option value="REJECTED">Declined</option>
            <option value="">All this term</option>
          </select>
        </label>

        {!rows ? <p className="muted">Loading…</p> : rows.length === 0 ? <p className="muted">Nothing here.</p> : (
          <table>
            <thead><tr><th>Student</th><th>Course</th><th>Checks</th><th>Decision</th></tr></thead>
            <tbody>
              {rows.map((r) => {
                const [text, cls] = STATUS[r.status] || ["", ""];
                const waiting = r.status === "PENDING";
                return (
                  <tr key={r.id}>
                    <td>{r.studentName}<div className="small muted">{r.faculty} · {new Date(r.requestedAt).toLocaleDateString()}</div></td>
                    <td><strong>{r.courseCode}</strong> {r.courseName}</td>
                    <td className="small">
                      {waiting ? (
                        <>
                          <div>Seats left: <strong>{r.seatsLeft}</strong> / {r.capacity}</div>
                          {r.missingPrerequisites.length > 0 && <div><span className="pill bad">Missing {r.missingPrerequisites.join(", ")}</span></div>}
                          {r.clash && <div><span className="pill bad">{r.clash}</span></div>}
                          {r.missingPrerequisites.length === 0 && !r.clash && r.seatsLeft > 0 && <span className="pill ok">All checks passed</span>}
                        </>
                      ) : <span className={`pill ${cls}`}>{text}</span>}
                    </td>
                    <td>
                      {waiting ? (
                        <div className="stack" style={{ gap: "0.4rem" }}>
                          <input placeholder="Note to the student (optional)" value={notes[r.id] || ""} onChange={(e) => setNotes({ ...notes, [r.id]: e.target.value })} />
                          {r.missingPrerequisites.length > 0 && (
                            <label className="check small">
                              <input type="checkbox" checked={!!override[r.id]} onChange={(e) => setOverride({ ...override, [r.id]: e.target.checked })} />
                              Approve even if prerequisites are missing
                            </label>
                          )}
                          <div>
                            <button className="btn primary" onClick={() => decide(r, true)}>Approve</button>{" "}
                            <button className="btn ghost danger" onClick={() => decide(r, false)}>Decline</button>
                          </div>
                        </div>
                      ) : <span className="small muted">{r.decidedBy ? `by ${r.decidedBy}` : ""}{r.note ? ` · ${r.note}` : ""}</span>}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </section>

      <section className="panel">
        <h2>Register a student directly</h2>
        <form onSubmit={enrollDirect} className="inline-form">
          <label>Student
            <select value={direct.studentId} onChange={(e) => setDirect({ ...direct, studentId: e.target.value })}>
              {students.map((s) => <option key={s.id} value={s.id}>{s.firstName} {s.lastName} · {s.universityEmail}</option>)}
            </select>
          </label>
          <label>Course
            <select value={direct.courseCode} onChange={(e) => setDirect({ ...direct, courseCode: e.target.value })}>
              {courses.map((c) => <option key={c.code} value={c.code}>{c.code} · {c.name}</option>)}
            </select>
          </label>
          <label className="check">
            <input type="checkbox" checked={direct.overridePrerequisites} onChange={(e) => setDirect({ ...direct, overridePrerequisites: e.target.checked })} />
            Ignore missing prerequisites
          </label>
          <button className="btn primary" disabled={!direct.studentId || !direct.courseCode}>Register</button>
        </form>
        <p className="muted small">The same seat and timetable-clash checks apply.</p>
      </section>
    </>
  );
}
