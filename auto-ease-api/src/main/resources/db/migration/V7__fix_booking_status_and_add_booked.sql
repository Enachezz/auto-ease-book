-- Fix bookings.status CHECK: uppercase + CONFIRMED instead of scheduled
ALTER TABLE bookings DROP CONSTRAINT chk_bookings_status;
ALTER TABLE bookings ALTER COLUMN status SET DEFAULT 'CONFIRMED';
ALTER TABLE bookings ADD CONSTRAINT chk_bookings_status
    CHECK (status IN ('CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'));

-- Add BOOKED to job_requests.status CHECK
ALTER TABLE job_requests DROP CONSTRAINT chk_job_requests_status;
ALTER TABLE job_requests ADD CONSTRAINT chk_job_requests_status
    CHECK (status IN ('OPEN', 'QUOTED', 'BOOKED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'));
