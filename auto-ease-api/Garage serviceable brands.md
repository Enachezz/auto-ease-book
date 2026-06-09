# Garage serviceable brands — endpoint summary

---

## AI-readable summary

**Purpose:** Let a garage (or admin) manage up to **5** serviceable car brands (car makes) per garage via `GarageMakeAssignment`. Public garage search can optionally filter by one brand (`makeId`). Brand ids come from reference data `GET /api/car-makes`.

**Business rules:**
- Max **5** brands per garage (`makeIds` size ≤ 5); empty set clears all brands.
- Garage owner: operates on **own** garage only (`/me`); `garageId` optional/ignored unless provided and must match own garage (else `403`).
- Admin: same endpoints; **`garageId` required** (`400` if missing) to target any garage.
- Search brand filter: applied **only when** `filter.makeId` is non-null; otherwise no brand constraint.

**Auth:** `GET` / `PUT /api/garages/me/makes` require authenticated JWT with **GARAGE** or **ADMIN** role. `POST /api/garages/search` remains **permitAll** (public).

**DTOs:**
- `SetGarageMakesRequest`: `garageId` (UUID, optional for GARAGE, required for ADMIN), `makeIds` (`Set<UUID>`, required, max 5, may be empty).
- `CarMakeResponse` (list element): `id` (UUID), `name` (String).
- `GarageFilterCriteria` (search, extended): adds `makeId` (UUID, optional; null = no brand filter).

**Related reference data:** `GET /api/car-makes` — list valid brand ids for `makeIds` and search filter.

**HTTP status notes:**
- `204 No Content` — successful `PUT`
- `400 Bad Request` — more than 5 brands, invalid make id(s), admin missing `garageId`
- `403 Forbidden` — garage user passed another garage's `garageId`
- `404 Not Found` — garage not found (or garage user has no profile)

---

## Human-readable endpoints

### `GET /api/garages/me/makes`

**What it does:** Returns the car brands (makes) a garage is configured to service.

**How to use:**
- **Auth:** Bearer JWT; user must be **GARAGE** (own garage) or **ADMIN** (any garage via `garageId`).
- **Query params:**

| Param | Type | Required | Notes |
|--------|------|----------|--------|
| `garageId` | UUID string | No for garage owner; **Yes for admin** | Garage owner: omit or use own id. Admin: must pass target garage id |

**Response:** JSON array of `CarMakeResponse`

| Field | Type |
|--------|------|
| `id` | UUID (string in JSON) |
| `name` | string (e.g. `"Toyota"`) |

**Example (garage owner):**
```http
GET /api/garages/me/makes
Authorization: Bearer <garage-token>
```

**Example (admin):**
```http
GET /api/garages/me/makes?garageId=550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer <admin-token>
```

---

### `PUT /api/garages/me/makes`

**What it does:** Replaces the full list of serviceable car brands for a garage (not add/remove one at a time). Max 5 brands; send empty `makeIds` to clear all.

**How to use:**
- **Auth:** Bearer JWT; user must be **GARAGE** or **ADMIN**.
- **Payload:** JSON body `SetGarageMakesRequest`

| Field | Type | Required | Notes |
|--------|------|----------|--------|
| `garageId` | UUID string | No for garage owner; **Yes for admin** | Which garage to update |
| `makeIds` | array of UUID strings | Yes | 0–5 ids from `GET /api/car-makes` |

**Response:** `204 No Content` (empty body)

**Example (garage owner):**
```json
{
  "makeIds": [
    "<toyota-uuid>",
    "<honda-uuid>"
  ]
}
```

**Example (admin):**
```json
{
  "garageId": "550e8400-e29b-41d4-a716-446655440000",
  "makeIds": [
    "<toyota-uuid>"
  ]
}
```

---

### `POST /api/garages/search` *(existing endpoint — new filter field)*

**What it does:** Returns a paginated list of **approved** garages. Can now filter by serviceable brand when `filter.makeId` is provided.

**How to use:**
- **Auth:** None (public).
- **Payload:** JSON body `GarageSearchRequest`; new optional filter field:

| Field | Type | Required | Notes |
|--------|------|----------|--------|
| `filter.makeId` | UUID string | No | If **omitted or null**, brand is **not** used as a filter. If set, only garages that service that brand are returned |

Other search fields (`filter.businessName`, `filter.categoryIds`, `filter.isDealership`, `page`) unchanged.

**Example with brand filter:**
```json
{
  "filter": {
    "makeId": "<toyota-uuid>"
  },
  "page": 0
}
```

**Response:** `PagedGaragesResponse` (unchanged shape)

| Field | Type |
|--------|------|
| `garages` | array of `GarageResponse` |
| `totalCount` | number (long) |
| `page` | number (int) |
| `pageSize` | number (int), fixed at 20 |
| `totalPages` | number (int) |

Each **`GarageResponse`** in `garages`:

| Field | Type |
|--------|------|
| `id` | UUID (string) |
| `userId` | string |
| `businessName` | string |
| `address` | string |
| `city` | string |
| `state` | string |
| `postalCode` | string |
| `phone` | string |
| `email` | string or null |
| `dealership` | boolean |
| `description` | string or null |
| `services` | array of strings |
| `isApproved` | boolean |
| `averageRating` | number (decimal) |
| `totalReviews` | number (integer) |

---

## Quick reference

| Endpoint | Role | Main action |
|----------|------|-------------|
| `GET /api/garages/me/makes` | GARAGE / ADMIN | Read serviceable brands for a garage |
| `PUT /api/garages/me/makes` | GARAGE / ADMIN | Replace serviceable brands (max 5) |
| `POST /api/garages/search` | Public | Search garages; optional `filter.makeId` for brand |
