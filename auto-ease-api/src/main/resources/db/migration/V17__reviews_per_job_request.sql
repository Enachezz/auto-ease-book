-- Migrate reviews from per-booking to per-job-request

ALTER TABLE reviews ADD COLUMN job_request_id UUID;

UPDATE reviews r
SET job_request_id = q.job_request_id
FROM bookings b
JOIN quotes q ON q.id = b.quote_id
WHERE r.booking_id = b.id;

ALTER TABLE reviews ALTER COLUMN job_request_id SET NOT NULL;

ALTER TABLE reviews DROP CONSTRAINT IF EXISTS reviews_booking_id_key;
ALTER TABLE reviews DROP COLUMN booking_id;

ALTER TABLE reviews
    ADD CONSTRAINT fk_reviews_job_request FOREIGN KEY (job_request_id) REFERENCES job_requests(id);

ALTER TABLE reviews ADD CONSTRAINT uq_reviews_job_request UNIQUE (job_request_id);

CREATE INDEX idx_reviews_job_request_id ON reviews (job_request_id);
