package com.leaddiscovery.repository;

import com.leaddiscovery.entity.ScrapingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ScrapingLogRepository extends JpaRepository<ScrapingLog, Long> {
    List<ScrapingLog> findByScrapingTaskIdOrderByCreatedAtAsc(Long scrapingTaskId);
}
