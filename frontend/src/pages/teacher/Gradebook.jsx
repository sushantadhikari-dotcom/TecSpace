import { useEffect, useState } from "react";
import api, { errMsg } from "../../api.js";

export default function Gradebook() {
  const [courses, setCourses] = useState(null);
  const [courseCode, setCourseCode] = useState("");
  const [trimester, setTrimester] = useState("");
  const [applied, setApplied] = useState("");
  const [view, setView] = useState(null);
  const [inputs, setInputs] = useState({});
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");

  useEffect(() => {
    api.get("/teacher/courses").then((r) => {
      setCourses(r.data);
      if (r.data[0]) setCourseCode(r.data[0].code);
    }).catch((e) => setError(errMsg(e)));
  }, []);

  const load = () => {
    if (!courseCode) return;
    setView(null);
    api.get("/teacher/gradebook", { params: { courseCode, trimesterName: applied || undefined } })
      .then((r) => {
        setView(r.data);
        setTrimester(r.data.trimesterName);
        const i = {};
        r.data.rows.forEach((x) => { i[x.studentId] = x.finalPercentage ?? ""; });
        setInputs(i);
      })
      .catch((e) => setError(errMsg(e)));
  };
  useEffect(() => { load(); }, [courseCode, applied]);

  const useCoursework = () => {
    const i = { ...inputs };
    view.rows.forEach((x) => { if (x.courseworkPercent != null) i[x.studentId] = x.courseworkPercent; });
    setInputs(i);
  };

  const save = async () => {
    setError(""); setOk("");
    const marks = view.rows.filter((x) => inputs[x.studentId] !== "" && inputs[x.studentId] != null)
      .map((x) => ({ studentId: x.studentId, percentage: Number(inputs[x.studentId]) }));
    if (marks.length === 0) { setError("Enter at least one mark first"); return; }
    try {
      const r = await api.put("/teacher/gradebook", { courseCode, trimesterName: view.trimesterName, marks });
      setOk(`Saved final marks for ${r.data.saved} students. Letter grades and GPA were worked out from the grading scale.`);
      load();
    } catch (err) { setError(errMsg(err)); }
  };

  if (!courses) return <p className="muted">{error || "Loading…"}</p>;

  return (
    <>
      <h1 className="page-title">Gradebook</h1>
      {error && <div className="alert error" role="alert">{error}</div>}
      {ok && <div className="alert success" role="status">{ok}</div>}

      {courses.length === 0 ? (
        <section className="panel"><p className="muted">You have not been assigned to any course yet.</p></section>
      ) : (
        <section className="panel">
          <div className="inline-form">
            <label>Course
              <select value={courseCode} onChange={(e) => { setApplied(""); setCourseCode(e.target.value); }}>
                {courses.map((c) => <option key={c.code} value={c.code}>{c.code} · {c.name}</option>)}
              </select>
            </label>
            <label>Trimester name
              <input value={trimester} onChange={(e) => setTrimester(e.target.value)} />
            </label>
            <button className="btn ghost" onClick={() => setApplied(trimester)}>Load</button>
          </div>
          <p className="muted small">
            Final marks go straight into each student's grade record under this trimester name. Use the same name the
            administration uses for this trimester so results are published together.
          </p>

          {view?.locked && (
            <div className="alert error" role="alert">
              "{view.trimesterName}" is locked or published. You can look, but an administrator must unlock it before you can change grades.
            </div>
          )}

          {!view ? <p className="muted">Loading…</p> : view.rows.length === 0 ? (
            <p className="muted">No students are registered for this course.</p>
          ) : (
            <>
              <table>
                <thead>
                  <tr><th>Student</th><th>Coursework ({view.assignmentCount} assignments)</th><th>Final marks %</th><th>Grade</th></tr>
                </thead>
                <tbody>
                  {view.rows.map((x) => (
                    <tr key={x.studentId}>
                      <td>{x.name}</td>
                      <td>
                        {x.gradedCount === 0 ? <span className="muted small">Nothing marked yet</span> : (
                          <>{x.courseworkEarned} / {x.courseworkPossible} <span className="pill">{x.courseworkPercent}%</span></>
                        )}
                      </td>
                      <td>
                        <input type="number" min="0" max="100" step="0.1" style={{ width: 100 }} disabled={view.locked}
                          value={inputs[x.studentId] ?? ""} onChange={(e) => setInputs({ ...inputs, [x.studentId]: e.target.value })} />
                      </td>
                      <td><strong>{x.grade || "—"}</strong></td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <div className="actions">
                <button className="btn ghost" onClick={useCoursework} disabled={view.locked}>Fill in from coursework %</button>
                <button className="btn primary" onClick={save} disabled={view.locked}>Save final marks</button>
              </div>
            </>
          )}
        </section>
      )}
    </>
  );
}
