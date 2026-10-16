import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import api, { errMsg, methodLabel, money } from "../../api.js";

const emptyCourse = () => ({
  courseCode: "",
  courseName: "",
  grade: "A",
  creditHours: 3,
  percentage: "",
});

const emptyTrimester = () => ({
  trimesterName: "",
  courses: [emptyCourse()],
});

export default function StudentDetail() {
  const { id } = useParams();

  const [fin, setFin] = useState(null);
  const [fees, setFees] = useState({ totalFee: "", paidFee: "" });
  const [cash, setCash] = useState("");
  const [grades, setGrades] = useState(null);
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");

  useEffect(() => {
    Promise.all([
      api.get(`/admin/financials/${id}`),
      api.get(`/admin/grades/${id}`),
    ])
      .then(([f, g]) => {
        setFin(f.data);
        setFees({
          totalFee: f.data.totalFee,
          paidFee: f.data.paidFee,
        });
        setGrades(g.data);
      })
      .catch((e) => setError(errMsg(e)));
  }, [id]);

  const flash = (msg) => {
    setOk(msg);
    setError("");
  };

  const saveFees = async (e) => {
    e.preventDefault();

    try {
      const r = await api.put(`/admin/financials/${id}`, {
        totalFee: Number(fees.totalFee),
        paidFee: Number(fees.paidFee),
      });

      setFin(r.data);
      flash("Fees saved.");
    } catch (err) {
      setError(errMsg(err));
      setOk("");
    }
  };

  const saveCash = async (e) => {
    e.preventDefault();

    try {
      const r = await api.post(`/admin/financials/${id}/cash`, {
        amount: Number(cash),
      });

      setFin(r.data);
      setFees({
        totalFee: r.data.totalFee,
        paidFee: r.data.paidFee,
      });
      setCash("");
      flash("Cash payment recorded.");
    } catch (err) {
      setError(errMsg(err));
      setOk("");
    }
  };

  const saveGrades = async () => {
    try {
      const trimesters = grades.trimesters.map((t) => ({
        ...t,
        courses: t.courses.map((c) => ({
          ...c,
          percentage:
            c.percentage === "" || c.percentage == null
              ? null
              : Number(c.percentage),
        })),
      }));

      const r = await api.put(`/admin/grades/${id}`, { trimesters });

      setGrades(r.data);
      flash(`Grades saved. CGPA is now ${r.data.cgpa.toFixed(2)}.`);
    } catch (err) {
      setError(errMsg(err));
      setOk("");
    }
  };

  const updTri = (ti, patch) =>
    setGrades({
      ...grades,
      trimesters: grades.trimesters.map((t, i) =>
        i === ti ? { ...t, ...patch } : t,
      ),
    });

  const updCourse = (ti, ci, patch) =>
    updTri(ti, {
      courses: grades.trimesters[ti].courses.map((c, i) =>
        i === ci ? { ...c, ...patch } : c,
      ),
    });

  if (!fin || !grades) {
    return <p className="muted">{error || "Loading student…"}</p>;
  }

  return (
    <>
      <Link to="/admin" className="back">
        Back to students
      </Link>

      <h1 className="page-title">Manage student</h1>

      {error && (
        <div className="alert error" role="alert">
          {error}
        </div>
      )}

      {ok && (
        <div className="alert success" role="status">
          {ok}
        </div>
      )}

      <section className="panel">
        <h2>Fees</h2>

        <p className="muted">
          Remaining balance: <strong>{money(fin.remainingBalance)}</strong>{" "}
          (paid {money(fin.paidFee)} of {money(fin.totalFee)})
        </p>

        <h3>Record a cash payment</h3>

        <p className="muted">
          Use this when the student pays cash at the office. Students cannot
          edit their balance themselves.
        </p>

        <form onSubmit={saveCash} className="inline-form">
          <label>
            Cash received
            <input
              type="number"
              step="0.01"
              min="0.01"
              max={fin.remainingBalance}
              value={cash}
              onChange={(e) => setCash(e.target.value)}
              required
            />
          </label>

          <button
            className="btn primary"
            disabled={Number(fin.remainingBalance) <= 0}
          >
            Record cash payment
          </button>
        </form>

        <h3>Correct fee totals</h3>

        <p className="muted">
          Only for fixing mistakes. For normal payments use the cash form above.
        </p>

        <form onSubmit={saveFees} className="inline-form">
          <label>
            Total fee
            <input
              type="number"
              step="0.01"
              min="0"
              value={fees.totalFee}
              onChange={(e) => setFees({ ...fees, totalFee: e.target.value })}
              required
            />
          </label>

          <label>
            Paid fee
            <input
              type="number"
              step="0.01"
              min="0"
              value={fees.paidFee}
              onChange={(e) => setFees({ ...fees, paidFee: e.target.value })}
              required
            />
          </label>

          <button className="btn ghost">Save corrections</button>
        </form>

        <h3>Payment history</h3>

        {fin.paymentHistory.length === 0 ? (
          <p className="muted">No payments yet.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Date</th>
                <th>Reference</th>
                <th>Method</th>
                <th className="num">Amount</th>
              </tr>
            </thead>

            <tbody>
              {[...fin.paymentHistory].reverse().map((t) => (
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

      <section className="panel">
        <div className="panel-head">
          <h2>Grades</h2>
          <span className="pill">CGPA {grades.cgpa.toFixed(2)}</span>
        </div>

        <p className="muted">
          Allowed grades: A, A-, B+, B, B-, C+, C, C-, D, F. GPA and CGPA are
          calculated when you save.
        </p>

        {grades.trimesters.map((t, ti) => (
          <div className="tri" key={ti}>
            <div className="panel-head">
              <input
                className="tri-name"
                placeholder="Trimester name, e.g. Trimester 1 - Fall 2026"
                value={t.trimesterName}
                onChange={(e) => updTri(ti, { trimesterName: e.target.value })}
              />

              <button
                className="btn ghost danger"
                onClick={() =>
                  setGrades({
                    ...grades,
                    trimesters: grades.trimesters.filter((_, i) => i !== ti),
                  })
                }
              >
                Remove trimester
              </button>
            </div>

            {t.courses.map((c, ci) => (
              <div className="course-row" key={ci}>
                <input
                  placeholder="Code"
                  value={c.courseCode}
                  onChange={(e) =>
                    updCourse(ti, ci, { courseCode: e.target.value })
                  }
                />

                <input
                  placeholder="Course name"
                  value={c.courseName}
                  onChange={(e) =>
                    updCourse(ti, ci, { courseName: e.target.value })
                  }
                />

                <input
                  placeholder="Grade"
                  value={c.grade}
                  onChange={(e) => updCourse(ti, ci, { grade: e.target.value })}
                />

                <input
                  type="number"
                  min="1"
                  value={c.creditHours}
                  onChange={(e) =>
                    updCourse(ti, ci, {
                      creditHours: Number(e.target.value),
                    })
                  }
                />

                <input
                  type="number"
                  min="0"
                  max="100"
                  placeholder="%"
                  value={c.percentage ?? ""}
                  onChange={(e) =>
                    updCourse(ti, ci, {
                      percentage: e.target.value,
                    })
                  }
                />

                <button
                  className="btn ghost danger"
                  onClick={() =>
                    updTri(ti, {
                      courses: t.courses.filter((_, i) => i !== ci),
                    })
                  }
                >
                  Remove
                </button>
              </div>
            ))}

            <button
              className="btn ghost"
              onClick={() =>
                updTri(ti, {
                  courses: [...t.courses, emptyCourse()],
                })
              }
            >
              Add course
            </button>
          </div>
        ))}

        <div className="actions">
          <button
            className="btn ghost"
            onClick={() =>
              setGrades({
                ...grades,
                trimesters: [...grades.trimesters, emptyTrimester()],
              })
            }
          >
            Add trimester
          </button>

          <button className="btn primary" onClick={saveGrades}>
            Save grades
          </button>
        </div>
      </section>
    </>
  );
}
