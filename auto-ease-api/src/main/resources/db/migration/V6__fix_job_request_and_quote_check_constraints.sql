-- Fix CHECK constraints and defaults to use uppercase values matching JPA EnumType.STRING

-- job_requests.urgency
ALTER TABLE job_requests DROP CONSTRAINT chk_job_requests_urgency;
ALTER TABLE job_requests ALTER COLUMN urgency SET DEFAULT 'NORMAL';
ALTER TABLE job_requests ADD CONSTRAINT chk_job_requests_urgency
    CHECK (urgency IN ('LOW', 'NORMAL', 'HIGH', 'EMERGENCY'));

-- job_requests.status
ALTER TABLE job_requests DROP CONSTRAINT chk_job_requests_status;
ALTER TABLE job_requests ALTER COLUMN status SET DEFAULT 'OPEN';
ALTER TABLE job_requests ADD CONSTRAINT chk_job_requests_status
    CHECK (status IN ('OPEN', 'QUOTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'));

-- quotes.status
ALTER TABLE quotes DROP CONSTRAINT chk_quotes_status;
ALTER TABLE quotes ALTER COLUMN status SET DEFAULT 'PENDING';
ALTER TABLE quotes ADD CONSTRAINT chk_quotes_status
    CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'EXPIRED'));
