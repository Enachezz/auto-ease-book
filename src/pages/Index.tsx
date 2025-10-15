import { useAuth } from '@/hooks/useAuth';
import { Layout } from '@/components/Layout';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { useNavigate } from 'react-router-dom';
import { 
  Car, 
  Wrench, 
  CheckCircle
} from 'lucide-react';

const Index = () => {
  const { user, profile, loading } = useAuth();
  const navigate = useNavigate();

  // Show loading state while auth is being checked
  if (loading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto mb-4"></div>
          <p className="text-muted-foreground">Se încarcă...</p>
        </div>
      </div>
    );
  }

  // Homepage for everyone (authenticated and non-authenticated users)
  return (
    <Layout>
      {/* Hero Section */}
      <div className="relative bg-gradient-to-br from-primary/5 to-blue-50 dark:from-primary/10 dark:to-slate-900 rounded-2xl overflow-hidden mb-16">
        <div className="absolute inset-0 opacity-20" style={{
          backgroundImage: `url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23000' fill-opacity='0.05'%3E%3Ccircle cx='7' cy='7' r='2'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E")`
        }}></div>
        <div className="relative px-8 py-16 lg:py-24">
          <div className="max-w-4xl mx-auto text-center">
            <div className="inline-flex items-center gap-2 bg-primary/10 text-primary px-4 py-2 rounded-full text-sm font-medium mb-6">
              <span>🚗</span>
              Alătură-te miilor de clienți satisfăcuți
            </div>
            <h1 className="text-4xl lg:text-6xl font-bold text-foreground mb-6 leading-tight">
              Găsește service-uri locale pe care te poți{' '}
              <span className="text-primary">baza</span>
            </h1>
            <p className="text-xl text-muted-foreground mb-8 max-w-2xl mx-auto">
              Te ajutăm să economisești bani la reparațiile auto în doar câteva click-uri. 
              Conectează-te cu mecanici de încredere și obține oferte competitive.
            </p>
            
            <div className="flex flex-col sm:flex-row gap-4 justify-center mb-12">
              {!user ? (
                <>
                  <Button 
                    size="lg" 
                    className="text-lg px-8 py-6 h-auto"
                    onClick={() => navigate('/auth')}
                  >
                    Începe Gratuit
                  </Button>
                  <Button 
                    variant="outline" 
                    size="lg" 
                    className="text-lg px-8 py-6 h-auto"
                    onClick={() => navigate('/auth')}
                  >
                    Pentru Service-uri
                  </Button>
                </>
              ) : profile?.user_type === 'car_owner' ? (
                <>
                  <Button 
                    size="lg" 
                    className="text-lg px-8 py-6 h-auto"
                    onClick={() => navigate('/request-service')}
                  >
                    Solicită Service
                  </Button>
                  <Button 
                    variant="outline" 
                    size="lg" 
                    className="text-lg px-8 py-6 h-auto"
                    onClick={() => navigate('/my-requests')}
                  >
                    Solicitările Mele
                  </Button>
                </>
              ) : (
                <>
                  <Button 
                    size="lg" 
                    className="text-lg px-8 py-6 h-auto"
                    onClick={() => navigate('/garage')}
                  >
                    Gestionează Service-ul
                  </Button>
                  <Button 
                    variant="outline" 
                    size="lg" 
                    className="text-lg px-8 py-6 h-auto"
                    onClick={() => navigate('/job-requests')}
                  >
                    Caută Lucrări
                  </Button>
                </>
              )}
            </div>

            {/* Trust indicators */}
            <div className="flex flex-wrap items-center justify-center gap-6 text-sm text-muted-foreground">
              <div className="flex items-center gap-2">
                <div className="flex text-yellow-400">
                  ★★★★★
                </div>
                <span>4.8/5 evaluare</span>
              </div>
              <div className="h-4 w-px bg-border hidden sm:block"></div>
              <span>10,000+ clienți mulțumiți</span>
              <div className="h-4 w-px bg-border hidden sm:block"></div>
              <span>500+ service-uri de încredere</span>
            </div>
          </div>
        </div>
      </div>

      {/* How it works */}
      <div className="mb-16">
        <div className="text-center mb-12">
          <h2 className="text-3xl font-bold mb-4">Cum Funcționează eCAR</h2>
          <p className="text-lg text-muted-foreground">Pași simpli pentru a-ți repara mașina</p>
        </div>
        
        <div className="grid md:grid-cols-3 gap-8">
          <div className="text-center">
            <div className="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-4">
              <Car className="h-8 w-8 text-primary" />
            </div>
            <h3 className="text-xl font-semibold mb-2">1. Descrie Problema</h3>
            <p className="text-muted-foreground">Spune-ne ce nu merge la mașina ta și unde te afli</p>
          </div>
          
          <div className="text-center">
            <div className="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-4">
              <Wrench className="h-8 w-8 text-primary" />
            </div>
            <h3 className="text-xl font-semibold mb-2">2. Primește Oferte</h3>
            <p className="text-muted-foreground">Primește oferte competitive de la service-uri locale de încredere</p>
          </div>
          
          <div className="text-center">
            <div className="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-4">
              <CheckCircle className="h-8 w-8 text-primary" />
            </div>
            <h3 className="text-xl font-semibold mb-2">3. Rezervă & Repară</h3>
            <p className="text-muted-foreground">Alege cea mai bună ofertă și rezervă programarea pentru reparație</p>
          </div>
        </div>
      </div>

      {/* Testimonials */}
      <div className="mb-16">
        <div className="text-center mb-12">
          <h2 className="text-3xl font-bold mb-4">Ce spun șoferii despre service-urile noastre</h2>
          <p className="text-lg text-muted-foreground">Credem că sunt grozave. Dar nu lua doar cuvântul nostru...</p>
        </div>
        
        <div className="grid md:grid-cols-3 gap-6">
          <Card className="p-6">
            <div className="flex text-yellow-400 mb-4">★★★★★</div>
            <p className="text-sm text-muted-foreground mb-4">
              "Service uimitor! Service-ul a fost foarte profesionist și mi-a reparat BMW-ul rapid. 
              Comunicare excelentă pe tot parcursul procesului."
            </p>
            <div className="text-sm font-medium">Proprietar BMW Seria 5</div>
          </Card>
          
          <Card className="p-6">
            <div className="flex text-yellow-400 mb-4">★★★★★</div>
            <p className="text-sm text-muted-foreground mb-4">
              "Service foarte bun, m-au ținut informat cu poze și mesaje. 
              Merită banii și prețuri foarte competitive."
            </p>
            <div className="text-sm font-medium">Proprietar Vauxhall Corsa</div>
          </Card>
          
          <Card className="p-6">
            <div className="flex text-yellow-400 mb-4">★★★★★</div>
            <p className="text-sm text-muted-foreground mb-4">
              "Recomand cu încredere! Service excelent, m-au programat foarte repede. 
              Charlie a fost foarte prietenos și m-a ajutat să înțeleg problema."
            </p>
            <div className="text-sm font-medium">Proprietar Peugeot 2008</div>
          </Card>
        </div>
      </div>

      {/* CTA Section */}
      <div className="bg-primary text-primary-foreground rounded-2xl p-8 text-center">
        <h2 className="text-3xl font-bold mb-4">Gata să începi?</h2>
        <p className="text-lg mb-6 opacity-90">
          Alătură-te miilor de șoferi care au încredere în eCAR pentru reparațiile auto
        </p>
        <div className="flex flex-col sm:flex-row gap-4 justify-center">
          {!user ? (
            <Button 
              variant="secondary" 
              size="lg"
              onClick={() => navigate('/auth')}
            >
              Înregistrează-te Acum
            </Button>
          ) : profile?.user_type === 'car_owner' ? (
            <Button 
              variant="secondary" 
              size="lg"
              onClick={() => navigate('/request-service')}
            >
              Solicită Service
            </Button>
          ) : (
            <Button 
              variant="secondary" 
              size="lg"
              onClick={() => navigate('/garage')}
            >
              Înscrie-te ca Service
            </Button>
          )}
        </div>
      </div>
    </Layout>
  );
};

export default Index;