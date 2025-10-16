import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Layout } from '@/components/Layout';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Label } from '@/components/ui/label';
import { ArrowLeft, Car, Calendar as CalendarIcon, MapPin } from 'lucide-react';
import { Calendar } from '@/components/ui/calendar';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { format } from 'date-fns';
import { cn } from '@/lib/utils';

const ServiceDetails = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const selectedService = searchParams.get('service') || 'Service Auto';
  
  const [formData, setFormData] = useState({
    description: '',
    preferredDate: undefined as Date | undefined,
    location: '',
    urgency: 'normal'
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    // Here you would typically save the service request and navigate to confirmation
    console.log('Service request submitted:', { service: selectedService, ...formData });
    // For now, navigate back to home with success message
    navigate('/', { state: { message: 'Cererea ta de service a fost trimisă cu succes!' } });
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
                <div className="flex justify-center border rounded-lg p-2">
                  <Calendar
                    mode="single"
                    selected={formData.preferredDate}
                    onSelect={(date) => setFormData({ ...formData, preferredDate: date })}
                    disabled={(date) => date < new Date(new Date().setHours(0, 0, 0, 0))}
                    initialFocus
                    className={cn("pointer-events-auto")}
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
                    { value: 'normal', label: 'Normal', color: 'bg-blue-100 text-blue-800 border-blue-300' },
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