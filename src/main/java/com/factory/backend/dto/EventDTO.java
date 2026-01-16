package com.factory.backend.dto;

import java.time.Instant;

public record EventDTO(
        String eventId,
        Instant eventTime,
        Instant receivedTime,
        String machineId,
        String factoryId,
        String lineId,
        long durationMs,
        int defectCount) {
}
