import { useEffect, useState } from "react";
import { NavLink, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../AuthContext.jsx";
import api from "../api.js";
import Logo from "./Logo.jsx";

const studentLinks = [
  { to: "/student/dashboard", label: "Dashboard" },
  { to: "/student/courses", label: "Course registration" },
  { to: "/student/assignments", label: "Assignments" },
  { to: "/student/timetable", label: "Timetable & exams" },
  { to: "/student/admit-card", label: "Exam admit card" },
  { to: "/calendar", label: "Academic calendar" },
  { to: "/student/attendance", label: "Attendance" },
  { to: "/student/financials", label: "Fees & payments" },
  { to: "/student/invoices", label: "Invoices & ledger" },
  { to: "/student/grades", label: "Grades" },
  { to: "/student/transcript", label: "Transcript" },
  { to: "/student/requests", label: "Requests & messages" },
  { to: "/student/profile", label: "My profile" },
  { to: "/student/library", label: "Library card" },
];

const teacherLinks = [
  { to: "/teacher/courses", label: "My courses" },
  { to: "/calendar", label: "Academic calendar" },
  { to: "/teacher/assignments", label: "Assignments" },
  { to: "/teacher/gradebook", label: "Gradebook" },
  { to: "/teacher/attendance", label: "Attendance" },
  { to: "/teacher/messages", label: "Message students" },
];

// perm = permission a STAFF member needs to see the link. adminOnly = administrators only.
const adminLinks = [
  { to: "/admin", label: "Students" },
  { to: "/admin/admissions", label: "Admissions", perm: "MANAGE_ADMISSIONS" },
  { to: "/admin/courses", label: "Courses" },
  { to: "/admin/calendar", label: "Calendar & reminders", perm: "MANAGE_COURSES" },
  { to: "/admin/registrations", label: "Course registrations", perm: "APPROVE_REGISTRATIONS" },
  { to: "/admin/timetable", label: "Timetable", perm: "MANAGE_TIMETABLE" },
  { to: "/admin/exams", label: "Exams", perm: "MANAGE_EXAMS" },
  { to: "/admin/attendance", label: "Attendance", perm: "MANAGE_ATTENDANCE" },
  { to: "/admin/finance", label: "Finance", perm: "MANAGE_FINANCE" },
  { to: "/admin/results", label: "Results & grading", perm: "MANAGE_RESULTS" },
  { to: "/admin/requests", label: "Student requests", perm: "PROCESS_REQUESTS" },
  { to: "/admin/analytics", label: "Analytics", perm: "VIEW_ANALYTICS" },
  { to: "/admin/messages", label: "Send message", perm: "COMMUNICATE" },
  { to: "/admin/announcements", label: "Announcements", perm: "COMMUNICATE" },
  { to: "/admin/users", label: "Users & roles", adminOnly: true },
  { to: "/admin/audit", label: "Audit log", adminOnly: true },
];

const ROLE_LABEL = { ROLE_ADMIN: "Administrator", ROLE_STAFF: "Staff", ROLE_TEACHER: "Teacher" };

export default function Layout({ children }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const [unread, setUnread] = useState(0);
  const [permissions, setPermissions] = useState([]);

  // Staff menus depend on what the administrator allowed. The server enforces this too.
  useEffect(() => {
    api.get("/account/me").then((r) => setPermissions(r.data.permissions || [])).catch(() => {});
  }, [user.role]);

  // Unread badge: refresh on every page change and every 30 seconds
  useEffect(() => {
    const load = () =>
      api.get("/notifications/unread-count").then((r) => setUnread(r.data.count)).catch(() => {});
    load();
    const t = setInterval(load, 30000);
    return () => clearInterval(t);
  }, [pathname]);

  let links;
  if (user.role === "ROLE_STUDENT") links = studentLinks;
  else if (user.role === "ROLE_TEACHER") links = teacherLinks;
  else if (user.role === "ROLE_ADMIN") links = adminLinks;
  else links = adminLinks.filter((l) => !l.adminOnly && (!l.perm || permissions.includes(l.perm)));

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
          <NavLink to="/account" className={({ isActive }) => (isActive ? "active" : "")}>Account</NavLink>
        </nav>
        <div className="who">
          <strong>{user.firstName} {user.lastName}</strong>
          <span>{ROLE_LABEL[user.role] || `${user.faculty} · ${user.session}`}</span>
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
