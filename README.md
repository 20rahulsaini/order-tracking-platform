# Real-Time Order &amp; Tracking Platform

A small, production-style order-tracking system built with Java 17, Spring Boot 3,
Apache Kafka, Redis, MySQL, React, and Docker Compose. The project is intentionally
small — three backend services, one frontend, one Kafka topic, two MySQL databases,
one Redis cache. No Kubernetes, no service discovery, no OAuth, no Elasticsearch.

> The goal is a **small, working, interview-defensible project**, not a huge enterprise system.

---

## Table of Contents

1. [Architecture](#1-architecture)
2. [Service Responsibilities](#2-service-responsibilities)
3. [Kafka Flow](#3-kafka-flow)
4. [Database Schema](#4-database-schema)
5. [Redis Usage](#5-redis-usage)
6. [Idempotency Explanation](#6-idempotency-explanation)
7. [DLQ Explanation](#7-dlq-explanation)
8. [API Documentation](#8-api-documentation)
9. [Local Setup Instructions](#9-local-setup-instructions)
10. [Example End-to-End Flow](#10-example-end-to-end-flow)
11. [Testing](#11-testing)

---

## 1. Architecture

```
                  +---------------------+
                  |    React UI         |   (port 8080)
                  |  Vite + nginx       |
                  +----------+----------+
                             |
                     /api/orders, /api/tracking, /api/delivery
                             |
        +--------------------+--------------------+--------------------+
        |                    |                    |
        v                    v                    v
+---------------+    +-----------------+   +------------------+
| Order Service |    | Tracking Service|   | Delivery Service |
| :8081         |    | :8082           |   | :8083            |
| MySQL         |    | MySQL + Redis   |   | (no DB)          |
+-------+-------+    +--------+--------+   +--------+---------+
        |                      ^                     |
        | produces              | consumes           | produces
        | order-events          | order-events       | order-events
        v                      |                     v
       +--------------------------------------+
       |     Apache Kafka (KRaft, :9092)      |
       |     topic: order-events             |
       |     topic: order-events.DLT         |
       +--------------------------------------+
```

### Components

| Service | Port | Stack |
|-----------|------|-------|
| Order Service | 8081 | Spring Boot 3, JPA/Hibernate, MySQL, Kafka producer, OpenAPI 3 / Swagger UI |
| Tracking Service | 8082 | Spring Boot 3, JPA/Hibernate, MySQL, Redis, Kafka consumer + DLQ producer, OpenAPI 3 / Swagger UI |
| Delivery Service | 8083 | Spring Boot 3, Kafka producer (no DB, simulation only), OpenAPI 3 / Swagger UI |
| Frontend | 8080 | React 18 + Vite, nginx reverse proxy |
| MySQL | 3306 | MySQL 8.0, two databases: `order_db`, `tracking_db` |
| Redis | 6379 | Redis 7, cache for tracking snapshots |
| Kafka | 9092 | Apache Kafka 3.7 in KRaft mode (no Zookeeper) |

### Swagger UI (OpenAPI 3 documentation)

Each service auto-publishes interactive API docs at runtime:

| Service | Swagger UI URL | OpenAPI JSON |
|---------|----------------|--------------|
| Order Service | http://localhost:8081/swagger-ui.html | http://localhost:8081/v3/api-docs |
| Tracking Service | http://localhost:8082/swagger-ui.html | http://localhost:8082/v3/api-docs |
| Delivery Service | http://localhost:8083/swagger-ui.html | http://localhost:8083/v3/api-docs |

OpenAPI annotations (`@Operation`, `@ApiResponse`, `@Tag`, `@Parameter`, `@Schema`)
live on the `*Rest` interfaces (e.g. `OrderRest`, `TrackingRest`, `DeliveryRest`)
so the documentation is co-located with the API contract — the implementation
classes stay clean.

### Why three services instead of one?

Each service has a single, well-bounded responsibility:

- **Order Service** owns order creation and the canonical `orders` table. It does
  not need to know whether the order is currently SHIPPED or DELIVERED — that's
  the Tracking Service's problem.
- **Tracking Service** owns the order lifecycle. It is the single writer to the
  `order_status_history` and `processed_events` tables. Because nothing else
  writes to those tables, the state-machine invariants are easy to enforce.
- **Delivery Service** is the placeholder for an external courier/warehouse
  webhook. It produces Kafka events but stores nothing locally. This
  demonstrates that *any* service can emit lifecycle events without coupling
  to the Tracking Service.

### What is *deliberately* not in this project

To keep it interview-defensible rather than enterprise-bloated, the project
omits: Kubernetes, service discovery (Eureka/Consul), config server, OAuth/OIDC,
API gateways (Spring Cloud Gateway), Elasticsearch, MongoDB, WebSockets,
distributed tracing (Tempo/Jaeger — though you'd add it next), Kubernetes
operators, and the Spring Cloud ecosystem in general.

---

## 2. Service Responsibilities

### Order Service (`com.example.orderservice`)

| Responsibility | Implementation |
|----------------|----------------|
| Create order | `POST /api/orders` → persists to `orders`, then publishes `ORDER_PLACED` event to Kafka |
| Get order | `GET /api/orders/{orderId}` → reads from MySQL |
| Publish lifecycle events to Kafka | `OrderEventProducer#publish` (synchronous send on the create path so the REST call fails fast on broker errors) |
| Store orders in MySQL | JPA entity `Order` in database `order_db` |

### Tracking Service (`com.example.trackingservice`)

| Responsibility | Implementation |
|----------------|----------------|
| Consume Kafka order events | `OrderEventConsumer` (`@KafkaListener` on `order-events`) |
| Validate status transitions | `OrderStatusStateMachine` (pure, fully unit-tested) |
| Maintain status/history in MySQL | `OrderStatusHistory` JPA entity (append-only) |
| Idempotent event processing | `ProcessedEvent` table + unique constraint on `event_id` |
| Retry + Dead Letter Topic | `DefaultErrorHandler` + `DeadLetterPublishingRecoverer` (3 retries, 1s backoff, then DLT) |
| Cache current order state in Redis | `OrderStateCache` (cache-aside pattern, TTL 1h) |
| `GET /api/tracking/orders/{orderId}` | `TrackingRestImpl` returns current status + full timeline |

### Delivery Service (`com.example.deliveryservice`)

A tiny simulation-only service:

| Responsibility | Implementation |
|----------------|----------------|
| Simulate SHIPPED | `POST /api/delivery/{orderId}/ship` → publishes `ORDER_SHIPPED` event |
| Simulate OUT_FOR_DELIVERY | `POST /api/delivery/{orderId}/out-for-delivery` → publishes `ORDER_OUT_FOR_DELIVERY` |
| Simulate DELIVERED | `POST /api/delivery/{orderId}/deliver` → publishes `ORDER_DELIVERED` |

> The Delivery Service has **no database** on purpose — its only job is to
> demonstrate that events from an independent producer flow through the
> same Kafka pipeline and land in the Tracking Service's history.

---

## 3. Kafka Flow

### Topic layout

| Topic | Key | Value | Notes |
|-------|-----|-------|-------|
| `order-events` | `orderId` | JSON `OrderEvent` | main lifecycle stream |
| `order-events.DLT` | `orderId` | JSON `OrderEvent` | dead letter topic, auto-created on first failed record |

### Event payload

```json
{
  "eventId":   "EVT-9b3f1c2a-...",
  "orderId":   "ORD-ABCD1234",
  "eventType": "ORDER_SHIPPED",
  "status":    "SHIPPED",
  "timestamp": "2024-09-15T10:23:45.123Z"
}
```

### Why `orderId` as the message key?

Using `orderId` as the Kafka record key guarantees that **every event for a
given order lands on the same partition**. Because Kafka preserves order
within a single partition, the Tracking Service observes events for any given
order in the exact order the producers emitted them. Without this, two events
in flight could be consumed out of order, and the state machine would reject
perfectly valid transitions.

### Producer configuration (Order Service &amp; Delivery Service)

| Property | Value | Why |
|----------|-------|-----|
| `acks` | `all` | Wait for all in-sync replicas; safe default for a single-broker setup |
| `enable.idempotence` | `true` | Producer-level sequence numbers protect against "send, retry, duplicate" on the wire |
| `retries` | `3` | Retry transient broker errors |
| `key.serializer` | `StringSerializer` | orderId is a string |
| `value.serializer` | `JsonSerializer` | POJO → JSON without manual ObjectMapper calls |

> **Important:** producer-level idempotence does *not* make the Tracking
> Service's business logic idempotent — that's the job of the
> `processed_events` table (see §6).

### Consumer configuration (Tracking Service)

| Property | Value | Why |
|----------|-------|-----|
| `group.id` | `tracking-service-group` | Persistent consumer group; survive consumer restarts |
| `enable.auto.commit` | `false` | Manual commit-after-processing via `AckMode.RECORD` |
| `auto.offset.reset` | `earliest` | Re-process backlog if offsets are missing |
| `value.deserializer` | `ErrorHandlingDeserializer` wrapping `JsonDeserializer` | A malformed payload lands in DLQ instead of crashing the listener loop |
| `default.deserialization.exception` | → DLQ | via `DeadLetterPublishingRecoverer` |

### Retry &amp; DLQ error handler

```java
DefaultErrorHandler errorHandler = new DefaultErrorHandler(
    new DeadLetterPublishingRecoverer(dlqKafkaTemplate),
    new FixedBackOff(1000, 2)         // 1s × 2 retries = 3 attempts total
);
errorHandler.addNotRetryableExceptions(
    InvalidStatusTransitionException.class,  // will keep failing forever
    IllegalArgumentException.class            // malformed event payload
);
```

Records that fail with a **retryable** exception (transient DB outage,
Redis timeout, etc.) are retried up to 3 times. Records that fail with a
**non-retryable** exception (invalid status transition, malformed payload)
are routed directly to `order-events.DLT` without retry.

---

## 4. Database Schema

The system uses two MySQL databases inside one MySQL instance. In production
these would likely be separate instances (or separate schemas in a shared
instance).

### `order_db.orders`

The canonical record of every order created by the Order Service.

| Column | Type | Notes |
|--------|------|-------|
| `id` | `BIGINT AUTO_INCREMENT` | surrogate primary key |
| `order_id` | `VARCHAR(32)` UNIQUE | business identifier, e.g. `ORD-ABCD1234` |
| `customer_name` | `VARCHAR(128)` | from the create request |
| `product` | `VARCHAR(128)` | from the create request |
| `quantity` | `INT` | from the create request |
| `total_amount` | `DECIMAL(12,2)` | from the create request |
| `status` | `VARCHAR(32)` | snapshot of order-creation-side status (starts at `PLACED`) |
| `created_at` | `DATETIME(6)` | |
| `updated_at` | `DATETIME(6)` | |

> The `status` column here is the Order Service's snapshot — it never changes
> after creation in this demo. The *authoritative* current status lives in the
> Tracking Service's `order_status_history` table.

### `tracking_db.order_status_history`

Append-only ledger of every status-change event the Tracking Service has
ever applied. One row per event.

| Column | Type | Notes |
|--------|------|-------|
| `id` | `BIGINT AUTO_INCREMENT` | surrogate primary key |
| `order_id` | `VARCHAR(32)` | FK-like reference; no actual FK constraint (separate DBs) |
| `status` | `VARCHAR(32)` | new status after this event |
| `event_type` | `VARCHAR(64)` | e.g. `ORDER_SHIPPED` |
| `event_id` | `VARCHAR(64)` UNIQUE | idempotency safety net (catches races) |
| `occurred_at` | `DATETIME(6)` | when the event was emitted (producer-supplied) |
| `recorded_at` | `DATETIME(6)` | when the Tracking Service wrote the row |

The latest row for an order (by `occurred_at DESC`) is the current status.

### `tracking_db.processed_events`

The idempotency ledger. One row per event that has been successfully
processed by the Tracking Service.

| Column | Type | Notes |
|--------|------|-------|
| `id` | `BIGINT AUTO_INCREMENT` | surrogate primary key |
| `event_id` | `VARCHAR(64)` UNIQUE | the unique event identifier |
| `order_id` | `VARCHAR(32)` | for diagnostic queries |
| `status` | `VARCHAR(32)` | new status after this event |
| `processed_at` | `DATETIME(6)` | |

### Schema creation

The schema is created by the MySQL container on first boot via
[`scripts/init-mysql.sql`](scripts/init-mysql.sql). The init script creates
both databases, both users, grants permissions, and applies the table DDL.

> To re-apply the schema (e.g. after a schema change), remove the
> `mysql_data` volume: `docker volume rm order-tracking-platform_mysql_data`.

---

## 5. Redis Usage

Redis is used **only** as a cache (not as a source of truth, not as a queue,
not as a lock provider). MySQL remains the source of truth at all times.

### Cache key &amp; value

```
KEY:   order:ORD-ABCD1234
VALUE: {
  "orderId":       "ORD-ABCD1234",
  "status":        "SHIPPED",
  "lastEventType": "ORDER_SHIPPED",
  "lastEventId":   "EVT-9b3f1c2a-...",
  "lastUpdated":   "2024-09-15T10:23:45.123Z"
}
TTL:   3600 seconds (1 hour)
```

### Cache-aside pattern

```
GET /api/tracking/orders/{orderId}
  |
  v
Redis GET order:ORD-... ?
  |
  +-- HIT  --> deserialize, return snapshot (no MySQL read for current status)
  |
  +-- MISS --> SELECT latest from order_status_history WHERE order_id = ?
              +-- found  --> update Redis (write-back), return snapshot
              +-- missing --> 404 Not Tracked
```

### Why cache-aside (and not write-through or write-back)?

- **Cache-aside** is the simplest pattern that meets the requirement. Writes
  happen *after* the MySQL transaction commits, so cache and DB are never
  inconsistent in a way that loses data.
- A cache miss is cheap (one MySQL read), and we refresh the cache on every
  status change. So the cache is warm for orders that are actively being
  tracked, and cold (and ignored) for orders that have finished.
- TTL of 1h bounds the cache size — old delivered orders eventually fall out
  and a subsequent read will repopulate from MySQL if needed.

### Failure mode

If Redis is down, `OrderStateCache` swallows the exception and returns `null`
on read or silently skips the write on write-back. The request then falls
through to MySQL, so the API continues to function (just slower). This is the
intended behavior — Redis is an optimization, not a hard dependency.

---

## 6. Idempotency Explanation

### The problem

Kafka provides **at-least-once** delivery (not exactly-once) in the default
configuration. Concretely, the Tracking Service consumer can see the same
event more than once in these scenarios:

1. The consumer processes an event, commits to MySQL, but crashes *before*
   committing the Kafka offset. On restart, it re-reads the same event.
2. A rebalance assigns a partition to a new consumer instance while the old
   instance is still mid-processing. Both instances may briefly process the
   same event.
3. The producer retries a send due to a transient network error. Even with
   producer-level idempotence enabled, the consumer may receive the same
   logical event from a different upstream path (e.g. a manual replay).

If the Tracking Service naively applied each event as it arrived, this would
lead to duplicate rows in `order_status_history`, double-counting in stats,
and spurious "invalid transition" errors when the same event is replayed
against an order whose state has already advanced.

### The solution

Every event carries an `eventId` (a UUID generated by the producer). The
Tracking Service uses this `eventId` as the idempotency key:

```java
@Transactional
public boolean applyEvent(OrderEvent event) {
    // 1. Idempotency fast-path: have we already processed this eventId?
    if (processedEventRepo.findByEventId(event.getEventId()).isPresent()) {
        return false;  // skip silently
    }

    // 2. Look up current status (Redis first, MySQL on miss)
    OrderStatus currentStatus = resolveCurrentStatus(event.getOrderId());
    OrderStatus targetStatus = parseStatus(event);

    // 3. Validate the transition
    if (currentStatus == null) {
        if (targetStatus != OrderStatus.PLACED) {
            throw new InvalidStatusTransitionException(...);
        }
    } else {
        OrderStatusStateMachine.validateTransition(currentStatus, targetStatus);
    }

    // 4. Insert history row (unique constraint on event_id catches races)
    try {
        historyRepo.saveAndFlush(history);
    } catch (DataIntegrityViolationException dup) {
        return false;  // another thread won the race; treat as idempotent skip
    }

    // 5. Insert processed_events row (unique constraint on event_id)
    try {
        processedEventRepo.saveAndFlush(ledger);
    } catch (DataIntegrityViolationException dup) {
        return false;
    }

    // 6. Refresh Redis cache
    cache.put(...);
    return true;
}
```

### Why two unique constraints?

The `processed_events.event_id` unique constraint is the primary idempotency
guarantee. But it's a *check-then-insert* pattern, which is not atomic in
Java — two threads can both pass the check and then race on the insert.
The unique constraint turns the second insert into a
`DataIntegrityViolationException`, which we catch and treat as a successful
idempotent skip.

The `order_status_history.event_id` unique constraint is a *defensive* second
layer. If somehow a row lands in history but not in processed_events (e.g. an
application crash between steps 4 and 5), the next attempt to insert the same
event into history will fail and we'll detect the duplicate. This is belt-and-
suspenders for the rare partial-failure case.

### Why not "exactly-once" Kafka?

True exactly-once delivery in Kafka requires:
- Transactional producer (`transactional.id` set)
- `isolation.level=read_committed` on the consumer
- The consumer's side effect (the MySQL write) must be **atomically tied to
  the Kafka offset commit** — typically via a system that writes both in the
  same transaction (e.g. Kafka Connect with JDBC sink, or a custom
  outbox pattern).

This is significantly more complex, fragile in operation, and most
importantly **unnecessary for this workload**. The idempotency ledger achieves
the actually-useful property: **idempotent side effects**. Whether the
Tracking Service sees an event once, twice, or five times, the resulting
state in MySQL is identical.

---

## 7. DLQ Explanation

### What is the DLQ?

The Dead Letter Topic (`order-events.DLT`) is a separate Kafka topic where the
Tracking Service sends records that could not be processed successfully after
exhausting retries.

### When does a record go to the DLQ?

Two paths:

1. **Retryable exception exhausted.** A transient failure (MySQL down,
   Redis timeout, transient deserialization issue) is retried up to 3 times
   with a 1-second backoff. If all 3 attempts fail, the record goes to the DLQ.

2. **Non-retryable exception.** Some exceptions will *always* fail no matter
   how many times we retry. For these, the error handler skips retries and
   routes the record directly to the DLQ. Currently classified as
   non-retryable:
   - `InvalidStatusTransitionException` — the event would violate the state
     machine (e.g. SHIPPED → PLACED, or skipping steps).
   - `IllegalArgumentException` — the event payload has an unknown status
     string that can't be parsed into `OrderStatus`.

### How is the DLQ implemented?

```java
DefaultErrorHandler errorHandler = new DefaultErrorHandler(
    new DeadLetterPublishingRecoverer(dlqKafkaTemplate),
    new FixedBackOff(1000L, 2L)   // 1s interval, 2 retries → 3 total attempts
);
errorHandler.addNotRetryableExceptions(
    InvalidStatusTransitionException.class,
    IllegalArgumentException.class
);
```

The `DeadLetterPublishingRecoverer` publishes the failed record to a topic
named `<original-topic>.DLT` (i.e. `order-events.DLT`), preserving the original
partition and key, and adding metadata headers about the failure reason
(`KAFKA_DLT_EXCEPTION_MESSAGE`, `KAFKA_DLT_EXCEPTION_FQCN`, etc.).

### How do you inspect the DLQ?

```bash
# from inside the Kafka container
docker exec -it otp-kafka /opt/bitnami/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic order-events.DLT \
  --from-beginning \
  --property print.headers=true
```

### How do you replay from the DLQ?

For this demo, replay is manual: read the failed record off the DLT, fix the
root cause (e.g. correct the upstream producer's status string), and re-publish
to `order-events` with a *new* `eventId` (so the idempotency ledger doesn't
skip it as a duplicate). In a production system you'd have an operator UI or
scheduled job for this.

---

## 8. API Documentation

### Order Service (`:8081`)

#### `POST /api/orders` — Create a new order

**Request body**
```json
{
  "customerName": "Alice",
  "product": "Mechanical Keyboard",
  "quantity": 2,
  "totalAmount": 199.98
}
```

**Validation**
- `customerName` — required, non-blank
- `product` — required, non-blank
- `quantity` — required, integer ≥ 1
- `totalAmount` — required, decimal > 0

**Response `201 Created`**
```json
{
  "orderId": "ORD-ABCD1234",
  "customerName": "Alice",
  "product": "Mechanical Keyboard",
  "quantity": 2,
  "totalAmount": 199.98,
  "status": "PLACED",
  "createdAt": "2024-09-15T10:23:45.123Z",
  "updatedAt": "2024-09-15T10:23:45.123Z"
}
```

**Errors**
- `400 Bad Request` — validation failure (e.g. missing field, negative quantity)
- `500 Internal Server Error` — Kafka unreachable (the create path fails fast
  rather than silently dropping the PLACED event)

#### `GET /api/orders/{orderId}` — Fetch an order

**Response `200 OK`**
```json
{
  "orderId": "ORD-ABCD1234",
  "customerName": "Alice",
  "product": "Mechanical Keyboard",
  "quantity": 2,
  "totalAmount": 199.98,
  "status": "PLACED",
  "createdAt": "2024-09-15T10:23:45.123Z",
  "updatedAt": "2024-09-15T10:23:45.123Z"
}
```

**Errors**
- `404 Not Found` — `{"error":"NOT_FOUND","message":"Order not found: ORD-XXXX"}`

---

### Tracking Service (`:8082`)

#### `GET /api/tracking/orders/{orderId}` — Get tracking snapshot

Returns the current status and the full timeline for an order. Backed by
the cache-aside pattern (Redis HIT → return; MISS → MySQL → write-back to Redis).

**Response `200 OK`**
```json
{
  "orderId": "ORD-ABCD1234",
  "currentStatus": "SHIPPED",
  "lastUpdated": "2024-09-15T10:25:00.000Z",
  "timeline": [
    {
      "status": "PLACED",
      "eventType": "ORDER_PLACED",
      "eventId": "EVT-aaa",
      "occurredAt": "2024-09-15T10:23:45.123Z",
      "recordedAt": "2024-09-15T10:23:45.200Z"
    },
    {
      "status": "CONFIRMED",
      "eventType": "ORDER_CONFIRMED",
      "eventId": "EVT-bbb",
      "occurredAt": "2024-09-15T10:24:00.000Z",
      "recordedAt": "2024-09-15T10:24:00.050Z"
    },
    {
      "status": "SHIPPED",
      "eventType": "ORDER_SHIPPED",
      "eventId": "EVT-ccc",
      "occurredAt": "2024-09-15T10:25:00.000Z",
      "recordedAt": "2024-09-15T10:25:00.080Z"
    }
  ]
}
```

**Errors**
- `404 Not Found` — `{"error":"NOT_TRACKED","message":"Order not tracked: ORD-XXXX"}`

---

### Delivery Service (`:8083`)

All endpoints return `200 OK` with a small JSON confirmation. They have no
side effects beyond publishing a Kafka event.

| Method + Path | Event published |
|---------------|-----------------|
| `POST /api/delivery/{orderId}/ship` | `ORDER_SHIPPED` / `SHIPPED` |
| `POST /api/delivery/{orderId}/out-for-delivery` | `ORDER_OUT_FOR_DELIVERY` / `OUT_FOR_DELIVERY` |
| `POST /api/delivery/{orderId}/deliver` | `ORDER_DELIVERED` / `DELIVERED` |

**Response shape**
```json
{
  "orderId": "ORD-ABCD1234",
  "status": "SHIPPED",
  "eventType": "ORDER_SHIPPED",
  "published": true
}
```

---

## 9. Local Setup Instructions

### Prerequisites

- Docker 24+ and Docker Compose v2+ (or `docker-compose` v2)
- ~4 GB of free RAM for the containers

### Option A: Full stack via Docker Compose (recommended)

```bash
git clone <your-repo> order-tracking-platform
cd order-tracking-platform

docker compose up --build
```

This brings up **all** services:

| Service | URL |
|---------|-----|
| Frontend (React + nginx) | http://localhost:8080 |
| Order Service | http://localhost:8081 |
| Tracking Service | http://localhost:8082 |
| Delivery Service | http://localhost:8083 |
| MySQL | localhost:3306 |
| Redis | localhost:6379 |
| Kafka | localhost:9092 |

Wait ~60 seconds for everything to come up (Maven downloads on first build,
then Kafka boots, then services wait for their dependencies via healthchecks).

Verify with:
```bash
# Order Service health
curl http://localhost:8081/actuator/health

# Tracking Service health
curl http://localhost:8082/actuator/health

# Kafka topics
docker exec otp-kafka /opt/bitnami/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 --list
```

### Option B: Run services locally for development

This is useful if you want hot reload on Java code.

#### Step 1: Start infrastructure

```bash
docker compose up -d mysql redis kafka
```

Wait for them to be healthy:
```bash
docker compose ps
```

#### Step 2: Run each service from your IDE or via Maven

```bash
# Terminal 1
cd order-service
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--MYSQL_HOST=localhost --MYSQL_PORT=3306 \
                                --MYSQL_USER=orderuser --MYSQL_PASSWORD=orderpass \
                                --MYSQL_DATABASE=order_db \
                                --KAFKA_BOOTSTRAP_SERVERS=localhost:9092"

# Terminal 2
cd tracking-service
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--MYSQL_HOST=localhost --MYSQL_PORT=3306 \
                                --MYSQL_USER=trackinguser --MYSQL_PASSWORD=trackingpass \
                                --MYSQL_DATABASE=tracking_db \
                                --REDIS_HOST=localhost --REDIS_PORT=6379 \
                                --KAFKA_BOOTSTRAP_SERVERS=localhost:9092"

# Terminal 3
cd delivery-service
mvn spring-boot:run \
  -Dspring-boot.run.arguments="--KAFKA_BOOTSTRAP_SERVERS=localhost:9092"
```

> If MySQL's `order_db` and `tracking_db` databases don't exist yet, create
> them with `scripts/init-mysql.sql`:
> ```bash
> docker exec -i otp-mysql mysql -uroot -prootpass < scripts/init-mysql.sql
> ```

#### Step 3: Run the frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend runs on http://localhost:3000 and proxies `/api/*` to the backend services via `vite.config.js`.

### Option C: Run the test suite

Tests run against in-memory H2 (no MySQL) and mocked Kafka (no broker).

```bash
mvn test
```

Or per module:
```bash
cd order-service      && mvn test
cd tracking-service   && mvn test
cd delivery-service   && mvn test
```

---

## 10. Example End-to-End Flow

This walkthrough demonstrates the full pipeline and verifies each step.

### Step 1: Create an order

```bash
curl -sX POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Alice",
    "product": "Mechanical Keyboard",
    "quantity": 2,
    "totalAmount": 199.98
  }' | jq
```

**Response** (status `PLACED`):
```json
{
  "orderId": "ORD-ABCD1234",
  "customerName": "Alice",
  "product": "Mechanical Keyboard",
  "quantity": 2,
  "totalAmount": 199.98,
  "status": "PLACED",
  "createdAt": "2024-09-15T10:23:45.123Z",
  "updatedAt": "2024-09-15T10:23:45.123Z"
}
```

Note the `orderId` for the next steps.

### Step 2: Verify the PLACED event reached the Tracking Service

```bash
curl -s http://localhost:8082/api/tracking/orders/ORD-ABCD1234 | jq
```

**Response**:
```json
{
  "orderId": "ORD-ABCD1234",
  "currentStatus": "PLACED",
  "lastUpdated": "2024-09-15T10:23:45.123Z",
  "timeline": [
    {
      "status": "PLACED",
      "eventType": "ORDER_PLACED",
      "eventId": "EVT-aaa-...",
      "occurredAt": "2024-09-15T10:23:45.123Z",
      "recordedAt": "2024-09-15T10:23:45.200Z"
    }
  ]
}
```

If you see a `404`, give the Tracking Service a second to consume the Kafka event.

### Step 3: Simulate CONFIRMED (you can do this manually, or skip)

For demo simplicity, the Order Service only publishes the initial `ORDER_PLACED`
event. The intermediate `CONFIRMED` and `PACKED` transitions can be triggered
the same way as the shipping transitions below — by publishing events to the
`order-events` topic. The React UI does not expose these buttons (they would
typically come from a warehouse management system), but the state machine
accepts them.

### Step 4: Advance the order through delivery

Use the Delivery Service endpoints. (The React UI exposes these buttons.)

```bash
# Confirm + Pack first (otherwise SHIPPED would be rejected by the state machine)
# Skip ahead — pretend the order is PACKED by emitting those events directly:

# Then:
curl -sX POST http://localhost:8083/api/delivery/ORD-ABCD1234/ship | jq
# -> {"orderId":"ORD-ABCD1234","status":"SHIPPED","eventType":"ORDER_SHIPPED","published":true}
```

Wait ~1 second for the Tracking Service to consume the event, then:

```bash
curl -sX POST http://localhost:8083/api/delivery/ORD-ABCD1234/out-for-delivery | jq
curl -s http://localhost:8082/api/tracking/orders/ORD-ABCD1234 | jq
```

```bash
curl -sX POST http://localhost:8083/api/delivery/ORD-ABCD1234/deliver | jq
curl -s http://localhost:8082/api/tracking/orders/ORD-ABCD1234 | jq
```

Final state:
```json
{
  "orderId": "ORD-ABCD1234",
  "currentStatus": "DELIVERED",
  "lastUpdated": "2024-09-15T10:30:00.000Z",
  "timeline": [ ... PLACED, CONFIRMED, PACKED, SHIPPED, OUT_FOR_DELIVERY, DELIVERED ... ]
}
```

### Step 5: Verify Redis has the cached state

```bash
docker exec otp-redis redis-cli GET order:ORD-ABCD1234
```

You should see the JSON snapshot. The next `GET /api/tracking/orders/...` call
will be served from Redis without hitting MySQL.

### Step 6: Verify DLQ behavior (negative test)

Trigger an invalid transition — try to ship an order that's already DELIVERED
(or just call `/ship` on a brand-new order that's still in PLACED):

```bash
curl -sX POST http://localhost:8083/api/delivery/ORD-ABCD1234/ship | jq
# 200 OK from delivery service (it just emits the event)

# Wait a moment, then check the DLQ:
docker exec otp-kafka /opt/bitnami/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic order-events.DLT \
  --from-beginning \
  --max-messages 1
```

The DLT message will include headers like:
```
KAFKA_DLT_EXCEPTION_FQCN: com.example.trackingservice.service.InvalidStatusTransitionException
KAFKA_DLT_EXCEPTION_MESSAGE: Invalid transition for order ORD-ABCD1234: DELIVERED -> SHIPPED
```

This is exactly what we want: invalid transitions are not retried forever,
they're routed to the DLT for manual inspection.

---

## 11. Testing

The project includes unit tests for every responsibility listed in the
requirements:

| Test | What it covers |
|------|----------------|
| `OrderServiceImplTest` | Order creation persists the right fields with status=PLACED; Kafka producer invoked once with the right shape; `getOrder` returns the row or throws on miss |
| `OrderEventProducerTest` | Kafka producer sends with topic + key + payload; failure propagates so the REST layer can fail fast |
| `OrderRestImplTest` | REST endpoints: 201 on create, 400 on validation failure, 404 on not-found |
| `OrderServiceApplicationTests` | Spring context loads with H2 instead of MySQL, Kafka producer mocked |
| `OrderStatusStateMachineTest` | Every transition (forward, backward, same-state, terminal) is validated correctly |
| `TrackingServiceImplTest` | **Idempotency** (duplicate event skipped), **invalid transition** (rejected, no DB writes), **duplicate caught by unique constraint** (race safety), **Redis cache HIT** (no MySQL read), **Redis cache MISS** (falls back, writes back), **404 for unknown order** |
| `OrderStateCacheTest` | Cache HIT, MISS, write, graceful degradation on Redis failure |
| `OrderEventConsumerTest` | Consumer delegates to `TrackingService.applyEvent`; on invalid transition re-throws so the DLQ handler routes the record to the DLT |
| `TrackingRestImplTest` | `GET /api/tracking/orders/{orderId}` returns current status + timeline; 404 on untracked order |
| `TrackingServiceApplicationTests` | Spring context loads with H2, Kafka consumer mocked |
| `DeliveryEventPublisherTest` | Publisher produces the right topic + key + payload shape |
| `DeliveryRestImplTest` | Each endpoint emits the correct event with the correct status |

### Run all tests

```bash
mvn test
```

### Test scope decisions

- **No live Kafka broker tests** by default. The Kafka consumer behavior is
  verified via unit tests on `TrackingService.applyEvent` (the listener's body)
  and on `OrderEventConsumer.onRecord` (which simply re-throws on invalid
  transitions so the configured error handler routes to the DLT). This keeps
  the test suite fast (under 10 seconds) and reliable across environments.
- **No live MySQL tests** by default. JPA entities are tested in unit tests
  via Mockito against the repository interfaces. The Spring context-load smoke
  tests use H2 in MySQL mode to verify wiring.
- **No live Redis tests** by default. `OrderStateCache` is tested via mocked
  `StringRedisTemplate`.

If you want to add full integration tests with `@EmbeddedKafka` and
Testcontainers, the natural place to add them is alongside the existing
unit tests in each module's `src/test/java` directory.

---

## Project Structure

```
order-tracking-platform/
├── pom.xml                          # Parent POM (multi-module)
├── docker-compose.yml               # Full stack: Kafka, MySQL, Redis, services, frontend
├── README.md                        # This file
├── scripts/
│   └── init-mysql.sql               # Schema bootstrap (runs on first boot)
├── order-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/example/orderservice/
│       │   ├── OrderServiceApplication.java
│       │   ├── controller/
│       │   │   ├── OrderRest.java          # interface (OpenAPI 3 docs)
│       │   │   └── OrderRestImpl.java       # @RestController impl
│       │   ├── service/
│       │   │   ├── OrderService.java        # interface
│       │   │   └── OrderServiceImpl.java    # @Service impl
│       │   ├── model/Order.java
│       │   ├── repository/OrderDao.java
│       │   ├── dto/{OrderRequest,OrderResponse,OrderEvent}.java
│       │   ├── kafka/OrderEventProducer.java
│       │   └── config/KafkaProducerConfig.java
│       ├── main/resources/
│       │   ├── application.yml
│       │   └── schema.sql
│       └── test/java/...            # OrderServiceImplTest, OrderRestImplTest, ...
├── tracking-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/example/trackingservice/
│       │   ├── TrackingServiceApplication.java
│       │   ├── controller/
│       │   │   ├── TrackingRest.java       # interface (OpenAPI 3 docs)
│       │   │   └── TrackingRestImpl.java    # @RestController impl
│       │   ├── service/
│       │   │   ├── TrackingService.java    # interface
│       │   │   ├── TrackingServiceImpl.java # @Service impl
│       │   │   ├── OrderStatusStateMachine.java
│       │   │   └── InvalidStatusTransitionException.java
│       │   ├── model/{OrderStatus,OrderStatusHistory,ProcessedEvent}.java
│       │   ├── repository/{OrderStatusHistoryDao,ProcessedEventDao}.java
│       │   ├── kafka/OrderEventConsumer.java
│       │   ├── redis/OrderStateCache.java
│       │   ├── config/KafkaConsumerConfig.java
│       │   └── dto/OrderEvent.java
│       ├── main/resources/
│       │   ├── application.yml
│       │   └── schema.sql
│       └── test/java/...            # TrackingServiceImplTest, TrackingRestImplTest, ...
├── delivery-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/example/deliveryservice/
│       │   ├── DeliveryServiceApplication.java
│       │   ├── controller/
│       │   │   ├── DeliveryRest.java        # interface (OpenAPI 3 docs)
│       │   │   └── DeliveryRestImpl.java    # @RestController impl
│       │   ├── service/
│       │   │   ├── DeliveryService.java    # interface
│       │   │   ├── DeliveryEventPublisherServiceImpl.java # @Service impl
│       │   │   └── DeliveryEventPublisherService.java
│       │   ├── dto/OrderEvent.java
│       │   └── kafka/KafkaProducerConfig.java
│       └── main/resources/application.yml
└── frontend/
    ├── package.json
    ├── vite.config.js
    ├── index.html
    ├── Dockerfile
    ├── nginx.conf
    └── src/
        ├── main.jsx
        ├── App.jsx
        ├── index.css
        ├── api/client.js
        └── components/
            ├── CreateOrderForm.jsx
            ├── OrderSearch.jsx
            ├── OrderDetail.jsx
            └── OrderStats.jsx
```

---

## What I'd add next (not in scope for the demo)

These are intentionally out of scope but worth flagging in an interview:

- **Distributed tracing** with OpenTelemetry + Tempo/Jaeger — `trace_id` flowing through the Kafka headers.
- **Schema registry** (Confluent or Apicurio) to evolve the `OrderEvent` contract safely.
- **Multiple Kafka partitions + parallel consumers** for higher throughput.
- **Outbox pattern** in the Order Service so that an order row + the PLACED event are written in the same DB transaction (eliminating the small window where the order exists but the event doesn't).
- **Spring Cloud Gateway** as a single external entry point with rate limiting.
- **OAuth 2.1 resource server** on each service.
- **Testcontainers** integration tests that spin up real Kafka + MySQL + Redis.
- **Prometheus + Grafana** for metrics (Spring Boot Actuator already exposes /actuator/prometheus).
