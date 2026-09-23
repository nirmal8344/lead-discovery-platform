package com.leaddiscovery.repository;

import com.leaddiscovery.entity.SourcePage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SourcePageRepository extends JpaRepository<SourcePage, Long> {
    List<SourcePage> findByWebsiteId(Long websiteId);
    Optional<SourcePage> findByWebsiteIdAndPageUrl(Long websiteId, String pageUrl);
}
