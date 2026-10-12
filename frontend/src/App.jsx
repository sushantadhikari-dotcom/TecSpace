import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "./AuthContext.jsx";
import Layout from "./components/Layout.jsx";
import Login from "./pages/Login.jsx";
import Signup from "./pages/Signup.jsx";
import PayScan from "./pages/PayScan.jsx";
import Financials from "./pages/student/Financials.jsx";
import Grades from "./pages/student/Grades.jsx";
import Library from "./pages/student/Library.jsx";
import Students from "./pages/admin/Students.jsx";
import StudentDetail from "./pages/admin/StudentDetail.jsx";

function Protected({ role, children }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (user.role !== role) return <Navigate to={home(user)} replace />;
  return <Layout>{children}</Layout>;
}

const home = (user) => (user?.role === "ROLE_ADMIN" ? "/admin" : "/student/financials");

export default function App() {
  const { user } = useAuth();
  return (
    <Routes>
      <Route path="/login" element={user ? <Navigate to={home(user)} replace /> : <Login />} />
      <Route path="/signup" element={user ? <Navigate to={home(user)} replace /> : <Signup />} />

      {/* Opened by whoever scans the QR - no login needed */}
      <Route path="/pay/:token" element={<PayScan />} />

      <Route path="/student/financials" element={<Protected role="ROLE_STUDENT"><Financials /></Protected>} />
      <Route path="/student/grades" element={<Protected role="ROLE_STUDENT"><Grades /></Protected>} />
      <Route path="/student/library" element={<Protected role="ROLE_STUDENT"><Library /></Protected>} />

      <Route path="/admin" element={<Protected role="ROLE_ADMIN"><Students /></Protected>} />
      <Route path="/admin/students/:id" element={<Protected role="ROLE_ADMIN"><StudentDetail /></Protected>} />

      <Route path="*" element={<Navigate to={user ? home(user) : "/login"} replace />} />
    </Routes>
  );
}
