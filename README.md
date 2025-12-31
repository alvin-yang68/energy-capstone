# Mini Energy Retailer Platform

A backend service built to model core electricity retail workflows: metering, tariff management, and monthly billing.

I built this project to bridge my background in Rust and TypeScript (NestJS) into the Kotlin & Spring Boot ecosystem. The goal was to implement a production-grade architecture that handles domain complexity (time-of-use rates, proration) and performance (batch processing, concurrency) without relying on "Spring Magic" where simple logic would suffice.

## Tech Stack

*   Language: Kotlin 2.2 (Running on JDK 24)
*   Framework: Spring Boot 4.0 (WebMVC, Data JPA)
*   Database: PostgreSQL 16 + TimescaleDB (for time-series metering data)
*   Concurrency: Kotlin Coroutines (`kotlinx-coroutines`)
*   Documentation: OpenAPI 3 / Swagger (`springdoc-openapi`)
*   Observability: Spring Boot Actuator
*   Testing: JUnit 5, Mockk, Testcontainers (Integration), AssertJ
*   Tools: Gradle (Kotlin DSL), Docker Compose, Python (for load simulation)

## Domain Scope

The system models a simplified energy retailer:
1.  Metering: Ingesting 30-minute interval data into TimescaleDB hypertables.
2.  Tariffs: A flexible pricing engine supporting Flat and Time-of-Use (TOU) rates using a Strategy pattern.
3.  Billing: A scheduled monthly job that generates invoices, handling logic like:
    *   Proration (switching plans mid-month).
    *   Multi-rate tariffs (e.g. Usage + Network Charges).
    *   Idempotency (preventing double-billing).

## Key Architectural Decisions

### 1. Hybrid Concurrency Model
Coming from Rust/Tokio, I wanted to handle I/O efficiently. Since Spring Data JPA is blocking (JDBC), I opted for a hybrid approach in the `BillingJob`:
*   Fixed Thread Pool: A custom `CoroutineDispatcher` backed by a fixed thread pool (size 4) to match the database connection pool limits.
*   Structured Concurrency: Used `supervisorScope` and `launch` to parallelize invoice generation for customer batches.
*   Result: This prevents database connection starvation while maintaining high throughput for the batch process.

### 2. Polymorphic Pricing Configuration
Instead of creating separate tables for every rate type (`flat_rate`, `tou_rate`), I used a single `tariff_rate` table with a `configuration` JSONB column.
*   Mapped via Jackson polymorphism (`@JsonTypeInfo`) to Kotlin Sealed Interface (`RateConfiguration`).
*   This provides type safety in the application layer while allowing schema flexibility for future rate types (e.g. Step/Block tiers) without DB migrations.

### 3. High-Performance Data Ingest
To handle bulk meter reading uploads efficiently:
*   Implemented `Persistable<ID>` on the `MeterReading` entity to bypass Spring Data's default "select-before-insert" check for manual IDs.
*   Enabled JDBC batching (`batch_size: 500`) and `reWriteBatchedInserts=true` in the Postgres driver.
*   Benchmark: Ingests ~30 days of 30-minute data (1,440 rows) in ~25ms.

### 4. TimescaleDB Integration
Meter readings are stored in a Hypertable partitioned by time. I used a Composite Primary Key (`site_id`, `read_at`) to ensure uniqueness and fast range queries during billing calculation.

## Project Structure

I followed a "Package-by-Feature" structure to keep related domain logic together, rather than grouping files by functional layer.

```text
src/main/kotlin/com/alvinyang/energycapstone
├── common            # Shared kernels (Currency, Global Exceptions, AOP)
├── features
│   ├── billing       # Invoice entities, Service logic, Scheduled Job
│   ├── customer      # Customer & Site management
│   ├── metering      # Meter reading ingest & TimescaleDB repos
│   └── pricing       # Tariff definitions & The Calculation Engine
└── ...
```

## Running Locally

Prerequisites: Docker and JDK 21+.

1.  Start Infrastructure:
    ```bash
    docker compose up -d
    ```
    This spins up a Postgres container with the TimescaleDB extension pre-loaded.

2.  Run the Application:
    ```bash
    ./gradlew bootRun
    ```

3.  Seed Data (Simulation):
    I wrote a Python script to seed customers, sites, tariffs, and 30 days of meter data.
    ```bash
    pip install -r scripts/requirements.txt
    python3 scripts/seed_data.py
    ```

4.  Trigger Billing Job:
    The job runs automatically on schedule (cron), or you can modify the cron expression in `BillingJob.kt` to run it immediately.

## Performance Benchmarks

Tested on a local development machine:
*   Data Volume: 50 Customers, ~72,000 Meter Readings.
*   Billing Job Execution: ~900ms total processing time.
*   Throughput: ~18ms per customer (End-to-End calculation & persistence).

## Future Improvements

*   R2DBC: Migrating to Reactive SQL to fully leverage non-blocking I/O.
*   Distributed Locking: Prevent the scheduled job from running more than once in parallel if deployed to multiple instances.