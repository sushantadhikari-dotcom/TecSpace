import { useEffect, useState } from "react";
import api, { errMsg } from "../api.js";

export const TYPE = {
  REGISTRATION: ["Registration", "ok"], CLASSES: ["Classes", ""], EXAM: ["Exams", "warn"],
  HOLIDAY: ["Holiday", "bad"], DEADLINE: ["Deadline", "warn"], OTHER: ["Other", ""],
};
const fmt = (d) => new Date(d + "T00:00:00").toLocaleDateString(undefined, { day: "numeric", month: "short", year: "numeric" });

export default function Calendar() {
  const [data, setData] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    api.get("/calendar").then((r) => setData(r.data)).catch((e) => setError(errMsg(e)));
  }, []);

  if (!data) return <p className="muted">{error || "Loading calendar…"}</p>;

  const today = new Date().toISOString().slice(0, 10);
  const upcoming = data.events.filter((e) => e.endDate >= today);
  const past = data.events.filter((e) => e.endDate < today).reverse();

  const row = (e) => {
    const [label, cls] = TYPE[e.type] || TYPE.OTHER;
    const now = e.startDate <= today && e.endDate >= today;
    return (
      <tr key={e.id}>
        <td style={{ whiteSpace: "nowrap" }}>{fmt(e.startDate)}{e.endDate !== e.startDate && <> – {fmt(e.endDate)}</>}</td>
        <td><strong>{e.title}</strong>{e.note && <div className="small muted">{e.note}</div>}</td>
        <td><span className={`pill ${cls}`}>{label}</span> {now && <span className="pill ok">Now</span>}</td>
      </tr>
    );
  };

  return (
    <>
      <h1 className="page-title">Academic calendar</h1>
      {data.registrationMessage && (
        <div className={`alert ${data.registrationOpen ? "success" : "error"}`} role="status">{data.registrationMessage}</div>
      )}

      <section className="panel">
        <h2>Upcoming and current</h2>
        {upcoming.length === 0 ? <p className="muted">Nothing scheduled yet.</p> : (
          <table><tbody>{upcoming.map(row)}</tbody></table>
        )}
      </section>

      {past.length > 0 && (
        <section className="panel">
          <h2>Earlier</h2>
          <table><tbody>{past.map(row)}</tbody></table>
        </section>
      )}
    </>
  );
}
