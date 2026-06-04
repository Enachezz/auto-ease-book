CREATE TABLE garage_make (
    id       UUID NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    garage_id UUID NOT NULL REFERENCES garages (id) ON DELETE CASCADE,
    make_id   UUID NOT NULL REFERENCES car_makes (id) ON DELETE CASCADE,
    CONSTRAINT uk_garage_make_garage_make UNIQUE (garage_id, make_id)
);

CREATE INDEX idx_garage_make_make_id ON garage_make (make_id);
