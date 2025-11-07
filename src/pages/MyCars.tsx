import { useState, useEffect, useRef } from 'react';
import { useAuth } from '@/hooks/useAuth';
import { useLocation } from 'react-router-dom';
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
  CheckCircle,
  Camera,
  Upload,
  X,
  ExternalLink
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
  const location = useLocation();
  const [cars, setCars] = useState<Car[]>([]);
  const [serviceHistory, setServiceHistory] = useState<ServiceHistory[]>([]);
  const [manualHistory, setManualHistory] = useState<ManualHistoryEntry[]>([]);
  const [carDocuments, setCarDocuments] = useState<CarDocument[]>([]);
  const [carMakes, setCarMakes] = useState<CarMake[]>([]);
  const [carModels, setCarModels] = useState<CarModel[]>([]);
  const [selectedCar, setSelectedCar] = useState<Car | null>(null);
  const [loading, setLoading] = useState(true);
  const [isAddingCar, setIsAddingCar] = useState(false);
  const [isEditingCar, setIsEditingCar] = useState(false);
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
  const [newDocument, setNewDocument] = useState<{
    type: 'asigurare' | 'rovinieta' | 'itp';
    title: string;
    expiry_date: string;
    notes: string;
    file_url?: string;
    file?: File;
  }>({
    type: 'asigurare' as const,
    title: '',
    expiry_date: '',
    notes: '',
    file_url: undefined,
    file: undefined
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

  useEffect(() => {
    // Check if we need to open edit dialog for a specific car
    const state = location.state as { editCarId?: string };
    if (state?.editCarId && cars.length > 0) {
      const carToEdit = cars.find(car => car.id === state.editCarId);
      if (carToEdit) {
        setSelectedCar(carToEdit);
        setIsEditingCar(true);
      }
    }
  }, [location.state, cars]);

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

  const handleAddDocument = async () => {
    if (!selectedCar || !user) return;
    
    try {
      let fileUrl = newDocument.file_url;
      
      // Upload file to Supabase Storage if a file was selected
      if (newDocument.file) {
        const fileExt = newDocument.file.name.split('.').pop();
        const fileName = `${user.id}/${selectedCar.id}/${Date.now()}.${fileExt}`;
        
        const { data: uploadData, error: uploadError } = await supabase.storage
          .from('car-documents')
          .upload(fileName, newDocument.file);

        if (uploadError) {
          throw uploadError;
        }

        // Get public URL
        const { data: { publicUrl } } = supabase.storage
          .from('car-documents')
          .getPublicUrl(fileName);
        
        fileUrl = publicUrl;
      }
      
      const newDoc: CarDocument = {
        id: Date.now().toString(),
        type: newDocument.type,
        title: newDocument.title,
        expiry_date: newDocument.expiry_date,
        file_url: fileUrl,
        notes: newDocument.notes || undefined
      };

      setCarDocuments(prev => [newDoc, ...prev]);
      setIsAddingDocument(false);
      setNewDocument({
        type: 'asigurare',
        title: '',
        expiry_date: '',
        notes: '',
        file_url: undefined,
        file: undefined
      });

      toast({
        title: "Succes",
        description: "Documentul a fost adăugat cu succes",
      });
    } catch (error: any) {
      toast({
        title: "Eroare",
        description: error.message || "Nu s-a putut încărca documentul",
        variant: "destructive",
      });
    }
  };

  const generatePDF = async () => {
    if (!selectedCar) return;

    try {
      const jsPDF = (await import('jspdf')).default;
      const doc = new jsPDF();

      // Load logo
      const logoImg = new Image();
      logoImg.src = '/src/assets/autofix-logo-pdf.png';
      
      await new Promise((resolve) => {
        logoImg.onload = resolve;
      });

      const totalPages = Math.ceil((serviceHistory.length + manualHistory.length) / 8) + 1;

      const addPageElements = (pageNum: number) => {
        // Add logo in bottom right corner
        const logoWidth = 35;
        const logoHeight = 10;
        doc.addImage(logoImg, 'PNG', 160, 280, logoWidth, logoHeight);
        
        // Add page number at bottom center
        doc.setFontSize(9);
        doc.setTextColor(150, 150, 150);
        doc.text(`Pagina ${pageNum} din ${totalPages}`, 105, 290, { align: 'center' });
        doc.setTextColor(0, 0, 0);
      };

      // Modern header with gradient effect
      doc.setFillColor(26, 35, 126);
      doc.rect(0, 0, 210, 45, 'F');
      doc.setFillColor(41, 128, 185);
      doc.rect(0, 0, 210, 40, 'F');
      
      doc.setTextColor(255, 255, 255);
      doc.setFontSize(28);
      doc.setFont('helvetica', 'bold');
      doc.text('ISTORIC VEHICUL', 105, 20, { align: 'center' });
      
      doc.setFontSize(10);
      doc.setFont('helvetica', 'normal');
      doc.text(`Generat: ${new Date().toLocaleDateString('ro-RO')} | AutoFix Report`, 105, 32, { align: 'center' });
      
      doc.setTextColor(0, 0, 0);

      // Vehicle Overview Section - Card Style
      let yPos = 55;
      doc.setFillColor(248, 249, 250);
      doc.roundedRect(15, yPos, 180, 60, 4, 4, 'F');
      
      // Add border accent
      doc.setDrawColor(41, 128, 185);
      doc.setLineWidth(1);
      doc.line(15, yPos + 12, 195, yPos + 12);
      
      doc.setFontSize(14);
      doc.setFont('helvetica', 'bold');
      doc.setTextColor(41, 128, 185);
      doc.text('INFORMAȚII VEHICUL', 20, yPos + 8);
      
      doc.setFontSize(10);
      doc.setFont('helvetica', 'normal');
      doc.setTextColor(50, 50, 50);
      
      // Left column
      doc.setFont('helvetica', 'bold');
      doc.text('Marca & Model:', 20, yPos + 22);
      doc.setFont('helvetica', 'normal');
      doc.text(`${selectedCar.car_makes?.name} ${selectedCar.car_models?.name}`, 20, yPos + 28);
      
      doc.setFont('helvetica', 'bold');
      doc.text('An fabricație:', 20, yPos + 38);
      doc.setFont('helvetica', 'normal');
      doc.text(`${selectedCar.year}`, 20, yPos + 44);
      
      doc.setFont('helvetica', 'bold');
      doc.text('Culoare:', 20, yPos + 50);
      doc.setFont('helvetica', 'normal');
      doc.text(`${selectedCar.color || 'Nespecificat'}`, 20, yPos + 56);
      
      // Right column
      doc.setFont('helvetica', 'bold');
      doc.text('VIN:', 110, yPos + 22);
      doc.setFont('helvetica', 'normal');
      doc.text(`${selectedCar.vin || 'Nespecificat'}`, 110, yPos + 28);
      
      doc.setFont('helvetica', 'bold');
      doc.text('Nr. înmatriculare:', 110, yPos + 38);
      doc.setFont('helvetica', 'normal');
      doc.text(`${selectedCar.license_plate || 'Nespecificat'}`, 110, yPos + 44);
      
      doc.setFont('helvetica', 'bold');
      doc.text('Kilometraj actual:', 110, yPos + 50);
      doc.setFont('helvetica', 'normal');
      doc.text(`${selectedCar.mileage ? `${selectedCar.mileage.toLocaleString()} km` : 'Nespecificat'}`, 110, yPos + 56);

      let yPosition = 125;
      let pageNum = 1;

      // Summary Statistics Card
      const totalServices = serviceHistory.length + manualHistory.length;
      const totalCost = manualHistory.reduce((sum, entry) => sum + (entry.cost || 0), 0);
      
      doc.setFillColor(255, 255, 255);
      doc.setDrawColor(200, 200, 200);
      doc.setLineWidth(0.5);
      doc.roundedRect(15, yPosition, 87, 25, 3, 3, 'FD');
      doc.roundedRect(108, yPosition, 87, 25, 3, 3, 'FD');
      
      // Total services
      doc.setFontSize(10);
      doc.setFont('helvetica', 'normal');
      doc.setTextColor(100, 100, 100);
      doc.text('Servicii totale', 20, yPosition + 8);
      doc.setFontSize(20);
      doc.setFont('helvetica', 'bold');
      doc.setTextColor(41, 128, 185);
      doc.text(totalServices.toString(), 20, yPosition + 20);
      
      // Total cost
      doc.setFontSize(10);
      doc.setFont('helvetica', 'normal');
      doc.setTextColor(100, 100, 100);
      doc.text('Cost total înregistrat', 113, yPosition + 8);
      doc.setFontSize(20);
      doc.setFont('helvetica', 'bold');
      doc.setTextColor(41, 128, 185);
      doc.text(`${totalCost.toLocaleString()} RON`, 113, yPosition + 20);
      
      yPosition += 35;
      doc.setTextColor(0, 0, 0);

      // Service history section - Modern card style
      if (serviceHistory.length > 0) {
        doc.setFillColor(41, 128, 185);
        doc.rect(15, yPosition, 4, 8, 'F');
        
        doc.setFontSize(13);
        doc.setFont('helvetica', 'bold');
        doc.setTextColor(41, 128, 185);
        doc.text('ISTORIC SERVICE PLATFORMĂ', 22, yPosition + 6);
        doc.setTextColor(0, 0, 0);
        yPosition += 15;

        serviceHistory.forEach((service, index) => {
          if (yPosition > 260) {
            addPageElements(pageNum);
            doc.addPage();
            pageNum++;
            yPosition = 20;
          }

          // Service card with shadow effect
          doc.setDrawColor(220, 220, 220);
          doc.setFillColor(255, 255, 255);
          doc.setLineWidth(0.3);
          doc.roundedRect(15, yPosition, 180, 22, 3, 3, 'FD');
          
          // Status badge
          const statusColor = service.status === 'completed' ? [34, 197, 94] : 
                            service.status === 'open' ? [59, 130, 246] : [156, 163, 175];
          doc.setFillColor(statusColor[0], statusColor[1], statusColor[2]);
          doc.roundedRect(170, yPosition + 4, 20, 6, 2, 2, 'F');
          doc.setFontSize(7);
          doc.setTextColor(255, 255, 255);
          doc.text(service.status.toUpperCase(), 180, yPosition + 8, { align: 'center' });
          
          // Service details
          doc.setFontSize(11);
          doc.setFont('helvetica', 'bold');
          doc.setTextColor(30, 30, 30);
          doc.text(`${service.title}`, 20, yPosition + 8);
          
          doc.setFontSize(8);
          doc.setFont('helvetica', 'normal');
          doc.setTextColor(100, 100, 100);
          const serviceDesc = service.description.length > 80 ? 
                             service.description.substring(0, 80) + '...' : 
                             service.description;
          doc.text(serviceDesc, 20, yPosition + 14);
          
          doc.setFontSize(7);
          doc.text(`Data: ${new Date(service.created_at).toLocaleDateString('ro-RO')}`, 20, yPosition + 19);
          
          yPosition += 27;
        });
      }

      // Manual history section - Modern card style
      if (manualHistory.length > 0) {
        if (yPosition > 235) {
          addPageElements(pageNum);
          doc.addPage();
          pageNum++;
          yPosition = 20;
        } else {
          yPosition += 5;
        }

        doc.setFillColor(41, 128, 185);
        doc.rect(15, yPosition, 4, 8, 'F');
        
        doc.setFontSize(13);
        doc.setFont('helvetica', 'bold');
        doc.setTextColor(41, 128, 185);
        doc.text('ISTORIC MANUAL', 22, yPosition + 6);
        doc.setTextColor(0, 0, 0);
        yPosition += 15;

        manualHistory.forEach((entry, index) => {
          if (yPosition > 255) {
            addPageElements(pageNum);
            doc.addPage();
            pageNum++;
            yPosition = 20;
          }

          const entryHeight = 28;
          
          // Entry card with shadow effect
          doc.setDrawColor(220, 220, 220);
          doc.setFillColor(255, 255, 255);
          doc.setLineWidth(0.3);
          doc.roundedRect(15, yPosition, 180, entryHeight, 3, 3, 'FD');
          
          // Type badge
          const typeColors: any = {
            service: [59, 130, 246],
            maintenance: [34, 197, 94],
            repair: [239, 68, 68],
            inspection: [168, 85, 247]
          };
          const badgeColor = typeColors[entry.type] || [156, 163, 175];
          doc.setFillColor(badgeColor[0], badgeColor[1], badgeColor[2]);
          doc.roundedRect(170, yPosition + 4, 20, 6, 2, 2, 'F');
          doc.setFontSize(7);
          doc.setTextColor(255, 255, 255);
          doc.text(entry.type.toUpperCase(), 180, yPosition + 8, { align: 'center' });
          
          // Entry details
          doc.setFontSize(11);
          doc.setFont('helvetica', 'bold');
          doc.setTextColor(30, 30, 30);
          doc.text(`${entry.title}`, 20, yPosition + 8);
          
          doc.setFontSize(8);
          doc.setFont('helvetica', 'normal');
          doc.setTextColor(100, 100, 100);
          const desc = entry.description.length > 80 ? 
                      entry.description.substring(0, 80) + '...' : 
                      entry.description;
          doc.text(desc, 20, yPosition + 14);
          
          // Metadata row
          doc.setFontSize(7);
          let metaText = `Data: ${new Date(entry.date).toLocaleDateString('ro-RO')}`;
          if (entry.mileage) metaText += ` | Kilometraj: ${entry.mileage.toLocaleString()} km`;
          if (entry.location) metaText += ` | ${entry.location}`;
          doc.text(metaText, 20, yPosition + 20);
          
          // Cost highlight
          if (entry.cost) {
            doc.setFontSize(10);
            doc.setFont('helvetica', 'bold');
            doc.setTextColor(41, 128, 185);
            doc.text(`${entry.cost} RON`, 20, yPosition + 26);
          }
          
          doc.setTextColor(0, 0, 0);
          yPosition += entryHeight + 5;
        });
      }

      // Add page elements to last page
      addPageElements(pageNum);

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
                          <Dialog open={isEditingCar} onOpenChange={(open) => {
                            setIsEditingCar(open);
                            if (open && selectedCar) {
                              setNewCar({
                                make_id: selectedCar.make_id,
                                model_id: selectedCar.model_id,
                                year: selectedCar.year,
                                color: selectedCar.color || '',
                                license_plate: selectedCar.license_plate || '',
                                mileage: selectedCar.mileage?.toString() || '',
                                vin: selectedCar.vin || ''
                              });
                              fetchCarModels(selectedCar.make_id);
                            }
                          }}>
                            <DialogTrigger asChild>
                              <Button variant="outline" size="sm">
                                <Edit className="h-4 w-4 mr-2" />
                                Edit
                              </Button>
                            </DialogTrigger>
                            <DialogContent className="max-w-md">
                              <DialogHeader>
                                <DialogTitle>Editează Mașina</DialogTitle>
                              </DialogHeader>
                              <div className="space-y-4">
                                <div>
                                  <Label htmlFor="edit-make">Make</Label>
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
                                  <Label htmlFor="edit-model">Model</Label>
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
                                  <Label htmlFor="edit-year">Year</Label>
                                  <Input
                                    id="edit-year"
                                    type="number"
                                    value={newCar.year}
                                    onChange={(e) => setNewCar({ ...newCar, year: parseInt(e.target.value) })}
                                    min="1900"
                                    max={new Date().getFullYear() + 1}
                                  />
                                </div>

                                <div>
                                  <Label htmlFor="edit-color">Color (Optional)</Label>
                                  <Input
                                    id="edit-color"
                                    value={newCar.color}
                                    onChange={(e) => setNewCar({ ...newCar, color: e.target.value })}
                                    placeholder="e.g. Red, Blue, White"
                                  />
                                </div>

                                <div>
                                  <Label htmlFor="edit-license_plate">Număr de Înmatriculare (Optional)</Label>
                                  <Input
                                    id="edit-license_plate"
                                    value={newCar.license_plate}
                                    onChange={(e) => setNewCar({ ...newCar, license_plate: e.target.value })}
                                    placeholder="e.g. ABC-123"
                                  />
                                </div>

                                <div>
                                  <Label htmlFor="edit-mileage">Kilometraj Curent (Optional)</Label>
                                  <Input
                                    id="edit-mileage"
                                    type="number"
                                    value={newCar.mileage}
                                    onChange={(e) => setNewCar({ ...newCar, mileage: e.target.value })}
                                    placeholder="e.g. 50000"
                                  />
                                </div>

                                <div>
                                  <Label htmlFor="edit-vin">VIN (Optional)</Label>
                                  <Input
                                    id="edit-vin"
                                    value={newCar.vin}
                                    onChange={(e) => setNewCar({ ...newCar, vin: e.target.value })}
                                    placeholder="17-character VIN"
                                    maxLength={17}
                                  />
                                </div>

                                <Button 
                                  onClick={async () => {
                                    try {
                                      const { error } = await supabase
                                        .from('cars')
                                        .update({
                                          make_id: newCar.make_id,
                                          model_id: newCar.model_id,
                                          year: newCar.year,
                                          color: newCar.color || null,
                                          license_plate: newCar.license_plate || null,
                                          mileage: newCar.mileage ? parseInt(newCar.mileage) : null,
                                          vin: newCar.vin || null
                                        })
                                        .eq('id', selectedCar?.id);

                                      if (error) throw error;

                                      toast({
                                        title: "Succes",
                                        description: "Mașina a fost actualizată cu succes",
                                      });

                                      setIsEditingCar(false);
                                      fetchCars();
                                    } catch (error) {
                                      toast({
                                        title: "Eroare",
                                        description: "Nu s-a putut actualiza mașina",
                                        variant: "destructive",
                                      });
                                    }
                                  }} 
                                  className="w-full"
                                  disabled={!newCar.make_id || !newCar.model_id}
                                >
                                  Salvează Modificările
                                </Button>
                              </div>
                            </DialogContent>
                          </Dialog>
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
                          <Button 
                            variant="outline" 
                            size="sm" 
                            onClick={() => window.open(`https://www.carvertical.ro/?vin=${selectedCar.vin || ''}`, '_blank')}
                            disabled={!selectedCar.vin}
                          >
                            <ExternalLink className="h-4 w-4 mr-2" />
                            Raport CarVertical
                          </Button>
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
                              Serviciile completate prin AutoFix și intrările manuale vor apărea aici
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
                          <DialogContent className="max-w-md max-h-[90vh] overflow-y-auto">
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
                              
                              {/* File Upload Section */}
                              <div className="space-y-3 border rounded-lg p-4 bg-muted/30">
                                <Label>Fotografie Document</Label>
                                <div className="grid grid-cols-2 gap-2">
                                  <label htmlFor="file-upload" className="cursor-pointer">
                                    <div className="flex flex-col items-center justify-center p-4 border-2 border-dashed rounded-lg hover:bg-muted/50 transition-colors">
                                      <Upload className="h-6 w-6 mb-2 text-muted-foreground" />
                                      <span className="text-xs text-center text-muted-foreground">
                                        Încarcă din Galerie
                                      </span>
                                    </div>
                                    <input
                                      id="file-upload"
                                      type="file"
                                      accept="image/*"
                                      className="hidden"
                                      onChange={(e) => {
                                        const file = e.target.files?.[0];
                                        if (file) {
                                          setNewDocument({ 
                                            ...newDocument, 
                                            file_url: URL.createObjectURL(file),
                                            file: file
                                          });
                                        }
                                      }}
                                    />
                                  </label>
                                  
                                  <label htmlFor="camera-capture" className="cursor-pointer">
                                    <div className="flex flex-col items-center justify-center p-4 border-2 border-dashed rounded-lg hover:bg-muted/50 transition-colors">
                                      <Camera className="h-6 w-6 mb-2 text-muted-foreground" />
                                      <span className="text-xs text-center text-muted-foreground">
                                        Fotografiază
                                      </span>
                                    </div>
                                    <input
                                      id="camera-capture"
                                      type="file"
                                      accept="image/*"
                                      capture="environment"
                                      className="hidden"
                                      onChange={(e) => {
                                        const file = e.target.files?.[0];
                                        if (file) {
                                          setNewDocument({ 
                                            ...newDocument, 
                                            file_url: URL.createObjectURL(file),
                                            file: file
                                          });
                                        }
                                      }}
                                    />
                                  </label>
                                </div>
                                
                                {newDocument.file_url && (
                                  <div className="relative mt-3">
                                    <img 
                                      src={newDocument.file_url} 
                                      alt="Document preview" 
                                      className="w-full h-48 object-cover rounded-lg"
                                    />
                                    <Button
                                      variant="destructive"
                                      size="icon"
                                      className="absolute top-2 right-2"
                                      onClick={() => setNewDocument({ 
                                        ...newDocument, 
                                        file_url: undefined,
                                        file: undefined
                                      })}
                                    >
                                      <X className="h-4 w-4" />
                                    </Button>
                                  </div>
                                )}
                                <p className="text-xs text-muted-foreground">
                                  Încarcă o fotografie a documentului (opțional)
                                </p>
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
                                  <div className="flex justify-between items-start gap-4">
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
                                      {doc.file_url && (
                                        <div className="mt-3">
                                          <img 
                                            src={doc.file_url} 
                                            alt={doc.title}
                                            className="w-48 h-32 object-cover rounded-lg border cursor-pointer hover:opacity-80 transition-opacity"
                                            onClick={() => window.open(doc.file_url, '_blank')}
                                          />
                                        </div>
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