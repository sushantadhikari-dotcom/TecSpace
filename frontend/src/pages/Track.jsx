import { useState } from "react";
import { Link } from "react-router-dom";
import api, { errMsg } from "../api.js";
import Logo from "../components/Logo.jsx";

const STATUS = {
  SUBMITTED: ["Under review", ""], APPROVED: ["Approved", "ok"], WAITLISTED: ["On the waiting list", "warn"],
  REJECTED: ["Not accepted", "bad"], ENROLLED: ["Enrolled", "ok"],
};
const DOC = { PENDING: ["Waiting", ""], VERIFIED: ["Verified", "ok"], REJECTED: ["Problem", "bad"] };

export default function Track() {
  const [code, setCode] = useState("");
  const [email, setEmail] = useState("");
  const [data, setData] = useState(null);
  const [error, setError] = useState("");

  const submit = async (e) => {
    e.preventDefault();
    setError(""); setData(null);
    try {
      const r = await api.get(`/public/applications/${encodeURIComponent(code.trim())}`, { params: { email } });
      setData(r.data);
    } catch (err) { setError(errMsg(err)); }
  };

  const [text, cls] = data ? STATUS[data.status] || ["", ""] : ["", ""];

  return (
    <div className="pay-page">
      <Logo size={30} />
      <div className="panel pay-card stack" style={{ maxWidth: 560 }}>
        <h1>Track your application</h1>
        <form onSubmit={submit} className="stack">
          <label>Tracking code<input value={code} onChange={(e) => setCode(e.target.value)} placeholder="APP-XXXXXX" required /></label>
          <label>Email you applied with<input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required /></label>
          <button className="btn primary">Check status</button>
        </form>
        {error && <div className="alert error" role="alert">{error}</div>}

        {data && (
          <>
            <h2>{data.name}</h2>
            <p>{data.faculty} · {data.session} · <span className={`pill ${cls}`}>{text}</span></p>
            {data.remarks && <p className="muted">{data.remarks}</p>}
            <table>
              <tbody>
                {data.documents.map((d) => (
                  <tr key={d.name}>
                    <td>{d.name}{d.note && <div className="small muted">{d.note}</div>}</td>
                    <td className="num"><span className={`pill ${(DOC[d.status] || DOC.PENDING)[1]}`}>{(DOC[d.status] || DOC.PENDING)[0]}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </>
        )}
        <p className="muted small"><Link to="/apply">New application</Link> · <Link to="/login">Log in</Link></p>
      </div>
    </div>
  );
}
