import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { api } from "../lib/api";
import type { AuthResponse, AuthUser, LoginPayload, RegisterPayload } from "../types/auth";

interface AuthContextValue {
  user: AuthUser | null;
  isLoading: boolean;
  login: (payload: LoginPayload) => Promise<void>;
  register: (payload: RegisterPayload) => Promise<void>;
  logout: () => Promise<void>;
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

  // There's no token to rehydrate from localStorage anymore - it's httpOnly,
  // so JS can never read it to check. Instead, ask the backend directly
  // whether the cookie it holds (sent automatically) is still valid.
  useEffect(() => {
    api
      .get<AuthResponse>("/api/auth/me")
      .then((response) => setUser(toAuthUser(response)))
      .catch(() => setUser(null))
      .finally(() => setIsLoading(false));
  }, []);

  async function login(payload: LoginPayload) {
    const response = await api.post<AuthResponse>("/api/auth/login", payload);
    setUser(toAuthUser(response));
  }

  async function register(payload: RegisterPayload) {
    const response = await api.post<AuthResponse>("/api/auth/register", payload);
    setUser(toAuthUser(response));
  }

  async function logout() {
    try {
      await api.post("/api/auth/logout", {});
    } finally {
      // Clear client state regardless of whether the request succeeded - the
      // cookie is either cleared server-side or was already invalid anyway.
      setUser(null);
    }
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
