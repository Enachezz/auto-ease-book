import { useState, useEffect } from 'react';
import { useAuth } from '@/hooks/useAuth';
import { Layout } from '@/components/Layout';
import { Button } from '@/components/ui/button';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { supabase } from '@/integrations/supabase/client';
import { useToast } from '@/hooks/use-toast';
import { 
  Car, 
  Plus, 
  Edit, 
  Calendar, 
  MapPin, 
  DollarSign, 
  Clock,
  Wrench,
  Star,
  FileText,
  Download,
  AlertCircle,
  CheckCircle
} from 'lucide-react';
import carsImage from '@/assets/cars.png';

interface Car {
  id: string;
  make_id: string;
  model_id: string;
  year: number;
  color?: string;
  license_plate?: string;
  mileage?: number;
  vin?: string;
  car_makes?: { name: string };
  car_models?: { name: string };
}

interface ServiceHistory {
  id: string;
  title: string;
  description: string;
  created_at: string;
  status: string;
  quotes?: {
    id: string;
    price: number;
    status: string;
    garage_id: string;
    garages?: {
      business_name: string;
    } | null;
    bookings?: {
      id: string;
      scheduled_date: string;
      status: string;
      reviews?: {
        rating: number;
        comment: string;
      }[] | null;
    }[] | null;
  }[] | null;
}

interface ManualHistoryEntry {
  id: string;
  type: 'service' | 'maintenance' | 'repair' | 'inspection';
  title: string;
  description: string;
  date: string;
  mileage?: number;
  cost?: number;
  location?: string;
  notes?: string;
}

interface CarDocument {
  id: string;
  type: 'asigurare' | 'rovinieta' | 'itp';
  title: string;
  expiry_date: string;
  file_url?: string;
  notes?: string;
}

interface CarMake {
  id: string;
  name: string;
}

interface CarModel {
  id: string;
  name: string;
  make_id: string;
}

const MyCars = () => {
  const { user, profile } = useAuth();
  const { toast } = useToast();
  const [cars, setCars] = useState<Car[]>([]);
  const [serviceHistory, setServiceHistory] = useState<ServiceHistory[]>([]);
  const [manualHistory, setManualHistory] = useState<ManualHistoryEntry[]>([]);
  const [carDocuments, setCarDocuments] = useState<CarDocument[]>([]);
  const [carMakes, setCarMakes] = useState<CarMake[]>([]);
  const [carModels, setCarModels] = useState<CarModel[]>([]);
  const [selectedCar, setSelectedCar] = useState<Car | null>(null);
  const [loading, setLoading] = useState(true);
  const [isAddingCar, setIsAddingCar] = useState(false);
  const [isAddingHistory, setIsAddingHistory] = useState(false);
  const [isAddingDocument, setIsAddingDocument] = useState(false);
  const [newCar, setNewCar] = useState({
    make_id: '',
    model_id: '',
    year: new Date().getFullYear(),
    color: '',
    license_plate: '',
    mileage: '',
    vin: ''
  });
  const [newHistoryEntry, setNewHistoryEntry] = useState({
    type: 'service' as const,
    title: '',
    description: '',
    date: '',
    mileage: '',
    cost: '',
    location: '',
    notes: ''
  });
  const [newDocument, setNewDocument] = useState({
    type: 'asigurare' as const,
    title: '',
    expiry_date: '',
    notes: ''
  });

  useEffect(() => {
    if (user) {
      fetchCars();
      fetchCarMakes();
    }
  }, [user]);

  useEffect(() => {
    if (selectedCar) {
      fetchServiceHistory(selectedCar.id);
    }
  }, [selectedCar]);

  const fetchCars = async () => {
    try {
      const { data, error } = await supabase
        .from('cars')
        .select(`
          *,
          car_makes (name),
          car_models (name)
        `)
        .eq('user_id', user?.id)
        .order('created_at', { ascending: false });

      if (error) throw error;
      setCars(data || []);
      if (data && data.length > 0 && !selectedCar) {
        setSelectedCar(data[0]);
      }
    } catch (error) {
      toast({
        title: "Error",
        description: "Failed to fetch cars",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  const fetchCarMakes = async () => {
    try {
      const { data, error } = await supabase
        .from('car_makes')
        .select('*')
        .order('name');

      if (error) throw error;
      setCarMakes(data || []);
    } catch (error) {
      console.error('Error fetching car makes:', error);
    }
  };

  const fetchCarModels = async (makeId: string) => {
    console.log('Fetching car models for makeId:', makeId);
    try {
      const { data, error } = await supabase
        .from('car_models')
        .select('*')
        .eq('make_id', makeId)
        .order('name');

      if (error) {
        console.error('Error fetching car models:', error);
        throw error;
      }
      console.log('Fetched car models:', data);
      setCarModels(data || []);
    } catch (error) {
      console.error('Error fetching car models:', error);
      setCarModels([]); // Ensure models are cleared on error
    }
  };

  const fetchServiceHistory = async (carId: string) => {
    try {
      const { data, error } = await supabase
        .from('job_requests')
        .select(`
          *,
          quotes (
            id,
            price,
            status,
            garage_id,
            garages (business_name),
            bookings (
              id,
              scheduled_date,
              status,
              reviews (rating, comment)
            )
          )
        `)
        .eq('car_id', carId)
        .order('created_at', { ascending: false });

      if (error) throw error;
      setServiceHistory((data as any) || []);
    } catch (error) {
      toast({
        title: "Error",
        description: "Failed to fetch service history",
        variant: "destructive",
      });
    }
  };

  const handleAddCar = async () => {
    try {
      const { error } = await supabase
        .from('cars')
        .insert({
          user_id: user?.id,
          make_id: newCar.make_id,
          model_id: newCar.model_id,
          year: newCar.year,
          color: newCar.color || null,
          license_plate: newCar.license_plate || null,
          mileage: newCar.mileage ? parseInt(newCar.mileage) : null,
          vin: newCar.vin || null
        });

      if (error) throw error;

      toast({
        title: "Success",
        description: "Car added successfully",
      });

      setIsAddingCar(false);
      setNewCar({
        make_id: '',
        model_id: '',
        year: new Date().getFullYear(),
        color: '',
        license_plate: '',
        mileage: '',
        vin: ''
      });
      fetchCars();
    } catch (error) {
      toast({
        title: "Error",
        description: "Failed to add car",
        variant: "destructive",
      });
    }
  };

  const handleAddManualHistory = () => {
    if (!selectedCar) return;
    
    const newEntry: ManualHistoryEntry = {
      id: Date.now().toString(),
      type: newHistoryEntry.type,
      title: newHistoryEntry.title,
      description: newHistoryEntry.description,
      date: newHistoryEntry.date,
      mileage: newHistoryEntry.mileage ? parseInt(newHistoryEntry.mileage) : undefined,
      cost: newHistoryEntry.cost ? parseFloat(newHistoryEntry.cost) : undefined,
      location: newHistoryEntry.location || undefined,
      notes: newHistoryEntry.notes || undefined
    };

    setManualHistory(prev => [newEntry, ...prev]);
    setIsAddingHistory(false);
    setNewHistoryEntry({
      type: 'service',
      title: '',
      description: '',
      date: '',
      mileage: '',
      cost: '',
      location: '',
      notes: ''
    });

    toast({
      title: "Succes",
      description: "Intrarea din istoric a fost adăugată cu succes",
    });
  };

  const handleAddDocument = () => {
    if (!selectedCar) return;
    
    const newDoc: CarDocument = {
      id: Date.now().toString(),
      type: newDocument.type,
      title: newDocument.title,
      expiry_date: newDocument.expiry_date,
      notes: newDocument.notes || undefined
    };

    setCarDocuments(prev => [newDoc, ...prev]);
    setIsAddingDocument(false);
    setNewDocument({
      type: 'asigurare',
      title: '',
      expiry_date: '',
      notes: ''
    });

    toast({
      title: "Succes",
      description: "Documentul a fost adăugat cu succes",
    });
  };

  const generatePDF = async () => {
    if (!selectedCar) return;

    try {
      const jsPDF = (await import('jspdf')).default;
      const doc = new jsPDF();

      // Title
      doc.setFontSize(20);
      doc.text('Raport Istoric Vehicul', 20, 20);

      // Car details
      doc.setFontSize(14);
      doc.text('Detalii Vehicul:', 20, 40);
      doc.setFontSize(12);
      doc.text(`Marca: ${selectedCar.car_makes?.name}`, 20, 50);
      doc.text(`Model: ${selectedCar.car_models?.name}`, 20, 60);
      doc.text(`An: ${selectedCar.year}`, 20, 70);
      doc.text(`VIN: ${selectedCar.vin || 'Nu este specificat'}`, 20, 80);
      doc.text(`Număr înmatriculare: ${selectedCar.license_plate || 'Nu este specificat'}`, 20, 90);
      doc.text(`Kilometraj: ${selectedCar.mileage ? `${selectedCar.mileage.toLocaleString()} km` : 'Nu este specificat'}`, 20, 100);

      let yPosition = 120;

      // Service history
      if (serviceHistory.length > 0) {
        doc.setFontSize(14);
        doc.text('Istoric Service (Platform):', 20, yPosition);
        yPosition += 10;

        serviceHistory.forEach((service) => {
          doc.setFontSize(10);
          doc.text(`• ${service.title} - ${new Date(service.created_at).toLocaleDateString()}`, 25, yPosition);
          yPosition += 10;
          if (yPosition > 280) {
            doc.addPage();
            yPosition = 20;
          }
        });
      }

      // Manual history
      if (manualHistory.length > 0) {
        yPosition += 10;
        doc.setFontSize(14);
        doc.text('Istoric Manual:', 20, yPosition);
        yPosition += 10;

        manualHistory.forEach((entry) => {
          doc.setFontSize(10);
          doc.text(`• ${entry.title} - ${new Date(entry.date).toLocaleDateString()}`, 25, yPosition);
          if (entry.cost) {
            doc.text(`Cost: ${entry.cost} RON`, 25, yPosition + 5);
            yPosition += 5;
          }
          yPosition += 10;
          if (yPosition > 280) {
            doc.addPage();
            yPosition = 20;
          }
        });
      }

      doc.save(`istoric-vehicul-${selectedCar.license_plate || selectedCar.id}.pdf`);
      
      toast({
        title: "PDF generat",
        description: "Raportul a fost descărcat cu succes",
      });
    } catch (error) {
      toast({
        title: "Eroare",
        description: "Nu s-a putut genera PDF-ul",
        variant: "destructive",
      });
    }
  };

  const getDocumentStatus = (expiryDate: string) => {
    const today = new Date();
    const expiry = new Date(expiryDate);
    const daysUntilExpiry = Math.ceil((expiry.getTime() - today.getTime()) / (1000 * 3600 * 24));
    
    if (daysUntilExpiry < 0) return { status: 'expired', color: 'destructive', days: Math.abs(daysUntilExpiry) };
    if (daysUntilExpiry <= 30) return { status: 'expiring', color: 'warning', days: daysUntilExpiry };
    return { status: 'valid', color: 'default', days: daysUntilExpiry };
  };

  if (!user || !profile) {
    return (
      <Layout>
        <div className="text-center py-12">
          <p>Please log in to view your cars.</p>
        </div>
      </Layout>
    );
  }

  if (profile.user_type !== 'car_owner') {
    return (
      <Layout>
        <div className="text-center py-12">
          <p>This page is only available for car owners.</p>
        </div>
      </Layout>
    );
  }

  if (loading) {
    return (
      <Layout>
        <div className="text-center py-12">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto mb-4"></div>
          <p>Loading your cars...</p>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="space-y-6">
        <div className="flex justify-between items-center">
          <div>
            <h1 className="text-3xl font-bold">My Cars</h1>
            <p className="text-muted-foreground">Manage your vehicles and view service history</p>
          </div>
          <Dialog open={isAddingCar} onOpenChange={(open) => {
            setIsAddingCar(open);
            if (open) {
              // Reset form and models when dialog opens
              setNewCar({
                make_id: '',
                model_id: '',
                year: new Date().getFullYear(),
                color: '',
                mileage: '',
                license_plate: '',
                vin: ''
              });
              setCarModels([]); // Clear models when dialog opens
            }
          }}>
            <DialogTrigger asChild>
              <Button>
                <Plus className="h-4 w-4 mr-2" />
                Adaugă Mașină
              </Button>
            </DialogTrigger>
            <DialogContent className="max-w-md">
              <DialogHeader>
                <DialogTitle>Adaugă Mașină Nouă</DialogTitle>
              </DialogHeader>
              <div className="space-y-4">
                <div>
                  <Label htmlFor="make">Make</Label>
                  <Select 
                    value={newCar.make_id} 
                    onValueChange={(value) => {
                      setNewCar({ ...newCar, make_id: value, model_id: '' });
                      fetchCarModels(value);
                    }}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Select make" />
                    </SelectTrigger>
                    <SelectContent>
                      {carMakes.map((make) => (
                        <SelectItem key={make.id} value={make.id}>
                          {make.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <div>
                  <Label htmlFor="model">Model</Label>
                  <Select 
                    value={newCar.model_id} 
                    onValueChange={(value) => setNewCar({ ...newCar, model_id: value })}
                    disabled={!newCar.make_id}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Select model" />
                    </SelectTrigger>
                    <SelectContent>
                      {carModels.map((model) => (
                        <SelectItem key={model.id} value={model.id}>
                          {model.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <div>
                  <Label htmlFor="year">Year</Label>
                  <Input
                    id="year"
                    type="number"
                    value={newCar.year}
                    onChange={(e) => setNewCar({ ...newCar, year: parseInt(e.target.value) })}
                    min="1900"
                    max={new Date().getFullYear() + 1}
                  />
                </div>

                <div>
                  <Label htmlFor="color">Color (Optional)</Label>
                  <Input
                    id="color"
                    value={newCar.color}
                    onChange={(e) => setNewCar({ ...newCar, color: e.target.value })}
                    placeholder="e.g. Red, Blue, White"
                  />
                </div>

                <div>
                  <Label htmlFor="license_plate">License Plate (Optional)</Label>
                  <Input
                    id="license_plate"
                    value={newCar.license_plate}
                    onChange={(e) => setNewCar({ ...newCar, license_plate: e.target.value })}
                    placeholder="e.g. ABC-123"
                  />
                </div>

                <div>
                  <Label htmlFor="mileage">Current Mileage (Optional)</Label>
                  <Input
                    id="mileage"
                    type="number"
                    value={newCar.mileage}
                    onChange={(e) => setNewCar({ ...newCar, mileage: e.target.value })}
                    placeholder="e.g. 50000"
                  />
                </div>

                <div>
                  <Label htmlFor="vin">VIN (Optional)</Label>
                  <Input
                    id="vin"
                    value={newCar.vin}
                    onChange={(e) => setNewCar({ ...newCar, vin: e.target.value })}
                    placeholder="17-character VIN"
                    maxLength={17}
                  />
                </div>

                <Button 
                  onClick={handleAddCar} 
                  className="w-full"
                  disabled={!newCar.make_id || !newCar.model_id}
                >
                  Add Car
                </Button>
              </div>
            </DialogContent>
          </Dialog>
        </div>

        {cars.length === 0 ? (
          <Card className="text-center py-12">
            <CardContent>
              <Car className="h-12 w-12 mx-auto mb-4 text-muted-foreground" />
              <h3 className="text-lg font-semibold mb-2">No cars added yet</h3>
              <p className="text-muted-foreground mb-4">
                Add your first car to start tracking service history
              </p>
              <Button onClick={() => setIsAddingCar(true)}>
                <Plus className="h-4 w-4 mr-2" />
                Add Your First Car
              </Button>
            </CardContent>
          </Card>
        ) : (
          <div className="grid lg:grid-cols-4 gap-6">
            {/* Car Selection Sidebar */}
            <div className="lg:col-span-1">
              <Card>
                <CardHeader>
                  <CardTitle className="text-lg">Your Cars</CardTitle>
                </CardHeader>
                <CardContent className="space-y-2">
                  {cars.map((car) => (
                    <div
                      key={car.id}
                      className={`p-3 rounded-lg border cursor-pointer transition-colors ${
                        selectedCar?.id === car.id 
                          ? 'bg-primary/10 border-primary' 
                          : 'hover:bg-muted/50'
                      }`}
                      onClick={() => setSelectedCar(car)}
                    >
                      <div className="font-medium">
                        {car.car_makes?.name} {car.car_models?.name}
                      </div>
                      <div className="text-sm text-muted-foreground">
                        {car.year} • {car.color || 'Unknown color'}
                      </div>
                      {car.license_plate && (
                        <div className="text-xs text-muted-foreground">
                          {car.license_plate}
                        </div>
                      )}
                    </div>
                  ))}
                </CardContent>
              </Card>
            </div>

            {/* Main Content */}
            <div className="lg:col-span-3">
              {selectedCar && (
                <Tabs defaultValue="details" className="space-y-6">
                  <TabsList>
                    <TabsTrigger value="details">Detalii Mașină</TabsTrigger>
                    <TabsTrigger value="history">Istoric Service</TabsTrigger>
                    <TabsTrigger value="documents">Documente</TabsTrigger>
                  </TabsList>

                  <div className="flex justify-center py-6">
                    <img 
                      src={carsImage} 
                      alt="Cars" 
                      className="w-full max-w-2xl h-auto object-contain"
                    />
                  </div>

                  <TabsContent value="details">
                    <Card>
                      <CardHeader>
                        <div className="flex justify-between items-center">
                          <CardTitle>
                            {selectedCar.car_makes?.name} {selectedCar.car_models?.name} ({selectedCar.year})
                          </CardTitle>
                          <Button variant="outline" size="sm">
                            <Edit className="h-4 w-4 mr-2" />
                            Edit
                          </Button>
                        </div>
                      </CardHeader>
                      <CardContent>
                        <div className="grid md:grid-cols-2 gap-6">
                          <div className="space-y-4">
                            <div>
                              <Label className="text-sm font-medium">Make & Model</Label>
                              <p className="text-sm text-muted-foreground">
                                {selectedCar.car_makes?.name} {selectedCar.car_models?.name}
                              </p>
                            </div>
                            <div>
                              <Label className="text-sm font-medium">Year</Label>
                              <p className="text-sm text-muted-foreground">{selectedCar.year}</p>
                            </div>
                            <div>
                              <Label className="text-sm font-medium">Color</Label>
                              <p className="text-sm text-muted-foreground">
                                {selectedCar.color || 'Not specified'}
                              </p>
                            </div>
                          </div>
                          <div className="space-y-4">
                            <div>
                              <Label className="text-sm font-medium">Număr de Înmatriculare</Label>
                              <p className="text-sm text-muted-foreground">
                                {selectedCar.license_plate || 'Nu este specificat'}
                              </p>
                            </div>
                            <div>
                              <Label className="text-sm font-medium">Kilometraj Curent</Label>
                              <p className="text-sm text-muted-foreground">
                                {selectedCar.mileage ? `${selectedCar.mileage.toLocaleString()} km` : 'Nu este specificat'}
                              </p>
                            </div>
                            <div>
                              <Label className="text-sm font-medium">VIN</Label>
                              <p className="text-sm text-muted-foreground font-mono">
                                {selectedCar.vin || 'Nu este specificat'}
                              </p>
                            </div>
                          </div>
                        </div>
                      </CardContent>
                    </Card>
                  </TabsContent>

                  <TabsContent value="history">
                    <div className="space-y-4">
                      <div className="flex justify-between items-center">
                        <h3 className="text-lg font-semibold">Istoricul Service-ului</h3>
                        <div className="flex gap-2">
                          <Dialog open={isAddingHistory} onOpenChange={setIsAddingHistory}>
                            <DialogTrigger asChild>
                              <Button variant="outline" size="sm">
                                <Plus className="h-4 w-4 mr-2" />
                                Adaugă Intrare
                              </Button>
                            </DialogTrigger>
                            <DialogContent className="max-w-md">
                              <DialogHeader>
                                <DialogTitle>Adaugă Intrare Manuală în Istoric</DialogTitle>
                              </DialogHeader>
                              <div className="space-y-4">
                                <div>
                                  <Label htmlFor="type">Tip Service</Label>
                                  <Select 
                                    value={newHistoryEntry.type} 
                                    onValueChange={(value: any) => setNewHistoryEntry({ ...newHistoryEntry, type: value })}
                                  >
                                    <SelectTrigger>
                                      <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent>
                                      <SelectItem value="service">Service</SelectItem>
                                      <SelectItem value="maintenance">Întreținere</SelectItem>
                                      <SelectItem value="repair">Reparație</SelectItem>
                                      <SelectItem value="inspection">Inspecție</SelectItem>
                                    </SelectContent>
                                  </Select>
                                </div>
                                <div>
                                  <Label htmlFor="title">Titlu</Label>
                                  <Input
                                    id="title"
                                    value={newHistoryEntry.title}
                                    onChange={(e) => setNewHistoryEntry({ ...newHistoryEntry, title: e.target.value })}
                                    placeholder="ex. Schimb ulei motor"
                                  />
                                </div>
                                <div>
                                  <Label htmlFor="description">Descriere</Label>
                                  <Textarea
                                    id="description"
                                    value={newHistoryEntry.description}
                                    onChange={(e) => setNewHistoryEntry({ ...newHistoryEntry, description: e.target.value })}
                                    placeholder="Descrierea lucrărilor efectuate"
                                  />
                                </div>
                                <div>
                                  <Label htmlFor="date">Data</Label>
                                  <Input
                                    id="date"
                                    type="date"
                                    value={newHistoryEntry.date}
                                    onChange={(e) => setNewHistoryEntry({ ...newHistoryEntry, date: e.target.value })}
                                  />
                                </div>
                                <div>
                                  <Label htmlFor="mileage">Kilometraj (opțional)</Label>
                                  <Input
                                    id="mileage"
                                    type="number"
                                    value={newHistoryEntry.mileage}
                                    onChange={(e) => setNewHistoryEntry({ ...newHistoryEntry, mileage: e.target.value })}
                                    placeholder="ex. 50000"
                                  />
                                </div>
                                <div>
                                  <Label htmlFor="cost">Cost (RON) (opțional)</Label>
                                  <Input
                                    id="cost"
                                    type="number"
                                    step="0.01"
                                    value={newHistoryEntry.cost}
                                    onChange={(e) => setNewHistoryEntry({ ...newHistoryEntry, cost: e.target.value })}
                                    placeholder="ex. 250.50"
                                  />
                                </div>
                                <div>
                                  <Label htmlFor="location">Locație (opțional)</Label>
                                  <Input
                                    id="location"
                                    value={newHistoryEntry.location}
                                    onChange={(e) => setNewHistoryEntry({ ...newHistoryEntry, location: e.target.value })}
                                    placeholder="ex. Service Auto XYZ"
                                  />
                                </div>
                                <div>
                                  <Label htmlFor="notes">Note (opțional)</Label>
                                  <Textarea
                                    id="notes"
                                    value={newHistoryEntry.notes}
                                    onChange={(e) => setNewHistoryEntry({ ...newHistoryEntry, notes: e.target.value })}
                                    placeholder="Note suplimentare"
                                  />
                                </div>
                                <Button 
                                  onClick={handleAddManualHistory} 
                                  className="w-full"
                                  disabled={!newHistoryEntry.title || !newHistoryEntry.description || !newHistoryEntry.date}
                                >
                                  Adaugă Intrare
                                </Button>
                              </div>
                            </DialogContent>
                          </Dialog>
                          <Button variant="outline" size="sm" onClick={generatePDF}>
                            <Download className="h-4 w-4 mr-2" />
                            Descarcă PDF
                          </Button>
                          <Badge variant="secondary">
                            {serviceHistory.length + manualHistory.length} intrări
                          </Badge>
                        </div>
                      </div>

                      {serviceHistory.length === 0 && manualHistory.length === 0 ? (
                        <Card className="text-center py-12">
                          <CardContent>
                            <Wrench className="h-12 w-12 mx-auto mb-4 text-muted-foreground" />
                            <h3 className="text-lg font-semibold mb-2">Niciun istoric de service încă</h3>
                            <p className="text-muted-foreground">
                              Serviciile completate prin AutoEase și intrările manuale vor apărea aici
                            </p>
                          </CardContent>
                        </Card>
                      ) : (
                        <div className="space-y-4">
                          {/* Manual History Entries */}
                          {manualHistory.map((entry) => (
                            <Card key={entry.id}>
                              <CardContent className="pt-6">
                                <div className="flex justify-between items-start mb-4">
                                  <div>
                                    <div className="flex items-center gap-2 mb-2">
                                      <h4 className="font-semibold">{entry.title}</h4>
                                      <Badge variant="outline" className="text-xs">Manual</Badge>
                                    </div>
                                    <p className="text-sm text-muted-foreground mb-2">
                                      {entry.description}
                                    </p>
                                    <div className="flex items-center gap-4 text-xs text-muted-foreground">
                                      <span className="flex items-center gap-1">
                                        <Calendar className="h-3 w-3" />
                                        {new Date(entry.date).toLocaleDateString()}
                                      </span>
                                      {entry.mileage && (
                                        <span>{entry.mileage.toLocaleString()} km</span>
                                      )}
                                      {entry.cost && (
                                        <span>{entry.cost} RON</span>
                                      )}
                                      {entry.location && (
                                        <span className="flex items-center gap-1">
                                          <MapPin className="h-3 w-3" />
                                          {entry.location}
                                        </span>
                                      )}
                                    </div>
                                    {entry.notes && (
                                      <p className="text-xs text-muted-foreground mt-2">
                                        Note: {entry.notes}
                                      </p>
                                    )}
                                  </div>
                                  <Badge variant="secondary" className="text-xs">
                                    {entry.type}
                                  </Badge>
                                </div>
                              </CardContent>
                            </Card>
                          ))}
                          
                          {/* Platform Service History */}
                          {serviceHistory.map((service) => (
                            <Card key={service.id}>
                              <CardContent className="pt-6">
                                <div className="flex justify-between items-start mb-4">
                                  <div>
                                    <h4 className="font-semibold">{service.title}</h4>
                                    <p className="text-sm text-muted-foreground mb-2">
                                      {service.description}
                                    </p>
                                    <div className="flex items-center gap-4 text-xs text-muted-foreground">
                                      <span className="flex items-center gap-1">
                                        <Calendar className="h-3 w-3" />
                                        {new Date(service.created_at).toLocaleDateString()}
                                      </span>
                                      <Badge variant={service.status === 'completed' ? 'default' : 'secondary'} className="text-xs">
                                        {service.status}
                                      </Badge>
                                    </div>
                                  </div>
                                </div>

                                {service.quotes && service.quotes.length > 0 && (
                                  <div className="space-y-3">
                                    {service.quotes.map((quote) => (
                                      <div key={quote.id} className="border rounded-lg p-4 space-y-2">
                                        <div className="flex justify-between items-center">
                                          <div>
                                            <p className="font-medium">{quote.garages?.business_name}</p>
                                            <p className="text-sm text-muted-foreground">
                                              Quote: ${quote.price}
                                            </p>
                                          </div>
                                          <Badge variant={quote.status === 'accepted' ? 'default' : 'secondary'}>
                                            {quote.status}
                                          </Badge>
                                        </div>

                                        {quote.bookings && quote.bookings.length > 0 && (
                                          <div className="space-y-2">
                                            {quote.bookings.map((booking) => (
                                              <div key={booking.id} className="bg-muted/50 rounded p-3">
                                                <div className="flex justify-between items-center mb-2">
                                                  <span className="text-sm font-medium">
                                                    Service Date: {new Date(booking.scheduled_date).toLocaleDateString()}
                                                  </span>
                                                  <Badge variant={booking.status === 'completed' ? 'default' : 'secondary'} className="text-xs">
                                                    {booking.status}
                                                  </Badge>
                                                </div>

                                                {booking.reviews && booking.reviews.length > 0 && (
                                                  <div className="space-y-2">
                                                    {booking.reviews.map((review, index) => (
                                                      <div key={index} className="bg-background rounded p-2">
                                                        <div className="flex items-center gap-2 mb-1">
                                                          <div className="flex">
                                                            {Array.from({ length: 5 }).map((_, i) => (
                                                              <Star
                                                                key={i}
                                                                className={`h-3 w-3 ${
                                                                  i < review.rating ? 'text-yellow-400 fill-current' : 'text-gray-300'
                                                                }`}
                                                              />
                                                            ))}
                                                          </div>
                                                          <span className="text-xs text-muted-foreground">
                                                            {review.rating}/5
                                                          </span>
                                                        </div>
                                                        {review.comment && (
                                                          <p className="text-xs text-muted-foreground">
                                                            "{review.comment}"
                                                          </p>
                                                        )}
                                                      </div>
                                                    ))}
                                                  </div>
                                                )}
                                              </div>
                                            ))}
                                          </div>
                                        )}
                                      </div>
                                    ))}
                                  </div>
                                )}
                              </CardContent>
                            </Card>
                          ))}
                        </div>
                      )}
                    </div>
                  </TabsContent>

                  <TabsContent value="documents">
                    <div className="space-y-4">
                      <div className="flex justify-between items-center">
                        <h3 className="text-lg font-semibold">Documente Obligatorii</h3>
                        <Dialog open={isAddingDocument} onOpenChange={setIsAddingDocument}>
                          <DialogTrigger asChild>
                            <Button variant="outline" size="sm">
                              <Plus className="h-4 w-4 mr-2" />
                              Adaugă Document
                            </Button>
                          </DialogTrigger>
                          <DialogContent className="max-w-md">
                            <DialogHeader>
                              <DialogTitle>Adaugă Document</DialogTitle>
                            </DialogHeader>
                            <div className="space-y-4">
                              <div>
                                <Label htmlFor="docType">Tip Document</Label>
                                <Select 
                                  value={newDocument.type} 
                                  onValueChange={(value: any) => setNewDocument({ ...newDocument, type: value })}
                                >
                                  <SelectTrigger>
                                    <SelectValue />
                                  </SelectTrigger>
                                  <SelectContent>
                                    <SelectItem value="asigurare">Asigurare RCA</SelectItem>
                                    <SelectItem value="rovinieta">Rovinietă</SelectItem>
                                    <SelectItem value="itp">ITP</SelectItem>
                                  </SelectContent>
                                </Select>
                              </div>
                              <div>
                                <Label htmlFor="docTitle">Titlu</Label>
                                <Input
                                  id="docTitle"
                                  value={newDocument.title}
                                  onChange={(e) => setNewDocument({ ...newDocument, title: e.target.value })}
                                  placeholder="ex. Asigurare RCA City Insurance"
                                />
                              </div>
                              <div>
                                <Label htmlFor="expiry">Data Expirare</Label>
                                <Input
                                  id="expiry"
                                  type="date"
                                  value={newDocument.expiry_date}
                                  onChange={(e) => setNewDocument({ ...newDocument, expiry_date: e.target.value })}
                                />
                              </div>
                              <div>
                                <Label htmlFor="docNotes">Note (opțional)</Label>
                                <Textarea
                                  id="docNotes"
                                  value={newDocument.notes}
                                  onChange={(e) => setNewDocument({ ...newDocument, notes: e.target.value })}
                                  placeholder="Note suplimentare despre document"
                                />
                              </div>
                              <Button 
                                onClick={handleAddDocument} 
                                className="w-full"
                                disabled={!newDocument.title || !newDocument.expiry_date}
                              >
                                Adaugă Document
                              </Button>
                            </div>
                          </DialogContent>
                        </Dialog>
                      </div>

                      {carDocuments.length === 0 ? (
                        <Card className="text-center py-12">
                          <CardContent>
                            <FileText className="h-12 w-12 mx-auto mb-4 text-muted-foreground" />
                            <h3 className="text-lg font-semibold mb-2">Niciun document adăugat</h3>
                            <p className="text-muted-foreground">
                              Adaugă documentele obligatorii pentru mașina ta
                            </p>
                          </CardContent>
                        </Card>
                      ) : (
                        <div className="grid gap-4">
                          {carDocuments.map((doc) => {
                            const status = getDocumentStatus(doc.expiry_date);
                            return (
                              <Card key={doc.id} className={`border-l-4 ${
                                status.status === 'expired' ? 'border-l-red-500' :
                                status.status === 'expiring' ? 'border-l-yellow-500' :
                                'border-l-green-500'
                              }`}>
                                <CardContent className="pt-6">
                                  <div className="flex justify-between items-start">
                                    <div className="flex-1">
                                      <div className="flex items-center gap-2 mb-2">
                                        <h4 className="font-semibold">{doc.title}</h4>
                                        <Badge variant="outline" className="text-xs">
                                          {doc.type.toUpperCase()}
                                        </Badge>
                                      </div>
                                      <div className="flex items-center gap-2 text-sm text-muted-foreground mb-2">
                                        <Calendar className="h-4 w-4" />
                                        <span>Expiră: {new Date(doc.expiry_date).toLocaleDateString()}</span>
                                      </div>
                                      {doc.notes && (
                                        <p className="text-sm text-muted-foreground">
                                          {doc.notes}
                                        </p>
                                      )}
                                    </div>
                                    <div className="flex items-center gap-2">
                                      {status.status === 'expired' ? (
                                        <div className="flex items-center gap-1 text-red-600">
                                          <AlertCircle className="h-4 w-4" />
                                          <span className="text-xs">Expirat cu {status.days} zile</span>
                                        </div>
                                      ) : status.status === 'expiring' ? (
                                        <div className="flex items-center gap-1 text-yellow-600">
                                          <AlertCircle className="h-4 w-4" />
                                          <span className="text-xs">Expiră în {status.days} zile</span>
                                        </div>
                                      ) : (
                                        <div className="flex items-center gap-1 text-green-600">
                                          <CheckCircle className="h-4 w-4" />
                                          <span className="text-xs">Valid ({status.days} zile)</span>
                                        </div>
                                      )}
                                    </div>
                                  </div>
                                </CardContent>
                              </Card>
                            );
                          })}
                        </div>
                      )}
                    </div>
                  </TabsContent>
                </Tabs>
              )}
            </div>
          </div>
        )}
      </div>
    </Layout>
  );
};

export default MyCars;