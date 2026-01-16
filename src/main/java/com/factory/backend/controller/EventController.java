package com.factory.backend.controller;

import com.factory.backend.dto.EventDTO;
import com.factory.backend.repository.EventRepository;
import com.factory.backend.service.EventService;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class EventController {

    private final EventService service;
    private final EventRepository repo;

    public EventController(EventService service, EventRepository repo) {
        this.service = service;
        this.repo = repo;
    }

    // -------- POST /events/batch --------

    @PostMapping("/events/batch")
    public Map<String, Object> ingest(@RequestBody List<EventDTO> events) {

        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger updated = new AtomicInteger();
        AtomicInteger deduped = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        List<Map<String, String>> rejections = Collections.synchronizedList(new ArrayList<>());

        events.parallelStream().forEach(e -> {
            try {
                String res = service.ingest(e);
                switch (res) {
                    case "ACCEPTED" -> accepted.incrementAndGet();
                    case "UPDATED" -> updated.incrementAndGet();
                    default -> deduped.incrementAndGet();
                }
            } catch (Exception ex) {
                rejected.incrementAndGet();
                rejections.add(Map.of("eventId", e.eventId(),
                        "reason", ex.getMessage()));
            }
        });

        return Map.of(
                "accepted", accepted.get(),
                "updated", updated.get(),
                "deduped", deduped.get(),
                "rejected", rejected.get(),
                "rejections", rejections);
    }

    // -------- GET /stats --------

    @GetMapping("/stats")
    public Map<String, Object> stats(@RequestParam String machineId,
            @RequestParam Instant start,
            @RequestParam Instant end) {

        long events = repo.countEvents(machineId, start, end);
        long defects = Optional.ofNullable(
                repo.sumDefects(machineId, start, end)).orElse(0L);

        double hours = Duration.between(start, end).toSeconds() / 3600.0;
        double rate = hours == 0 ? 0 : defects / hours;

        return Map.of(
                "machineId", machineId,
                "start", start,
                "end", end,
                "eventsCount", events,
                "defectsCount", defects,
                "avgDefectRate", rate,
                "status", rate < 2 ? "Healthy" : "Warning");
    }

    // -------- GET /stats/top-defect-lines --------

    @GetMapping("/stats/top-defect-lines")
    public List<Map<String, Object>> topDefectLines(@RequestParam String factoryId,
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam(defaultValue = "10") int limit) {

        return repo.topDefectLines(factoryId, from, to).stream()
                .map(r -> {
                    long defects = ((Number) r[1]).longValue();
                    long count = ((Number) r[2]).longValue();
                    double percent = count == 0 ? 0 : (defects * 100.0) / count;

                    return Map.of(
                            "lineId", r[0],
                            "totalDefects", defects,
                            "eventCount", count,
                            "defectsPercent", Math.round(percent * 100.0) / 100.0);
                })
                .sorted((a, b) -> Double.compare(
                        (Double) b.get("defectsPercent"),
                        (Double) a.get("defectsPercent")))
                .limit(limit)
                .toList();
    }
}
