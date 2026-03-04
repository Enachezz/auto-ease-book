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
import { api } from '@/lib/api';
import { useToast } from '@/hooks/use-toast';
import { ArrowLeft, Plus, MapPin, Calendar, Clock, DollarSign, Car, Eye, Trash2, Edit, MoreVertical } from 'lucide-react';
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle } from '@/components/ui/alert-dialog';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu';

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
  status: string;
  quoteCount?: number;
}

interface Quote {
  id: string;
  price: number;
  description?: string;
  estimatedDuration?: string;
  status: string;
  warrantyInfo?: string;
  garageId: string;
  garageName?: string;
  garageCity?: string;
  garageRating?: number;
}

export default function JobRequests() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const { toast } = useToast();
  const [loading, setLoading] = useState(false);
  const [jobRequests, setJobRequests] = useState<JobRequest[]>([]);
  const [selectedJob, setSelectedJob] = useState<JobRequest | null>(null);
  const [quotes, setQuotes] = useState<Quote[]>([]);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [jobToDelete, setJobToDelete] = useState<string | null>(null);

  useEffect(() => {
    fetchJobRequests();
  }, []);

  const fetchJobRequests = async () => {
    try {
      const data = await api.get<any[]>('/job-requests');
      if (data) setJobRequests(data);
    } catch (error) {
      console.error('Error fetching job requests:', error);
    }
  };

  const fetchQuotes = async (jobId: string) => {
    try {
      const data = await api.get<any[]>(`/job-requests/${jobId}/quotes`);
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
      await api.post(`/quotes/${quoteId}/accept`, {});

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

  const handleRejectQuote = async (_quoteId: string) => {
    toast({
      title: "Info",
      description: "Rejecting individual quotes is handled automatically when you accept another quote."
    });
  };

  const getStatusColor = (status: string) => {
    switch (status.toUpperCase()) {
      case 'OPEN': return 'default';
      case 'BOOKED': return 'secondary';
      default: return 'secondary';
    }
  };

  const getUrgencyColor = (urgency: string) => {
    switch (urgency.toUpperCase()) {
      case 'HIGH': case 'EMERGENCY': return 'destructive';
      case 'NORMAL': return 'default';
      case 'LOW': return 'secondary';
      default: return 'secondary';
    }
  };

  const handleDeleteRequest = async () => {
    if (!jobToDelete) return;
    
    setLoading(true);
    try {
      await api.delete(`/job-requests/${jobToDelete}`);

      toast({
        title: "Succes",
        description: "Cererea a fost ștearsă"
      });

      fetchJobRequests();
      setDeleteDialogOpen(false);
      setJobToDelete(null);
    } catch (error: any) {
      toast({
        title: "Eroare",
        description: error.message,
        variant: "destructive"
      });
    } finally {
      setLoading(false);
    }
  };

  const handleEditRequest = (jobId: string) => {
    navigate(`/edit-request/${jobId}`);
  };

  return (
    <Layout>
      <div className="space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex flex-col sm:flex-row sm:items-center gap-4">
            <Button variant="outline" size="sm" onClick={() => navigate('/')} className="self-start sm:self-auto">
              <ArrowLeft className="h-4 w-4 mr-2" />
              Înapoi
            </Button>
            <div>
              <h1 className="text-2xl md:text-3xl font-bold">Cererile Mele de Serviciu</h1>
              <p className="text-sm md:text-base text-muted-foreground">Gestionează cererile și vezi ofertele</p>
            </div>
          </div>
          <Button onClick={() => navigate('/request-service')} className="self-start sm:self-auto">
            <Plus className="h-4 w-4 mr-2" />
            <span className="hidden sm:inline">Cerere Nouă</span>
            <span className="sm:hidden">Nouă</span>
          </Button>
        </div>

        <div className="space-y-4">
          {jobRequests.map((job) => (
            <Card key={job.id}>
              <CardContent className="p-6">
                <div className="flex justify-between items-start mb-4">
                  <div className="flex-1">
                    <h3 className="text-lg font-medium">{job.title}</h3>
                    {(job.locationCity || job.locationAddress) && (
                      <p className="text-sm text-muted-foreground flex items-center mt-1">
                        <MapPin className="h-3 w-3 mr-1" />
                        {job.locationAddress || `${job.locationCity}, ${job.locationState}`}
                      </p>
                    )}
                  </div>
                  <div className="flex items-start gap-2">
                    <div className="flex flex-col gap-2 items-end">
                      <div className="flex gap-2">
                        <Badge variant={getUrgencyColor(job.urgency)}>
                          {job.urgency.toLowerCase()} priority
                        </Badge>
                        <Badge variant={getStatusColor(job.status)}>
                          {job.status}
                        </Badge>
                      </div>
                      {job.budgetMin != null && job.budgetMax != null && (
                        <p className="text-sm text-muted-foreground">
                          Budget: ${job.budgetMin} - ${job.budgetMax}
                        </p>
                      )}
                    </div>
                    
                    {job.status.toUpperCase() === 'OPEN' && (
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" size="icon" className="h-8 w-8">
                            <MoreVertical className="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem onClick={() => handleEditRequest(job.id)}>
                            <Edit className="h-4 w-4 mr-2" />
                            Modifică
                          </DropdownMenuItem>
                          <DropdownMenuItem 
                            onClick={() => {
                              setJobToDelete(job.id);
                              setDeleteDialogOpen(true);
                            }}
                            className="text-destructive"
                          >
                            <Trash2 className="h-4 w-4 mr-2" />
                            Șterge
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
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

                <div className="flex gap-2">
                  <Dialog>
                    <DialogTrigger asChild>
                      <Button 
                        variant="outline" 
                        size="sm"
                        onClick={() => handleViewQuotes(job)}
                      >
                        <Eye className="h-4 w-4 mr-2" />
                        View Quotes {job.quoteCount ? `(${job.quoteCount})` : ''}
                      </Button>
                    </DialogTrigger>
                    <DialogContent className="max-w-4xl max-h-[80vh] overflow-y-auto mx-4">
                      <DialogHeader>
                        <DialogTitle className="text-base md:text-lg">Oferte pentru: {selectedJob?.title}</DialogTitle>
                      </DialogHeader>
                      
                      <div className="space-y-4">
                        {quotes.length > 0 ? (
                          quotes.map((quote) => (
                            <Card key={quote.id}>
                              <CardContent className="p-4">
                                <div className="flex justify-between items-start mb-3">
                                  <div>
                                    <h4 className="font-medium">{quote.garageName}</h4>
                                    {quote.garageCity && (
                                      <p className="text-sm text-muted-foreground">
                                        {quote.garageCity}
                                      </p>
                                    )}
                                    {quote.garageRating != null && (
                                      <p className="text-sm text-muted-foreground">
                                        Rating: {quote.garageRating}/5.0
                                      </p>
                                    )}
                                  </div>
                                  <div className="text-right">
                                    <p className="text-2xl font-bold text-primary">${quote.price}</p>
                                    <Badge variant={quote.status === 'ACCEPTED' ? 'default' : quote.status === 'REJECTED' ? 'destructive' : 'secondary'}>
                                      {quote.status}
                                    </Badge>
                                  </div>
                                </div>

                                {quote.description && (
                                  <p className="text-sm mb-2">{quote.description}</p>
                                )}

                                <div className="flex gap-4 text-sm text-muted-foreground mb-3">
                                  {quote.estimatedDuration && (
                                    <span className="flex items-center">
                                      <Clock className="h-3 w-3 mr-1" />
                                      {quote.estimatedDuration}
                                    </span>
                                  )}
                                </div>

                                {quote.warrantyInfo && (
                                  <p className="text-sm text-muted-foreground mb-3">
                                    Warranty: {quote.warrantyInfo}
                                  </p>
                                )}

                                {quote.status === 'PENDING' && selectedJob?.status.toUpperCase() === 'OPEN' && (
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

        <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>Ești sigur că vrei să ștergi această cerere?</AlertDialogTitle>
              <AlertDialogDescription>
                Această acțiune nu poate fi anulată. Cererea și toate ofertele asociate vor fi șterse permanent.
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel onClick={() => {
                setDeleteDialogOpen(false);
                setJobToDelete(null);
              }}>
                Anulează
              </AlertDialogCancel>
              <AlertDialogAction onClick={handleDeleteRequest} disabled={loading}>
                Șterge
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </div>
    </Layout>
  );
}
