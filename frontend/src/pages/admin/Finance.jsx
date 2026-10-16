import { useEffect, useState } from "react";
import api, { errMsg, money } from "../../api.js";
import { downloadCsv } from "../../csv.js";

const TABS = [
  ["collections", "Collections"],
  ["structures", "Fee structures"],
  ["invoices", "Invoices"],
  ["aid", "Scholarships & plans"],
  ["ledger", "Ledger"],
];
const todayStr = () => {
  const d = new Date();
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
};

export default function Finance() {
  const [tab, setTab] = useState("collections");
  const [students, setStudents] = useState([]);
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");
  const [ledgerFor, setLedgerFor] = useState("");

  useEffect(() => {
    api.get("/admin/students").then((r) => setStudents(r.data)).catch((e) => setError(errMsg(e)));
  }, []);

  const common = { students, setError, setOk };
  const openLedger = (id) => { setLedgerFor(id); setTab("ledger"); };

  return (
    <>
      <h1 className="page-title">Finance</h1>
      {error && <div className="alert error" role="alert">{error}</div>}
      {ok && <div className="alert success" role="status">{ok}</div>}

      <div className="tabs" role="tablist">
        {TABS.map(([key, label]) => (
          <button key={key} role="tab" aria-selected={tab === key} className={tab === key ? "active" : ""}
            onClick={() => { setTab(key); setError(""); setOk(""); }}>{label}</button>
        ))}
      </div>

      {tab === "collections" && <Collections {...common} onLedger={openLedger} />}
      {tab === "structures" && <Structures {...common} />}
      {tab === "invoices" && <InvoicesTab {...common} />}
      {tab === "aid" && <Aid {...common} />}
      {tab === "ledger" && <Ledger {...common} initial={ledgerFor} />}
    </>
  );
}

/* ------------------------------------------------------------ Collections */
function Collections({ setError, onLedger }) {
  const [s, setS] = useState(null);
  useEffect(() => {
    api.get("/admin/finance/summary").then((r) => setS(r.data)).catch((e) => setError(errMsg(e)));
  }, [setError]);

  if (!s) return <p className="muted">Loading…</p>;
  const pct = Number(s.totalBilled) > 0 ? Math.round((Number(s.totalCollected) / Number(s.totalBilled)) * 100) : 0;

  return (
    <>
      <div className="stats">
        <div className="stat"><span className="muted">Billed</span><strong>{money(s.totalBilled)}</strong></div>
        <div className="stat"><span className="muted">Collected</span><strong>{money(s.totalCollected)}</strong><span className="small">{pct}% of billed</span></div>
        <div className="stat"><span className="muted">Outstanding</span><strong className="warn">{money(s.totalOutstanding)}</strong></div>
        <div className="stat"><span className="muted">Fully paid</span><strong>{s.fullyPaidCount} / {s.studentCount}</strong><span className="small">students</span></div>
      </div>
      <section className="panel">
        <div className="panel-head">
          <h2>Who owes what</h2>
          <button className="btn ghost" onClick={() => downloadCsv("collections.csv", [
            ["Student", "Faculty", "Total fee", "Paid", "Remaining"],
            ...s.rows.map((r) => [r.name, r.faculty, r.totalFee, r.paidFee, r.remaining]),
          ])}>Download CSV</button>
        </div>
        {s.rows.length === 0 ? <p className="muted">No students yet.</p> : (
          <table>
            <thead><tr><th>Student</th><th>Faculty</th><th className="num">Total</th><th className="num">Paid</th><th className="num">Remaining</th><th></th></tr></thead>
            <tbody>
              {s.rows.map((r) => (
                <tr key={r.studentId}>
                  <td>{r.name}</td><td>{r.faculty}</td>
                  <td className="num">{money(r.totalFee)}</td><td className="num">{money(r.paidFee)}</td>
                  <td className="num"><strong>{money(r.remaining)}</strong></td>
                  <td className="num"><button className="btn ghost" onClick={() => onLedger(r.studentId)}>Ledger</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </>
  );
}

/* ------------------------------------------------------------ Fee structures */
const blankItem = () => ({ category: "TUITION", label: "", amount: "", optional: false });

function Structures({ setError, setOk }) {
  const [list, setList] = useState(null);
  const [form, setForm] = useState({ name: "", faculty: "ALL", items: [{ ...blankItem(), label: "Tuition" }] });

  const load = () => api.get("/admin/fee-structures").then((r) => setList(r.data)).catch((e) => setError(errMsg(e)));
  useEffect(() => { load(); }, []);

  const setItem = (i, patch) =>
    setForm({ ...form, items: form.items.map((it, n) => (n === i ? { ...it, ...patch } : it)) });

  const submit = async (e) => {
    e.preventDefault();
    setError(""); setOk("");
    try {
      await api.post("/admin/fee-structures", {
        ...form, items: form.items.map((it) => ({ ...it, amount: Number(it.amount) })),
      });
      setOk("Fee structure saved.");
      setForm({ name: "", faculty: "ALL", items: [{ ...blankItem(), label: "Tuition" }] });
      load();
    } catch (err) { setError(errMsg(err)); }
  };

  const remove = async (s) => {
    if (!window.confirm(`Delete "${s.name}"?`)) return;
    setError(""); setOk("");
    try { await api.delete(`/admin/fee-structures/${s.id}`); load(); } catch (err) { setError(errMsg(err)); }
  };

  return (
    <>
      <section className="panel">
        <h2>New fee structure</h2>
        <form onSubmit={submit} className="stack">
          <div className="row">
            <label>Name<input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="BSIT Fall 2026" required /></label>
            <label>For faculty
              <select value={form.faculty} onChange={(e) => setForm({ ...form, faculty: e.target.value })}>
                <option value="ALL">All faculties</option><option>BSIT</option><option>BBA</option><option>MSCIT</option>
              </select>
            </label>
          </div>

          {form.items.map((it, i) => (
            <div className="fee-row" key={i}>
              <label>Type
                <select value={it.category} onChange={(e) => setItem(i, { category: e.target.value })}>
                  <option>TUITION</option><option>LAB</option><option>HOSTEL</option><option>OTHER</option>
                </select>
              </label>
              <label>Label<input value={it.label} onChange={(e) => setItem(i, { label: e.target.value })} required /></label>
              <label>Amount<input type="number" min="0.01" step="0.01" value={it.amount} onChange={(e) => setItem(i, { amount: e.target.value })} required /></label>
              <label className="check"><input type="checkbox" checked={it.optional} onChange={(e) => setItem(i, { optional: e.target.checked })} />Optional</label>
              <button type="button" className="btn ghost danger" disabled={form.items.length === 1}
                onClick={() => setForm({ ...form, items: form.items.filter((_, n) => n !== i) })}>Remove</button>
            </div>
          ))}
          <div className="actions">
            <button type="button" className="btn ghost" onClick={() => setForm({ ...form, items: [...form.items, blankItem()] })}>Add fee item</button>
            <button className="btn primary">Save structure</button>
          </div>
          <p className="muted small">Optional items (for example hostel) are only billed when you choose them while generating an invoice.</p>
        </form>
      </section>

      <section className="panel">
        <h2>Fee structures</h2>
        {!list ? <p className="muted">Loading…</p> : list.length === 0 ? <p className="muted">None yet.</p> : list.map((s) => (
          <div className="invoice" key={s.id}>
            <div className="panel-head">
              <div><strong>{s.name}</strong> <span className="pill">{s.faculty}</span> <span className="small muted">{s.term}</span></div>
              <button className="btn ghost danger" onClick={() => remove(s)}>Delete</button>
            </div>
            <table><tbody>
              {s.items.map((it, i) => (
                <tr key={i}><td>{it.label}{it.optional && <span className="pill" style={{ marginLeft: 8 }}>Optional</span>}</td>
                  <td className="small muted">{it.category}</td><td className="num">{money(it.amount)}</td></tr>
              ))}
            </tbody></table>
          </div>
        ))}
      </section>
    </>
  );
}

/* ------------------------------------------------------------ Invoices */
function InvoicesTab({ students, setError, setOk }) {
  const [structures, setStructures] = useState([]);
  const [invoices, setInvoices] = useState(null);
  const [form, setForm] = useState({ structureId: "", studentId: "", dueDate: todayStr(), includeOptional: [] });
  const nameOf = (id) => { const s = students.find((x) => x.id === id); return s ? `${s.firstName} ${s.lastName}` : id; };

  const load = () => api.get("/admin/invoices").then((r) => setInvoices(r.data)).catch((e) => setError(errMsg(e)));
  useEffect(() => {
    load();
    api.get("/admin/fee-structures").then((r) => {
      setStructures(r.data);
      if (r.data[0]) setForm((f) => ({ ...f, structureId: r.data[0].id }));
    }).catch((e) => setError(errMsg(e)));
  }, []);

  const current = structures.find((s) => s.id === form.structureId);
  const optionalItems = current ? current.items.filter((i) => i.optional) : [];

  const toggleOptional = (label) =>
    setForm({ ...form, includeOptional: form.includeOptional.includes(label)
      ? form.includeOptional.filter((l) => l !== label) : [...form.includeOptional, label] });

  const generate = async (e) => {
    e.preventDefault();
    setError(""); setOk("");
    try {
      const r = await api.post("/admin/invoices/generate", form);
      setOk(`${r.data.created} invoice(s) issued, ${r.data.skipped} skipped (already invoiced).`);
      load();
    } catch (err) { setError(errMsg(err)); }
  };

  const cancel = async (inv) => {
    if (!window.confirm(`Cancel ${inv.invoiceNumber}? The amount is taken off the student's fees.`)) return;
    setError(""); setOk("");
    try { await api.post(`/admin/invoices/${inv.id}/cancel`); setOk("Invoice cancelled."); load(); }
    catch (err) { setError(errMsg(err)); }
  };

  return (
    <>
      <section className="panel">
        <h2>Generate invoices</h2>
        {structures.length === 0 ? <p className="muted">Create a fee structure first.</p> : (
          <form onSubmit={generate} className="stack">
            <div className="row">
              <label>Fee structure
                <select value={form.structureId} onChange={(e) => setForm({ ...form, structureId: e.target.value, includeOptional: [] })}>
                  {structures.map((s) => <option key={s.id} value={s.id}>{s.name} ({s.faculty})</option>)}
                </select>
              </label>
              <label>Bill
                <select value={form.studentId} onChange={(e) => setForm({ ...form, studentId: e.target.value })}>
                  <option value="">Every student of this faculty</option>
                  {students.map((s) => <option key={s.id} value={s.id}>{s.firstName} {s.lastName} · {s.faculty}</option>)}
                </select>
              </label>
            </div>
            <div className="row">
              <label>Due date<input type="date" value={form.dueDate} onChange={(e) => setForm({ ...form, dueDate: e.target.value })} required /></label>
              <div>
                {optionalItems.length > 0 && <span className="small muted">Also bill these optional items:</span>}
                {optionalItems.map((i) => (
                  <label className="check" key={i.label}>
                    <input type="checkbox" checked={form.includeOptional.includes(i.label)} onChange={() => toggleOptional(i.label)} />
                    {i.label} ({money(i.amount)})
                  </label>
                ))}
              </div>
            </div>
            <div><button className="btn primary">Issue invoices</button></div>
          </form>
        )}
      </section>

      <section className="panel">
        <h2>All invoices</h2>
        {!invoices ? <p className="muted">Loading…</p> : invoices.length === 0 ? <p className="muted">No invoices yet.</p> : (
          <table>
            <thead><tr><th>Number</th><th>Student</th><th>Structure</th><th>Due</th><th className="num">Amount</th><th>Status</th><th></th></tr></thead>
            <tbody>
              {invoices.map((i) => (
                <tr key={i.id}>
                  <td className="mono">{i.invoiceNumber}</td><td>{nameOf(i.studentId)}</td><td>{i.structureName}</td>
                  <td>{i.dueDate}</td><td className="num">{money(i.subtotal)}</td>
                  <td><span className={`pill ${i.status === "ISSUED" ? "ok" : "bad"}`}>{i.status === "ISSUED" ? "Issued" : "Cancelled"}</span></td>
                  <td className="num">{i.status === "ISSUED" && <button className="btn ghost danger" onClick={() => cancel(i)}>Cancel</button>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </>
  );
}

/* ------------------------------------------------------------ Scholarships & plans */
function Aid({ students, setError, setOk }) {
  const [list, setList] = useState(null);
  const [sch, setSch] = useState({ studentId: "", name: "", type: "PERCENT", value: "" });
  const [plan, setPlan] = useState({ studentId: "", count: 3, firstDueDate: todayStr(), intervalDays: 30 });
  const [view, setView] = useState(null);
  const nameOf = (id) => { const s = students.find((x) => x.id === id); return s ? `${s.firstName} ${s.lastName}` : id; };

  const load = () => api.get("/admin/scholarships").then((r) => setList(r.data)).catch((e) => setError(errMsg(e)));
  useEffect(() => { load(); }, []);
  useEffect(() => {
    if (students[0]) {
      setSch((s) => (s.studentId ? s : { ...s, studentId: students[0].id }));
      setPlan((p) => (p.studentId ? p : { ...p, studentId: students[0].id }));
    }
  }, [students]);
  useEffect(() => {
    if (!plan.studentId) return;
    api.get(`/admin/payment-plans/${plan.studentId}`).then((r) => setView(r.data)).catch(() => setView(null));
  }, [plan.studentId]);

  const award = async (e) => {
    e.preventDefault();
    setError(""); setOk("");
    try {
      await api.post("/admin/scholarships", { ...sch, value: Number(sch.value) });
      setOk("Scholarship awarded and taken off the student's fees.");
      setSch({ ...sch, name: "", value: "" });
      load();
    } catch (err) { setError(errMsg(err)); }
  };

  const revoke = async (s) => {
    if (!window.confirm(`Revoke "${s.name}"? The amount is added back to the student's fees.`)) return;
    setError(""); setOk("");
    try { await api.delete(`/admin/scholarships/${s.id}`); load(); } catch (err) { setError(errMsg(err)); }
  };

  const makePlan = async (e) => {
    e.preventDefault();
    setError(""); setOk("");
    try {
      const r = await api.post("/admin/payment-plans", { ...plan, count: Number(plan.count), intervalDays: Number(plan.intervalDays) });
      setView(r.data);
      setOk("Payment plan created. The student was notified.");
    } catch (err) { setError(errMsg(err)); }
  };

  const removePlan = async () => {
    setError(""); setOk("");
    try { await api.delete(`/admin/payment-plans/${plan.studentId}`); setView({ exists: false, installments: [] }); }
    catch (err) { setError(errMsg(err)); }
  };

  const studentSelect = (value, onChange) => (
    <select value={value} onChange={onChange}>
      {students.map((s) => <option key={s.id} value={s.id}>{s.firstName} {s.lastName} · {s.universityEmail}</option>)}
    </select>
  );

  return (
    <>
      <section className="panel">
        <h2>Award a scholarship or discount</h2>
        <form onSubmit={award} className="inline-form">
          <label>Student{studentSelect(sch.studentId, (e) => setSch({ ...sch, studentId: e.target.value }))}</label>
          <label>Name<input value={sch.name} onChange={(e) => setSch({ ...sch, name: e.target.value })} placeholder="Merit scholarship" required /></label>
          <label>Type
            <select value={sch.type} onChange={(e) => setSch({ ...sch, type: e.target.value })}>
              <option value="PERCENT">Percent of fees</option><option value="FIXED">Fixed amount</option>
            </select>
          </label>
          <label>{sch.type === "PERCENT" ? "Percent" : "Amount"}
            <input type="number" min="0.01" step="0.01" style={{ width: 120 }} value={sch.value} onChange={(e) => setSch({ ...sch, value: e.target.value })} required />
          </label>
          <button className="btn primary">Award</button>
        </form>
        <p className="muted small">The student must already have an invoice for this term. Percentages are worked out on that invoice.</p>

        {!list ? null : list.length === 0 ? <p className="muted">No scholarships awarded yet.</p> : (
          <table>
            <thead><tr><th>Student</th><th>Scholarship</th><th className="num">Amount off</th><th>Status</th><th></th></tr></thead>
            <tbody>
              {list.map((s) => (
                <tr key={s.id}>
                  <td>{nameOf(s.studentId)}</td>
                  <td>{s.name}<div className="small muted">{s.type === "PERCENT" ? `${s.value}%` : "Fixed"} · {s.term}</div></td>
                  <td className="num">{money(s.amount)}</td>
                  <td><span className={`pill ${s.status === "ACTIVE" ? "ok" : "bad"}`}>{s.status === "ACTIVE" ? "Active" : "Revoked"}</span></td>
                  <td className="num">{s.status === "ACTIVE" && <button className="btn ghost danger" onClick={() => revoke(s)}>Revoke</button>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      <section className="panel">
        <h2>Payment plan</h2>
        <form onSubmit={makePlan} className="inline-form">
          <label>Student{studentSelect(plan.studentId, (e) => setPlan({ ...plan, studentId: e.target.value }))}</label>
          <label>Installments<input type="number" min="2" max="12" style={{ width: 100 }} value={plan.count} onChange={(e) => setPlan({ ...plan, count: e.target.value })} /></label>
          <label>First due<input type="date" value={plan.firstDueDate} onChange={(e) => setPlan({ ...plan, firstDueDate: e.target.value })} required /></label>
          <label>Every (days)<input type="number" min="7" max="180" style={{ width: 100 }} value={plan.intervalDays} onChange={(e) => setPlan({ ...plan, intervalDays: e.target.value })} /></label>
          <button className="btn primary">{view?.exists ? "Replace plan" : "Create plan"}</button>
        </form>
        <p className="muted small">The student's current remaining balance is split into equal installments.</p>

        {view?.exists && (
          <>
            <table>
              <thead><tr><th>Due date</th><th className="num">Amount</th><th className="num">Paid</th><th>Status</th></tr></thead>
              <tbody>
                {view.installments.map((i, n) => (
                  <tr key={n}><td>{i.dueDate}</td><td className="num">{money(i.amount)}</td><td className="num">{money(i.paid)}</td>
                    <td><span className={`pill ${i.status === "PAID" ? "ok" : i.status === "OVERDUE" ? "bad" : i.status === "PARTIAL" ? "warn" : ""}`}>{i.status.toLowerCase()}</span></td></tr>
                ))}
              </tbody>
            </table>
            <div className="actions"><span /><button className="btn ghost danger" onClick={removePlan}>Remove plan</button></div>
          </>
        )}
      </section>
    </>
  );
}

/* ------------------------------------------------------------ Ledger */
function Ledger({ students, setError, initial }) {
  const [studentId, setStudentId] = useState(initial || "");
  const [data, setData] = useState(null);

  useEffect(() => {
    if (!studentId && students[0]) setStudentId(students[0].id);
  }, [students, studentId]);

  useEffect(() => {
    if (!studentId) return;
    setData(null);
    api.get(`/admin/ledger/${studentId}`).then((r) => setData(r.data)).catch((e) => setError(errMsg(e)));
  }, [studentId, setError]);

  return (
    <section className="panel">
      <div className="inline-form">
        <label>Student
          <select value={studentId} onChange={(e) => setStudentId(e.target.value)}>
            {students.map((s) => <option key={s.id} value={s.id}>{s.firstName} {s.lastName} · {s.universityEmail}</option>)}
          </select>
        </label>
        {data && (
          <button className="btn ghost" onClick={() => downloadCsv(`ledger-${data.studentName.replace(/\s+/g, "-")}.csv`, [
            ["Date", "Type", "Description", "Debit", "Credit", "Balance"],
            ...data.entries.map((e) => [new Date(e.date).toLocaleDateString(), e.type, e.description, e.debit, e.credit, e.balance]),
          ])}>Download CSV</button>
        )}
      </div>

      {!data ? <p className="muted">Loading…</p> : (
        <>
          <p className="muted">
            {data.studentName}: total {money(data.totalFee)}, paid {money(data.paidFee)}, remaining <strong>{money(data.remainingBalance)}</strong>
          </p>
          {data.entries.length === 0 ? <p className="muted">No entries yet.</p> : (
            <table>
              <thead><tr><th>Date</th><th>Description</th><th className="num">Charged</th><th className="num">Paid / credited</th><th className="num">Balance</th></tr></thead>
              <tbody>
                {data.entries.map((e, i) => (
                  <tr key={i}>
                    <td>{new Date(e.date).toLocaleDateString()}</td><td>{e.description}</td>
                    <td className="num">{Number(e.debit) ? money(e.debit) : ""}</td>
                    <td className="num">{Number(e.credit) ? money(e.credit) : ""}</td>
                    <td className="num"><strong>{money(e.balance)}</strong></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}
    </section>
  );
}
