ALTER TABLE SERVICE
    ADD COLUMN owner_user_id VARCHAR(50) REFERENCES APP_USER (uuid);

CREATE UNIQUE INDEX uq_service_owner_user_id
    ON SERVICE (owner_user_id)
    WHERE owner_user_id IS NOT NULL;

CREATE TABLE service_entity_category (
    service_uuid VARCHAR(50) NOT NULL REFERENCES SERVICE (uuid) ON DELETE CASCADE,
    category_id    UUID        NOT NULL REFERENCES service_categories (id) ON DELETE CASCADE,
    PRIMARY KEY (service_uuid, category_id)
);
