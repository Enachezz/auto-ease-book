# Quote & quote log endpoints

Documentation for quote-related APIs, including **`QUOTE_LOG`** (comments/notifications on quotes), addendum quotes, rejection, and inbox listing.

---

## AI-readable summary

**Purpose:** Support quote conversations and notifications via `QUOTE_LOG` rows tied to a quote; garages post messages (optionally spawning an addendum `Quote`); car owners **accept** (with explicit `addendumFlow`) or **reject** pending quotes; recipients list unread/all logs with pagination and mark logs read.

**Auth (Spring Security):**

| Endpoint                         | Roles                                                                 |
| -------------------------------- | --------------------------------------------------------------------- |
| `POST /api/quotes/{quoteId}/logs` | **GARAGE** (JWT); caller must own the garage on `quoteId`              |
| `POST /api/quotes/{quoteId}/accept` | **CAR_OWNER** (JWT); caller must own the job request for that quote; JSON body **required** (`AcceptQuoteRequest`) |
| `POST /api/quotes/{quoteId}/reject` | **CAR_OWNER** (JWT); caller must own the job request for that quote |
| `GET /api/quote-logs`            | Authenticated **CAR_OWNER** or **GARAGE**; results auto-scoped by recipient |
| `POST /api/quote-logs/{logId}/mark-read` | Authenticated user must match log recipient (`typeOfNotification` + `reference`) |

**DTOs:**

- **`AcceptQuoteRequest`:** `addendumFlow` (**Boolean**, required — must not be null): `false` = initial marketplace acceptance while job request is **OPEN** (reject sibling quotes, set job **BOOKED**); `true` = addendum acceptance while job request is **BOOKED** or **IN_PROGRESS** (separate booking; quote-log side effects). Also `scheduledDate` (`LocalDate`), `scheduledTime` (`LocalTime`), `notes` (`String`), all optional.
- **`CreateQuoteLogRequest`:** `message` (String, optional), `notificationFlag` (Boolean), `updateFlag` (Boolean), `newQuote` (`CreateQuoteRequest`, optional; required when `updateFlag === true`).
- **`CreateQuoteRequest`** (nested under `newQuote`): not validated by Jakarta Bean Validation on bind; **`QuoteLogService`** requires non-null `newQuote` and non-null `newQuote.price` when `updateFlag === true`. Other fields optional.
- **`QuoteLogResponse`:** `id`, `quoteId`, `authorUserId`, `message`, `typeOfNotification`, `reference`, `notificationFlag`, `triggeredQuoteId`, `createdDate`, `modifiedDate`.
- **`PagedQuoteLogsResponse`:** `logs` (array of `QuoteLogResponse`), `totalCount` (long), `page` (int), `pageSize` (int), `totalPages` (int).
- **`QuoteResponse`** (reject): `id`, `jobRequestId`, `garageId`, `garageName`, `garageCity`, `garageRating`, `price`, `estimatedDuration`, `description`, `warrantyInfo`, `status`.
- **`BookingResponse`** (accept): `id`, `quoteId`, `garageId`, `garageName`, `jobTitle`, `price`, `scheduledDate`, `scheduledTime`, `status`, `notes`.

**Accept semantics (`POST /api/quotes/{quoteId}/accept`):**

- The client **must** send `addendumFlow`; the server **does not** infer acceptance mode from job status alone (prevents choosing the wrong branch).
- **Cross-validation:** `addendumFlow === false` is allowed only when `job_requests.status === OPEN`. `addendumFlow === true` is allowed only when status is **BOOKED** or **IN_PROGRESS**. Mismatch → `400 Bad Request`.

**Listing semantics (`GET /api/quote-logs`):**

- `notificationFlag=true`: only logs where `notificationFlag === true`, sorted **ascending** by `createdDate` (oldest first).
- `notificationFlag=false` or omitted: all logs for the scoped recipient, sorted **descending** by `createdDate` (newest first).
- `page`: zero-based; fixed **page size 20**.
- **CAR_OWNER scope:** `typeOfNotification === CAR_OWNER` and `reference === JWT subject (APP_USER uuid string)`.
- **GARAGE scope:** `typeOfNotification === GARAGE_OWNER` and `reference === garages.id as string`.

**Create log semantics (`POST /api/quotes/{quoteId}/logs`):**

- Persists a log on `quoteId`; unread behaviour follows request `notificationFlag` (defaults applied server-side when omitted).
- If `updateFlag === true`: `newQuote` must be present (`400` if null); `newQuote.price` must be present (`400` if null). Creates an additional **PENDING** quote on the same `job_request_id`; response includes non-null `triggeredQuoteId` for that addendum quote.

**Reject semantics (`POST /api/quotes/{quoteId}/reject`):**

- Sets quote status to **REJECTED** when pending and the caller owns the linked job request; may trigger quote-log side effects for addendum flows (originating CAR_OWNER notification cleared; garage notified via service logic).

**Related existing endpoints (not new):**

- `POST /api/job-requests/{jobRequestId}/quotes` — garage submits initial quote.
- `GET /api/job-requests/{jobRequestId}/quotes` — car owner lists quotes for a job request.
- `GET /api/quotes/mine` — garage lists its quotes.
- `GET /api/bookings` — list bookings for the authenticated user (car owner and/or garage views merged).

## Human-readable endpoints

### `POST /api/quotes/{quoteId}/logs`

**Main functionality:** Lets the **garage** that owns the quote add a **quote log** (comment/notification for the car owner). Optionally creates an **addendum quote** on the same job request when `updateFlag` is true.

**How to use:**

- **Auth:** Bearer JWT; user must have role **GARAGE**. The authenticated garage must match `quotes.garage_id` for `{quoteId}`.
- **Path:** `quoteId` — UUID of the quote this message is attached to.
- **Payload (JSON body — `CreateQuoteLogRequest`):**

| Field               | Type    | Required                  | Notes                                                         |
| ------------------- | ------- | ------------------------- | ------------------------------------------------------------- |
| `message`           | string  | No                        | Text shown to the car owner                                   |
| `notificationFlag`  | boolean | No                        | If true, log counts as unread for unread-only listing          |
| `updateFlag`        | boolean | No                        | If true, also creates a new **PENDING** quote on the same job   |
| `newQuote`          | object  | When `updateFlag` is true | Same shape as submit-quote body; enforced in **`QuoteLogService`** (must be present; `price` required). |

**`newQuote` object (`CreateQuoteRequest`):**

| Field                | Type             | Required |
| -------------------- | ---------------- | -------- |
| `price`              | number (decimal) | Yes      |
| `estimatedDuration`  | string           | No       |
| `description`        | string           | No       |
| `warrantyInfo`       | string           | No       |

**Response:** **`QuoteLogResponse`** (JSON object)

| Field                | Type               |
| -------------------- | ------------------ |
| `id`                 | UUID string        |
| `quoteId`            | UUID string        |
| `authorUserId`       | string (APP_USER uuid) |
| `message`            | string or null     |
| `typeOfNotification` | string (`CAR_OWNER` or `GARAGE_OWNER`) |
| `reference`          | string (recipient key: car owner uuid or garage id string) |
| `notificationFlag`   | boolean            |
| `triggeredQuoteId`   | UUID string or null (addendum quote id when `updateFlag` was used) |
| `createdDate`        | ISO date-time string |
| `modifiedDate`       | ISO date-time string |

**HTTP status:** `201 Created` on success.

---

### `POST /api/quotes/{quoteId}/reject`

**Main functionality:** Lets the **car owner** reject a **pending** quote (including addendum quotes).

**How to use:**

- **Auth:** Bearer JWT; user must have role **CAR_OWNER**. Caller must own the job request linked to that quote.
- **Path:** `quoteId` — UUID of the quote to reject.
- **Payload:** None (empty body).

**Response:** **`QuoteResponse`** (JSON object)

| Field               | Type               |
| ------------------- | ------------------ |
| `id`                | UUID string        |
| `jobRequestId`      | UUID string        |
| `garageId`          | UUID string        |
| `garageName`        | string             |
| `garageCity`        | string or null     |
| `garageRating`      | number (decimal) or null |
| `price`             | number (decimal)   |
| `estimatedDuration` | string or null     |
| `description`       | string or null     |
| `warrantyInfo`      | string or null     |
| `status`            | string (e.g. `REJECTED`) |

**HTTP status:** `200 OK` on success.

---

### `POST /api/quotes/{quoteId}/accept`

**Main functionality:** Car owner accepts a **pending** quote and creates a **booking**. The client must declare whether this is the **initial** acceptance (`addendumFlow: false`, job still **OPEN**) or an **addendum** acceptance (`addendumFlow: true`, job already **BOOKED** or **IN_PROGRESS**). The server checks that flag against the job request status and rejects mismatches with `400`.

**How to use:**

- **Auth:** Bearer JWT; user must have role **CAR_OWNER**. Caller must own the job request linked to `quoteId`.
- **Path:** `quoteId` — UUID of the quote to accept.
- **Payload (JSON body — `AcceptQuoteRequest`, required):**

| Field            | Type    | Required | Notes |
| ---------------- | ------- | -------- | ----- |
| `addendumFlow`   | boolean | **Yes**  | `false` = first acceptance (job must be **OPEN**); other quotes on the job are rejected and the job becomes **BOOKED**. `true` = accepting extra work (job must be **BOOKED** or **IN_PROGRESS**); siblings and job status unchanged; quote-log follow-up for garage. |
| `scheduledDate`  | string (ISO date, `YYYY-MM-DD`) | No | Booking field |
| `scheduledTime`  | string (ISO time, e.g. `HH:mm:ss`) | No | Booking field |
| `notes`          | string | No | Booking field |

**Response:** **`BookingResponse`** (JSON object)

| Field            | Type |
| ---------------- | ---- |
| `id`             | UUID string |
| `quoteId`        | UUID string |
| `garageId`       | UUID string |
| `garageName`     | string |
| `jobTitle`       | string |
| `price`          | number (decimal) or null |
| `scheduledDate`  | string (ISO date) or null |
| `scheduledTime`  | string (ISO time) or null |
| `status`         | string (e.g. `CONFIRMED`) |
| `notes`          | string or null |

**HTTP status:** `201 Created` on success. `400` if `addendumFlow` is missing, or if it does not match the current job request status (e.g. `addendumFlow: true` while job is still **OPEN**).

---

### `GET /api/quote-logs`

**Main functionality:** Returns **paginated** quote logs **only for the logged-in user’s inbox**: car owners see logs targeted at them; garages see logs targeted at their garage. Supports filtering to **unread-only** vs **all**, with different sort orders.

**How to use:**

- **Auth:** Bearer JWT; **CAR_OWNER** or **GARAGE**.
- **Query parameters:**

| Parameter          | Type    | Required | Notes                                                                 |
| ------------------ | ------- | -------- | --------------------------------------------------------------------- |
| `notificationFlag` | boolean | No       | `true` → only unread logs, oldest→newest. Omit or `false` → all logs, newest→oldest |
| `page`             | integer | No       | Default `0`; zero-based page index                                    |

**Payload:** None.

**Response:** **`PagedQuoteLogsResponse`** (JSON object)

| Field         | Type                                      |
| ------------- | ----------------------------------------- |
| `logs`        | array of **`QuoteLogResponse`** (same fields as `POST /api/quotes/{quoteId}/logs` response) |
| `totalCount`  | number (long)                             |
| `page`        | number (int)                              |
| `pageSize`    | number (int), fixed **20**                |
| `totalPages`  | number (int)                              |

**HTTP status:** `200 OK`.

---

### `POST /api/quote-logs/{logId}/mark-read`

**Main functionality:** Sets **`notificationFlag`** to **false** on one log so it drops out of the “unread” list. Only the **intended recipient** (matching log type + reference) may call this.

**How to use:**

- **Auth:** Bearer JWT (car owner or garage, depending on who the log is for).
- **Path:** `logId` — UUID of the quote log row.
- **Payload:** None.

**Response:** **`QuoteLogResponse`** (same field table as `POST /api/quotes/{quoteId}/logs` response).

**HTTP status:** `200 OK` on success; `403` if the caller is not the recipient for that log.
