import type { Page } from '@playwright/test';

const API = 'http://localhost:8080/api';

async function apiPost<T = unknown>(path: string, body: unknown, token?: string): Promise<T> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const res = await fetch(`${API}${path}`, {
    method: 'POST',
    headers,
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error(`${res.status} ${await res.text()}`);
  const text = await res.text();
  return text ? JSON.parse(text) : (undefined as T);
}

async function apiGet<T = unknown>(path: string, token?: string): Promise<T> {
  const headers: Record<string, string> = {};
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const res = await fetch(`${API}${path}`, { headers });
  if (!res.ok) throw new Error(`${res.status} ${await res.text()}`);
  return res.json();
}

async function apiPatch<T = unknown>(path: string, token: string): Promise<T> {
  const res = await fetch(`${API}${path}`, {
    method: 'PATCH',
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) throw new Error(`${res.status} ${await res.text()}`);
  const text = await res.text();
  return text ? JSON.parse(text) : (undefined as T);
}

const uniqueEmail = () => `e2e-${Date.now()}-${Math.random().toString(36).slice(2, 6)}@test.com`;

export interface AuthResult {
  token: string;
  userId: string;
  email: string;
}

export async function registerViaApi(
  userType: 'CAR_OWNER' | 'GARAGE' | 'ADMIN' = 'CAR_OWNER',
  fullName = 'E2E User'
): Promise<AuthResult & { password: string }> {
  const email = uniqueEmail();
  const password = 'password123';
  const data = await apiPost<{ token: string; userId: string; email: string }>(
    '/auth/register',
    { email, password, fullName, userType }
  );
  return { ...data, password };
}

export async function setTokenInBrowser(page: Page, token: string): Promise<void> {
  await page.goto('/auth');
  await page.evaluate((t) => localStorage.setItem('token', t), token);
  await page.goto('/');
  await page.waitForLoadState('networkidle');
}

export interface CarResult {
  id: number;
  makeName: string;
  modelName: string;
}

export async function addCarViaApi(token: string): Promise<CarResult> {
  const makes = await apiGet<{ id: string; name: string }[]>('/car-makes');
  const toyota = makes.find((m) => m.name === 'Toyota');
  if (!toyota) throw new Error('Toyota not in seed data');

  const models = await apiGet<{ id: string; name: string }[]>(
    `/car-makes/${toyota.id}/models`
  );
  const corolla = models.find((m) => m.name === 'Corolla');
  if (!corolla) throw new Error('Corolla not in seed data');

  return apiPost<CarResult>(
    '/cars',
    { makeId: toyota.id, modelId: corolla.id, year: 2022 },
    token
  );
}

export interface JobRequestResult {
  id: number;
  title: string;
  status: string;
}

export async function createJobRequestViaApi(
  token: string,
  carId: number
): Promise<JobRequestResult> {
  const categories = await apiGet<{ id: string; name: string }[]>('/service-categories');
  const categoryId = categories[0]?.id;

  return apiPost<JobRequestResult>(
    '/job-requests',
    {
      carId,
      categoryId: categoryId ?? null,
      title: 'E2E Oil Change Request',
      description: 'Full oil change and filter replacement for E2E test',
      urgency: 'NORMAL',
      locationCity: 'București',
    },
    token
  );
}

export async function createGarageViaApi(token: string): Promise<{ id: string }> {
  return apiPost<{ id: string }>(
    '/garages',
    {
      businessName: 'E2E Auto Service',
      address: 'Str. Testului 42',
      city: 'Cluj-Napoca',
      state: 'Cluj',
      postalCode: '400001',
      phone: '+40741000000',
      description: 'Full service auto repair for E2E testing',
      services: ['Oil Change', 'Brake Service'],
    },
    token
  );
}

export async function approveGarageViaApi(garageId: string, adminToken: string): Promise<void> {
  await apiPatch(`/garages/${garageId}/approve`, adminToken);
}

export async function registerAdminViaApi(): Promise<AuthResult> {
  return registerViaApi('ADMIN', 'E2E Admin');
}

export interface QuoteResult {
  id: string;
  price: number;
  status: string;
}

export async function submitQuoteViaApi(
  token: string,
  jobRequestId: number,
  price = 250,
  description = 'E2E test quote'
): Promise<QuoteResult> {
  return apiPost<QuoteResult>(
    `/job-requests/${jobRequestId}/quotes`,
    { price, estimatedDuration: '2 hours', description },
    token
  );
}

export async function acceptQuoteViaApi(token: string, quoteId: string): Promise<void> {
  await apiPost(`/quotes/${quoteId}/accept`, {}, token);
}

export async function submitReviewViaApi(
  token: string,
  bookingId: string,
  rating = 5,
  comment = 'Excellent E2E service'
): Promise<{ id: string }> {
  return apiPost<{ id: string }>(
    `/bookings/${bookingId}/reviews`,
    { rating, comment },
    token
  );
}

export async function getBookingsViaApi(token: string): Promise<{ id: string; status: string }[]> {
  return apiGet<{ id: string; status: string }[]>('/bookings', token);
}

export async function getGarageReviewsViaApi(garageId: string): Promise<{ id: string; rating: number; comment: string }[]> {
  return apiGet<{ id: string; rating: number; comment: string }[]>(`/garages/${garageId}/reviews`);
}
