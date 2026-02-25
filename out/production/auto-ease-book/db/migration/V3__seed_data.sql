-- V3: Seed reference data — service categories, car makes, car models.

-- Service categories (10)
INSERT INTO service_categories (name, description, icon) VALUES
    ('Oil Change',    'Regular oil and filter change service',          'oil-can'),
    ('Brake Service', 'Brake pad replacement, rotor resurfacing, fluid flush', 'disc'),
    ('Engine Repair', 'Engine diagnostics, repair, and rebuild',        'engine'),
    ('Transmission',  'Transmission repair, fluid change, rebuild',     'cog'),
    ('Electrical',    'Battery, alternator, wiring, and electronics',   'zap'),
    ('AC / Heating',  'Climate control diagnostics and repair',         'thermometer'),
    ('Tire Service',  'Tire rotation, balancing, alignment, replacement','circle'),
    ('Body Work',     'Dent repair, painting, collision repair',        'paintbrush'),
    ('Inspection',    'Pre-purchase and safety inspections',            'clipboard-check'),
    ('Towing',        'Emergency towing and roadside assistance',       'truck');

-- Car makes (15)
INSERT INTO car_makes (name) VALUES
    ('Toyota'),
    ('Honda'),
    ('Ford'),
    ('Chevrolet'),
    ('Nissan'),
    ('BMW'),
    ('Mercedes-Benz'),
    ('Audi'),
    ('Volkswagen'),
    ('Hyundai'),
    ('Kia'),
    ('Mazda'),
    ('Subaru'),
    ('Lexus'),
    ('Acura');

-- Car models — 5 each for Toyota, Honda, Ford (15 total)
INSERT INTO car_models (make_id, name)
SELECT m.id, v.name
FROM car_makes m
CROSS JOIN (VALUES ('Camry'),('Corolla'),('RAV4'),('Highlander'),('Tacoma')) AS v(name)
WHERE m.name = 'Toyota';

INSERT INTO car_models (make_id, name)
SELECT m.id, v.name
FROM car_makes m
CROSS JOIN (VALUES ('Civic'),('Accord'),('CR-V'),('Pilot'),('HR-V')) AS v(name)
WHERE m.name = 'Honda';

INSERT INTO car_models (make_id, name)
SELECT m.id, v.name
FROM car_makes m
CROSS JOIN (VALUES ('F-150'),('Mustang'),('Explorer'),('Escape'),('Bronco')) AS v(name)
WHERE m.name = 'Ford';
