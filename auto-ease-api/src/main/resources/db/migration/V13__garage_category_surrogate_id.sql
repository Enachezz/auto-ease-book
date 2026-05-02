-- Simple surrogate PK for garage_category; keeps uniqueness of (garage_id, category_id).

ALTER TABLE garage_category
    ADD COLUMN id UUID NOT NULL DEFAULT gen_random_uuid();

ALTER TABLE garage_category DROP CONSTRAINT garage_category_pkey;

ALTER TABLE garage_category
    ADD CONSTRAINT garage_category_pkey PRIMARY KEY (id);

ALTER TABLE garage_category
    ADD CONSTRAINT uk_garage_category_garage_category UNIQUE (garage_id, category_id);
