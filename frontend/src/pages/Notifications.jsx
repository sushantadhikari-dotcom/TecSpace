import { useEffect, useState } from "react";
import api, { errMsg } from "../api.js";
import { useAuth } from "../AuthContext.jsx";

const TYPE_LABEL = { ATTENDANCE: "Attendance", EXAM: "Exam", MESSAGE: "Message", SYSTEM: "Notice" };

export default function Notifications() {
  const { user } = useAuth();
  const isStudent = user.role === "ROLE_STUDENT";
  const [list, setList] = useState(null);
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");
  const [form, setForm] = useState({ subject: "", body: "" });

  const load = () => api.get("/notifications").then((r) => setList(r.data)).catch((e) => setError(errMsg(e)));
  useEffect(() => { load(); }, []);

  const open = async (n) => {
    if (n.read) return;
    try {
      await api.put(`/notifications/${n.id}/read`);
      setList((l) => l.map((x) => (x.id === n.id ? { ...x, read: true } : x)));
    } catch { /* ignore */ }
  };

  const readAll = async () => {
    try { await api.put("/notifications/read-all"); load(); } catch (err) { setError(errMsg(err)); }
  };

  const send = async (e) => {
    e.preventDefault();
    setError(""); setOk("");
    try {
      await api.post("/notifications/contact-admin", form);
      setForm({ subject: "", body: "" });
      setOk("Your message was sent to the administration.");
    } catch (err) { setError(errMsg(err)); }
  };

  return (
    <>
      <h1 className="page-title">Inbox</h1>
      {error && <div className="alert error" role="alert">{error}</div>}
      {ok && <div className="alert success" role="status">{ok}</div>}

      <section className="panel">
        <div className="panel-head">
          <h2>Notifications</h2>
          <button className="btn ghost" onClick={readAll}>Mark all as read</button>
        </div>
        {!list ? <p className="muted">Loading…</p> : list.length === 0 ? (
          <p className="muted">You have no notifications yet.</p>
        ) : list.map((n) => (
          <div key={n.id} className={`note ${n.read ? "" : "unread"}`} onClick={() => open(n)} role="button" tabIndex={0}
            onKeyDown={(e) => { if (e.key === "Enter") open(n); }}>
            <div className="panel-head">
              <strong>{n.title}</strong>
              <span className="pill">{TYPE_LABEL[n.type] || "Notice"}</span>
            </div>
            <p>{n.message}</p>
            <span className="small muted">
              {n.senderName ? `From ${n.senderName} · ` : ""}{new Date(n.createdAt).toLocaleString()}
            </span>
          </div>
        ))}
      </section>

      {isStudent && (
        <section className="panel">
          <h2>Contact the administration</h2>
          <form onSubmit={send} className="stack">
            <label>Subject<input value={form.subject} onChange={(e) => setForm({ ...form, subject: e.target.value })} required /></label>
            <label>Message
              <textarea rows="4" value={form.body} onChange={(e) => setForm({ ...form, body: e.target.value })} required />
            </label>
            <div><button className="btn primary">Send message</button></div>
          </form>
        </section>
      )}
    </>
  );
}
