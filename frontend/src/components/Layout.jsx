import { useEffect, useState } from "react";
import { NavLink, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../AuthContext.jsx";
import api from "../api.js";
import Logo from "./Logo.jsx";

const studentLinks = [
  { to: "/student/dashboard", label: "Dashboard" },
  { to: "/student/courses", label: "Course registration" },
  { to: "/student/timetable", label: "Timetable & exams" },
  { to: "/student/attendance", label: "Attendance" },
  { to: "/student/financials", label: "Fees & payments" },
  { to: "/student/invoices", label: "Invoices & ledger" },
  { to: "/student/grades", label: "Grades" },
  { to: "/student/transcript", label: "Transcript" },
  { to: "/student/library", label: "Library card" },
];
const adminLinks = [
  { to: "/admin", label: "Students" },
  { to: "/admin/courses", label: "Courses" },
  { to: "/admin/timetable", label: "Timetable" },
  { to: "/admin/exams", label: "Exams" },
  { to: "/admin/attendance", label: "Attendance" },
  { to: "/admin/finance", label: "Finance" },
  { to: "/admin/results", label: "Results & grading" },
  { to: "/admin/messages", label: "Send message" },
  { to: "/admin/announcements", label: "Announcements" },
];

export default function Layout({ children }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const [unread, setUnread] = useState(0);
  const links = user.role === "ROLE_ADMIN" ? adminLinks : studentLinks;

  // Unread badge: refresh on every page change and every 30 seconds
  useEffect(() => {
    const load = () =>
      api.get("/notifications/unread-count").then((r) => setUnread(r.data.count)).catch(() => {});
    load();
    const t = setInterval(load, 30000);
    return () => clearInterval(t);
  }, [pathname]);

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
          <NavLink to="/notifications" className={({ isActive }) => (isActive ? "active" : "")}>
            Inbox {unread > 0 && <span className="badge-count" aria-label={`${unread} unread`}>{unread}</span>}
          </NavLink>
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
