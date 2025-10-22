import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Layout } from '@/components/Layout';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Label } from '@/components/ui/label';
import { ArrowLeft, Car, Calendar as CalendarIcon, MapPin, AlertCircle, Plus, LogIn } from 'lucide-react';
import { Calendar } from '@/components/ui/calendar';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { format } from 'date-fns';
import { cn } from '@/lib/utils';
import { supabase } from '@/integrations/supabase/client';
import { useToast } from '@/hooks/use-toast';
import { useAuth } from '@/hooks/useAuth';

interface UserCar {
  id: string;
  make_id: string;
  model_id: string;
  year: number;
  vin: string | null;
  license_plate: string | null;
  car_makes?: { name: string };
  car_models?: { name: string };
}

const ServiceDetails = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const selectedService = searchParams.get('service') || 'Service Auto';
  const { toast } = useToast();
  const { user } = useAuth();
  
  const [cars, setCars] = useState<UserCar[]>([]);
  const [loadingCars, setLoadingCars] = useState(true);
  const [formData, setFormData] = useState({
    description: '',
    preferredDate: undefined as Date | undefined,
    location: '',
    urgency: 'medium',
    carId: ''
  });

  useEffect(() => {
    fetchUserCars();
  }, []);

  const fetchUserCars = async () => {
    try {
      if (!user) {
        setLoadingCars(false);
        return;
      }

      const { data, error } = await supabase
        .from('cars')
        .select(`
          *,
          car_makes(name),
          car_models(name)
        `)
        .eq('user_id', user.id);

      if (error) throw error;
      setCars(data || []);
    } catch (error) {
      console.error('Error fetching cars:', error);
      toast({
        title: "Eroare",
        description: "Nu s-au putut încărca mașinile tale.",
        variant: "destructive",
      });
    } finally {
      setLoadingCars(false);
    }
  };

  const selectedCar = cars.find(car => car.id === formData.carId);
  const showVinWarning = selectedCar && !selectedCar.vin;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!formData.carId) {
      toast({
        title: "Eroare",
        description: "Te rugăm să selectezi o mașină.",
        variant: "destructive",
      });
      return;
    }

    try {
      const { data: { user } } = await supabase.auth.getUser();
      if (!user) {
        toast({
          title: "Eroare",
          description: "Trebuie să fii autentificat.",
          variant: "destructive",
        });
        return;
      }

      // Get or create service category
      let categoryId = null;
      const { data: categories } = await supabase
        .from('service_categories')
        .select('id')
        .limit(1)
        .single();
      
      categoryId = categories?.id;

      if (!categoryId) {
        toast({
          title: "Eroare",
          description: "Nu s-a putut găsi categoria de service.",
          variant: "destructive",
        });
        return;
      }

      // Insert job request
      const { error } = await supabase
        .from('job_requests')
        .insert({
          user_id: user.id,
          car_id: formData.carId,
          category_id: categoryId,
          title: selectedService,
          description: formData.description,
          preferred_date: formData.preferredDate?.toISOString().split('T')[0],
          urgency: formData.urgency,
          location_address: formData.location,
          status: 'open'
        });

      if (error) throw error;

      toast({
        title: "Success",
        description: "Cererea ta de service a fost trimisă cu succes!",
      });
      
      navigate('/job-requests');
    } catch (error) {
      console.error('Error submitting service request:', error);
      toast({
        title: "Eroare",
        description: "Nu s-a putut trimite cererea. Te rugăm să încerci din nou.",
        variant: "destructive",
      });
    }
  };

  return (
    <Layout>
      <div className="max-w-2xl mx-auto space-y-6 p-4 md:p-0">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center gap-4">
          <Button variant="outline" size="sm" onClick={() => navigate('/request-service')}>
            <ArrowLeft className="h-4 w-4 mr-2" />
            Înapoi
          </Button>
          <div>
            <h1 className="text-2xl md:text-3xl font-bold">Detalii Service</h1>
            <p className="text-sm md:text-base text-muted-foreground">Completează detaliile pentru: {selectedService}</p>
          </div>
        </div>

        {/* Progress Steps */}
        <div className="flex items-center justify-center space-x-2 md:space-x-4 mb-8">
          <div className="flex items-center space-x-2 text-muted-foreground">
            <div className="w-6 h-6 md:w-8 md:h-8 rounded-full flex items-center justify-center bg-primary text-primary-foreground text-sm md:text-base">
              ✓
            </div>
            <span className="font-medium text-sm md:text-base">Lucrarea Ta</span>
          </div>
          <div className="w-10 md:w-20 h-px bg-border"></div>
          <div className="flex items-center space-x-2 text-primary">
            <div className="w-6 h-6 md:w-8 md:h-8 rounded-full flex items-center justify-center bg-primary text-primary-foreground text-sm md:text-base">
              2
            </div>
            <span className="font-medium text-sm md:text-base">Detalii</span>
          </div>
        </div>

        {/* Service Details Form */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Car className="h-5 w-5" />
              {selectedService}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-6">
              {/* Car Selection */}
              <div className="space-y-2">
                <Label htmlFor="car">Selectează mașina *</Label>
                {loadingCars ? (
                  <div className="text-sm text-muted-foreground">Se încarcă mașinile...</div>
                ) : !user ? (
                  <div className="space-y-3">
                    <Alert>
                      <AlertCircle className="h-4 w-4" />
                      <AlertDescription>
                        Pentru a trimite o cerere de service, trebuie să ai un cont AutoFix.
                      </AlertDescription>
                    </Alert>
                    <Button
                      type="button"
                      onClick={() => navigate('/auth')}
                      className="w-full"
                    >
                      <LogIn className="h-4 w-4 mr-2" />
                      Conectează-te sau Creează Cont
                    </Button>
                  </div>
                ) : cars.length === 0 ? (
                  <div className="space-y-3">
                    <Alert>
                      <AlertCircle className="h-4 w-4" />
                      <AlertDescription>
                        Nu ai nicio mașină adăugată. Adaugă o mașină pentru a putea solicita service.
                      </AlertDescription>
                    </Alert>
                    <Button
                      type="button"
                      onClick={() => navigate('/my-cars')}
                      className="w-full"
                    >
                      <Plus className="h-4 w-4 mr-2" />
                      Adaugă Mașină
                    </Button>
                  </div>
                ) : (
                  <>
                    <Select
                      value={formData.carId}
                      onValueChange={(value) => setFormData({ ...formData, carId: value })}
                      required
                    >
                      <SelectTrigger>
                        <SelectValue placeholder="Alege mașina" />
                      </SelectTrigger>
                      <SelectContent>
                        {cars.map((car) => (
                          <SelectItem key={car.id} value={car.id}>
                            {car.car_makes?.name} {car.car_models?.name} ({car.year})
                            {car.license_plate && ` - ${car.license_plate}`}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    
                    {showVinWarning && (
                      <Alert className="mt-3">
                        <AlertCircle className="h-4 w-4" />
                        <AlertDescription className="space-y-2">
                          <p className="text-sm">
                            Mașina selectată nu are numărul de șasiu (VIN) adăugat. 
                            Adăugarea VIN-ului ajută mecanicul să găsească piesele mai rapid.
                          </p>
                          <p className="text-xs text-muted-foreground">
                            Nu-ți face griji! Numărul de șasiu va ajunge la mecanic doar după confirmarea lucrării.
                          </p>
                          <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={() => navigate('/my-cars', { state: { editCarId: formData.carId } })}
                            className="mt-2"
                          >
                            Adaugă VIN acum
                          </Button>
                        </AlertDescription>
                      </Alert>
                    )}
                  </>
                )}
              </div>

              {/* Description */}
              <div className="space-y-2">
                <Label htmlFor="description">Descrierea problemei *</Label>
                <Textarea
                  id="description"
                  placeholder="Descrie detaliat problema sau serviciul de care ai nevoie..."
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  required
                  rows={4}
                />
              </div>

              {/* Preferred Date */}
              <div className="space-y-2">
                <Label>Data preferată</Label>
                <div className="border rounded-lg p-4">
                  <Calendar
                    mode="single"
                    selected={formData.preferredDate}
                    onSelect={(date) => setFormData({ ...formData, preferredDate: date })}
                    disabled={(date) => date < new Date(new Date().setHours(0, 0, 0, 0))}
                    initialFocus
                    className={cn("w-full pointer-events-auto")}
                  />
                </div>
              </div>

              {/* Location */}
              <div className="space-y-2">
                <Label htmlFor="location">Locația (oraș, județ)</Label>
                <div className="relative">
                  <MapPin className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground h-4 w-4" />
                  <Input
                    id="location"
                    placeholder="Ex: București, Sector 1"
                    value={formData.location}
                    onChange={(e) => setFormData({ ...formData, location: e.target.value })}
                    className="pl-10"
                  />
                </div>
              </div>

              {/* Urgency */}
              <div className="space-y-2">
                <Label>Urgența</Label>
                <div className="flex gap-4">
                  {[
                    { value: 'low', label: 'Nu e urgent', color: 'bg-green-100 text-green-800 border-green-300' },
                    { value: 'medium', label: 'Normal', color: 'bg-blue-100 text-blue-800 border-blue-300' },
                    { value: 'high', label: 'Urgent', color: 'bg-red-100 text-red-800 border-red-300' }
                  ].map((urgency) => (
                    <button
                      key={urgency.value}
                      type="button"
                      onClick={() => setFormData({ ...formData, urgency: urgency.value })}
                      className={`px-4 py-2 rounded-lg border-2 transition-all ${
                        formData.urgency === urgency.value
                          ? urgency.color
                          : 'bg-background border-border hover:border-primary/50'
                      }`}
                    >
                      {urgency.label}
                    </button>
                  ))}
                </div>
              </div>

              {/* Submit Button */}
              <div className="flex gap-4 pt-4">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => navigate('/request-service')}
                  className="flex-1"
                >
                  Înapoi
                </Button>
                <Button type="submit" className="flex-1">
                  Trimite Cererea
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      </div>
    </Layout>
  );
};

export default ServiceDetails;