# Job completion, reviews & replies

Documentation for job completion, per-job reviews, review reply threads, and related API changes.

---

## AI-readable summary

**Purpose:** Close the job lifecycle so garages can mark work done, car owners leave one Google-style review per job (1–5 rating + optional comment), and garage owners and car owners can alternate threaded replies. Garage search can filter by minimum rating/review count.

**Auth:**

| Endpoint | Role |
|----------|------|
| `POST /api/job-requests/{id}/complete` | **GARAGE** (JWT); garage must be approved and have an accepted quote on the job |
| `POST /api/job-requests/{jobRequestId}/reviews` | **CAR_OWNER** (JWT); caller must own the job request |
| `POST /api/reviews/{reviewId}/replies` | **CAR_OWNER** or **GARAGE** (JWT); only the review author or garage owner; alternating turns |
| `GET /api/garages/{garageId}/reviews` | **Public** (no auth) |

**Review model:** One review per **job request** (`job_request_id` unique). Ratings live only on `reviews`; replies have no rating. `Garage.averageRating` / `totalReviews` update when a review is created.

**Job completion rules:**
- Job status must be `BOOKED` or `IN_PROGRESS`
- No `PENDING` quotes on the job (addendums must be accepted or rejected)
- At least one booking exists for an accepted quote
- On success: all such bookings → `COMPLETED`, job → `COMPLETED`

**Review creation rules:**
- Job status must be `COMPLETED`
- Caller owns the job
- One review per job (`409` if duplicate)

**Reply rules:**
- First reply: garage owner only; `parentReplyId` must be omitted
- Later replies: alternate garage ↔ car owner; `parentReplyId` must be the latest reply id

**Removed:** `POST /api/bookings/{bookingId}/reviews` (replaced by job-scoped review endpoint)

**Extended (not new endpoints):**
- `GET /api/job-requests`, `GET /api/job-requests/{id}` — `JobRequestResponse` adds `reviewEligible`, `reviewId`
- `POST /api/garages/search` — `GarageFilterCriteria` adds `minRating`, `minReviews`

---

## Human-readable endpoints

### `POST /api/job-requests/{id}/complete`

**What it does:** Lets the garage mark the whole job as done. All bookings for accepted quotes on that job are set to `COMPLETED`, and the job request becomes `COMPLETED`. The car owner can then leave a review.

**How to use**

- **Auth:** Bearer JWT; user must be **GARAGE**. The garage must own an accepted quote on this job and be approved.
- **Path:** `id` — UUID of the job request.
- **Payload:** None (empty body).

**Response:** `JobRequestResponse` (JSON object)

| Field | Type |
|--------|------|
| `id` | UUID (string) |
| `carId` | number (integer) |
| `makeName` | string |
| `modelName` | string |
| `carYear` | number (integer) or null |
| `categoryId` | UUID (string) or null |
| `categoryName` | string or null |
| `title` | string |
| `description` | string or null |
| `urgency` | string (e.g. `NORMAL`) |
| `preferredDate` | string (ISO date) or null |
| `budgetMin` | number (decimal) or null |
| `budgetMax` | number (decimal) or null |
| `status` | string (e.g. `COMPLETED`) |
| `locationAddress` | string or null |
| `locationCity` | string or null |
| `locationState` | string or null |
| `quoteCount` | number (integer) |
| `reviewEligible` | boolean (`true` when job is completed and no review exists yet) |
| `reviewId` | UUID (string) or null |

**HTTP status:** `200 OK` on success.

**Typical errors:** `400` if job is not `BOOKED`/`IN_PROGRESS`, or if a quote is still `PENDING`; `403` if garage does not own the job; `404` if job not found.

---

### `POST /api/job-requests/{jobRequestId}/reviews`

**What it does:** Lets the car owner submit one review for a completed job — a 1–5 rating and an optional comment. Updates the garage’s average rating and review count.

**How to use**

- **Auth:** Bearer JWT; user must be **CAR_OWNER** and own the job request.
- **Path:** `jobRequestId` — UUID of the job request.
- **Payload (JSON body — `CreateReviewRequest`):**

| Field | Type | Required | Notes |
|--------|------|----------|--------|
| `rating` | number (integer) | Yes | 1–5 |
| `comment` | string | No | Optional review text |

**Response:** `ReviewResponse` (JSON object)

| Field | Type |
|--------|------|
| `id` | UUID (string) |
| `jobRequestId` | UUID (string) |
| `garageId` | UUID (string) |
| `userId` | string (reviewer’s user UUID) |
| `rating` | number (integer), 1–5 |
| `comment` | string or null |
| `createdDate` | string (ISO date-time) |
| `replies` | array of `ReviewReplyResponse` (empty on create) |

**HTTP status:** `201 Created` on success.

**Typical errors:** `400` if job is not `COMPLETED`; `403` if caller does not own the job; `404` if job not found; `409` if a review already exists for this job.

---

### `POST /api/reviews/{reviewId}/replies`

**What it does:** Adds a reply in the review thread. The garage owner posts the first reply; after that, garage and car owner alternate. Replies have no rating.

**How to use**

- **Auth:** Bearer JWT; user must be **CAR_OWNER** or **GARAGE**, and must be either the review author or the garage owner for that review.
- **Path:** `reviewId` — UUID of the review.
- **Payload (JSON body — `CreateReviewReplyRequest`):**

| Field | Type | Required | Notes |
|--------|------|----------|--------|
| `message` | string | Yes | Reply text (non-blank) |
| `parentReplyId` | UUID (string) | No | Omit for the first reply (garage only). For later replies, must be the id of the latest reply in the thread. |

**Response:** `ReviewReplyResponse` (JSON object)

| Field | Type |
|--------|------|
| `id` | UUID (string) |
| `authorUserId` | string |
| `authorRole` | string (`GARAGE` or `CAR_OWNER`) |
| `message` | string |
| `parentReplyId` | UUID (string) or null |
| `createdDate` | string (ISO date-time) |

**HTTP status:** `201 Created` on success.

**Typical errors:** `400` if `parentReplyId` is wrong for the thread; `403` if wrong role, wrong user, or not your turn; `404` if review not found.

---

### `GET /api/garages/{garageId}/reviews` *(updated)*

**What it does:** Returns all reviews for a garage (public). Each review now includes the full reply thread and is keyed by job request, not booking.

**How to use**

- **Auth:** None (public).
- **Path:** `garageId` — UUID of the garage.
- **Payload:** None.

**Response:** JSON **array** of `ReviewResponse`

| Field | Type |
|--------|------|
| `id` | UUID (string) |
| `jobRequestId` | UUID (string) |
| `garageId` | UUID (string) |
| `userId` | string |
| `rating` | number (integer), 1–5 |
| `comment` | string or null |
| `createdDate` | string (ISO date-time) |
| `replies` | array of `ReviewReplyResponse` |

Each **`ReviewReplyResponse`** in `replies`:

| Field | Type |
|--------|------|
| `id` | UUID (string) |
| `authorUserId` | string |
| `authorRole` | string (`GARAGE`, `CAR_OWNER`, or `UNKNOWN`) |
| `message` | string |
| `parentReplyId` | UUID (string) or null |
| `createdDate` | string (ISO date-time) |

**HTTP status:** `200 OK`.

---

## Extended existing endpoints

### `GET /api/job-requests` and `GET /api/job-requests/{id}`

Same behavior as before, but **`JobRequestResponse`** now also returns:

| Field | Type | Meaning |
|--------|------|---------|
| `reviewEligible` | boolean | `true` when job is `COMPLETED` and the owner has not reviewed yet |
| `reviewId` | UUID (string) or null | Set when a review already exists |

### `POST /api/garages/search`

No new URL; **`GarageFilterCriteria`** in the request body adds optional filters:

| Field | Type | Notes |
|--------|------|--------|
| `filter.minRating` | number (decimal) | Garages with `averageRating >= minRating` |
| `filter.minReviews` | number (integer) | Garages with `totalReviews >= minReviews` |

---

## Removed endpoint

| Old endpoint | Replacement |
|--------------|-------------|
| `POST /api/bookings/{bookingId}/reviews` | `POST /api/job-requests/{jobRequestId}/reviews` |

Reviews are per **job**, not per booking.
