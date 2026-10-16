import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import api, { errMsg, money } from "../api.js";
import Logo from "../components/Logo.jsx";

/** eSewa sends the payer back here. We never trust the URL: the server re-checks with eSewa. */
export default function PayReturn() {
  const [params] = useSearchParams();
  const data = params.get("data");
  const [info, setInfo] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!data) return;
    api.post("/pay/verify", { data })
      .then((r) => setInfo(r.data))
      .catch((e) => setError(errMsg(e)));
  }, [data]);

  return (
    <div className="pay-page">
      <Logo size={30} />
      <div className="panel pay-card">
        {!data && (
          <>
            <h1>Payment not completed</h1>
            <p className="muted">The payment was cancelled or did not go through. No money was taken. Ask the student to generate a new QR code.</p>
          </>
        )}
        {data && error && <div className="alert error" role="alert">{error}</div>}
        {data && !info && !error && <p className="muted">Confirming your payment with eSewa…</p>}
        {info?.status === "COMPLETED" && (
          <>
            <h1>Payment completed</h1>
            <p>{money(info.amount)} was paid for {info.studentName}. You can close this page.</p>
          </>
        )}
      </div>
    </div>
  );
}