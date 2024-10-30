CREATE TYPE connection_type AS ENUM (
    'NETCONF',
    'GNMI',
    'CLI',
    'SNMP'
);

CREATE TYPE status AS ENUM (
    'ACTIVE',
    'DELETED'
);

CREATE TABLE blueprint
(
    id
        SERIAL
        PRIMARY KEY,
    name
        VARCHAR(255)
        NOT NULL
        UNIQUE,
    connection_type
        connection_type
        NOT NULL,
    vendor_pattern
        VARCHAR(255),
    model_pattern
        VARCHAR(255),
    version_pattern
        VARCHAR(255),
    template
        JSONB
        NOT NULL,
    status
        status
        NOT NULL
        DEFAULT 'ACTIVE',
    created_at
        TIMESTAMP
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP,
    updated_at
        TIMESTAMP
        NOT NULL
        DEFAULT CURRENT_TIMESTAMP
);
