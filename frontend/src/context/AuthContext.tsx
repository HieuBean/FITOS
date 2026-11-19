'use client';

import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import api from '@/lib/api';
import { useRouter } from 'next/navigation';

export interface User {
  id: number;
  email: string;
  fullName: string;
  role: 'ADMIN' | 'MANAGER' | 'RECEPTIONIST' | 'TRAINER' | 'MEMBER' | string;
  studioId?: number;
  phone?: string;
}

export interface RegisterPayload {
  email: string;
  password: string;
  fullName: string;
  phone?: string;
  studioId?: number;
}

function normalizeAuthUser(data: any): User {
  const rawUser = data?.user ?? data;
  const roles: string[] = rawUser?.roles ?? data?.roles ?? [];
  const rawRole = rawUser?.role ?? ['ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_RECEPTIONIST',
    'ROLE_TRAINER', 'ROLE_MEMBER'].find((candidate) => roles.includes(candidate)) ?? roles[0] ?? 'MEMBER';
  const role = String(rawRole).replace(/^ROLE_/, '').toUpperCase();
  return {
    id: Number(rawUser?.id ?? data?.userId),
    email: rawUser?.email ?? data?.email,
    fullName: rawUser?.fullName ?? rawUser?.name ?? data?.fullName ?? '',
    role,
    studioId: rawUser?.studioId ?? data?.studioId,
    phone: rawUser?.phone ?? data?.phone,
  };
}

export interface AuthContextType {
  user: User | null;
  token: string | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  login: (email: string, password: string, studioId?: number) => Promise<User>;
  register: (payload: RegisterPayload) => Promise<User>;
  logout: () => Promise<void>;
  setUser: React.Dispatch<React.SetStateAction<User | null>>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const router = useRouter();

  useEffect(() => {
    // Each tab keeps its own account, including after a reload.
    try {
      const storedToken = sessionStorage.getItem('fitos_access_token');
      const storedUser = sessionStorage.getItem('fitos_user');

      // Remove credentials left by the older shared-session implementation.
      localStorage.removeItem('fitos_access_token');
      localStorage.removeItem('fitos_refresh_token');
      localStorage.removeItem('fitos_user');
      localStorage.removeItem('fitos_token');
      document.cookie = 'fitos_token=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT';

      if (storedToken === 'demo-jwt-token-fitos-offline') {
        sessionStorage.removeItem('fitos_access_token');
        sessionStorage.removeItem('fitos_refresh_token');
        sessionStorage.removeItem('fitos_user');
      } else if (storedToken && storedUser) {
        setToken(storedToken);
        setUser(JSON.parse(storedUser));
      }
    } catch (e) {
      console.error('Failed to parse stored auth session', e);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const login = async (email: string, password: string, studioId?: number): Promise<User> => {
    setIsLoading(true);
    try {
      const res = await api.post('/auth/login', { email, password, studioId });
      const { accessToken, refreshToken } = res.data;
      const authUser = normalizeAuthUser(res.data);

      sessionStorage.setItem('fitos_access_token', accessToken);
      if (refreshToken) {
        sessionStorage.setItem('fitos_refresh_token', refreshToken);
      }
      sessionStorage.setItem('fitos_user', JSON.stringify(authUser));

      setToken(accessToken);
      setUser(authUser);
      return authUser;
    } catch (err: any) {
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const register = async (payload: RegisterPayload): Promise<User> => {
    setIsLoading(true);
    try {
      const res = await api.post('/auth/register', payload);
      const { accessToken, refreshToken } = res.data;
      const authUser = normalizeAuthUser(res.data);

      sessionStorage.setItem('fitos_access_token', accessToken);
      if (refreshToken) {
        sessionStorage.setItem('fitos_refresh_token', refreshToken);
      }
      sessionStorage.setItem('fitos_user', JSON.stringify(authUser));

      setToken(accessToken);
      setUser(authUser);
      return authUser;
    } catch (err: any) {
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const logout = async () => {
    try {
      await api.post('/auth/logout', { refreshToken: sessionStorage.getItem('fitos_refresh_token') });
    } catch {
      // Continue client cleanup even if backend is offline
    } finally {
      sessionStorage.removeItem('fitos_access_token');
      sessionStorage.removeItem('fitos_refresh_token');
      sessionStorage.removeItem('fitos_user');
      setUser(null);
      setToken(null);
      router.push('/login');
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isLoading,
        isAuthenticated: !!user,
        login,
        register,
        logout,
        setUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
