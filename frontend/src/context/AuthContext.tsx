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
    // Hydrate authentication state from localStorage on mount
    try {
      const storedToken = localStorage.getItem('fitos_access_token');
      const storedUser = localStorage.getItem('fitos_user');

      if (storedToken === 'demo-jwt-token-fitos-offline') {
        localStorage.removeItem('fitos_access_token');
        localStorage.removeItem('fitos_refresh_token');
        localStorage.removeItem('fitos_user');
        document.cookie = 'fitos_token=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT';
      } else if (storedToken && storedUser) {
        setToken(storedToken);
        setUser(JSON.parse(storedUser));
        document.cookie = `fitos_token=${storedToken}; path=/; max-age=604800; SameSite=Lax`;
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

      localStorage.setItem('fitos_access_token', accessToken);
      if (refreshToken) {
        localStorage.setItem('fitos_refresh_token', refreshToken);
      }
      localStorage.setItem('fitos_user', JSON.stringify(authUser));
      document.cookie = `fitos_token=${accessToken}; path=/; max-age=604800; SameSite=Lax`;

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

      localStorage.setItem('fitos_access_token', accessToken);
      if (refreshToken) {
        localStorage.setItem('fitos_refresh_token', refreshToken);
      }
      localStorage.setItem('fitos_user', JSON.stringify(authUser));
      document.cookie = `fitos_token=${accessToken}; path=/; max-age=604800; SameSite=Lax`;

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
      await api.post('/auth/logout', { refreshToken: localStorage.getItem('fitos_refresh_token') });
    } catch {
      // Continue client cleanup even if backend is offline
    } finally {
      localStorage.removeItem('fitos_access_token');
      localStorage.removeItem('fitos_refresh_token');
      localStorage.removeItem('fitos_user');
      document.cookie = 'fitos_token=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT';
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
