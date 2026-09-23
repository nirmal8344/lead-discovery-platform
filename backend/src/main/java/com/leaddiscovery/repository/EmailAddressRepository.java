package com.leaddiscovery.repository;

import com.leaddiscovery.entity.EmailAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EmailAddressRepository extends JpaRepository<EmailAddress, Long> {
    List<EmailAddress> findByOrganizationId(Long organizationId);
    Optional<EmailAddress> findByOrganizationIdAndNormalizedValue(Long organizationId, String normalizedValue);
}
