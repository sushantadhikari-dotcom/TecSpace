import { useEffect, useState } from "react";

const blankContact = () => ({ name: "", relationship: "", phone: "" });

/** Personal details, emergency contacts and medical information. Used by students and by staff. */
export default function ProfileForm({ data, onSave, saveLabel = "Save profile" }) {
  const [p, setP] = useState(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const m = data.medical || {};
    setP({
      dateOfBirth: data.dateOfBirth || "",
      gender: data.gender || "",
      address: data.address || "",
      nationality: data.nationality || "",
      emergencyContacts: (data.emergencyContacts || []).map((c) => ({ ...c })),
      medical: {
        bloodGroup: m.bloodGroup || "", allergies: m.allergies || "", conditions: m.conditions || "",
        medications: m.medications || "", notes: m.notes || "",
      },
    });
  }, [data]);

  if (!p) return null;

  const set = (k) => (e) => setP({ ...p, [k]: e.target.value });
  const setMed = (k) => (e) => setP({ ...p, medical: { ...p.medical, [k]: e.target.value } });
  const setContact = (i, patch) =>
    setP({ ...p, emergencyContacts: p.emergencyContacts.map((c, n) => (n === i ? { ...c, ...patch } : c)) });

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    try { await onSave({ ...p, dateOfBirth: p.dateOfBirth || null }); }
    finally { setBusy(false); }
  };

  return (
    <form onSubmit={submit} className="stack">
      <section className="panel">
        <h2>Personal details</h2>
        <div className="row">
          <label>Date of birth<input type="date" value={p.dateOfBirth} onChange={set("dateOfBirth")} /></label>
          <label>Gender
            <select value={p.gender} onChange={set("gender")}>
              <option value="">Prefer not to say</option><option value="MALE">Male</option>
              <option value="FEMALE">Female</option><option value="OTHER">Other</option>
            </select>
          </label>
        </div>
        <div className="row">
          <label>Nationality<input value={p.nationality} onChange={set("nationality")} /></label>
          <label>Address<input value={p.address} onChange={set("address")} /></label>
        </div>
      </section>

      <section className="panel">
        <h2>Emergency contacts</h2>
        {p.emergencyContacts.length === 0 && <p className="muted">No contacts yet. Add at least one person we can call in an emergency.</p>}
        {p.emergencyContacts.map((c, i) => (
          <div className="contact-row" key={i}>
            <label>Name<input value={c.name} onChange={(e) => setContact(i, { name: e.target.value })} required /></label>
            <label>Relationship<input value={c.relationship} onChange={(e) => setContact(i, { relationship: e.target.value })} placeholder="Mother, brother…" required /></label>
            <label>Phone<input value={c.phone} onChange={(e) => setContact(i, { phone: e.target.value })} placeholder="+9779800000000" required /></label>
            <button type="button" className="btn ghost danger" onClick={() => setP({ ...p, emergencyContacts: p.emergencyContacts.filter((_, n) => n !== i) })}>Remove</button>
          </div>
        ))}
        {p.emergencyContacts.length < 5 && (
          <button type="button" className="btn ghost" onClick={() => setP({ ...p, emergencyContacts: [...p.emergencyContacts, blankContact()] })}>Add a contact</button>
        )}
      </section>

      <section className="panel">
        <h2>Medical information</h2>
        <p className="muted small">Only you and authorised staff can see this. Share what the university should know in an emergency.</p>
        <div className="row">
          <label>Blood group<input value={p.medical.bloodGroup} onChange={setMed("bloodGroup")} placeholder="O+" /></label>
          <label>Allergies<input value={p.medical.allergies} onChange={setMed("allergies")} /></label>
        </div>
        <div className="row">
          <label>Medical conditions<input value={p.medical.conditions} onChange={setMed("conditions")} /></label>
          <label>Regular medication<input value={p.medical.medications} onChange={setMed("medications")} /></label>
        </div>
        <label>Other notes<textarea rows="3" value={p.medical.notes} onChange={setMed("notes")} /></label>
      </section>

      <div><button className="btn primary" disabled={busy}>{busy ? "Saving…" : saveLabel}</button></div>
    </form>
  );
}
