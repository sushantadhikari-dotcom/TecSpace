import { useEffect, useState } from "react";
import api, { errMsg } from "../../api.js";

const blank = {
  code: "", name: "", department: "", creditHours: 3, capacity: 30, instructorName: "",
  prerequisites: "", syllabus: "", syllabusUrl: "",
};

export default function Courses() {
  const [list, setList] = useState(null);
  const [form, setForm] = useState(blank);
  const [editing, setEditing] = useState(false);
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");

  const load = () => api.get("/admin/courses").then((r) => setList(r.data)).catch((e) => setError(errMsg(e)));
  useEffect(() => { load(); }, []);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setError(""); setOk("");
    const body = {
      code: form.code, name: form.name, department: form.department,
      creditHours: Number(form.creditHours), capacity: Number(form.capacity),
      instructorName: form.instructorName,
      prerequisites: form.prerequisites.split(",").map((s) => s.trim()).filter(Boolean),
      syllabus: form.syllabus, syllabusUrl: form.syllabusUrl,
    };
    try {
      if (editing) await api.put(`/admin/courses/${form.code}`, body);
      else await api.post("/admin/courses", body);
      setOk(editing ? "Course updated." : "Course created.");
      setForm(blank); setEditing(false);
      load();
    } catch (err) { setError(errMsg(err)); }
  };

  const edit = (c) => {
    setForm({
      code: c.code, name: c.name, department: c.department, creditHours: c.creditHours, capacity: c.capacity,
      instructorName: c.instructorName || "", prerequisites: c.prerequisites.join(", "),
      syllabus: c.syllabus || "", syllabusUrl: c.syllabusUrl || "",
    });
    setEditing(true);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const remove = async (c) => {
    if (!window.confirm(`Delete ${c.code} and its timetable slots?`)) return;
    setError(""); setOk("");
    try { await api.delete(`/admin/courses/${c.code}`); setOk(`${c.code} deleted.`); load(); }
    catch (err) { setError(errMsg(err)); }
  };

  return (
    <>
      <h1 className="page-title">Courses</h1>
      {error && <div className="alert error" role="alert">{error}</div>}
      {ok && <div className="alert success" role="status">{ok}</div>}

      <section className="panel">
        <h2>{editing ? `Edit ${form.code}` : "Add a course"}</h2>
        <form onSubmit={submit} className="course-form">
          <label>Code<input value={form.code} onChange={set("code")} disabled={editing} placeholder="CS101" required /></label>
          <label>Name<input value={form.name} onChange={set("name")} required /></label>
          <label>Department<input value={form.department} onChange={set("department")} placeholder="IT" required /></label>
          <label>Credit hours<input type="number" min="1" value={form.creditHours} onChange={set("creditHours")} required /></label>
          <label>Seats<input type="number" min="1" value={form.capacity} onChange={set("capacity")} required /></label>
          <label>Instructor<input value={form.instructorName} onChange={set("instructorName")} /></label>
          <label className="wide">Prerequisites (course codes, comma separated)
            <input value={form.prerequisites} onChange={set("prerequisites")} placeholder="CS101, MTH102" />
          </label>
          <label className="wide">Syllabus (students see it only after their registration is approved)
            <textarea rows="8" value={form.syllabus} onChange={set("syllabus")}
              placeholder={"Course description, weekly topics, books, grading..."} />
          </label>
          <label className="wide">Link to a syllabus document (optional)
            <input type="url" value={form.syllabusUrl} onChange={set("syllabusUrl")} placeholder="https://" />
          </label>
          <div className="actions wide">
            {editing && <button type="button" className="btn ghost" onClick={() => { setForm(blank); setEditing(false); }}>Cancel</button>}
            <button className="btn primary">{editing ? "Save changes" : "Add course"}</button>
          </div>
        </form>
      </section>

      <section className="panel">
        <h2>This term's courses</h2>
        {!list ? <p className="muted">Loading…</p> : list.length === 0 ? (
          <p className="muted">No courses yet. Add the first one above.</p>
        ) : (
          <table>
            <thead><tr><th>Code</th><th>Course</th><th>Instructor</th><th>Credits</th><th>Seats</th><th>Prerequisites</th><th>Syllabus</th><th></th></tr></thead>
            <tbody>
              {list.map((c) => (
                <tr key={c.code}>
                  <td className="mono">{c.code}</td>
                  <td>{c.name}<div className="small muted">{c.department}</div></td>
                  <td>{c.instructorName || "—"}</td>
                  <td>{c.creditHours}</td>
                  <td>{c.capacity}</td>
                  <td>{c.prerequisites.length ? c.prerequisites.join(", ") : "—"}</td>
                  <td>{(c.syllabus || c.syllabusUrl) ? <span className="pill ok">Added</span> : <span className="pill warn">Missing</span>}</td>
                  <td className="num">
                    <button className="btn ghost" onClick={() => edit(c)}>Edit</button>{" "}
                    <button className="btn ghost danger" onClick={() => remove(c)}>Delete</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </>
  );
}
