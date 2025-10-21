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
import heroBackground from '@/assets/hero-background.jpg';
import autofixLogo from '@/assets/autofix-logo.png';

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
      <div className="relative rounded-2xl overflow-hidden mb-16">
        <div 
          className="absolute inset-0 bg-cover bg-center"
          style={{
            backgroundImage: `url(${heroBackground})`
          }}
        ></div>
        <div className="absolute inset-0 bg-black/40"></div>
        <div className="relative px-8 py-16 lg:py-24">
          <div className="max-w-4xl mx-auto text-center">
            {!user && (
              <div 
                className="inline-flex items-center gap-2 bg-white/20 text-white backdrop-blur-sm px-4 py-2 rounded-full text-sm font-medium mb-6 cursor-pointer hover:bg-white/30 transition-colors"
                onClick={() => navigate('/auth')}
              >
                <span>🚗</span>
                Alătură-te miilor de clienți satisfăcuți
              </div>
            )}
            <h1 className="text-4xl lg:text-6xl font-bold text-white mb-6 leading-tight">
              Găsește service-uri locale pe care te poți{' '}
              <span className="text-white drop-shadow-lg">baza</span>
            </h1>
            <p className="text-xl text-white mb-8 max-w-2xl mx-auto drop-shadow-md">
              Te ajutăm să economisești bani la reparațiile auto în doar câteva click-uri. 
              Conectează-te cu mecanici de încredere și obține oferte competitive.
            </p>
            
            <div className="flex flex-col sm:flex-row gap-4 justify-center mb-12">
              {!user ? (
                <>
                  <Button 
                    size="lg" 
                    className="text-lg px-8 py-6 h-auto"
                    onClick={() => navigate('/request-service')}
                  >
                    Vezi Servicii
                  </Button>
                  <Button 
                    variant="outline" 
                    size="lg" 
                    className="text-lg px-8 py-6 h-auto"
                    onClick={() => navigate('/auth')}
                  >
                    Începe Gratuit
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
            <div className="flex flex-wrap items-center justify-center gap-6 text-sm text-white/90">
              <div className="flex items-center gap-2">
                <div className="flex text-yellow-400">
                  ★★★★★
                </div>
                <span>4.8/5 evaluare</span>
              </div>
              <div className="h-4 w-px bg-white/30 hidden sm:block"></div>
              <span>10,000+ clienți mulțumiți</span>
              <div className="h-4 w-px bg-white/30 hidden sm:block"></div>
              <span>500+ service-uri de încredere</span>
            </div>
          </div>
        </div>
      </div>

      {/* How it works */}
      <div className="mb-16">
        <div className="text-center mb-12">
          <h2 className="text-3xl font-bold mb-4">Cum Funcționează AutoFix</h2>
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
      <div className="bg-primary text-primary-foreground rounded-2xl p-8 text-center mb-16">
        <h2 className="text-3xl font-bold mb-4">Gata să începi?</h2>
        <p className="text-lg mb-6 opacity-90">
          Alătură-te miilor de șoferi care au încredere în AutoFix pentru reparațiile auto
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

      {/* Garage CTA Section */}
      <div className="bg-accent text-accent-foreground rounded-2xl p-8 mb-16">
        <div className="max-w-4xl mx-auto flex flex-col lg:flex-row items-center justify-between gap-6">
          <div>
            <h2 className="text-2xl lg:text-3xl font-bold mb-3">Ai un service auto?</h2>
            <p className="text-lg mb-2">
              Fie că ai un service auto sau ești mecanic mobil, începe astăzi și obține acces instant la mii de clienți potențiali.
            </p>
          </div>
          <div className="flex flex-col items-center gap-2">
            <Button 
              size="lg"
              onClick={() => navigate('/auth')}
              className="whitespace-nowrap"
            >
              Începe Astăzi
            </Button>
            <span className="text-sm opacity-80">Gratuit - fără plată necesară</span>
          </div>
        </div>
      </div>

      {/* Footer */}
      <footer className="border-t pt-12 pb-8">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8 mb-8">
          {/* Company */}
          <div>
            <h3 className="font-semibold mb-4 text-sm uppercase tracking-wider text-muted-foreground">Companie</h3>
            <ul className="space-y-3">
              <li>
                <a href="#" className="text-sm hover:text-primary transition-colors">Ajutor</a>
              </li>
              <li>
                <a href="#" className="text-sm hover:text-primary transition-colors">Ghiduri și Noutăți</a>
              </li>
              <li>
                <a href="#" className="text-sm hover:text-primary transition-colors">Contact</a>
              </li>
              <li>
                <a href="#" className="text-sm hover:text-primary transition-colors">Despre Noi</a>
              </li>
              <li>
                <a href="#" className="text-sm hover:text-primary transition-colors">Recenzii</a>
              </li>
              <li>
                <a href="#" className="text-sm hover:text-primary transition-colors">Cariere</a>
              </li>
            </ul>
          </div>

          {/* For Garages */}
          <div>
            <h3 className="font-semibold mb-4 text-sm uppercase tracking-wider text-muted-foreground">Pentru Service-uri</h3>
            <ul className="space-y-3">
              <li>
                <a href="#" className="text-sm hover:text-primary transition-colors">Pentru Service-uri</a>
              </li>
              <li>
                <a href="#" className="text-sm hover:text-primary transition-colors">Evaluare Service</a>
              </li>
              <li>
                <button onClick={() => navigate('/auth')} className="text-sm hover:text-primary transition-colors text-left">Conectare</button>
              </li>
            </ul>
          </div>

          {/* Legal */}
          <div>
            <h3 className="font-semibold mb-4 text-sm uppercase tracking-wider text-muted-foreground">Legal</h3>
            <ul className="space-y-3">
              <li>
                <a href="#" className="text-sm hover:text-primary transition-colors">Politica de Confidențialitate</a>
              </li>
              <li>
                <a href="#" className="text-sm hover:text-primary transition-colors">Termeni și Condiții</a>
              </li>
              <li>
                <a href="#" className="text-sm hover:text-primary transition-colors">Cod de Conduită</a>
              </li>
              <li>
                <a href="#" className="text-sm hover:text-primary transition-colors">Politica Cookie-uri</a>
              </li>
            </ul>
          </div>

          {/* Social */}
          <div>
            <h3 className="font-semibold mb-4 text-sm uppercase tracking-wider text-muted-foreground">Social</h3>
            <ul className="space-y-3">
              <li>
                <a href="#" className="text-sm hover:text-primary transition-colors">Facebook</a>
              </li>
              <li>
                <a href="#" className="text-sm hover:text-primary transition-colors">Instagram</a>
              </li>
              <li>
                <a href="#" className="text-sm hover:text-primary transition-colors">LinkedIn</a>
              </li>
              <li>
                <a href="#" className="text-sm hover:text-primary transition-colors">TikTok</a>
              </li>
              <li>
                <a href="#" className="text-sm hover:text-primary transition-colors">YouTube</a>
              </li>
            </ul>
          </div>
        </div>

        {/* Bottom Footer */}
        <div className="pt-8 border-t flex flex-col sm:flex-row justify-between items-center gap-4">
          <div className="flex items-center gap-2">
            <img src={autofixLogo} alt="AutoFix" className="h-8 brightness-0 dark:brightness-100" />
            <div className="text-sm text-muted-foreground">
              <p className="font-medium">AutoFix</p>
              <p>Economisește mai mult. Stres mai puțin.</p>
              <p>Viitorul reparațiilor auto începe astăzi.</p>
            </div>
          </div>
          <div className="flex gap-3">
            <Button 
              variant="outline" 
              size="sm"
              onClick={() => navigate('/auth')}
            >
              Conectare
            </Button>
            <Button 
              size="sm"
              onClick={() => navigate('/auth')}
            >
              Înregistrare
            </Button>
          </div>
        </div>
      </footer>
    </Layout>
  );
};

export default Index;