package com.leaddiscovery.repository;

import com.leaddiscovery.entity.PhoneNumber;
import com.leaddiscovery.entity.enums.PhoneType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PhoneNumberRepository extends JpaRepository<PhoneNumber, Long> {
    List<PhoneNumber> findByOrganizationId(Long organizationId);
    Optional<PhoneNumber> findByOrganizationIdAndNormalizedValue(Long organizationId, String normalizedValue);
    Optional<PhoneNumber> findByOrganizationIdAndNormalizedValueAndPhoneType(
            Long organizationId, String normalizedValue, PhoneType phoneType);
}
