# Step 05 — Garage Profiles

> Part of the [AutoFix Java Backend MVP](./LOVABLE-TO-JAVA-MIGRATION.md).
> Depends on: [Step 02 — Authentication & Security](./STEP-02-AUTHENTICATION-AND-SECURITY.md)

---

## Goal

Users with role GARAGE can create and manage their garage (workshop) profile. Car owners and the public can view approved garages. This gives service providers an identity in the marketplace.

---

## Scope

### Do

- Garage entity, repository, service, controller
- `POST /api/garages` — create garage profile (GARAGE only, one per user)
- `GET /api/garages/me` — get own garage (GARAGE only)
- `PUT /api/garages/me` — update own garage (GARAGE only)
- `GET /api/garages` — list approved garages (public)
- Ownership enforcement: garage users can only manage their own garage
- DTOs: `CreateGarageRequest`, `UpdateGarageRequest`, `GarageResponse`

### Don't

- No admin approval endpoint (garages start as `is_approved = false`; admin flow is post-MVP)
- No rating calculation (that comes with reviews in Step 07)
- No geo-search

---

## Tasks

### 1. Garage entity

```java
@Entity @Table(name = "garages")
public class Garage {
    @Id private UUID id;
    private UUID userId;             // FK → app_users (1:1)
    private String businessName;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String phone;
    private String description;
    private String[] services;       // TEXT[] in Postgres
    private Boolean isApproved;      // default false
    private BigDecimal averageRating; // default 0
    private Integer totalReviews;    // default 0
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 2. GarageService

- `createGarage(UUID userId, CreateGarageRequest)` → verify no existing garage for user, save with `isApproved = false`
- `getMyGarage(UUID userId)` → find by userId
- `updateGarage(UUID userId, UpdateGarageRequest)` → verify ownership, update
- `listApprovedGarages()` → find all where `isApproved = true`

### 3. GarageController

```
POST /api/garages          @PreAuthorize("hasRole('GARAGE')")
GET  /api/garages/me       @PreAuthorize("hasRole('GARAGE')")
PUT  /api/garages/me       @PreAuthorize("hasRole('GARAGE')")
GET  /api/garages          Public (returns only approved)
```

---

## Acceptance Tests

### Test 1: Create garage — happy path

```
POST /api/garages
Authorization: Bearer <garage-token>

{
  "businessName": "AutoService Pro",
  "address": "Str. Republicii 10",
  "city": "Cluj-Napoca",
  "state": "Cluj",
  "postalCode": "400001",
  "phone": "+40741000000",
  "description": "Full service auto repair",
  "services": ["Oil Change", "Brake Service", "Engine Repair"]
}
```

**Pass criteria:**
- Status `201 Created`
- Response contains garage with `id`, `isApproved: false`
- Database: `garages` has 1 row with correct `user_id`

### Test 2: Create garage — car owner rejected

```
POST /api/garages
Authorization: Bearer <car-owner-token>
{ ... }
```

**Pass criteria:** Status `403 Forbidden`

### Test 3: Create garage — duplicate rejected

```
POST /api/garages
Authorization: Bearer <same-garage-token>
{ ... same garage user tries again ... }
```

**Pass criteria:** Status `409 Conflict` with error message

### Test 4: Get own garage

```
GET /api/garages/me
Authorization: Bearer <garage-token>
```

**Pass criteria:**
- Status `200 OK`
- Returns the garage profile for this user

### Test 5: Update own garage

```
PUT /api/garages/me
Authorization: Bearer <garage-token>

{
  "description": "Updated description",
  "services": ["Oil Change", "Tire Service"]
}
```

**Pass criteria:**
- Status `200 OK`
- Subsequent GET reflects updated values

### Test 6: List approved garages — public

```
GET /api/garages
(no auth required)
```

**Pass criteria:**
- Status `200 OK`
- Returns only garages with `isApproved = true`
- Unapproved garages are NOT in the list

### Test 7: List garages — empty when none approved

After creating a garage (which starts unapproved):

```
GET /api/garages
```

**Pass criteria:** Status `200 OK`, empty list

### Test 8: Integration test — create then retrieve

```java
@Test
void createGarageThenGet() {
    AuthResponse auth = registerGarage();

    // Create garage
    var req = new CreateGarageRequest("My Garage", "Addr", "City", "State", "12345", "0741000000", null, null);
    rest.postForEntity("/api/garages", withAuth(auth, req), GarageResponse.class);

    // Get own garage
    ResponseEntity<GarageResponse> resp = rest.exchange(
        "/api/garages/me", HttpMethod.GET, withAuth(auth), GarageResponse.class
    );
    assertEquals("My Garage", resp.getBody().getBusinessName());
    assertFalse(resp.getBody().getIsApproved());
}
```

---

## Definition of Done

- [ ] `POST /api/garages` creates a garage for GARAGE users (one per user)
- [ ] `GET /api/garages/me` returns the authenticated garage user's profile
- [ ] `PUT /api/garages/me` updates the garage profile
- [ ] `GET /api/garages` returns only approved garages (public)
- [ ] Car owners cannot create garages
- [ ] Duplicate garage creation is rejected
- [ ] All 8 acceptance tests pass
