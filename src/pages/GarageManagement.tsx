import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Layout } from '@/components/Layout';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { useAuth } from '@/hooks/useAuth';
import { supabase } from '@/integrations/supabase/client';
import { useToast } from '@/hooks/use-toast';
import { ArrowLeft, Wrench, MapPin, Clock, DollarSign, Calendar, Car } from 'lucide-react';

interface Garage {
  id: string;
  business_name: string;
  address: string;
  city: string;
  state: string;
  phone: string;
  is_approved: boolean;
  services: string[];
}

interface JobRequest {
  id: string;
  title: string;
  description: string;
  urgency: string;
  budget_min?: number;
  budget_max?: number;
  preferred_date?: string;
  location_city?: string;
  location_state?: string;
  created_at: string;
}

interface Quote {
  id: string;
  job_request_id: string;
  price: number;
  description?: string;
  estimated_duration?: string;
  status: string;
  created_at: string;
}

export default function GarageManagement() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const { toast } = useToast();
  const [loading, setLoading] = useState(false);
  const [garage, setGarage] = useState<Garage | null>(null);
  const [jobRequests, setJobRequests] = useState<JobRequest[]>([]);
  const [quotes, setQuotes] = useState<Quote[]>([]);
  const [showGarageForm, setShowGarageForm] = useState(false);

  const [garageForm, setGarageForm] = useState({
    business_name: '',
    address: '',
    city: '',
    state: '',
    postal_code: '',
    phone: '',
    description: '',
    services: [] as string[]
  });

  const [quoteForm, setQuoteForm] = useState({
    job_request_id: '',
    price: '',
    description: '',
    estimated_duration: '',
    warranty_info: ''
  });

  const serviceOptions = [
    'Oil Change', 'Brake Service', 'Tire Service', 'Engine Repair', 
    'Transmission', 'Electrical', 'AC/Heating', 'Battery', 
    'Suspension', 'Exhaust', 'Inspection', 'Diagnostics'
  ];

  useEffect(() => {
    fetchGarageData();
  }, []);

  const fetchGarageData = async () => {
    try {
      // Fetch garage profile
      const { data: garageData } = await supabase
        .from('garages')
        .select('*')
        .eq('user_id', user?.id)
        .maybeSingle();

      if (garageData) {
        setGarage(garageData);
        setGarageForm({
          business_name: garageData.business_name,
          address: garageData.address,
          city: garageData.city,
          state: garageData.state,
          postal_code: garageData.postal_code,
          phone: garageData.phone,
          description: garageData.description || '',
          services: garageData.services || []
        });

        // Fetch available job requests
        const { data: jobsData } = await supabase
          .from('job_requests')
          .select('*')
          .eq('status', 'open')
          .order('created_at', { ascending: false });

        if (jobsData) setJobRequests(jobsData);

        // Fetch garage's quotes
        const { data: quotesData } = await supabase
          .from('quotes')
          .select('*')
          .eq('garage_id', garageData.id)
          .order('created_at', { ascending: false });

        if (quotesData) setQuotes(quotesData);
      } else {
        setShowGarageForm(true);
      }
    } catch (error) {
      console.error('Error fetching garage data:', error);
    }
  };

  const handleGarageSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      const garageData = {
        ...garageForm,
        user_id: user?.id
      };

      if (garage) {
        // Update existing garage
        const { error } = await supabase
          .from('garages')
          .update(garageData)
          .eq('id', garage.id);

        if (error) throw error;
        toast({ title: "Success", description: "Garage updated successfully!" });
      } else {
        // Create new garage
        const { data, error } = await supabase
          .from('garages')
          .insert([garageData])
          .select()
          .single();

        if (error) throw error;
        setGarage(data);
        setShowGarageForm(false);
        toast({ title: "Success", description: "Garage profile created! Awaiting admin approval." });
      }
    } catch (error: any) {
      toast({
        title: "Error",
        description: error.message,
        variant: "destructive"
      });
    } finally {
      setLoading(false);
    }
  };

  const handleQuoteSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      const { error } = await supabase
        .from('quotes')
        .insert([{
          ...quoteForm,
          garage_id: garage?.id,
          price: parseFloat(quoteForm.price)
        }]);

      if (error) throw error;

      toast({ title: "Success", description: "Quote submitted successfully!" });
      setQuoteForm({
        job_request_id: '',
        price: '',
        description: '',
        estimated_duration: '',
        warranty_info: ''
      });
      fetchGarageData(); // Refresh data
    } catch (error: any) {
      toast({
        title: "Error",
        description: error.message,
        variant: "destructive"
      });
    } finally {
      setLoading(false);
    }
  };

  const toggleService = (service: string) => {
    const updatedServices = garageForm.services.includes(service)
      ? garageForm.services.filter(s => s !== service)
      : [...garageForm.services, service];
    setGarageForm({ ...garageForm, services: updatedServices });
  };

  if (!garage && !showGarageForm) {
    return (
      <Layout>
        <div className="max-w-2xl mx-auto text-center">
          <Wrench className="h-16 w-16 mx-auto mb-4 text-muted-foreground" />
          <h2 className="text-2xl font-bold mb-4">Set up your garage profile</h2>
          <p className="text-muted-foreground mb-6">
            Create your garage profile to start receiving job requests and submitting quotes.
          </p>
          <Button onClick={() => setShowGarageForm(true)}>
            Create Garage Profile
          </Button>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center gap-4">
          <Button variant="outline" size="sm" onClick={() => navigate('/')} className="self-start sm:self-auto">
            <ArrowLeft className="h-4 w-4 mr-2" />
            Înapoi
          </Button>
          <div>
            <h1 className="text-2xl md:text-3xl font-bold">Gestionare Service</h1>
            <p className="text-sm md:text-base text-muted-foreground">Gestionează service-ul și cererile de lucru</p>
          </div>
        </div>

        {garage && !garage.is_approved && (
          <Card className="border-yellow-200 bg-yellow-50">
            <CardContent className="p-4">
              <div className="flex items-center gap-2">
                <Clock className="h-4 w-4 text-yellow-600" />
                <span className="text-yellow-800 font-medium">Pending Approval</span>
              </div>
              <p className="text-sm text-yellow-700 mt-1">
                Your garage profile is awaiting admin approval. You can still submit quotes once approved.
              </p>
            </CardContent>
          </Card>
        )}

        <Tabs defaultValue="profile" className="space-y-4 md:space-y-6">
          <TabsList className="grid w-full grid-cols-3">
            <TabsTrigger value="profile" className="text-xs md:text-sm">Profil</TabsTrigger>
            <TabsTrigger value="jobs" className="text-xs md:text-sm">Locuri de muncă</TabsTrigger>
            <TabsTrigger value="quotes" className="text-xs md:text-sm">Ofertele mele</TabsTrigger>
          </TabsList>

          <TabsContent value="profile">
            <Card>
              <CardHeader>
                <CardTitle>Garage Profile</CardTitle>
              </CardHeader>
              <CardContent>
                <form onSubmit={handleGarageSubmit} className="space-y-4">
                  <div>
                    <Label htmlFor="business_name">Business Name</Label>
                    <Input
                      value={garageForm.business_name}
                      onChange={(e) => setGarageForm({ ...garageForm, business_name: e.target.value })}
                      required
                    />
                  </div>

                  <div>
                    <Label htmlFor="description">Description</Label>
                    <Textarea
                      value={garageForm.description}
                      onChange={(e) => setGarageForm({ ...garageForm, description: e.target.value })}
                      placeholder="Tell customers about your garage..."
                    />
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <Label htmlFor="phone">Phone</Label>
                      <Input
                        value={garageForm.phone}
                        onChange={(e) => setGarageForm({ ...garageForm, phone: e.target.value })}
                        required
                      />
                    </div>
                    <div>
                      <Label htmlFor="postal_code">Postal Code</Label>
                      <Input
                        value={garageForm.postal_code}
                        onChange={(e) => setGarageForm({ ...garageForm, postal_code: e.target.value })}
                        required
                      />
                    </div>
                  </div>

                  <div>
                    <Label htmlFor="address">Address</Label>
                    <Input
                      value={garageForm.address}
                      onChange={(e) => setGarageForm({ ...garageForm, address: e.target.value })}
                      required
                    />
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <Label htmlFor="city">City</Label>
                      <Input
                        value={garageForm.city}
                        onChange={(e) => setGarageForm({ ...garageForm, city: e.target.value })}
                        required
                      />
                    </div>
                    <div>
                      <Label htmlFor="state">State</Label>
                      <Input
                        value={garageForm.state}
                        onChange={(e) => setGarageForm({ ...garageForm, state: e.target.value })}
                        required
                      />
                    </div>
                  </div>

                  <div>
                    <Label>Services Offered</Label>
                    <div className="grid grid-cols-3 gap-2 mt-2">
                      {serviceOptions.map((service) => (
                        <Button
                          key={service}
                          type="button"
                          variant={garageForm.services.includes(service) ? "default" : "outline"}
                          size="sm"
                          onClick={() => toggleService(service)}
                        >
                          {service}
                        </Button>
                      ))}
                    </div>
                  </div>

                  <Button type="submit" disabled={loading}>
                    {loading ? 'Saving...' : garage ? 'Update Profile' : 'Create Profile'}
                  </Button>
                </form>
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="jobs">
            <div className="space-y-4">
              {jobRequests.map((job) => (
                <Card key={job.id}>
                  <CardContent className="p-6">
                    <div className="flex justify-between items-start mb-4">
                      <div>
                        <h3 className="text-lg font-medium">{job.title}</h3>
                        <p className="text-sm text-muted-foreground flex items-center mt-1">
                          <MapPin className="h-3 w-3 mr-1" />
                          {job.location_city}, {job.location_state}
                        </p>
                      </div>
                      <div className="text-right">
                        <Badge variant={job.urgency === 'high' ? 'destructive' : job.urgency === 'medium' ? 'default' : 'secondary'}>
                          {job.urgency} priority
                        </Badge>
                        {job.budget_min && job.budget_max && (
                          <p className="text-sm text-muted-foreground mt-1">
                            Budget: ${job.budget_min} - ${job.budget_max}
                          </p>
                        )}
                      </div>
                    </div>
                    
                    <p className="text-sm mb-4">{job.description}</p>
                    
                    {job.preferred_date && (
                      <p className="text-sm text-muted-foreground mb-4 flex items-center">
                        <Calendar className="h-3 w-3 mr-1" />
                        Preferred date: {new Date(job.preferred_date).toLocaleDateString()}
                      </p>
                    )}

                    <form onSubmit={handleQuoteSubmit} className="space-y-3 bg-muted p-4 rounded-lg">
                      <h4 className="font-medium">Submit Quote</h4>
                      <div className="grid grid-cols-2 gap-3">
                        <div>
                          <Label htmlFor="price">Price ($)</Label>
                          <Input
                            type="number"
                            step="0.01"
                            value={quoteForm.job_request_id === job.id ? quoteForm.price : ''}
                            onChange={(e) => setQuoteForm({ ...quoteForm, job_request_id: job.id, price: e.target.value })}
                            required
                          />
                        </div>
                        <div>
                          <Label htmlFor="estimated_duration">Duration</Label>
                          <Input
                            value={quoteForm.job_request_id === job.id ? quoteForm.estimated_duration : ''}
                            onChange={(e) => setQuoteForm({ ...quoteForm, job_request_id: job.id, estimated_duration: e.target.value })}
                            placeholder="e.g. 2 hours"
                          />
                        </div>
                      </div>
                      <div>
                        <Label htmlFor="description">Details</Label>
                        <Textarea
                          value={quoteForm.job_request_id === job.id ? quoteForm.description : ''}
                          onChange={(e) => setQuoteForm({ ...quoteForm, job_request_id: job.id, description: e.target.value })}
                          placeholder="Describe what you'll do, parts needed, etc."
                          rows={2}
                        />
                      </div>
                      <Button 
                        type="submit" 
                        size="sm" 
                        disabled={loading || !garage?.is_approved || quoteForm.job_request_id !== job.id || !quoteForm.price}
                      >
                        {loading ? 'Submitting...' : 'Submit Quote'}
                      </Button>
                    </form>
                  </CardContent>
                </Card>
              ))}

              {jobRequests.length === 0 && (
                <Card>
                  <CardContent className="p-6 text-center">
                    <Car className="h-12 w-12 mx-auto mb-4 text-muted-foreground" />
                    <h3 className="text-lg font-medium mb-2">No job requests available</h3>
                    <p className="text-muted-foreground">Check back later for new service requests</p>
                  </CardContent>
                </Card>
              )}
            </div>
          </TabsContent>

          <TabsContent value="quotes">
            <div className="space-y-4">
              {quotes.map((quote) => (
                <Card key={quote.id}>
                  <CardContent className="p-6">
                    <div className="flex justify-between items-start">
                      <div>
                        <h3 className="text-lg font-medium">Quote #{quote.id.slice(0, 8)}</h3>
                        <p className="text-sm text-muted-foreground">
                          Submitted {new Date(quote.created_at).toLocaleDateString()}
                        </p>
                      </div>
                      <div className="text-right">
                        <p className="text-lg font-bold">${quote.price}</p>
                        <Badge variant={quote.status === 'accepted' ? 'default' : quote.status === 'rejected' ? 'destructive' : 'secondary'}>
                          {quote.status}
                        </Badge>
                      </div>
                    </div>
                    {quote.description && (
                      <p className="text-sm mt-2">{quote.description}</p>
                    )}
                    {quote.estimated_duration && (
                      <p className="text-sm text-muted-foreground mt-1">
                        Estimated duration: {quote.estimated_duration}
                      </p>
                    )}
                  </CardContent>
                </Card>
              ))}

              {quotes.length === 0 && (
                <Card>
                  <CardContent className="p-6 text-center">
                    <DollarSign className="h-12 w-12 mx-auto mb-4 text-muted-foreground" />
                    <h3 className="text-lg font-medium mb-2">No quotes yet</h3>
                    <p className="text-muted-foreground">Submit quotes for job requests to see them here</p>
                  </CardContent>
                </Card>
              )}
            </div>
          </TabsContent>
        </Tabs>
      </div>
    </Layout>
  );
}