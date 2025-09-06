import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Layout } from '@/components/Layout';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { ArrowLeft, Search, Wrench, Disc, Settings, Zap, Circle, Truck, Cog, Wind, Hammer as HammerIcon, Shield, Key, Move, Gauge, Eye, Hammer as ToolIcon, HelpCircle, Stethoscope, Battery, SearchIcon } from 'lucide-react';

const RequestService = () => {
  const navigate = useNavigate();
  const [searchTerm, setSearchTerm] = useState('');

  const repairCategories = {
    popular: [
      { id: 1, name: 'Întreținere și ITP', icon: Wrench, description: 'Service-uri regulate și inspecții tehnice' },
      { id: 2, name: 'Reparații Ambreiaj', icon: Disc, description: 'Înlocuire și reparație ambreiaj' },
      { id: 3, name: 'Reparații Frâne', icon: Circle, description: 'Plăcuțe, discuri și sistem de frânare' },
      { id: 4, name: 'Mecanici Mobili și Servicii', icon: Truck, description: 'Servicii la domiciliu' }
    ],
    other: [
      { id: 5, name: 'Motor și Răcire', icon: Settings, description: 'Reparații motor, radiator, pompa de apă' },
      { id: 6, name: 'Aer Condiționat, Încălzire și Răcire', icon: Wind, description: 'Sisteme HVAC și climatizare' },
      { id: 7, name: 'Caroserie, Lovituri și Reparații Smart', icon: HammerIcon, description: 'Reparații caroserie și vopsitorie' },
      { id: 8, name: 'Depanare și Recuperare', icon: ToolIcon, description: 'Servicii de depanare și tractare' },
      { id: 9, name: 'Componente Electrice și Baterii', icon: Battery, description: 'Sistem electric și baterie' },
      { id: 10, name: 'Vehicule Hibride și Electrice', icon: Zap, description: 'Servicii pentru vehicule eco' },
      { id: 11, name: 'Interior, Audio și Navigație', icon: SearchIcon, description: 'Sisteme multimedia și interior' },
      { id: 12, name: 'Componente de Siguranță', icon: Shield, description: 'Airbag-uri, centuri de siguranță' },
      { id: 13, name: 'Siguranță, Închidere și Chei', icon: Key, description: 'Sisteme de blocare și chei' },
      { id: 14, name: 'Direcție și Suspensie', icon: Move, description: 'Sistem de direcție și amortizoare' },
      { id: 15, name: 'Anvelope, Jante și Geometrie', icon: Circle, description: 'Anvelope și aliniere roți' },
      { id: 16, name: 'Geamuri și Parbriz', icon: Eye, description: 'Înlocuire și reparare geamuri' },
      { id: 17, name: 'Reparații Cutie de Viteze', icon: Cog, description: 'Transmisie manuală și automată' },
      { id: 18, name: 'Reparații Eșapament', icon: Gauge, description: 'Sistemul de evacuare' }
    ],
    notSure: [
      { id: 19, name: 'Nu știu sigur - Ajută-mă să aleg!', icon: HelpCircle, description: 'Ghid pentru alegerea serviciului potrivit' },
      { id: 20, name: 'Diagnosticare', icon: Stethoscope, description: 'Testare computerizată și identificare probleme' }
    ]
  };

  const allCategories = [...repairCategories.popular, ...repairCategories.other, ...repairCategories.notSure];
  
  const filteredCategories = searchTerm 
    ? allCategories.filter(category => 
        category.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        category.description.toLowerCase().includes(searchTerm.toLowerCase())
      )
    : allCategories;

  const handleCategorySelect = (categoryName: string) => {
    // For now, just show a placeholder - future implementation would continue to details form
    console.log('Selected category:', categoryName);
  };

  return (
    <Layout>
      <div className="max-w-4xl mx-auto space-y-6 p-4 md:p-0">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center gap-4">
          <Button variant="outline" size="sm" onClick={() => navigate('/')}>
            <ArrowLeft className="h-4 w-4 mr-2" />
            Înapoi
          </Button>
          <div>
            <h1 className="text-2xl md:text-3xl font-bold">Solicită Service</h1>
            <p className="text-sm md:text-base text-muted-foreground">Alege tipul de service de care ai nevoie</p>
          </div>
        </div>

        {/* Progress Steps */}
        <div className="flex items-center justify-center space-x-2 md:space-x-4 mb-8">
          <div className="flex items-center space-x-2 text-primary">
            <div className="w-6 h-6 md:w-8 md:h-8 rounded-full flex items-center justify-center bg-primary text-primary-foreground text-sm md:text-base">
              1
            </div>
            <span className="font-medium text-sm md:text-base">Lucrarea Ta</span>
          </div>
          <div className="w-10 md:w-20 h-px bg-border"></div>
          <div className="flex items-center space-x-2 text-muted-foreground">
            <div className="w-6 h-6 md:w-8 md:h-8 rounded-full flex items-center justify-center bg-muted text-sm md:text-base">
              2
            </div>
            <span className="font-medium text-sm md:text-base">Detalii</span>
          </div>
        </div>

        {/* Search Bar */}
        <div className="relative mb-8">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground h-4 w-4" />
          <Input
            placeholder="Caută tipul de reparație"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="pl-10 h-12 text-base md:text-lg"
          />
        </div>

        {!searchTerm ? (
          <>
            {/* Most Popular Section */}
            <div className="mb-8">
              <h2 className="text-lg md:text-xl font-semibold mb-4">Cele mai populare</h2>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3 md:gap-4">
                {repairCategories.popular.map((category) => (
                  <Card 
                    key={category.id} 
                    className="cursor-pointer hover:shadow-md transition-shadow border-l-4 border-l-primary/20 hover:border-l-primary" 
                    onClick={() => handleCategorySelect(category.name)}
                  >
                    <CardContent className="flex items-center p-4 md:p-6">
                      <div className="flex items-center justify-center w-10 h-10 md:w-12 md:h-12 bg-primary/10 rounded-lg mr-3 md:mr-4 flex-shrink-0">
                        <category.icon className="h-5 w-5 md:h-6 md:w-6 text-primary" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <h3 className="font-medium text-sm md:text-lg leading-tight">{category.name}</h3>
                        <p className="text-xs md:text-sm text-muted-foreground mt-1">{category.description}</p>
                      </div>
                      <ArrowLeft className="h-4 w-4 md:h-5 md:w-5 rotate-180 text-muted-foreground flex-shrink-0 ml-2" />
                    </CardContent>
                  </Card>
                ))}
              </div>
            </div>

            {/* Other Section */}
            <div className="mb-8">
              <h2 className="text-lg md:text-xl font-semibold mb-4">Altele</h2>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3 md:gap-4">
                {repairCategories.other.map((category) => (
                  <Card 
                    key={category.id} 
                    className="cursor-pointer hover:shadow-md transition-shadow" 
                    onClick={() => handleCategorySelect(category.name)}
                  >
                    <CardContent className="flex items-center p-4 md:p-6">
                      <div className="flex items-center justify-center w-10 h-10 md:w-12 md:h-12 bg-primary/10 rounded-lg mr-3 md:mr-4 flex-shrink-0">
                        <category.icon className="h-5 w-5 md:h-6 md:w-6 text-primary" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <h3 className="font-medium text-sm md:text-lg leading-tight">{category.name}</h3>
                        <p className="text-xs md:text-sm text-muted-foreground mt-1">{category.description}</p>
                      </div>
                      <ArrowLeft className="h-4 w-4 md:h-5 md:w-5 rotate-180 text-muted-foreground flex-shrink-0 ml-2" />
                    </CardContent>
                  </Card>
                ))}
              </div>
            </div>

            {/* Not Sure Section */}
            <div>
              <h2 className="text-lg md:text-xl font-semibold mb-4">Nu ești sigur?</h2>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3 md:gap-4">
                {repairCategories.notSure.map((category) => (
                  <Card 
                    key={category.id} 
                    className="cursor-pointer hover:shadow-md transition-shadow border-l-4 border-l-orange-200 hover:border-l-orange-400" 
                    onClick={() => handleCategorySelect(category.name)}
                  >
                    <CardContent className="flex items-center p-4 md:p-6">
                      <div className="flex items-center justify-center w-10 h-10 md:w-12 md:w-12 bg-orange-50 rounded-lg mr-3 md:mr-4 flex-shrink-0">
                        <category.icon className="h-5 w-5 md:h-6 md:w-6 text-orange-600" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <h3 className="font-medium text-sm md:text-lg leading-tight">{category.name}</h3>
                        <p className="text-xs md:text-sm text-muted-foreground mt-1">{category.description}</p>
                      </div>
                      <ArrowLeft className="h-4 w-4 md:h-5 md:w-5 rotate-180 text-muted-foreground flex-shrink-0 ml-2" />
                    </CardContent>
                  </Card>
                ))}
              </div>
            </div>
          </>
        ) : (
          /* Search Results */
          <div>
            <h2 className="text-lg md:text-xl font-semibold mb-4">
              Rezultate pentru "{searchTerm}" ({filteredCategories.length})
            </h2>
            {filteredCategories.length === 0 ? (
              <div className="text-center py-12">
                <SearchIcon className="h-12 w-12 mx-auto mb-4 text-muted-foreground" />
                <p className="text-muted-foreground">Nu au fost găsite rezultate pentru "{searchTerm}"</p>
                <p className="text-sm text-muted-foreground mt-2">Încearcă cu alte cuvinte cheie</p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3 md:gap-4">
                {filteredCategories.map((category) => (
                  <Card 
                    key={category.id} 
                    className="cursor-pointer hover:shadow-md transition-shadow" 
                    onClick={() => handleCategorySelect(category.name)}
                  >
                    <CardContent className="flex items-center p-4 md:p-6">
                      <div className="flex items-center justify-center w-10 h-10 md:w-12 md:h-12 bg-primary/10 rounded-lg mr-3 md:mr-4 flex-shrink-0">
                        <category.icon className="h-5 w-5 md:h-6 md:w-6 text-primary" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <h3 className="font-medium text-sm md:text-lg leading-tight">{category.name}</h3>
                        <p className="text-xs md:text-sm text-muted-foreground mt-1">{category.description}</p>
                      </div>
                      <ArrowLeft className="h-4 w-4 md:h-5 md:w-5 rotate-180 text-muted-foreground flex-shrink-0 ml-2" />
                    </CardContent>
                  </Card>
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </Layout>
  );
};

export default RequestService;
