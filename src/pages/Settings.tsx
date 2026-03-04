import { Layout } from '@/components/Layout';
import { useAuth } from '@/hooks/useAuth';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Switch } from '@/components/ui/switch';
import { Separator } from '@/components/ui/separator';
import { Camera, Lock, Mail, Phone, Bell, FileCheck } from 'lucide-react';
import { useState, useRef } from 'react';
import { api } from '@/lib/api';
import { useToast } from '@/hooks/use-toast';
import { useNavigate } from 'react-router-dom';

export default function Settings() {
  const { profile, user } = useAuth();
  const { toast } = useToast();
  const navigate = useNavigate();
  const fileInputRef = useRef<HTMLInputElement>(null);
  
  const [phone, setPhone] = useState(profile?.phone || '');
  const [notifyValabilities, setNotifyValabilities] = useState(true);
  const [notifyMeetings, setNotifyMeetings] = useState(true);
  const [drivingLicense, setDrivingLicense] = useState<File | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  if (!user || !profile) {
    navigate('/auth');
    return null;
  }

  const handlePhoneUpdate = async () => {
    setIsLoading(true);
    try {
      await api.put('/profiles/me', { phone });
      
      toast({
        title: "Telefon actualizat",
        description: "Numărul de telefon a fost actualizat cu succes.",
      });
    } catch (error: any) {
      toast({
        title: "Eroare",
        description: error.message,
        variant: "destructive",
      });
    } finally {
      setIsLoading(false);
    }
  };

  const handleDrivingLicenseUpload = async () => {
    if (!drivingLicense) return;

    setIsLoading(true);
    try {
      toast({
        title: "Funcție în dezvoltare",
        description: "Funcționalitatea de încărcare a permisului de conducere va fi disponibilă în curând.",
      });
    } catch (error: any) {
      toast({
        title: "Eroare",
        description: error.message,
        variant: "destructive",
      });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Layout>
      <div className="max-w-4xl mx-auto space-y-6">
        <div>
          <h1 className="text-3xl font-bold">Setări</h1>
          <p className="text-muted-foreground">Gestionează-ți contul și preferințele</p>
        </div>

        {/* Email (read-only for now) */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Mail className="h-5 w-5" />
              Email
            </CardTitle>
            <CardDescription>Adresa ta de email</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="email">Adresa de Email</Label>
              <Input
                id="email"
                type="email"
                value={profile.email}
                disabled
              />
              <p className="text-xs text-muted-foreground">
                Modificarea emailului va fi disponibilă într-o versiune viitoare.
              </p>
            </div>
          </CardContent>
        </Card>

        {/* Phone Number */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Phone className="h-5 w-5" />
              Telefon
            </CardTitle>
            <CardDescription>Actualizează numărul tău de telefon</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="phone">Număr de Telefon</Label>
              <div className="flex gap-2">
                <Input
                  id="phone"
                  type="tel"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="+40 XXX XXX XXX"
                />
                <Button onClick={handlePhoneUpdate} disabled={isLoading}>
                  Salvează
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Password (deferred) */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Lock className="h-5 w-5" />
              Parolă
            </CardTitle>
            <CardDescription>Schimbă parola contului tău</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <p className="text-sm text-muted-foreground">
              Schimbarea parolei va fi disponibilă într-o versiune viitoare.
            </p>
          </CardContent>
        </Card>

        {/* Notifications */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Bell className="h-5 w-5" />
              Notificări
            </CardTitle>
            <CardDescription>Gestionează notificările tale</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center justify-between">
              <div className="space-y-0.5">
                <Label htmlFor="notify-valabilities">Valabilități Documente</Label>
                <p className="text-sm text-muted-foreground">
                  Primește notificări când se apropie expirarea documentelor mașinii
                </p>
              </div>
              <Switch
                id="notify-valabilities"
                checked={notifyValabilities}
                onCheckedChange={setNotifyValabilities}
              />
            </div>
            <Separator />
            <div className="flex items-center justify-between">
              <div className="space-y-0.5">
                <Label htmlFor="notify-meetings">Programări Service</Label>
                <p className="text-sm text-muted-foreground">
                  Primește notificări pentru programările cu mecanicii
                </p>
              </div>
              <Switch
                id="notify-meetings"
                checked={notifyMeetings}
                onCheckedChange={setNotifyMeetings}
              />
            </div>
          </CardContent>
        </Card>

        {/* Driving License Verification */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <FileCheck className="h-5 w-5" />
              Verificare Cont
            </CardTitle>
            <CardDescription>
              Încarcă o fotografie a permisului de conducere pentru verificare
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="driving-license">Permis de Conducere</Label>
              <div className="flex gap-2">
                <Input
                  id="driving-license"
                  type="file"
                  accept="image/*"
                  ref={fileInputRef}
                  onChange={(e) => setDrivingLicense(e.target.files?.[0] || null)}
                  className="cursor-pointer"
                />
                <Button
                  onClick={handleDrivingLicenseUpload}
                  disabled={!drivingLicense || isLoading}
                >
                  <Camera className="h-4 w-4 mr-2" />
                  Încarcă
                </Button>
              </div>
              <p className="text-xs text-muted-foreground">
                Acest document va fi folosit doar pentru verificarea contului tău
              </p>
            </div>
          </CardContent>
        </Card>
      </div>
    </Layout>
  );
}
