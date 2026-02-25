CREATE TABLE SERVICE_ENTRY (
    id              SERIAL PRIMARY KEY,
    created_date    TIMESTAMP NOT NULL,
    modified_date   TIMESTAMP NOT NULL,
    client_uuid     VARCHAR(30),
    service_uuid    VARCHAR(30),
    entry_type      VARCHAR(100),
    car_make        VARCHAR(100),
    car_model       VARCHAR(100),
    car_year        INT,
    car_vin         VARCHAR(100),
    description     TEXT,
    service_date    DATE,
    location        VARCHAR(500),
    priority        INT,
    cost            BIGINT,
    note            TEXT
);

CREATE TABLE RATING(
    id                SERIAL PRIMARY KEY,
    created_date      TIMESTAMP NOT NULL,
    modified_date     TIMESTAMP NOT NULL,
    rating            INT,
    service_entry_id  INT,

    CONSTRAINT fk_service_entry_id_to_service_entry
        FOREIGN KEY (service_entry_id)
        REFERENCES service_entry(id)
);

CREATE TABLE REQUEST_TYPE(
    type         VARCHAR(100) PRIMARY KEY,
    description  VARCHAR(100)
);

CREATE TABLE CAR(
    id               SERIAL PRIMARY KEY,
    created_date     TIMESTAMP NOT NULL,
    modified_date    TIMESTAMP NOT NULL,
    make             VARCHAR(100),
    model            VARCHAR(100),
    made             DATE,
    color            VARCHAR(100),
    license_plate    VARCHAR(15),
    current_mileage  INT,
    vin              VARCHAR(100)
);

CREATE TABLE DOCUMENT(
    id               SERIAL PRIMARY KEY,
    created_date     TIMESTAMP NOT NULL,
    modified_date    TIMESTAMP NOT NULL,
    type             VARCHAR(100) NOT NULL,
    name             VARCHAR(100),
    s3_path          VARCHAR(500) NOT NULL,
    expiration_date  DATE,
    note             VARCHAR(100)
);

CREATE TABLE APP_USER (
    uuid           VARCHAR(50) PRIMARY KEY,
    created_date   TIMESTAMP NOT NULL,
    modified_date  TIMESTAMP NOT NULL,
    email          VARCHAR(200),
    phone          VARCHAR(12),
    type           VARCHAR(50)
);