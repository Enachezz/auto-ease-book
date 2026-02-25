# auto-ease-api Documentation

## Design Document

| Document | Description |
|----------|-------------|
| [LOVABLE-TO-JAVA-MIGRATION.md](./LOVABLE-TO-JAVA-MIGRATION.md) | Main design doc — architecture, data model, auth, endpoints, inventory (keep/refactor/build), effort estimate |

## Implementation Steps

Each step builds on the previous. Each has its own scope, tasks, acceptance tests, and definition of done.

| Step | Document | Depends On | ~Hours |
|------|----------|------------|--------|
| 01 | [Project Setup & Schema](./STEP-01-PROJECT-SETUP-AND-SCHEMA.md) | — | 8h |
| 02 | [Authentication & Security](./STEP-02-AUTHENTICATION-AND-SECURITY.md) | Step 01 | 19h |
| 03 | [Profiles & Reference Data](./STEP-03-PROFILES-AND-REFERENCE-DATA.md) | Step 02 | 8h |
| 04 | [Vehicle Management](./STEP-04-VEHICLE-MANAGEMENT.md) | Step 03 | 8h |
| 05 | [Garage Profiles](./STEP-05-GARAGE-PROFILES.md) | Step 02 | 6h |
| 06 | [Job Requests & Quotes](./STEP-06-JOB-REQUESTS-AND-QUOTES.md) | Steps 04, 05 | 20h |
| 07 | [Bookings & Reviews](./STEP-07-BOOKINGS-AND-REVIEWS.md) | Step 06 | 13h |
| 08 | [Frontend Migration & E2E](./STEP-08-FRONTEND-MIGRATION-AND-E2E.md) | Steps 01–07 | 15h |

**Total: ~97h**

## Dependency Graph

```
Step 01: Project Setup & Schema
    │
    ▼
Step 02: Authentication & Security
    │
    ├──────────────────┐
    ▼                  ▼
Step 03: Profiles    Step 05: Garages
& Reference Data
    │                  │
    ▼                  │
Step 04: Vehicles      │
    │                  │
    └──────┬───────────┘
           ▼
Step 06: Job Requests & Quotes
           │
           ▼
Step 07: Bookings & Reviews
           │
           ▼
Step 08: Frontend Migration & E2E
```
