import { useEffect, useState } from "react";
import api, { errMsg } from "../../api.js";

const STATUSES = ["PRESENT", "ABSENT", "LATE", "EXCUSED"];
const todayStr = () => {
  const d = new Date();
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
};
const LEVEL = { OK: ["Good", "ok"], WARNING: ["Close to limit", "warn"], LOW: ["Below minimum", "bad"], NO_DATA: ["No data", ""] };

export default function Attendance() {
  const [tab, setTab] = useState("register");
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");

  return (
    <>
      <h1 className="page-title">Attendance</h1>
      {error && <div className="alert error" role="alert">{error}</div>}
      {ok && <div className="alert success" role="status">{ok}</div>}

      <div className="tabs" role="tablist">
        <button role="tab" aria-selected={tab === "register"} className={tab === "register" ? "active" : ""} onClick={() => setTab("register")}>Take register</button>
        <button role="tab" aria-selected={tab === "report"} className={tab === "report" ? "active" : ""} onClick={() => setTab("report")}>Report &amp; flags</button>
      </div>

      {tab === "register" ? <Register setError={setError} setOk={setOk} /> : <Report setError={setError} />}
    </>
  );
}

function Register({ setError, setOk }) {
  const [courses, setCourses] = useState([]);
  const [courseCode, setCourseCode] = useState("");
  const [date, setDate] = useState(todayStr());
  const [roster, setRoster] = useState(null);
  const [marks, setMarks] = useState({});

  useEffect(() => {
    api.get("/admin/courses").then((r) => {
      setCourses(r.data);
      if (r.data[0]) setCourseCode(r.data[0].code);
    }).catch((e) => setError(errMsg(e)));
  }, [setError]);

  useEffect(() => {
    if (!courseCode || !date) return;
    setRoster(null);
    api.get("/admin/attendance/roster", { params: { courseCode, date } })
      .then((r) => {
        setRoster(r.data);
        const m = {};
        r.data.forEach((s) => { m[s.studentId] = s.status || "PRESENT"; });
        setMarks(m);
      })
      .catch((e) => setError(errMsg(e)));
  }, [courseCode, date, setError]);

  const setAll = (status) => {
    const m = {};
    roster.forEach((s) => { m[s.studentId] = status; });
    setMarks(m);
  };

  const save = async () => {
    setError(""); setOk("");
    try {
      const entries = roster.map((s) => ({ studentId: s.studentId, status: marks[s.studentId] }));
      const r = await api.put("/admin/attendance", { courseCode, date, entries });
      setOk(`Register saved for ${r.data.saved} students.`);
    } catch (err) { setError(errMsg(err)); }
  };

  return (
    <section className="panel">
      {courses.length === 0 ? <p className="muted">Create a course first.</p> : (
        <>
          <div className="inline-form">
            <label>Course
              <select value={courseCode} onChange={(e) => setCourseCode(e.target.value)}>
                {courses.map((c) => <option key={c.code} value={c.code}>{c.code} · {c.name}</option>)}
              </select>
            </label>
            <label>Date<input type="date" max={todayStr()} value={date} onChange={(e) => setDate(e.target.value)} /></label>
            {roster && roster.length > 0 && (
              <>
                <button className="btn ghost" onClick={() => setAll("PRESENT")}>All present</button>
                <button className="btn ghost" onClick={() => setAll("ABSENT")}>All absent</button>
              </>
            )}
          </div>

          {!roster ? <p className="muted">Loading register…</p> : roster.length === 0 ? (
            <p className="muted">No students are registered for this course yet.</p>
          ) : (
            <>
              <table>
                <thead><tr><th>Student</th><th>Status</th></tr></thead>
                <tbody>
                  {roster.map((s) => (
                    <tr key={s.studentId}>
                      <td>{s.name}</td>
                      <td>
                        <select value={marks[s.studentId] || "PRESENT"}
                          onChange={(e) => setMarks({ ...marks, [s.studentId]: e.target.value })}>
                          {STATUSES.map((st) => <option key={st} value={st}>{st.charAt(0) + st.slice(1).toLowerCase()}</option>)}
                        </select>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <div className="actions"><span /><button className="btn primary" onClick={save}>Save register</button></div>
            </>
          )}
        </>
      )}
    </section>
  );
}

function Report({ setError }) {
  const [rows, setRows] = useState(null);
  const [flaggedOnly, setFlaggedOnly] = useState(true);
  const [threshold, setThreshold] = useState("");
  const [used, setUsed] = useState(null);

  useEffect(() => {
    setError("");
    const params = { flaggedOnly };
    if (threshold) params.threshold = threshold;
    api.get("/admin/attendance/report", { params })
      .then((r) => { setRows(r.data.rows); setUsed(r.data.threshold); })
      .catch((e) => setError(errMsg(e)));
  }, [flaggedOnly, threshold, setError]);

  return (
    <section className="panel">
      <div className="inline-form">
        <label className="check">
          <input type="checkbox" checked={flaggedOnly} onChange={(e) => setFlaggedOnly(e.target.checked)} />
          Only show students with problems
        </label>
        <label>Minimum %
          <input type="number" min="1" max="100" style={{ width: 100 }} value={threshold}
            placeholder={used ?? ""} onChange={(e) => setThreshold(e.target.value)} />
        </label>
      </div>
      <p className="muted small">
        Flagged = below the minimum, within 5 points of it, or absent 3 or more times in the last 5 classes.
        Excused absences are not counted.
      </p>

      {!rows ? <p className="muted">Loading…</p> : rows.length === 0 ? (
        <p className="muted">No students to show.</p>
      ) : (
        <table>
          <thead><tr><th>Student</th><th>Course</th><th>Attended</th><th>%</th><th>Status</th><th>Recent</th></tr></thead>
          <tbody>
            {rows.map((r) => {
              const [text, cls] = LEVEL[r.level] || LEVEL.OK;
              return (
                <tr key={r.studentId + r.courseCode}>
                  <td>{r.studentName}</td>
                  <td><strong>{r.courseCode}</strong></td>
                  <td>{r.attended} / {r.sessions}</td>
                  <td>{r.sessions === 0 ? "—" : `${r.percent}%`}</td>
                  <td><span className={`pill ${cls}`}>{text}</span></td>
                  <td>{r.trending ? <span className="pill bad">Missed {r.recentAbsences} of last 5</span> : (r.recentAbsences ? `${r.recentAbsences} of last 5 missed` : "—")}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      )}
    </section>
  );
}
