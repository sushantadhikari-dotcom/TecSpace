import { useEffect, useState } from "react";
import api, { errMsg } from "../../api.js";

export default function Announcements() {
  const [list, setList] = useState(null);
  const [form, setForm] = useState({ title: "", message: "" });
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");

  const load = () => api.get("/admin/announcements").then((r) => setList(r.data)).catch((e) => setError(errMsg(e)));
  useEffect(() => { load(); }, []);

  const submit = async (e) => {
    e.preventDefault();
    setError(""); setOk("");
    try {
      await api.post("/admin/announcements", form);
      setForm({ title: "", message: "" });
      setOk("Announcement posted. Students will see it on their dashboard.");
      load();
    } catch (err) { setError(errMsg(err)); }
  };

  const remove = async (a) => {
    try { await api.delete(`/admin/announcements/${a.id}`); load(); }
    catch (err) { setError(errMsg(err)); }
  };

  return (
    <>
      <h1 className="page-title">Announcements</h1>
      {error && <div className="alert error" role="alert">{error}</div>}
      {ok && <div className="alert success" role="status">{ok}</div>}

      <section className="panel">
        <h2>Post a notice</h2>
        <form onSubmit={submit} className="stack">
          <label>Title<input value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required /></label>
          <label>Message
            <textarea rows="4" value={form.message} onChange={(e) => setForm({ ...form, message: e.target.value })} required />
          </label>
          <div><button className="btn primary">Post announcement</button></div>
        </form>
      </section>

      <section className="panel">
        <h2>Recent announcements</h2>
        {!list ? <p className="muted">Loading…</p> : list.length === 0 ? (
          <p className="muted">No announcements yet.</p>
        ) : list.map((a) => (
          <div className="ann" key={a.id}>
            <div className="panel-head">
              <strong>{a.title}</strong>
              <button className="btn ghost danger" onClick={() => remove(a)}>Delete</button>
            </div>
            <span className="small muted">{new Date(a.createdAt).toLocaleString()}</span>
            <p>{a.message}</p>
          </div>
        ))}
      </section>
    </>
  );
}
