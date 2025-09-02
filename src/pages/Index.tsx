import { useAuth } from '@/hooks/useAuth';
import { Layout } from '@/components/Layout';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { useNavigate } from 'react-router-dom';
import { 
  Car, 
  Wrench, 
  CheckCircle
} from 'lucide-react';

const Index = () => {
  const { user, profile, loading } = useAuth();
  const navigate = useNavigate();

  // Show loading state while auth is being checked
  if (loading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto mb-4"></div>
          <p className="text-muted-foreground">Loading...</p>
        </div>
      </div>
    );
  }

  // Homepage for everyone (authenticated and non-authenticated users)
  return (
    <Layout>
      {/* Hero Section */}
      <div className="relative bg-gradient-to-br from-primary/5 to-blue-50 dark:from-primary/10 dark:to-slate-900 rounded-2xl overflow-hidden mb-16">
        <div className="absolute inset-0 opacity-20" style={{
          backgroundImage: `url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23000' fill-opacity='0.05'%3E%3Ccircle cx='7' cy='7' r='2'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E")`
        }}></div>
        <div className="relative px-8 py-16 lg:py-24">
          <div className="max-w-4xl mx-auto text-center">
            <div className="inline-flex items-center gap-2 bg-primary/10 text-primary px-4 py-2 rounded-full text-sm font-medium mb-6">
              <span>🚗</span>
              Join thousands of satisfied customers
            </div>
            <h1 className="text-4xl lg:text-6xl font-bold text-foreground mb-6 leading-tight">
              Find local garages you can{' '}
              <span className="text-primary">rely on</span>
            </h1>
            <p className="text-xl text-muted-foreground mb-8 max-w-2xl mx-auto">
              We'll help you save money on car repairs in just a few clicks. 
              Connect with trusted mechanics and get competitive quotes.
            </p>
            
            <div className="flex flex-col sm:flex-row gap-4 justify-center mb-12">
              {!user ? (
                <>
                  <Button 
                    size="lg" 
                    className="text-lg px-8 py-6 h-auto"
                    onClick={() => navigate('/auth')}
                  >
                    Get Started Free
                  </Button>
                  <Button 
                    variant="outline" 
                    size="lg" 
                    className="text-lg px-8 py-6 h-auto"
                    onClick={() => navigate('/auth')}
                  >
                    For Garages
                  </Button>
                </>
              ) : profile?.user_type === 'car_owner' ? (
                <>
                  <Button 
                    size="lg" 
                    className="text-lg px-8 py-6 h-auto"
                    onClick={() => navigate('/request-service')}
                  >
                    Request Service
                  </Button>
                  <Button 
                    variant="outline" 
                    size="lg" 
                    className="text-lg px-8 py-6 h-auto"
                    onClick={() => navigate('/my-requests')}
                  >
                    My Requests
                  </Button>
                </>
              ) : (
                <>
                  <Button 
                    size="lg" 
                    className="text-lg px-8 py-6 h-auto"
                    onClick={() => navigate('/garage')}
                  >
                    Manage Garage
                  </Button>
                  <Button 
                    variant="outline" 
                    size="lg" 
                    className="text-lg px-8 py-6 h-auto"
                    onClick={() => navigate('/job-requests')}
                  >
                    Browse Jobs
                  </Button>
                </>
              )}
            </div>

            {/* Trust indicators */}
            <div className="flex flex-wrap items-center justify-center gap-6 text-sm text-muted-foreground">
              <div className="flex items-center gap-2">
                <div className="flex text-yellow-400">
                  ★★★★★
                </div>
                <span>4.8/5 rating</span>
              </div>
              <div className="h-4 w-px bg-border hidden sm:block"></div>
              <span>10,000+ happy customers</span>
              <div className="h-4 w-px bg-border hidden sm:block"></div>
              <span>500+ trusted garages</span>
            </div>
          </div>
        </div>
      </div>

      {/* How it works */}
      <div className="mb-16">
        <div className="text-center mb-12">
          <h2 className="text-3xl font-bold mb-4">How AutoEase Works</h2>
          <p className="text-lg text-muted-foreground">Simple steps to get your car repaired</p>
        </div>
        
        <div className="grid md:grid-cols-3 gap-8">
          <div className="text-center">
            <div className="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-4">
              <Car className="h-8 w-8 text-primary" />
            </div>
            <h3 className="text-xl font-semibold mb-2">1. Describe Your Issue</h3>
            <p className="text-muted-foreground">Tell us what's wrong with your car and where you're located</p>
          </div>
          
          <div className="text-center">
            <div className="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-4">
              <Wrench className="h-8 w-8 text-primary" />
            </div>
            <h3 className="text-xl font-semibold mb-2">2. Get Quotes</h3>
            <p className="text-muted-foreground">Receive competitive quotes from local trusted garages</p>
          </div>
          
          <div className="text-center">
            <div className="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-4">
              <CheckCircle className="h-8 w-8 text-primary" />
            </div>
            <h3 className="text-xl font-semibold mb-2">3. Book & Repair</h3>
            <p className="text-muted-foreground">Choose the best quote and book your repair appointment</p>
          </div>
        </div>
      </div>

      {/* Testimonials */}
      <div className="mb-16">
        <div className="text-center mb-12">
          <h2 className="text-3xl font-bold mb-4">What drivers say about our garages</h2>
          <p className="text-lg text-muted-foreground">We think they're great. But don't just take our word for it...</p>
        </div>
        
        <div className="grid md:grid-cols-3 gap-6">
          <Card className="p-6">
            <div className="flex text-yellow-400 mb-4">★★★★★</div>
            <p className="text-sm text-muted-foreground mb-4">
              "Amazing service! The garage was very professional and fixed my BMW quickly. 
              Great communication throughout the process."
            </p>
            <div className="text-sm font-medium">BMW 5 Series owner</div>
          </Card>
          
          <Card className="p-6">
            <div className="flex text-yellow-400 mb-4">★★★★★</div>
            <p className="text-sm text-muted-foreground mb-4">
              "Really good service, kept me informed with photos and texts. 
              Well worth the money and very competitive pricing."
            </p>
            <div className="text-sm font-medium">Vauxhall Corsa owner</div>
          </Card>
          
          <Card className="p-6">
            <div className="flex text-yellow-400 mb-4">★★★★★</div>
            <p className="text-sm text-muted-foreground mb-4">
              "Highly recommend! Great service, booked me in very quickly. 
              Charlie was very friendly and helped me understand the problem."
            </p>
            <div className="text-sm font-medium">Peugeot 2008 owner</div>
          </Card>
        </div>
      </div>

      {/* CTA Section */}
      <div className="bg-primary text-primary-foreground rounded-2xl p-8 text-center">
        <h2 className="text-3xl font-bold mb-4">Ready to get started?</h2>
        <p className="text-lg mb-6 opacity-90">
          Join thousands of drivers who trust AutoEase for their car repairs
        </p>
        <div className="flex flex-col sm:flex-row gap-4 justify-center">
          {!user ? (
            <Button 
              variant="secondary" 
              size="lg"
              onClick={() => navigate('/auth')}
            >
              Sign Up Now
            </Button>
          ) : profile?.user_type === 'car_owner' ? (
            <Button 
              variant="secondary" 
              size="lg"
              onClick={() => navigate('/request-service')}
            >
              Request Service
            </Button>
          ) : (
            <Button 
              variant="secondary" 
              size="lg"
              onClick={() => navigate('/garage')}
            >
              Join as Garage
            </Button>
          )}
        </div>
      </div>
    </Layout>
  );
};

export default Index;