# Factory Backend

This project implements a Spring Boot backend that receives machine events from a factory, performs validation, deduplication and conditional updates, and exposes analytics APIs to monitor machine health and defect trends.

---

## 1. Architecture

The system follows a layered architecture:

Controller → Service → Repository → Database

- **Controller** handles HTTP requests.
- **Service** contains all business rules (validation, dedupe, update).
- **Repository** uses Spring Data JPA to query the database.
- **H2 In-Memory DB** stores events locally for fast execution.

---

## 2. Deduplication & Update Logic

Events are uniquely identified by `eventId`.

| Case | Behaviour |
|------|-----------|
| Same eventId + identical payload | Deduplicated (ignored) |
| Same eventId + different payload + newer receivedTime | Updated |
| Same eventId + different payload + older receivedTime | Ignored |

Only the record with the **newest receivedTime** is considered the “winning” record.

---

## 3. Validation Rules

An event is rejected if:

- `durationMs < 0` or `durationMs > 6 hours`
- `eventTime` is more than 15 minutes in the future

Special rule:

- `defectCount = -1` means unknown defect count.  
  The event is stored but ignored in defect calculations.

---

## 4. Thread Safety

Thread safety is ensured by:

- UNIQUE constraint on `eventId`
- `@Transactional` service layer
- Optimistic locking using `@Version`
- Parallel batch ingestion using `parallelStream()`

This guarantees correctness even under concurrent ingestion.

---

## 5. Data Model

Event table:

| Column |
|--------|
| id (PK) |
| eventId (UNIQUE) |
| eventTime |
| receivedTime |
| machineId |
| factoryId |
| lineId |
| durationMs |
| defectCount |
| version (@Version) |

---

## 6. Performance Strategy

- Batch processing using `parallelStream()`
- In-memory H2 database
- Optimistic locking avoids heavy synchronization
- Only required queries are executed

This processes 1000 events under 1 second.

---

## 7. APIs

### POST /events/batch

Ingests batch events.

curl.exe -X POST http://localhost:8081/events/batch -H "Content-Type: application/json" --data-binary "@events.json"

---

### GET /stats

/stats?machineId=M1&start=2026-01-15T09:00:00Z&end=2026-01-15T11:00:00Z

Returns:
- eventsCount
- defectsCount
- avgDefectRate
- status (Healthy / Warning)

---

### GET /stats/top-defect-lines

/stats/top-defect-lines?factoryId=F1&from=2026-01-15T00:00:00Z&to=2026-01-16T00:00:00Z&limit=5


---

## 8. Setup & Run

mvn clean install
mvn spring-boot:run

Server runs on:
http://localhost:8081


---

## 9. Tests

mvn test

Includes 8 mandatory test cases.

---

## 10. Improvements

- PostgreSQL persistence
- Monitoring dashboards
- Authentication & authorization