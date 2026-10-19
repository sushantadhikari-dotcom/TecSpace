import { useEffect, useState } from "react";
import api, { errMsg } from "../../api.js";
import { TYPE } from "../Calendar.jsx";

const blank = { title: "", type: "REGISTRATION", startDate: "", endDate: "", note: "" };

export default function CalendarAdmin() {
  const [list, setList] = useState(null);
  const [form, setForm] = useState(blank);
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");
  const [sent, setSent] = useState(null);

  const load = () => api.get("/calendar").then((r) => setList(r.data.events)).catch((e) => setError(errMsg(e)));
  useEffect(() => { load(); }, []);

  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setError(""); setOk("");
    try {
      if (editingId) await api.put(`/admin/calendar/${editingId}`, form);
      else await api.post("/admin/calendar", form);
      setOk(editingId ? "Event updated." : "Event added.");
      setForm(blank); setEditingId(null);
      load();
    } catch (err) { setError(errMsg(err)); }
  };

  const remove = async (ev) => {
    if (!window.confirm(`Delete "${ev.title}"?`)) return;
    setError(""); setOk("");
    try { await api.delete(`/admin/calendar/${ev.id}`); load(); } catch (err) { setError(errMsg(err)); }
  };

  const runReminders = async () => {
    setError(""); setOk(""); setSent(null);
    try { setSent((await api.post("/admin/reminders/run")).data); } catch (err) { setError(errMsg(err)); }
  };

  return (
    <>
      <h1 className="page-title">Calendar &amp; reminders</h1>
      {error && <div className="alert error" role="alert">{error}</div>}
      {ok && <div className="alert success" role="status">{ok}</div>}

      <section className="panel">
        <h2>{editingId ? "Edit event" : "Add an event"}</h2>
        <form onSubmit={submit} className="stack">
          <div className="row">
            <label>Title<input value={form.title} onChange={set("title")} placeholder="Fall 2026 course registration" required /></label>
            <label>Type
              <select value={form.type} onChange={set("type")}>
                {Object.entries(TYPE).map(([k, v]) => <option key={k} value={k}>{v[0]}</option>)}
              </select>
            </label>
          </div>
          <div className="row">
            <label>Starts<input type="date" value={form.startDate} onChange={set("startDate")} required /></label>
            <label>Ends<input type="date" value={form.endDate} onChange={set("endDate")} required /></label>
          </div>
          <label>Note (optional)<input value={form.note} onChange={set("note")} /></label>
          <div className="actions">
            {editingId ? <button type="button" className="btn ghost" onClick={() => { setForm(blank); setEditingId(null); }}>Cancel</button> : <span />}
            <button className="btn primary">{editingId ? "Save changes" : "Add event"}</button>
          </div>
        </form>
        <p className="muted small">
          <strong>Registration</strong> events are the windows when students can ask for courses. If you add none,
          registration is always open. You can still register students yourself at any time.
        </p>
      </section>

      <section className="panel">
        <h2>All events</h2>
        {!list ? <p className="muted">Loading…</p> : list.length === 0 ? <p className="muted">No events yet.</p> : (
          <table>
            <thead><tr><th>Dates</th><th>Event</th><th>Type</th><th></th></tr></thead>
            <tbody>
              {list.map((e) => {
                const [label, cls] = TYPE[e.type] || TYPE.OTHER;
                return (
                  <tr key={e.id}>
                    <td style={{ whiteSpace: "nowrap" }}>{e.startDate}{e.endDate !== e.startDate && ` → ${e.endDate}`}</td>
                    <td>{e.title}{e.note && <div className="small muted">{e.note}</div>}</td>
                    <td><span className={`pill ${cls}`}>{label}</span></td>
                    <td className="num">
                      <button className="btn ghost" onClick={() => { setForm({ title: e.title, type: e.type, startDate: e.startDate, endDate: e.endDate, note: e.note || "" }); setEditingId(e.id); window.scrollTo({ top: 0, behavior: "smooth" }); }}>Edit</button>{" "}
                      <button className="btn ghost danger" onClick={() => remove(e)}>Delete</button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </section>

      <section className="panel">
        <h2>Automatic reminders</h2>
        <p className="muted">
          Every morning at 8:00 the system reminds students about fee installments and invoices that are due or overdue,
          assignments due within 24 hours that they have not handed in, and exams tomorrow. Nobody gets the same reminder twice.
        </p>
        <button className="btn primary" onClick={runReminders}>Send reminders now</button>
        {sent && (
          <div className="alert success" role="status" style={{ marginTop: "1rem" }}>
            Sent: {sent.fees} fee reminders, {sent.assignments} assignment reminders, {sent.exams} exam reminders.
          </div>
        )}
        <p className="muted small">Only administrators can press this button. It is useful for a demo.</p>
      </section>
    </>
  );
}
