import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import api, { errMsg } from "../../api.js";

export default function CourseDetail() {
  const { code } = useParams();
  const [c, setC] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    api.get(`/student/courses/${code}/syllabus`).then((r) => setC(r.data)).catch((e) => setError(errMsg(e)));
  }, [code]);

  return (
    <>
      <Link to="/student/courses" className="back">Back to course registration</Link>
      {error && (
        <>
          <h1 className="page-title">{code}</h1>
          <div className="alert error" role="alert">{error}</div>
        </>
      )}

      {c && (
        <>
          <h1 className="page-title">{c.code} · {c.name}</h1>

          <section className="panel">
            <table className="kv">
              <tbody>
                <tr><th>Department</th><td>{c.department}</td></tr>
                <tr><th>Credit hours</th><td>{c.creditHours}</td></tr>
                <tr><th>Instructor</th><td>{c.instructorName || "To be announced"}</td></tr>
                <tr><th>Prerequisites</th><td>{c.prerequisites.length ? c.prerequisites.join(", ") : "None"}</td></tr>
                <tr><th>Classes</th><td>{c.schedule.length ? c.schedule.map((s) => <div key={s}>{s}</div>) : "Not scheduled yet"}</td></tr>
              </tbody>
            </table>
          </section>

          <section className="panel">
            <h2>Syllabus</h2>
            {!c.syllabus && !c.syllabusUrl && <p className="muted">The syllabus has not been added yet. Please check again later.</p>}
            {c.syllabus && <div className="syllabus">{c.syllabus}</div>}
            {c.syllabusUrl && (
              <p><a href={c.syllabusUrl} target="_blank" rel="noreferrer">Open the full syllabus document</a></p>
            )}
          </section>
        </>
      )}
      {!c && !error && <p className="muted">Loading…</p>}
    </>
  );
}
