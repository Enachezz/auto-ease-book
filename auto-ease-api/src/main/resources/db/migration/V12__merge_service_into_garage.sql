-- Merge legacy SERVICE into garages; categories link directly to garage.

ALTER TABLE garages
    ADD COLUMN email VARCHAR(255),
    ADD COLUMN dealership BOOLEAN NOT NULL DEFAULT false;

UPDATE garages g
SET email = s.email,
    dealership = COALESCE(s.dealership, false)
FROM service s
WHERE s.garage_id = g.id;

CREATE TABLE garage_category (
    garage_id   UUID NOT NULL REFERENCES garages (id) ON DELETE CASCADE,
    category_id UUID NOT NULL REFERENCES service_categories (id) ON DELETE CASCADE,
    PRIMARY KEY (garage_id, category_id)
);

INSERT INTO garage_category (garage_id, category_id)
SELECT s.garage_id, sec.category_id
FROM service_entity_category sec
         INNER JOIN service s ON s.uuid = sec.service_uuid
WHERE s.garage_id IS NOT NULL;

DROP TABLE IF EXISTS service_entity_category;

ALTER TABLE service_entry
    ADD COLUMN garage_id UUID REFERENCES garages (id);

UPDATE service_entry se
SET garage_id = s.garage_id
FROM service s
WHERE se.service_uuid IS NOT NULL
  AND se.service_uuid = s.uuid
  AND s.garage_id IS NOT NULL;

ALTER TABLE service_entry
    DROP COLUMN service_uuid;

DROP TABLE IF EXISTS service_specialization;
DROP TABLE IF EXISTS specialization;
DROP TABLE IF EXISTS service;
