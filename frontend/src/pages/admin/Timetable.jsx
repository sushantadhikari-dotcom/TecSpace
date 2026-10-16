import { useEffect, useState } from "react";
import api, { errMsg } from "../../api.js";

const DAYS = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];
const label = (d) => d.charAt(0) + d.slice(1).toLowerCase();

export default function Timetable() {
  const [slots, setSlots] = useState(null);
  const [courses, setCourses] = useState([]);
  const [form, setForm] = useState({ courseCode: "", day: "MONDAY", startTime: "09:00", endTime: "10:00", room: "" });
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");

  const load = () => api.get("/admin/timetable").then((r) => setSlots(r.data)).catch((e) => setError(errMsg(e)));
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
      await api.post("/admin/timetable", form);
      setOk("Class added. No clashes found.");
      load();
    } catch (err) { setError(errMsg(err)); }
  };

  const remove = async (s) => {
    setError(""); setOk("");
    try { await api.delete(`/admin/timetable/${s.id}`); load(); }
    catch (err) { setError(errMsg(err)); }
  };

  return (
    <>
      <h1 className="page-title">Timetable</h1>
      {error && <div className="alert error" role="alert">{error}</div>}
      {ok && <div className="alert success" role="status">{ok}</div>}

      <section className="panel">
        <h2>Schedule a class</h2>
        {courses.length === 0 ? (
          <p className="muted">Create a course first, then you can schedule it here.</p>
        ) : (
          <form onSubmit={submit} className="inline-form">
            <label>Course
              <select value={form.courseCode} onChange={set("courseCode")}>
                {courses.map((c) => <option key={c.code} value={c.code}>{c.code} · {c.name}</option>)}
              </select>
            </label>
            <label>Day
              <select value={form.day} onChange={set("day")}>
                {DAYS.map((d) => <option key={d} value={d}>{label(d)}</option>)}
              </select>
            </label>
            <label>Start<input type="time" value={form.startTime} onChange={set("startTime")} required /></label>
            <label>End<input type="time" value={form.endTime} onChange={set("endTime")} required /></label>
            <label>Room<input value={form.room} onChange={set("room")} placeholder="101" required /></label>
            <button className="btn primary">Add class</button>
          </form>
        )}
        <p className="muted small">The server blocks double-booked rooms and instructors teaching two classes at once.</p>
      </section>

      <section className="panel">
        <h2>All classes</h2>
        {!slots ? <p className="muted">Loading…</p> : slots.length === 0 ? (
          <p className="muted">Nothing scheduled yet.</p>
        ) : (
          <table>
            <thead><tr><th>Day</th><th>Time</th><th>Course</th><th>Room</th><th>Instructor</th><th></th></tr></thead>
            <tbody>
              {slots.map((s) => (
                <tr key={s.id}>
                  <td>{label(s.day)}</td>
                  <td>{s.startTime} – {s.endTime}</td>
                  <td><strong>{s.courseCode}</strong> {s.courseName}</td>
                  <td>{s.room}</td>
                  <td>{s.instructorName || "—"}</td>
                  <td className="num"><button className="btn ghost danger" onClick={() => remove(s)}>Delete</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </>
  );
}
