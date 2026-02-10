import React, { createContext, useContext, useState, useCallback, useEffect } from 'react';
import axios from 'axios';

interface User {
  id: number;
  username: string;
  role: string;
}

interface AuthState {
  user: User | null;
  token: string | null;
  ready: boolean;
}

interface AuthContextValue extends AuthState {
  login: (username: string, password: string) => Promise<void>;
  register: (username: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

const TOKEN_KEY = 'summarizer_token';
const USER_KEY = 'summarizer_user';

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<AuthState>({
    user: null,
    token: null,
    ready: false,
  });

  const setAuth = useCallback((user: User | null, token: string | null) => {
    if (token) {
      localStorage.setItem(TOKEN_KEY, token);
      if (user) localStorage.setItem(USER_KEY, JSON.stringify(user));
    } else {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
    }
    setState({ user, token, ready: true });
  }, []);

  useEffect(() => {
    const token = localStorage.getItem(TOKEN_KEY);
    const userStr = localStorage.getItem(USER_KEY);
    if (token && userStr) {
      try {
        const user = JSON.parse(userStr) as User;
        setState({ user, token, ready: true });
      } catch {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USER_KEY);
        setState({ user: null, token: null, ready: true });
      }
    } else {
      setState((s) => ({ ...s, ready: true }));
    }
  }, []);

  useEffect(() => {
    const interceptor = axios.interceptors.request.use((config) => {
      const token = localStorage.getItem(TOKEN_KEY);
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    });
    return () => axios.interceptors.request.eject(interceptor);
  }, []);

  useEffect(() => {
    const interceptor = axios.interceptors.response.use(
      (res) => res,
      (err) => {
        if (err?.response?.status === 401) {
          localStorage.removeItem(TOKEN_KEY);
          localStorage.removeItem(USER_KEY);
          setState({ user: null, token: null, ready: true });
          window.location.href = '/login';
        }
        return Promise.reject(err);
      }
    );
    return () => axios.interceptors.response.eject(interceptor);
  }, []);

  const login = useCallback(
    async (username: string, password: string) => {
      const { data } = await axios.post<{ token: string; username: string; role: string; id: number }>(
        '/api/auth/login',
        { username, password }
      );
      const user: User = { id: data.id, username: data.username, role: data.role };
      setAuth(user, data.token);
    },
    [setAuth]
  );

  const register = useCallback(
    async (username: string, password: string) => {
      const { data } = await axios.post<{ token: string; username: string; role: string; id: number }>(
        '/api/auth/register',
        { username, password }
      );
      const user: User = { id: data.id, username: data.username, role: data.role };
      setAuth(user, data.token);
    },
    [setAuth]
  );

  const logout = useCallback(() => {
    setAuth(null, null);
  }, [setAuth]);

  const value: AuthContextValue = {
    ...state,
    login,
    register,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
