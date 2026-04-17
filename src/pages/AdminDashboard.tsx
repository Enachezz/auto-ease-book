import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Layout } from '@/components/Layout';
import { Button } from '@/components/ui/button';
import { Card, CardHeader, CardTitle, CardContent, CardDescription } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { useAuth } from '@/hooks/useAuth';
import { api } from '@/lib/api';
import { useToast } from '@/hooks/use-toast';
import { 
  Shield, 
  CheckCircle, 
  XCircle, 
  Building2, 
  Users, 
  Clock,
  MapPin,
  Phone
} from 'lucide-react';

interface GarageForAdmin {
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
  userId: string;
}

export default function AdminDashboard() {
  const { profile, loading } = useAuth();
  const navigate = useNavigate();
  const { toast } = useToast();

  const [garages, setGarages] = useState<GarageForAdmin[]>([]);
  const [loadingGarages, setLoadingGarages] = useState(true);
  const [processingId, setProcessingId] = useState<string | null>(null);

  useEffect(() => {
    if (!loading && (!profile || profile.user_type !== 'admin')) {
      navigate('/');
    }
  }, [loading, profile, navigate]);

  useEffect(() => {
    if (profile?.user_type === 'admin') {
      loadGarages();
    }
  }, [profile]);

  const loadGarages = async () => {
    try {
      setLoadingGarages(true);
      const data = await api.get<GarageForAdmin[]>('/garages/all');
      setGarages(data);
    } catch (error: any) {
      toast({
        title: 'Eroare',
        description: 'Nu s-au putut încărca service-urile.',
        variant: 'destructive',
      });
    } finally {
      setLoadingGarages(false);
    }
  };

  const handleApprove = async (garageId: string) => {
    try {
      setProcessingId(garageId);
      await api.put(`/garages/${garageId}/approve`);
      toast({
        title: 'Succes',
        description: 'Service-ul a fost aprobat cu succes!',
      });
      loadGarages();
    } catch (error: any) {
      toast({
        title: 'Eroare',
        description: error.message || 'Nu s-a putut aproba service-ul.',
        variant: 'destructive',
      });
    } finally {
      setProcessingId(null);
    }
  };

  const handleReject = async (garageId: string) => {
    try {
      setProcessingId(garageId);
      await api.put(`/garages/${garageId}/reject`);
      toast({
        title: 'Succes',
        description: 'Service-ul a fost respins.',
      });
      loadGarages();
    } catch (error: any) {
      toast({
        title: 'Eroare',
        description: error.message || 'Nu s-a putut respinge service-ul.',
        variant: 'destructive',
      });
    } finally {
      setProcessingId(null);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
      </div>
    );
  }

  if (!profile || profile.user_type !== 'admin') {
    return null;
  }

  const pendingGarages = garages.filter(g => !g.isApproved);
  const approvedGarages = garages.filter(g => g.isApproved);

  return (
    <Layout>
      <div className="max-w-6xl mx-auto">
        {/* Header */}
        <div className="flex items-center gap-3 mb-8">
          <div className="w-12 h-12 bg-primary/10 rounded-full flex items-center justify-center">
            <Shield className="h-6 w-6 text-primary" />
          </div>
          <div>
            <h1 className="text-3xl font-bold">Panou Admin</h1>
            <p className="text-muted-foreground">Gestionează platforma Fast Fix Auto</p>
          </div>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-8">
          <Card>
            <CardContent className="pt-6">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-yellow-500/10 rounded-full flex items-center justify-center">
                  <Clock className="h-5 w-5 text-yellow-600" />
                </div>
                <div>
                  <p className="text-2xl font-bold">{pendingGarages.length}</p>
                  <p className="text-sm text-muted-foreground">În așteptare</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="pt-6">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-green-500/10 rounded-full flex items-center justify-center">
                  <CheckCircle className="h-5 w-5 text-green-600" />
                </div>
                <div>
                  <p className="text-2xl font-bold">{approvedGarages.length}</p>
                  <p className="text-sm text-muted-foreground">Aprobate</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="pt-6">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-primary/10 rounded-full flex items-center justify-center">
                  <Building2 className="h-5 w-5 text-primary" />
                </div>
                <div>
                  <p className="text-2xl font-bold">{garages.length}</p>
                  <p className="text-sm text-muted-foreground">Total Service-uri</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Tabs */}
        <Tabs defaultValue="pending">
          <TabsList className="mb-6">
            <TabsTrigger value="pending" className="gap-2">
              <Clock className="h-4 w-4" />
              În Așteptare ({pendingGarages.length})
            </TabsTrigger>
            <TabsTrigger value="approved" className="gap-2">
              <CheckCircle className="h-4 w-4" />
              Aprobate ({approvedGarages.length})
            </TabsTrigger>
          </TabsList>

          <TabsContent value="pending">
            {loadingGarages ? (
              <div className="text-center py-12">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto mb-4"></div>
                <p className="text-muted-foreground">Se încarcă...</p>
              </div>
            ) : pendingGarages.length === 0 ? (
              <Card>
                <CardContent className="py-12 text-center">
                  <CheckCircle className="h-12 w-12 text-green-500 mx-auto mb-4" />
                  <h3 className="text-lg font-semibold mb-2">Totul este la zi!</h3>
                  <p className="text-muted-foreground">Nu sunt service-uri în așteptarea aprobării.</p>
                </CardContent>
              </Card>
            ) : (
              <div className="space-y-4">
                {pendingGarages.map(garage => (
                  <GarageCard
                    key={garage.id}
                    garage={garage}
                    onApprove={handleApprove}
                    onReject={handleReject}
                    processing={processingId === garage.id}
                    showActions
                  />
                ))}
              </div>
            )}
          </TabsContent>

          <TabsContent value="approved">
            {loadingGarages ? (
              <div className="text-center py-12">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto mb-4"></div>
                <p className="text-muted-foreground">Se încarcă...</p>
              </div>
            ) : approvedGarages.length === 0 ? (
              <Card>
                <CardContent className="py-12 text-center">
                  <Building2 className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
                  <p className="text-muted-foreground">Niciun service aprobat încă.</p>
                </CardContent>
              </Card>
            ) : (
              <div className="space-y-4">
                {approvedGarages.map(garage => (
                  <GarageCard
                    key={garage.id}
                    garage={garage}
                    onApprove={handleApprove}
                    onReject={handleReject}
                    processing={processingId === garage.id}
                    showActions={false}
                  />
                ))}
              </div>
            )}
          </TabsContent>
        </Tabs>
      </div>
    </Layout>
  );
}

interface GarageCardProps {
  garage: GarageForAdmin;
  onApprove: (id: string) => void;
  onReject: (id: string) => void;
  processing: boolean;
  showActions: boolean;
}

function GarageCard({ garage, onApprove, onReject, processing, showActions }: GarageCardProps) {
  return (
    <Card>
      <CardContent className="pt-6">
        <div className="flex flex-col md:flex-row md:items-start md:justify-between gap-4">
          <div className="flex-1 space-y-3">
            <div className="flex items-center gap-3">
              <h3 className="text-lg font-semibold">{garage.businessName}</h3>
              <Badge variant={garage.isApproved ? 'default' : 'secondary'}>
                {garage.isApproved ? 'Aprobat' : 'În așteptare'}
              </Badge>
            </div>

            <div className="flex flex-wrap gap-4 text-sm text-muted-foreground">
              <span className="flex items-center gap-1">
                <MapPin className="h-4 w-4" />
                {garage.address}, {garage.city}, {garage.state} {garage.postalCode}
              </span>
              <span className="flex items-center gap-1">
                <Phone className="h-4 w-4" />
                {garage.phone}
              </span>
            </div>

            {garage.description && (
              <p className="text-sm text-muted-foreground">{garage.description}</p>
            )}

            {garage.services && garage.services.length > 0 && (
              <div className="flex flex-wrap gap-2">
                {garage.services.map((service, i) => (
                  <Badge key={i} variant="outline" className="text-xs">
                    {service}
                  </Badge>
                ))}
              </div>
            )}
          </div>

          {showActions && (
            <div className="flex gap-2 md:flex-col">
              <Button
                onClick={() => onApprove(garage.id)}
                disabled={processing}
                className="gap-2"
                size="sm"
              >
                <CheckCircle className="h-4 w-4" />
                Aprobă
              </Button>
              <Button
                variant="destructive"
                onClick={() => onReject(garage.id)}
                disabled={processing}
                className="gap-2"
                size="sm"
              >
                <XCircle className="h-4 w-4" />
                Respinge
              </Button>
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
