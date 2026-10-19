import { useEffect, useRef, useState } from "react";
import api, { errMsg } from "../api.js";
import { downloadFile } from "../files.js";

const kb = (n) => (n >= 1048576 ? `${(n / 1048576).toFixed(1)} MB` : `${Math.max(1, Math.round(n / 1024))} KB`);

/**
 * Lists the files at listPath and lets the user add more (PDF, PNG, JPG or Word, up to 5 MB).
 * downloadPath and deletePath are optional functions that turn a file into its URL.
 */
export default function FileUploader({
  listPath, uploadPath, params, downloadPath, deletePath, canEdit = true,
  accept = ".pdf,.png,.jpg,.jpeg,.docx", onChange,
}) {
  const [files, setFiles] = useState([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const input = useRef(null);

  const load = () =>
    api.get(listPath, { params }).then((r) => setFiles(r.data)).catch((e) => setError(errMsg(e)));
  useEffect(() => { load(); }, [listPath]);

  const upload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    setError(""); setBusy(true);
    try {
      const fd = new FormData();
      fd.append("file", file);
      await api.post(uploadPath, fd, { params });
      await load();
      if (onChange) onChange();
    } catch (err) { setError(errMsg(err)); }
    finally { setBusy(false); if (input.current) input.current.value = ""; }
  };

  const remove = async (f) => {
    setError("");
    try {
      await api.delete(deletePath(f));
      await load();
      if (onChange) onChange();
    } catch (err) { setError(errMsg(err)); }
  };

  return (
    <div className="file-box">
      {error && <div className="alert error" role="alert">{error}</div>}
      {files.length === 0 ? <p className="muted small" style={{ margin: "0 0 0.5rem" }}>No files yet.</p> : (
        <ul className="file-list">
          {files.map((f) => (
            <li key={f.id}>
              {downloadPath
                ? <button type="button" className="link-btn" onClick={() => downloadFile(downloadPath(f), f.name).catch((e) => setError(errMsg(e)))}>{f.name}</button>
                : <span>{f.name}</span>}
              <span className="small muted"> · {kb(f.size)}</span>
              {canEdit && deletePath && <button type="button" className="link-btn danger-link" onClick={() => remove(f)}> Remove</button>}
            </li>
          ))}
        </ul>
      )}
      {canEdit && (
        <label className="file-pick">
          <span className="btn ghost">{busy ? "Uploading…" : "Add a file"}</span>
          <input ref={input} type="file" accept={accept} onChange={upload} disabled={busy} hidden />
          <span className="small muted">PDF, PNG, JPG or Word · up to 5 MB</span>
        </label>
      )}
    </div>
  );
}
