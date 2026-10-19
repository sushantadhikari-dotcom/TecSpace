import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import api, { errMsg } from "../../api.js";
import ProfileForm from "../../components/ProfileForm.jsx";
import PhotoUpload from "../../components/PhotoUpload.jsx";
import HistoryTable from "../../components/HistoryTable.jsx";

export default function StudentRecord() {
  const { id } = useParams();
  const [rec, setRec] = useState(null);
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");

  useEffect(() => {
    api.get(`/admin/student-profiles/${id}`).then((r) => setRec(r.data)).catch((e) => setError(errMsg(e)));
  }, [id]);

  const save = async (body) => {
    setError(""); setOk("");
    try {
      const r = await api.put(`/admin/student-profiles/${id}`, body);
      setRec({ ...rec, profile: r.data });
      setOk("Record saved.");
    } catch (err) { setError(errMsg(err)); }
  };

  if (!rec) return <p className="muted">{error || "Loading record…"}</p>;
  const s = rec.student;

  return (
    <>
      <Link to="/admin" className="back">Back to students</Link>
      <h1 className="page-title">{s.firstName} {s.lastName}</h1>
      {error && <div className="alert error" role="alert">{error}</div>}
      {ok && <div className="alert success" role="status">{ok}</div>}

      <section className="panel">
        <div className="panel-head">
          <div>
            <div>{s.universityEmail}</div>
            <div className="muted small">{s.mobileNumber} · {s.faculty} · {s.session} · joined {new Date(s.createdAt).toLocaleDateString()}</div>
          </div>
          <Link to={`/admin/students/${id}`}>Fees &amp; grades</Link>
        </div>
        <p className="muted small">Opening this record is written to the audit log.</p>
      </section>

      <section className="panel">
        <h2>Photo</h2>
        <PhotoUpload photoPath={`/admin/student-profiles/${id}/photo`} />
      </section>

      <ProfileForm data={rec.profile} onSave={save} saveLabel="Save record" />

      <section className="panel">
        <h2>Course registration history</h2>
        <HistoryTable rows={rec.history} />
      </section>
    </>
  );
}
