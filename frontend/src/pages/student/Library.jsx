import { useEffect, useState } from "react";
import api, { errMsg } from "../../api.js";
import { useAuth } from "../../AuthContext.jsx";
import VirtualLibraryCard from "../../components/VirtualLibraryCard.jsx";
import { blobUrl } from "../../files.js";

export default function Library() {
  const { user } = useAuth();
  const [card, setCard] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [photo, setPhoto] = useState(null);
  useEffect(() => { blobUrl("/student/photo").then(setPhoto).catch(() => {}); }, []);

  useEffect(() => {
    api.get("/student/library-card")
      .then((r) => setCard(r.data))
      .catch((e) => { if (e.response?.status !== 404) setError(errMsg(e)); })
      .finally(() => setLoading(false));
  }, []);

  const apply = async () => {
    setError(""); setBusy(true);
    try {
      setCard((await api.post("/student/library-card/apply")).data);
    } catch (err) {
      setError(errMsg(err));
    } finally {
      setBusy(false);
    }
  };

  if (loading) return <p className="muted">Loading…</p>;

  return (
    <>
      <h1 className="page-title">Library card</h1>
      {error && <div className="alert error" role="alert">{error}</div>}

      {card ? (
        <section className="panel center">
          <VirtualLibraryCard card={card} student={user} photo={photo} />
          <p className="muted">Show this QR code at the library desk to borrow books.</p>
        </section>
      ) : (
        <section className="panel">
          <h2>You don't have a library card yet</h2>
          <p className="muted">Apply now and your virtual card is issued instantly. It is valid for four years.</p>
          <button className="btn primary" onClick={apply} disabled={busy}>
            {busy ? "Issuing card…" : "Apply for library card"}
          </button>
        </section>
      )}
    </>
  );
}
