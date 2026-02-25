# Step 06 — Job Requests & Quotes

> Part of the [AutoFix Java Backend MVP](./LOVABLE-TO-JAVA-MIGRATION.md).
> Depends on: [Step 04 — Vehicles](./STEP-04-VEHICLE-MANAGEMENT.md) and [Step 05 — Garages](./STEP-05-GARAGE-PROFILES.md)

---

## Goal

Car owners can create service requests (job requests). Garages can view open requests and submit quotes. Owners can view quotes for their requests. This is the core marketplace interaction.

---

## Scope

### Do

**Job Requests:**
- `POST /api/job-requests` — owner creates a request (CAR_OWNER)
- `GET /api/job-requests` — owner lists own requests (CAR_OWNER)
- `GET /api/job-requests/open` — garage lists open requests (GARAGE)
- `PUT /api/job-requests/{id}` — owner updates own request (CAR_OWNER)
- `DELETE /api/job-requests/{id}` — owner deletes own request (CAR_OWNER)

**Quotes:**
- `POST /api/job-requests/{id}/quotes` — garage submits a quote (GARAGE)
- `GET /api/job-requests/{id}/quotes` — owner views quotes for their request (CAR_OWNER)
- `GET /api/quotes/mine` — garage views own submitted quotes (GARAGE)

### Don't

- No counter-offers
- No quote expiration logic
- No accepting quotes yet (Step 07)

---

## Tasks

### 1. JobRequest entity

```java
@Entity @Table(name = "job_requests")
public class JobRequest {
    @Id private UUID id;
    private UUID userId;         // FK → app_users (owner)
    private UUID carId;          // FK → cars
    private UUID categoryId;     // FK → service_categories
    private String title;
    private String description;
    @Enumerated(EnumType.STRING)
    private Urgency urgency;     // LOW, MEDIUM, HIGH
    private LocalDate preferredDate;
    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    @Enumerated(EnumType.STRING)
    private JobRequestStatus status; // OPEN, QUOTED, BOOKED, IN_PROGRESS, COMPLETED, CANCELLED
    private String locationAddress;
    private String locationCity;
    private String locationState;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 2. Quote entity

```java
@Entity @Table(name = "quotes")
@Table(uniqueConstraints = @UniqueConstraint(columns = {"job_request_id", "garage_id"}))
public class Quote {
    @Id private UUID id;
    private UUID jobRequestId;   // FK → job_requests
    private UUID garageId;       // FK → garages
    private BigDecimal price;
    private String estimatedDuration;
    private String description;
    private String warrantyInfo;
    @Enumerated(EnumType.STRING)
    private QuoteStatus status;  // PENDING, ACCEPTED, REJECTED, EXPIRED
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 3. JobRequestService

- `createJobRequest(UUID userId, CreateJobRequestRequest)` → validate carId belongs to user, validate categoryId exists, save with `status = OPEN`
- `getMyJobRequests(UUID userId)` → list where userId matches
- `getOpenJobRequests()` → list where `status = OPEN` (for garages)
- `updateJobRequest(UUID userId, UUID id, ...)` → verify ownership + status is OPEN
- `deleteJobRequest(UUID userId, UUID id)` → verify ownership

### 4. QuoteService

- `submitQuote(UUID garageUserId, UUID jobRequestId, CreateQuoteRequest)` → find garage by userId, verify job exists and is OPEN, enforce unique(jobRequestId, garageId), save with `status = PENDING`
- `getQuotesForRequest(UUID ownerUserId, UUID jobRequestId)` → verify owner owns the request, list quotes with garage info
- `getMyQuotes(UUID garageUserId)` → find garage, list quotes

### 5. Response DTOs

**JobRequestResponse** — include car info (make/model/year), category name, quote count.

**QuoteResponse** — include garage name, city, rating.

---

## Acceptance Tests

### Test 1: Create job request — happy path

```
POST /api/job-requests
Authorization: Bearer <car-owner-token>

{
  "carId": "<car-uuid>",
  "categoryId": "<oil-change-uuid>",
  "title": "Schimb ulei motor",
  "description": "Schimb ulei + filtru la Toyota Corolla 2020",
  "urgency": "MEDIUM",
  "preferredDate": "2025-03-15",
  "locationCity": "București"
}
```

**Pass criteria:**
- Status `201 Created`
- Response has `status: "OPEN"`, all fields populated
- Database: `job_requests` has 1 row

### Test 2: Create job request — car not owned by user

```
POST /api/job-requests
Authorization: Bearer <owner-A-token>
{ "carId": "<owner-B-car-uuid>", ... }
```

**Pass criteria:** Status `403` or `400` with error

### Test 3: Create job request — garage user rejected

```
POST /api/job-requests
Authorization: Bearer <garage-token>
{ ... }
```

**Pass criteria:** Status `403 Forbidden`

### Test 4: List own job requests

```
GET /api/job-requests
Authorization: Bearer <car-owner-token>
```

**Pass criteria:**
- Status `200 OK`
- Returns only requests for this user
- Includes car info and category name

### Test 5: List open job requests (garage view)

```
GET /api/job-requests/open
Authorization: Bearer <garage-token>
```

**Pass criteria:**
- Status `200 OK`
- Returns only requests with `status = OPEN`
- Does not return requests from other statuses

### Test 6: Submit quote — happy path

```
POST /api/job-requests/{jobId}/quotes
Authorization: Bearer <garage-token>

{
  "price": 250.00,
  "estimatedDuration": "2 ore",
  "description": "Schimb ulei Castrol 5W-30 + filtru Mann",
  "warrantyInfo": "6 luni garanție"
}
```

**Pass criteria:**
- Status `201 Created`
- Quote has `status: "PENDING"`
- Database: `quotes` has 1 row

### Test 7: Submit quote — duplicate rejected

Same garage submits a second quote for the same job request.

**Pass criteria:** Status `409 Conflict`

### Test 8: Submit quote — car owner rejected

```
POST /api/job-requests/{jobId}/quotes
Authorization: Bearer <car-owner-token>
{ ... }
```

**Pass criteria:** Status `403 Forbidden`

### Test 9: View quotes for own request

```
GET /api/job-requests/{jobId}/quotes
Authorization: Bearer <car-owner-token>
```

**Pass criteria:**
- Status `200 OK`
- Returns quotes with garage name, price, status
- Only visible to the owner of the job request

### Test 10: View quotes — not your request

```
GET /api/job-requests/{other-owner-job-id}/quotes
Authorization: Bearer <different-owner-token>
```

**Pass criteria:** Status `403` or `404`

### Test 11: Integration test — full request-quote flow

```java
@Test
void ownerCreatesRequestGarageQuotes() {
    // 1. Register car owner + add car
    AuthResponse owner = registerCarOwner();
    CarResponse car = addCar(owner);

    // 2. Create job request
    var jobReq = new CreateJobRequestRequest(car.getId(), categoryId, "Test", "Desc", "MEDIUM", null, null);
    JobRequestResponse job = createJobRequest(owner, jobReq);
    assertEquals("OPEN", job.getStatus());

    // 3. Register garage + create garage profile
    AuthResponse garage = registerGarage();
    createGarageProfile(garage);

    // 4. Submit quote
    var quoteReq = new CreateQuoteRequest(BigDecimal.valueOf(100), "1h", "Details", null);
    QuoteResponse quote = submitQuote(garage, job.getId(), quoteReq);
    assertEquals("PENDING", quote.getStatus());

    // 5. Owner sees quote
    QuoteResponse[] quotes = getQuotesForRequest(owner, job.getId());
    assertEquals(1, quotes.length);
    assertEquals(BigDecimal.valueOf(100), quotes[0].getPrice());
}
```

---

## Definition of Done

- [ ] Car owners can create, list, update, delete job requests
- [ ] Garages can list open job requests
- [ ] Garages can submit quotes (one per job request)
- [ ] Duplicate quotes are rejected
- [ ] Owners can view quotes for their own requests
- [ ] Garages can view their submitted quotes
- [ ] Ownership and role checks enforced on all endpoints
- [ ] All 11 acceptance tests pass
