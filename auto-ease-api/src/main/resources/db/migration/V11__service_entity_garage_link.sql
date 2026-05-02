-- SERVICE profiles belong to a garage; replaces direct APP_USER ownership.

ALTER TABLE SERVICE
    ADD COLUMN garage_id UUID REFERENCES garages (id) ON DELETE SET NULL;

UPDATE SERVICE s
SET garage_id = g.id
FROM garages g
WHERE s.owner_user_id IS NOT NULL
  AND s.owner_user_id = g.user_id;

ALTER TABLE SERVICE
    DROP COLUMN owner_user_id;

CREATE UNIQUE INDEX uq_service_garage_id
    ON SERVICE (garage_id)
    WHERE garage_id IS NOT NULL;
