import { useEffect, useState } from "react";
import api, { errMsg } from "../../api.js";

export default function Exams() {
  const [exams, setExams] = useState(null);
  const [courses, setCourses] = useState([]);
  const [form, setForm] = useState({ courseCode: "", date: "", startTime: "10:00", endTime: "12:00", room: "", note: "" });
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");

  const load = () => api.get("/admin/exams").then((r) => setExams(r.data)).catch((e) => setError(errMsg(e)));
  useEffect(() => {
    load();
    api.get("/admin/courses").then((r) => {
      setCourses(r.data);
      if (r.data[0]) setForm((f) => ({ ...f, courseCode: r.data[0].code }));
    }).catch((e) => setError(errMsg(e)));
  }, []);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setError(""); setOk("");
    try {
      await api.post("/admin/exams", form);
      setOk("Exam scheduled. Enrolled students were notified.");
      load();
    } catch (err) { setError(errMsg(err)); }
  };

  const remove = async (x) => {
    if (!window.confirm(`Cancel the ${x.courseCode} exam on ${x.date}? Students will be notified.`)) return;
    setError(""); setOk("");
    try { await api.delete(`/admin/exams/${x.id}`); load(); } catch (err) { setError(errMsg(err)); }
  };

  return (
    <>
      <h1 className="page-title">Exam timetable</h1>
      {error && <div className="alert error" role="alert">{error}</div>}
      {ok && <div className="alert success" role="status">{ok}</div>}

      <section className="panel">
        <h2>Schedule an exam</h2>
        {courses.length === 0 ? (
          <p className="muted">Create a course first.</p>
        ) : (
          <form onSubmit={submit} className="inline-form">
            <label>Course
              <select value={form.courseCode} onChange={set("courseCode")}>
                {courses.map((c) => <option key={c.code} value={c.code}>{c.code} · {c.name}</option>)}
              </select>
            </label>
            <label>Date<input type="date" value={form.date} onChange={set("date")} required /></label>
            <label>Start<input type="time" value={form.startTime} onChange={set("startTime")} required /></label>
            <label>End<input type="time" value={form.endTime} onChange={set("endTime")} required /></label>
            <label>Room<input value={form.room} onChange={set("room")} required /></label>
            <label>Note<input value={form.note} onChange={set("note")} placeholder="Optional" /></label>
            <button className="btn primary">Add exam</button>
          </form>
        )}
        <p className="muted small">
          The server blocks a double-booked room and any time where a student would have two exams at once.
        </p>
      </section>

      <section className="panel">
        <h2>All exams</h2>
        {!exams ? <p className="muted">Loading…</p> : exams.length === 0 ? (
          <p className="muted">No exams scheduled yet.</p>
        ) : (
          <table>
            <thead><tr><th>Date</th><th>Time</th><th>Course</th><th>Room</th><th>Note</th><th></th></tr></thead>
            <tbody>
              {exams.map((x) => (
                <tr key={x.id}>
                  <td>{x.date}</td>
                  <td>{x.startTime} – {x.endTime}</td>
                  <td><strong>{x.courseCode}</strong> {x.courseName}</td>
                  <td>{x.room}</td>
                  <td>{x.note || "—"}</td>
                  <td className="num"><button className="btn ghost danger" onClick={() => remove(x)}>Cancel</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </>
  );
}
