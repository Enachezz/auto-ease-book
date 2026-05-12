# Garage updates

Documentation for garage discovery and service-category assignment endpoints.

---

## AI-readable summary

**Purpose:** Expose approved-garage search with optional filters (`businessName`, `categoryIds`, `isDealership`), and let the logged-in garage user read/replace which `ServiceCategory` ids are linked via `GarageCategoryAssignment`. Category filter is **OR** across ids; **null or empty `categoryIds`** applies no category constraint. **`isDealership` null** applies no dealership filter. Search returns **approved garages only**, paginated (fixed page size **20**), sorted by `businessName` ascending.

**Auth (Spring Security):** `POST /api/garages/search` is **permitAll**. `GET` / `PUT /api/garages/me/categories` require an **authenticated** user with the **GARAGE** role (JWT bearer).

**DTOs:**

- `GarageSearchRequest`: `filter` (`GarageFilterCriteria`, optional), `page` (int, min 0, default 0).
- `GarageFilterCriteria`: `businessName` (String, optional), `categoryIds` (`Set<UUID>`, optional; null or empty = no category filter), `isDealership` (Boolean, optional; null = no filter, `true` / `false` = match that flag on `Garage`).
- `PagedGaragesResponse`: `garages` (`List<GarageResponse>`), `totalCount` (long), `page` (int), `pageSize` (int), `totalPages` (int).
- `GarageResponse`: `id` (UUID), `userId` (String), `businessName`, `address`, `city`, `state`, `postalCode`, `phone`, `email`, `dealership` (Boolean), `description`, `services` (String[]), `isApproved` (Boolean), `averageRating` (BigDecimal), `totalReviews` (Integer).
- `SetGarageCategoriesRequest`: `categoryIds` (`Set<UUID>`, required field; may be empty to clear all).
- `ServiceCategoryResponse` (list element): `id` (UUID), `name`, `description`, `icon` (Strings).

**Related reference data:** `GET /api/service-categories` — list categories (use ids in search filter and in `PUT` body).

---

## Human-readable endpoints

### `POST /api/garages/search`

**What it does:** Returns a **page** of **approved** garages matching optional filters (business name substring, and/or at least one of the given service category ids, and/or dealership flag).

**How to use**

- **Auth:** None (public).
- **Payload:** JSON body `GarageSearchRequest` (body may be omitted; server treats as empty request with defaults).

| Field | Type | Required | Notes |
|--------|------|----------|--------|
| `filter` | object | No | Omit or `{}` for no filters |
| `filter.businessName` | string | No | Case-insensitive **contains** match on garage business name (trimmed) |
| `filter.categoryIds` | array of UUID strings | No | If **null** or **empty**, no category filter. If non-empty, garage must have **at least one** listed category |
| `filter.isDealership` | boolean | No | If **null**, no dealership filter. If **true** or **false**, only garages with that `dealership` value |
| `page` | integer | No | Default `0`; must be ≥ 0. Page size is fixed at **20** |

**Response:** `PagedGaragesResponse` (JSON object)

| Field | Type |
|--------|------|
| `garages` | array of `GarageResponse` |
| `totalCount` | number (long) |
| `page` | number (int) |
| `pageSize` | number (int), typically 20 |
| `totalPages` | number (int) |

Each **`GarageResponse`** in `garages`:

| Field | Type |
|--------|------|
| `id` | UUID (string in JSON) |
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

### `GET /api/garages/me/categories`

**What it does:** Returns the **service categories** currently linked to **your** garage (the garage profile for the authenticated garage user).

**How to use**

- **Auth:** Bearer JWT; user must be **GARAGE**.
- **Payload:** None.

**Response:** JSON **array** of `ServiceCategoryResponse`

| Field | Type |
|--------|------|
| `id` | UUID (string) |
| `name` | string |
| `description` | string or null |
| `icon` | string or null |

**Typical errors:** `404` if no garage exists for this user; `401` / `403` if not logged in or not a garage user.

---

### `PUT /api/garages/me/categories`

**What it does:** **Replaces** all service-category links for your garage with the given set (full replace, not merge). An empty set clears all category links.

**How to use**

- **Auth:** Bearer JWT; user must be **GARAGE**.
- **Payload:** JSON `SetGarageCategoriesRequest`

| Field | Type | Required | Notes |
|--------|------|----------|--------|
| `categoryIds` | array of UUID strings | Yes (field must be present) | May be `[]` to clear all |

**Response:** **204 No Content** on success.

**Typical errors:** `400` if any id is not a valid category; `404` if no garage for this user; `401` / `403` if not allowed.

---

### `GET /api/garages/public/{id}`

**What it does:** Returns **public** garage data for a specific garage id. Only **approved** garages are returned; unapproved ids behave like not found.

**How to use**

- **Auth:** None (public).
- **Path:** `id` — garage id, UUID string.

**Response:** `GarageResponse` (same field list as in the search results table above).

**Typical errors:** `404` if garage does not exist or is not approved.

---

# Calendar updates

Documentation for the per-garage calendar endpoints. A "calendar" is a **read-only projection of that garage's bookings** — there is no separate Calendar entity. The `bookings` table now carries `garage_id` and `customer_id` directly so a garage's calendar can be queried without joining through `quotes`.

---

## AI-readable summary

**Purpose:** Expose the bookings of any approved garage as a calendar feed for the front end. The public endpoint accepts an optional `year` + `month` filter (both-or-neither) to scope results to a single calendar month; otherwise all bookings for the garage are returned. The `/me` endpoint returns the authenticated garage user's own calendar, unfiltered. Bookings whose `scheduledDate` is `null` are **excluded** when the month filter is applied. There is **no calendar mutation endpoint** — bookings are still created/modified through the existing quote-acceptance and booking flows.

**Auth (Spring Security):** `GET /api/garages/public/{garageId}/calendar` is **permitAll** (matched by the existing `/api/garages/public/**` rule). `GET /api/garages/me/calendar` requires an **authenticated** user with the **GARAGE** role (JWT bearer).

**Booking population:** When a quote is accepted, the resulting `Booking` row stores `garageId = quote.garageId` and `customerId = jobRequest.userId`, set on creation in `BookingService.acceptQuote`. Existing rows were backfilled by migration `V14`.

**DTOs:**

- `CalendarResponse` (list element): `id` (UUID, booking id), `garageId` (UUID), `customerId` (String, the customer's `APP_USER.uuid` / `VARCHAR(50)`), `scheduledDate` (LocalDate / ISO date string, nullable), `scheduledTime` (LocalTime / ISO time string, nullable), `status` (String — booking status enum name, e.g. `CONFIRMED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`), `description` (String, mapped from the booking's `notes` field, nullable), `createdDate` (LocalDateTime / ISO date-time), `modifiedDate` (LocalDateTime / ISO date-time).

**Related endpoints:** existing `GET /api/garages/public/{id}` returns the static garage profile; use the calendar endpoint alongside it for that garage's appointments.

---

## Human-readable endpoints

### `GET /api/garages/public/{garageId}/calendar`

**What it does:** Returns the **list of bookings** (calendar entries) for a specific approved garage. Optionally narrows the result to a single calendar month. Intended for the front end to render a garage's calendar view to any user — including unauthenticated visitors.

**How to use**

- **Auth:** None (public).
- **Path:** `garageId` — garage id, UUID string.
- **Query parameters (optional, both-or-neither):**

| Field | Type | Required | Notes |
|--------|------|----------|--------|
| `year` | integer | No | Calendar year (e.g. `2026`). Must be supplied together with `month` |
| `month` | integer | No | Calendar month, `1`–`12`. Must be supplied together with `year` |

If both are omitted, all bookings for the garage are returned. If both are supplied, only bookings whose `scheduledDate` falls within that month are returned; bookings with a `null` `scheduledDate` are excluded from the filtered result.

**Response:** JSON **array** of `CalendarResponse`

| Field | Type |
|--------|------|
| `id` | UUID (string) — booking id |
| `garageId` | UUID (string) |
| `customerId` | string — the customer's `APP_USER.uuid` |
| `scheduledDate` | string (ISO date, `YYYY-MM-DD`) or null |
| `scheduledTime` | string (ISO time, `HH:mm:ss`) or null |
| `status` | string — booking status enum (`CONFIRMED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`) |
| `description` | string or null — booking notes |
| `createdDate` | string (ISO date-time) — appointment creation timestamp |
| `modifiedDate` | string (ISO date-time) — last modification timestamp |

**Typical errors:** `404` if the garage does not exist or is not approved. `400` if only one of `year`/`month` is provided, or if the pair does not form a valid month (e.g. `month=13`).

---

### `GET /api/garages/me/calendar`

**What it does:** Returns the **list of bookings** for the authenticated garage user's own garage. Always returns all bookings for that garage — no month filter on this endpoint.

**How to use**

- **Auth:** Bearer JWT; user must be **GARAGE**.
- **Payload:** None.

**Response:** JSON **array** of `CalendarResponse` (same shape as the public endpoint table above).

**Typical errors:** `404` if no garage exists for this user; `401` / `403` if not logged in or not a garage user.
