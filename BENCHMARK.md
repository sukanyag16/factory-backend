System:
- CPU: Intel i7
- RAM: 32 GB
- OS: Windows 11
- Java: 21

Test:
curl.exe -X POST http://localhost:8081/events/batch --data-binary "@events.json"

Result:
Processed 1000 events in ~420 ms.

Optimizations:
- parallelStream() for batch ingest
- In-memory H2 DB
- Optimistic locking using @Version
- Transactional service layer
