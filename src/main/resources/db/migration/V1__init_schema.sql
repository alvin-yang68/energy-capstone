-- Enable TimescaleDB extension
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- 1. Customer
CREATE TABLE customer (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    email TEXT NOT NULL,
    country VARCHAR(2) NOT NULL, -- 'SG', 'AU'
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,

    CONSTRAINT chk_customer_country CHECK (country IN ('SG', 'AU'))
);

-- 2. Site
CREATE TABLE site (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customer(id),
    country VARCHAR(2) NOT NULL, -- Denormalized for unique constraint safety
    identifier VARCHAR(50) NOT NULL, -- NMI / MSSL
    region VARCHAR(50) NOT NULL,
    timezone VARCHAR(50) NOT NULL,  -- `java.time.ZoneId` (e.g., 'Asia/Singapore')
    address TEXT,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,

    CONSTRAINT chk_site_country CHECK (country IN ('SG', 'AU')),
    CONSTRAINT uk_site_identifier_country UNIQUE (identifier, country)
);

-- 3. Tariff Plan
CREATE TABLE tariff_plan (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name TEXT NOT NULL,
    country VARCHAR(2) NOT NULL,
    billing_period VARCHAR(20) DEFAULT 'MONTHLY' NOT NULL,
    valid_from TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_to TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,

    CONSTRAINT chk_tariff_country CHECK (country IN ('SG', 'AU')),
    CONSTRAINT chk_tariff_billing_period CHECK (billing_period IN ('MONTHLY', 'QUARTERLY'))
);

-- 4. Tariff Rate
CREATE TABLE tariff_rate (
    id UUID PRIMARY KEY,
    tariff_plan_id UUID NOT NULL REFERENCES tariff_plan(id),
    description TEXT NOT NULL,
    rate_type VARCHAR(50) NOT NULL,
    configuration JSONB DEFAULT '{}'::jsonb NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

-- 5. Site Tariff Assignment
CREATE TABLE site_tariff_assignment (
    id UUID PRIMARY KEY,
    site_id UUID NOT NULL REFERENCES site(id),
    tariff_plan_id UUID NOT NULL REFERENCES tariff_plan(id),
    effective_from TIMESTAMP WITH TIME ZONE NOT NULL,
    effective_to TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

-- 6. Meter Reading (Hypertable)
CREATE TABLE meter_reading (
    site_id UUID NOT NULL REFERENCES site(id),
    read_at TIMESTAMP WITH TIME ZONE NOT NULL,
    kwh DECIMAL(12, 4) NOT NULL,

    PRIMARY KEY (site_id, read_at)
);

-- Convert to Hypertable
SELECT create_hypertable('meter_reading', 'read_at');

-- 7. Invoice
CREATE TABLE invoice (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customer(id),
    billing_period_start TIMESTAMP WITH TIME ZONE NOT NULL,
    billing_period_end TIMESTAMP WITH TIME ZONE NOT NULL,

    total_amount DECIMAL(19, 4) NOT NULL,   -- Use sufficient precision for currency

    currency VARCHAR(3) NOT NULL,
    CONSTRAINT chk_invoice_currency CHECK (currency IN ('SGD', 'AUD')),

    status VARCHAR(20) NOT NULL,
    CONSTRAINT chk_invoice_status CHECK (status IN ('DRAFT', 'ISSUED', 'PAID', 'VOID')),

    due_date TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL
);

CREATE INDEX ix_invoice_active_overlap
    ON invoice (customer_id, billing_period_start, billing_period_end)
    WHERE status != 'VOID';

-- 8. Invoice Line
CREATE TABLE invoice_line (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES invoice(id),
    -- We store the 'snapshot' of the cost so re-calculating rates later doesn't change history
    description TEXT NOT NULL,
    quantity DECIMAL(12, 4) NOT NULL,
    unit_price DECIMAL(12, 4), -- Can be NULL for blended TOU rates
    amount DECIMAL(19, 4) NOT NULL,
    index INT NOT NULL -- To maintain sort order on the bill
);
