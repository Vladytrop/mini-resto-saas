package com.resto.saas.rag.history;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RagQueryLogRepository extends JpaRepository<RagQueryLog, Long> {
    // Пока только save(). Примеры аналитических запросов для интервью:
    //   @Query("select count(l) from RagQueryLog l where l.escalated = true and l.createdAt > :since")
    //   long countEscalationsSince(Instant since);
}