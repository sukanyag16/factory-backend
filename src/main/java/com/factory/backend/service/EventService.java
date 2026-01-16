package com.factory.backend.service;

import com.factory.backend.dto.EventDTO;
import com.factory.backend.model.EventEntity;
import com.factory.backend.repository.EventRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class EventService {

    private final EventRepository repo;

    public EventService(EventRepository repo) {
        this.repo = repo;
    }

    // ----- VALIDATION ------
    private void validate(EventDTO e) {
        if (e.durationMs() < 0 || e.durationMs() > 6 * 60 * 60 * 1000) {
            throw new RuntimeException("INVALID_DURATION");
        }

        if (e.eventTime().isAfter(Instant.now().plus(15, ChronoUnit.MINUTES))) {
            throw new RuntimeException("FUTURE_EVENT");
        }
    }

    // ------ INGEST LOGIC ------
    @Transactional
    public String ingest(EventDTO dto) {
        validate(dto);

        Optional<EventEntity> opt = repo.findByEventId(dto.eventId());
        Instant serverNow = Instant.now();

        if (opt.isEmpty()) {
            repo.save(map(dto, serverNow));
            return "ACCEPTED";
        }

        EventEntity existing = opt.get();

        if (samePayload(existing, dto)) {
            return "DEDUPED";
        }

        if (dto.receivedTime() != null &&
                dto.receivedTime().isAfter(existing.getReceivedTime())) {

            update(existing, dto, serverNow);
            return "UPDATED";
        }

        return "DEDUPED";
    }

    // ------ HELPERS -------
    private boolean samePayload(EventEntity e, EventDTO d) {
        return e.getEventTime().equals(d.eventTime())
                && e.getDurationMs() == d.durationMs()
                && e.getDefectCount() == d.defectCount();
    }

    private EventEntity map(EventDTO d, Instant now) {
        EventEntity e = new EventEntity();
        update(e, d, now);
        return e;
    }

    private void update(EventEntity e, EventDTO d, Instant now) {
        e.setEventId(d.eventId());
        e.setEventTime(d.eventTime());
        e.setReceivedTime(d.receivedTime() != null ? d.receivedTime() : now);

        e.setMachineId(d.machineId());
        e.setFactoryId(d.factoryId());
        e.setLineId(d.lineId());
        e.setDurationMs(d.durationMs());
        e.setDefectCount(d.defectCount());
        repo.save(e);
    }
}
