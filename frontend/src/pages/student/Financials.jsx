import { useCallback, useEffect, useState } from "react";
import { QRCodeSVG } from "qrcode.react";
import api, { errMsg, methodLabel, money } from "../../api.js";

const LIVE = ["PENDING", "SCANNED"];

export default function Financials() {
  const [data, setData] = useState(null);
  const [session, setSession] = useState(null);
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");
  const [busy, setBusy] = useState(false);

  const loadFinancials = useCallback(
    () => api.get("/student/financials").then((r) => setData(r.data)).catch((e) => setError(errMsg(e))),
    []
  );

  useEffect(() => {
    loadFinancials();
    // Keep the QR on screen after a page refresh
    api.get("/student/payment-sessions/active")
      .then((r) => { if (r.status === 200 && r.data) setSession(r.data); })
      .catch(() => {});
  }, [loadFinancials]);

  // While a QR is live, ask the server every 2 seconds whether the payment was completed.
  const token = session?.token;
  const status = session?.status;
  useEffect(() => {
    if (!token || !LIVE.includes(status)) return;
    const timer = setInterval(async () => {
      try {
        const r = await api.get(`/student/payment-sessions/${token}`);
        setSession(r.data);
        if (r.data.status === "COMPLETED") {
          setOk("Payment received. Thank you!");
          loadFinancials();
        }
      } catch {
        /* ignore a failed poll, the next one will retry */
      }
    }, 2000);
    return () => clearInterval(timer);
  }, [token, status, loadFinancials]);

  const generate = async () => {
    setError(""); setOk(""); setBusy(true);
    try {
      const r = await api.post("/student/payment-sessions");
      setSession(r.data);
    } catch (err) {
      setError(errMsg(err));
    } finally {
      setBusy(false);
    }
  };

  if (!data) return <p className="muted">{error || "Loading your fees…"}</p>;

  const pct = data.totalFee > 0 ? Math.min(100, (data.paidFee / data.totalFee) * 100) : 0;
  const cleared = Number(data.remainingBalance) <= 0;
  const live = session && LIVE.includes(session.status);
  // The QR opens our payer page, which sends the phone to eSewa. Open this site on your LAN address
  // (not "localhost") so the phone can reach it.
  const payUrl = session ? `${window.location.origin}/pay/esewa/${session.token}` : "";

  return (
    <>
      <h1 className="page-title">Fees &amp; payments</h1>
      {error && <div className="alert error" role="alert">{error}</div>}
      {ok && <div className="alert success" role="status">{ok}</div>}

      <section className="panel">
        <div className="balance">
          <div>
            <span className="muted">Remaining balance</span>
            <div className="big">{money(data.remainingBalance)}</div>
          </div>
          <div className="split">
            <div><span className="muted">Total fee</span><strong>{money(data.totalFee)}</strong></div>
            <div><span className="muted">Paid</span><strong>{money(data.paidFee)}</strong></div>
          </div>
        </div>
        <div className="bar" role="progressbar" aria-valuenow={Math.round(pct)} aria-valuemin={0} aria-valuemax={100}>
          <div style={{ width: `${pct}%` }} />
        </div>
      </section>

      {!cleared && (
        <section className="panel two-col">
          <div>
            <h2>Pay by QR</h2>
            <p className="muted">
              Fees can only be paid by scanning a QR code. The amount is set by the university and cannot be changed.
              If you paid in cash, the accounts office will update your balance.
            </p>
            {!live ? (
              <>
                {session?.status === "EXPIRED" && <p className="muted">Your last QR code expired.</p>}
                <button className="btn primary" onClick={generate} disabled={busy}>
                  {busy ? "Creating…" : `Generate QR for ${money(data.remainingBalance)}`}
                </button>
              </>
            ) : (
              <>
                <p><strong>Amount to pay: {money(session.amount)}</strong></p>
                <p className="muted small">
                  Scan the code with your phone camera, then log in to eSewa to pay. Valid until{" "}
                  {new Date(session.expiresAt).toLocaleTimeString()}.{" "}
                  <button className="link-btn" onClick={generate} disabled={busy}>Generate a new QR</button>
                </p>
              </>
            )}
          </div>

          {live && payUrl && (
            <div className="qr-box">
              <div className="qr-wrap">
                <div className="qr-code">
                  <QRCodeSVG value={payUrl} size={170} />
                </div>
              </div>
              <span className="small muted" role="status">Waiting for payment…</span>
            </div>
          )}
        </section>
      )}

      <section className="panel">
        <h2>Payment history</h2>
        {data.paymentHistory.length === 0 ? (
          <p className="muted">No payments yet. Your first payment will show up here.</p>
        ) : (
          <table>
            <thead><tr><th>Date</th><th>Reference</th><th>Method</th><th className="num">Amount</th></tr></thead>
            <tbody>
              {[...data.paymentHistory].reverse().map((t) => (
                <tr key={t.transactionId}>
                  <td>{new Date(t.timestamp).toLocaleString()}</td>
                  <td className="mono">{t.transactionId}</td>
                  <td>{methodLabel(t.method)}</td>
                  <td className="num">{money(t.amount)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </>
  );
}