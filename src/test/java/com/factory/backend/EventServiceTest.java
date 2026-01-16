package com.factory.backend;

import com.factory.backend.dto.EventDTO;
import com.factory.backend.repository.EventRepository;
import com.factory.backend.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class EventServiceTest {

    @Autowired
    private EventService service;

    @Autowired
    private EventRepository repo;

    private EventDTO sample(String id, int defect, Instant recv) {
        return new EventDTO(
                id,
                Instant.parse("2026-01-15T10:00:00Z"),
                recv,
                "M1", "F1", "L1", 1000, defect);
    }

    // 1️) identical duplicate -> deduped
    @Test
    void duplicateIsDeduped() {
        repo.deleteAll();
        EventDTO e = sample("E1", 1, Instant.now());
        service.ingest(e);
        service.ingest(e);
        assertEquals(1, repo.count());
    }

    // 2️) newer receivedTime -> update
    @Test
    void newerReceivedTimeUpdates() {
        repo.deleteAll();
        service.ingest(sample("E2", 1, Instant.parse("2026-01-15T10:00:05Z")));
        service.ingest(sample("E2", 2, Instant.parse("2026-01-15T10:00:10Z")));
        assertEquals(1, repo.count());
        assertEquals(2, repo.findByEventId("E2").get().getDefectCount());
    }

    // 3️) older receivedTime ignored
    @Test
    void olderReceivedTimeIgnored() {
        repo.deleteAll();
        service.ingest(sample("E3", 1, Instant.parse("2026-01-15T10:00:10Z")));
        service.ingest(sample("E3", 2, Instant.parse("2026-01-15T10:00:05Z")));
        assertEquals(1, repo.count());
        assertEquals(1, repo.findByEventId("E3").get().getDefectCount());
    }

    // 4️) invalid duration rejected
    @Test
    void invalidDurationRejected() {
        EventDTO e = new EventDTO("E4", Instant.now(), Instant.now(),
                "M1", "F1", "L1", -10, 1);
        assertThrows(RuntimeException.class, () -> service.ingest(e));
    }

    // 5️) future eventTime rejected
    @Test
    void futureEventRejected() {
        EventDTO e = new EventDTO("E5",
                Instant.now().plusSeconds(3600),
                Instant.now(), "M1", "F1", "L1", 1000, 1);
        assertThrows(RuntimeException.class, () -> service.ingest(e));
    }

    // 6️) defectCount -1 ignored in stats
    @Test
    void defectMinusOneIgnored() {
        repo.deleteAll();
        service.ingest(sample("E6", -1, Instant.now()));
        Long sum = repo.sumDefects("M1",
                Instant.now().minusSeconds(3600),
                Instant.now().plusSeconds(3600));
        assertNull(sum);
    }

    // 7️) start inclusive end exclusive
    @Test
    void startInclusiveEndExclusive() {
        repo.deleteAll();
        Instant start = Instant.parse("2026-01-15T10:00:00Z");
        Instant end = Instant.parse("2026-01-15T11:00:00Z");

        service.ingest(new EventDTO("E7", start, Instant.now(),
                "M1", "F1", "L1", 1000, 1));

        assertEquals(1, repo.countEvents("M1", start, end));
    }

    // 8️) concurrency safe
    @Test
    void concurrentIngestSafe() throws Exception {
        repo.deleteAll();
        ExecutorService pool = Executors.newFixedThreadPool(5);

        Callable<Void> task = () -> {
            service.ingest(sample("E8", 1, Instant.now()));
            return null;
        };

        pool.invokeAll(List.of(task, task, task, task, task));
        pool.shutdown();

        assertEquals(1, repo.count());
    }
}
