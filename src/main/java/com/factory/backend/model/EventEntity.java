package com.factory.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "events", uniqueConstraints = @UniqueConstraint(columnNames = "eventId"))
public class EventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String eventId;
    private Instant eventTime;
    private Instant receivedTime;
    private String machineId;
    private String factoryId;
    private String lineId;
    private long durationMs;
    private int defectCount;

    @Version
    private long version;

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public Instant getEventTime() {
        return eventTime;
    }

    public Instant getReceivedTime() {
        return receivedTime;
    }

    public String getMachineId() {
        return machineId;
    }

    public String getFactoryId() {
        return factoryId;
    }

    public String getLineId() {
        return lineId;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public int getDefectCount() {
        return defectCount;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public void setEventTime(Instant eventTime) {
        this.eventTime = eventTime;
    }

    public void setReceivedTime(Instant receivedTime) {
        this.receivedTime = receivedTime;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public void setFactoryId(String factoryId) {
        this.factoryId = factoryId;
    }

    public void setLineId(String lineId) {
        this.lineId = lineId;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public void setDefectCount(int defectCount) {
        this.defectCount = defectCount;
    }
}
