# EV Charging Slot Booking System — SDLC / Technical Specification

**Document purpose:** Single source of truth for AI-assisted implementation, testing, demonstration, and DBMS report preparation.

**Project type:** University Mini Project — Java + DBMS
**Primary stack:** Java 21 + Swing + JDBC + MySQL 8.0+
**Build system:** Maven
**Database engine:** MySQL / InnoDB
**Architecture:** Swing UI → Controller → Service → DAO → JDBC → MySQL
**Booking model:** Fixed 60-minute reservations
**Payment model:** Mock payment only; no real payment gateway
**Application timezone:** `Asia/Kolkata` as a configurable application constant
**Target:** Complete, runnable, demonstrable project satisfying all stated DBMS mini-project requirements.

---

# 0. AGENT OPERATING CONTRACT

This document is the implementation authority. The project must be implemented from this specification rather than from assumptions about how an EV charging product normally works.

## 0.1 Rules for the coding agent

1. Read this entire document before modifying project files.
2. Treat the exact rules, table names, column names, statuses, workflows, package names, and acceptance tests in this document as authoritative.
3. Do not add Spring Boot, Hibernate/JPA, REST APIs, React, Node.js, cloud services, real payment APIs, maps, authentication providers, or other frameworks unless this specification explicitly requests them. It does not.
4. Do not silently redesign the database because another design is more production-oriented. The academic DBMS design is intentional.
5. Do not remove DBMS features merely because Java can perform the same operation. The project must visibly demonstrate SQL queries, a function, a procedure, and a trigger.
6. Use parameterized SQL everywhere. Do not concatenate user-controlled values into SQL.
7. Use transactions for booking and cancellation operations.
8. The database, not Java synchronization alone, is responsible for preventing double booking.
9. Use `BigDecimal` for money. Never use `double` or `float` for currency.
10. Use `java.time` in application logic and convert to JDBC timestamp/time types only at the database boundary.
11. Do not store plaintext user passwords. Passwords must be salted and hashed using Java's built-in PBKDF2 implementation.
12. Never commit local database passwords or machine-specific secrets to Git.
13. Every completed feature must compile and have a corresponding test or manual verification step.
14. Before changing an existing feature, inspect the current implementation and preserve working behavior unless the change is necessary to satisfy this specification.
15. Do not create duplicate classes that perform the same responsibility.
16. Keep the final implementation understandable enough to explain in a university viva.

## 0.2 Change-control rule

When a requirement is unclear, first check this document, then the project source files, then the acceptance tests. Only ask for clarification when the documents genuinely contradict each other. Do not invent new requirements.

## 0.3 Definition of done

The project is considered complete only when:

- the MySQL schema can be created from the supplied SQL scripts;
- seed data loads successfully;
- the Maven project compiles cleanly;
- the Swing application starts successfully;
- registration and login work;
- station/slot search and availability work;
- booking works;
- overlapping and duplicate bookings are rejected;
- booking uses a DB transaction and row locking;
- mock payment is recorded;
- history and cancellation work;
- admin station/slot management works;
- required SQL queries execute;
- a stored function, stored procedure, and trigger execute successfully;
- test cases are documented with expected and observed results;
- the final DBMS report contains the required design sections;
- the project can be demonstrated from a clean setup using the README.

---

# 1. SOURCE / ACADEMIC REQUIREMENTS

The university mini-project guideline requires the project to cover four evaluation areas: Project Planning & Requirement Analysis, Database Design, Functionality & Implementation, and Demonstration & Individual Contribution. Its required workflow explicitly includes functional requirements, entities and relationships, ER/EER diagrams, relational schema conversion, anomaly identification, functional dependencies, normalization, MySQL/Oracle implementation, and execution of queries/functions/procedures/triggers. The final demonstration/report is the assessed deliverable.

The supplied example DBMS report uses a report organization built around Introduction, Problem Statement, System Architecture and Modules, Functional Requirements, Entities/Relationships/Attributes, ER/EER Diagram, Relational Schema, and Codd's Rules. This specification preserves that academic shape while extending it with implementation, testing, and demonstration sections.

**Academic note:** Codd's Rules are not a core implementation requirement in the mini-project guideline; include them in the report only as an appendix/extra section if the instructor expects the same structure as the supplied example report.

---

# 2. PROJECT OBJECTIVE

Build a desktop EV charging reservation system in which registered users can find EV charging stations, view charging slots, check time availability, reserve an available slot for one hour, complete a simulated payment, view booking history, and cancel future bookings. An administrator can manage stations and charging slots and view basic reports.

The project exists primarily to demonstrate:

- relational database design;
- primary and foreign keys;
- integrity constraints;
- normalization up to 3NF;
- SQL joins, filtering, grouping, aggregation, subqueries/CTEs;
- transactions and concurrency control;
- JDBC integration;
- stored function/procedure/trigger usage;
- a complete end-to-end database-driven application.

---

# 3. SCOPE

## 3.1 Included

### Customer

- register account;
- log in and log out;
- search stations by city/name;
- view station details;
- view charger/slot details;
- select a future booking date/time;
- see currently available slots for that time;
- book exactly one 60-minute interval;
- complete a mock payment automatically;
- view booking history;
- cancel a future confirmed booking;
- view booking/payment details.

### Administrator

- log in using an administrator account;
- create/update/deactivate stations;
- create/update/deactivate charging slots;
- put a station in maintenance/inactive state;
- view station usage/revenue reports;
- view bookings and payments.

### Database functionality

- schema with PK/FK/UNIQUE/CHECK constraints;
- indexes for important access paths;
- transactional booking;
- `SELECT ... FOR UPDATE` locking for booking concurrency;
- stored function;
- stored procedure;
- trigger;
- complex reporting queries.

## 3.2 Explicitly excluded

- real online payment gateways;
- live EV charger hardware control;
- RFID/card hardware;
- GPS/maps;
- charger telemetry;
- notifications/email/SMS;
- mobile application;
- web deployment;
- multi-tenant cloud deployment;
- recurring bookings;
- variable-duration bookings;
- promotions/coupons;
- wallet/refund settlement with an external provider;
- real-time charger health monitoring.

---

# 4. FUNCTIONAL REQUIREMENTS

## FR-01 — User Registration

The system shall allow a new customer to register with:

- first name;
- last name;
- email;
- phone number;
- password.

Rules:

- email must be unique;
- phone must be unique and contain exactly 10 digits, starting with `9876`;
- password is never stored as plaintext;
- new customer role is `CUSTOMER`;
- new customer status is `ACTIVE`.

## FR-02 — Authentication

The system shall allow an active user to log in using email + password.

Rules:

- blocked users cannot log in;
- invalid credentials must display a generic authentication error;
- successful login creates an in-memory session object only;
- no session/password is persisted in the database.

## FR-03 — Station Search

A customer shall be able to:

- search by city;
- search by station name;
- view all active stations when search fields are empty.

## FR-04 — Station Details

For a selected station, the system shall display:

- station name;
- address;
- city/state/postal code;
- operating hours;
- station status;
- all associated charging slots;
- connector type;
- maximum power in kW;
- hourly rate;
- slot status.

## FR-05 — Availability

A customer chooses a future start date/time.

Every booking is exactly 60 minutes.

The system shall show a slot as unavailable when an existing non-cancelled booking overlaps the requested interval.

Overlap rule:

```text
existing.start_time < requested.end_time
AND
existing.end_time > requested.start_time
```

Adjacent bookings are allowed:

```text
10:00–11:00
11:00–12:00
```

These do not overlap.

## FR-06 — Booking

A customer shall be able to reserve one available slot for exactly one hour.

A booking must satisfy all of the following:

- customer is authenticated and active;
- slot exists;
- slot is active;
- station is active;
- requested start time is in the future;
- requested start minute must be `00` (hour-aligned);
- requested interval must fit completely inside station operating hours;
- no active booking overlaps the requested interval.

A successful booking creates:

1. one row in `bookings` with status `CONFIRMED`;
2. one row in `payments` with method `MOCK`, status `PAID`;
3. both operations in one DB transaction.

If any DB operation fails, neither row may remain committed.

## FR-07 — Double-booking Prevention

The booking path must acquire a database row lock on the selected charging slot before performing the final overlap check.

Required conceptual sequence:

```text
BEGIN
  ↓
LOCK charging_slots row with SELECT ... FOR UPDATE
  ↓
validate slot/station state and operating hours
  ↓
check overlapping active bookings
  ↓
if conflict → ROLLBACK
  ↓
insert booking
  ↓
insert mock payment
  ↓
COMMIT
```

Java `synchronized` is not a substitute for the DB lock.

## FR-08 — Mock Payment

There is no external payment gateway.

Every successful booking uses:

- payment method: `MOCK`;
- payment status: `PAID`;
- generated unique transaction reference.

The UI must clearly label this as simulated/mock payment.

## FR-09 — Booking History

The user shall be able to view their own bookings ordered by start time descending.

Columns shown should include:

- booking ID;
- station;
- slot number;
- connector type;
- start time;
- end time;
- booking status;
- amount;
- payment status.

## FR-10 — Cancellation

A customer may cancel only a `CONFIRMED` booking whose start time is still in the future.

Cancellation is transactional:

```text
BEGIN
  ↓
lock target booking row
  ↓
verify ownership, status, and future start time
  ↓
update booking status → CANCELLED
  ↓
update associated paid payment → REFUNDED
  ↓
COMMIT
```

Because the payment is mock, `REFUNDED` is only a database state representing a simulated refund.

A cancelled booking must no longer block future availability.

## FR-11 — Completion Status

Before displaying history/report data, the application may synchronize old confirmed bookings whose `end_time <= current application time` to `COMPLETED`.

This is a deterministic application action, not a background scheduler.

Cancelled bookings remain `CANCELLED`.

## FR-12 — Administrator Functions

Administrators shall be able to:

- create stations;
- update station details;
- deactivate/reactivate stations;
- place stations in maintenance state;
- add charging slots to stations;
- update charging-slot attributes;
- deactivate/reactivate charging slots;
- view reports;
- view booking/payment records.

An inactive station cannot accept bookings.
An inactive/maintenance slot cannot accept bookings.

---

# 5. NON-FUNCTIONAL REQUIREMENTS

## NFR-01 — Simplicity

Use Core Java/JDBC + Swing. Do not introduce a large enterprise framework.

## NFR-02 — Maintainability

Use clear separation between UI, controller, service, DAO, model, and utility layers.

## NFR-03 — Integrity

Use DB constraints plus application validation.

## NFR-04 — Security basics

- PBKDF2 password hashing with per-user random salt;
- `PreparedStatement` for values;
- no plaintext passwords in DB;
- no DB credentials in source control.

## NFR-05 — Reliability

Database transactions must rollback after booking/cancellation failure.

## NFR-06 — Demonstrability

A complete booking flow must be executable in under a few minutes from the desktop UI.

---

# 6. USERS AND ROLES

## CUSTOMER

Can manage only their own account/booking operations and browse active stations.

## ADMIN

Can perform station/slot management and reporting.

Role values in database:

```text
CUSTOMER
ADMIN
```

User status values:

```text
ACTIVE
BLOCKED
```

---

# 7. DATA MODEL

## 7.1 Core entities

1. `USERS`
2. `EV_STATIONS`
3. `CHARGING_SLOTS`
4. `BOOKINGS`
5. `PAYMENTS`

## 7.2 Relationships

```text
USER 1 ───────── N BOOKING

EV_STATION 1 ───────── N CHARGING_SLOT

CHARGING_SLOT 1 ────── N BOOKING     (over time)

BOOKING 1 ──────────── 0..1 PAYMENT
```

Meaning:

- one user can make many bookings;
- one station has many physical charging slots;
- one slot can have many bookings over different times;
- one booking has at most one payment record.

---

# 8. ENTITY ATTRIBUTES

## 8.1 USERS

| Attribute | Type | Constraints | Meaning |
|---|---|---|---|
| `user_id` | BIGINT UNSIGNED | PK, AUTO_INCREMENT | User identifier |
| `first_name` | VARCHAR(50) | NOT NULL | First name |
| `last_name` | VARCHAR(50) | NOT NULL | Last name |
| `email` | VARCHAR(120) | NOT NULL, UNIQUE | Login email |
| `phone` | VARCHAR(20) | NOT NULL, UNIQUE | Contact number |
| `password_hash` | VARCHAR(255) | NOT NULL | PBKDF2 encoded value |
| `role` | ENUM | NOT NULL | CUSTOMER/ADMIN |
| `status` | ENUM | NOT NULL | ACTIVE/BLOCKED |
| `created_at` | TIMESTAMP | NOT NULL | Creation time |

## 8.2 EV_STATIONS

| Attribute | Type | Constraints | Meaning |
|---|---|---|---|
| `station_id` | BIGINT UNSIGNED | PK, AUTO_INCREMENT | Station identifier |
| `station_name` | VARCHAR(100) | NOT NULL | Station name |
| `address` | VARCHAR(255) | NOT NULL | Street/address |
| `city` | VARCHAR(80) | NOT NULL | City |
| `state` | VARCHAR(80) | NOT NULL | State/region |
| `postal_code` | VARCHAR(15) | NOT NULL | Postal code |
| `latitude` | DECIMAL(9,6) | nullable | Approximate coordinate |
| `longitude` | DECIMAL(9,6) | nullable | Approximate coordinate |
| `opening_time` | TIME | NOT NULL | Opening time |
| `closing_time` | TIME | NOT NULL | Closing time |
| `status` | ENUM | NOT NULL | ACTIVE/MAINTENANCE/INACTIVE |
| `created_at` | TIMESTAMP | NOT NULL | Creation time |

Rule:

```text
opening_time < closing_time
```

Overnight station hours are explicitly not supported.

## 8.3 CHARGING_SLOTS

| Attribute | Type | Constraints | Meaning |
|---|---|---|---|
| `slot_id` | BIGINT UNSIGNED | PK, AUTO_INCREMENT | Slot identifier |
| `station_id` | BIGINT UNSIGNED | FK, NOT NULL | Parent station |
| `slot_number` | INT UNSIGNED | NOT NULL | Human-readable slot number |
| `connector_type` | ENUM | NOT NULL | Connector type |
| `max_power_kw` | DECIMAL(6,2) | NOT NULL, >0 | Maximum charging power |
| `hourly_rate` | DECIMAL(10,2) | NOT NULL, >=0 | One-hour booking price |
| `status` | ENUM | NOT NULL | ACTIVE/MAINTENANCE/INACTIVE |
| `created_at` | TIMESTAMP | NOT NULL | Creation time |

Unique constraint:

```text
(station_id, slot_number)
```

Recommended connector values:

```text
CCS2
CHAdeMO
TYPE2
GB_T
```

Only these values are required by the project; no connector expansion is necessary.

## 8.4 BOOKINGS

| Attribute | Type | Constraints | Meaning |
|---|---|---|---|
| `booking_id` | BIGINT UNSIGNED | PK, AUTO_INCREMENT | Booking identifier |
| `user_id` | BIGINT UNSIGNED | FK, NOT NULL | Customer |
| `slot_id` | BIGINT UNSIGNED | FK, NOT NULL | Reserved slot |
| `start_time` | DATETIME(3) | NOT NULL | Booking start |
| `end_time` | DATETIME(3) | NOT NULL | Booking end |
| `rate_per_hour` | DECIMAL(10,2) | NOT NULL | Price snapshot at booking time |
| `status` | ENUM | NOT NULL | CONFIRMED/CANCELLED/COMPLETED |
| `created_at` | TIMESTAMP | NOT NULL | Creation time |
| `updated_at` | TIMESTAMP | NOT NULL | Last update |

Rules:

```text
end_time > start_time
end_time = start_time + 1 hour
start_time minute = 0
```

The `rate_per_hour` value is copied from the slot at booking time so that later rate changes do not change historical booking prices.

### Active-booking uniqueness defense

The database must include a generated column that is non-NULL only for active reservations:

```text
active_start_time = start_time when status = CONFIRMED, otherwise NULL
```

and a unique constraint on:

```text
(slot_id, active_start_time)
```

This prevents exact duplicate active starts while still allowing a cancelled booking to be rebooked at the same start time. The row-lock + overlap check remains the primary concurrency protection because the unique constraint does not detect arbitrary interval overlap.

## 8.5 PAYMENTS

| Attribute | Type | Constraints | Meaning |
|---|---|---|---|
| `payment_id` | BIGINT UNSIGNED | PK, AUTO_INCREMENT | Payment identifier |
| `booking_id` | BIGINT UNSIGNED | FK, UNIQUE, NOT NULL | One-to-one booking payment |
| `amount` | DECIMAL(10,2) | NOT NULL, >=0 | Amount paid |
| `method` | ENUM | NOT NULL | MOCK |
| `status` | ENUM | NOT NULL | PAID/REFUNDED |
| `transaction_reference` | VARCHAR(80) | UNIQUE, NOT NULL | Simulated transaction id |
| `paid_at` | TIMESTAMP | NOT NULL | Payment timestamp |
| `updated_at` | TIMESTAMP | NOT NULL | Last change |

Only `MOCK` is required as a payment method in this project.

---

# 9. RELATIONAL SCHEMA

```text
USERS(
  user_id PK,
  first_name,
  last_name,
  email UNIQUE,
  phone UNIQUE,
  password_hash,
  role,
  status,
  created_at
)

EV_STATIONS(
  station_id PK,
  station_name,
  address,
  city,
  state,
  postal_code,
  latitude,
  longitude,
  opening_time,
  closing_time,
  status,
  created_at
)

CHARGING_SLOTS(
  slot_id PK,
  station_id FK → EV_STATIONS.station_id,
  slot_number,
  connector_type,
  max_power_kw,
  hourly_rate,
  status,
  created_at,
  UNIQUE(station_id, slot_number)
)

BOOKINGS(
  booking_id PK,
  user_id FK → USERS.user_id,
  slot_id FK → CHARGING_SLOTS.slot_id,
  start_time,
  end_time,
  rate_per_hour,
  status,
  created_at,
  updated_at,
  UNIQUE(slot_id, active_start_time)
)

PAYMENTS(
  payment_id PK,
  booking_id FK → BOOKINGS.booking_id UNIQUE,
  amount,
  method,
  status,
  transaction_reference UNIQUE,
  paid_at,
  updated_at
)
```

---

# 10. FUNCTIONAL DEPENDENCIES

The following dependencies must be discussed in the report.

## USERS

```text
user_id → first_name, last_name, email, phone, password_hash, role, status, created_at
email → user_id, first_name, last_name, phone, password_hash, role, status, created_at
phone → user_id, first_name, last_name, email, password_hash, role, status, created_at
```

The last two reflect the uniqueness constraints.

## EV_STATIONS

```text
station_id → station_name, address, city, state, postal_code,
              latitude, longitude, opening_time, closing_time, status, created_at
```

## CHARGING_SLOTS

```text
slot_id → station_id, slot_number, connector_type,
          max_power_kw, hourly_rate, status, created_at

(station_id, slot_number) → slot_id, connector_type,
                             max_power_kw, hourly_rate, status, created_at
```

## BOOKINGS

```text
booking_id → user_id, slot_id, start_time, end_time,
             rate_per_hour, status, created_at, updated_at
```

## PAYMENTS

```text
payment_id → booking_id, amount, method, status,
             transaction_reference, paid_at, updated_at

booking_id → payment_id, amount, method, status,
             transaction_reference, paid_at, updated_at
```

because `booking_id` is UNIQUE in `payments`.

---

# 11. NORMALIZATION

The final schema must be justified as being in 3NF.

## 11.1 First Normal Form

All attributes are atomic. No repeating groups or multi-valued attributes are stored in a single field.

Examples:

- one phone value per user;
- one connector type per slot;
- one station per station row;
- one booking interval per booking row.

## 11.2 Second Normal Form

All non-key attributes depend on the whole candidate key. Most tables use a single-column surrogate primary key; `CHARGING_SLOTS` additionally has a composite candidate key `(station_id, slot_number)` and its non-key attributes describe that entire slot identity.

## 11.3 Third Normal Form

Non-key attributes do not depend transitively on another non-key attribute.

Examples:

- station address belongs to `EV_STATIONS`, not `BOOKINGS`;
- connector type and hourly rate belong to `CHARGING_SLOTS`, not `EV_STATIONS`;
- payment data belongs to `PAYMENTS`, not `BOOKINGS`.

The booking price snapshot is deliberately stored in `BOOKINGS` because it is a historical fact about the booking, not a duplicate current slot price.

---

# 12. ANOMALIES TO DISCUSS

The report must explain why separating the entities avoids common anomalies.

## Update anomaly

If station address were copied into every booking row, changing a station address would require updating many rows and could leave inconsistent values.

## Insert anomaly

If station and booking information were stored in one combined table, a new station with no booking would be difficult or impossible to represent without meaningless booking values.

## Delete anomaly

If the only row containing a station's descriptive information were deleted with its last booking, the station information could disappear accidentally.

The normalized design separates stable entity facts from event/transaction facts.

---

# 13. ER / EER DIAGRAM REQUIREMENT

Create both:

1. a standard ER diagram;
2. an EER diagram showing specialization/generalization where useful.

Recommended EER interpretation:

```text
USER
  ├── CUSTOMER
  └── ADMIN
```

The implementation may use the single `USERS` table with a `role` attribute rather than separate customer/admin tables. The EER diagram is an academic representation; the relational implementation uses the role attribute to avoid unnecessary duplication.

The ER/EER diagram must clearly show:

- primary keys;
- important attributes;
- cardinalities;
- relationship names;
- the one-to-one booking/payment relationship;
- the one-to-many station/slot relationship;
- the one-to-many user/booking relationship;
- the one-to-many slot/booking-over-time relationship.

The final report should insert exported diagram images rather than screenshots of an unfinished editor.

---

# 14. DATABASE DDL

The agent must create `database/01_schema.sql` containing an executable schema.

Use MySQL 8.0.16+ and InnoDB.

The schema must implement the following logical definition.

```sql
CREATE DATABASE IF NOT EXISTS ev_charging_system;
USE ev_charging_system;
```

### USERS

```sql
CREATE TABLE users (
    user_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('CUSTOMER','ADMIN') NOT NULL DEFAULT 'CUSTOMER',
    status ENUM('ACTIVE','BLOCKED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;
```

### EV_STATIONS

```sql
CREATE TABLE ev_stations (
    station_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    station_name VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    city VARCHAR(80) NOT NULL,
    state VARCHAR(80) NOT NULL,
    postal_code VARCHAR(15) NOT NULL,
    latitude DECIMAL(9,6) NULL,
    longitude DECIMAL(9,6) NULL,
    opening_time TIME NOT NULL,
    closing_time TIME NOT NULL,
    status ENUM('ACTIVE','MAINTENANCE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_station_hours CHECK (opening_time < closing_time),
    CONSTRAINT chk_station_lat CHECK (latitude IS NULL OR (latitude BETWEEN -90 AND 90)),
    CONSTRAINT chk_station_lon CHECK (longitude IS NULL OR (longitude BETWEEN -180 AND 180))
) ENGINE=InnoDB;
```

### CHARGING_SLOTS

```sql
CREATE TABLE charging_slots (
    slot_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    station_id BIGINT UNSIGNED NOT NULL,
    slot_number INT UNSIGNED NOT NULL,
    connector_type ENUM('CCS2','CHAdeMO','TYPE2','GB_T') NOT NULL,
    max_power_kw DECIMAL(6,2) NOT NULL,
    hourly_rate DECIMAL(10,2) NOT NULL,
    status ENUM('ACTIVE','MAINTENANCE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_slot_station
        FOREIGN KEY (station_id) REFERENCES ev_stations(station_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT uq_slot_number UNIQUE (station_id, slot_number),
    CONSTRAINT chk_slot_power CHECK (max_power_kw > 0),
    CONSTRAINT chk_slot_rate CHECK (hourly_rate >= 0)
) ENGINE=InnoDB;
```

### BOOKINGS

The implementation must allow cancelled rows to remain in history without permanently preventing the same time from being booked again.

```sql
CREATE TABLE bookings (
    booking_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    slot_id BIGINT UNSIGNED NOT NULL,
    start_time DATETIME(3) NOT NULL,
    end_time DATETIME(3) NOT NULL,
    rate_per_hour DECIMAL(10,2) NOT NULL,
    status ENUM('CONFIRMED','CANCELLED','COMPLETED') NOT NULL DEFAULT 'CONFIRMED',
    active_start_time DATETIME(3)
        GENERATED ALWAYS AS (
            CASE WHEN status = 'CONFIRMED' THEN start_time ELSE NULL END
        ) STORED,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_booking_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_booking_slot
        FOREIGN KEY (slot_id) REFERENCES charging_slots(slot_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT uq_active_slot_start UNIQUE (slot_id, active_start_time),
    CONSTRAINT chk_booking_time CHECK (end_time > start_time),
    CONSTRAINT chk_booking_rate CHECK (rate_per_hour >= 0)
) ENGINE=InnoDB;
```

Application code must additionally enforce that every booking is exactly 60 minutes and hour-aligned.

### PAYMENTS

```sql
CREATE TABLE payments (
    payment_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT UNSIGNED NOT NULL UNIQUE,
    amount DECIMAL(10,2) NOT NULL,
    method ENUM('MOCK') NOT NULL DEFAULT 'MOCK',
    status ENUM('PAID','REFUNDED') NOT NULL DEFAULT 'PAID',
    transaction_reference VARCHAR(80) NOT NULL UNIQUE,
    paid_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_booking
        FOREIGN KEY (booking_id) REFERENCES bookings(booking_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT chk_payment_amount CHECK (amount >= 0)
) ENGINE=InnoDB;
```

### Required indexes

```sql
CREATE INDEX idx_slots_station_status
    ON charging_slots(station_id, status);

CREATE INDEX idx_bookings_slot_time_status
    ON bookings(slot_id, start_time, end_time, status);

CREATE INDEX idx_bookings_user_time
    ON bookings(user_id, start_time);

CREATE INDEX idx_payments_status_date
    ON payments(status, paid_at);
```

The agent may add additional indexes only when justified by a query or measured need.

---

# 15. SEED DATA

Create `database/02_seed.sql`.

Seed data must contain enough records to make the demonstration meaningful:

- at least 1 admin account;
- at least 3 customer accounts;
- at least 3 stations;
- at least 3–5 slots per station;
- multiple connector types;
- multiple rates;
- multiple cities or neighborhoods to make searching visible;
- sample historical bookings;
- sample payments.

The agent must provide a small Java utility for creating a correctly hashed admin/customer password rather than hard-coding plaintext hashes that are not explainable.

For a fresh demo installation, an admin login may be initialized using a documented local demo password such as `Admin@123`; the password must not be committed into source code as a database credential or treated as a production secret.

---

# 16. REQUIRED SQL QUERIES

Create `database/03_queries.sql` containing at least the following.

## Q1 — Available slots for a requested interval

```sql
SELECT
    cs.slot_id,
    cs.slot_number,
    cs.connector_type,
    cs.max_power_kw,
    cs.hourly_rate
FROM charging_slots cs
JOIN ev_stations es ON es.station_id = cs.station_id
WHERE cs.station_id = ?
  AND cs.status = 'ACTIVE'
  AND es.status = 'ACTIVE'
  AND NOT EXISTS (
      SELECT 1
      FROM bookings b
      WHERE b.slot_id = cs.slot_id
        AND b.status = 'CONFIRMED'
        AND b.start_time < ?
        AND b.end_time > ?
  )
ORDER BY cs.slot_number;
```

Parameters are requested `end_time` and `start_time` respectively in the correct JDBC order.

## Q2 — User booking history

Join:

```text
users → bookings → charging_slots → ev_stations → payments
```

and return booking/payment information for one user.

## Q3 — Station revenue by month

Use a CTE/aggregation to calculate paid revenue per station and month.

## Q4 — Most-used connector type per station

Count completed/confirmed bookings by connector type and rank within station.

## Q5 — Station-wise booking counts

Return each station and its total number of bookings, including stations with zero bookings using an appropriate outer join.

## Q6 — Revenue ranking

Rank stations by total paid revenue using a window function such as `RANK()`.

## Q7 — Cancellation count by station

Group cancelled bookings by station.

## Q8 — Average hourly rate by connector type

Use aggregation and grouping.

## Q9 — Users with more than N bookings

Use `GROUP BY ... HAVING`.

## Q10 — Nested/subquery example

Return stations having at least one charging slot above a configurable power threshold.

The exact SQL may be formatted by the agent, but the query concepts above must all exist in the final SQL file and report.

---

# 17. REQUIRED STORED FUNCTION

Create `database/04_routines.sql`.

Function name:

```text
fn_booking_hours
```

Purpose:

Return the number of booked hours from `start_time` and `end_time`.

Expected result for this project is normally `1.00`, but the function should use the stored interval rather than blindly returning `1` so it demonstrates meaningful SQL routine logic.

Example conceptual implementation:

```sql
TIMESTAMPDIFF(MINUTE, p_start, p_end) / 60.0
```

Because this is a stored function, the final implementation should use an appropriate MySQL numeric return type.

---

# 18. REQUIRED STORED PROCEDURE

Procedure name:

```text
sp_station_revenue
```

Input:

```text
p_station_id
p_from_date
p_to_date
```

Output/result set:

- station;
- number of paid bookings;
- total revenue;
- total refunded amount;
- net simulated revenue.

The procedure must use joins/aggregation and be callable from MySQL and Java.

---

# 19. REQUIRED TRIGGER

Trigger name:

```text
trg_booking_cancel_payment
```

Purpose:

When a booking transitions from `CONFIRMED` to `CANCELLED`, automatically change its associated paid mock payment to `REFUNDED`.

Important implementation note:

- Avoid creating a trigger that recursively modifies the `bookings` table.
- The trigger should update only `payments`.
- The Java cancellation transaction must be compatible with the trigger.

The report must explain why the trigger demonstrates database-side business integrity.

---

# 20. JAVA PROJECT STRUCTURE

Use Maven standard layout:

```text
EV-Charging-Slot-Booking-System/
│
├── pom.xml
├── README.md
├── .gitignore
│
├── database/
│   ├── 01_schema.sql
│   ├── 02_seed.sql
│   ├── 03_queries.sql
│   ├── 04_routines.sql
│   └── 05_reset.sql
│
├── docs/
│   ├── EV_Charging_Slot_Booking_SDLС_SPEC.md
│   ├── er-diagram.png
│   ├── eer-diagram.png
│   ├── test-report.md
│   └── demo-script.md
│
└── src/
    ├── main/
    │   └── java/
    │       └── com/
    │           └── evcharging/
    │               ├── Main.java
    │               ├── model/
    │               │   ├── User.java
    │               │   ├── EVStation.java
    │               │   ├── ChargingSlot.java
    │               │   ├── Booking.java
    │               │   └── Payment.java
    │               ├── dao/
    │               │   ├── UserDAO.java
    │               │   ├── StationDAO.java
    │               │   ├── SlotDAO.java
    │               │   ├── BookingDAO.java
    │               │   └── PaymentDAO.java
    │               ├── service/
    │               │   ├── AuthService.java
    │               │   ├── StationService.java
    │               │   ├── BookingService.java
    │               │   ├── PaymentService.java
    │               │   └── AdminService.java
    │               ├── controller/
    │               │   ├── LoginController.java
    │               │   ├── CustomerController.java
    │               │   └── AdminController.java
    │               ├── ui/
    │               │   ├── LoginFrame.java
    │               │   ├── RegisterFrame.java
    │               │   ├── CustomerDashboardFrame.java
    │               │   ├── StationSearchPanel.java
    │               │   ├── BookingPanel.java
    │               │   ├── BookingHistoryPanel.java
    │               │   ├── AdminDashboardFrame.java
    │               │   └── ReportPanel.java
    │               ├── security/
    │               │   └── PasswordHasher.java
    │               ├── util/
    │               │   ├── DBConnection.java
    │               │   ├── AppConfig.java
    │               │   └── ValidationUtil.java
    │               └── exception/
    │                   ├── BookingConflictException.java
    │                   ├── AuthenticationException.java
    │                   └── ValidationException.java
    │
    └── test/
        └── java/
            └── com/evcharging/
```

The exact count of UI classes may be adjusted as long as responsibilities remain clear. The database names and DAO/service responsibilities are not optional.

---

# 21. MAVEN CONFIGURATION

Use Java 21.

MySQL's current Connector/J is distributed as Maven artifact `com.mysql:mysql-connector-j`. At the time this specification was prepared, MySQL documents Connector/J 26.7.0 as the current GA line. Use that version unless the environment specifically requires an earlier compatible 8.x/9.x release.

Required dependency:

```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>26.7.0</version>
</dependency>
```

Testing dependency:

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.13.4</version>
    <scope>test</scope>
</dependency>
```

No BCrypt dependency is required. Use Java's built-in:

```text
PBKDF2WithHmacSHA256
```

for password hashing.

The agent may use Maven plugins for compilation and test execution, but should avoid unnecessary libraries.

---

# 22. DATABASE CONNECTION

Class:

```text
com.evcharging.util.DBConnection
```

Use JDBC `DriverManager` rather than a connection pool for this mini-project.

Configuration must be externalized using environment variables or a local configuration file excluded from Git.

Required logical configuration:

```text
DB_URL      = jdbc:mysql://localhost:3306/ev_charging_system
DB_USER     = local MySQL user
DB_PASSWORD = local MySQL password
APP_TIMEZONE = Asia/Kolkata
```

JDBC URL should include appropriate Unicode/time handling if needed, for example:

```text
jdbc:mysql://localhost:3306/ev_charging_system?useSSL=false&serverTimezone=Asia/Kolkata
```

Do not hard-code the actual local password in source code.

---

# 23. PASSWORD HASHING

Implement `PasswordHasher` using:

```text
PBKDF2WithHmacSHA256
```

Recommended fixed parameters for this project:

```text
iterations = 600000
salt length = 16 bytes
derived key length = 256 bits
```

Each stored password should contain enough information to verify the password later, for example:

```text
pbkdf2_sha256$iterations$saltBase64$hashBase64
```

The exact Java encoding may be different but must preserve the same information.

Verification must use a constant-time comparison such as `MessageDigest.isEqual`.

---

# 24. JAVA MODEL RULES

Use normal Java classes with private fields, constructors, getters/setters where useful, and clear `toString()` methods for debugging.

Enums should represent DB status/role values, for example:

```text
UserRole
UserStatus
StationStatus
SlotStatus
BookingStatus
PaymentStatus
ConnectorType
PaymentMethod
```

Do not represent constrained status values as arbitrary strings throughout the application.

---

# 25. DAO RESPONSIBILITIES

## UserDAO

Required operations:

```text
createUser(...)
findByEmail(email)
findById(userId)
updateStatus(userId, status)
listCustomers()
```

## StationDAO

Required operations:

```text
findActiveStations(searchText, city)
findById(stationId)
createStation(...)
updateStation(...)
updateStationStatus(...)
listAllStations()
```

## SlotDAO

Required operations:

```text
findByStation(stationId)
createSlot(...)
updateSlot(...)
updateSlotStatus(...)
findById(slotId)
```

## BookingDAO

Required operations:

```text
findAvailableSlots(stationId, requestedStart)
bookSlot(userId, slotId, requestedStart)
findByUser(userId)
findByIdForUpdate(bookingId)
cancelBooking(userId, bookingId)
refreshCompletedBookings()
```

`bookSlot` and `cancelBooking` must use transactions.

## PaymentDAO

Required operations:

```text
createMockPayment(bookingId, amount, reference)
findByBooking(bookingId)
updateRefunded(bookingId)
```

The booking service may perform payment insertion inside the booking transaction rather than exposing raw transaction logic to the UI.

---

# 26. CRITICAL BOOKING ALGORITHM

The agent must implement the booking workflow with database locking. Conceptual Java/JDBC algorithm:

```text
function bookSlot(userId, slotId, requestedStart):

    validate userId
    validate slotId
    validate requestedStart

    end = requestedStart + 1 hour

    require requestedStart is in future
    require requestedStart.minute == 0
    require requestedStart.second == 0
    require requestedStart.nano == 0

    open connection
    disable auto-commit
    set transaction isolation to READ_COMMITTED

    LOCK selected slot row:

        SELECT
            cs.slot_id,
            cs.station_id,
            cs.status AS slot_status,
            cs.hourly_rate,
            es.status AS station_status,
            es.opening_time,
            es.closing_time
        FROM charging_slots cs
        JOIN ev_stations es ON es.station_id = cs.station_id
        WHERE cs.slot_id = ?
        FOR UPDATE

    if no row:
        throw ValidationException("Slot not found")

    if slot_status != ACTIVE:
        rollback
        throw ValidationException("Slot unavailable")

    if station_status != ACTIVE:
        rollback
        throw ValidationException("Station unavailable")

    require local start time >= opening_time
    require local end time <= closing_time

    check overlap using current booking state:

        SELECT booking_id
        FROM bookings
        WHERE slot_id = ?
          AND status = 'CONFIRMED'
          AND start_time < ?
          AND end_time > ?
        FOR UPDATE

    if any row exists:
        rollback
        throw BookingConflictException

    insert booking:
        user_id
        slot_id
        start_time
        end_time
        rate_per_hour = locked slot hourly_rate
        status = CONFIRMED

    insert payment:
        booking_id = new booking
        amount = rate_per_hour
        method = MOCK
        status = PAID
        unique transaction_reference

    commit

    return booking confirmation

on any failure:
    rollback
    rethrow meaningful application exception

finally:
    close result sets/statements/connection
```

## Why both lock and overlap check are required

The lock serializes booking attempts for the same physical slot.

The overlap query enforces the actual business rule.

The unique active-start constraint is only defense in depth; it cannot replace interval-overlap checking.

---

# 27. CANCELLATION ALGORITHM

```text
function cancelBooking(userId, bookingId):

    begin transaction

    SELECT booking + payment information
    FROM bookings
    WHERE booking_id = ?
      AND user_id = ?
    FOR UPDATE

    if no booking:
        rollback
        throw ValidationException

    if status != CONFIRMED:
        rollback
        throw ValidationException

    if start_time <= now:
        rollback
        throw ValidationException

    update bookings
    set status = CANCELLED

    allow trigger trg_booking_cancel_payment to change PAID → REFUNDED

    commit
```

The application must not assume cancellation succeeded unless the transaction commits.

---

# 28. TRANSACTION / CONCURRENCY EXPLANATION FOR VIVA

The project must be able to explain this exact race condition:

```text
User A                Database                 User B
  |                      |                       |
  |-- lock slot -------->|                       |
  |                      |<----- lock slot ------|
  |                      |      waits            |
  |-- overlap check ---->|                       |
  |-- insert booking --->|                       |
  |-- commit ----------->|                       |
  |                      |---- lock granted ---->|
  |                      |<--- overlap check ----|
  |                      |   conflict found      |
  |                      |<------ rollback ------|
```

Core explanation:

- `FOR UPDATE` locks the chosen slot row until the transaction ends;
- another transaction attempting the same row must wait;
- once the first booking commits, the next booking attempt sees the committed booking during its current/locking read;
- the second transaction rejects the overlap and rolls back;
- Java-only synchronization cannot guarantee this across multiple JVM instances/processes.

This transaction/concurrency behavior is a major viva point and must remain demonstrable in code.

---

# 29. EDGE CASES

The following must be explicitly handled and tested.

| Case | Expected result |
|---|---|
| exact duplicate booking time | rejected |
| partial overlap at beginning | rejected |
| partial overlap at end | rejected |
| requested interval completely contains existing booking | rejected |
| existing booking completely contains requested interval | rejected |
| adjacent interval | allowed |
| slot maintenance | rejected |
| slot inactive | rejected |
| station maintenance | rejected |
| station inactive | rejected |
| past start time | rejected |
| non-hour-aligned start | rejected |
| interval beyond closing time | rejected |
| station opens after requested start | rejected |
| cancelled old booking at same start | new booking allowed |
| duplicate email registration | rejected by app + DB |
| duplicate phone registration | rejected by app + DB |
| blocked account login | rejected |
| wrong password | rejected |
| cancelled booking cancellation attempt | rejected |
| completed booking cancellation attempt | rejected |
| booking insert failure | whole transaction rolled back |
| payment insert failure | booking also rolled back |
| cancel payment update | booking cancellation committed consistently |

---

# 30. UI REQUIREMENTS — SWING

The application must use Java Swing, not JavaFX.

The interface should be functional and visually clean, but no time should be spent on elaborate animations.

## Screen 1 — Login

Fields:

- email;
- password.

Buttons:

- Login;
- Register.

Display role-appropriate dashboard after success.

## Screen 2 — Registration

Fields:

- first name;
- last name;
- email;
- phone;
- password;
- confirm password.

Buttons:

- Register;
- Back to Login.

## Screen 3 — Customer Dashboard

Display:

- logged-in user's name;
- Search Stations;
- My Bookings;
- Logout.

## Screen 4 — Station Search

Controls:

- city text field;
- station name text field;
- Search button;
- station table.

Selecting a station opens details.

## Screen 5 — Station Details / Booking

Display station information and slot table.

Booking controls:

- date selector or a text field with strict parsing;
- hour selector;
- selected slot;
- booking summary;
- Book & Pay (Mock) button.

The UI must make it impossible or clearly invalid to select a non-hour start time.

After success, show booking ID, station, slot, interval, amount, and mock transaction reference.

## Screen 6 — My Bookings

Show current and past bookings.

Provide Cancel for eligible future confirmed bookings only.

## Screen 7 — Admin Dashboard

Tabs/panels:

- Stations;
- Charging Slots;
- Bookings;
- Payments;
- Reports.

Use forms + tables rather than complicated navigation.

---

# 31. ERROR-HANDLING RULES

User-facing exceptions must be meaningful but not expose SQL internals.

Examples:

```text
"Invalid email or password."
"This account is blocked."
"The charging slot is no longer available."
"The selected time overlaps an existing booking."
"The station is currently unavailable."
"The selected time is outside station operating hours."
"A booking must start on an exact hour."
"You can only cancel future confirmed bookings."
```

SQL error details should be logged for debugging, not shown directly in the UI.

---

# 32. LOGGING

Use simple Java logging (`java.util.logging`) unless an additional logging library is genuinely required.

Log:

- application startup;
- DB connection failures;
- authentication events without password data;
- booking success/failure;
- cancellation success/failure;
- unexpected exceptions.

Never log passwords, password hashes, or DB passwords.

---

# 33. TESTING STRATEGY

Create tests under `src/test/java`.

At minimum, cover:

## Unit tests

- password hashing + verification;
- validation rules;
- booking duration calculation;
- overlap detection helper;
- money calculation.

## Integration/manual DB tests

- registration with unique values;
- duplicate email;
- duplicate phone;
- correct login;
- blocked login;
- available slot query;
- successful booking;
- exact duplicate rejection;
- overlap rejection;
- adjacent booking acceptance;
- cancellation;
- payment changes to REFUNDED;
- station/slot status rejection;
- stored function;
- stored procedure;
- trigger.

## Concurrency test

Create a small test/demo utility that starts two booking attempts for the same slot and overlapping time.

Expected result:

```text
exactly one transaction succeeds
exactly one transaction fails with BookingConflictException or equivalent
```

The test must verify database state afterward rather than relying only on console messages.

---

# 34. ACCEPTANCE TEST MATRIX

The final report should include a table like the following.

| ID | Test | Expected | Result |
|---|---|---|---|
| AT-01 | Register new user | user inserted | PASS |
| AT-02 | Duplicate email | rejected | PASS |
| AT-03 | Valid login | dashboard opens | PASS |
| AT-04 | Invalid login | error shown | PASS |
| AT-05 | Search station | matching rows | PASS |
| AT-06 | View slots | slot list shown | PASS |
| AT-07 | Book free slot | booking + payment inserted | PASS |
| AT-08 | Double-book same interval | second rejected | PASS |
| AT-09 | Partial overlap | rejected | PASS |
| AT-10 | Adjacent interval | allowed | PASS |
| AT-11 | Cancel future booking | status CANCELLED + REFUNDED | PASS |
| AT-12 | Cancel old/completed booking | rejected | PASS |
| AT-13 | Inactive station booking | rejected | PASS |
| AT-14 | Inactive slot booking | rejected | PASS |
| AT-15 | Stored function | correct result | PASS |
| AT-16 | Stored procedure | result set returned | PASS |
| AT-17 | Trigger | payment becomes REFUNDED | PASS |
| AT-18 | Admin creates station | station appears | PASS |
| AT-19 | Admin manages slot | slot changes persist | PASS |
| AT-20 | Concurrency race | one success, one conflict | PASS |

Do not mark a test PASS until it has actually been executed.

---

# 35. README REQUIREMENTS

The final root `README.md` must include:

1. project overview;
2. features;
3. software prerequisites;
4. MySQL setup;
5. Maven setup;
6. database creation order;
7. environment/configuration instructions;
8. how to run;
9. admin demo credentials/process;
10. test instructions;
11. SQL routines and trigger instructions;
12. troubleshooting;
13. project structure;
14. team contribution placeholder.

Basic setup flow must be:

```text
install JDK 21
install MySQL 8.0+
install Maven (or use Maven wrapper)
open project
configure DB credentials
run 01_schema.sql
run 02_seed.sql
run 04_routines.sql
run mvn test
run application
```

---

# 36. RESET SCRIPT

Create `database/05_reset.sql`.

It must safely remove the project's tables/routines in dependency order so the database can be recreated for a clean demonstration.

The reset script must not drop the user's whole MySQL server or unrelated databases.

---

# 37. REPORT STRUCTURE

The final university report should use the following organization.

## 1. Introduction

Explain EV charging demand and the purpose of a slot reservation database system.

## 2. Problem Statement

Describe the difficulty of locating available charging slots and preventing conflicting reservations when data is manually managed.

## 3. Objectives and Scope

State what the system solves and what is deliberately outside scope.

## 4. SDLC / Project Planning

Explain:

- problem selection;
- requirements gathering;
- analysis;
- design;
- implementation;
- testing;
- deployment/demo;
- maintenance considerations.

For this mini-project, a straightforward sequential/incremental implementation plan is acceptable.

## 5. System Architecture and Modules

Show:

```text
Swing UI
   ↓
Controller
   ↓
Service
   ↓
DAO
   ↓
JDBC
   ↓
MySQL
```

Explain each module.

## 6. Functional Requirements

Include FR-01 through FR-12.

## 7. Entities, Relationships and Attributes

Include entity descriptions and cardinalities.

## 8. ER Diagram

Insert final ER diagram.

## 9. EER Diagram

Insert final EER diagram.

## 10. Relational Schema

Show each table, PK/FK, candidate/alternate unique keys, and important constraints.

## 11. Functional Dependencies

Include the dependency analysis from this specification.

## 12. Normalization

Explain 1NF, 2NF, and 3NF using the actual project schema.

## 13. Anomalies

Explain insertion, update, and deletion anomalies and how decomposition avoids them.

## 14. Database Implementation

Include DDL screenshots and selected table outputs.

## 15. SQL Queries

Include representative simple and complex queries with output screenshots.

## 16. Function, Procedure and Trigger

Show SQL definitions and execution results.

## 17. Java/JDBC Implementation

Explain packages/classes and how JDBC connects Java to MySQL.

## 18. Transaction and Concurrency Control

Explain `FOR UPDATE`, transactions, overlap detection, and why Java `synchronized` alone is insufficient.

## 19. User Interface

Include screenshots of:

- registration;
- login;
- station search;
- booking;
- confirmation/payment;
- history;
- admin dashboard;
- reports.

## 20. Testing

Include the acceptance test matrix and selected screenshots.

## 21. Individual Contribution

Each group member must have an honest, concrete contribution statement.

Do not invent equal contributions if actual contributions differ.

## 22. Demonstration Flow

Use the final demo script in this specification.

## 23. Conclusion

Summarize database design, implementation, and demonstrated functionality.

## Appendix A — Codd's Rules

Include only when required/expected by the instructor or report template.

## Appendix B — SQL Scripts / Important Code

Include selected code only; avoid dumping the entire source code into the report.

---

# 38. DEMONSTRATION SCRIPT

The live demo should follow this exact order because it maps cleanly to the rubric.

## Demo Part 1 — Planning / Requirements

30–60 seconds:

- state problem;
- state objective;
- show architecture;
- state functional requirements.

## Demo Part 2 — Database Design

1–2 minutes:

- show ERD;
- explain entities;
- explain 1:N and 1:1 relationships;
- show relational schema;
- explain one normalization decision.

## Demo Part 3 — Application

3–5 minutes:

1. register a customer;
2. login;
3. search for station;
4. select station;
5. select a free slot/time;
6. book;
7. show mock payment;
8. show booking history;
9. attempt overlapping booking and show rejection;
10. cancel a future booking and show refunded mock payment.

## Demo Part 4 — Database Features

1–2 minutes:

- execute availability query;
- execute revenue query;
- call stored function;
- call stored procedure;
- demonstrate trigger;
- explain transaction/`FOR UPDATE`.

## Demo Part 5 — Admin

1 minute:

- login as admin;
- modify station/slot status;
- demonstrate that unavailable station/slot cannot be booked;
- show report.

Keep a pre-seeded database backup so a failed demo does not require rebuilding everything live.

---

# 39. VIVA CORE QUESTIONS TO PREPARE

The implementation must be explainable for these questions:

1. Why did you separate station and charging slot?
2. Why is booking a separate table?
3. Why is payment separate from booking?
4. What are the cardinalities?
5. What are the functional dependencies?
6. Why is the schema in 3NF?
7. What insertion/update/deletion anomalies were avoided?
8. Why do you store `rate_per_hour` in booking?
9. How is slot availability calculated?
10. What is the overlap condition?
11. Why are adjacent bookings allowed?
12. Why is `FOR UPDATE` needed?
13. Why is a Java `synchronized` method not enough?
14. What happens when two users book simultaneously?
15. What happens if booking succeeds but payment insertion fails?
16. What does rollback do?
17. What does the unique constraint prevent?
18. Why does the unique constraint not fully solve interval overlap?
19. What is a stored function?
20. What is a stored procedure?
21. What is a trigger?
22. Why use a trigger here?
23. Why use `PreparedStatement`?
24. Why use PBKDF2?
25. Why use `BigDecimal` for money?
26. What is JDBC?
27. What does DAO mean?
28. What is the difference between controller/service/DAO?
29. Why use foreign keys?
30. What is ACID and how does booking use it?

The group should be able to answer without reading the code verbatim.

---

# 40. DEVELOPMENT PHASES

The coding agent must work in these phases and verify each phase before moving forward.

## Phase 0 — Repository bootstrap

Create:

- Maven project;
- `.gitignore`;
- README skeleton;
- package structure;
- configuration utility.

Verification:

```text
mvn test
```

must compile.

## Phase 1 — Database

Create:

- schema;
- indexes;
- seed;
- reset script;
- function;
- procedure;
- trigger;
- query script.

Verification:

- every SQL script executes in order;
- all tables exist;
- all constraints exist;
- sample queries return expected data.

## Phase 2 — Models + DB connection + password hashing

Verification:

- DB connection test passes;
- password hash/verify tests pass.

## Phase 3 — User authentication

Implement registration/login.

Verification:

- registration;
- unique constraints;
- valid/invalid login;
- blocked user.

## Phase 4 — Station/slot browsing

Implement station search, details, and slots.

Verification:

- correct station data displayed;
- inactive stations filtered from customer browsing;
- slot status shown.

## Phase 5 — Booking engine

Implement availability, transaction, `FOR UPDATE`, overlap detection, insert booking, mock payment.

Verification:

- success;
- duplicate;
- overlap;
- adjacent;
- maintenance;
- outside hours;
- past time;
- rollback;
- concurrency race.

## Phase 6 — History/cancellation/completion

Verification:

- history;
- cancel future confirmed booking;
- trigger refunds payment;
- cancelled booking becomes available again;
- old bookings become completed through refresh.

## Phase 7 — Admin/reporting

Verification:

- station CRUD/status;
- slot CRUD/status;
- stored procedure from Java;
- report screens.

## Phase 8 — UI polish

Only after functionality is stable.

No large visual redesign before integration works.

## Phase 9 — Final verification

Run:

```text
mvn test
```

plus all manual acceptance tests.

Fix every failure before preparing the report.

## Phase 10 — Documentation/demo

Generate:

- final ER/EER diagrams;
- screenshots;
- test report;
- demo script;
- final report.

---

# 41. GIT CHECKPOINTS

Create commits after every stable phase.

Recommended commit names:

```text
chore: bootstrap maven project
feat: implement mysql schema and seed data
feat: add jdbc connection and models
feat: add registration and authentication
feat: add station and slot management
feat: implement transactional booking
feat: add booking history and cancellation
feat: add admin dashboard and reports
feat: add stored function procedure and trigger integration
test: complete acceptance and concurrency tests
docs: add erd report screenshots and demo guide
```

Do not make one giant commit containing the entire project.

---

# 42. AI AGENT WORKFLOW / CREDIT OPTIMIZATION

This section controls how Antigravity and Freebuff should be used.

## 42.1 Primary rule

Use **one primary coding agent at a time on the same working tree**.

Do not let Antigravity and Freebuff simultaneously modify the same files. This avoids conflicting edits and makes Git history understandable.

## 42.2 Antigravity — use for high-context, high-leverage work

Antigravity is the primary builder for this project because it is designed for multi-step development across the editor, terminal, browser, and artifacts, and its `/plan` flow is specifically intended to explore a non-trivial workspace and produce a reviewable implementation plan before code changes.

Use Antigravity for:

### A. One initial planning session

Start with `/plan` and attach:

1. the prior architecture response;
2. the prior implementation/dependency response;
3. this specification document;
4. the university guideline/report files.

Prompt:

```text
Read all attached context and this SDLC specification.
Do not write implementation code yet.
Use /plan mode to inspect the workspace, identify dependencies,
map the specification to a concrete implementation plan, and list
verification checkpoints. Do not change the database or source files.
The SDLC specification is the source of truth.
```

Review the plan once.

### B. Initial database implementation

Use one substantial Antigravity task to create the database scripts and Java DB foundation together.

### C. Booking/concurrency implementation

Use Antigravity for this because it requires understanding of schema, DAO, service logic, transaction boundaries, and tests at the same time.

### D. End-to-end integration

Use Antigravity after all modules exist to run/build/test the full application and fix cross-module problems.

### E. Final verification

Use Antigravity once for a full repository audit against this document:

```text
Do a specification-compliance audit.
Do not redesign the project.
Inspect source, SQL, tests, README and report-support files.
List every unmet requirement and then fix only confirmed gaps.
Run the complete build/tests after changes.
```

## 42.3 Freebuff — use for narrow, repetitive, inexpensive tasks

Freebuff is most useful after the architecture is already fixed.

Use it for:

- compile-error fixing;
- one-file bug fixes;
- code review;
- test generation for an existing method;
- SQL formatting/cleanup;
- repetitive getters/setters/model cleanup;
- README wording;
- checking obvious null/exception handling;
- reviewing a diff;
- small Swing layout corrections;
- writing/repairing individual unit tests.

A particularly good Freebuff task is:

```text
Read the existing project and this specification.
Do not redesign anything.
Review only the files changed in the last Git commit.
Find compile errors, obvious logic errors, violated requirements,
and missing tests. Make the smallest safe fixes possible.
Run tests and report exactly what changed.
```

This keeps Freebuff's smaller/specialized subagents focused instead of spending a large context on the whole project.

## 42.4 What NOT to spend agents on

Do not repeatedly ask an agent to:

- rewrite the whole project;
- regenerate already-working classes;
- redesign the UI before database correctness is stable;
- explain the same architecture again;
- refactor every file for style;
- add features not required by the rubric;
- replace JDBC with an ORM;
- make the project “production scale”;
- create cloud deployment;
- invent extra entities.

These tasks consume model budget without improving the assessed deliverable.

## 42.5 Suggested distribution

```text
ANTIGRAVITY
├── Session 1: /plan + workspace audit
├── Session 2: DB + Maven + base architecture
├── Session 3: authentication + station/slot modules
├── Session 4: booking transaction + concurrency + payment
├── Session 5: history + cancellation + admin
├── Session 6: complete integration + tests + bug fixing
└── Session 7: final audit + report-support artifacts

FREEBUFF
├── compile/test fixes between major sessions
├── code review of each completed phase
├── isolated UI fixes
├── isolated SQL/query fixes
├── test additions
└── final diff review
```

The exact number of sessions is flexible. The important principle is **few large Antigravity sessions for architectural work, many small Freebuff sessions for mechanical work**.

## 42.6 Protect the final agent budget

Before every expensive agent call, prepare one precise prompt containing:

```text
Goal
Relevant files
Exact requirements
What must not change
Verification command
Expected output
```

Do not make the agent rediscover the project from a vague sentence.

## 42.7 Daily Freebuff allowance strategy

Freebuff currently advertises a daily free allocation measured in “Freebucks,” with different model consumption rates; the allowance refills daily and does not carry over. Use the free allocation on targeted tasks rather than burning it on broad “build the entire app” requests.

A good daily pattern is:

```text
1 large review/fix task
+
2–5 small isolated tasks
```

instead of one giant task that repeatedly revisits the entire repository.

## 42.8 Do not assume Antigravity is unlimited

Treat Antigravity as the scarce/high-value resource even if the interface makes usage feel autonomous. Spend it when the task needs repository-wide reasoning, terminal verification, browser/UI testing, or coordinated multi-file changes.

---

# 43. RECOMMENDED START COMMANDS FOR ANTIGRAVITY

After placing this document in the repository, use this sequence.

## Command 1 — planning only

```text
/plan

Read:
- EV_Charging_Slot_Booking_SDLС_SPEC.md
- the two previously attached project-design responses
- the supplied university mini-project guideline
- the supplied example DBMS report

Inspect the current workspace.
Create a concrete implementation plan.
Do not modify files yet.
The specification is authoritative.
Identify any contradiction; otherwise do not ask unnecessary questions.
Include exact verification commands for every phase.
```

## Command 2 — execute the approved plan

```text
Proceed with the approved implementation plan.
Implement Phase 0 and Phase 1 only.
After implementation, run the database/build verification.
Do not move to later phases until Phase 1 is verified.
```

## Command 3 — continue phase-by-phase

```text
Continue with the next phase in the SDLC specification.
Inspect existing implementation first.
Make the smallest changes required by the specification.
Run the phase's verification tests before reporting completion.
Do not start unrelated features.
```

## Command 4 — final audit

```text
Perform a complete final audit against EV_Charging_Slot_Booking_SDLС_SPEC.md.
Check SQL, Java, UI, tests, transaction logic, stored function,
stored procedure, trigger, README, ER/EER artifacts, and report support.
Do not redesign working features.
For each unmet requirement, fix it and verify it.
Finish by running the full test/build commands and reporting the result.
```

---

# 44. FINAL PRE-DEMO CHECKLIST

The team should not consider the project finished until all are true:

```text
[ ] JDK 21 works
[ ] MySQL server works
[ ] Maven build works
[ ] database scripts execute cleanly
[ ] seed data exists
[ ] ER diagram final
[ ] EER diagram final
[ ] 3NF explanation complete
[ ] anomaly explanation complete
[ ] functional dependencies complete
[ ] registration works
[ ] login works
[ ] station search works
[ ] availability works
[ ] booking transaction works
[ ] FOR UPDATE exists in booking path
[ ] interval overlap protection works
[ ] mock payment works
[ ] booking history works
[ ] cancellation works
[ ] trigger refunds mock payment
[ ] completion synchronization works
[ ] admin station management works
[ ] admin slot management works
[ ] required complex queries work
[ ] stored function works
[ ] stored procedure works
[ ] trigger works
[ ] concurrency test works
[ ] acceptance tests executed
[ ] README works on clean setup
[ ] screenshots captured
[ ] demo script rehearsed
[ ] every member can explain their contribution
[ ] every member can explain normalization
[ ] every member can explain booking concurrency
```

---

# 45. IMPORTANT IMPLEMENTATION JUDGMENTS ALREADY RESOLVED

To prevent the coding agent from reopening design debates, the following decisions are final:

| Decision | Final choice |
|---|---|
| UI | Java Swing |
| Backend | Core Java |
| DB access | JDBC |
| DB | MySQL + InnoDB |
| ORM | None |
| Framework | No Spring Boot |
| Build | Maven |
| Java | 21 |
| MySQL Connector/J | 26.7.0 unless environment requires compatible older version |
| Password hashing | PBKDF2WithHmacSHA256 |
| Connection pool | None; use DriverManager |
| Booking duration | exactly 60 minutes |
| Booking start | hour-aligned, future only |
| Overnight station hours | not supported |
| Payment | mock only |
| Payment status after booking | PAID |
| Booking status after successful payment | CONFIRMED |
| Cancellation | future confirmed bookings only |
| Refund | simulated; payment becomes REFUNDED |
| Completion | synchronized on relevant reads/actions |
| Concurrency | InnoDB transaction + `SELECT ... FOR UPDATE` |
| Double-booking defense | lock + overlap check + active-start unique constraint |
| Geographic maps | not included |
| External APIs | none |
| Main academic target | DBMS rubric + Java/JDBC implementation |

---

# 46. END STATE

The finished repository should allow a new machine to go from:

```text
Fresh machine
   ↓
JDK + MySQL + Maven
   ↓
configure local DB credentials
   ↓
run SQL scripts
   ↓
mvn test
   ↓
launch Swing application
   ↓
register/login
   ↓
find station
   ↓
check availability
   ↓
book 1-hour slot
   ↓
mock payment
   ↓
view history
   ↓
cancel booking
   ↓
admin management/reports
   ↓
execute SQL function/procedure/trigger
   ↓
complete DBMS demonstration
```

That is the complete target. Any feature not required by this specification should be treated as out of scope until the assessed deliverable is finished and verified.
