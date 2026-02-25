# Step 04 — Vehicle Management

> Part of the [AutoFix Java Backend MVP](./LOVABLE-TO-JAVA-MIGRATION.md).
> Depends on: [Step 03 — Profiles & Reference Data](./STEP-03-PROFILES-AND-REFERENCE-DATA.md)

---

## Goal

Car owners can add, list, update, and delete their vehicles. Each car is linked to the owner and references car makes/models from the reference data. Vehicles are required context for every job request.

---

## Scope

### Do

- Car entity, repository, service, controller
- `POST /api/cars` — add a car (CAR_OWNER only)
- `GET /api/cars` — list own cars (CAR_OWNER only)
- `PUT /api/cars/{id}` — update own car
- `DELETE /api/cars/{id}` — delete own car
- Ownership enforcement: users can only access their own cars
- DTOs: `CreateCarRequest`, `UpdateCarRequest`, `CarResponse`

### Don't

- No VIN decoding
- Garages don't need car endpoints

---

## Tasks

### 1. Car entity

```java
@Entity @Table(name = "cars")
public class Car {
    @Id private UUID id;
    private UUID userId;        // FK → app_users (owner)
    private UUID makeId;        // FK → car_makes
    private UUID modelId;       // FK → car_models
    private Integer year;
    private String color;
    private String licensePlate;
    private String vin;
    private Integer mileage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 2. CarService

- `addCar(UUID userId, CreateCarRequest)` → validate makeId/modelId exist, save
- `getMyCars(UUID userId)` → list cars where userId matches
- `updateCar(UUID userId, UUID carId, UpdateCarRequest)` → verify ownership, update
- `deleteCar(UUID userId, UUID carId)` → verify ownership, delete

### 3. CarController

```
POST   /api/cars          @PreAuthorize("hasRole('CAR_OWNER')")
GET    /api/cars           @PreAuthorize("hasRole('CAR_OWNER')")
PUT    /api/cars/{id}      @PreAuthorize("hasRole('CAR_OWNER')")
DELETE /api/cars/{id}      @PreAuthorize("hasRole('CAR_OWNER')")
```

All endpoints extract `userId` from the JWT SecurityContext.

### 4. CarResponse DTO

Include resolved make/model names (not just IDs):

```json
{
  "id": "uuid",
  "makeName": "Toyota",
  "modelName": "Corolla",
  "year": 2020,
  "color": "White",
  "licensePlate": "B-123-ABC",
  "vin": "1HGBH41JXMN109186",
  "mileage": 45000
}
```

---

## Acceptance Tests

### Test 1: Add car — happy path

```
POST /api/cars
Authorization: Bearer <car-owner-token>

{
  "makeId": "<toyota-uuid>",
  "modelId": "<corolla-uuid>",
  "year": 2020,
  "color": "White",
  "licensePlate": "B-123-ABC"
}
```

**Pass criteria:**
- Status `201 Created`
- Response contains car with `id`, resolved `makeName`, `modelName`
- Database: `cars` has 1 row with correct `user_id`

### Test 2: Add car — garage user rejected

```
POST /api/cars
Authorization: Bearer <garage-token>
{ ... }
```

**Pass criteria:** Status `403 Forbidden`

### Test 3: List own cars

```
GET /api/cars
Authorization: Bearer <car-owner-token>
```

**Pass criteria:**
- Status `200 OK`
- Returns only cars belonging to this user
- Does not include cars from other users

### Test 4: Update own car

```
PUT /api/cars/{carId}
Authorization: Bearer <car-owner-token>

{
  "mileage": 50000,
  "vin": "1HGBH41JXMN109186"
}
```

**Pass criteria:**
- Status `200 OK`
- Subsequent GET returns updated values
- `userId` unchanged

### Test 5: Update someone else's car

```
PUT /api/cars/{other-user-car-id}
Authorization: Bearer <different-owner-token>
```

**Pass criteria:** Status `403 Forbidden` or `404 Not Found`

### Test 6: Delete own car

```
DELETE /api/cars/{carId}
Authorization: Bearer <car-owner-token>
```

**Pass criteria:**
- Status `204 No Content` (or `200`)
- Car no longer appears in `GET /api/cars`

### Test 7: Add car — invalid makeId

```
POST /api/cars
Authorization: Bearer <car-owner-token>
{ "makeId": "00000000-...", "modelId": "...", "year": 2020 }
```

**Pass criteria:** Status `400 Bad Request` with descriptive error

### Test 8: Integration test — add then list

```java
@Test
void addCarThenList() {
    AuthResponse auth = registerCarOwner();

    // Add a car
    CreateCarRequest req = new CreateCarRequest(toyotaId, corollaId, 2020, "Red", null, null, null);
    rest.postForEntity("/api/cars", withAuth(auth, req), CarResponse.class);

    // List cars
    ResponseEntity<CarResponse[]> list = rest.exchange(
        "/api/cars", HttpMethod.GET, withAuth(auth), CarResponse[].class
    );
    assertEquals(1, list.getBody().length);
    assertEquals("Toyota", list.getBody()[0].getMakeName());
}
```

---

## Definition of Done

- [ ] `POST /api/cars` creates a car linked to the authenticated owner
- [ ] `GET /api/cars` returns only the authenticated user's cars
- [ ] `PUT /api/cars/{id}` updates only if user owns the car
- [ ] `DELETE /api/cars/{id}` deletes only if user owns the car
- [ ] Garage users cannot access car endpoints
- [ ] Response includes resolved make/model names
- [ ] All 8 acceptance tests pass
