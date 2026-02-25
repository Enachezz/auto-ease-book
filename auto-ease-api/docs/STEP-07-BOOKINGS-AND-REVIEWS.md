# Step 07 — Bookings & Reviews

> Part of the [AutoFix Java Backend MVP](./LOVABLE-TO-JAVA-MIGRATION.md).
> Depends on: [Step 06 — Job Requests & Quotes](./STEP-06-JOB-REQUESTS-AND-QUOTES.md)

---

## Goal

Car owners can accept a quote, which creates a booking and closes the marketplace loop. After a booking is completed, owners can leave a review. This is the final piece of backend business logic.

---

## Scope

### Do

**Bookings:**
- `POST /api/quotes/{quoteId}/accept` — owner accepts a quote (creates booking, rejects other quotes, updates job status)
- `GET /api/bookings` — list own bookings (both owners and garages)

**Reviews:**
- `POST /api/bookings/{bookingId}/reviews` — owner reviews a completed booking
- `GET /api/garages/{garageId}/reviews` — list reviews for a garage (public)

### Don't

- No rescheduling
- No cancellation flow (post-MVP)
- No payment integration

---

## Tasks

### 1. Booking entity

```java
@Entity @Table(name = "bookings")
public class Booking {
    @Id private UUID id;
    private UUID quoteId;         // FK → quotes (UNIQUE — one booking per quote)
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    @Enumerated(EnumType.STRING)
    private BookingStatus status; // CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 2. Review entity

```java
@Entity @Table(name = "reviews")
public class Review {
    @Id private UUID id;
    private UUID bookingId;       // FK → bookings (UNIQUE — one review per booking)
    private UUID garageId;        // FK → garages
    private UUID userId;          // FK → app_users (reviewer)
    private Integer rating;       // 1-5
    private String comment;
    private LocalDateTime createdAt;
}
```

### 3. BookingService — accept quote (transactional)

This is the most important business operation. It must be **@Transactional**:

```
acceptQuote(UUID ownerUserId, UUID quoteId, AcceptQuoteRequest):
  1. Find the quote → verify it's PENDING
  2. Find the job request → verify the owner matches ownerUserId
  3. Verify the job request status is OPEN
  4. Set quote.status = ACCEPTED
  5. Set all OTHER quotes for this job request to REJECTED
  6. Set jobRequest.status = BOOKED
  7. Create a Booking with:
     - quoteId = the accepted quote
     - scheduledDate, scheduledTime from request
     - status = CONFIRMED
  8. Return the booking
```

If any step fails, the whole transaction rolls back.

### 4. ReviewService

- `createReview(UUID userId, UUID bookingId, CreateReviewRequest)` → verify user owns the booking (through quote → job_request.userId), verify booking status is COMPLETED, save review, update garage averageRating + totalReviews
- `getReviewsForGarage(UUID garageId)` → list reviews

### 5. AcceptQuoteRequest DTO

```json
{
  "scheduledDate": "2025-03-20",
  "scheduledTime": "10:00"
}
```

---

## Acceptance Tests

### Test 1: Accept quote — happy path

```
POST /api/quotes/{quoteId}/accept
Authorization: Bearer <car-owner-token>

{
  "scheduledDate": "2025-03-20",
  "scheduledTime": "10:00"
}
```

**Pass criteria:**
- Status `201 Created`
- Response contains booking with `status: "CONFIRMED"`
- Database: accepted quote has `status = ACCEPTED`
- Database: job request has `status = BOOKED`
- Database: other quotes for this job have `status = REJECTED`

### Test 2: Accept quote — not your job request

```
POST /api/quotes/{quoteId}/accept
Authorization: Bearer <different-owner-token>
```

**Pass criteria:** Status `403 Forbidden`

### Test 3: Accept quote — garage user rejected

```
POST /api/quotes/{quoteId}/accept
Authorization: Bearer <garage-token>
```

**Pass criteria:** Status `403 Forbidden`

### Test 4: Accept quote — already accepted

After accepting one quote, try accepting another for the same job.

**Pass criteria:** Status `400 Bad Request` (job is no longer OPEN)

### Test 5: List own bookings — car owner

```
GET /api/bookings
Authorization: Bearer <car-owner-token>
```

**Pass criteria:**
- Status `200 OK`
- Returns bookings for jobs this user owns (through quote → job_request)
- Includes garage name, job title, scheduled date

### Test 6: List own bookings — garage

```
GET /api/bookings
Authorization: Bearer <garage-token>
```

**Pass criteria:**
- Status `200 OK`
- Returns bookings for quotes this garage submitted

### Test 7: Create review — happy path

First, manually set booking status to COMPLETED (or add an endpoint). Then:

```
POST /api/bookings/{bookingId}/reviews
Authorization: Bearer <car-owner-token>

{
  "rating": 5,
  "comment": "Serviciu excelent!"
}
```

**Pass criteria:**
- Status `201 Created`
- Review saved with correct garageId, userId
- Garage `average_rating` and `total_reviews` updated

### Test 8: Create review — not your booking

```
POST /api/bookings/{other-booking-id}/reviews
Authorization: Bearer <different-owner-token>
```

**Pass criteria:** Status `403 Forbidden`

### Test 9: Create review — duplicate rejected

Submit a second review for the same booking.

**Pass criteria:** Status `409 Conflict`

### Test 10: List reviews for garage (public)

```
GET /api/garages/{garageId}/reviews
```

**Pass criteria:**
- Status `200 OK`
- Returns all reviews for this garage
- No auth required

### Test 11: Integration test — full marketplace loop

```java
@Test
void fullMarketplaceLoop() {
    // 1. Owner registers + adds car
    AuthResponse owner = registerCarOwner();
    CarResponse car = addCar(owner);

    // 2. Owner creates job request
    JobRequestResponse job = createJobRequest(owner, car.getId(), categoryId);
    assertEquals("OPEN", job.getStatus());

    // 3. Garage registers + creates profile
    AuthResponse garageAuth = registerGarage();
    createGarageProfile(garageAuth);

    // 4. Garage submits quote
    QuoteResponse quote = submitQuote(garageAuth, job.getId(), BigDecimal.valueOf(200));
    assertEquals("PENDING", quote.getStatus());

    // 5. Owner accepts quote → booking created
    BookingResponse booking = acceptQuote(owner, quote.getId(), "2025-03-20", "10:00");
    assertEquals("CONFIRMED", booking.getStatus());

    // 6. Verify job is BOOKED
    JobRequestResponse updatedJob = getJobRequest(owner, job.getId());
    assertEquals("BOOKED", updatedJob.getStatus());

    // 7. Verify quote is ACCEPTED
    QuoteResponse[] quotes = getQuotesForRequest(owner, job.getId());
    assertEquals("ACCEPTED", quotes[0].getStatus());
}
```

This is the **key E2E test** — it validates the entire MVP loop.

---

## Definition of Done

- [ ] `POST /api/quotes/{id}/accept` creates a booking, marks quote as accepted, rejects others, updates job status — all in one transaction
- [ ] `GET /api/bookings` returns bookings for the authenticated user (owner or garage)
- [ ] `POST /api/bookings/{id}/reviews` creates a review and updates garage rating
- [ ] `GET /api/garages/{id}/reviews` returns reviews (public)
- [ ] Ownership and role checks enforced
- [ ] Transaction rollback on failure
- [ ] All 11 acceptance tests pass (especially Test 11 — full loop)
