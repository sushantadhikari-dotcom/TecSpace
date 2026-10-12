import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import api, { errMsg, money } from "../api.js";
import Logo from "../components/Logo.jsx";

/** Page opened when the QR is scanned. Stands in for the payer's banking app in this demo. */
export default function PayScan() {
  const { token } = useParams();
  const [info, setInfo] = useState(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  // Opening this page = the QR was scanned. Safe to call twice (the server ignores repeats).
  useEffect(() => {
    api.post(`/pay/${token}/scan`)
      .then((r) => setInfo(r.data))
      .catch((e) => setError(errMsg(e)));
  }, [token]);

  const confirm = async () => {
    setError(""); setBusy(true);
    try {
      const r = await api.post(`/pay/${token}/confirm`);
      setInfo(r.data);
    } catch (err) {
      setError(errMsg(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="pay-page">
      <Logo size={30} />
      <div className="panel pay-card">
        {error && <div className="alert error" role="alert">{error}</div>}
        {!info && !error && <p className="muted">Opening payment…</p>}

        {info?.status === "SCANNED" && (
          <>
            <h1>Confirm payment</h1>
            <p className="muted">Tuition fees for {info.studentName}</p>
            <div className="big">{money(info.amount)}</div>
            <button className="btn primary" onClick={confirm} disabled={busy}>
              {busy ? "Processing…" : "Pay now"}
            </button>
            <p className="muted small">Demo only. No real money moves.</p>
          </>
        )}
        {info?.status === "COMPLETED" && (
          <>
            <h1>Payment completed</h1>
            <p>{money(info.amount)} was paid for {info.studentName}. You can close this page.</p>
          </>
        )}
        {info && ["EXPIRED", "CANCELLED"].includes(info.status) && (
          <>
            <h1>QR code no longer valid</h1>
            <p className="muted">Ask the student to generate a new QR code.</p>
          </>
        )}
      </div>
    </div>
  );
}
