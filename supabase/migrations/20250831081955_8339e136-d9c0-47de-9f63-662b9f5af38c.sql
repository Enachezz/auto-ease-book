-- Create user types enum
CREATE TYPE user_type AS ENUM ('car_owner', 'garage', 'admin');

-- Create user profiles table
CREATE TABLE public.profiles (
  id UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL UNIQUE,
  user_type user_type NOT NULL DEFAULT 'car_owner',
  email TEXT NOT NULL,
  phone TEXT,
  full_name TEXT NOT NULL,
  avatar_url TEXT,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Create garages table
CREATE TABLE public.garages (
  id UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL UNIQUE,
  business_name TEXT NOT NULL,
  address TEXT NOT NULL,
  city TEXT NOT NULL,
  state TEXT NOT NULL,
  postal_code TEXT NOT NULL,
  latitude DECIMAL(10, 8),
  longitude DECIMAL(11, 8),
  phone TEXT NOT NULL,
  description TEXT,
  services TEXT[] DEFAULT '{}',
  opening_hours JSONB,
  is_approved BOOLEAN DEFAULT false,
  average_rating DECIMAL(3, 2) DEFAULT 0.00,
  total_reviews INTEGER DEFAULT 0,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Create service categories table
CREATE TABLE public.service_categories (
  id UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
  name TEXT NOT NULL UNIQUE,
  description TEXT,
  icon TEXT,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Create car makes table
CREATE TABLE public.car_makes (
  id UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
  name TEXT NOT NULL UNIQUE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Create car models table
CREATE TABLE public.car_models (
  id UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
  make_id UUID REFERENCES public.car_makes(id) ON DELETE CASCADE NOT NULL,
  name TEXT NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  UNIQUE(make_id, name)
);

-- Create cars table
CREATE TABLE public.cars (
  id UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  make_id UUID REFERENCES public.car_makes(id) NOT NULL,
  model_id UUID REFERENCES public.car_models(id) NOT NULL,
  year INTEGER NOT NULL,
  color TEXT,
  license_plate TEXT,
  vin TEXT,
  mileage INTEGER,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Create job requests table
CREATE TABLE public.job_requests (
  id UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  car_id UUID REFERENCES public.cars(id) ON DELETE CASCADE NOT NULL,
  category_id UUID REFERENCES public.service_categories(id) NOT NULL,
  title TEXT NOT NULL,
  description TEXT NOT NULL,
  urgency TEXT CHECK (urgency IN ('low', 'medium', 'high')) DEFAULT 'medium',
  preferred_date DATE,
  budget_min DECIMAL(10, 2),
  budget_max DECIMAL(10, 2),
  status TEXT CHECK (status IN ('open', 'quoted', 'booked', 'in_progress', 'completed', 'cancelled')) DEFAULT 'open',
  location_address TEXT,
  location_city TEXT,
  location_state TEXT,
  location_latitude DECIMAL(10, 8),
  location_longitude DECIMAL(11, 8),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Create quotes table
CREATE TABLE public.quotes (
  id UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
  job_request_id UUID REFERENCES public.job_requests(id) ON DELETE CASCADE NOT NULL,
  garage_id UUID REFERENCES public.garages(id) ON DELETE CASCADE NOT NULL,
  price DECIMAL(10, 2) NOT NULL,
  estimated_duration TEXT,
  description TEXT,
  warranty_info TEXT,
  status TEXT CHECK (status IN ('pending', 'accepted', 'rejected', 'expired')) DEFAULT 'pending',
  expires_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  UNIQUE(job_request_id, garage_id)
);

-- Create bookings table
CREATE TABLE public.bookings (
  id UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
  quote_id UUID REFERENCES public.quotes(id) ON DELETE CASCADE NOT NULL UNIQUE,
  scheduled_date DATE NOT NULL,
  scheduled_time TIME NOT NULL,
  status TEXT CHECK (status IN ('confirmed', 'in_progress', 'completed', 'cancelled')) DEFAULT 'confirmed',
  notes TEXT,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Create reviews table
CREATE TABLE public.reviews (
  id UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
  booking_id UUID REFERENCES public.bookings(id) ON DELETE CASCADE NOT NULL UNIQUE,
  garage_id UUID REFERENCES public.garages(id) ON DELETE CASCADE NOT NULL,
  user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
  rating INTEGER CHECK (rating >= 1 AND rating <= 5) NOT NULL,
  comment TEXT,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Enable Row Level Security
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.garages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.service_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.car_makes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.car_models ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.cars ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.job_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.quotes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.bookings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reviews ENABLE ROW LEVEL SECURITY;

-- Create RLS policies for profiles
CREATE POLICY "Users can view their own profile" ON public.profiles FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can update their own profile" ON public.profiles FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY "Users can insert their own profile" ON public.profiles FOR INSERT WITH CHECK (auth.uid() = user_id);

-- Create RLS policies for garages
CREATE POLICY "Anyone can view approved garages" ON public.garages FOR SELECT USING (is_approved = true OR auth.uid() = user_id);
CREATE POLICY "Garage owners can update their garage" ON public.garages FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY "Users can insert their garage" ON public.garages FOR INSERT WITH CHECK (auth.uid() = user_id);

-- Create RLS policies for service categories (public read)
CREATE POLICY "Anyone can view service categories" ON public.service_categories FOR SELECT USING (true);

-- Create RLS policies for car makes and models (public read)
CREATE POLICY "Anyone can view car makes" ON public.car_makes FOR SELECT USING (true);
CREATE POLICY "Anyone can view car models" ON public.car_models FOR SELECT USING (true);

-- Create RLS policies for cars
CREATE POLICY "Users can view their own cars" ON public.cars FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Users can insert their own cars" ON public.cars FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can update their own cars" ON public.cars FOR UPDATE USING (auth.uid() = user_id);
CREATE POLICY "Users can delete their own cars" ON public.cars FOR DELETE USING (auth.uid() = user_id);

-- Create RLS policies for job requests
CREATE POLICY "Users can view their own job requests" ON public.job_requests FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "Garages can view open job requests" ON public.job_requests FOR SELECT USING (
  EXISTS (SELECT 1 FROM public.profiles WHERE user_id = auth.uid() AND user_type = 'garage') 
  AND status = 'open'
);
CREATE POLICY "Users can insert their own job requests" ON public.job_requests FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "Users can update their own job requests" ON public.job_requests FOR UPDATE USING (auth.uid() = user_id);

-- Create RLS policies for quotes
CREATE POLICY "Job owners can view quotes for their requests" ON public.quotes FOR SELECT USING (
  EXISTS (SELECT 1 FROM public.job_requests WHERE id = job_request_id AND user_id = auth.uid())
);
CREATE POLICY "Garage owners can view their own quotes" ON public.quotes FOR SELECT USING (
  EXISTS (SELECT 1 FROM public.garages WHERE id = garage_id AND user_id = auth.uid())
);
CREATE POLICY "Garages can insert quotes" ON public.quotes FOR INSERT WITH CHECK (
  EXISTS (SELECT 1 FROM public.garages WHERE id = garage_id AND user_id = auth.uid())
);
CREATE POLICY "Garages can update their own quotes" ON public.quotes FOR UPDATE USING (
  EXISTS (SELECT 1 FROM public.garages WHERE id = garage_id AND user_id = auth.uid())
);

-- Create RLS policies for bookings
CREATE POLICY "Users can view bookings for their quotes" ON public.bookings FOR SELECT USING (
  EXISTS (
    SELECT 1 FROM public.quotes q 
    JOIN public.job_requests jr ON q.job_request_id = jr.id 
    WHERE q.id = quote_id AND jr.user_id = auth.uid()
  ) OR EXISTS (
    SELECT 1 FROM public.quotes q 
    JOIN public.garages g ON q.garage_id = g.id 
    WHERE q.id = quote_id AND g.user_id = auth.uid()
  )
);
CREATE POLICY "Users can insert bookings for accepted quotes" ON public.bookings FOR INSERT WITH CHECK (
  EXISTS (
    SELECT 1 FROM public.quotes q 
    JOIN public.job_requests jr ON q.job_request_id = jr.id 
    WHERE q.id = quote_id AND jr.user_id = auth.uid() AND q.status = 'accepted'
  )
);

-- Create RLS policies for reviews
CREATE POLICY "Anyone can view reviews" ON public.reviews FOR SELECT USING (true);
CREATE POLICY "Users can insert reviews for their bookings" ON public.reviews FOR INSERT WITH CHECK (
  EXISTS (
    SELECT 1 FROM public.bookings b 
    JOIN public.quotes q ON b.quote_id = q.id 
    JOIN public.job_requests jr ON q.job_request_id = jr.id 
    WHERE b.id = booking_id AND jr.user_id = auth.uid()
  )
);

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION public.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create triggers for updated_at
CREATE TRIGGER update_profiles_updated_at BEFORE UPDATE ON public.profiles FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_garages_updated_at BEFORE UPDATE ON public.garages FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_cars_updated_at BEFORE UPDATE ON public.cars FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_job_requests_updated_at BEFORE UPDATE ON public.job_requests FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_quotes_updated_at BEFORE UPDATE ON public.quotes FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();
CREATE TRIGGER update_bookings_updated_at BEFORE UPDATE ON public.bookings FOR EACH ROW EXECUTE FUNCTION public.update_updated_at_column();

-- Create function to handle new user registration
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO public.profiles (user_id, email, full_name, user_type)
  VALUES (
    NEW.id,
    NEW.email,
    COALESCE(NEW.raw_user_meta_data ->> 'full_name', NEW.email),
    COALESCE(NEW.raw_user_meta_data ->> 'user_type', 'car_owner')::user_type
  );
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Create trigger for new user registration
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- Insert initial service categories
INSERT INTO public.service_categories (name, description, icon) VALUES
('Oil Change', 'Regular oil and filter changes', 'wrench'),
('Brake Service', 'Brake pad and rotor replacement', 'disc'),
('Engine Repair', 'Engine diagnostics and repair', 'engine'),
('Transmission', 'Transmission service and repair', 'gear'),
('Electrical', 'Electrical system diagnostics', 'zap'),
('AC/Heating', 'Climate control system service', 'thermometer'),
('Tire Service', 'Tire rotation, balancing, replacement', 'circle'),
('Body Work', 'Collision repair and painting', 'hammer'),
('Inspection', 'State inspection and emissions', 'search'),
('Towing', 'Emergency towing service', 'truck');

-- Insert popular car makes
INSERT INTO public.car_makes (name) VALUES
('Toyota'), ('Honda'), ('Ford'), ('Chevrolet'), ('Nissan'),
('BMW'), ('Mercedes-Benz'), ('Audi'), ('Volkswagen'), ('Hyundai'),
('Kia'), ('Mazda'), ('Subaru'), ('Lexus'), ('Acura');

-- Insert some popular models for Toyota
INSERT INTO public.car_models (make_id, name) 
SELECT id, model FROM public.car_makes, UNNEST(ARRAY['Camry', 'Corolla', 'RAV4', 'Prius', 'Highlander']) AS model 
WHERE name = 'Toyota';

-- Insert some popular models for Honda
INSERT INTO public.car_models (make_id, name) 
SELECT id, model FROM public.car_makes, UNNEST(ARRAY['Civic', 'Accord', 'CR-V', 'Pilot', 'Odyssey']) AS model 
WHERE name = 'Honda';

-- Insert some popular models for Ford
INSERT INTO public.car_models (make_id, name) 
SELECT id, model FROM public.car_makes, UNNEST(ARRAY['F-150', 'Escape', 'Explorer', 'Mustang', 'Focus']) AS model 
WHERE name = 'Ford';