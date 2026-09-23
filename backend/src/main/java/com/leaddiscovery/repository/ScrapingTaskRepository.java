package com.leaddiscovery.repository;

import com.leaddiscovery.entity.ScrapingTask;
import com.leaddiscovery.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScrapingTaskRepository extends JpaRepository<ScrapingTask, Long> {
    Page<ScrapingTask> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<ScrapingTask> findByCreatedByOrderByCreatedAtDesc(User createdBy, Pageable pageable);
    Page<ScrapingTask> findByCreatedByIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
