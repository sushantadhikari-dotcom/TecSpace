import { useEffect, useState } from "react";
import api, { errMsg } from "../../api.js";

const label = (d) => d.charAt(0) + d.slice(1).toLowerCase();

export default function Courses() {
  const [courses, setCourses] = useState(null);
  const [slots, setSlots] = useState([]);
  const [error, setError] = useState("");

  useEffect(() => {
    api.get("/teacher/courses").then((r) => setCourses(r.data)).catch((e) => setError(errMsg(e)));
    api.get("/teacher/timetable").then((r) => setSlots(r.data)).catch(() => {});
  }, []);

  if (!courses) return <p className="muted">{error || "Loading your courses…"}</p>;

  return (
    <>
      <h1 className="page-title">My courses</h1>
      <section className="panel">
        {courses.length === 0 ? (
          <p className="muted">You have not been assigned to any course yet. Ask the administration to assign your courses.</p>
        ) : (
          <table>
            <thead><tr><th>Code</th><th>Course</th><th>Students</th><th>Schedule</th></tr></thead>
            <tbody>
              {courses.map((c) => (
                <tr key={c.code}>
                  <td className="mono">{c.code}</td>
                  <td>{c.name}<div className="small muted">{c.department} · {c.creditHours} credits</div></td>
                  <td>{c.enrolledCount} / {c.capacity}</td>
                  <td className="small">{c.schedule.length ? c.schedule.join(" | ") : "Not scheduled yet"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      {slots.length > 0 && (
        <section className="panel">
          <h2>My weekly timetable</h2>
          <table>
            <thead><tr><th>Day</th><th>Time</th><th>Course</th><th>Room</th></tr></thead>
            <tbody>
              {slots.map((s) => (
                <tr key={s.id}><td>{label(s.day)}</td><td>{s.startTime} – {s.endTime}</td>
                  <td><strong>{s.courseCode}</strong> {s.courseName}</td><td>{s.room}</td></tr>
              ))}
            </tbody>
          </table>
        </section>
      )}
    </>
  );
}
