import { useEffect, useState } from "react";
import api, { errMsg } from "../../api.js";

export default function Messages() {
  const [courses, setCourses] = useState(null);
  const [form, setForm] = useState({ courseCode: "", subject: "", body: "" });
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");

  useEffect(() => {
    api.get("/teacher/courses").then((r) => {
      setCourses(r.data);
      if (r.data[0]) setForm((f) => ({ ...f, courseCode: r.data[0].code }));
    }).catch((e) => setError(errMsg(e)));
  }, []);

  const send = async (e) => {
    e.preventDefault();
    setError(""); setOk("");
    try {
      const r = await api.post("/teacher/messages", form);
      setOk(`Message sent to ${r.data.sentTo} student(s).`);
      setForm({ ...form, subject: "", body: "" });
    } catch (err) { setError(errMsg(err)); }
  };

  return (
    <>
      <h1 className="page-title">Message students</h1>
      {error && <div className="alert error" role="alert">{error}</div>}
      {ok && <div className="alert success" role="status">{ok}</div>}
      <section className="panel">
        {!courses ? <p className="muted">Loading…</p> : courses.length === 0 ? (
          <p className="muted">You have not been assigned to any course yet.</p>
        ) : (
          <form onSubmit={send} className="stack">
            <label>Send to everyone registered in
              <select value={form.courseCode} onChange={(e) => setForm({ ...form, courseCode: e.target.value })}>
                {courses.map((c) => <option key={c.code} value={c.code}>{c.code} · {c.name} ({c.enrolledCount} students)</option>)}
              </select>
            </label>
            <label>Subject<input value={form.subject} onChange={(e) => setForm({ ...form, subject: e.target.value })} required /></label>
            <label>Message<textarea rows="5" value={form.body} onChange={(e) => setForm({ ...form, body: e.target.value })} required /></label>
            <div><button className="btn primary">Send message</button></div>
          </form>
        )}
        <p className="muted small">Students reply from their Requests &amp; messages page. Replies arrive in your Inbox.</p>
      </section>
    </>
  );
}
