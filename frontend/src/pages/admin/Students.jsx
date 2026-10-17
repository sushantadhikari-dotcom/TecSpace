import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api, { errMsg } from "../../api.js";

export default function Students() {
  const [students, setStudents] = useState(null);
  const [error, setError] = useState("");
  const [q, setQ] = useState("");

  useEffect(() => {
    api.get("/admin/students").then((r) => setStudents(r.data)).catch((e) => setError(errMsg(e)));
  }, []);

  if (!students) return <p className="muted">{error || "Loading students…"}</p>;

  const filtered = students.filter((s) =>
    `${s.firstName} ${s.lastName} ${s.universityEmail} ${s.faculty}`.toLowerCase().includes(q.toLowerCase())
  );

  return (
    <>
      <h1 className="page-title">Students</h1>
      <section className="panel">
        <div className="panel-head">
          <span className="muted">{students.length} enrolled</span>
          <input className="search" placeholder="Search by name, email or faculty" value={q} onChange={(e) => setQ(e.target.value)} />
        </div>
        {filtered.length === 0 ? (
          <p className="muted">No students match your search.</p>
        ) : (
          <table>
            <thead><tr><th>Name</th><th>Email</th><th>Faculty</th><th>Session</th><th></th></tr></thead>
            <tbody>
              {filtered.map((s) => (
                <tr key={s.id}>
                  <td>{s.firstName} {s.lastName}</td>
                  <td>{s.universityEmail}</td>
                  <td>{s.faculty}</td>
                  <td>{s.session}</td>
                  <td className="num">
                    <Link to={`/admin/records/${s.id}`}>Record</Link>{" · "}
                    <Link to={`/admin/students/${s.id}`}>Fees &amp; grades</Link>
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
