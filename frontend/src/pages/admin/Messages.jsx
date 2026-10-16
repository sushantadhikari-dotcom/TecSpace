import { useEffect, useState } from "react";
import api, { errMsg } from "../../api.js";

export default function Messages() {
  const [students, setStudents] = useState([]);
  const [courses, setCourses] = useState([]);
  const [form, setForm] = useState({ target: "ALL", courseCode: "", studentId: "", subject: "", body: "" });
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");

  useEffect(() => {
    api.get("/admin/students").then((r) => {
      setStudents(r.data);
      if (r.data[0]) setForm((f) => ({ ...f, studentId: r.data[0].id }));
    }).catch(() => {});
    api.get("/admin/courses").then((r) => {
      setCourses(r.data);
      if (r.data[0]) setForm((f) => ({ ...f, courseCode: r.data[0].code }));
    }).catch(() => {});
  }, []);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const send = async (e) => {
    e.preventDefault();
    setError(""); setOk("");
    try {
      const r = await api.post("/admin/messages", form);
      setOk(`Message sent to ${r.data.sentTo} student(s).`);
      setForm({ ...form, subject: "", body: "" });
    } catch (err) { setError(errMsg(err)); }
  };

  return (
    <>
      <h1 className="page-title">Send a message</h1>
      {error && <div className="alert error" role="alert">{error}</div>}
      {ok && <div className="alert success" role="status">{ok}</div>}

      <section className="panel">
        <form onSubmit={send} className="stack">
          <label>Send to
            <select value={form.target} onChange={set("target")}>
              <option value="ALL">All students</option>
              <option value="COURSE">Everyone in a course</option>
              <option value="STUDENT">One student</option>
            </select>
          </label>

          {form.target === "COURSE" && (
            <label>Course
              <select value={form.courseCode} onChange={set("courseCode")}>
                {courses.map((c) => <option key={c.code} value={c.code}>{c.code} · {c.name}</option>)}
              </select>
            </label>
          )}
          {form.target === "STUDENT" && (
            <label>Student
              <select value={form.studentId} onChange={set("studentId")}>
                {students.map((s) => <option key={s.id} value={s.id}>{s.firstName} {s.lastName} · {s.universityEmail}</option>)}
              </select>
            </label>
          )}

          <label>Subject<input value={form.subject} onChange={set("subject")} required /></label>
          <label>Message<textarea rows="5" value={form.body} onChange={set("body")} required /></label>
          <div><button className="btn primary">Send message</button></div>
        </form>
        <p className="muted small">
          Students see it in their Inbox and can write back through "Contact the administration". Replies from
          students arrive in your own Inbox.
        </p>
      </section>
    </>
  );
}
