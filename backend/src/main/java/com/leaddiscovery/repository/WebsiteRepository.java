package com.leaddiscovery.repository;

import com.leaddiscovery.entity.Website;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WebsiteRepository extends JpaRepository<Website, Long> {
    List<Website> findByOrganizationId(Long organizationId);
    Optional<Website> findByOrganizationIdAndNormalizedUrl(Long organizationId, String normalizedUrl);
}
