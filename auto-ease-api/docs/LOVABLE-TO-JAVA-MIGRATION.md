# AutoFix MVP — Java Backend Design Document

**Document Purpose:** Design document for building the Java Spring Boot backend that will replace the Lovable/Supabase backend, resulting in a fully functional app where the React frontend communicates exclusively with the Java API.

**Date:** February 2026

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Architecture: Before & After](#2-architecture-before--after)
3. [MVP Marketplace Loop](#3-mvp-marketplace-loop)
4. [Target Data Model](#4-target-data-model)
5. [Current Java Codebase Audit](#5-current-java-codebase-audit)
6. [Gap Analysis: Java vs Target](#6-gap-analysis-java-vs-target)
7. [Inventory: Keep / Refactor / Build](#7-inventory-keep--refactor--build)
8. [Authentication Design](#8-authentication-design)
9. [Role-Based Access Control](#9-role-based-access-control)
10. [API Endpoints](#10-api-endpoints)
11. [Frontend Migration](#11-frontend-migration)
12. [Effort Estimate](#12-effort-estimate)
13. [Implementation Order](#13-implementation-order)
14. [Appendix A: Current Java Structure](#appendix-a-current-java-structure)
15. [Appendix B: Known Bugs in Current Java Code](#appendix-b-known-bugs-in-current-java-code)

---

## 1. Executive Summary

The AutoFix frontend was generated with **Lovable** and currently runs on **Supabase** (auth, Postgres, storage). The goal of this project is to build a **Java Spring Boot backend** that replaces Supabase entirely, so the app is self-contained and does not depend on Lovable infrastructure.

An initial Java project (`auto-ease-api`) already exists with a skeleton domain model. This document:

- Defines the **target architecture** and data model (derived from the working Lovable app)
- Audits the **existing Java code** against that target
- Classifies every component as **keep**, **refactor**, or **build from scratch**
- Specifies **auth, roles, endpoints, and frontend wiring**
- Provides an **effort estimate** and **implementation order**

**Key decisions:**
- **Auth:** Own auth in Java (registration, login, JWT, BCrypt) — no Supabase dependency
- **Schema:** Lovable's Postgres schema is the source of truth for the Java domain model
- **Frontend:** Will be migrated to call Java REST endpoints instead of Supabase client

---

## 2. Architecture: Before & After

### 2.1 Current State (Lovable / Supabase)

```
┌───────────────────────────────────────────────────────────────────┐
│                 CURRENT: LOVABLE / SUPABASE                       │
│                                                                   │
│   ┌──────────────┐                 ┌───────────────────────────┐  │
│   │   React SPA  │                 │        SUPABASE           │  │
│   │   (Vite)     │────────────────▶│  • Auth (auth.users)      │  │
│   │              │ Supabase Client  │  • Postgres + RLS         │  │
│   │  - useAuth   │                 │  • Storage                │  │
│   │  - supabase  │                 │  • Realtime               │  │
│   └──────────────┘                 └───────────────────────────┘  │
│                                                                   │
│   ┌──────────────┐                                                │
│   │ auto-ease-api│  ◄── Exists but NOT connected to frontend      │
│   │ (Spring Boot)│      Has its own Flyway schema                 │
│   └──────────────┘                                                │
│                                                                   │
└───────────────────────────────────────────────────────────────────┘
```

### 2.2 Target State (Java Backend)

```
┌───────────────────────────────────────────────────────────────────┐
│                 TARGET: JAVA BACKEND                              │
│                                                                   │
│   ┌──────────────┐                 ┌───────────────────────────┐  │
│   │   React SPA  │                 │  Java Spring Boot API     │  │
│   │   (Vite)     │────────────────▶│  • Own Auth (JWT, BCrypt) │  │
│   │              │  REST + JWT      │  • REST Controllers       │  │
│   │  - apiClient │                 │  • JPA + PostgreSQL       │  │
│   │  - useAuth   │                 │  • Role-based Security    │  │
│   └──────────────┘                 └────────────┬──────────────┘  │
│                                                  │                │
│                                                  ▼                │
│                                    ┌───────────────────────────┐  │
│                                    │       PostgreSQL          │  │
│                                    └───────────────────────────┘  │
│                                                                   │
│   Supabase: REMOVED — no dependency                               │
│                                                                   │
└───────────────────────────────────────────────────────────────────┘
```

---

## 3. MVP Marketplace Loop

The product goal is the core marketplace loop. Every backend feature must support this:

```
    Owner posts problem → Garages send quotes → Owner accepts one → Booking created

    ┌─────────┐    ┌──────────────┐    ┌──────────┐    ┌────────────────┐
    │  Owner  │───▶│ Post Request │───▶│  Garage  │───▶│ Accept + Book  │
    │         │    │ (JobRequest) │    │  Quote   │    │  (Booking)     │
    └─────────┘    └──────────────┘    └──────────┘    └────────────────┘
         │                │                  │                  │
         ▼                ▼                  ▼                  ▼
    AppUser          JobRequest           Quote             Booking
    Profile          ServiceCategory      Garage
    Car, CarMake     
    CarModel
```

---

## 4. Target Data Model

The Java domain must implement this schema (derived from Lovable's working Postgres):

```
┌─────────────┐     ┌─────────────┐     ┌──────────────────┐
│  AppUser    │     │   Profile   │     │     Garage        │
├─────────────┤     ├─────────────┤     ├──────────────────┤
│ id (PK,UUID)│◀────│ userId (FK) │     │ id (PK, UUID)     │
│ email       │     │ userType    │     │ userId (FK)       │
│ password    │     │ fullName    │     │ businessName      │
│ createdAt   │     │ email,phone │     │ address, city     │
└─────────────┘     └─────────────┘     │ isApproved        │
                                        │ averageRating     │
                                        │ services[]        │
                                        └──────────────────┘
                                                 │
┌─────────────┐     ┌─────────────┐              │
│  CarMake    │     │  CarModel   │              │
├─────────────┤     ├─────────────┤              │
│ id (PK)     │◀────│ makeId (FK) │              │
│ name        │     │ name        │              │
└─────────────┘     └─────────────┘              │
       │                   │                      │
       └───────────────────┘                      │
                   │                              │
                   ▼                              │
           ┌─────────────┐                        │
           │    Car      │                        │
           ├─────────────┤                        │
           │ id (PK)     │                        │
           │ userId (FK) │                        │
           │ makeId (FK) │                        │
           │ modelId(FK) │                        │
           │ year, vin   │                        │
           └──────┬──────┘                        │
                  │                               │
                  ▼                               │
┌────────────────────┐  ┌───────────────────┐     │
│ ServiceCategory    │  │   JobRequest      │     │
├────────────────────┤  ├───────────────────┤     │
│ id (PK)            │◀─│ categoryId (FK)   │     │
│ name, description  │  │ userId (FK)       │     │
│ icon               │  │ carId (FK)        │     │
└────────────────────┘  │ title, description│     │
                        │ status, urgency   │     │
                        │ preferredDate     │     │
                        │ location          │     │
                        └────────┬──────────┘     │
                                 │                │
                                 ▼                │
                        ┌───────────────────┐     │
                        │     Quote         │     │
                        ├───────────────────┤     │
                        │ id (PK)           │     │
                        │ jobRequestId (FK) │     │
                        │ garageId (FK) ────┼─────┘
                        │ price, status     │
                        │ description       │
                        │ estimatedDuration │
                        │ UNIQUE(jobReq,gar)│
                        └────────┬──────────┘
                                 │
                                 ▼
                        ┌───────────────────┐
                        │    Booking        │
                        ├───────────────────┤
                        │ id (PK)           │
                        │ quoteId (FK, UQ)  │
                        │ scheduledDate     │
                        │ scheduledTime     │
                        │ status            │
                        └────────┬──────────┘
                                 │
                                 ▼
                        ┌───────────────────┐
                        │    Review         │
                        ├───────────────────┤
                        │ id (PK)           │
                        │ bookingId (FK,UQ) │
                        │ garageId (FK)     │
                        │ userId (FK)       │
                        │ rating (1-5)      │
                        │ comment           │
                        └───────────────────┘
```

### Enums & Status Values

| Enum / Field | Values |
|-------------|--------|
| `UserType` | `CAR_OWNER`, `GARAGE`, `ADMIN` |
| `JobRequest.status` | `OPEN`, `QUOTED`, `BOOKED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` |
| `Quote.status` | `PENDING`, `ACCEPTED`, `REJECTED`, `EXPIRED` |
| `Booking.status` | `CONFIRMED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` |
| `JobRequest.urgency` | `LOW`, `MEDIUM`, `HIGH` |

---

## 5. Current Java Codebase Audit

### Existing Entities vs Target

| Target Entity | Existing Java Class | Verdict |
|---------------|---------------------|---------|
| AppUser (auth) | `AppUser` | Refactor — needs password, fullName; types are wrong |
| Profile | — | Build |
| Garage | `Service` | Refactor — fundamentally different (no user link, no approval) |
| Car | `Car` | Refactor — missing userId, FKs for make/model |
| CarMake | — | Build |
| CarModel | — | Build |
| ServiceCategory | `RequestType` | Refactor — simpler, different schema |
| JobRequest | `ServiceEntry` | Replace — different concept entirely |
| Quote | — | Build |
| Booking | — | Build |
| Review | `Rating` | Refactor — wrong relationships |
| Document | `Document` | Keep — may be useful later (not MVP) |

### Existing Infrastructure

| Component | Status |
|-----------|--------|
| Spring Boot + JPA + Flyway | Good — keep |
| Spring Security (dependency in pom.xml) | Good — but no config exists yet |
| WebSocket (dependency) | Not needed for MVP |
| Session JDBC (dependency) | Not needed — JWT is stateless |
| Batch (dependency) | Not needed for MVP |

---

## 6. Gap Analysis: Java vs Target

### Entity Field Comparison

**AppUser (current) vs AppUser + Profile (target)**

| Current Field | Target Field | Gap |
|---------------|-------------|-----|
| uuid (VARCHAR 50) | id (UUID) | Change type |
| email | email | OK |
| phone | phone (on Profile) | Move to Profile |
| type (CLIENT/SERVICE) | — (on Profile as UserType) | Move to Profile, new enum values |
| — | password (hashed) | Missing |
| — | fullName (on Profile) | Missing |
| — | avatarUrl (on Profile) | Missing |
| — | userType (CAR_OWNER/GARAGE/ADMIN) | Missing |

**Car (current) vs Car (target)**

| Current Field | Target Field | Gap |
|---------------|-------------|-----|
| make (String) | makeId (FK → CarMake) | Change to FK |
| model (String) | modelId (FK → CarModel) | Change to FK |
| made (LocalDate) | year (Integer) | Change type |
| — | userId (FK → AppUser) | Missing — critical |
| color, licensePlate, vin, mileage | same | OK |

**Service (current) vs Garage (target)**

| Current Field | Target Field | Gap |
|---------------|-------------|-----|
| uuid | id | OK (rename) |
| name | businessName | Rename |
| address | address, city, state, postalCode | Split into multiple fields |
| phone (Integer!) | phone (String) | Change type |
| email | — (on user) | Remove |
| — | userId (FK → AppUser) | Missing — critical |
| — | isApproved, averageRating, totalReviews | Missing |
| — | services[], openingHours, lat/lng | Missing |

---

## 7. Inventory: Keep / Refactor / Build

### ✅ KEEP — Good foundations to build on

| Item | Location | Why |
|------|----------|-----|
| Package structure | `com.api.auto_ease.*` | Clean controller/service/repository layering |
| BaseEntity | `domain/BaseEntity.java` | Reusable `id`, `createdDate`, `modifiedDate`, lifecycle hooks |
| MessageException | `exception/MessageException.java` | Custom exception pattern |
| Strategy pattern | `serviceEntry/strategy/` | Pattern is sound; implementations will change |
| DTO approach | `serviceEntry/dto/` | Request/response objects — good practice |
| Flyway infrastructure | `resources/db/migration/` | Migration tooling — content will be replaced |
| Spring Boot dependencies | `pom.xml` | JPA, Web, Security, Flyway, Postgres, Lombok — all correct |
| Lombok | All entities | Keeps boilerplate down |

### ⚠️ REFACTOR — Exists but needs significant changes

| Item | What Needs to Change |
|------|---------------------|
| **AppUser** entity | Add `password` (hashed); change `uuid` to UUID type; remove `type` (move to Profile) |
| **AppUserType** enum | Replace `CLIENT`/`SERVICE` with `CAR_OWNER`/`GARAGE`/`ADMIN` |
| **Car** entity | Add `userId` FK; replace `make`/`model` strings with `makeId`/`modelId` FKs; change `made` (Date) to `year` (Integer) |
| **Service** entity | Rename to **Garage**; add `userId`, `isApproved`, `averageRating`, `services[]`, location fields; fix `phone` type |
| **RequestType** entity | Rename to **ServiceCategory**; add `id` (UUID), `description`, `icon` |
| **Rating** entity | Rename to **Review**; link to `bookingId` + `garageId` + `userId` instead of `serviceEntryId` |
| **Flyway V1 migration** | Replace all table DDL with target schema |
| **Flyway V1_1 migration** | Remove or fix (contains `student_id`/`course_id` typo) |
| **AppUserRestController** | Rewrite as proper car/profile endpoints |
| **ServiceRestController** | Replace with JobRequest/Quote/Booking controllers |
| **ServiceEntryService** | Replace with JobRequestService, QuoteService, BookingService |
| **pom.xml** | Remove unneeded: spring-batch, session-jdbc, websocket; add JWT library (e.g. jjwt) |

### ❌ BUILD — Does not exist yet

| Item | Description |
|------|-------------|
| **Auth layer** | Registration + login endpoints; BCrypt password hashing; JWT generation |
| **Spring Security config** | JWT validation filter; SecurityFilterChain with role-based rules |
| **Profile** entity + repo + service + controller | User profile linked to AppUser |
| **CarMake** entity + repo + controller | Read-only reference data |
| **CarModel** entity + repo + controller | Read-only reference data, linked to CarMake |
| **Garage** entity + repo + service + controller | Full CRUD with approval workflow |
| **JobRequest** entity + repo + service + controller | Core marketplace: owner creates, garage views open |
| **Quote** entity + repo + service + controller | Garage submits; owner views for their requests |
| **Booking** entity + repo + service + controller | Created when owner accepts quote; transactional logic |
| **Review** entity + repo + service + controller | Owner reviews completed booking |
| **ServiceCategory** entity + repo + controller | Seeded reference data |
| **application.properties / .yml** | DB URL, JWT secret, CORS config, server port |
| **Seed data migration** | Service categories, car makes/models (from Lovable seeds) |
| **Global exception handler** | `@ControllerAdvice` for consistent error responses |
| **CORS configuration** | Allow frontend origin |

---

## 8. Authentication Design

The Java backend owns the full auth lifecycle. No Supabase dependency.

### Flow

```
    ┌────────────┐                        ┌──────────────────┐
    │  Frontend  │  POST /api/auth/register│  Java Backend    │
    │            │───────────────────────▶│                  │
    │            │  { email, password,     │  • Validate      │
    │            │    fullName, userType } │  • BCrypt hash   │
    │            │                        │  • Save AppUser  │
    │            │◀───────────────────────│  • Create Profile│
    │            │  { token, user }        │  • Return JWT    │
    │            │                        └──────────────────┘
    │            │                        
    │            │  POST /api/auth/login   ┌──────────────────┐
    │            │───────────────────────▶│  • Verify creds   │
    │            │  { email, password }    │  • Generate JWT  │
    │            │◀───────────────────────│  • Return token  │
    │            │  { token, user }        └──────────────────┘
    │            │
    │            │  GET /api/job-requests   ┌──────────────────┐
    │            │───────────────────────▶│  JWT filter       │
    │            │  Authorization: Bearer…│  • Extract userId │
    │            │                        │  • Extract role   │
    │            │◀───────────────────────│  • Authorize      │
    │            │  [ ... ]               └──────────────────┘
    └────────────┘
```

### JWT Payload

```json
{
  "sub": "<user-uuid>",
  "email": "user@example.com",
  "role": "CAR_OWNER",
  "iat": 1738000000,
  "exp": 1738086400
}
```

### Role Mapping

| Signup choice | Java enum | Spring Security role |
|---------------|-----------|---------------------|
| car_owner | `UserType.CAR_OWNER` | `ROLE_CAR_OWNER` |
| garage | `UserType.GARAGE` | `ROLE_GARAGE` |
| admin | `UserType.ADMIN` | `ROLE_ADMIN` |

---

## 9. Role-Based Access Control

Derived from Lovable's RLS policies. The Java backend must enforce these in controllers/services:

| Resource | CAR_OWNER | GARAGE | ADMIN | Public |
|----------|-----------|--------|-------|--------|
| **Auth** | Register, login | Register, login | — | — |
| **Profile** | CRUD own | CRUD own | — | — |
| **Cars** | CRUD own | — | — | — |
| **Car makes/models** | — | — | — | Read all |
| **Service categories** | — | — | — | Read all |
| **Garages** | Read approved | CRUD own; read approved | — | Read approved |
| **Job requests** | CRUD own | Read open | — | — |
| **Quotes** | Read for own requests | CRUD for own garage | — | — |
| **Bookings** | Create (on accept); read own | Read own | — | — |
| **Reviews** | Create for own bookings | — | — | Read all |

---

## 10. API Endpoints

### Auth

| Method | Path | Role | Description |
|--------|------|------|-------------|
| POST | `/api/auth/register` | Public | Register new user |
| POST | `/api/auth/login` | Public | Login, returns JWT |

### Profile

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | `/api/profiles/me` | Authenticated | Get own profile |
| PUT | `/api/profiles/me` | Authenticated | Update own profile |

### Cars

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | `/api/cars` | CAR_OWNER | List own cars |
| POST | `/api/cars` | CAR_OWNER | Add car |
| PUT | `/api/cars/{id}` | CAR_OWNER | Update own car |
| DELETE | `/api/cars/{id}` | CAR_OWNER | Delete own car |

### Reference Data

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | `/api/car-makes` | Public | List all makes |
| GET | `/api/car-makes/{id}/models` | Public | List models for a make |
| GET | `/api/service-categories` | Public | List all categories |

### Garages

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | `/api/garages` | Public | List approved garages |
| GET | `/api/garages/me` | GARAGE | Get own garage |
| POST | `/api/garages` | GARAGE | Create garage profile |
| PUT | `/api/garages/me` | GARAGE | Update own garage |

### Job Requests

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | `/api/job-requests` | CAR_OWNER | List own requests |
| GET | `/api/job-requests/open` | GARAGE | List open requests |
| POST | `/api/job-requests` | CAR_OWNER | Create request |
| PUT | `/api/job-requests/{id}` | CAR_OWNER | Update own request |
| DELETE | `/api/job-requests/{id}` | CAR_OWNER | Delete own request |

### Quotes

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | `/api/job-requests/{id}/quotes` | CAR_OWNER | List quotes for own request |
| GET | `/api/quotes/mine` | GARAGE | List own quotes |
| POST | `/api/job-requests/{id}/quotes` | GARAGE | Submit quote |

### Bookings

| Method | Path | Role | Description |
|--------|------|------|-------------|
| POST | `/api/quotes/{id}/accept` | CAR_OWNER | Accept quote → create booking |
| GET | `/api/bookings` | Authenticated | List own bookings |

### Reviews

| Method | Path | Role | Description |
|--------|------|------|-------------|
| POST | `/api/bookings/{id}/reviews` | CAR_OWNER | Create review |
| GET | `/api/garages/{id}/reviews` | Public | List reviews for a garage |

---

## 11. Frontend Migration

The frontend must be changed to call the Java API instead of Supabase. Key changes:

| Current (Supabase) | Target (Java API) |
|---------------------|-------------------|
| `supabase.auth.signUp()` | `POST /api/auth/register` |
| `supabase.auth.signInWithPassword()` | `POST /api/auth/login` |
| `supabase.auth.signOut()` | Clear local JWT |
| `supabase.from('cars').select()` | `GET /api/cars` with Bearer token |
| `supabase.from('job_requests').insert()` | `POST /api/job-requests` with Bearer token |
| `supabase.from('quotes').select()` | `GET /api/job-requests/{id}/quotes` |
| RLS (automatic row filtering) | Backend enforces ownership in service layer |

### Migration approach

1. Create an `apiClient` utility (e.g. Axios or fetch wrapper) that attaches the JWT to every request.
2. Replace `useAuth.tsx` to use Java auth endpoints and store JWT in localStorage.
3. Replace each Supabase query with the corresponding REST call, page by page.
4. Remove `@supabase/supabase-js` dependency and `integrations/supabase/` folder.

---

## 12. Effort Estimate

| Category | Task | Hours |
|----------|------|-------|
| **Auth** | Registration endpoint, validation, BCrypt | 5h |
| | Login endpoint, JWT generation | 4h |
| | JWT validation filter, SecurityFilterChain | 6h |
| | Role-based access annotations | 4h |
| **Schema & Entities** | New Flyway migrations (target schema + seed data) | 4h |
| | New entities: Profile, Garage, JobRequest, Quote, Booking, Review, CarMake, CarModel, ServiceCategory | 8h |
| | Refactor existing: AppUser, Car; remove legacy entities | 4h |
| **Vehicles** | Car, CarMake, CarModel repos + service + endpoints | 8h |
| **Categories** | ServiceCategory repo + endpoint | 2h |
| **Garages** | Garage repo + service + CRUD + approval | 6h |
| **Job Requests** | JobRequest repo + service + endpoints + role checks | 11h |
| **Quotes** | Quote repo + service + endpoints + unique constraint | 9h |
| **Bookings** | Booking repo + service + accept flow (transactional) | 10h |
| **Reviews** | Review repo + service + endpoint | 3h |
| **Profiles** | Profile repo + service + endpoint | 3h |
| **Config** | application.properties, CORS, global exception handler, env | 4h |
| **Tests** | Auth + core flows (happy + failure paths) | 8h |
| **Frontend migration** | Replace Supabase calls with REST client, all pages | 12h |
| **Cleanup** | Remove unused deps from pom.xml, delete legacy code | 3h |

| | |
|---|---|
| **Total** | **~114 hours** |

---

## 13. Implementation Order

Build in this order to have a testable backend at each stage:

```
Phase 1: Foundation                    Phase 2: Core Loop
┌─────────────────────────┐            ┌──────────────────────────┐
│ 1. Schema + Flyway      │            │ 5. Job Requests          │
│ 2. Auth (register/login)│            │ 6. Quotes                │
│ 3. Spring Security      │            │ 7. Bookings (accept flow)│
│ 4. Profiles + Cars +    │            │ 8. Reviews               │
│    Garages + Categories  │            │                          │
└─────────────────────────┘            └──────────────────────────┘
        ~45h                                    ~33h

Phase 3: Integration                   Phase 4: Polish
┌─────────────────────────┐            ┌──────────────────────────┐
│ 9. Frontend migration   │            │ 11. Tests                │
│ 10. CORS + config       │            │ 12. Cleanup              │
│                         │            │                          │
└─────────────────────────┘            └──────────────────────────┘
        ~16h                                    ~11h
```

Each phase results in a working, testable increment:
- After **Phase 1**: Can register, login, manage cars and garage profiles
- After **Phase 2**: Full marketplace loop works via API (Postman/tests)
- After **Phase 3**: Frontend works with Java backend; Supabase removed
- After **Phase 4**: Production-ready with tests and clean codebase

---

## Appendix A: Current Java Structure

```
auto-ease-api/
├── domain/
│   ├── appUser/       (AppUser, AppUserType)        ← REFACTOR
│   ├── car/           (Car)                         ← REFACTOR
│   ├── document/      (Document)                    ← KEEP (future use)
│   ├── rating/        (Rating)                      ← REFACTOR → Review
│   ├── requestType/   (RequestType)                 ← REFACTOR → ServiceCategory
│   ├── service/       (Service)                     ← REFACTOR → Garage
│   ├── serviceEntry/  (ServiceEntry)                ← REPLACE → JobRequest + Quote
│   ├── serviceSpecialization/                       ← REMOVE
│   ├── specialization/                              ← REMOVE
│   └── BaseEntity.java                              ← KEEP
├── controller/
│   ├── appUser/       (AppUserRestController)       ← REPLACE
│   └── service/       (ServiceRestController)       ← REPLACE
├── service/
│   ├── appUser/       (AppUserService)              ← REFACTOR
│   ├── car/           (CarService)                  ← REFACTOR
│   ├── document/      (DocumentService)             ← KEEP (future use)
│   ├── rating/        (RatingService)               ← REFACTOR → ReviewService
│   ├── requestType/   (RequestTypeService)          ← REFACTOR → ServiceCategoryService
│   ├── service/       (ServiceService)              ← REFACTOR → GarageService
│   ├── serviceEntry/  (ServiceEntryService + DTOs)  ← REPLACE
│   ├── serviceSpecialization/                       ← REMOVE
│   └── specialization/                              ← REMOVE
├── repository/         (one per entity)              ← REFACTOR to match new entities
├── exception/          (MessageException)            ← KEEP
└── resources/db/migration/
    ├── V1__createBasicDomainNeeds.sql               ← REPLACE
    └── V1_1__createServiceEntity.sql                ← REMOVE
```

---

## Appendix B: Known Bugs in Current Java Code

These should be addressed during the refactor phase:

| Location | Issue |
|----------|-------|
| `AppUserRestController.updateCar()` | Returns `Car` but has no `return` statement; missing `@RequestBody` and proper DI |
| `V1_1__createServiceEntity.sql` | `SERVICE_SPECIALIZATION` primary key uses `student_id`, `course_id` (copy-paste error) |
| `ServiceRestController.processServiceEntry()` | `Object payload` parameter cannot be safely cast to typed DTOs |
| `Service.phone` | Typed as `Integer` — should be `String` |
| No `application.properties` | Config file is missing entirely — no DB connection, no server port |
