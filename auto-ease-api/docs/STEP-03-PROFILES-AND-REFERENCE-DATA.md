# Step 03 — User Profiles & Reference Data

> Part of the [AutoFix Java Backend MVP](./LOVABLE-TO-JAVA-MIGRATION.md).
> Depends on: [Step 02 — Authentication & Security](./STEP-02-AUTHENTICATION-AND-SECURITY.md)

---

## Goal

Authenticated users can view and update their profile. Public endpoints serve reference data (car makes, car models, service categories) needed by the frontend dropdowns and forms.

---

## Scope

### Do

- Profile entity, repository, service, controller
  - `GET /api/profiles/me` — get own profile
  - `PUT /api/profiles/me` — update own profile (fullName, phone, avatarUrl)
- CarMake entity, repository, controller
  - `GET /api/car-makes` — list all makes
- CarModel entity, repository, controller
  - `GET /api/car-makes/{makeId}/models` — list models for a make
- ServiceCategory entity, repository, controller
  - `GET /api/service-categories` — list all categories

### Don't

- No admin CRUD for reference data (seeded via Flyway)
- No avatar upload (just URL field)

---

## Tasks

### 1. Profile controller

The profile is already created during registration (Step 02). This step adds read/update.

```
GET  /api/profiles/me         → Profile for the authenticated user
PUT  /api/profiles/me         → Update fullName, phone, avatarUrl
```

The `userId` comes from the JWT (SecurityContext), not from a path parameter. Users can only access their own profile.

### 2. CarMake + CarModel entities

```java
@Entity @Table(name = "car_makes")
public class CarMake {
    @Id private UUID id;
    private String name;
}

@Entity @Table(name = "car_models")
public class CarModel {
    @Id private UUID id;
    private UUID makeId;
    private String name;
}
```

### 3. ServiceCategory entity

```java
@Entity @Table(name = "service_categories")
public class ServiceCategory {
    @Id private UUID id;
    private String name;
    private String description;
    private String icon;
}
```

### 4. Read-only controllers

These are public endpoints (configured in SecurityConfig from Step 02):

```
GET /api/car-makes                      → List<CarMakeResponse>
GET /api/car-makes/{makeId}/models      → List<CarModelResponse>
GET /api/service-categories             → List<ServiceCategoryResponse>
```

---

## Acceptance Tests

### Test 1: Get own profile (authenticated)

```
GET /api/profiles/me
Authorization: Bearer <car-owner-token>
```

**Pass criteria:**
- Status `200 OK`
- Response contains: `id`, `userId`, `fullName`, `email`, `userType`, `phone`
- `userType` matches what was set during registration

### Test 2: Update own profile

```
PUT /api/profiles/me
Authorization: Bearer <token>
Content-Type: application/json

{
  "fullName": "Updated Name",
  "phone": "+40712345678"
}
```

**Pass criteria:**
- Status `200 OK`
- Subsequent `GET /api/profiles/me` returns updated values
- `email` and `userType` unchanged

### Test 3: Profile — unauthenticated

```
GET /api/profiles/me
(no token)
```

**Pass criteria:** Status `401`

### Test 4: List car makes

```
GET /api/car-makes
```

**Pass criteria:**
- Status `200 OK`
- Returns 15 makes (from seed data)
- Each has `id` and `name`
- No auth required

### Test 5: List models for a make

```
GET /api/car-makes/{toyotaId}/models
```

**Pass criteria:**
- Status `200 OK`
- Returns 5 models for Toyota (Camry, Corolla, RAV4, Prius, Highlander)
- Each has `id`, `makeId`, `name`

### Test 6: Models for nonexistent make

```
GET /api/car-makes/00000000-0000-0000-0000-000000000000/models
```

**Pass criteria:** Status `200 OK` with empty list (or `404`)

### Test 7: List service categories

```
GET /api/service-categories
```

**Pass criteria:**
- Status `200 OK`
- Returns 10 categories (from seed data)
- Each has `id`, `name`, `description`, `icon`

### Test 8: Integration test — register then get profile

```java
@Test
void registerAndGetProfile() {
    // 1. Register as CAR_OWNER
    AuthResponse auth = register("user@test.com", "pass", "Test User", "CAR_OWNER");

    // 2. GET /api/profiles/me with token
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(auth.getToken());
    ResponseEntity<ProfileResponse> resp = rest.exchange(
        "/api/profiles/me", HttpMethod.GET,
        new HttpEntity<>(headers), ProfileResponse.class
    );

    assertEquals(HttpStatus.OK, resp.getStatusCode());
    assertEquals("Test User", resp.getBody().getFullName());
    assertEquals("CAR_OWNER", resp.getBody().getUserType());
}
```

---

## Definition of Done

- [ ] `GET /api/profiles/me` returns the authenticated user's profile
- [ ] `PUT /api/profiles/me` updates fullName, phone, avatarUrl
- [ ] `GET /api/car-makes` returns all seeded makes (public)
- [ ] `GET /api/car-makes/{id}/models` returns models for a make (public)
- [ ] `GET /api/service-categories` returns all seeded categories (public)
- [ ] All 8 acceptance tests pass
