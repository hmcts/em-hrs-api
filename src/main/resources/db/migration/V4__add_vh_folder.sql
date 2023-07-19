
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

INSERT INTO folder (id, name, hearing_source, created_by)
VALUES (uuid_generate_v4(),'VH', 'VH', 'hrs-api');



