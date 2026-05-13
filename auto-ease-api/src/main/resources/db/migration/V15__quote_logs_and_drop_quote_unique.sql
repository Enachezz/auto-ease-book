-- V15: Allow a garage to submit multiple quotes for the same job request (e.g. an "addendum"
-- quote for unexpected work discovered mid-booking) and add a quote_logs table that captures
-- comments / notifications between car owners and garage owners about a given quote.

ALTER TABLE quotes
    DROP CONSTRAINT uq_quotes_request_garage;

CREATE TABLE quote_logs
(
    id                   UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    quote_id             UUID        NOT NULL,
    author_user_id       VARCHAR(50) NOT NULL,
    message              TEXT,
    type_of_notification VARCHAR(20) NOT NULL,
    reference            VARCHAR(50) NOT NULL,
    notification_flag    BOOLEAN     NOT NULL DEFAULT TRUE,
    triggered_quote_id   UUID,
    created_date         TIMESTAMPTZ NOT NULL DEFAULT now(),
    modified_date        TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_quote_logs_quote
        FOREIGN KEY (quote_id) REFERENCES quotes (id),
    CONSTRAINT fk_quote_logs_triggered_quote
        FOREIGN KEY (triggered_quote_id) REFERENCES quotes (id),
    CONSTRAINT fk_quote_logs_author
        FOREIGN KEY (author_user_id) REFERENCES APP_USER (uuid),
    CONSTRAINT chk_quote_logs_type
        CHECK (type_of_notification IN ('GARAGE_OWNER', 'CAR_OWNER'))
);

CREATE INDEX idx_quote_logs_quote_id ON quote_logs (quote_id);
CREATE INDEX idx_quote_logs_type_reference ON quote_logs (type_of_notification, reference);
CREATE INDEX idx_quote_logs_notification_flag ON quote_logs (notification_flag);
