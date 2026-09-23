package com.leaddiscovery.repository;

import com.leaddiscovery.entity.ScrapingTaskError;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ScrapingTaskErrorRepository extends JpaRepository<ScrapingTaskError, Long> {
    List<ScrapingTaskError> findByScrapingTaskIdOrderByCreatedAtDesc(Long taskId);
    long countByScrapingTaskId(Long taskId);
}
