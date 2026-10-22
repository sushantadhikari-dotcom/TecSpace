import { useEffect, useState } from "react";
import api, { errMsg } from "../../api.js";

const ROLE_LABEL = { ROLE_ADMIN: "Administrator", ROLE_STAFF: "Staff", ROLE_TEACHER: "Teacher" };

export default function Users() {
  const [users, setUsers] = useState(null);
  const [catalog, setCatalog] = useState({});
  const [courses, setCourses] = useState([]);
  const [assign, setAssign] = useState([]);
  const [form, setForm] = useState({ universityEmail: "", firstName: "", lastName: "", mobileNumber: "", role: "STAFF", permissions: [] });
  const [editing, setEditing] = useState(null);
  const [secret, setSecret] = useState(null);
  const [error, setError] = useState("");
  const [ok, setOk] = useState("");

  const load = () => {
    api.get("/admin/users").then((r) => setUsers(r.data)).catch((e) => setError(errMsg(e)));
    api.get("/admin/faculty-assignments").then((r) => setAssign(r.data)).catch(() => {});
  };
  useEffect(() => {
    load();
    api.get("/admin/users/permissions").then((r) => setCatalog(r.data)).catch(() => {});
    api.get("/admin/courses").then((r) => setCourses(r.data)).catch(() => {});
  }, []);

  const togglePerm = (list, key) => (list.includes(key) ? list.filter((p) => p !== key) : [...list, key]);

  const create = async (e) => {
    e.preventDefault();
    setError(""); setOk(""); setSecret(null);
    try {
      const r = await api.post("/admin/users", form);
      setSecret({ who: r.data.user.universityEmail, password: r.data.temporaryPassword });
      setForm({ ...form, universityEmail: "", firstName: "", lastName: "", mobileNumber: "", permissions: [] });
      load();
    } catch (err) { setError(errMsg(err)); }
  };

  const saveEdit = async () => {
    setError(""); setOk("");
    try {
      await api.put(`/admin/users/${editing.id}`, { role: editing.role, permissions: editing.permissions, active: editing.active });
      setOk("Account updated.");
      setEditing(null);
      load();
    } catch (err) { setError(errMsg(err)); }
  };

  const reset = async (u) => {
    if (!window.confirm(`Reset the password for ${u.universityEmail}?`)) return;
    setError(""); setOk(""); setSecret(null);
    try {
      const r = await api.post(`/admin/users/${u.id}/reset-password`);
      setSecret({ who: u.universityEmail, password: r.data.temporaryPassword });
    } catch (err) { setError(errMsg(err)); }
  };

  const unlock = async (u) => {
    setError(""); setOk(""); setSecret(null);
    try {
      await api.post(`/admin/users/${u.id}/unlock`);
      setOk(`${u.universityEmail} was unlocked.`);
    } catch (err) { setError(errMsg(err)); }
  };

  const saveAssign = async (t, codes) => {
    setError(""); setOk("");
    try {
      const r = await api.put(`/admin/faculty-assignments/${t.teacherId}`, { courseCodes: codes });
      setAssign(r.data);
      setOk(`Courses saved for ${t.teacherName}.`);
    } catch (err) { setError(errMsg(err)); }
  };

  const permBoxes = (selected, onToggle) => (
    <div className="perm-grid">
      {Object.entries(catalog).map(([key, text]) => (
        <label className="check" key={key}>
          <input type="checkbox" checked={selected.includes(key)} onChange={() => onToggle(key)} />
          {text}
        </label>
      ))}
    </div>
  );

  return (
    <>
      <h1 className="page-title">Users &amp; roles</h1>
      {error && <div className="alert error" role="alert">{error}</div>}
      {ok && <div className="alert success" role="status">{ok}</div>}
      {secret && (
        <div className="alert success" role="status">
          Temporary password for <strong>{secret.who}</strong>. Copy it now, it will not be shown again:
          <div className="temp-pass">{secret.password}</div>
          Ask them to change it from the Account page after logging in.
        </div>
      )}

      <section className="panel">
        <h2>Add a staff member, teacher or administrator</h2>
        <form onSubmit={create} className="stack">
          <div className="row">
            <label>First name<input value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} required /></label>
            <label>Last name<input value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} required /></label>
          </div>
          <div className="row">
            <label>Email (their login)<input type="email" value={form.universityEmail} onChange={(e) => setForm({ ...form, universityEmail: e.target.value })} required /></label>
            <label>Mobile<input value={form.mobileNumber} onChange={(e) => setForm({ ...form, mobileNumber: e.target.value })} placeholder="+9779800000000" required /></label>
          </div>
          <label style={{ maxWidth: 260 }}>Role
            <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
              <option value="STAFF">Staff (office work, with permissions)</option>
              <option value="TEACHER">Teacher (their own courses)</option>
              <option value="ADMIN">Administrator (everything)</option>
            </select>
          </label>
          {form.role === "STAFF" && (
            <>
              <span className="small muted">What can this staff member do?</span>
              {permBoxes(form.permissions, (k) => setForm({ ...form, permissions: togglePerm(form.permissions, k) }))}
            </>
          )}
          <div><button className="btn primary">Create account</button></div>
        </form>
      </section>

      <section className="panel">
        <h2>Accounts</h2>
        {!users ? <p className="muted">Loading…</p> : (
          <table>
            <thead><tr><th>Name</th><th>Role</th><th>Status</th><th></th></tr></thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id}>
                  <td>{u.firstName} {u.lastName}<div className="small muted">{u.universityEmail}</div></td>
                  <td>{ROLE_LABEL[u.role]}{u.role === "ROLE_STAFF" && <div className="small muted">{u.permissions.length} permission(s)</div>}</td>
                  <td><span className={`pill ${u.active ? "ok" : "bad"}`}>{u.active ? "Active" : "Disabled"}</span></td>
                  <td className="num">
                    <button className="btn ghost" onClick={() => setEditing({ id: u.id, name: `${u.firstName} ${u.lastName}`, role: u.role.replace("ROLE_", ""), permissions: [...u.permissions], active: u.active })}>Edit</button>{" "}
                    <button className="btn ghost" onClick={() => reset(u)}>Reset password</button>{" "}
                    <button className="btn ghost" onClick={() => unlock(u)}>Unlock</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      {editing && (
        <section className="panel">
          <h2>Edit {editing.name}</h2>
          <div className="stack">
            <label style={{ maxWidth: 260 }}>Role
              <select value={editing.role} onChange={(e) => setEditing({ ...editing, role: e.target.value })}>
                <option value="STAFF">Staff</option><option value="TEACHER">Teacher</option><option value="ADMIN">Administrator</option>
              </select>
            </label>
            {editing.role === "STAFF" && permBoxes(editing.permissions, (k) => setEditing({ ...editing, permissions: togglePerm(editing.permissions, k) }))}
            <label className="check">
              <input type="checkbox" checked={editing.active} onChange={(e) => setEditing({ ...editing, active: e.target.checked })} />
              Account is active (untick to block this person from logging in)
            </label>
            <div className="actions">
              <button className="btn ghost" onClick={() => setEditing(null)}>Cancel</button>
              <button className="btn primary" onClick={saveEdit}>Save changes</button>
            </div>
          </div>
        </section>
      )}

      <section className="panel">
        <h2>Teacher course assignments</h2>
        {assign.length === 0 ? <p className="muted">Create a teacher account first, then assign their courses here.</p> : assign.map((t) => (
          <Assign key={t.teacherId} teacher={t} courses={courses} onSave={saveAssign} />
        ))}
      </section>
    </>
  );
}

function Assign({ teacher, courses, onSave }) {
  const [codes, setCodes] = useState(teacher.courseCodes);
  useEffect(() => { setCodes(teacher.courseCodes); }, [teacher.courseCodes]);
  const toggle = (c) => setCodes(codes.includes(c) ? codes.filter((x) => x !== c) : [...codes, c]);

  return (
    <div className="invoice">
      <div className="panel-head">
        <div><strong>{teacher.teacherName}</strong><div className="small muted">{teacher.email}</div></div>
        <button className="btn primary" onClick={() => onSave(teacher, codes)}>Save</button>
      </div>
      {courses.length === 0 ? <p className="muted small">No courses exist yet.</p> : (
        <div className="perm-grid">
          {courses.map((c) => (
            <label className="check" key={c.code}>
              <input type="checkbox" checked={codes.includes(c.code)} onChange={() => toggle(c.code)} />
              {c.code} · {c.name}
            </label>
          ))}
        </div>
      )}
    </div>
  );
}
