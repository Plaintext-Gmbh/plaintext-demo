-- plaintext-demo: products of the demo table. PostgreSQL syntax like every migration of the
-- platform; on the demo's H2 database it runs unchanged.
CREATE TABLE IF NOT EXISTS demo_product (
    id                 BIGSERIAL PRIMARY KEY,
    created_by         VARCHAR(255),
    created_date       TIMESTAMP,
    deleted            BOOLEAN DEFAULT FALSE,
    last_modified_by   VARCHAR(255),
    last_modified_date TIMESTAMP,
    mandat             VARCHAR(255),
    tags               VARCHAR(5000),
    sku                VARCHAR(40) NOT NULL,
    name               VARCHAR(200) NOT NULL,
    category           VARCHAR(80),
    supplier           VARCHAR(120),
    warehouse          VARCHAR(80),
    quantity           INTEGER,
    unit               VARCHAR(20),
    price              NUMERIC(12, 2),
    currency           VARCHAR(3),
    vat_rate           NUMERIC(5, 2),
    weight_kg          NUMERIC(10, 3),
    color              VARCHAR(40),
    status             VARCHAR(40),
    active             BOOLEAN DEFAULT TRUE,
    release_date       DATE,
    last_delivery      DATE,
    owner              VARCHAR(120),
    rating             INTEGER,
    country            VARCHAR(80),
    ean                VARCHAR(20),
    notes              VARCHAR(1000)
);
CREATE INDEX IF NOT EXISTS idx_demo_product_mandat ON demo_product (mandat);
