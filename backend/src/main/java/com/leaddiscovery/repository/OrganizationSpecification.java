package com.leaddiscovery.repository;

import com.leaddiscovery.dto.LeadFilterCriteria;
import com.leaddiscovery.entity.Organization;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class OrganizationSpecification {

    private OrganizationSpecification() {
    }

    public static Specification<Organization> withCriteria(LeadFilterCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria == null) {
                return cb.conjunction();
            }

            if (criteria.getUserId() != null) {
                Predicate userMatch = cb.equal(root.get("scrapingTask").get("createdBy").get("id"), criteria.getUserId());
                Predicate noTask = cb.isNull(root.get("scrapingTask"));
                Predicate noUser = cb.isNull(root.get("scrapingTask").get("createdBy"));
                predicates.add(cb.or(userMatch, noTask, noUser));
            }

            if (criteria.getTaskId() != null) {
                predicates.add(cb.equal(root.get("scrapingTask").get("id"), criteria.getTaskId()));
            }

            if (criteria.getCity() != null && !criteria.getCity().isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("city")), criteria.getCity().trim().toLowerCase()));
            }

            if (criteria.getCategory() != null && !criteria.getCategory().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("category")), "%" + criteria.getCategory().trim().toLowerCase() + "%"));
            }

            if (criteria.getVerificationStatus() != null) {
                predicates.add(cb.equal(root.get("verificationStatus"), criteria.getVerificationStatus()));
            }

            if (criteria.getSearch() != null && !criteria.getSearch().isBlank()) {
                String searchPattern = "%" + criteria.getSearch().trim().toLowerCase() + "%";
                Predicate searchPredicate = cb.or(
                        cb.like(cb.lower(root.get("businessName")), searchPattern),
                        cb.like(cb.lower(root.get("city")), searchPattern),
                        cb.like(cb.lower(root.get("category")), searchPattern)
                );
                predicates.add(searchPredicate);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
