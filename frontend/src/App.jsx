import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "./AuthContext.jsx";
import Layout from "./components/Layout.jsx";
import Login from "./pages/Login.jsx";
import Signup from "./pages/Signup.jsx";
import Apply from "./pages/Apply.jsx";
import Track from "./pages/Track.jsx";
import PayEsewa from "./pages/PayEsewa.jsx";
import PayReturn from "./pages/PayReturn.jsx";
import Notifications from "./pages/Notifications.jsx";
import Account from "./pages/Account.jsx";
import Dashboard from "./pages/student/Dashboard.jsx";
import Courses from "./pages/student/Courses.jsx";
import Assignments from "./pages/student/Assignments.jsx";
import Timetable from "./pages/student/Timetable.jsx";
import Attendance from "./pages/student/Attendance.jsx";
import Financials from "./pages/student/Financials.jsx";
import Invoices from "./pages/student/Invoices.jsx";
import Receipt from "./pages/student/Receipt.jsx";
import Grades from "./pages/student/Grades.jsx";
import Transcript from "./pages/student/Transcript.jsx";
import Requests from "./pages/student/Requests.jsx";
import Certificate from "./pages/student/Certificate.jsx";
import Profile from "./pages/student/Profile.jsx";
import Library from "./pages/student/Library.jsx";
import TeacherCourses from "./pages/teacher/Courses.jsx";
import TeacherAssignments from "./pages/teacher/Assignments.jsx";
import TeacherGradebook from "./pages/teacher/Gradebook.jsx";
import TeacherAttendance from "./pages/teacher/Attendance.jsx";
import TeacherMessages from "./pages/teacher/Messages.jsx";
import Students from "./pages/admin/Students.jsx";
import StudentDetail from "./pages/admin/StudentDetail.jsx";
import StudentRecord from "./pages/admin/StudentRecord.jsx";
import AdminCourses from "./pages/admin/Courses.jsx";
import AdminTimetable from "./pages/admin/Timetable.jsx";
import AdminAnnouncements from "./pages/admin/Announcements.jsx";
import AdminExams from "./pages/admin/Exams.jsx";
import AdminAttendance from "./pages/admin/Attendance.jsx";
import AdminMessages from "./pages/admin/Messages.jsx";
import AdminFinance from "./pages/admin/Finance.jsx";
import AdminResults from "./pages/admin/Results.jsx";
import AdminUsers from "./pages/admin/Users.jsx";
import AdminAdmissions from "./pages/admin/Admissions.jsx";
import AdminRequests from "./pages/admin/Requests.jsx";
import AdminAudit from "./pages/admin/Audit.jsx";
import AdminAnalytics from "./pages/admin/Analytics.jsx";

const home = (user) => {
  if (user?.role === "ROLE_ADMIN" || user?.role === "ROLE_STAFF") return "/admin";
  if (user?.role === "ROLE_TEACHER") return "/teacher/courses";
  return "/student/dashboard";
};

function Protected({ roles, children }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (roles && !roles.includes(user.role)) return <Navigate to={home(user)} replace />;
  return <Layout>{children}</Layout>;
}

export default function App() {
  const { user } = useAuth();
  const S = (el) => <Protected roles={["ROLE_STUDENT"]}>{el}</Protected>;
  const T = (el) => <Protected roles={["ROLE_TEACHER"]}>{el}</Protected>;
  const A = (el) => <Protected roles={["ROLE_ADMIN", "ROLE_STAFF"]}>{el}</Protected>;
  const AdminOnly = (el) => <Protected roles={["ROLE_ADMIN"]}>{el}</Protected>;

  return (
    <Routes>
      <Route path="/login" element={user ? <Navigate to={home(user)} replace /> : <Login />} />
      <Route path="/signup" element={user ? <Navigate to={home(user)} replace /> : <Signup />} />

      {/* Public: admissions */}
      <Route path="/apply" element={<Apply />} />
      <Route path="/track" element={<Track />} />

      {/* Payer pages: opened from the QR on a phone, so no login needed */}
      <Route path="/pay/esewa/:token" element={<PayEsewa />} />
      <Route path="/pay/return" element={<PayReturn />} />

      {/* Any logged-in user */}
      <Route path="/notifications" element={<Protected><Notifications /></Protected>} />
      <Route path="/account" element={<Protected><Account /></Protected>} />

      <Route path="/student/dashboard" element={S(<Dashboard />)} />
      <Route path="/student/courses" element={S(<Courses />)} />
      <Route path="/student/assignments" element={S(<Assignments />)} />
      <Route path="/student/timetable" element={S(<Timetable />)} />
      <Route path="/student/attendance" element={S(<Attendance />)} />
      <Route path="/student/financials" element={S(<Financials />)} />
      <Route path="/student/invoices" element={S(<Invoices />)} />
      <Route path="/student/receipt/:id" element={S(<Receipt />)} />
      <Route path="/student/grades" element={S(<Grades />)} />
      <Route path="/student/transcript" element={S(<Transcript />)} />
      <Route path="/student/requests" element={S(<Requests />)} />
      <Route path="/student/certificate/:id" element={S(<Certificate />)} />
      <Route path="/student/profile" element={S(<Profile />)} />
      <Route path="/student/library" element={S(<Library />)} />

      <Route path="/teacher/courses" element={T(<TeacherCourses />)} />
      <Route path="/teacher/assignments" element={T(<TeacherAssignments />)} />
      <Route path="/teacher/gradebook" element={T(<TeacherGradebook />)} />
      <Route path="/teacher/attendance" element={T(<TeacherAttendance />)} />
      <Route path="/teacher/messages" element={T(<TeacherMessages />)} />

      <Route path="/admin" element={A(<Students />)} />
      <Route path="/admin/students/:id" element={A(<StudentDetail />)} />
      <Route path="/admin/records/:id" element={A(<StudentRecord />)} />
      <Route path="/admin/admissions" element={A(<AdminAdmissions />)} />
      <Route path="/admin/courses" element={A(<AdminCourses />)} />
      <Route path="/admin/timetable" element={A(<AdminTimetable />)} />
      <Route path="/admin/exams" element={A(<AdminExams />)} />
      <Route path="/admin/attendance" element={A(<AdminAttendance />)} />
      <Route path="/admin/finance" element={A(<AdminFinance />)} />
      <Route path="/admin/results" element={A(<AdminResults />)} />
      <Route path="/admin/requests" element={A(<AdminRequests />)} />
      <Route path="/admin/analytics" element={A(<AdminAnalytics />)} />
      <Route path="/admin/messages" element={A(<AdminMessages />)} />
      <Route path="/admin/announcements" element={A(<AdminAnnouncements />)} />
      <Route path="/admin/users" element={AdminOnly(<AdminUsers />)} />
      <Route path="/admin/audit" element={AdminOnly(<AdminAudit />)} />

      <Route path="*" element={<Navigate to={user ? home(user) : "/login"} replace />} />
    </Routes>
  );
}
