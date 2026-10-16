import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "./AuthContext.jsx";
import Layout from "./components/Layout.jsx";
import Login from "./pages/Login.jsx";
import Signup from "./pages/Signup.jsx";
import PayEsewa from "./pages/PayEsewa.jsx";
import PayReturn from "./pages/PayReturn.jsx";
import Notifications from "./pages/Notifications.jsx";
import Dashboard from "./pages/student/Dashboard.jsx";
import Courses from "./pages/student/Courses.jsx";
import Timetable from "./pages/student/Timetable.jsx";
import Attendance from "./pages/student/Attendance.jsx";
import Financials from "./pages/student/Financials.jsx";
import Invoices from "./pages/student/Invoices.jsx";
import Receipt from "./pages/student/Receipt.jsx";
import Grades from "./pages/student/Grades.jsx";
import Transcript from "./pages/student/Transcript.jsx";
import Library from "./pages/student/Library.jsx";
import Students from "./pages/admin/Students.jsx";
import StudentDetail from "./pages/admin/StudentDetail.jsx";
import AdminCourses from "./pages/admin/Courses.jsx";
import AdminTimetable from "./pages/admin/Timetable.jsx";
import AdminAnnouncements from "./pages/admin/Announcements.jsx";
import AdminExams from "./pages/admin/Exams.jsx";
import AdminAttendance from "./pages/admin/Attendance.jsx";
import AdminMessages from "./pages/admin/Messages.jsx";
import AdminFinance from "./pages/admin/Finance.jsx";
import AdminResults from "./pages/admin/Results.jsx";

function Protected({ role, children }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (role && user.role !== role) return <Navigate to={home(user)} replace />;
  return <Layout>{children}</Layout>;
}

const home = (user) => (user?.role === "ROLE_ADMIN" ? "/admin" : "/student/dashboard");

export default function App() {
  const { user } = useAuth();
  const S = (el) => <Protected role="ROLE_STUDENT">{el}</Protected>;
  const A = (el) => <Protected role="ROLE_ADMIN">{el}</Protected>;

  return (
    <Routes>
      <Route path="/login" element={user ? <Navigate to={home(user)} replace /> : <Login />} />
      <Route path="/signup" element={user ? <Navigate to={home(user)} replace /> : <Signup />} />

      {/* Payer pages: opened from the QR on a phone, so no login needed */}
      <Route path="/pay/esewa/:token" element={<PayEsewa />} />
      <Route path="/pay/return" element={<PayReturn />} />

      {/* Inbox: any logged-in user */}
      <Route path="/notifications" element={<Protected><Notifications /></Protected>} />

      <Route path="/student/dashboard" element={S(<Dashboard />)} />
      <Route path="/student/courses" element={S(<Courses />)} />
      <Route path="/student/timetable" element={S(<Timetable />)} />
      <Route path="/student/attendance" element={S(<Attendance />)} />
      <Route path="/student/financials" element={S(<Financials />)} />
      <Route path="/student/invoices" element={S(<Invoices />)} />
      <Route path="/student/receipt/:id" element={S(<Receipt />)} />
      <Route path="/student/grades" element={S(<Grades />)} />
      <Route path="/student/transcript" element={S(<Transcript />)} />
      <Route path="/student/library" element={S(<Library />)} />

      <Route path="/admin" element={A(<Students />)} />
      <Route path="/admin/students/:id" element={A(<StudentDetail />)} />
      <Route path="/admin/courses" element={A(<AdminCourses />)} />
      <Route path="/admin/timetable" element={A(<AdminTimetable />)} />
      <Route path="/admin/exams" element={A(<AdminExams />)} />
      <Route path="/admin/attendance" element={A(<AdminAttendance />)} />
      <Route path="/admin/finance" element={A(<AdminFinance />)} />
      <Route path="/admin/results" element={A(<AdminResults />)} />
      <Route path="/admin/messages" element={A(<AdminMessages />)} />
      <Route path="/admin/announcements" element={A(<AdminAnnouncements />)} />

      <Route path="*" element={<Navigate to={user ? home(user) : "/login"} replace />} />
    </Routes>
  );
}
