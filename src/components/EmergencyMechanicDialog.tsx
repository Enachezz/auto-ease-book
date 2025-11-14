import { useState, useEffect } from 'react';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { AlertCircle, MapPin, Clock, DollarSign, Navigation } from 'lucide-react';
import { supabase } from '@/integrations/supabase/client';
import { useToast } from '@/hooks/use-toast';

interface EmergencyMechanic {
  id: string;
  business_name: string;
  latitude: number;
  longitude: number;
  distance: number;
  price: number;
  user_id: string;
}

interface EmergencyMechanicDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export const EmergencyMechanicDialog = ({ open, onOpenChange }: EmergencyMechanicDialogProps) => {
  const { toast } = useToast();
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

  useEffect(() => {
    if (!selectedMechanic) return;

    // Subscribe to mechanic location updates
    const channel = supabase
      .channel('mechanic-location-updates')
      .on(
        'postgres_changes',
        {
          event: 'UPDATE',
          schema: 'public',
          table: 'garages',
          filter: `id=eq.${selectedMechanic.id}`
        },
        (payload) => {
          const updated = payload.new as any;
          setSelectedMechanic(prev => prev ? {
            ...prev,
            latitude: updated.latitude,
            longitude: updated.longitude,
            distance: calculateDistance(
              userLocation!.lat,
              userLocation!.lng,
              updated.latitude,
              updated.longitude
            )
          } : null);
        }
      )
      .subscribe();

    return () => {
      supabase.removeChannel(channel);
    };
  }, [selectedMechanic, userLocation]);

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

  const calculateDistance = (lat1: number, lon1: number, lat2: number, lon2: number): number => {
    const R = 6371; // Earth's radius in km
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a = 
      Math.sin(dLat/2) * Math.sin(dLat/2) +
      Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
      Math.sin(dLon/2) * Math.sin(dLon/2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    return R * c;
  };

  const fetchNearbyMechanics = async () => {
    if (!userLocation) return;

    try {
      const { data, error } = await supabase
        .from('garages')
        .select('*')
        .eq('is_approved', true)
        .not('latitude', 'is', null)
        .not('longitude', 'is', null);

      if (error) throw error;

      const mechanicsWithDistance = data.map(garage => ({
        id: garage.id,
        business_name: garage.business_name,
        latitude: garage.latitude,
        longitude: garage.longitude,
        user_id: garage.user_id,
        distance: calculateDistance(
          userLocation.lat,
          userLocation.lng,
          garage.latitude,
          garage.longitude
        ),
        price: 50 + Math.floor(calculateDistance(
          userLocation.lat,
          userLocation.lng,
          garage.latitude,
          garage.longitude
        ) * 5)
      }))
      .sort((a, b) => a.distance - b.distance)
      .slice(0, 5);

      setMechanics(mechanicsWithDistance);
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
      const { data: { user } } = await supabase.auth.getUser();
      if (!user) {
        toast({
          title: "Eroare",
          description: "Trebuie să fii autentificat pentru a solicita ajutor.",
          variant: "destructive",
        });
        return;
      }

      // Here you would create an emergency request in the database
      // For now, we'll just show the mechanic as selected
      setSelectedMechanic(mechanic);
      
      toast({
        title: "Succes",
        description: `Mecanic solicitat! ${mechanic.business_name} se îndreaptă spre tine.`,
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
              <div className="flex items-center gap-2 mb-4">
                <div className="animate-pulse h-3 w-3 bg-primary rounded-full"></div>
                <span className="font-semibold text-primary">Mecanic în drum spre tine</span>
              </div>
              
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
              <p className="mb-2">ℹ️ Locația mecanicului se actualizează automat în timp real.</p>
              <p>Vei fi contactat telefonic de către mecanic pentru detalii suplimentare.</p>
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
