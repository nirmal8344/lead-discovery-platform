package com.leaddiscovery.repository;

import com.leaddiscovery.entity.SocialLink;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SocialLinkRepository extends JpaRepository<SocialLink, Long> {
    List<SocialLink> findByOrganizationId(Long organizationId);
    java.util.Optional<SocialLink> findByOrganizationIdAndPlatform(Long organizationId, String platform);
}
