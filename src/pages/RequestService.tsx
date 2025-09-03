import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Layout } from '@/components/Layout';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { useAuth } from '@/hooks/useAuth';
import { supabase } from '@/integrations/supabase/client';
import { useToast } from '@/hooks/use-toast';
import { ArrowLeft, Car, MapPin, Calendar, DollarSign, Search, Wrench, Disc, Settings, Zap, Circle, Truck, Cog, Thermometer, Hammer, SearchIcon } from 'lucide-react';

interface Car {
  id: string;
  make_id: string;
  model_id: string;
  year: number;
  color?: string;
  license_plate?: string;
}

interface ServiceCategory {
  id: string;
  name: string;
  description?: string;
  icon?: string;
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

export default function RequestService() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const { toast } = useToast();
  const [loading, setLoading] = useState(false);
  const [cars, setCars] = useState<Car[]>([]);
  const [categories, setCategories] = useState<ServiceCategory[]>([]);
  const [makes, setMakes] = useState<CarMake[]>([]);
  const [models, setModels] = useState<CarModel[]>([]);
  const [showAddCar, setShowAddCar] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [step, setStep] = useState(1); // 1: Select Service, 2: Fill Details

  const [formData, setFormData] = useState({
    car_id: '',
    category_id: '',
    title: '',
    description: '',
    urgency: 'medium',
    budget_min: '',
    budget_max: '',
    preferred_date: '',
    location_address: '',
    location_city: '',
    location_state: ''
  });

  const [newCar, setNewCar] = useState({
    make_id: '',
    model_id: '',
    year: '',
    color: '',
    license_plate: ''
  });

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      const [carsRes, categoriesRes, makesRes, modelsRes] = await Promise.all([
        supabase.from('cars').select('*').eq('user_id', user?.id),
        supabase.from('service_categories').select('*'),
        supabase.from('car_makes').select('*'),
        supabase.from('car_models').select('*')
      ]);

      if (carsRes.data) setCars(carsRes.data);
      if (categoriesRes.data) setCategories(categoriesRes.data);
      if (makesRes.data) setMakes(makesRes.data);
      if (modelsRes.data) setModels(modelsRes.data);
    } catch (error) {
      console.error('Error fetching data:', error);
    }
  };

  const handleAddCar = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      const { data, error } = await supabase
        .from('cars')
        .insert([{
          ...newCar,
          year: parseInt(newCar.year),
          user_id: user?.id
        }])
        .select()
        .single();

      if (error) throw error;

      setCars([...cars, data]);
      setFormData({ ...formData, car_id: data.id });
      setShowAddCar(false);
      setNewCar({ make_id: '', model_id: '', year: '', color: '', license_plate: '' });
      
      toast({
        title: "Success",
        description: "Car added successfully!"
      });
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

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      const { error } = await supabase
        .from('job_requests')
        .insert([{
          ...formData,
          user_id: user?.id,
          budget_min: formData.budget_min ? parseFloat(formData.budget_min) : null,
          budget_max: formData.budget_max ? parseFloat(formData.budget_max) : null,
          preferred_date: formData.preferred_date || null
        }]);

      if (error) throw error;

      toast({
        title: "Success",
        description: "Service request submitted! Garages will start sending you quotes soon."
      });

      navigate('/');
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

  const filteredModels = models.filter(model => model.make_id === newCar.make_id);
  const filteredCategories = categories.filter(category => 
    category.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    (category.description && category.description.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  const popularServices = ['Oil Change', 'Brake Service', 'Tire Service', 'Engine Repair'];
  const popularCategories = categories.filter(cat => popularServices.includes(cat.name));
  const otherCategories = categories.filter(cat => !popularServices.includes(cat.name));

  const getIconForCategory = (iconName: string) => {
    const iconMap: Record<string, any> = {
      wrench: Wrench,
      disc: Disc,
      engine: Settings,
      zap: Zap,
      circle: Circle,
      truck: Truck,
      gear: Cog,
      thermometer: Thermometer,
      hammer: Hammer,
      search: SearchIcon
    };
    const IconComponent = iconMap[iconName] || Wrench;
    return <IconComponent className="h-6 w-6" />;
  };

  return (
    <Layout>
      <div className="max-w-4xl mx-auto space-y-6">
        <div className="flex items-center gap-4">
          <Button variant="outline" size="sm" onClick={() => step === 2 ? setStep(1) : navigate('/')}>
            <ArrowLeft className="h-4 w-4 mr-2" />
            Back
          </Button>
          <div>
            <h1 className="text-3xl font-bold">Request Service</h1>
            <p className="text-muted-foreground">Get quotes from local garages</p>
          </div>
        </div>

        {/* Progress Steps */}
        <div className="flex items-center justify-center space-x-4 mb-8">
          <div className={`flex items-center space-x-2 ${step >= 1 ? 'text-primary' : 'text-muted-foreground'}`}>
            <div className={`w-8 h-8 rounded-full flex items-center justify-center ${step >= 1 ? 'bg-primary text-primary-foreground' : 'bg-muted'}`}>
              1
            </div>
            <span className="font-medium">Your Job</span>
          </div>
          <div className="w-20 h-px bg-border"></div>
          <div className={`flex items-center space-x-2 ${step >= 2 ? 'text-primary' : 'text-muted-foreground'}`}>
            <div className={`w-8 h-8 rounded-full flex items-center justify-center ${step >= 2 ? 'bg-primary text-primary-foreground' : 'bg-muted'}`}>
              2
            </div>
            <span className="font-medium">Details</span>
          </div>
        </div>

        {step === 1 && (
          <>
            {/* Search Bar */}
            <div className="relative mb-8">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-muted-foreground h-4 w-4" />
              <Input
                placeholder="Search for service"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-10 h-12 text-lg"
              />
            </div>

            {!searchTerm && (
              <>
                {/* Most Popular Section */}
                <div className="mb-8">
                  <h2 className="text-xl font-semibold mb-4">Most popular</h2>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {popularCategories.map((category) => (
                      <Card key={category.id} className="cursor-pointer hover:shadow-md transition-shadow" onClick={() => {
                        setFormData({ ...formData, category_id: category.id });
                        setStep(2);
                      }}>
                        <CardContent className="flex items-center p-6">
                          <div className="flex items-center justify-center w-12 h-12 bg-primary/10 rounded-lg mr-4">
                            {getIconForCategory(category.icon || 'wrench')}
                          </div>
                          <div className="flex-1">
                            <h3 className="font-medium text-lg">{category.name}</h3>
                            {category.description && (
                              <p className="text-sm text-muted-foreground">{category.description}</p>
                            )}
                          </div>
                          <ArrowLeft className="h-5 w-5 rotate-180 text-muted-foreground" />
                        </CardContent>
                      </Card>
                    ))}
                  </div>
                </div>

                {/* Other Services */}
                {otherCategories.length > 0 && (
                  <div>
                    <h2 className="text-xl font-semibold mb-4">Other</h2>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      {otherCategories.map((category) => (
                        <Card key={category.id} className="cursor-pointer hover:shadow-md transition-shadow" onClick={() => {
                          setFormData({ ...formData, category_id: category.id });
                          setStep(2);
                        }}>
                          <CardContent className="flex items-center p-6">
                            <div className="flex items-center justify-center w-12 h-12 bg-primary/10 rounded-lg mr-4">
                              {getIconForCategory(category.icon || 'wrench')}
                            </div>
                            <div className="flex-1">
                              <h3 className="font-medium text-lg">{category.name}</h3>
                              {category.description && (
                                <p className="text-sm text-muted-foreground">{category.description}</p>
                              )}
                            </div>
                            <ArrowLeft className="h-5 w-5 rotate-180 text-muted-foreground" />
                          </CardContent>
                        </Card>
                      ))}
                    </div>
                  </div>
                )}
              </>
            )}

            {searchTerm && (
              <div>
                <h2 className="text-xl font-semibold mb-4">Search Results</h2>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {filteredCategories.map((category) => (
                    <Card key={category.id} className="cursor-pointer hover:shadow-md transition-shadow" onClick={() => {
                      setFormData({ ...formData, category_id: category.id });
                      setStep(2);
                    }}>
                      <CardContent className="flex items-center p-6">
                        <div className="flex items-center justify-center w-12 h-12 bg-primary/10 rounded-lg mr-4">
                          {getIconForCategory(category.icon || 'wrench')}
                        </div>
                        <div className="flex-1">
                          <h3 className="font-medium text-lg">{category.name}</h3>
                          {category.description && (
                            <p className="text-sm text-muted-foreground">{category.description}</p>
                          )}
                        </div>
                        <ArrowLeft className="h-5 w-5 rotate-180 text-muted-foreground" />
                      </CardContent>
                    </Card>
                  ))}
                </div>
                {filteredCategories.length === 0 && (
                  <div className="text-center py-8">
                    <p className="text-muted-foreground">No services found matching "{searchTerm}"</p>
                  </div>
                )}
              </div>
            )}
          </>
        )}

        {step === 2 && (
          <>
            {cars.length === 0 && !showAddCar && (
              <Card>
                <CardContent className="p-6 text-center">
                  <Car className="h-12 w-12 mx-auto mb-4 text-muted-foreground" />
                  <h3 className="text-lg font-medium mb-2">No cars added yet</h3>
                  <p className="text-muted-foreground mb-4">Add your car to request services</p>
                  <Button onClick={() => setShowAddCar(true)}>
                    Add Your Car
                  </Button>
                </CardContent>
              </Card>
            )}

            {showAddCar && (
              <Card>
                <CardHeader>
                  <CardTitle>Add Your Car</CardTitle>
                </CardHeader>
                <CardContent>
                  <form onSubmit={handleAddCar} className="space-y-4">
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <Label htmlFor="make">Make</Label>
                        <Select value={newCar.make_id} onValueChange={(value) => setNewCar({ ...newCar, make_id: value, model_id: '' })}>
                          <SelectTrigger>
                            <SelectValue placeholder="Select make" />
                          </SelectTrigger>
                          <SelectContent>
                            {makes.map((make) => (
                              <SelectItem key={make.id} value={make.id}>{make.name}</SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </div>
                      <div>
                        <Label htmlFor="model">Model</Label>
                        <Select value={newCar.model_id} onValueChange={(value) => setNewCar({ ...newCar, model_id: value })} disabled={!newCar.make_id}>
                          <SelectTrigger>
                            <SelectValue placeholder="Select model" />
                          </SelectTrigger>
                          <SelectContent>
                            {filteredModels.map((model) => (
                              <SelectItem key={model.id} value={model.id}>{model.name}</SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </div>
                    </div>
                    <div className="grid grid-cols-3 gap-4">
                      <div>
                        <Label htmlFor="year">Year</Label>
                        <Input
                          type="number"
                          min="1900"
                          max="2025"
                          value={newCar.year}
                          onChange={(e) => setNewCar({ ...newCar, year: e.target.value })}
                          required
                        />
                      </div>
                      <div>
                        <Label htmlFor="color">Color</Label>
                        <Input
                          value={newCar.color}
                          onChange={(e) => setNewCar({ ...newCar, color: e.target.value })}
                          placeholder="e.g. Blue"
                        />
                      </div>
                      <div>
                        <Label htmlFor="license_plate">License Plate</Label>
                        <Input
                          value={newCar.license_plate}
                          onChange={(e) => setNewCar({ ...newCar, license_plate: e.target.value })}
                          placeholder="ABC123"
                        />
                      </div>
                    </div>
                    <div className="flex gap-2">
                      <Button type="submit" disabled={loading}>
                        {loading ? 'Adding...' : 'Add Car'}
                      </Button>
                      <Button type="button" variant="outline" onClick={() => setShowAddCar(false)}>
                        Cancel
                      </Button>
                    </div>
                  </form>
                </CardContent>
              </Card>
            )}

            {cars.length > 0 && (
              <Card>
                <CardHeader>
                  <CardTitle>Service Request Details</CardTitle>
                  <p className="text-sm text-muted-foreground">
                    Selected service: {categories.find(c => c.id === formData.category_id)?.name}
                  </p>
                </CardHeader>
                <CardContent>
                  <form onSubmit={handleSubmit} className="space-y-6">
                    <div>
                      <Label htmlFor="car">Select Your Car</Label>
                      <Select value={formData.car_id} onValueChange={(value) => setFormData({ ...formData, car_id: value })}>
                        <SelectTrigger>
                          <SelectValue placeholder="Choose your car" />
                        </SelectTrigger>
                        <SelectContent>
                          {cars.map((car) => {
                            const make = makes.find(m => m.id === car.make_id);
                            const model = models.find(m => m.id === car.model_id);
                            return (
                              <SelectItem key={car.id} value={car.id}>
                                {car.year} {make?.name} {model?.name} {car.color && `(${car.color})`}
                              </SelectItem>
                            );
                          })}
                        </SelectContent>
                      </Select>
                      <Button type="button" variant="link" className="p-0 h-auto mt-1" onClick={() => setShowAddCar(true)}>
                        + Add another car
                      </Button>
                    </div>

                    <div>
                      <Label htmlFor="title">Service Title</Label>
                      <Input
                        value={formData.title}
                        onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                        placeholder="e.g. Brake pads replacement"
                        required
                      />
                    </div>

                    <div>
                      <Label htmlFor="description">Description</Label>
                      <Textarea
                        value={formData.description}
                        onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                        placeholder="Describe the problem or service needed in detail..."
                        rows={4}
                        required
                      />
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <Label htmlFor="urgency">Urgency</Label>
                        <Select value={formData.urgency} onValueChange={(value) => setFormData({ ...formData, urgency: value })}>
                          <SelectTrigger>
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="low">Low - When convenient</SelectItem>
                            <SelectItem value="medium">Medium - This week</SelectItem>
                            <SelectItem value="high">High - ASAP</SelectItem>
                          </SelectContent>
                        </Select>
                      </div>
                      <div>
                        <Label htmlFor="preferred_date">Preferred Date</Label>
                        <Input
                          type="date"
                          value={formData.preferred_date}
                          onChange={(e) => setFormData({ ...formData, preferred_date: e.target.value })}
                        />
                      </div>
                    </div>

                    <div>
                      <Label>Budget Range (Optional)</Label>
                      <div className="grid grid-cols-2 gap-4">
                        <div>
                          <Input
                            type="number"
                            placeholder="Min $"
                            value={formData.budget_min}
                            onChange={(e) => setFormData({ ...formData, budget_min: e.target.value })}
                          />
                        </div>
                        <div>
                          <Input
                            type="number"
                            placeholder="Max $"
                            value={formData.budget_max}
                            onChange={(e) => setFormData({ ...formData, budget_max: e.target.value })}
                          />
                        </div>
                      </div>
                    </div>

                    <div className="space-y-4">
                      <Label>Location</Label>
                      <Input
                        placeholder="Street address"
                        value={formData.location_address}
                        onChange={(e) => setFormData({ ...formData, location_address: e.target.value })}
                      />
                      <div className="grid grid-cols-2 gap-4">
                        <Input
                          placeholder="City"
                          value={formData.location_city}
                          onChange={(e) => setFormData({ ...formData, location_city: e.target.value })}
                        />
                        <Input
                          placeholder="State"
                          value={formData.location_state}
                          onChange={(e) => setFormData({ ...formData, location_state: e.target.value })}
                        />
                      </div>
                    </div>

                    <Button type="submit" className="w-full" disabled={loading || !formData.car_id || !formData.category_id}>
                      {loading ? 'Submitting...' : 'Request Service'}
                    </Button>
                  </form>
                </CardContent>
              </Card>
            )}
          </>
        )}
      </div>
    </Layout>
  );
}
