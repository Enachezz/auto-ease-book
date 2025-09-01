import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Layout } from '@/components/Layout';
import { Button } from '@/components/ui/button';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { useAuth } from '@/hooks/useAuth';
import { supabase } from '@/integrations/supabase/client';
import { useToast } from '@/hooks/use-toast';
import { ArrowLeft, Plus, MapPin, Calendar, Clock, DollarSign, Car, Eye } from 'lucide-react';

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
  status: string;
  created_at: string;
}

interface Quote {
  id: string;
  price: number;
  description?: string;
  estimated_duration?: string;
  status: string;
  warranty_info?: string;
  created_at: string;
  garage_id: string;
  garages?: {
    business_name: string;
    address: string;
    city: string;
    phone: string;
    average_rating: number;
  };
}

export default function JobRequests() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const { toast } = useToast();
  const [loading, setLoading] = useState(false);
  const [jobRequests, setJobRequests] = useState<JobRequest[]>([]);
  const [selectedJob, setSelectedJob] = useState<JobRequest | null>(null);
  const [quotes, setQuotes] = useState<Quote[]>([]);

  useEffect(() => {
    fetchJobRequests();
  }, []);

  const fetchJobRequests = async () => {
    try {
      const { data, error } = await supabase
        .from('job_requests')
        .select('*')
        .eq('user_id', user?.id)
        .order('created_at', { ascending: false });

      if (error) throw error;
      if (data) setJobRequests(data);
    } catch (error) {
      console.error('Error fetching job requests:', error);
    }
  };

  const fetchQuotes = async (jobId: string) => {
    try {
      const { data, error } = await supabase
        .from('quotes')
        .select(`
          *,
          garages (
            business_name,
            address,
            city,
            phone,
            average_rating
          )
        `)
        .eq('job_request_id', jobId)
        .order('created_at', { ascending: false });

      if (error) throw error;
      if (data) setQuotes(data);
    } catch (error) {
      console.error('Error fetching quotes:', error);
    }
  };

  const handleViewQuotes = async (job: JobRequest) => {
    setSelectedJob(job);
    await fetchQuotes(job.id);
  };

  const handleAcceptQuote = async (quoteId: string) => {
    setLoading(true);
    try {
      const { error } = await supabase
        .from('quotes')
        .update({ status: 'accepted' })
        .eq('id', quoteId);

      if (error) throw error;

      // Update job status to closed
      await supabase
        .from('job_requests')
        .update({ status: 'closed' })
        .eq('id', selectedJob?.id);

      toast({
        title: "Success",
        description: "Quote accepted! The garage will contact you to schedule the service."
      });

      fetchJobRequests();
      if (selectedJob) fetchQuotes(selectedJob.id);
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

  const handleRejectQuote = async (quoteId: string) => {
    setLoading(true);
    try {
      const { error } = await supabase
        .from('quotes')
        .update({ status: 'rejected' })
        .eq('id', quoteId);

      if (error) throw error;

      toast({
        title: "Success",
        description: "Quote rejected"
      });

      if (selectedJob) fetchQuotes(selectedJob.id);
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

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'open': return 'default';
      case 'closed': return 'secondary';
      default: return 'secondary';
    }
  };

  const getUrgencyColor = (urgency: string) => {
    switch (urgency) {
      case 'high': return 'destructive';
      case 'medium': return 'default';
      case 'low': return 'secondary';
      default: return 'secondary';
    }
  };

  return (
    <Layout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <Button variant="outline" size="sm" onClick={() => navigate('/')}>
              <ArrowLeft className="h-4 w-4 mr-2" />
              Back
            </Button>
            <div>
              <h1 className="text-3xl font-bold">My Service Requests</h1>
              <p className="text-muted-foreground">Manage your job requests and view quotes</p>
            </div>
          </div>
          <Button onClick={() => navigate('/request-service')}>
            <Plus className="h-4 w-4 mr-2" />
            New Request
          </Button>
        </div>

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
                    <p className="text-sm text-muted-foreground">
                      Requested {new Date(job.created_at).toLocaleDateString()}
                    </p>
                  </div>
                  <div className="flex flex-col gap-2 items-end">
                    <div className="flex gap-2">
                      <Badge variant={getUrgencyColor(job.urgency)}>
                        {job.urgency} priority
                      </Badge>
                      <Badge variant={getStatusColor(job.status)}>
                        {job.status}
                      </Badge>
                    </div>
                    {job.budget_min && job.budget_max && (
                      <p className="text-sm text-muted-foreground">
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

                <div className="flex gap-2">
                  <Dialog>
                    <DialogTrigger asChild>
                      <Button 
                        variant="outline" 
                        size="sm"
                        onClick={() => handleViewQuotes(job)}
                      >
                        <Eye className="h-4 w-4 mr-2" />
                        View Quotes
                      </Button>
                    </DialogTrigger>
                    <DialogContent className="max-w-4xl max-h-[80vh] overflow-y-auto">
                      <DialogHeader>
                        <DialogTitle>Quotes for: {selectedJob?.title}</DialogTitle>
                      </DialogHeader>
                      
                      <div className="space-y-4">
                        {quotes.length > 0 ? (
                          quotes.map((quote) => (
                            <Card key={quote.id}>
                              <CardContent className="p-4">
                                <div className="flex justify-between items-start mb-3">
                                  <div>
                                    <h4 className="font-medium">{quote.garages?.business_name}</h4>
                                    <p className="text-sm text-muted-foreground">
                                      {quote.garages?.address}, {quote.garages?.city}
                                    </p>
                                    <p className="text-sm text-muted-foreground">
                                      Phone: {quote.garages?.phone}
                                    </p>
                                    {quote.garages?.average_rating && (
                                      <p className="text-sm text-muted-foreground">
                                        Rating: {quote.garages.average_rating}/5.0
                                      </p>
                                    )}
                                  </div>
                                  <div className="text-right">
                                    <p className="text-2xl font-bold text-primary">${quote.price}</p>
                                    <Badge variant={quote.status === 'accepted' ? 'default' : quote.status === 'rejected' ? 'destructive' : 'secondary'}>
                                      {quote.status}
                                    </Badge>
                                  </div>
                                </div>

                                {quote.description && (
                                  <p className="text-sm mb-2">{quote.description}</p>
                                )}

                                <div className="flex gap-4 text-sm text-muted-foreground mb-3">
                                  {quote.estimated_duration && (
                                    <span className="flex items-center">
                                      <Clock className="h-3 w-3 mr-1" />
                                      {quote.estimated_duration}
                                    </span>
                                  )}
                                  <span>
                                    Quoted {new Date(quote.created_at).toLocaleDateString()}
                                  </span>
                                </div>

                                {quote.warranty_info && (
                                  <p className="text-sm text-muted-foreground mb-3">
                                    Warranty: {quote.warranty_info}
                                  </p>
                                )}

                                {quote.status === 'pending' && selectedJob?.status === 'open' && (
                                  <div className="flex gap-2">
                                    <Button 
                                      size="sm" 
                                      onClick={() => handleAcceptQuote(quote.id)}
                                      disabled={loading}
                                    >
                                      Accept Quote
                                    </Button>
                                    <Button 
                                      size="sm" 
                                      variant="outline"
                                      onClick={() => handleRejectQuote(quote.id)}
                                      disabled={loading}
                                    >
                                      Reject
                                    </Button>
                                  </div>
                                )}
                              </CardContent>
                            </Card>
                          ))
                        ) : (
                          <div className="text-center py-8">
                            <DollarSign className="h-12 w-12 mx-auto mb-4 text-muted-foreground" />
                            <h3 className="text-lg font-medium mb-2">No quotes yet</h3>
                            <p className="text-muted-foreground">Garages will start sending quotes soon</p>
                          </div>
                        )}
                      </div>
                    </DialogContent>
                  </Dialog>
                </div>
              </CardContent>
            </Card>
          ))}

          {jobRequests.length === 0 && (
            <Card>
              <CardContent className="p-6 text-center">
                <Car className="h-12 w-12 mx-auto mb-4 text-muted-foreground" />
                <h3 className="text-lg font-medium mb-2">No service requests yet</h3>
                <p className="text-muted-foreground mb-4">
                  Create your first service request to get quotes from local garages
                </p>
                <Button onClick={() => navigate('/request-service')}>
                  <Plus className="h-4 w-4 mr-2" />
                  Request Service
                </Button>
              </CardContent>
            </Card>
          )}
        </div>
      </div>
    </Layout>
  );
}