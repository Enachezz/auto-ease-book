-- V2: Create target schema tables for the Lovable-to-Java migration.
-- Existing V1/V1_1 tables are left untouched.

-- Add password column to existing APP_USER table
ALTER TABLE APP_USER ADD COLUMN password VARCHAR(255);

-- Fix SERVICE_ENTRY.cost column type (V1 created it as BIGINT, entity expects NUMERIC)
ALTER TABLE SERVICE_ENTRY ALTER COLUMN cost TYPE NUMERIC(38,2);

-- Profiles (extends APP_USER with display info and role)
CREATE TABLE profiles (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    VARCHAR(50) NOT NULL,
    user_type  VARCHAR(20) NOT NULL DEFAULT 'car_owner',
    full_name  VARCHAR(200),
    email      VARCHAR(200),
    phone      VARCHAR(20),
    avatar_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_profiles_user FOREIGN KEY (user_id) REFERENCES APP_USER(uuid),
    CONSTRAINT chk_profiles_user_type CHECK (user_type IN ('car_owner', 'garage', 'admin'))
);

CREATE UNIQUE INDEX idx_profiles_user_id ON profiles(user_id);

-- Service categories
CREATE TABLE service_categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    icon        VARCHAR(50),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Car makes
CREATE TABLE car_makes (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Car models
CREATE TABLE car_models (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    make_id    UUID NOT NULL,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_car_models_make FOREIGN KEY (make_id) REFERENCES car_makes(id),
    CONSTRAINT uq_car_models_make_name UNIQUE (make_id, name)
);

-- Garages
CREATE TABLE garages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         VARCHAR(50) NOT NULL,
    business_name   VARCHAR(200) NOT NULL,
    address         VARCHAR(500),
    city            VARCHAR(100),
    state           VARCHAR(100),
    postal_code     VARCHAR(20),
    phone           VARCHAR(20),
    description     TEXT,
    services        TEXT[],
    opening_hours   JSONB,
    is_approved     BOOLEAN NOT NULL DEFAULT false,
    average_rating  NUMERIC(3,2) NOT NULL DEFAULT 0,
    total_reviews   INTEGER NOT NULL DEFAULT 0,
    latitude        DOUBLE PRECISION,
    longitude       DOUBLE PRECISION,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_garages_user FOREIGN KEY (user_id) REFERENCES APP_USER(uuid)
);

CREATE UNIQUE INDEX idx_garages_user_id ON garages(user_id);

-- Job requests
CREATE TABLE job_requests (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         VARCHAR(50) NOT NULL,
    car_id          INTEGER NOT NULL,
    category_id     UUID,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    urgency         VARCHAR(20) NOT NULL DEFAULT 'normal',
    preferred_date  DATE,
    budget_min      NUMERIC(10,2),
    budget_max      NUMERIC(10,2),
    status          VARCHAR(20) NOT NULL DEFAULT 'open',
    location_address VARCHAR(500),
    location_city   VARCHAR(100),
    location_state  VARCHAR(100),
    location_lat    DOUBLE PRECISION,
    location_lng    DOUBLE PRECISION,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_job_requests_user     FOREIGN KEY (user_id)     REFERENCES APP_USER(uuid),
    CONSTRAINT fk_job_requests_car      FOREIGN KEY (car_id)      REFERENCES CAR(id),
    CONSTRAINT fk_job_requests_category FOREIGN KEY (category_id) REFERENCES service_categories(id),
    CONSTRAINT chk_job_requests_urgency CHECK (urgency IN ('low', 'normal', 'high', 'emergency')),
    CONSTRAINT chk_job_requests_status  CHECK (status IN ('open', 'quoted', 'in_progress', 'completed', 'cancelled'))
);

-- Quotes
CREATE TABLE quotes (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_request_id     UUID NOT NULL,
    garage_id          UUID NOT NULL,
    price              NUMERIC(10,2) NOT NULL,
    estimated_duration VARCHAR(100),
    description        TEXT,
    warranty_info      TEXT,
    status             VARCHAR(20) NOT NULL DEFAULT 'pending',
    expires_at         TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_quotes_job_request FOREIGN KEY (job_request_id) REFERENCES job_requests(id),
    CONSTRAINT fk_quotes_garage      FOREIGN KEY (garage_id)      REFERENCES garages(id),
    CONSTRAINT uq_quotes_request_garage UNIQUE (job_request_id, garage_id),
    CONSTRAINT chk_quotes_status CHECK (status IN ('pending', 'accepted', 'rejected', 'expired'))
);

-- Bookings
CREATE TABLE bookings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quote_id        UUID NOT NULL UNIQUE,
    scheduled_date  DATE,
    scheduled_time  TIME,
    status          VARCHAR(20) NOT NULL DEFAULT 'scheduled',
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_bookings_quote FOREIGN KEY (quote_id) REFERENCES quotes(id),
    CONSTRAINT chk_bookings_status CHECK (status IN ('scheduled', 'in_progress', 'completed', 'cancelled'))
);

-- Reviews
CREATE TABLE reviews (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id  UUID NOT NULL UNIQUE,
    garage_id   UUID NOT NULL,
    user_id     VARCHAR(50) NOT NULL,
    rating      INTEGER NOT NULL,
    comment     TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_reviews_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
    CONSTRAINT fk_reviews_garage  FOREIGN KEY (garage_id)  REFERENCES garages(id),
    CONSTRAINT fk_reviews_user    FOREIGN KEY (user_id)    REFERENCES APP_USER(uuid),
    CONSTRAINT chk_reviews_rating CHECK (rating >= 1 AND rating <= 5)
);
