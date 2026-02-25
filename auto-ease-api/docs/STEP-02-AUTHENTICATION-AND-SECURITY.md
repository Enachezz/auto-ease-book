# Step 02 — Authentication & Security

> Part of the [AutoFix Java Backend MVP](./LOVABLE-TO-JAVA-MIGRATION.md).
> Depends on: [Step 01 — Project Setup & Schema](./STEP-01-PROJECT-SETUP-AND-SCHEMA.md)

---

## Goal

Users can register and log in. The API returns a JWT token. All subsequent requests are authenticated via `Authorization: Bearer <token>`. Roles (CAR_OWNER, GARAGE) are embedded in the token and enforced by Spring Security.

---

## Scope

### Do

- `AppUser` entity (id, email, password, createdAt, updatedAt) — this is the auth identity
- `UserType` enum: `CAR_OWNER`, `GARAGE`, `ADMIN`
- Registration endpoint: `POST /api/auth/register`
- Login endpoint: `POST /api/auth/login`
- BCrypt password hashing
- JWT generation (with userId, email, role in claims)
- JWT validation filter (reads Bearer token, sets SecurityContext)
- `SecurityFilterChain` config: public endpoints vs authenticated
- DTOs: `RegisterRequest`, `LoginRequest`, `AuthResponse`

### Don't

- No profile CRUD yet (Step 03)
- No email verification
- No password reset
- No refresh tokens (MVP simplicity)

---

## Tasks

### 1. AppUser entity

```java
@Entity
@Table(name = "app_users")
public class AppUser {
    @Id
    private UUID id;             // gen_random_uuid() or UUID.randomUUID()
    private String email;        // unique
    private String password;     // BCrypt hash
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 2. Profile entity (minimal — just created on registration)

```java
@Entity
@Table(name = "profiles")
public class Profile {
    @Id
    private UUID id;
    private UUID userId;          // FK → app_users
    @Enumerated(EnumType.STRING)
    private UserType userType;
    private String fullName;
    private String email;
    private String phone;
}
```

Registration creates both `AppUser` + `Profile` in a single transaction.

### 3. DTOs

```java
// RegisterRequest
{ email, password, fullName, userType }

// LoginRequest
{ email, password }

// AuthResponse
{ token, userId, email, fullName, userType }
```

### 4. AuthService

- `register(RegisterRequest)` → validate, hash password, save AppUser + Profile, generate JWT
- `login(LoginRequest)` → find user by email, verify BCrypt, generate JWT

### 5. JwtService

- `generateToken(AppUser, UserType)` → JWT with claims: sub=userId, email, role
- `validateToken(String token)` → parse and validate; return claims
- `extractUserId(String token)` → UUID
- JWT secret from `application.properties`: `jwt.secret=...`
- Expiration: 24h (configurable)

### 6. JwtAuthFilter

A `OncePerRequestFilter` that:
1. Reads `Authorization: Bearer <token>` header
2. Validates the token via JwtService
3. Creates `UsernamePasswordAuthenticationToken` with role
4. Sets `SecurityContextHolder`

### 7. SecurityConfig

```
Public (no auth):
  POST /api/auth/register
  POST /api/auth/login
  GET  /api/car-makes/**
  GET  /api/car-models/**
  GET  /api/service-categories
  GET  /api/garages (public listing)

Authenticated (any role):
  Everything else

Role-specific:
  Handled per-endpoint with @PreAuthorize in later steps
```

### 8. application.properties additions

```properties
jwt.secret=<a-random-256-bit-secret>
jwt.expiration-ms=86400000
```

---

## Acceptance Tests

### Test 1: Register — happy path

```
POST /api/auth/register
Content-Type: application/json

{
  "email": "owner@test.com",
  "password": "password123",
  "fullName": "Test Owner",
  "userType": "CAR_OWNER"
}
```

**Pass criteria:**
- Status `200 OK` (or `201 Created`)
- Response contains `token`, `userId`, `email`, `fullName`, `userType`
- Token is a valid JWT string (3 dot-separated parts)
- Database: `app_users` has 1 row with BCrypt-hashed password
- Database: `profiles` has 1 row with `user_type = 'CAR_OWNER'`

### Test 2: Register — duplicate email

```
POST /api/auth/register
{ "email": "owner@test.com", "password": "pass", "fullName": "Dup", "userType": "CAR_OWNER" }
```

**Pass criteria:** Status `409 Conflict` (or `400`) with error message

### Test 3: Register — missing fields

```
POST /api/auth/register
{ "email": "bad@test.com" }
```

**Pass criteria:** Status `400 Bad Request` with validation errors

### Test 4: Login — happy path

```
POST /api/auth/login
{ "email": "owner@test.com", "password": "password123" }
```

**Pass criteria:**
- Status `200 OK`
- Response contains valid `token`
- Token claims contain correct `sub`, `email`, `role`

### Test 5: Login — wrong password

```
POST /api/auth/login
{ "email": "owner@test.com", "password": "wrong" }
```

**Pass criteria:** Status `401 Unauthorized`

### Test 6: Login — nonexistent email

```
POST /api/auth/login
{ "email": "nobody@test.com", "password": "pass" }
```

**Pass criteria:** Status `401 Unauthorized`

### Test 7: Authenticated request with valid token

```
GET /api/profiles/me
Authorization: Bearer <token-from-login>
```

**Pass criteria:** Does not return `401` or `403` (may return 404 if profile endpoint isn't built yet — that's fine for this step, just not a security rejection)

### Test 8: Unauthenticated request rejected

```
GET /api/profiles/me
(no Authorization header)
```

**Pass criteria:** Status `401 Unauthorized`

### Test 9: Unit test — JwtService

```java
@Test
void generateAndValidateToken() {
    String token = jwtService.generateToken(userId, email, UserType.CAR_OWNER);
    assertNotNull(token);

    Claims claims = jwtService.validateToken(token);
    assertEquals(userId.toString(), claims.getSubject());
    assertEquals("CAR_OWNER", claims.get("role"));
}

@Test
void expiredTokenFails() {
    // Generate token with -1h expiry, assert validation throws
}
```

### Test 10: Integration test — full register + login flow

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class AuthIntegrationTest {

    @Autowired TestRestTemplate rest;

    @Test
    void registerThenLogin() {
        // 1. Register
        var registerReq = new RegisterRequest("test@test.com", "pass123", "Test", UserType.CAR_OWNER);
        var registerResp = rest.postForEntity("/api/auth/register", registerReq, AuthResponse.class);
        assertEquals(HttpStatus.OK, registerResp.getStatusCode());
        assertNotNull(registerResp.getBody().getToken());

        // 2. Login with same creds
        var loginReq = new LoginRequest("test@test.com", "pass123");
        var loginResp = rest.postForEntity("/api/auth/login", loginReq, AuthResponse.class);
        assertEquals(HttpStatus.OK, loginResp.getStatusCode());
        assertNotNull(loginResp.getBody().getToken());
    }
}
```

---

## Definition of Done

- [ ] `POST /api/auth/register` creates user + profile, returns JWT
- [ ] `POST /api/auth/login` validates credentials, returns JWT
- [ ] Passwords stored as BCrypt hashes (never plaintext)
- [ ] JWT contains userId, email, role
- [ ] Unauthenticated requests to protected endpoints return 401
- [ ] Public endpoints work without a token
- [ ] All 10 acceptance tests pass
