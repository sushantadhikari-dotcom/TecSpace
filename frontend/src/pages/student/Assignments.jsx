import { useEffect, useState } from "react";
import api, { errMsg } from "../../api.js";

const STATUS = {
  NOT_SUBMITTED: ["Not handed in", ""], SUBMITTED: ["Handed in", "ok"], LATE: ["Handed in late", "warn"], GRADED: ["Marked", "ok"],
};
const when = (d) => new Date(d).toLocaleString(undefined, { weekday: "short", day: "numeric", month: "short", hour: "2-digit", minute: "2-digit" });

export default function Assignments() {
  const [list, setList] = useState(null);
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");

  const load = () => api.get("/student/assignments").then((r) => setList(r.data)).catch((e) => setError(errMsg(e)));
  useEffect(() => { load(); }, []);

  if (!list) return <p className="muted">{error || "Loading assignments…"}</p>;

  const todo = list.filter((a) => a.status === "NOT_SUBMITTED" && !a.overdue);
  const rest = list.filter((a) => !(a.status === "NOT_SUBMITTED" && !a.overdue));

  return (
    <>
      <h1 className="page-title">Assignments</h1>
      {error && <div className="alert error" role="alert">{error}</div>}
      {ok && <div className="alert success" role="status">{ok}</div>}

      {list.length === 0 && (
        <section className="panel"><p className="muted">No assignments yet. They appear here when your teachers post them for your registered courses.</p></section>
      )}

      {todo.length > 0 && <h2 style={{ margin: "0 0 0.75rem" }}>To do</h2>}
      {todo.map((a) => <Card key={a.id} a={a} reload={load} setError={setError} setOk={setOk} />)}

      {rest.length > 0 && <h2 style={{ margin: "1.5rem 0 0.75rem" }}>Handed in, marked or overdue</h2>}
      {rest.map((a) => <Card key={a.id} a={a} reload={load} setError={setError} setOk={setOk} />)}
    </>
  );
}

function Card({ a, reload, setError, setOk }) {
  const [open, setOpen] = useState(a.status === "NOT_SUBMITTED");
  const [text, setText] = useState(a.text || "");
  const [link, setLink] = useState(a.link || "");
  const [busy, setBusy] = useState(false);
  const [text0, cls] = a.overdue ? ["Overdue", "bad"] : STATUS[a.status];

  const submit = async (e) => {
    e.preventDefault();
    setError(""); setOk(""); setBusy(true);
    try {
      await api.post(`/student/assignments/${a.id}/submit`, { text, link });
      setOk(`Handed in: ${a.title}`);
      reload();
    } catch (err) { setError(errMsg(err)); }
    finally { setBusy(false); }
  };

  return (
    <section className="panel">
      <div className="panel-head">
        <div>
          <strong>{a.title}</strong>
          <div className="small muted">{a.courseCode} · {a.courseName} · due {when(a.dueAt)} · {a.maxMarks} marks</div>
        </div>
        <span className={`pill ${cls}`}>{text0}</span>
      </div>
      {a.description && <p style={{ whiteSpace: "pre-wrap" }}>{a.description}</p>}

      {a.status === "GRADED" && (
        <div className="alert success">
          You scored <strong>{a.marks} / {a.maxMarks}</strong>.
          {a.feedback && <div style={{ marginTop: 4 }}>Feedback: {a.feedback}</div>}
        </div>
      )}

      {a.status !== "GRADED" && (
        <>
          <button className="btn ghost" onClick={() => setOpen(!open)}>
            {open ? "Hide" : a.status === "NOT_SUBMITTED" ? "Hand in" : "Change my work"}
          </button>
          {open && (
            <form onSubmit={submit} className="stack" style={{ marginTop: "0.75rem" }}>
              <label>Your answer
                <textarea rows="5" value={text} onChange={(e) => setText(e.target.value)} />
              </label>
              <label>Link to your work (optional, for example a shared folder)
                <input type="url" value={link} onChange={(e) => setLink(e.target.value)} placeholder="https://" />
              </label>
              <div>
                <button className="btn primary" disabled={busy}>{busy ? "Sending…" : a.status === "NOT_SUBMITTED" ? "Hand in" : "Hand in again"}</button>
                {a.overdue && <span className="small muted" style={{ marginLeft: 12 }}>The deadline has passed. It will be marked as late.</span>}
              </div>
            </form>
          )}
        </>
      )}
    </section>
  );
}
