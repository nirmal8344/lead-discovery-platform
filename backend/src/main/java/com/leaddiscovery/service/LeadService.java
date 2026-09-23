package com.leaddiscovery.service;

import com.leaddiscovery.dto.CategoryStatDto;

import com.leaddiscovery.dto.LeadDetailDto;
import com.leaddiscovery.dto.LeadFilterCriteria;
import com.leaddiscovery.dto.LeadListItemDto;
import com.leaddiscovery.entity.Organization;
import com.leaddiscovery.entity.ScrapingTask;
import com.leaddiscovery.exception.ResourceNotFoundException;
import com.leaddiscovery.repository.OrganizationRepository;
import com.leaddiscovery.repository.OrganizationSpecification;
import com.leaddiscovery.repository.ScrapingTaskRepository;
import com.leaddiscovery.security.SecurityUtils;
import com.leaddiscovery.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class LeadService {

    private final OrganizationRepository organizationRepository;
    private final ScrapingTaskRepository taskRepository;
    private final SecurityUtils securityUtils;

    public LeadService(OrganizationRepository organizationRepository,
                       ScrapingTaskRepository taskRepository,
                       SecurityUtils securityUtils) {
        this.organizationRepository = organizationRepository;
        this.taskRepository = taskRepository;
        this.securityUtils = securityUtils;
    }

    @Transactional(readOnly = true)
    public Page<LeadListItemDto> listLeads(LeadFilterCriteria criteria, Pageable pageable) {
        LeadFilterCriteria effectiveCriteria = criteria != null ? criteria : new LeadFilterCriteria();

        if (!securityUtils.isAdmin()) {
            UserPrincipal principal = securityUtils.getRequiredCurrentUserPrincipal();
            effectiveCriteria.setUserId(principal.getId());
        }

        return organizationRepository.findAll(OrganizationSpecification.withCriteria(effectiveCriteria), pageable)
                .map(LeadListItemDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public LeadDetailDto getLeadDetails(Long leadId) {
        Organization org = organizationRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id: " + leadId));

        validateLeadOwnership(org);
        return LeadDetailDto.fromEntity(org);
    }

    @Transactional
    public void deleteLead(Long leadId) {
        Organization org = organizationRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with id: " + leadId));

        validateLeadOwnership(org);

        // Explicitly clear source page references before cascade delete
        if (org.getEmailAddresses() != null) {
            org.getEmailAddresses().forEach(e -> e.setSourcePage(null));
        }
        if (org.getPhoneNumbers() != null) {
            org.getPhoneNumbers().forEach(p -> p.setSourcePage(null));
        }
        if (org.getSocialLinks() != null) {
            org.getSocialLinks().forEach(s -> s.setSourcePage(null));
        }
        if (org.getContacts() != null) {
            org.getContacts().forEach(c -> c.setSourcePage(null));
        }

        organizationRepository.delete(org);
        organizationRepository.flush();
    }

    @Transactional(readOnly = true)
    public Page<LeadListItemDto> listLeadsByTask(Long taskId, Pageable pageable) {
        ScrapingTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Scraping task not found with id: " + taskId));

        if (!securityUtils.isAdmin()) {
            UserPrincipal principal = securityUtils.getRequiredCurrentUserPrincipal();
            if (task.getCreatedBy() == null || !task.getCreatedBy().getId().equals(principal.getId())) {
                throw new AccessDeniedException("You do not have permission to access leads for this task");
            }
        }

        LeadFilterCriteria criteria = new LeadFilterCriteria();
        criteria.setTaskId(taskId);
        return organizationRepository.findAll(OrganizationSpecification.withCriteria(criteria), pageable)
                .map(LeadListItemDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<CategoryStatDto> getCategoryStats() {
        if (securityUtils.isAdmin()) {
            return organizationRepository.findAllCategoryStats();
        } else {
            UserPrincipal principal = securityUtils.getRequiredCurrentUserPrincipal();
            return organizationRepository.findCategoryStatsByUserId(principal.getId());
        }
    }

    public void validateLeadOwnership(Organization org) {
        if (securityUtils.isAdmin()) {
            return;
        }

        UserPrincipal principal = securityUtils.getRequiredCurrentUserPrincipal();
        if (org.getScrapingTask() != null &&
            org.getScrapingTask().getCreatedBy() != null &&
            !org.getScrapingTask().getCreatedBy().getId().equals(principal.getId())) {
            throw new AccessDeniedException("You do not have permission to access or modify this lead");
        }
    }
}

