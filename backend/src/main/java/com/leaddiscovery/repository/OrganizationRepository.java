package com.leaddiscovery.repository;

import com.leaddiscovery.dto.CategoryStatDto;
import com.leaddiscovery.entity.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long>, JpaSpecificationExecutor<Organization> {
    Page<Organization> findByScrapingTaskId(Long scrapingTaskId, Pageable pageable);
    List<Organization> findByNormalizedNameAndScrapingTaskId(String normalizedName, Long scrapingTaskId);
    Optional<Organization> findByNormalizedNameAndCityIgnoreCase(String normalizedName, String city);
    Page<Organization> findAll(Pageable pageable);

    @Query("SELECT new com.leaddiscovery.dto.CategoryStatDto(o.category, COUNT(o)) " +
           "FROM Organization o " +
           "WHERE o.category IS NOT NULL AND TRIM(o.category) <> '' " +
           "GROUP BY o.category " +
           "ORDER BY COUNT(o) DESC, o.category ASC")
    List<CategoryStatDto> findAllCategoryStats();

    @Query("SELECT new com.leaddiscovery.dto.CategoryStatDto(o.category, COUNT(o)) " +
           "FROM Organization o " +
           "WHERE o.scrapingTask.createdBy.id = :userId AND o.category IS NOT NULL AND TRIM(o.category) <> '' " +
           "GROUP BY o.category " +
           "ORDER BY COUNT(o) DESC, o.category ASC")
    List<CategoryStatDto> findCategoryStatsByUserId(@Param("userId") Long userId);
}

