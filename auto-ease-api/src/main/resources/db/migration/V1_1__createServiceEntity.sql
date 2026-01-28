CREATE TABLE SERVICE(
    uuid            VARCHAR(50) PRIMARY KEY,
    created_date    TIMESTAMP NOT NULL,
    modified_date   TIMESTAMP NOT NULL,
    name            VARCHAR(50),
    description     VARCHAR(5000),
    phone           INTEGER,
    email           VARCHAR(200),
    address         VARCHAR(500)
)


CREATE TABLE SPECIALIZATION(
   name            VARCHAR(50) PRIMARY KEY
)

CREATE TABLE SERVICE_SPECIALIZATION(
   service_uuid    VARCHAR(50),
   name            VARCHAR(50),
   PRIMARY KEY (student_id, course_id)
)