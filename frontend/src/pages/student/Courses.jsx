import { useEffect, useState } from "react";
import api, { errMsg } from "../../api.js";

export default function Courses() {
  const [list, setList] = useState(null);
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");
  const [busy, setBusy] = useState("");

  const load = () => api.get("/student/courses").then((r) => setList(r.data)).catch((e) => setError(errMsg(e)));
  useEffect(() => { load(); }, []);

  const act = async (c, adding) => {
    setError(""); setOk(""); setBusy(c.code);
    try {
      if (adding) await api.post("/student/enrollments", { courseCode: c.code });
      else await api.delete(`/student/enrollments/${c.code}`);
      setOk(adding ? `Registered for ${c.code}.` : `Dropped ${c.code}.`);
      await load();
    } catch (err) {
      setError(errMsg(err));
    } finally {
      setBusy("");
    }
  };

  if (!list) return <p className="muted">{error || "Loading courses…"}</p>;

  const mine = list.filter((c) => c.enrolled);
  const credits = mine.reduce((n, c) => n + c.creditHours, 0);

  return (
    <>
      <h1 className="page-title">Course registration</h1>
      {error && <div className="alert error" role="alert">{error}</div>}
      {ok && <div className="alert success" role="status">{ok}</div>}

      <section className="panel">
        <div className="panel-head">
          <h2>My courses</h2>
          <span className="pill">{mine.length} courses · {credits} credits</span>
        </div>
        {mine.length === 0 ? (
          <p className="muted">You have not registered for any course yet. Pick from the list below.</p>
        ) : (
          <table>
            <thead><tr><th>Code</th><th>Course</th><th>Schedule</th><th></th></tr></thead>
            <tbody>
              {mine.map((c) => (
                <tr key={c.code}>
                  <td className="mono">{c.code}</td>
                  <td>{c.name}</td>
                  <td className="small">{c.schedule.length ? c.schedule.join(" | ") : "Not scheduled yet"}</td>
                  <td className="num">
                    <button className="btn ghost danger" disabled={busy === c.code} onClick={() => act(c, false)}>Drop</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      <section className="panel">
        <h2>Available courses</h2>
        {list.length === 0 ? (
          <p className="muted">No courses are open for registration yet.</p>
        ) : (
          <table>
            <thead>
              <tr><th>Code</th><th>Course</th><th>Credits</th><th>Prerequisites</th><th>Seats left</th><th></th></tr>
            </thead>
            <tbody>
              {list.map((c) => (
                <tr key={c.code}>
                  <td className="mono">{c.code}</td>
                  <td>
                    {c.name}
                    <div className="small muted">{c.department}{c.instructorName ? ` · ${c.instructorName}` : ""}</div>
                  </td>
                  <td>{c.creditHours}</td>
                  <td>{c.prerequisites.length ? c.prerequisites.join(", ") : "None"}</td>
                  <td>{c.seatsLeft} / {c.capacity}</td>
                  <td className="num">
                    {c.enrolled ? (
                      <span className="pill ok">Registered</span>
                    ) : (
                      <button className="btn primary" disabled={busy === c.code || c.seatsLeft === 0} onClick={() => act(c, true)}>
                        {c.seatsLeft === 0 ? "Full" : "Add"}
                      </button>
                    )}
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
