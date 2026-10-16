import { useEffect, useState } from "react";
import api, { errMsg } from "../../api.js";

const LEVEL = {
  OK: { text: "Good", cls: "ok" },
  WARNING: { text: "Close to the limit", cls: "warn" },
  LOW: { text: "Below minimum", cls: "bad" },
  NO_DATA: { text: "No classes yet", cls: "" },
};

export default function Attendance() {
  const [data, setData] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    api.get("/student/attendance").then((r) => setData(r.data)).catch((e) => setError(errMsg(e)));
  }, []);

  if (!data) return <p className="muted">{error || "Loading attendance…"}</p>;

  const worrying = data.courses.filter((c) => c.level === "LOW" || c.level === "WARNING");

  return (
    <>
      <h1 className="page-title">Attendance</h1>

      {worrying.length > 0 && (
        <div className="alert error" role="alert">
          Your attendance is at or near the {data.threshold}% minimum in:{" "}
          <strong>{worrying.map((c) => c.courseCode).join(", ")}</strong>. Please attend upcoming classes.
        </div>
      )}

      <section className="panel">
        <p className="muted">
          Minimum required: {data.threshold}%. Excused absences are not counted against you.
        </p>
        {data.courses.length === 0 ? (
          <p className="muted">Register for courses to see your attendance here.</p>
        ) : (
          <table>
            <thead><tr><th>Course</th><th>Attended</th><th style={{ width: "30%" }}>Percentage</th><th>Status</th></tr></thead>
            <tbody>
              {data.courses.map((c) => {
                const lv = LEVEL[c.level] || LEVEL.OK;
                return (
                  <tr key={c.courseCode}>
                    <td><strong>{c.courseCode}</strong> {c.courseName}</td>
                    <td>{c.attended} / {c.sessions}</td>
                    <td>
                      <div className="bar" role="progressbar" aria-valuenow={Math.round(c.percent)} aria-valuemin={0} aria-valuemax={100}>
                        <div style={{ width: `${c.percent}%` }} className={c.level === "LOW" ? "low" : ""} />
                      </div>
                      <span className="small">{c.sessions === 0 ? "—" : `${c.percent}%`}</span>
                    </td>
                    <td><span className={`pill ${lv.cls}`}>{lv.text}</span></td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </section>
    </>
  );
}
