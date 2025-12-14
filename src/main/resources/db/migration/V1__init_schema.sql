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

-- Index for "Get me all readings for this site" (Most common query)
CREATE INDEX ix_meter_reading_site_time ON meter_reading (site_id, read_at DESC);