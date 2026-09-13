import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { api, clearToken, getToken, setToken } from "../lib/api";
import type { AuthResponse, AuthUser, LoginPayload, RegisterPayload } from "../types/auth";

const USER_KEY = "cadence_user";

interface AuthContextValue {
  user: AuthUser | null;
  isLoading: boolean;
  login: (payload: LoginPayload) => Promise<void>;
  register: (payload: RegisterPayload) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function toAuthUser(response: AuthResponse): AuthUser {
  return {
    userId: response.userId,
    name: response.name,
    email: response.email,
    role: response.role,
  };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Rehydrate from localStorage on load, so a page refresh doesn't log the user out.
  // This trusts the stored user record without re-validating the token against the
  // backend - fine for a 2-day assignment, but a real deployment would want a
  // "/api/auth/me" check here to catch an expired/invalid token immediately.
  useEffect(() => {
    const token = getToken();
    const storedUser = localStorage.getItem(USER_KEY);

    if (token && storedUser) {
      try {
        setUser(JSON.parse(storedUser) as AuthUser);
      } catch {
        clearToken();
        localStorage.removeItem(USER_KEY);
      }
    }

    setIsLoading(false);
  }, []);

  function persist(response: AuthResponse) {
    setToken(response.token);
    const authUser = toAuthUser(response);
    localStorage.setItem(USER_KEY, JSON.stringify(authUser));
    setUser(authUser);
  }

  async function login(payload: LoginPayload) {
    const response = await api.post<AuthResponse>("/api/auth/login", payload);
    persist(response);
  }

  async function register(payload: RegisterPayload) {
    const response = await api.post<AuthResponse>("/api/auth/register", payload);
    persist(response);
  }

  function logout() {
    clearToken();
    localStorage.removeItem(USER_KEY);
    setUser(null);
  }

  return (
    <AuthContext.Provider value={{ user, isLoading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
