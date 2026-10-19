import { useEffect, useRef, useState } from "react";
import api, { errMsg } from "../api.js";
import { blobUrl } from "../files.js";

/** Shows a protected photo. If uploadPath is given, the person can also replace it. */
export default function PhotoUpload({ photoPath, uploadPath }) {
  const [src, setSrc] = useState(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const input = useRef(null);

  const load = () => blobUrl(photoPath).then(setSrc).catch(() => setSrc(null));
  useEffect(() => { load(); }, [photoPath]);

  const upload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    setError(""); setBusy(true);
    try {
      const fd = new FormData();
      fd.append("file", file);
      await api.put(uploadPath, fd);
      await load();
    } catch (err) { setError(errMsg(err)); }
    finally { setBusy(false); if (input.current) input.current.value = ""; }
  };

  return (
    <div className="photo-row">
      {src ? <img className="avatar" src={src} alt="Student photo" /> : <div className="avatar empty" aria-label="No photo">No photo</div>}
      <div>
        {error && <div className="alert error" role="alert">{error}</div>}
        {uploadPath ? (
          <label className="file-pick">
            <span className="btn ghost">{busy ? "Uploading…" : src ? "Change photo" : "Upload a photo"}</span>
            <input ref={input} type="file" accept=".png,.jpg,.jpeg" onChange={upload} disabled={busy} hidden />
            <span className="small muted">A clear passport-style photo. PNG or JPG, up to 5 MB. It appears on your library card.</span>
          </label>
        ) : <span className="small muted">{src ? "Photo uploaded by the student." : "The student has not uploaded a photo."}</span>}
      </div>
    </div>
  );
}
