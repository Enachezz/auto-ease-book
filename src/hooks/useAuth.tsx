import { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { api } from '@/lib/api';
import { useToast } from '@/hooks/use-toast';

type UserType = 'car_owner' | 'garage' | 'admin';

interface AuthUser {
  id: string;
  email: string;
}

interface Profile {
  id: string;
  user_id: string;
  user_type: UserType;
  email: string;
  phone?: string;
  full_name: string;
  avatar_url?: string;
}

interface AuthContextType {
  user: AuthUser | null;
  profile: Profile | null;
  loading: boolean;
  signUp: (email: string, password: string, fullName: string, userType: UserType) => Promise<{ error: any }>;
  signIn: (email: string, password: string) => Promise<{ error: any }>;
  signOut: () => Promise<{ error: any }>;
}

interface AuthResponse {
  token: string;
  userId: string;
  email: string;
  fullName: string;
  userType: string;
}

interface ProfileResponse {
  id: string;
  userId: string;
  fullName: string;
  email: string;
  phone?: string;
  avatarUrl?: string;
  userType: string;
}

function mapProfile(data: ProfileResponse): Profile {
  return {
    id: data.id,
    user_id: data.userId,
    user_type: data.userType.toLowerCase() as UserType,
    email: data.email,
    phone: data.phone,
    full_name: data.fullName,
    avatar_url: data.avatarUrl,
  };
}

const USER_TYPE_MAP: Record<string, string> = {
  car_owner: 'CAR_OWNER',
  garage: 'GARAGE',
  admin: 'ADMIN',
};

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [profile, setProfile] = useState<Profile | null>(null);
  const [loading, setLoading] = useState(true);
  const { toast } = useToast();

  const loadProfile = useCallback(async () => {
    try {
      const data = await api.get<ProfileResponse>('/profiles/me');
      setProfile(mapProfile(data));
    } catch {
      localStorage.removeItem('token');
      setUser(null);
      setProfile(null);
    }
  }, []);

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        setUser({ id: payload.sub, email: payload.sub });
        loadProfile().finally(() => setLoading(false));
      } catch {
        localStorage.removeItem('token');
        setLoading(false);
      }
    } else {
      setLoading(false);
    }
  }, [loadProfile]);

  const signUp = async (email: string, password: string, fullName: string, userType: UserType) => {
    try {
      const data = await api.post<AuthResponse>('/auth/register', {
        email,
        password,
        fullName,
        userType: USER_TYPE_MAP[userType] || userType.toUpperCase(),
      });

      localStorage.setItem('token', data.token);
      setUser({ id: data.userId, email: data.email });

      await loadProfile();

      toast({
        title: "Succes",
        description: "Contul a fost creat cu succes!",
      });

      return { error: null };
    } catch (error: any) {
      toast({
        title: "Eroare",
        description: error.message,
        variant: "destructive",
      });
      return { error };
    }
  };

  const signIn = async (email: string, password: string) => {
    try {
      const data = await api.post<AuthResponse>('/auth/login', { email, password });

      localStorage.setItem('token', data.token);
      setUser({ id: data.userId, email: data.email });

      await loadProfile();

      return { error: null };
    } catch (error: any) {
      toast({
        title: "Eroare",
        description: error.message,
        variant: "destructive",
      });
      return { error };
    }
  };

  const signOut = async () => {
    try {
      localStorage.removeItem('token');
      setUser(null);
      setProfile(null);
      return { error: null };
    } catch (error: any) {
      toast({
        title: "Eroare",
        description: error.message,
        variant: "destructive",
      });
      return { error };
    }
  };

  const value = {
    user,
    profile,
    loading,
    signUp,
    signIn,
    signOut,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
