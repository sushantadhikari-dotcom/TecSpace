import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import api, { errMsg, money, methodLabel } from "../../api.js";
import Logo from "../../components/Logo.jsx";

export default function Receipt() {
  const { id } = useParams();
  const [r, setR] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    api.get(`/student/receipts/${id}`).then((x) => setR(x.data)).catch((e) => setError(errMsg(e)));
  }, [id]);

  if (!r) return <p className="muted">{error || "Loading receipt…"}</p>;

  return (
    <>
      <div className="no-print actions" style={{ marginBottom: "1rem" }}>
        <Link to="/student/invoices">Back to invoices</Link>
        <button className="btn primary" onClick={() => window.print()}>Print / Save as PDF</button>
      </div>

      <section className="panel paper">
        <div className="paper-head">
          <Logo size={26} />
          <div className="right">
            <h1>Payment receipt</h1>
            <span className="mono">{r.receiptNumber}</span>
          </div>
        </div>

        <table className="kv">
          <tbody>
            <tr><th>Received from</th><td>{r.studentName}</td></tr>
            <tr><th>Email</th><td>{r.email}</td></tr>
            <tr><th>Faculty / session</th><td>{r.faculty} · {r.session}</td></tr>
            <tr><th>Date</th><td>{new Date(r.date).toLocaleString()}</td></tr>
            <tr><th>Method</th><td>{methodLabel(r.method)}</td></tr>
            <tr><th>Transaction reference</th><td className="mono">{r.transactionId}</td></tr>
          </tbody>
        </table>

        <div className="paper-total">
          <span>Amount received</span>
          <strong>{money(r.amount)}</strong>
        </div>
        <p className="muted small">Balance remaining after this payment: {money(r.balanceNow)}</p>
        <p className="muted small">This is a computer-generated receipt issued by TecSpace.</p>
      </section>
    </>
  );
}
