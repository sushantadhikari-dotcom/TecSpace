import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api, { errMsg, money, methodLabel } from "../../api.js";
import { downloadCsv } from "../../csv.js";

const PLAN_STATUS = {
  PAID: ["Paid", "ok"], OVERDUE: ["Overdue", "bad"], PARTIAL: ["Part paid", "warn"], UPCOMING: ["Upcoming", ""],
};

export default function Invoices() {
  const [d, setD] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    Promise.all([
      api.get("/student/invoices"), api.get("/student/scholarships"), api.get("/student/payment-plan"),
      api.get("/student/ledger"), api.get("/student/financials"),
    ]).then(([inv, sch, plan, led, fin]) =>
      setD({ invoices: inv.data, scholarships: sch.data, plan: plan.data, ledger: led.data, fin: fin.data })
    ).catch((e) => setError(errMsg(e)));
  }, []);

  if (!d) return <p className="muted">{error || "Loading your fee details…"}</p>;

  const issued = d.invoices.filter((i) => i.status === "ISSUED");
  const activeScholarships = d.scholarships.filter((s) => s.status === "ACTIVE");

  const exportLedger = () =>
    downloadCsv("tecspace-ledger.csv", [
      ["Date", "Type", "Description", "Debit", "Credit", "Balance"],
      ...d.ledger.entries.map((e) => [new Date(e.date).toLocaleDateString(), e.type, e.description, e.debit, e.credit, e.balance]),
    ]);

  return (
    <>
      <h1 className="page-title">Invoices &amp; ledger</h1>

      <div className="stats">
        <div className="stat"><span className="muted">Total fees</span><strong>{money(d.ledger.totalFee)}</strong></div>
        <div className="stat"><span className="muted">Paid</span><strong>{money(d.ledger.paidFee)}</strong></div>
        <Link to="/student/financials" className="stat">
          <span className="muted">Remaining</span>
          <strong className={Number(d.ledger.remainingBalance) > 0 ? "warn" : ""}>{money(d.ledger.remainingBalance)}</strong>
          <span className="small">{Number(d.ledger.remainingBalance) > 0 ? "Pay now" : "All paid"}</span>
        </Link>
      </div>

      <section className="panel">
        <h2>Fee breakdown</h2>
        {issued.length === 0 ? (
          <p className="muted">No invoices yet. Your fee breakdown will appear here once the accounts office issues it.</p>
        ) : issued.map((inv) => (
          <div className="invoice" key={inv.id}>
            <div className="panel-head">
              <div>
                <strong>{inv.invoiceNumber}</strong> <span className="muted">· {inv.structureName}</span>
                <div className="small muted">Due {inv.dueDate} · {inv.term}</div>
              </div>
              <strong>{money(inv.subtotal)}</strong>
            </div>
            <table>
              <tbody>
                {inv.items.map((it, i) => (
                  <tr key={i}><td>{it.label}</td><td className="small muted">{it.category}</td><td className="num">{money(it.amount)}</td></tr>
                ))}
              </tbody>
            </table>
          </div>
        ))}
        {activeScholarships.length > 0 && (
          <>
            <h2 style={{ marginTop: "1rem" }}>Scholarships &amp; discounts</h2>
            <table>
              <tbody>
                {activeScholarships.map((s) => (
                  <tr key={s.id}>
                    <td>{s.name}<div className="small muted">{s.type === "PERCENT" ? `${s.value}% of fees` : "Fixed amount"} · {s.term}</div></td>
                    <td className="num">− {money(s.amount)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </>
        )}
      </section>

      <section className="panel">
        <h2>Payment plan</h2>
        {!d.plan.exists ? (
          <p className="muted">You are not on an installment plan. If you need one, ask the accounts office through your Inbox.</p>
        ) : (
          <>
            <p className="muted">{money(d.plan.totalPaid)} paid of {money(d.plan.totalAmount)} planned.</p>
            <table>
              <thead><tr><th>Due date</th><th className="num">Amount</th><th className="num">Paid</th><th>Status</th></tr></thead>
              <tbody>
                {d.plan.installments.map((i, n) => {
                  const [text, cls] = PLAN_STATUS[i.status] || PLAN_STATUS.UPCOMING;
                  return (
                    <tr key={n}>
                      <td>{i.dueDate}</td><td className="num">{money(i.amount)}</td><td className="num">{money(i.paid)}</td>
                      <td><span className={`pill ${cls}`}>{text}</span></td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </>
        )}
      </section>

      <section className="panel">
        <h2>Payments &amp; receipts</h2>
        {d.fin.paymentHistory.length === 0 ? (
          <p className="muted">No payments yet.</p>
        ) : (
          <table>
            <thead><tr><th>Date</th><th>Reference</th><th>Method</th><th className="num">Amount</th><th></th></tr></thead>
            <tbody>
              {[...d.fin.paymentHistory].reverse().map((t) => (
                <tr key={t.transactionId}>
                  <td>{new Date(t.timestamp).toLocaleDateString()}</td>
                  <td className="mono">{t.transactionId}</td>
                  <td>{methodLabel(t.method)}</td>
                  <td className="num">{money(t.amount)}</td>
                  <td className="num">{Number(t.amount) > 0 && <Link to={`/student/receipt/${t.transactionId}`}>Receipt</Link>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      <section className="panel">
        <div className="panel-head">
          <h2>Ledger</h2>
          <button className="btn ghost" onClick={exportLedger}>Download CSV</button>
        </div>
        {d.ledger.entries.length === 0 ? (
          <p className="muted">Nothing in your ledger yet.</p>
        ) : (
          <table>
            <thead><tr><th>Date</th><th>Description</th><th className="num">Charged</th><th className="num">Paid / credited</th><th className="num">Balance</th></tr></thead>
            <tbody>
              {d.ledger.entries.map((e, i) => (
                <tr key={i}>
                  <td>{new Date(e.date).toLocaleDateString()}</td>
                  <td>{e.description}</td>
                  <td className="num">{Number(e.debit) ? money(e.debit) : ""}</td>
                  <td className="num">{Number(e.credit) ? money(e.credit) : ""}</td>
                  <td className="num"><strong>{money(e.balance)}</strong></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </>
  );
}
