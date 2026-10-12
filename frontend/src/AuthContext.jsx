import { createContext, useContext, useState } from "react";
import api from "./api.js";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem("tecspace_user"));
    } catch {
      return null;
    }
  });

  const persist = (data) => {
    const { token, ...profile } = data;
    localStorage.setItem("tecspace_token", token);
    localStorage.setItem("tecspace_user", JSON.stringify(profile));
    setUser(profile);
    return profile;
  };

  const login = async (universityEmail, password) =>
    persist((await api.post("/auth/login", { universityEmail, password })).data);

  const signup = async (form) => persist((await api.post("/auth/signup", form)).data);

  const logout = () => {
    localStorage.removeItem("tecspace_token");
    localStorage.removeItem("tecspace_user");
    setUser(null);
  };

  return <AuthContext.Provider value={{ user, login, signup, logout }}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
