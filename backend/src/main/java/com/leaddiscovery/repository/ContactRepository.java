package com.leaddiscovery.repository;

import com.leaddiscovery.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Long> {
    List<Contact> findByOrganizationId(Long organizationId);
}
