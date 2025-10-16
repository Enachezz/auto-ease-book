import { useAuth } from '@/hooks/useAuth';
import { Button } from '@/components/ui/button';
import { useNavigate } from 'react-router-dom';
import { Car, LogOut, User, Wrench, List, Menu, X } from 'lucide-react';
import { Sheet, SheetContent, SheetTrigger } from '@/components/ui/sheet';
import { useState } from 'react';
import autoeaseLogo from '@/assets/autoease-logo.png';

interface LayoutProps {
  children: React.ReactNode;
}

export function Layout({ children }: LayoutProps) {
  const { profile, signOut } = useAuth();
  const navigate = useNavigate();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const handleSignOut = async () => {
    await signOut();
    navigate('/auth');
  };

  const handleNavigation = (path: string) => {
    navigate(path);
    setMobileMenuOpen(false);
  };

  return (
    <div className="min-h-screen bg-background">
      <header className="border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="container flex h-16 items-center justify-between px-4">
          {/* Logo */}
          <div 
            className="flex items-center gap-2 cursor-pointer" 
            onClick={() => navigate('/')}
          >
            <img src={autoeaseLogo} alt="AutoEase" className="h-8 md:h-10" />
          </div>

          {/* Desktop Navigation */}
          {profile && (
            <nav className="hidden lg:flex items-center gap-4">
              {profile.user_type === 'car_owner' && (
                <>
                  <Button 
                    variant="ghost" 
                    size="sm"
                    onClick={() => navigate('/request-service')}
                  >
                    Solicită Service
                  </Button>
                  <Button 
                    variant="ghost" 
                    size="sm"
                    onClick={() => navigate('/my-requests')}
                  >
                    <List className="h-4 w-4 mr-2" />
                    Solicitările Mele
                  </Button>
                  <Button 
                    variant="ghost" 
                    size="sm"
                    onClick={() => navigate('/my-cars')}
                  >
                    <Car className="h-4 w-4 mr-2" />
                    Mașinile Mele
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
                  Gestionare Service
                </Button>
              )}
            </nav>
          )}

          {/* Desktop User Info & Sign Out */}
          {profile && (
            <div className="hidden md:flex items-center gap-2 lg:gap-4">
              <div className="hidden lg:flex items-center gap-2">
                <User className="h-4 w-4" />
                <span className="text-sm font-medium">{profile.full_name}</span>
                <span className="hidden xl:block text-xs text-muted-foreground capitalize">
                  ({profile.user_type.replace('_', ' ') === 'car owner' ? 'proprietar auto' : profile.user_type.replace('_', ' ') === 'garage' ? 'service auto' : 'admin'})
                </span>
              </div>
              <Button variant="outline" size="sm" onClick={handleSignOut}>
                <LogOut className="h-4 w-4 md:mr-2" />
                <span className="hidden md:inline">Deconectare</span>
              </Button>
            </div>
          )}

          {/* Mobile Menu */}
          {profile && (
            <div className="flex md:hidden items-center gap-2">
              <Sheet open={mobileMenuOpen} onOpenChange={setMobileMenuOpen}>
                <SheetTrigger asChild>
                  <Button variant="ghost" size="sm">
                    <Menu className="h-5 w-5" />
                  </Button>
                </SheetTrigger>
                <SheetContent side="right" className="w-80">
                  <div className="flex flex-col space-y-4 mt-8">
                    {/* User Info */}
                    <div className="flex items-center gap-3 pb-4 border-b">
                      <User className="h-5 w-5" />
                      <div>
                        <span className="text-sm font-medium block">{profile.full_name}</span>
                        <span className="text-xs text-muted-foreground capitalize">
                          {profile.user_type.replace('_', ' ') === 'car owner' ? 'proprietar auto' : profile.user_type.replace('_', ' ') === 'garage' ? 'service auto' : 'admin'}
                        </span>
                      </div>
                    </div>

                    {/* Navigation Links */}
                    {profile.user_type === 'car_owner' && (
                      <>
                        <Button 
                          variant="ghost" 
                          className="justify-start"
                          onClick={() => handleNavigation('/request-service')}
                        >
                          Solicită Service
                        </Button>
                        <Button 
                          variant="ghost" 
                          className="justify-start"
                          onClick={() => handleNavigation('/my-requests')}
                        >
                          <List className="h-4 w-4 mr-2" />
                          Solicitările Mele
                        </Button>
                        <Button 
                          variant="ghost" 
                          className="justify-start"
                          onClick={() => handleNavigation('/my-cars')}
                        >
                          <Car className="h-4 w-4 mr-2" />
                          Mașinile Mele
                        </Button>
                      </>
                    )}
                    {profile.user_type === 'garage' && (
                      <Button 
                        variant="ghost" 
                        className="justify-start"
                        onClick={() => handleNavigation('/garage')}
                      >
                        <Wrench className="h-4 w-4 mr-2" />
                        Gestionare Service
                      </Button>
                    )}

                    {/* Sign Out */}
                    <Button 
                      variant="outline" 
                      className="justify-start mt-auto" 
                      onClick={handleSignOut}
                    >
                      <LogOut className="h-4 w-4 mr-2" />
                      Deconectare
                    </Button>
                  </div>
                </SheetContent>
              </Sheet>
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