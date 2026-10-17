import { useEffect, useState } from "react";
import api, { errMsg, money } from "../../api.js";
import { downloadCsv } from "../../csv.js";

function Bars({ rows, fmt, empty = "No data yet." }) {
  if (!rows || rows.length === 0) return <p className="muted small">{empty}</p>;
  const biggest = Math.max(...rows.map((r) => r.max || r.value), 1);
  return (
    <div>
      {rows.map((r) => (
        <div className="hbar" key={r.label}>
          <span className="hbar-label">{r.label}</span>
          <div className="hbar-track"><div className="hbar-fill" style={{ width: `${Math.min(100, (r.value / (r.max || biggest)) * 100)}%` }} /></div>
          <span className="hbar-value">{fmt ? fmt(r) : r.value}</span>
        </div>
      ))}
    </div>
  );
}

const EXPORTS = [
  ["students", "Student list"], ["enrollments", "Course enrolments"], ["attendance", "Attendance"],
  ["finance", "Fees & collections"], ["grades", "Grades"],
];

export default function Analytics() {
  const [d, setD] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    api.get("/admin/analytics").then((r) => setD(r.data)).catch((e) => setError(errMsg(e)));
  }, []);

  const exportCsv = async (kind) => {
    setError("");
    try {
      const r = await api.get(`/admin/analytics/export/${kind}`);
      downloadCsv(`tecspace-${kind}.csv`, r.data);
    } catch (err) { setError(errMsg(err)); }
  };

  if (!d) return <p className="muted">{error || "Loading analytics…"}</p>;

  const collectedPct = Number(d.billed) > 0 ? Math.round((Number(d.collected) / Number(d.billed)) * 100) : 0;

  return (
    <>
      <h1 className="page-title">Analytics</h1>
      {error && <div className="alert error" role="alert">{error}</div>}

      <div className="stats">
        <div className="stat"><span className="muted">Students</span><strong>{d.students}</strong></div>
        <div className="stat"><span className="muted">Collected</span><strong>{money(d.collected)}</strong><span className="small">{collectedPct}% of {money(d.billed)} billed</span></div>
        <div className="stat"><span className="muted">Outstanding</span><strong className="warn">{money(d.outstanding)}</strong></div>
        <div className="stat"><span className="muted">Attendance flags</span><strong className={d.attendanceFlagged ? "warn" : ""}>{d.attendanceFlagged}</strong><span className="small">students need attention</span></div>
      </div>

      <div className="charts">
        <section className="panel"><h2>Students by programme</h2><Bars rows={d.studentsByFaculty} /></section>
        <section className="panel"><h2>Students by intake</h2><Bars rows={d.studentsBySession} /></section>
        <section className="panel"><h2>Applications</h2><Bars rows={d.applicationsByStatus} empty="No applications yet." /></section>
        <section className="panel"><h2>Course seats filled</h2><Bars rows={d.courseFill} fmt={(r) => `${r.value} / ${r.max}`} empty="No courses yet." /></section>
        <section className="panel"><h2>Average attendance by course</h2><Bars rows={d.attendanceByCourse} fmt={(r) => `${r.value}%`} empty="No attendance taken yet." /></section>
        <section className="panel"><h2>Grade distribution</h2><Bars rows={d.gradeDistribution} empty="No grades yet." /></section>
        <section className="panel"><h2>Average CGPA by programme</h2><Bars rows={d.avgCgpaByFaculty} fmt={(r) => r.value.toFixed(2)} empty="No grades yet." /></section>
        <section className="panel"><h2>Fees collected, last 6 months</h2><Bars rows={d.monthlyCollections} fmt={(r) => money(r.value)} /></section>
      </div>

      <section className="panel">
        <h2>Reports for download</h2>
        <p className="muted small">CSV files open in Excel. Use them for regulatory and compliance reporting.</p>
        <div className="actions" style={{ justifyContent: "flex-start", gap: "0.6rem", flexWrap: "wrap" }}>
          {EXPORTS.map(([kind, label]) => <button key={kind} className="btn ghost" onClick={() => exportCsv(kind)}>{label}</button>)}
        </div>
      </section>
    </>
  );
}
