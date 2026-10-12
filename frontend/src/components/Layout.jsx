import { NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../AuthContext.jsx";
import Logo from "./Logo.jsx";

const studentLinks = [
  { to: "/student/financials", label: "Fees & payments" },
  { to: "/student/grades", label: "Grades" },
  { to: "/student/library", label: "Library card" },
];
const adminLinks = [{ to: "/admin", label: "Students" }];

export default function Layout({ children }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const links = user.role === "ROLE_ADMIN" ? adminLinks : studentLinks;

  return (
    <div className="shell">
      <aside className="sidebar">
        <Logo />
        <nav aria-label="Main">
          {links.map((l) => (
            <NavLink key={l.to} to={l.to} end={l.to === "/admin"} className={({ isActive }) => (isActive ? "active" : "")}>
              {l.label}
            </NavLink>
          ))}
        </nav>
        <div className="who">
          <strong>{user.firstName} {user.lastName}</strong>
          <span>{user.role === "ROLE_ADMIN" ? "Administrator" : `${user.faculty} · ${user.session}`}</span>
          <button
            className="btn ghost"
            onClick={() => {
              logout();
              navigate("/login");
            }}
          >
            Log out
          </button>
        </div>
      </aside>
      <main className="content">{children}</main>
    </div>
  );
}
