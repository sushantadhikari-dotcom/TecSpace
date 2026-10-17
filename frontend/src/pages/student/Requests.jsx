import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api, { errMsg } from "../../api.js";

const TYPES = {
  TRANSCRIPT: "Official transcript", ID_REISSUE: "ID card re-issue", BONAFIDE: "Bonafide certificate", LEAVE: "Leave application",
};
const STATUS = { PENDING: ["Waiting", "warn"], APPROVED: ["Approved", "ok"], REJECTED: ["Declined", "bad"] };

export default function Requests() {
  const [list, setList] = useState(null);
  const [mine, setMine] = useState([]);
  const [form, setForm] = useState({ type: "BONAFIDE", details: "", leaveFrom: "", leaveTo: "" });
  const [msg, setMsg] = useState({ courseCode: "", subject: "", body: "" });
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");

  const load = () => api.get("/student/requests").then((r) => setList(r.data)).catch((e) => setError(errMsg(e)));
  useEffect(() => {
    load();
    api.get("/student/enrollments").then((r) => {
      setMine(r.data);
      if (r.data[0]) setMsg((m) => ({ ...m, courseCode: r.data[0].code }));
    }).catch(() => {});
  }, []);

  const submit = async (e) => {
    e.preventDefault();
    setError(""); setOk("");
    try {
      const body = { type: form.type, details: form.details };
      if (form.type === "LEAVE") { body.leaveFrom = form.leaveFrom; body.leaveTo = form.leaveTo; }
      await api.post("/student/requests", body);
      setOk("Request sent. You will get a notification when it is decided.");
      setForm({ ...form, details: "", leaveFrom: "", leaveTo: "" });
      load();
    } catch (err) { setError(errMsg(err)); }
  };

  const sendMessage = async (e) => {
    e.preventDefault();
    setError(""); setOk("");
    try {
      await api.post("/student/instructor-messages", msg);
      setOk("Message sent to the course instructor.");
      setMsg({ ...msg, subject: "", body: "" });
    } catch (err) { setError(errMsg(err)); }
  };

  return (
    <>
      <h1 className="page-title">Requests &amp; messages</h1>
      {error && <div className="alert error" role="alert">{error}</div>}
      {ok && <div className="alert success" role="status">{ok}</div>}

      <section className="panel">
        <h2>New request</h2>
        <form onSubmit={submit} className="stack">
          <label>What do you need?
            <select value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value })}>
              {Object.entries(TYPES).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
            </select>
          </label>
          {form.type === "LEAVE" && (
            <div className="row">
              <label>First day of leave<input type="date" value={form.leaveFrom} onChange={(e) => setForm({ ...form, leaveFrom: e.target.value })} required /></label>
              <label>Last day of leave<input type="date" value={form.leaveTo} onChange={(e) => setForm({ ...form, leaveTo: e.target.value })} required /></label>
            </div>
          )}
          <label>{form.type === "LEAVE" ? "Reason" : "Purpose (optional)"}
            <textarea rows="3" value={form.details} onChange={(e) => setForm({ ...form, details: e.target.value })} required={form.type === "LEAVE"} />
          </label>
          <div><button className="btn primary">Send request</button></div>
        </form>
      </section>

      <section className="panel">
        <h2>My requests</h2>
        {!list ? <p className="muted">Loading…</p> : list.length === 0 ? <p className="muted">You have not made any requests yet.</p> : (
          <table>
            <thead><tr><th>Request</th><th>Date</th><th>Status</th><th></th></tr></thead>
            <tbody>
              {list.map((q) => {
                const [text, cls] = STATUS[q.status] || STATUS.PENDING;
                return (
                  <tr key={q.id}>
                    <td>
                      <strong>{TYPES[q.type]}</strong>
                      {q.type === "LEAVE" && <div className="small muted">{q.leaveFrom} to {q.leaveTo}</div>}
                      {q.details && <div className="small muted">{q.details}</div>}
                      {q.adminNote && <div className="small">Note: {q.adminNote}</div>}
                    </td>
                    <td>{new Date(q.createdAt).toLocaleDateString()}</td>
                    <td><span className={`pill ${cls}`}>{text}</span></td>
                    <td className="num">
                      {q.status === "APPROVED" && q.type === "BONAFIDE" && <Link to={`/student/certificate/${q.id}`}>Open certificate</Link>}
                      {q.status === "APPROVED" && q.type === "TRANSCRIPT" && <Link to="/student/transcript">Open transcript</Link>}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </section>

      <section className="panel">
        <h2>Message an instructor</h2>
        {mine.length === 0 ? <p className="muted">Register for a course to message its instructor.</p> : (
          <form onSubmit={sendMessage} className="stack">
            <label>Course
              <select value={msg.courseCode} onChange={(e) => setMsg({ ...msg, courseCode: e.target.value })}>
                {mine.map((c) => <option key={c.code} value={c.code}>{c.code} · {c.name}{c.instructorName ? ` (${c.instructorName})` : ""}</option>)}
              </select>
            </label>
            <label>Subject<input value={msg.subject} onChange={(e) => setMsg({ ...msg, subject: e.target.value })} required /></label>
            <label>Message<textarea rows="4" value={msg.body} onChange={(e) => setMsg({ ...msg, body: e.target.value })} required /></label>
            <div><button className="btn primary">Send message</button></div>
          </form>
        )}
      </section>
    </>
  );
}
