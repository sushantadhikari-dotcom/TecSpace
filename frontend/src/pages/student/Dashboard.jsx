import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api, { errMsg, money } from "../../api.js";
import { useAuth } from "../../AuthContext.jsx";

const when = (d) => new Date(d).toLocaleString(undefined, { weekday: "short", day: "numeric", month: "short", hour: "2-digit", minute: "2-digit" });

export default function Dashboard() {
  const { user } = useAuth();
  const [d, setD] = useState(null);
  const [deadlines, setDeadlines] = useState([]);
  const [error, setError] = useState("");

  useEffect(() => {
    api.get("/student/dashboard").then((r) => setD(r.data)).catch((e) => setError(errMsg(e)));
    api.get("/student/assignments")
      .then((r) => setDeadlines(r.data.filter((a) => a.status === "NOT_SUBMITTED" && !a.overdue).slice(0, 5)))
      .catch(() => {});
  }, []);

  if (!d) return <p className="muted">{error || "Loading your dashboard…"}</p>;

  const due = Number(d.remainingBalance) > 0;

  return (
    <>
      <h1 className="page-title">Hello, {user.firstName}</h1>
      <p className="muted" style={{ marginTop: "-1rem" }}>{d.term}</p>

      <div className="stats">
        <Link to="/student/financials" className="stat">
          <span className="muted">Fee due</span>
          <strong className={due ? "warn" : ""}>{money(d.remainingBalance)}</strong>
          <span className="small">{due ? "Pay now" : "All paid"}</span>
        </Link>
        <Link to="/student/courses" className="stat">
          <span className="muted">Registered courses</span>
          <strong>{d.enrolledCount}</strong>
          <span className="small">{d.enrolledCredits} credit hours</span>
        </Link>
        <Link to="/student/assignments" className="stat">
          <span className="muted">Assignments to hand in</span>
          <strong className={deadlines.length ? "warn" : ""}>{deadlines.length}</strong>
          <span className="small">{deadlines.length ? "See deadlines" : "Nothing due"}</span>
        </Link>
        <Link to="/student/timetable" className="stat">
          <span className="muted">Classes today</span>
          <strong>{d.todayClasses.length}</strong>
          <span className="small">See full timetable</span>
        </Link>
      </div>

      {deadlines.length > 0 && (
        <section className="panel">
          <h2>Upcoming deadlines</h2>
          <table>
            <tbody>
              {deadlines.map((a) => (
                <tr key={a.id}>
                  <td><strong>{a.courseCode}</strong> · {a.title}</td>
                  <td className="num">{when(a.dueAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}

      <section className="panel">
        <h2>Today's classes</h2>
        {d.todayClasses.length === 0 ? (
          <p className="muted">No classes today.</p>
        ) : (
          <table>
            <thead><tr><th>Time</th><th>Course</th><th>Room</th><th>Instructor</th></tr></thead>
            <tbody>
              {d.todayClasses.map((c) => (
                <tr key={c.id}>
                  <td>{c.startTime} – {c.endTime}</td>
                  <td><strong>{c.courseCode}</strong> {c.courseName}</td>
                  <td>{c.room}</td>
                  <td>{c.instructorName || "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      <section className="panel">
        <h2>Announcements</h2>
        {d.announcements.length === 0 ? (
          <p className="muted">Nothing new.</p>
        ) : (
          d.announcements.map((a) => (
            <div className="ann" key={a.id}>
              <strong>{a.title}</strong>
              <span className="small muted">{new Date(a.createdAt).toLocaleDateString()}</span>
              <p>{a.message}</p>
            </div>
          ))
        )}
      </section>
    </>
  );
}
