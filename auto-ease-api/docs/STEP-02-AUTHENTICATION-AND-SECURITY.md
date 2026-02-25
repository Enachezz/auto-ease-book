# Step 02 — Authentication & Security

> Part of the [AutoFix Java Backend MVP](./LOVABLE-TO-JAVA-MIGRATION.md).
> Depends on: [Step 01 — Project Setup & Schema](./STEP-01-PROJECT-SETUP-AND-SCHEMA.md)

---

## Goal

Users can register and log in. The API returns a JWT token. All subsequent requests are authenticated via `Authorization: Bearer <token>`. Roles (`CAR_OWNER`, `GARAGE`, `ADMIN`) are embedded in the token and enforced by Spring Security.

---

## Scope

### Do

- Extend existing `AppUser` entity with `password` field and update `AppUserType` enum values
- `Profile` entity (created on registration, UUID PK, FK → `APP_USER.uuid`)
- Registration endpoint: `POST /api/auth/register`
- Login endpoint: `POST /api/auth/login`
- BCrypt password hashing
- JWT generation (with userId, email, role in claims)
- JWT validation filter (reads Bearer token, sets SecurityContext)
- Replace temporary `SecurityConfig` with proper role-based one
- DTOs: `RegisterRequest`, `LoginRequest`, `AuthResponse`
- JWT config in `application.yaml`

### Don't

- No profile CRUD yet (Step 03)
- No email verification
- No password reset
- No refresh tokens (MVP simplicity)

---

## Schema Notes

### What already exists (keep as-is)

- `APP_USER` table: `uuid VARCHAR(50)` PK, `email`, `phone`, `type`, `created_date`, `modified_date`
- V2 migration already adds: `password VARCHAR(255)` to `APP_USER`
- `profiles` table: UUID PK, `user_id VARCHAR(50)` FK → `APP_USER.uuid`, `user_type`, `full_name`, `email`, `phone`, `avatar_url`, `created_date`, `modified_date`

### PK strategy

- **User-related entities** (`AppUser`, `Profile`, `Garage`): UUID / VARCHAR(50) PKs — these predate or relate to `APP_USER.uuid`
- **Service/transactional entities** (`ServiceCategory`, `CarMake`, `CarModel`, `JobRequest`, `Quote`, `Booking`, `Review`): `SERIAL INTEGER` PKs — these extend `BaseEntity`

This means `AppUser` and `Profile` do **not** extend `BaseEntity` (different PK type). They manage their own `created_date` / `modified_date` with `@PrePersist` / `@PreUpdate`, matching the existing `AppUser` pattern.

---

## Tasks

### 1. Update `AppUserType` enum

Rename existing values to match Lovable schema. Existing: `CLIENT`, `SERVICE`. New:

```java
public enum AppUserType {
    CAR_OWNER,
    GARAGE,
    ADMIN
}
```

> **Note:** This is a breaking change on any existing data in `APP_USER.type`. For `db_test` this is fine (drop and recreate or run a migration). Add a V4 migration to handle it if needed.

### 2. Update `AppUser` entity

The entity already exists at `com.api.auto_ease.domain.appUser.AppUser`. Add the `password` field and align field names. Keep existing structure — does **not** extend `BaseEntity`.

```java
@Entity
@Table(name = "APP_USER")
public class AppUser {

    @Id
    @Column(name = "uuid", unique = true, nullable = false, length = 50)
    private String uuid;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private AppUserType type;

    @Column(name = "password")
    private String password;     // BCrypt hash — added by V2 migration

    @PreUpdate
    void onUpdate() { modifiedDate = LocalDateTime.now(); }

    @PrePersist
    void onPersist() { modifiedDate = createdDate = LocalDateTime.now(); }
}
```

`uuid` is set to `UUID.randomUUID().toString()` in the service before save (no DB-generated value since the column is `VARCHAR(50)` not `UUID`).

### 3. `Profile` entity (new)

UUID PK, does **not** extend `BaseEntity`. Timestamps use `created_at` / `updated_at` (matching V2 SQL column names).

```java
@Entity
@Table(name = "profiles")
public class Profile {

    @Id
    @Column(columnDefinition = "uuid", updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;          // FK → APP_USER.uuid

    @Column(name = "user_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AppUserType userType;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;

    @PrePersist
    void onPersist() {
        id = UUID.randomUUID();
        modifiedDate = createdDate = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() { modifiedDate = LocalDateTime.now(); }
}
```

Registration creates both `AppUser` + `Profile` in a single `@Transactional` call.

### 4. DTOs

```java
// RegisterRequest
{ email, password, fullName, phone, userType }   // userType: "CAR_OWNER" | "GARAGE"

// LoginRequest
{ email, password }

// AuthResponse
{ token, userId, email, fullName, userType }
```

### 5. `AuthService`

- `register(RegisterRequest)` → validate email unique, BCrypt hash password, set `uuid = UUID.randomUUID().toString()`, save `AppUser` + `Profile`, return JWT
- `login(LoginRequest)` → find `AppUser` by email, verify BCrypt, return JWT

### 6. `JwtService`

- `generateToken(String uuid, String email, AppUserType role)` → JWT with claims: `sub=uuid`, `email`, `role`
- `validateToken(String token)` → parse and validate; throw on invalid/expired
- `extractUuid(String token)` → String
- Secret and expiry from `application.yaml`
- Expiration: 24h

### 7. `JwtAuthFilter`

A `OncePerRequestFilter` that:
1. Reads `Authorization: Bearer <token>` header
2. Validates token via `JwtService`
3. Creates `UsernamePasswordAuthenticationToken` with `ROLE_<userType>` granted authority
4. Sets `SecurityContextHolder`

### 8. Replace `SecurityConfig`

Replace the temporary permit-all `SecurityConfig` with proper JWT-based config:

```
Public (no auth required):
  POST /api/auth/register
  POST /api/auth/login
  GET  /api/car-makes/**
  GET  /api/car-models/**
  GET  /api/service-categories
  GET  /api/garages (public listing)

Authenticated (any valid JWT):
  Everything else

Role-specific:
  Enforced per-endpoint with @PreAuthorize in later steps
```

### 9. `application.yaml` additions

```yaml
jwt:
  secret: <a-random-256-bit-base64-encoded-secret>
  expiration-ms: 86400000
```

---

## Acceptance Tests

### Test 1: Register — happy path (cURL)

```bash
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"owner@test.com","password":"password123","fullName":"Test Owner","userType":"CAR_OWNER"}'
```

**Pass criteria:**
- Status `200 OK` (or `201 Created`)
- Response contains `token`, `userId`, `email`, `fullName`, `userType`
- Token is a valid JWT string (3 dot-separated parts)
- `APP_USER` table: 1 row, password is BCrypt hash (starts with `$2a$`)
- `profiles` table: 1 row with `user_type = 'CAR_OWNER'`

### Test 2: Register — duplicate email (cURL)

```bash
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"owner@test.com","password":"pass","fullName":"Dup","userType":"CAR_OWNER"}'
```

**Pass criteria:** Status `409 Conflict` with error message

### Test 3: Register — missing fields (cURL)

```bash
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"bad@test.com"}'
```

**Pass criteria:** Status `400 Bad Request` with validation errors

### Test 4: Login — happy path (cURL)

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"owner@test.com","password":"password123"}'
```

**Pass criteria:**
- Status `200 OK`
- Response contains valid `token`
- Token claims contain correct `sub` (uuid), `email`, `role`

### Test 5: Login — wrong password (cURL)

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"owner@test.com","password":"wrong"}'
```

**Pass criteria:** Status `401 Unauthorized`

### Test 6: Authenticated request with valid token (cURL)

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"owner@test.com","password":"password123"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

curl -s http://localhost:8080/api/profiles/me \
  -H "Authorization: Bearer $TOKEN"
```

**Pass criteria:** Does not return `401` or `403`

### Test 7: Unauthenticated request rejected (cURL)

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/profiles/me
```

**Pass criteria:** `401`

### Test 8: Unit test — JwtService

```java
@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("test-secret-that-is-long-enough-for-hs256", 86400000L);
    }

    @Test
    void generateAndValidateToken() {
        String uuid = UUID.randomUUID().toString();
        String token = jwtService.generateToken(uuid, "test@test.com", AppUserType.CAR_OWNER);
        assertNotNull(token);
        assertEquals(3, token.split("\\.").length);   // valid JWT format

        Claims claims = jwtService.validateToken(token);
        assertEquals(uuid, claims.getSubject());
        assertEquals("CAR_OWNER", claims.get("role"));
        assertEquals("test@test.com", claims.get("email"));
    }

    @Test
    void expiredTokenThrows() {
        JwtService shortLived = new JwtService("test-secret-long-enough-for-hs256-algo", -1000L);
        String token = shortLived.generateToken("uuid", "e@e.com", AppUserType.GARAGE);
        assertThrows(Exception.class, () -> jwtService.validateToken(token));
    }
}
```

### Test 9: Integration test — register + login flow

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class AuthIntegrationTest {

    @Autowired TestRestTemplate rest;

    @Test
    void registerThenLogin() {
        var req = Map.of(
            "email", "integration@test.com",
            "password", "pass123",
            "fullName", "Integration User",
            "userType", "CAR_OWNER"
        );

        // Register
        var regResp = rest.postForEntity("/api/auth/register", req, Map.class);
        assertEquals(HttpStatus.OK, regResp.getStatusCode());
        assertNotNull(regResp.getBody().get("token"));

        // Login
        var loginResp = rest.postForEntity("/api/auth/login",
            Map.of("email", "integration@test.com", "password", "pass123"), Map.class);
        assertEquals(HttpStatus.OK, loginResp.getStatusCode());
        assertNotNull(loginResp.getBody().get("token"));
    }

    @Test
    void duplicateEmailReturns409() {
        var req = Map.of("email", "dup@test.com", "password", "p", "fullName", "D", "userType", "GARAGE");
        rest.postForEntity("/api/auth/register", req, Map.class);
        var resp = rest.postForEntity("/api/auth/register", req, Map.class);
        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
    }
}
```

---

## Definition of Done

- [ ] `AppUserType` enum updated to `CAR_OWNER`, `GARAGE`, `ADMIN`
- [ ] `AppUser` entity has `password` field (BCrypt hashed on save)
- [ ] `Profile` entity created, linked to `AppUser` by `userId` (VARCHAR)
- [ ] `POST /api/auth/register` creates `AppUser` + `Profile` in one transaction, returns JWT
- [ ] `POST /api/auth/login` validates credentials, returns JWT
- [ ] Passwords stored as BCrypt hashes (never plaintext)
- [ ] JWT contains `uuid`, `email`, `role`
- [ ] `JwtAuthFilter` validates token and sets `SecurityContext`
- [ ] Unauthenticated requests to protected endpoints return `401`
- [ ] Public endpoints work without a token
- [ ] JWT config in `application.yaml`
- [ ] All 9 acceptance tests pass
