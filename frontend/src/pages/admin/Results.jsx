import { useEffect, useState } from "react";
import api, { errMsg } from "../../api.js";

export default function Results() {
  const [scaleName, setScaleName] = useState("");
  const [bands, setBands] = useState(null);
  const [terms, setTerms] = useState(null);
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");

  const loadTerms = () => api.get("/admin/result-terms").then((r) => setTerms(r.data)).catch((e) => setError(errMsg(e)));
  useEffect(() => {
    loadTerms();
    api.get("/admin/grading-scale").then((r) => {
      setScaleName(r.data.name);
      setBands(r.data.bands.map((b) => ({ letter: b.letter, minPercent: String(b.minPercent), gradePoint: String(b.gradePoint) })));
    }).catch((e) => setError(errMsg(e)));
  }, []);

  const setBand = (i, patch) => setBands(bands.map((b, n) => (n === i ? { ...b, ...patch } : b)));

  const saveScale = async () => {
    setError(""); setOk("");
    try {
      const r = await api.put("/admin/grading-scale", {
        name: scaleName,
        bands: bands.map((b) => ({ letter: b.letter, minPercent: Number(b.minPercent), gradePoint: Number(b.gradePoint) })),
      });
      setBands(r.data.bands.map((b) => ({ letter: b.letter, minPercent: String(b.minPercent), gradePoint: String(b.gradePoint) })));
      setOk("Grading scale saved. It applies the next time grades are saved.");
    } catch (err) { setError(errMsg(err)); }
  };

  const act = async (action, name) => {
    setError(""); setOk("");
    try {
      await api.post(`/admin/result-terms/${action}`, { trimesterName: name });
      setOk(`Done: ${action} "${name}".`);
      loadTerms();
    } catch (err) { setError(errMsg(err)); }
  };

  return (
    <>
      <h1 className="page-title">Results &amp; grading</h1>
      {error && <div className="alert error" role="alert">{error}</div>}
      {ok && <div className="alert success" role="status">{ok}</div>}

      <section className="panel">
        <h2>Trimester results</h2>
        <p className="muted small">
          <strong>Lock</strong> stops anyone changing that trimester's grades. <strong>Publish</strong> locks it too and puts it on
          every student's official transcript and report card, and notifies them.
        </p>
        {!terms ? <p className="muted">Loading…</p> : terms.length === 0 ? (
          <p className="muted">No grades have been entered yet. Add grades from a student's page first.</p>
        ) : (
          <table>
            <thead><tr><th>Trimester</th><th>Students</th><th>Status</th><th></th></tr></thead>
            <tbody>
              {terms.map((t) => (
                <tr key={t.trimesterName}>
                  <td><strong>{t.trimesterName}</strong></td>
                  <td>{t.students}</td>
                  <td>
                    <span className={`pill ${t.locked ? "warn" : ""}`}>{t.locked ? "Locked" : "Open"}</span>{" "}
                    <span className={`pill ${t.published ? "ok" : ""}`}>{t.published ? "Published" : "Not published"}</span>
                  </td>
                  <td className="num">
                    {t.locked
                      ? <button className="btn ghost" onClick={() => act("unlock", t.trimesterName)}>Unlock</button>
                      : <button className="btn ghost" onClick={() => act("lock", t.trimesterName)}>Lock</button>}{" "}
                    {t.published
                      ? <button className="btn ghost danger" onClick={() => act("unpublish", t.trimesterName)}>Unpublish</button>
                      : <button className="btn primary" onClick={() => act("publish", t.trimesterName)}>Publish</button>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      <section className="panel">
        <h2>Grading scale</h2>
        <p className="muted small">
          Letter grades, the minimum percentage that earns each one, and the grade points used for GPA. When an admin enters
          marks (a percentage) for a course, the letter is chosen from this scale automatically.
        </p>
        {!bands ? <p className="muted">Loading…</p> : (
          <>
            <label style={{ maxWidth: 320, marginBottom: "1rem" }}>Scale name<input value={scaleName} onChange={(e) => setScaleName(e.target.value)} /></label>
            <table>
              <thead><tr><th>Letter</th><th>Minimum %</th><th>Grade points</th><th></th></tr></thead>
              <tbody>
                {bands.map((b, i) => (
                  <tr key={i}>
                    <td><input style={{ width: 80 }} value={b.letter} onChange={(e) => setBand(i, { letter: e.target.value })} /></td>
                    <td><input type="number" min="0" max="100" step="0.1" style={{ width: 110 }} value={b.minPercent} onChange={(e) => setBand(i, { minPercent: e.target.value })} /></td>
                    <td><input type="number" min="0" max="10" step="0.1" style={{ width: 110 }} value={b.gradePoint} onChange={(e) => setBand(i, { gradePoint: e.target.value })} /></td>
                    <td className="num"><button className="btn ghost danger" disabled={bands.length <= 2} onClick={() => setBands(bands.filter((_, n) => n !== i))}>Remove</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="actions">
              <button className="btn ghost" onClick={() => setBands([...bands, { letter: "", minPercent: "", gradePoint: "" }])}>Add grade</button>
              <button className="btn primary" onClick={saveScale}>Save grading scale</button>
            </div>
          </>
        )}
      </section>
    </>
  );
}
