# Step 08 — Frontend Migration & End-to-End Tests

> Part of the [AutoFix Java Backend MVP](./LOVABLE-TO-JAVA-MIGRATION.md).
> Depends on: All previous steps (01–07). The Java backend is fully functional.

---

## Goal

Replace all Supabase calls in the React frontend with calls to the Java REST API. Remove the Supabase dependency entirely. Verify the full application works end-to-end with the Java backend, running locally on Windows.

---

## Scope

### Do

- Create an API client utility (fetch/axios wrapper with JWT)
- Rewrite `useAuth.tsx` to use Java auth endpoints
- Replace every `supabase.from(...)` call with the corresponding REST endpoint
- Replace `supabase.auth.*` calls with Java auth
- Remove `@supabase/supabase-js` dependency
- Remove `src/integrations/supabase/` folder
- Add CORS configuration to the Java backend
- Write E2E tests that exercise the full flow through the frontend

### Don't

- No Supabase Storage migration (document upload can use local storage or be deferred)
- No Supabase Realtime replacement (EmergencyMechanicDialog can be simplified or deferred)

---

## Tasks

### 1. Java backend: CORS configuration

Add to Spring Security config or a separate `@Configuration`:

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("http://localhost:5173")); // Vite dev server
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    return new UrlBasedCorsConfigurationSource() {{
        registerCorsConfiguration("/**", config);
    }};
}
```

### 2. Frontend: API client utility

Create `src/lib/api.ts`:

```typescript
const API_URL = 'http://localhost:8080/api';

async function request(path, options) {
  const token = localStorage.getItem('token');
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const response = await fetch(`${API_URL}${path}`, { ...options, headers });
  if (!response.ok) throw new Error(await response.text());
  return response.json();
}

export const api = {
  get: (path) => request(path, { method: 'GET' }),
  post: (path, body) => request(path, { method: 'POST', body: JSON.stringify(body) }),
  put: (path, body) => request(path, { method: 'PUT', body: JSON.stringify(body) }),
  delete: (path) => request(path, { method: 'DELETE' }),
};
```

### 3. Frontend: Rewrite `useAuth.tsx`

Replace Supabase auth with Java auth:

| Current (Supabase) | Target (Java API) |
|---------------------|-------------------|
| `supabase.auth.signUp(...)` | `api.post('/auth/register', {...})` |
| `supabase.auth.signInWithPassword(...)` | `api.post('/auth/login', {...})` |
| `supabase.auth.signOut()` | `localStorage.removeItem('token')` |
| `supabase.auth.getSession()` | Read token from localStorage, decode JWT |
| `supabase.from('profiles').select(...)` | `api.get('/profiles/me')` |

### 4. Frontend: Replace Supabase queries page by page

| Page | Supabase Call | Java API Call |
|------|---------------|---------------|
| **Auth.tsx** | `supabase.auth.signUp/signIn` | `api.post('/auth/register')`, `api.post('/auth/login')` |
| **MyCars.tsx** | `supabase.from('cars').select(...)` | `api.get('/cars')` |
| | `supabase.from('car_makes').select(...)` | `api.get('/car-makes')` |
| | `supabase.from('car_models').select(...)` | `api.get('/car-makes/{id}/models')` |
| | `supabase.from('cars').insert(...)` | `api.post('/cars', {...})` |
| | `supabase.from('cars').update(...)` | `api.put('/cars/{id}', {...})` |
| **ServiceDetails.tsx** | `supabase.from('service_categories')` | `api.get('/service-categories')` |
| | `supabase.from('job_requests').insert(...)` | `api.post('/job-requests', {...})` |
| **JobRequests.tsx** | `supabase.from('job_requests').select(...)` | `api.get('/job-requests')` |
| | `supabase.from('quotes').select(...)` | `api.get('/job-requests/{id}/quotes')` |
| | `supabase.from('quotes').update({status:'accepted'})` | `api.post('/quotes/{id}/accept', {...})` |
| | `supabase.from('quotes').update({status:'rejected'})` | (Optional: `api.put('/quotes/{id}/reject')`) |
| | `supabase.from('job_requests').delete()` | `api.delete('/job-requests/{id}')` |
| **EditRequest.tsx** | `supabase.from('job_requests').update(...)` | `api.put('/job-requests/{id}', {...})` |
| **GarageManagement.tsx** | `supabase.from('garages').select(...)` | `api.get('/garages/me')` |
| | `supabase.from('garages').insert(...)` | `api.post('/garages', {...})` |
| | `supabase.from('garages').update(...)` | `api.put('/garages/me', {...})` |
| | `supabase.from('job_requests').select(...)` (open) | `api.get('/job-requests/open')` |
| | `supabase.from('quotes').insert(...)` | `api.post('/job-requests/{id}/quotes', {...})` |
| **Settings.tsx** | `supabase.auth.updateUser(...)` | `api.put('/profiles/me', {...})` |

### 5. Remove Supabase

- Delete `src/integrations/supabase/` (client.ts, types.ts)
- Remove `@supabase/supabase-js` from `package.json`
- Run `npm install` to update lockfile
- Search for any remaining `supabase` imports and replace

### 6. Handle deferred features

- **EmergencyMechanicDialog** uses Supabase Realtime → simplify or disable for MVP
- **Document upload** uses Supabase Storage → defer or use a simple file upload endpoint

---

## Acceptance Tests

### E2E Test 1: Register and login via UI

1. Start Java backend: `mvn spring-boot:run` (port 8080)
2. Start frontend: `npm run dev` (port 5173)
3. Open `http://localhost:5173/auth`
4. Register as car owner with email/password
5. Verify redirect to home page
6. Verify profile name shown in navigation

**Pass criteria:** User is registered, logged in, and can see their name

### E2E Test 2: Add a car via UI

1. Log in as car owner
2. Navigate to "My Cars"
3. Click "Adaugă Mașină"
4. Select make → models load
5. Fill form, submit
6. Car appears in the list

**Pass criteria:** Car is created and visible in the list

### E2E Test 3: Create a job request via UI

1. Log in as car owner (with a car)
2. Navigate to "Request Service"
3. Select a category
4. Fill details (car, description, urgency, location)
5. Submit
6. Navigate to "My Requests" → request is listed

**Pass criteria:** Job request created and visible

### E2E Test 4: Garage submits a quote

1. Log in as garage user
2. Navigate to "Garage Management"
3. Create garage profile (if not exists)
4. Switch to "Jobs" tab → open requests visible
5. Submit a quote for a request

**Pass criteria:** Quote submitted and visible in garage's "My Quotes"

### E2E Test 5: Owner accepts quote → booking created

1. Log in as car owner
2. Navigate to "My Requests"
3. Click "View Quotes" on a request with quotes
4. Accept a quote
5. Verify request status changes to "BOOKED"

**Pass criteria:** Booking created, job status updated, other quotes rejected

### E2E Test 6: Full loop — automated integration test

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class FullMarketplaceE2ETest {

    @Autowired TestRestTemplate rest;

    @Test
    void completeMarketplaceLoop() {
        // 1. Register car owner
        AuthResponse owner = register("owner@test.com", "pass", "Owner", "CAR_OWNER");

        // 2. Add car
        CarResponse car = post(owner, "/api/cars", carRequest(), CarResponse.class);

        // 3. Create job request
        JobRequestResponse job = post(owner, "/api/job-requests",
            jobRequest(car.getId()), JobRequestResponse.class);

        // 4. Register garage
        AuthResponse garage = register("garage@test.com", "pass", "Garage", "GARAGE");

        // 5. Create garage profile
        post(garage, "/api/garages", garageRequest(), GarageResponse.class);

        // 6. Garage sees open job
        JobRequestResponse[] openJobs = get(garage, "/api/job-requests/open", JobRequestResponse[].class);
        assertTrue(openJobs.length >= 1);

        // 7. Garage submits quote
        QuoteResponse quote = post(garage, "/api/job-requests/" + job.getId() + "/quotes",
            quoteRequest(), QuoteResponse.class);

        // 8. Owner views quotes
        QuoteResponse[] quotes = get(owner, "/api/job-requests/" + job.getId() + "/quotes",
            QuoteResponse[].class);
        assertEquals(1, quotes.length);

        // 9. Owner accepts quote
        BookingResponse booking = post(owner, "/api/quotes/" + quote.getId() + "/accept",
            acceptRequest(), BookingResponse.class);
        assertEquals("CONFIRMED", booking.getStatus());

        // 10. Verify final states
        JobRequestResponse finalJob = get(owner, "/api/job-requests", JobRequestResponse[].class)[0];
        assertEquals("BOOKED", finalJob.getStatus());
    }
}
```

**Pass criteria:** All 10 steps complete without errors. The full Owner → Request → Quote → Accept → Booking loop works.

### E2E Test 7: No Supabase references remain

```bash
# In the frontend src/ folder
grep -r "supabase" src/
```

**Pass criteria:** Zero matches. No Supabase imports, no Supabase client calls.

---

## Definition of Done

- [ ] All Supabase calls replaced with Java API calls
- [ ] `@supabase/supabase-js` removed from dependencies
- [ ] `src/integrations/supabase/` deleted
- [ ] CORS configured on Java backend for `localhost:5173`
- [ ] Auth flow works through the UI (register, login, logout)
- [ ] All pages work with Java backend (cars, requests, quotes, garages)
- [ ] E2E Tests 1–6 pass (manual or automated)
- [ ] E2E Test 7 passes (no Supabase references)
- [ ] Both `mvn spring-boot:run` and `npm run dev` start cleanly
