import { useEffect, useRef, useState } from "react";
import { useParams } from "react-router-dom";
import api, { errMsg } from "../api.js";
import Logo from "../components/Logo.jsx";

/** Opened when the QR is scanned. Sends the payer to eSewa, where they log in and approve the payment. */
export default function PayEsewa() {
  const { token } = useParams();
  const [form, setForm] = useState(null);
  const [error, setError] = useState("");
  const formRef = useRef(null);

  useEffect(() => {
    api.get(`/pay/${token}/esewa-form`)
      .then((r) => setForm(r.data))
      .catch((e) => setError(errMsg(e)));
  }, [token]);

  useEffect(() => {
    if (form) formRef.current?.submit();
  }, [form]);

  return (
    <div className="pay-page">
      <Logo size={30} />
      <div className="panel pay-card">
        {error && <div className="alert error" role="alert">{error}</div>}
        {!error && <p className="muted">Taking you to eSewa…</p>}
        {form && (
          <form ref={formRef} method="POST" action={form.action}>
            {Object.entries(form.fields).map(([k, v]) => (
              <input key={k} type="hidden" name={k} value={v} />
            ))}
            <noscript><button className="btn primary">Continue to eSewa</button></noscript>
          </form>
        )}
      </div>
    </div>
  );
}