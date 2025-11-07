import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Layout } from '@/components/Layout';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Label } from '@/components/ui/label';
import { ArrowLeft, Car, MapPin } from 'lucide-react';
import { Calendar } from '@/components/ui/calendar';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
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

const EditRequest = () => {
  const navigate = useNavigate();
  const { jobId } = useParams();
  const { toast } = useToast();
  const { user } = useAuth();
  
  const [cars, setCars] = useState<UserCar[]>([]);
  const [loading, setLoading] = useState(true);
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    preferredDate: undefined as Date | undefined,
    location: '',
    urgency: 'medium',
    carId: ''
  });

  useEffect(() => {
    if (user && jobId) {
      Promise.all([fetchUserCars(), fetchJobRequest()]);
    }
  }, [user, jobId]);

  const fetchUserCars = async () => {
    try {
      if (!user) return;

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
    }
  };

  const fetchJobRequest = async () => {
    try {
      setLoading(true);
      const { data, error } = await supabase
        .from('job_requests')
        .select('*')
        .eq('id', jobId)
        .eq('user_id', user?.id)
        .single();

      if (error) throw error;

      if (data) {
        setFormData({
          title: data.title || '',
          description: data.description || '',
          preferredDate: data.preferred_date ? new Date(data.preferred_date) : undefined,
          location: data.location_address || '',
          urgency: data.urgency || 'medium',
          carId: data.car_id || ''
        });
      }
    } catch (error) {
      console.error('Error fetching job request:', error);
      toast({
        title: "Eroare",
        description: "Nu s-a putut încărca cererea.",
        variant: "destructive",
      });
      navigate('/my-requests');
    } finally {
      setLoading(false);
    }
  };

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
      const { error } = await supabase
        .from('job_requests')
        .update({
          car_id: formData.carId,
          title: formData.title,
          description: formData.description,
          preferred_date: formData.preferredDate?.toISOString().split('T')[0],
          urgency: formData.urgency,
          location_address: formData.location,
        })
        .eq('id', jobId)
        .eq('user_id', user?.id);

      if (error) throw error;

      toast({
        title: "Success",
        description: "Cererea ta a fost actualizată cu succes!",
      });
      
      navigate('/my-requests');
    } catch (error) {
      console.error('Error updating service request:', error);
      toast({
        title: "Eroare",
        description: "Nu s-a putut actualiza cererea. Te rugăm să încerci din nou.",
        variant: "destructive",
      });
    }
  };

  if (loading) {
    return (
      <Layout>
        <div className="max-w-2xl mx-auto space-y-6 p-4">
          <div className="text-center">Se încarcă...</div>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="max-w-2xl mx-auto space-y-6 p-4 md:p-0">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center gap-4">
          <Button variant="outline" size="sm" onClick={() => navigate('/my-requests')}>
            <ArrowLeft className="h-4 w-4 mr-2" />
            Înapoi
          </Button>
          <div>
            <h1 className="text-2xl md:text-3xl font-bold">Modifică Cererea</h1>
            <p className="text-sm md:text-base text-muted-foreground">Actualizează detaliile cererii tale</p>
          </div>
        </div>

        {/* Edit Form */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Car className="h-5 w-5" />
              {formData.title}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-6">
              {/* Car Selection */}
              <div className="space-y-2">
                <Label htmlFor="car">Selectează mașina *</Label>
                {cars.length === 0 ? (
                  <div className="text-sm text-muted-foreground">Nu ai nicio mașină adăugată.</div>
                ) : (
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
                  onClick={() => navigate('/my-requests')}
                  className="flex-1"
                >
                  Anulează
                </Button>
                <Button type="submit" className="flex-1">
                  Salvează Modificările
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
      </div>
    </Layout>
  );
};

export default EditRequest;
