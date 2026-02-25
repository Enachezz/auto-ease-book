# Step 01 — Project Setup & Schema

> Part of the [AutoFix Java Backend MVP](./LOVABLE-TO-JAVA-MIGRATION.md).
> This is the foundation step. Nothing else works without this.

---

## Goal

Get the Spring Boot project into a clean, runnable state with the correct database schema, seed data, and project dependencies. After this step, `mvn spring-boot:run` starts the app and Flyway creates all tables.

---

## Scope

### Do

- Create `application.yml` (DB connection, server port, Flyway config)
- Clean up `pom.xml` (remove unused deps: batch, session-jdbc, websocket; add JWT library)
- Add new Flyway migrations for the target schema (keep existing V1, V1_1 untouched)
- Add seed data (service categories, car makes, car models)
- Verify the app starts and Flyway runs cleanly against a local Postgres

### Don't

- No controllers, services, or business logic yet
- No auth yet
- No entity Java classes yet (just the SQL schema)

---

## Prerequisites

- Java 21 installed
- PostgreSQL running locally (e.g. `localhost:5432`)
- A database created (e.g. `CREATE DATABASE autoease;`)

---

## Tasks

### 1. Create `application.yml`

Location: `src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/autoease
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  flyway:
    enabled: true
    locations: classpath:db/migration
```

### 2. Clean up `pom.xml`

**Remove** these dependencies (not needed for MVP):
- `spring-boot-starter-batch` + its test dep
- `spring-boot-starter-session-jdbc` + its test dep
- `spring-boot-starter-websocket` + its test dep

**Add** (for JWT, needed in Step 02):
- `io.jsonwebtoken:jjwt-api`
- `io.jsonwebtoken:jjwt-impl` (runtime)
- `io.jsonwebtoken:jjwt-jackson` (runtime)

**Add** (for tests without running Postgres):
- `com.h2database:h2` (test scope)

**Keep**:
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-flyway`
- `spring-boot-starter-security`
- `spring-boot-starter-webmvc`
- `flyway-database-postgresql`
- `postgresql`
- `lombok`

**Note:** Spring Boot 4.0.0-SNAPSHOT may cause issues. Consider switching to a stable release (e.g. 3.4.x) if snapshot repos are unreliable.

### 3. Add new Flyway migrations

Keep existing migrations untouched:
- `V1__createBasicDomainNeeds.sql` — creates SERVICE_ENTRY, RATING, REQUEST_TYPE, CAR, DOCUMENT, APP_USER
- `V1_1__createServiceEntity.sql` — creates SERVICE, SPECIALIZATION, SERVICE_SPECIALIZATION

Add new migration: `V2__create_target_schema.sql`

New tables (see design doc Section 4 for full field details):
- `profiles` — id (UUID PK), user_id (FK → APP_USER), user_type, full_name, email, phone, avatar_url
- `garages` — id (UUID PK), user_id (FK → APP_USER), business_name, address, city, state, postal_code, phone, description, services (TEXT[]), opening_hours (JSONB), is_approved, average_rating, total_reviews, lat, lng
- `service_categories` — id (UUID PK), name, description, icon
- `car_makes` — id (UUID PK), name
- `car_models` — id (UUID PK), make_id (FK → car_makes), name, UNIQUE(make_id, name)
- `job_requests` — id (UUID PK), user_id (FK), car_id (FK → CAR), category_id (FK), title, description, urgency, preferred_date, budget_min, budget_max, status, location fields
- `quotes` — id (UUID PK), job_request_id (FK), garage_id (FK), price, estimated_duration, description, warranty_info, status, expires_at, UNIQUE(job_request_id, garage_id)
- `bookings` — id (UUID PK), quote_id (FK UNIQUE), scheduled_date, scheduled_time, status, notes
- `reviews` — id (UUID PK), booking_id (FK UNIQUE), garage_id (FK), user_id (FK), rating (1-5 CHECK), comment

Also ALTER APP_USER to add a `password` column (VARCHAR 255, nullable for now).

### 4. Create seed data migration: `V3__seed_data.sql`

- 10 service categories (from Lovable: Oil Change, Brake Service, Engine Repair, etc.)
- 15 car makes (Toyota, Honda, Ford, etc.)
- Car models for top 3 makes (5 models each)

### 5. Temporarily disable Spring Security

Until Step 02, add a minimal security config that permits all requests (so the app can start without 401s).

---

## Acceptance Tests

### Test 1: App starts cleanly

```
# From auto-ease-api directory
mvn spring-boot:run
```

**Pass criteria:**
- App starts on port 8080 without errors
- Logs show Flyway migrations applied: `V1__createBasicDomainNeeds.sql`, `V1_1__createServiceEntity.sql`, `V2__create_target_schema.sql`, `V3__seed_data.sql`
- No Hibernate validation errors

### Test 2: Tables exist

Connect to the database and verify:

```sql
SELECT table_name FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;
```

**Expected tables (from V1 + V1_1):** app_user, car, document, rating, request_type, service, service_entry, service_specialization, specialization

**Expected tables (from V2):** profiles, garages, service_categories, car_makes, car_models, job_requests, quotes, bookings, reviews

### Test 3: Seed data loaded

```sql
SELECT COUNT(*) FROM service_categories;  -- Expected: 10
SELECT COUNT(*) FROM car_makes;           -- Expected: 15
SELECT COUNT(*) FROM car_models;          -- Expected: 15 (5 × 3 makes)
```

### Test 4: Health check responds

```
curl http://localhost:8080/actuator/health
```

Or simply: `curl http://localhost:8080/` should not return a connection error.

### Test 5: Unit test — context loads

```java
@SpringBootTest
class AutoEaseApplicationTests {
    @Test
    void contextLoads() {
        // Passes if Spring context starts without errors
    }
}
```

Run: `mvn test`

**Pass criteria:** All tests green, context loads successfully.

---

## Definition of Done

- [ ] `application.yml` exists and configures DB + Flyway
- [ ] `pom.xml` cleaned up (no batch/session/websocket)
- [ ] Existing Flyway migrations kept (V1, V1_1)
- [ ] New `V2__create_target_schema.sql` creates all 9 new tables + alters APP_USER
- [ ] `V3__seed_data.sql` seeds categories, makes, models
- [ ] Temporary SecurityConfig permits all requests
- [ ] `mvn spring-boot:run` starts without errors
- [ ] All 5 acceptance tests pass
