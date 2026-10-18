import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api, { errMsg } from "../../api.js";

export default function Courses() {
  const [list, setList] = useState(null);
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");
  const [busy, setBusy] = useState("");

  const load = () => api.get("/student/courses").then((r) => setList(r.data)).catch((e) => setError(errMsg(e)));
  useEffect(() => { load(); }, []);

  const request = async (c) => {
    setError(""); setOk(""); setBusy(c.code);
    try {
      await api.post("/student/enrollments", { courseCode: c.code });
      setOk(`Your request for ${c.code} was sent. You will get a notification when the administration decides.`);
      await load();
    } catch (err) { setError(errMsg(err)); }
    finally { setBusy(""); }
  };

  const remove = async (c, message) => {
    setError(""); setOk(""); setBusy(c.code);
    try {
      await api.delete(`/student/enrollments/${c.code}`);
      setOk(message);
      await load();
    } catch (err) { setError(errMsg(err)); }
    finally { setBusy(""); }
  };

  if (!list) return <p className="muted">{error || "Loading courses…"}</p>;

  const approved = list.filter((c) => c.status === "ENROLLED");
  const waiting = list.filter((c) => c.status === "PENDING");
  const others = list.filter((c) => c.status === "NONE" || c.status === "REJECTED");
  const credits = approved.reduce((n, c) => n + c.creditHours, 0);

  return (
    <>
      <h1 className="page-title">Course registration</h1>
      {error && <div className="alert error" role="alert">{error}</div>}
      {ok && <div className="alert success" role="status">{ok}</div>}

      <section className="panel">
        <p className="muted" style={{ marginTop: 0 }}>
          Pick the courses you want and send a request. Your registration is final only after the administration approves it.
          Once approved, the course appears in your timetable and you can open its syllabus.
        </p>
      </section>

      <section className="panel">
        <div className="panel-head">
          <h2>My approved courses</h2>
          <span className="pill">{approved.length} courses · {credits} credits</span>
        </div>
        {approved.length === 0 ? (
          <p className="muted">No approved courses yet.</p>
        ) : (
          <table>
            <thead><tr><th>Code</th><th>Course</th><th>Schedule</th><th></th></tr></thead>
            <tbody>
              {approved.map((c) => (
                <tr key={c.code}>
                  <td className="mono">{c.code}</td>
                  <td>{c.name}<div className="small muted">{c.instructorName}</div></td>
                  <td className="small">{c.schedule.length ? c.schedule.join(" | ") : "Not scheduled yet"}</td>
                  <td className="num">
                    {c.hasSyllabus
                      ? <Link to={`/student/courses/${c.code}`}>Syllabus</Link>
                      : <Link to={`/student/courses/${c.code}`}>Details</Link>}{" "}
                    <button className="btn ghost danger" disabled={busy === c.code}
                      onClick={() => window.confirm(`Drop ${c.code}?`) && remove(c, `Dropped ${c.code}.`)}>Drop</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      {waiting.length > 0 && (
        <section className="panel">
          <h2>Waiting for approval</h2>
          <table>
            <tbody>
              {waiting.map((c) => (
                <tr key={c.code}>
                  <td className="mono">{c.code}</td>
                  <td>{c.name}</td>
                  <td><span className="pill warn">Waiting for the administration</span></td>
                  <td className="num">
                    <button className="btn ghost" disabled={busy === c.code} onClick={() => remove(c, `Request for ${c.code} cancelled.`)}>Cancel request</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}

      <section className="panel">
        <h2>Available courses</h2>
        {others.length === 0 ? (
          <p className="muted">No other courses are open for registration.</p>
        ) : (
          <table>
            <thead><tr><th>Code</th><th>Course</th><th>Credits</th><th>Prerequisites</th><th>Seats left</th><th></th></tr></thead>
            <tbody>
              {others.map((c) => (
                <tr key={c.code}>
                  <td className="mono">{c.code}</td>
                  <td>
                    {c.name}
                    <div className="small muted">{c.department}{c.instructorName ? ` · ${c.instructorName}` : ""}</div>
                    {c.status === "REJECTED" && (
                      <div className="small" style={{ color: "var(--danger)" }}>Declined{c.note ? `: ${c.note}` : ""}</div>
                    )}
                  </td>
                  <td>{c.creditHours}</td>
                  <td>{c.prerequisites.length ? c.prerequisites.join(", ") : "None"}</td>
                  <td>{c.seatsLeft} / {c.capacity}</td>
                  <td className="num">
                    <button className="btn primary" disabled={busy === c.code || c.seatsLeft === 0} onClick={() => request(c)}>
                      {c.seatsLeft === 0 ? "Full" : c.status === "REJECTED" ? "Ask again" : "Request"}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </>
  );
}
