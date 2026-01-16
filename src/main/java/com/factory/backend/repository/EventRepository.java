package com.factory.backend.repository;

import com.factory.backend.model.EventEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.*;

public interface EventRepository extends JpaRepository<EventEntity, Long> {

    Optional<EventEntity> findByEventId(String eventId);

    @Query("""
                SELECT COUNT(e) FROM EventEntity e
                WHERE e.machineId = :machineId
                AND e.eventTime >= :start AND e.eventTime < :end
            """)
    long countEvents(@Param("machineId") String machineId,
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("""
                SELECT SUM(e.defectCount) FROM EventEntity e
                WHERE e.machineId = :machineId
                AND e.defectCount >= 0
                AND e.eventTime >= :start AND e.eventTime < :end
            """)
    Long sumDefects(@Param("machineId") String machineId,
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("""
              SELECT e.lineId, SUM(e.defectCount), COUNT(e)
              FROM EventEntity e
              WHERE e.factoryId = :factoryId
              AND e.defectCount >= 0
              AND e.eventTime >= :from AND e.eventTime < :to
              GROUP BY e.lineId
            """)
    List<Object[]> topDefectLines(@Param("factoryId") String factoryId,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
