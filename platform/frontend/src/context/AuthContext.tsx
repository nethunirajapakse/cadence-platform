import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { acceptInvitation, getCurrentUser, loginUser, logoutUser } from "@/api/auth";
import type { AcceptInvitePayload, AuthResponse, AuthUser, LoginPayload } from "@/types/auth";

interface AuthContextValue {
  user: AuthUser | null;
  isLoading: boolean;
  login: (payload: LoginPayload) => Promise<void>;
  acceptInvite: (payload: AcceptInvitePayload) => Promise<void>;
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

  // No token to rehydrate from localStorage - it's httpOnly, so JS can never
  // read it to check. Ask the backend directly whether the cookie is valid.
  useEffect(() => {
    getCurrentUser()
      .then((response) => setUser(toAuthUser(response)))
      .catch(() => setUser(null))
      .finally(() => setIsLoading(false));
  }, []);

  async function login(payload: LoginPayload) {
    const response = await loginUser(payload);
    setUser(toAuthUser(response));
  }

  // Accounts are only created from an invitation link; this also signs the new user in.
  async function acceptInvite(payload: AcceptInvitePayload) {
    const response = await acceptInvitation(payload);
    setUser(toAuthUser(response));
  }

  async function logout() {
    try {
      await logoutUser();
    } finally {
      setUser(null);
    }
  }

  return (
    <AuthContext.Provider value={{ user, isLoading, login, acceptInvite, logout }}>
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
