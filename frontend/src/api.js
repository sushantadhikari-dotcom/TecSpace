import axios from "axios";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "http://localhost:8080/api",
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("tecspace_token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (err) => {
    const isAuthCall = err.config?.url?.startsWith("/auth/");
    if (err.response?.status === 401 && !isAuthCall) {
      localStorage.removeItem("tecspace_token");
      localStorage.removeItem("tecspace_user");
      window.location.href = "/login";
    }
    return Promise.reject(err);
  },
);

/** Pull a readable message out of an axios error. */
export function errMsg(err) {
  return (
    err.response?.data?.message ||
    (err.code === "ERR_NETWORK"
      ? "Cannot reach the TecSpace server. Is the backend running?"
      : "Something went wrong")
  );
}

export const money = (n) =>
  "Rs " +
  new Intl.NumberFormat("en-IN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Number(n || 0));

export const methodLabel = (m) =>
  ({
    QR_PAYMENT: "QR payment",
    CASH_PAYMENT: "Cash payment",
    ADMIN_ADJUSTMENT: "Admin adjustment",
  })[m] || m;

export default api;
