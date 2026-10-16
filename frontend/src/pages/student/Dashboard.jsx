import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api, { errMsg, money } from "../../api.js";
import { useAuth } from "../../AuthContext.jsx";

export default function Dashboard() {
  const { user } = useAuth();
  const [d, setD] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    api.get("/student/dashboard").then((r) => setD(r.data)).catch((e) => setError(errMsg(e)));
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
        <Link to="/student/timetable" className="stat">
          <span className="muted">Classes today</span>
          <strong>{d.todayClasses.length}</strong>
          <span className="small">See full timetable</span>
        </Link>
      </div>

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
