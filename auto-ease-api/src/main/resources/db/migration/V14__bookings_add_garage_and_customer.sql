-- Denormalise garage_id and customer_id onto bookings so a garage's calendar
-- (its bookings) can be queried directly without joining through quotes.

ALTER TABLE bookings
    ADD COLUMN garage_id   UUID,
    ADD COLUMN customer_id VARCHAR(50);

UPDATE bookings b
SET garage_id   = q.garage_id,
    customer_id = jr.user_id
FROM quotes q
         INNER JOIN job_requests jr ON jr.id = q.job_request_id
WHERE b.quote_id = q.id;

ALTER TABLE bookings
    ALTER COLUMN garage_id   SET NOT NULL,
    ALTER COLUMN customer_id SET NOT NULL;

ALTER TABLE bookings
    ADD CONSTRAINT fk_bookings_garage   FOREIGN KEY (garage_id)   REFERENCES garages (id),
    ADD CONSTRAINT fk_bookings_customer FOREIGN KEY (customer_id) REFERENCES APP_USER (uuid);

CREATE INDEX idx_bookings_garage_id   ON bookings (garage_id);
CREATE INDEX idx_bookings_customer_id ON bookings (customer_id);
