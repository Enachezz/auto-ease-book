import { useAuth } from '@/hooks/useAuth';
import { Button } from '@/components/ui/button';
import { useNavigate } from 'react-router-dom';
import { Car, LogOut, User, Wrench, List } from 'lucide-react';

interface LayoutProps {
  children: React.ReactNode;
}

export function Layout({ children }: LayoutProps) {
  const { profile, signOut } = useAuth();
  const navigate = useNavigate();

  const handleSignOut = async () => {
    await signOut();
    navigate('/auth');
  };

  return (
    <div className="min-h-screen bg-background">
      <header className="border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="container flex h-16 items-center justify-between">
          <div className="flex items-center gap-6">
            <div 
              className="flex items-center gap-2 cursor-pointer" 
              onClick={() => navigate('/')}
            >
              <Car className="h-6 w-6 text-primary" />
              <span className="text-xl font-bold">AutoEase</span>
            </div>
            
            {profile && (
              <nav className="flex items-center gap-4">
                {profile.user_type === 'car_owner' && (
                  <>
                    <Button 
                      variant="ghost" 
                      size="sm"
                      onClick={() => navigate('/request-service')}
                    >
                      Request Service
                    </Button>
                    <Button 
                      variant="ghost" 
                      size="sm"
                      onClick={() => navigate('/my-requests')}
                    >
                      <List className="h-4 w-4 mr-2" />
                      My Requests
                    </Button>
                  </>
                )}
                {profile.user_type === 'garage' && (
                  <Button 
                    variant="ghost" 
                    size="sm"
                    onClick={() => navigate('/garage')}
                  >
                    <Wrench className="h-4 w-4 mr-2" />
                    Garage Management
                  </Button>
                )}
              </nav>
            )}
          </div>

          {profile && (
            <div className="flex items-center gap-4">
              <div className="flex items-center gap-2">
                <User className="h-4 w-4" />
                <span className="text-sm font-medium">{profile.full_name}</span>
                <span className="text-xs text-muted-foreground capitalize">
                  ({profile.user_type.replace('_', ' ')})
                </span>
              </div>
              <Button variant="outline" size="sm" onClick={handleSignOut}>
                <LogOut className="h-4 w-4 mr-2" />
                Sign Out
              </Button>
            </div>
          )}
        </div>
      </header>

      <main className="container py-6">
        {children}
      </main>
    </div>
  );
}