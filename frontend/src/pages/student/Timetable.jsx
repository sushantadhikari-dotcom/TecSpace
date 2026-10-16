import { useEffect, useState } from "react";
import api, { errMsg } from "../../api.js";

const DAYS = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];
const label = (d) => d.charAt(0) + d.slice(1).toLowerCase();

export default function Timetable() {
  const [slots, setSlots] = useState(null);
  const [exams, setExams] = useState([]);
  const [error, setError] = useState("");

  useEffect(() => {
    api.get("/student/timetable").then((r) => setSlots(r.data)).catch((e) => setError(errMsg(e)));
    api.get("/student/exams").then((r) => setExams(r.data)).catch(() => {});
  }, []);

  if (!slots) return <p className="muted">{error || "Loading timetable…"}</p>;

  const today = DAYS[(new Date().getDay() + 6) % 7];
  const days = DAYS.filter((d) => slots.some((s) => s.day === d));

  return (
    <>
      <h1 className="page-title">Timetable &amp; exams</h1>

      {slots.length === 0 ? (
        <section className="panel">
          <p className="muted">
            Your timetable is empty. Register for courses first, then your classes will appear here once the
            administration has scheduled them.
          </p>
        </section>
      ) : (
        <div className="week">
          {days.map((d) => (
            <section key={d} className={`panel day ${d === today ? "today" : ""}`}>
              <h2>{label(d)} {d === today && <span className="pill ok">Today</span>}</h2>
              {slots.filter((s) => s.day === d).map((s) => (
                <div className="slot" key={s.id}>
                  <strong>{s.startTime} – {s.endTime}</strong>
                  <div>{s.courseCode} · {s.courseName}</div>
                  <div className="small muted">Room {s.room}{s.instructorName ? ` · ${s.instructorName}` : ""}</div>
                </div>
              ))}
            </section>
          ))}
        </div>
      )}

      <section className="panel" style={{ marginTop: "1.25rem" }}>
        <h2>Exam schedule</h2>
        {exams.length === 0 ? (
          <p className="muted">No exams have been scheduled for your courses yet. You will get a notification when one is added.</p>
        ) : (
          <table>
            <thead><tr><th>Date</th><th>Time</th><th>Course</th><th>Venue</th><th>Note</th></tr></thead>
            <tbody>
              {exams.map((e) => (
                <tr key={e.id}>
                  <td>{new Date(e.date + "T00:00:00").toLocaleDateString(undefined, { weekday: "short", day: "numeric", month: "short", year: "numeric" })}</td>
                  <td>{e.startTime} – {e.endTime}</td>
                  <td><strong>{e.courseCode}</strong> {e.courseName}</td>
                  <td>Room {e.room}</td>
                  <td>{e.note || "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </>
  );
}
