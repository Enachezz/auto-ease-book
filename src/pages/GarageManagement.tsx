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
import { api } from '@/lib/api';
import { useToast } from '@/hooks/use-toast';
import { ArrowLeft, Wrench, MapPin, Clock, DollarSign, Calendar, Car } from 'lucide-react';

interface Garage {
  id: string;
  businessName: string;
  address: string;
  city: string;
  state: string;
  phone: string;
  postalCode: string;
  description: string;
  isApproved: boolean;
  services: string[];
}

interface JobRequest {
  id: string;
  title: string;
  description: string;
  urgency: string;
  budgetMin?: number;
  budgetMax?: number;
  preferredDate?: string;
  locationCity?: string;
  locationState?: string;
  locationAddress?: string;
}

interface Quote {
  id: string;
  jobRequestId: string;
  price: number;
  description?: string;
  estimatedDuration?: string;
  status: string;
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
    businessName: '',
    address: '',
    city: '',
    state: '',
    postalCode: '',
    phone: '',
    description: '',
    services: [] as string[]
  });

  const [quoteForm, setQuoteForm] = useState({
    jobRequestId: '',
    price: '',
    description: '',
    estimatedDuration: '',
    warrantyInfo: ''
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
      const garageData = await api.get<Garage>('/garages/me');
      setGarage(garageData);
      setGarageForm({
        businessName: garageData.businessName,
        address: garageData.address || '',
        city: garageData.city || '',
        state: garageData.state || '',
        postalCode: garageData.postalCode || '',
        phone: garageData.phone || '',
        description: garageData.description || '',
        services: garageData.services || []
      });

      const jobsData = await api.get<JobRequest[]>('/job-requests/open');
      if (jobsData) setJobRequests(jobsData);

      const quotesData = await api.get<Quote[]>('/quotes/mine');
      if (quotesData) setQuotes(quotesData);
    } catch {
      setShowGarageForm(true);
    }
  };

  const handleGarageSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      if (garage) {
        await api.put('/garages/me', garageForm);
        toast({ title: "Success", description: "Garage updated successfully!" });
      } else {
        const data = await api.post<Garage>('/garages', garageForm);
        setGarage(data);
        setShowGarageForm(false);
        toast({ title: "Success", description: "Garage profile created! Awaiting admin approval." });
      }
      fetchGarageData();
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
      await api.post(`/job-requests/${quoteForm.jobRequestId}/quotes`, {
        price: parseFloat(quoteForm.price),
        description: quoteForm.description || null,
        estimatedDuration: quoteForm.estimatedDuration || null,
        warrantyInfo: quoteForm.warrantyInfo || null,
      });

      toast({ title: "Success", description: "Quote submitted successfully!" });
      setQuoteForm({
        jobRequestId: '',
        price: '',
        description: '',
        estimatedDuration: '',
        warrantyInfo: ''
      });
      fetchGarageData();
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

        {garage && !garage.isApproved && (
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
                      id="business_name"
                      value={garageForm.businessName}
                      onChange={(e) => setGarageForm({ ...garageForm, businessName: e.target.value })}
                      required
                    />
                  </div>

                  <div>
                    <Label htmlFor="description">Description</Label>
                    <Textarea
                      id="description"
                      value={garageForm.description}
                      onChange={(e) => setGarageForm({ ...garageForm, description: e.target.value })}
                      placeholder="Tell customers about your garage..."
                    />
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <Label htmlFor="phone">Phone</Label>
                      <Input
                        id="phone"
                        value={garageForm.phone}
                        onChange={(e) => setGarageForm({ ...garageForm, phone: e.target.value })}
                        required
                      />
                    </div>
                    <div>
                      <Label htmlFor="postal_code">Postal Code</Label>
                      <Input
                        id="postal_code"
                        value={garageForm.postalCode}
                        onChange={(e) => setGarageForm({ ...garageForm, postalCode: e.target.value })}
                        required
                      />
                    </div>
                  </div>

                  <div>
                    <Label htmlFor="address">Address</Label>
                    <Input
                      id="address"
                      value={garageForm.address}
                      onChange={(e) => setGarageForm({ ...garageForm, address: e.target.value })}
                      required
                    />
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <Label htmlFor="city">City</Label>
                      <Input
                        id="city"
                        value={garageForm.city}
                        onChange={(e) => setGarageForm({ ...garageForm, city: e.target.value })}
                        required
                      />
                    </div>
                    <div>
                      <Label htmlFor="state">State</Label>
                      <Input
                        id="state"
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
                        {(job.locationAddress || job.locationCity) && (
                          <p className="text-sm text-muted-foreground flex items-center mt-1">
                            <MapPin className="h-3 w-3 mr-1" />
                            {job.locationAddress || `${job.locationCity}, ${job.locationState}`}
                          </p>
                        )}
                      </div>
                      <div className="text-right">
                        <Badge variant={job.urgency === 'HIGH' || job.urgency === 'EMERGENCY' ? 'destructive' : job.urgency === 'NORMAL' ? 'default' : 'secondary'}>
                          {job.urgency.toLowerCase()} priority
                        </Badge>
                        {job.budgetMin != null && job.budgetMax != null && (
                          <p className="text-sm text-muted-foreground mt-1">
                            Budget: ${job.budgetMin} - ${job.budgetMax}
                          </p>
                        )}
                      </div>
                    </div>
                    
                    <p className="text-sm mb-4">{job.description}</p>
                    
                    {job.preferredDate && (
                      <p className="text-sm text-muted-foreground mb-4 flex items-center">
                        <Calendar className="h-3 w-3 mr-1" />
                        Preferred date: {new Date(job.preferredDate).toLocaleDateString()}
                      </p>
                    )}

                    <form onSubmit={handleQuoteSubmit} className="space-y-3 bg-muted p-4 rounded-lg">
                      <h4 className="font-medium">Submit Quote</h4>
                      <div className="grid grid-cols-2 gap-3">
                        <div>
                          <Label htmlFor={`quote_price_${job.id}`}>Price ($)</Label>
                          <Input
                            id={`quote_price_${job.id}`}
                            type="number"
                            step="0.01"
                            value={quoteForm.jobRequestId === job.id ? quoteForm.price : ''}
                            onChange={(e) => setQuoteForm({ ...quoteForm, jobRequestId: job.id, price: e.target.value })}
                            required
                          />
                        </div>
                        <div>
                          <Label htmlFor={`quote_duration_${job.id}`}>Duration</Label>
                          <Input
                            id={`quote_duration_${job.id}`}
                            value={quoteForm.jobRequestId === job.id ? quoteForm.estimatedDuration : ''}
                            onChange={(e) => setQuoteForm({ ...quoteForm, jobRequestId: job.id, estimatedDuration: e.target.value })}
                            placeholder="e.g. 2 hours"
                          />
                        </div>
                      </div>
                      <div>
                        <Label htmlFor={`quote_details_${job.id}`}>Details</Label>
                        <Textarea
                          id={`quote_details_${job.id}`}
                          value={quoteForm.jobRequestId === job.id ? quoteForm.description : ''}
                          onChange={(e) => setQuoteForm({ ...quoteForm, jobRequestId: job.id, description: e.target.value })}
                          placeholder="Describe what you'll do, parts needed, etc."
                          rows={2}
                        />
                      </div>
                      <Button 
                        type="submit" 
                        size="sm" 
                        disabled={loading || !garage?.isApproved || quoteForm.jobRequestId !== job.id || !quoteForm.price}
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
                        <h3 className="text-lg font-medium">Quote #{String(quote.id).slice(0, 8)}</h3>
                      </div>
                      <div className="text-right">
                        <p className="text-lg font-bold">${quote.price}</p>
                        <Badge variant={quote.status === 'ACCEPTED' ? 'default' : quote.status === 'REJECTED' ? 'destructive' : 'secondary'}>
                          {quote.status}
                        </Badge>
                      </div>
                    </div>
                    {quote.description && (
                      <p className="text-sm mt-2">{quote.description}</p>
                    )}
                    {quote.estimatedDuration && (
                      <p className="text-sm text-muted-foreground mt-1">
                        Estimated duration: {quote.estimatedDuration}
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
