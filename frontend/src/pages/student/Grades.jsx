import { useEffect, useState } from "react";
import api, { errMsg } from "../../api.js";

export default function Grades() {
  const [data, setData] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    api.get("/student/grades").then((r) => setData(r.data)).catch((e) => setError(errMsg(e)));
  }, []);

  if (!data) return <p className="muted">{error || "Loading your grades…"}</p>;

  return (
    <>
      <h1 className="page-title">Grades</h1>

      <section className="panel cgpa">
        <span className="muted">Cumulative GPA</span>
        <div className="big">{data.cgpa.toFixed(2)}</div>
      </section>

      {data.trimesters.length === 0 && (
        <section className="panel">
          <p className="muted">No grades have been published yet. They will appear here once your faculty uploads them.</p>
        </section>
      )}

      {data.trimesters.map((t) => (
        <section className="panel" key={t.trimesterName}>
          <div className="panel-head">
            <h2>{t.trimesterName}</h2>
            <span className="pill">GPA {t.gpa.toFixed(2)}</span>
          </div>
          <table>
            <thead><tr><th>Code</th><th>Course</th><th className="num">Credits</th><th className="num">Grade</th></tr></thead>
            <tbody>
              {t.courses.map((c) => (
                <tr key={c.courseCode}>
                  <td className="mono">{c.courseCode}</td>
                  <td>{c.courseName}</td>
                  <td className="num">{c.creditHours}</td>
                  <td className="num"><strong>{c.grade}</strong></td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      ))}
    </>
  );
}
