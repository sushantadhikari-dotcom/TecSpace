import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import api, { errMsg } from "../api.js";
import Logo from "../components/Logo.jsx";

/** Opened when an invigilator scans the QR code on an admit card. No login needed. */
export default function VerifyAdmit() {
  const [params] = useSearchParams();
  const [v, setV] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    api.get("/public/admit-card/verify", { params: { student: params.get("student"), token: params.get("token") } })
      .then((r) => setV(r.data)).catch((e) => setError(errMsg(e)));
  }, []);

  return (
    <div className="pay-page">
      <Logo size={30} />
      <div className="panel pay-card stack" style={{ maxWidth: 560 }}>
        <h1>Admit card check</h1>
        {error && <div className="alert error" role="alert">{error}</div>}
        {!v && !error && <p className="muted">Checking…</p>}
        {v && !v.valid && <div className="alert error" role="alert"><strong>This admit card is not valid.</strong> Do not admit this person on the strength of this code.</div>}
        {v && v.valid && (
          <>
            <div className="alert success" role="status"><strong>Valid admit card</strong></div>
            <p><strong>{v.studentName}</strong><br /><span className="muted">{v.faculty} · {v.session}</span></p>
            {v.exams.length === 0 ? <p className="muted">No upcoming exams.</p> : (
              <table>
                <tbody>
                  {v.exams.map((e) => (
                    <tr key={e.examId}>
                      <td>{e.date}<div className="small muted">{e.startTime} – {e.endTime}</div></td>
                      <td><strong>{e.courseCode}</strong> · room {e.room}</td>
                      <td className="num">{e.eligible ? <span className="pill ok">Allowed</span> : <span className="pill bad">Not allowed</span>}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </>
        )}
      </div>
    </div>
  );
}
