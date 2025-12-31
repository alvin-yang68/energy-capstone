import requests
import random
import time
from datetime import datetime, timedelta, timezone
from faker import Faker

# CONFIGURATION
API_URL = "http://localhost:8080/api"
NUM_CUSTOMERS = 50
DAYS_OF_DATA = 30
READINGS_PER_DAY = 48  # 30 min intervals
TOTAL_READINGS_PER_SITE = DAYS_OF_DATA * READINGS_PER_DAY

fake = Faker()
session = requests.Session()    # Use session for connection pooling

def log(msg):
    print(f"[{datetime.now().strftime('%H:%M:%S')}] {msg}")


def create_tariff_plan():
    """Creates a default Tariff Plan if it doesn't exist (conceptually)"""
    # For simplicity, we just create a new one every time script runs
    payload = {
        "code": f"PLAN-{random.randint(1000, 9999)}",
        "name": "Standard Flat Rate 2025",
        "country": "SG",
        "billingPeriod": "MONTHLY",
        "rates": [
            {
                "rateType": "FLAT",
                "description": "Base Energy Charge",
                "configuration": {
                    "type": "FLAT",
                    "pricePerKwh": 0.2543
                }
            }
        ]
    }
    resp = session.post(f"{API_URL}/tariffs", json=payload)
    resp.raise_for_status()
    plan_id = resp.json()["id"]
    log(f"Created Tariff Plan: {plan_id}")
    return plan_id

def generate_readings(site_id, start_date):
    """Generates a list of reading dicts"""
    readings = []
    current_time = start_date

    for _ in range(TOTAL_READINGS_PER_SITE):
        # Random usage between 0.0 and 2.0 kWh per 30 mins
        kwh = round(random.uniform(0.1, 2.0), 4)

        readings.append({
            "siteId": site_id,
            "readAt": current_time.isoformat(),
            "kwh": kwh
        })

        # Advance 30 mins
        current_time += timedelta(minutes=30)

    return readings

def main():
    log("Starting Data Seeder...")

    # 1. Setup Infra
    try:
        plan_id = create_tariff_plan()
    except requests.exceptions.ConnectionError:
        log(f"ERROR: Could not connect to {API_URL}. Is Spring Boot running?")
        return

    # Start seeding from 40 days ago
    start_date = datetime.now(timezone.utc) - timedelta(days=40)

    for i in range(NUM_CUSTOMERS):
        # 2. Create Customer
        cust_payload = {
            "name": fake.name(),
            "email": fake.email(),
            "country": "SG"
        }
        resp = session.post(f"{API_URL}/customers", json=cust_payload)
        resp.raise_for_status()
        customer_id = resp.json()["id"]

        # 3. Create Site
        site_payload = {
            "customerId": customer_id,
            "identifier": f"NMI-{fake.uuid4()[:8].upper()}",
            "country": "SG",
            "region": "SG-NORTH",
            "timezone": "Asia/Singapore",
            "address": fake.address()
        }
        resp = session.post(f"{API_URL}/sites", json=site_payload)
        resp.raise_for_status()
        site_id = resp.json()["id"]

        # 4. Assign Tariff
        assign_payload = {
            "siteId": site_id,
            "tariffPlanId": plan_id,
            "effectiveFrom": "2025-01-01T00:00:00Z"
        }
        session.post(f"{API_URL}/tariffs/assignments", json=assign_payload).raise_for_status()

        # 5. Bulk Ingest Readings
        readings = generate_readings(site_id, start_date)

        # Send in one big batch (since we tuned batch_size to 500, this handles 1440 fine)
        # or chunk it if payload is too massive.
        chunk_size = 500
        for j in range(0, len(readings), chunk_size):
            chunk = readings[j:j+chunk_size]
            resp = session.post(f"{API_URL}/readings/bulk", json=chunk)
            if resp.status_code != 202:
                log(f"Error ingesting chunk: {resp.text}")

        if (i + 1) % 10 == 0:
            log(f"Seeded {i + 1}/{NUM_CUSTOMERS} customers...")

    log("Seeding Complete!")

if __name__ == "__main__":
    main()