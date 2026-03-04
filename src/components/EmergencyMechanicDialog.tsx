import { useState, useEffect } from 'react';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { AlertCircle, MapPin, Clock, DollarSign, Navigation } from 'lucide-react';
import { api } from '@/lib/api';
import { useAuth } from '@/hooks/useAuth';
import { useToast } from '@/hooks/use-toast';

interface GarageItem {
  id: string;
  businessName: string;
}

interface EmergencyMechanic {
  id: string;
  business_name: string;
  distance: number;
  price: number;
}

interface EmergencyMechanicDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export const EmergencyMechanicDialog = ({ open, onOpenChange }: EmergencyMechanicDialogProps) => {
  const { toast } = useToast();
  const { user } = useAuth();
  const [userLocation, setUserLocation] = useState<{ lat: number; lng: number } | null>(null);
  const [mechanics, setMechanics] = useState<EmergencyMechanic[]>([]);
  const [selectedMechanic, setSelectedMechanic] = useState<EmergencyMechanic | null>(null);
  const [loading, setLoading] = useState(false);
  const [requestingHelp, setRequestingHelp] = useState(false);

  useEffect(() => {
    if (open) {
      getUserLocation();
    }
  }, [open]);

  useEffect(() => {
    if (userLocation) {
      fetchNearbyMechanics();
    }
  }, [userLocation]);

  const getUserLocation = () => {
    setLoading(true);
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          setUserLocation({
            lat: position.coords.latitude,
            lng: position.coords.longitude
          });
          setLoading(false);
        },
        (error) => {
          console.error('Error getting location:', error);
          toast({
            title: "Eroare",
            description: "Nu s-a putut obține locația ta. Te rugăm să activezi locația.",
            variant: "destructive",
          });
          setLoading(false);
        }
      );
    } else {
      toast({
        title: "Eroare",
        description: "Browser-ul tău nu suportă geolocația.",
        variant: "destructive",
      });
      setLoading(false);
    }
  };

  const fetchNearbyMechanics = async () => {
    if (!userLocation) return;

    try {
      const data = await api.get<GarageItem[]>('/garages');
      const mechanicsData = (data || []).map((garage, idx) => ({
        id: garage.id,
        business_name: garage.businessName,
        distance: 2 + idx * 3,
        price: 50 + (2 + idx * 3) * 5
      })).slice(0, 5);

      setMechanics(mechanicsData);
    } catch (error) {
      console.error('Error fetching mechanics:', error);
      toast({
        title: "Eroare",
        description: "Nu s-au putut încărca mecanicii disponibili.",
        variant: "destructive",
      });
    }
  };

  const handleRequestHelp = async (mechanic: EmergencyMechanic) => {
    setRequestingHelp(true);
    try {
      if (!user) {
        toast({
          title: "Eroare",
          description: "Trebuie să fii autentificat pentru a solicita ajutor.",
          variant: "destructive",
        });
        return;
      }

      setSelectedMechanic(mechanic);

      toast({
        title: "Funcție în dezvoltare",
        description: "Solicitarea de urgență va fi disponibilă într-o versiune viitoare.",
      });
    } catch (error) {
      console.error('Error requesting help:', error);
      toast({
        title: "Eroare",
        description: "Nu s-a putut trimite solicitarea.",
        variant: "destructive",
      });
    } finally {
      setRequestingHelp(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2 text-destructive">
            <AlertCircle className="h-6 w-6" />
            Apel de Urgență Mecanic
          </DialogTitle>
          <DialogDescription>
            Solicită un mecanic în locația ta curentă pentru asistență de urgență
          </DialogDescription>
        </DialogHeader>

        {loading && (
          <div className="flex items-center justify-center py-8">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
          </div>
        )}

        {!loading && !userLocation && (
          <div className="text-center py-8">
            <MapPin className="h-12 w-12 mx-auto mb-4 text-muted-foreground" />
            <p className="text-muted-foreground">
              Nu s-a putut obține locația ta. Te rugăm să activezi locația și să încerci din nou.
            </p>
            <Button onClick={getUserLocation} className="mt-4">
              Încearcă din nou
            </Button>
          </div>
        )}

        {!loading && userLocation && !selectedMechanic && (
          <div className="space-y-4">
            <div className="bg-muted/50 p-4 rounded-lg">
              <h3 className="font-semibold mb-2 flex items-center gap-2">
                <MapPin className="h-4 w-4" />
                Locația ta
              </h3>
              <p className="text-sm text-muted-foreground">
                Lat: {userLocation.lat.toFixed(6)}, Lng: {userLocation.lng.toFixed(6)}
              </p>
            </div>

            <div>
              <h3 className="font-semibold mb-3">Mecanici Disponibili</h3>
              {mechanics.length === 0 ? (
                <p className="text-center text-muted-foreground py-8">
                  Nu sunt mecanici disponibili în zona ta momentan.
                </p>
              ) : (
                <div className="space-y-3">
                  {mechanics.map(mechanic => (
                    <div
                      key={mechanic.id}
                      className="border border-border rounded-lg p-4 hover:border-primary transition-colors"
                    >
                      <div className="flex justify-between items-start mb-3">
                        <div>
                          <h4 className="font-semibold">{mechanic.business_name}</h4>
                          <div className="flex items-center gap-4 mt-2 text-sm text-muted-foreground">
                            <span className="flex items-center gap-1">
                              <Navigation className="h-3 w-3" />
                              {mechanic.distance.toFixed(1)} km
                            </span>
                            <span className="flex items-center gap-1">
                              <Clock className="h-3 w-3" />
                              ~{Math.ceil(mechanic.distance * 2)} min
                            </span>
                          </div>
                        </div>
                        <div className="text-right">
                          <div className="flex items-center gap-1 text-lg font-bold text-primary">
                            <DollarSign className="h-5 w-5" />
                            {mechanic.price} RON
                          </div>
                          <p className="text-xs text-muted-foreground">Tarif deplasare</p>
                        </div>
                      </div>
                      <Button
                        onClick={() => handleRequestHelp(mechanic)}
                        disabled={requestingHelp}
                        className="w-full bg-destructive hover:bg-destructive/90 text-destructive-foreground"
                      >
                        {requestingHelp ? 'Se procesează...' : 'Solicită Ajutor'}
                      </Button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}

        {selectedMechanic && (
          <div className="space-y-4">
            <div className="bg-primary/10 border-2 border-primary rounded-lg p-6">
              <h3 className="text-xl font-bold mb-4">{selectedMechanic.business_name}</h3>

              <div className="grid grid-cols-2 gap-4 mb-4">
                <div className="bg-background/50 rounded-lg p-3">
                  <div className="flex items-center gap-2 text-sm text-muted-foreground mb-1">
                    <Navigation className="h-4 w-4" />
                    Distanță
                  </div>
                  <div className="text-2xl font-bold">{selectedMechanic.distance.toFixed(1)} km</div>
                </div>

                <div className="bg-background/50 rounded-lg p-3">
                  <div className="flex items-center gap-2 text-sm text-muted-foreground mb-1">
                    <Clock className="h-4 w-4" />
                    Timp estimat
                  </div>
                  <div className="text-2xl font-bold">~{Math.ceil(selectedMechanic.distance * 2)} min</div>
                </div>
              </div>

              <div className="bg-background/50 rounded-lg p-4">
                <div className="flex justify-between items-center">
                  <span className="text-muted-foreground">Tarif deplasare</span>
                  <span className="text-2xl font-bold text-primary">{selectedMechanic.price} RON</span>
                </div>
              </div>
            </div>

            <div className="bg-muted/50 p-4 rounded-lg text-sm text-muted-foreground">
              <p>Funcționalitatea de urgență completă va fi disponibilă într-o versiune viitoare.</p>
            </div>

            <Button
              onClick={() => {
                setSelectedMechanic(null);
                onOpenChange(false);
              }}
              variant="outline"
              className="w-full"
            >
              Închide
            </Button>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
};
