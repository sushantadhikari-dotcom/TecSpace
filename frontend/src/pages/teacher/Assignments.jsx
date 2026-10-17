import { useEffect, useState } from "react";
import api, { errMsg } from "../../api.js";

const blank = { courseCode: "", title: "", description: "", dueAt: "", maxMarks: 100 };
const when = (d) => new Date(d).toLocaleString(undefined, { day: "numeric", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" });
const STATUS = {
  NOT_SUBMITTED: ["Not handed in", ""], SUBMITTED: ["Handed in", "ok"], LATE: ["Late", "warn"], GRADED: ["Marked", "ok"],
};

export default function Assignments() {
  const [courses, setCourses] = useState(null);
  const [list, setList] = useState(null);
  const [form, setForm] = useState(blank);
  const [editingId, setEditingId] = useState(null);
  const [openId, setOpenId] = useState(null);
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");

  const load = () => api.get("/teacher/assignments").then((r) => setList(r.data)).catch((e) => setError(errMsg(e)));
  useEffect(() => {
    load();
    api.get("/teacher/courses").then((r) => {
      setCourses(r.data);
      if (r.data[0]) setForm((f) => ({ ...f, courseCode: r.data[0].code }));
    }).catch((e) => setError(errMsg(e)));
  }, []);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setError(""); setOk("");
    const body = { ...form, maxMarks: Number(form.maxMarks) };
    try {
      if (editingId) await api.put(`/teacher/assignments/${editingId}`, body);
      else await api.post("/teacher/assignments", body);
      setOk(editingId ? "Assignment updated." : "Assignment posted. Registered students were notified.");
      setForm({ ...blank, courseCode: form.courseCode });
      setEditingId(null);
      load();
    } catch (err) { setError(errMsg(err)); }
  };

  const edit = (a) => {
    setForm({ courseCode: a.courseCode, title: a.title, description: a.description || "", dueAt: String(a.dueAt).slice(0, 16), maxMarks: a.maxMarks });
    setEditingId(a.id);
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const remove = async (a) => {
    if (!window.confirm(`Delete "${a.title}"?`)) return;
    setError(""); setOk("");
    try { await api.delete(`/teacher/assignments/${a.id}`); load(); } catch (err) { setError(errMsg(err)); }
  };

  if (!courses) return <p className="muted">{error || "Loading…"}</p>;

  return (
    <>
      <h1 className="page-title">Assignments</h1>
      {error && <div className="alert error" role="alert">{error}</div>}
      {ok && <div className="alert success" role="status">{ok}</div>}

      {courses.length === 0 ? (
        <section className="panel"><p className="muted">You have not been assigned to any course yet.</p></section>
      ) : (
        <section className="panel">
          <h2>{editingId ? "Edit assignment" : "Post an assignment"}</h2>
          <form onSubmit={submit} className="stack">
            <div className="row">
              <label>Course
                <select value={form.courseCode} onChange={set("courseCode")} disabled={!!editingId}>
                  {courses.map((c) => <option key={c.code} value={c.code}>{c.code} · {c.name}</option>)}
                </select>
              </label>
              <label>Title<input value={form.title} onChange={set("title")} required /></label>
            </div>
            <label>Instructions<textarea rows="3" value={form.description} onChange={set("description")} /></label>
            <div className="row">
              <label>Deadline<input type="datetime-local" value={form.dueAt} onChange={set("dueAt")} required /></label>
              <label>Total marks<input type="number" min="1" step="0.5" value={form.maxMarks} onChange={set("maxMarks")} required /></label>
            </div>
            <div className="actions">
              {editingId ? <button type="button" className="btn ghost" onClick={() => { setEditingId(null); setForm({ ...blank, courseCode: form.courseCode }); }}>Cancel</button> : <span />}
              <button className="btn primary">{editingId ? "Save changes" : "Post assignment"}</button>
            </div>
          </form>
        </section>
      )}

      <section className="panel">
        <h2>My assignments</h2>
        {!list ? <p className="muted">Loading…</p> : list.length === 0 ? <p className="muted">Nothing posted yet.</p> : list.map((a) => (
          <div className="invoice" key={a.id}>
            <div className="panel-head">
              <div>
                <strong>{a.title}</strong> <span className="pill">{a.courseCode}</span>
                <div className="small muted">Due {when(a.dueAt)} · {a.maxMarks} marks · {a.submissionCount} of {a.studentCount} handed in · {a.gradedCount} marked</div>
              </div>
              <div>
                <button className="btn ghost" onClick={() => setOpenId(openId === a.id ? null : a.id)}>{openId === a.id ? "Hide" : "Submissions & marking"}</button>{" "}
                <button className="btn ghost" onClick={() => edit(a)}>Edit</button>{" "}
                <button className="btn ghost danger" onClick={() => remove(a)}>Delete</button>
              </div>
            </div>
            {openId === a.id && <Submissions assignment={a} setError={setError} setOk={setOk} onChanged={load} />}
          </div>
        ))}
      </section>
    </>
  );
}

function Submissions({ assignment, setError, setOk, onChanged }) {
  const [rows, setRows] = useState(null);
  const [edits, setEdits] = useState({});

  const load = () =>
    api.get(`/teacher/assignments/${assignment.id}/submissions`).then((r) => {
      setRows(r.data);
      const e = {};
      r.data.forEach((s) => { e[s.studentId] = { marks: s.marks ?? "", feedback: s.feedback || "" }; });
      setEdits(e);
    }).catch((err) => setError(errMsg(err)));
  useEffect(() => { load(); }, [assignment.id]);

  const save = async (s) => {
    setError(""); setOk("");
    const e = edits[s.studentId];
    try {
      await api.put(`/teacher/assignments/${assignment.id}/grade`, { studentId: s.studentId, marks: Number(e.marks), feedback: e.feedback });
      setOk(`Marked ${s.name}.`);
      load(); onChanged();
    } catch (err) { setError(errMsg(err)); }
  };

  if (!rows) return <p className="muted">Loading…</p>;
  if (rows.length === 0) return <p className="muted">No students are registered for this course.</p>;

  return (
    <table>
      <thead><tr><th>Student</th><th>Work</th><th>Marks (/{assignment.maxMarks})</th><th>Feedback</th><th></th></tr></thead>
      <tbody>
        {rows.map((s) => {
          const [text, cls] = STATUS[s.status];
          const e = edits[s.studentId] || { marks: "", feedback: "" };
          return (
            <tr key={s.studentId}>
              <td>{s.name}<div><span className={`pill ${cls}`}>{text}</span></div></td>
              <td>
                {s.status === "NOT_SUBMITTED" ? <span className="muted small">—</span> : (
                  <>
                    {s.text && <details><summary className="small">Read answer</summary><p style={{ whiteSpace: "pre-wrap" }}>{s.text}</p></details>}
                    {s.link && <a href={s.link} target="_blank" rel="noreferrer" className="small">Open link</a>}
                    <div className="small muted">{when(s.submittedAt)}</div>
                  </>
                )}
              </td>
              <td><input type="number" min="0" max={assignment.maxMarks} step="0.5" style={{ width: 90 }} disabled={s.status === "NOT_SUBMITTED"}
                value={e.marks} onChange={(ev) => setEdits({ ...edits, [s.studentId]: { ...e, marks: ev.target.value } })} /></td>
              <td><input disabled={s.status === "NOT_SUBMITTED"} value={e.feedback}
                onChange={(ev) => setEdits({ ...edits, [s.studentId]: { ...e, feedback: ev.target.value } })} /></td>
              <td className="num"><button className="btn primary" disabled={s.status === "NOT_SUBMITTED" || e.marks === ""} onClick={() => save(s)}>Save</button></td>
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}
