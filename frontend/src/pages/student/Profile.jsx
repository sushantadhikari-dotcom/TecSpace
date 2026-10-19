import { useEffect, useState } from "react";
import api, { errMsg } from "../../api.js";
import ProfileForm from "../../components/ProfileForm.jsx";
import PhotoUpload from "../../components/PhotoUpload.jsx";
import HistoryTable from "../../components/HistoryTable.jsx";

export default function Profile() {
  const [data, setData] = useState(null);
  const [history, setHistory] = useState(null);
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");

  useEffect(() => {
    api.get("/student/profile").then((r) => setData(r.data)).catch((e) => setError(errMsg(e)));
    api.get("/student/history").then((r) => setHistory(r.data)).catch(() => {});
  }, []);

  const save = async (body) => {
    setError(""); setOk("");
    try {
      const r = await api.put("/student/profile", body);
      setData(r.data);
      setOk("Your profile was saved.");
    } catch (err) { setError(errMsg(err)); }
  };

  return (
    <>
      <h1 className="page-title">My profile</h1>
      {error && <div className="alert error" role="alert">{error}</div>}
      {ok && <div className="alert success" role="status">{ok}</div>}

      <section className="panel">
        <h2>Photo</h2>
        <PhotoUpload photoPath="/student/photo" uploadPath="/student/photo" />
      </section>

      {!data ? <p className="muted">Loading…</p> : <ProfileForm data={data} onSave={save} />}

      <section className="panel">
        <h2>Course registration history</h2>
        {!history ? <p className="muted">Loading…</p> : <HistoryTable rows={history} />}
      </section>
    </>
  );
}
