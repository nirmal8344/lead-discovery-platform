package com.leaddiscovery.service;

import com.leaddiscovery.discovery.UrlFilterUtils;
import com.leaddiscovery.dto.*;
import com.leaddiscovery.entity.*;
import com.leaddiscovery.entity.enums.*;
import com.leaddiscovery.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LeadDiscoveryPipelineService {

    private static final Logger log = LoggerFactory.getLogger(LeadDiscoveryPipelineService.class);

    /** Matches 5-6 digit postal/PIN/ZIP codes (US, India, EU, etc.) */
    private static final Pattern PINCODE_PATTERN = Pattern.compile("\\b(\\d{5,6})\\b");

    private final LeadDiscoveryService leadDiscoveryService;
    private final WebScraperService webScraperService;
    private final ConfidenceScoringService confidenceScoringService;

    private final ScrapingTaskRepository taskRepository;
    private final OrganizationRepository organizationRepository;
    private final WebsiteRepository websiteRepository;
    private final SourcePageRepository sourcePageRepository;
    private final EmailAddressRepository emailAddressRepository;
    private final PhoneNumberRepository phoneNumberRepository;
    private final SocialLinkRepository socialLinkRepository;
    private final ScrapingLogRepository logRepository;

    public LeadDiscoveryPipelineService(LeadDiscoveryService leadDiscoveryService,
                                        WebScraperService webScraperService,
                                        ConfidenceScoringService confidenceScoringService,
                                        ScrapingTaskRepository taskRepository,
                                        OrganizationRepository organizationRepository,
                                        WebsiteRepository websiteRepository,
                                        SourcePageRepository sourcePageRepository,
                                        EmailAddressRepository emailAddressRepository,
                                        PhoneNumberRepository phoneNumberRepository,
                                        SocialLinkRepository socialLinkRepository,
                                        ScrapingLogRepository logRepository) {
        this.leadDiscoveryService = leadDiscoveryService;
        this.webScraperService = webScraperService;
        this.confidenceScoringService = confidenceScoringService;
        this.taskRepository = taskRepository;
        this.organizationRepository = organizationRepository;
        this.websiteRepository = websiteRepository;
        this.sourcePageRepository = sourcePageRepository;
        this.emailAddressRepository = emailAddressRepository;
        this.phoneNumberRepository = phoneNumberRepository;
        this.socialLinkRepository = socialLinkRepository;
        this.logRepository = logRepository;
    }

    /**
     * Executes the end-to-end discovery, identity verification, crawling, extraction, scoring, and persistence pipeline.
     */
    public PipelineDiscoveryResponse executePipeline(PipelineDiscoveryRequest request) {
        String location = request.getLocation().trim();
        String keyword = request.getKeyword().trim();
        int maxResults = Math.max(1, Math.min(request.getMaxResults(), 100));
        int maxPagesPerSite = Math.max(1, Math.min(request.getMaxPagesPerSite(), 20));

        log.info("Executing Lead Discovery Pipeline: keyword='{}', location='{}', maxResults={}, maxPagesPerSite={}",
                keyword, location, maxResults, maxPagesPerSite);

        // 1. Create and save ScrapingTask record in PostgreSQL
        ScrapingTask task = new ScrapingTask();
        task.setLocation(location);
        task.setKeyword(keyword);
        task.setMaxResults(maxResults);
        task.setMaxPagesPerSite(maxPagesPerSite);
        task.setSearchRadiusKm(request.getSearchRadiusKm());
        task.setRequiredFields(request.getRequiredFields());
        task.setStatus(TaskStatus.RUNNING);
        task.setStartTime(LocalDateTime.now());
        task.setCurrentStage("DISCOVERING");
        task.setProgressPercentage(10);
        task = taskRepository.save(task);

        logScrapingEvent(task, "INFO", "Started lead discovery pipeline for '" + keyword + " in " + location + "'");

        List<LeadSummaryDto> savedLeads = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int totalDiscovered = 0;
        int totalCrawled = 0;
        int totalSaved = 0;
        int totalFailed = 0;

        try {
            // 2. Discover business candidates
            LeadDiscoveryRequest discoveryReq = new LeadDiscoveryRequest(location, keyword, maxResults, request.getSearchRadiusKm());
            LeadDiscoveryResponse discoveryResp = leadDiscoveryService.discoverLeads(discoveryReq);
            warnings.addAll(discoveryResp.getWarnings());

            List<DiscoveredBusinessDto> candidates = discoveryResp.getBusinesses();
            totalDiscovered = candidates.size();
            task.setDiscoveredBusinesses(totalDiscovered);
            task.setCurrentStage("VALIDATING_CANDIDATES");
            task.setProgressPercentage(25);
            task = taskRepository.save(task);

            logScrapingEvent(task, "INFO", "Discovered " + totalDiscovered + " candidate websites to validate");

            // 3. Process, verify identity, and crawl each candidate website
            int processedCount = 0;
            for (DiscoveredBusinessDto candidate : candidates) {
                processedCount++;
                String websiteUrl = candidate.getWebsiteUrl();
                String rawCandidateName = candidate.getBusinessName();

                try {
                    log.info("Processing candidate [{}/{}]: {} ({})", processedCount, totalDiscovered, rawCandidateName, websiteUrl);

                    task.setCurrentStage("CRAWLING_WEBSITES");
                    int progress = 25 + (int) (((double) processedCount / totalDiscovered) * 65);
                    task.setProgressPercentage(Math.min(90, progress));
                    taskRepository.save(task);

                    // Crawl multi-page website with targeted extraction and extract structured organization identity
                    int maxCrawlDepth = task.getMaxCrawlDepth() != null ? task.getMaxCrawlDepth() : 3;
                    CrawlWebsiteResponse crawlResponse = webScraperService.crawlWebsite(
                            websiteUrl, maxPagesPerSite, maxCrawlDepth, task.getRequiredFields()
                    );
                    totalCrawled++;

                    // Verify whether candidate identity was rejected (e.g. listicle, directory, aggregator)
                    OrganizationIdentity identity = crawlResponse.getOrganizationIdentity();
                    if (identity != null && identity.getIdentityStatus() == IdentityValidationResult.IDENTITY_REJECTED) {
                        log.warn("Skipping rejected candidate identity '{}' ({}): {}", rawCandidateName, websiteUrl, identity.getRejectionReason());
                        logScrapingEvent(task, "WARN", "Rejected candidate " + rawCandidateName + ": " + identity.getRejectionReason());
                        totalFailed++;
                        task.setFailedRecords(totalFailed);
                        continue;
                    }

                    // Normalize and persist lead data inside transaction
                    LeadSummaryDto savedLead = persistDiscoveredLead(task, candidate, crawlResponse);
                    if (savedLead != null) {
                        savedLeads.add(savedLead);
                        totalSaved++;
                        task.setLeadsSaved(totalSaved);
                    } else {
                        totalFailed++;
                        task.setFailedRecords(totalFailed);
                    }

                    task.setProcessedWebsites(totalCrawled);
                    taskRepository.save(task);

                } catch (Exception e) {
                    totalFailed++;
                    task.setFailedRecords(totalFailed);
                    log.error("Failed to crawl/persist candidate '{}' ({}): {}", rawCandidateName, websiteUrl, e.getMessage());
                    logScrapingEvent(task, "WARN", "Failed to process " + rawCandidateName + " (" + websiteUrl + "): " + e.getMessage());
                    warnings.add("Failed to crawl " + rawCandidateName + " (" + websiteUrl + "): " + e.getMessage());
                }
            }

            // 4. Finalize ScrapingTask
            task.setCurrentStage("SCORING");
            task.setProgressPercentage(95);
            taskRepository.save(task);

            if (totalSaved > 0) {
                task.setStatus(TaskStatus.COMPLETED);
                task.setCurrentStage("COMPLETED");
                logScrapingEvent(task, "INFO", "Pipeline completed: " + totalSaved + " verified leads saved, " + totalFailed + " failed/rejected");
            } else if (totalDiscovered == 0) {
                task.setStatus(TaskStatus.COMPLETED_WITH_NO_RESULTS);
                task.setCurrentStage("COMPLETED_WITH_NO_RESULTS");
                task.setErrorMessage("No candidate businesses found matching keyword '" + keyword + "' and location '" + location + "'.");
                logScrapingEvent(task, "WARN", "Pipeline completed with no candidate results discovered for '" + keyword + " in " + location + "'");
            } else {
                task.setStatus(TaskStatus.FAILED);
                task.setCurrentStage("FAILED");
                task.setErrorMessage("Discovered " + totalDiscovered + " candidates, but failed to extract verified organization identities.");
                logScrapingEvent(task, "ERROR", "Pipeline failed: " + totalFailed + " sites failed/rejected out of " + totalDiscovered + " candidates");
            }
            task.setEndTime(LocalDateTime.now());
            task.setProgressPercentage(100);
            taskRepository.save(task);

        } catch (Exception e) {
            log.error("Fatal error in discovery pipeline for task {}: {}", task.getId(), e.getMessage());
            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage(e.getMessage());
            task.setEndTime(LocalDateTime.now());
            taskRepository.save(task);
            logScrapingEvent(task, "ERROR", "Pipeline failed: " + e.getMessage());
            warnings.add("Pipeline execution error: " + e.getMessage());
        }

        return new PipelineDiscoveryResponse(
                task.getId(),
                location,
                keyword,
                totalDiscovered,
                totalCrawled,
                totalSaved,
                totalFailed,
                savedLeads,
                warnings,
                task.getStatus().name()
        );
    }

    /**
     * Normalizes and persists a single discovered lead and all its associated child entities.
     * Establishes authentic organization identity rather than persisting search result titles.
     */
    @Transactional
    public LeadSummaryDto persistDiscoveredLead(ScrapingTask task, DiscoveredBusinessDto candidate, CrawlWebsiteResponse crawl) {
        OrganizationIdentity identity = crawl.getOrganizationIdentity();

        // Reject if identity is explicitly flagged as invalid/listicle
        if (identity != null && identity.getIdentityStatus() == IdentityValidationResult.IDENTITY_REJECTED) {
            log.warn("Cannot persist rejected identity: {}", identity.getRejectionReason());
            return null;
        }

        // Establish the verified organization name:
        // Priority 1: Structured identity extracted from website (JSON-LD, OpenGraph, Logo, Footer, Clean Title)
        // Priority 2: Cleaned candidate title stripped of noise
        // Priority 3: Formatted brand name derived from official domain
        String verifiedBusinessName = null;
        if (identity != null && identity.getExtractedBusinessName() != null && !identity.getExtractedBusinessName().isBlank()) {
            verifiedBusinessName = identity.getExtractedBusinessName();
        } else if (candidate.getBusinessName() != null && !UrlFilterUtils.isGenericTitle(candidate.getBusinessName())) {
            verifiedBusinessName = UrlFilterUtils.cleanBrandFromTitle(candidate.getBusinessName(), crawl.getDomain());
        }

        if (verifiedBusinessName == null || verifiedBusinessName.isBlank() || UrlFilterUtils.isGenericTitle(verifiedBusinessName)) {
            verifiedBusinessName = UrlFilterUtils.formatDomainAsBrand(crawl.getDomain());
        }

        String normalizedName = DataNormalizationUtils.normalizeBusinessName(verifiedBusinessName);
        String city = task.getLocation();
        String category = task.getKeyword();

        log.info("[IDENTITY_EXTRACTED] Established verified business name '{}' (normalized: '{}') for domain '{}'",
                verifiedBusinessName, normalizedName, crawl.getDomain());

        // Normalize contacts, emails, phones, addresses, and social links
        Set<String> normalizedEmails = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (crawl.getAggregatedEmails() != null) {
            for (String rawEmail : crawl.getAggregatedEmails()) {
                String cleanEmail = DataNormalizationUtils.normalizeEmail(rawEmail);
                if (cleanEmail != null) {
                    normalizedEmails.add(cleanEmail);
                }
            }
        }

        Set<String> normalizedPhones = new LinkedHashSet<>();
        if (crawl.getAggregatedPhoneNumbers() != null) {
            for (String rawPhone : crawl.getAggregatedPhoneNumbers()) {
                String cleanPhone = DataNormalizationUtils.normalizePhoneNumber(rawPhone);
                if (cleanPhone != null) {
                    normalizedPhones.add(cleanPhone);
                }
            }
        }

        Set<String> normalizedAddresses = new LinkedHashSet<>();
        if (crawl.getAggregatedAddresses() != null) {
            for (String rawAddr : crawl.getAggregatedAddresses()) {
                String cleanAddr = DataNormalizationUtils.normalizeAddress(rawAddr);
                if (cleanAddr != null) {
                    normalizedAddresses.add(cleanAddr);
                }
            }
        }

        Map<String, String> socialLinks = crawl.getAggregatedSocialLinks() != null ?
                crawl.getAggregatedSocialLinks() : Collections.emptyMap();

        // Calculate transparent, explainable confidence score
        ConfidenceScoringService.ConfidenceResult confidence = confidenceScoringService.calculateConfidence(
                identity,
                crawl.getRootUrl(),
                crawl.getPages(),
                normalizedEmails,
                normalizedPhones,
                crawl.getAggregatedWhatsAppNumbers(),
                normalizedAddresses,
                socialLinks,
                task != null ? task.getRequiredFields() : null
        );

        String primaryAddress = normalizedAddresses.stream().findFirst().orElse(null);

        // Build combined missing fields and warnings note
        StringBuilder notesBuilder = new StringBuilder();
        if (confidence.getMissingFields() != null && !confidence.getMissingFields().isBlank()) {
            notesBuilder.append("Missing: ").append(confidence.getMissingFields());
        }
        if (confidence.getWarnings() != null && !confidence.getWarnings().isEmpty()) {
            if (notesBuilder.length() > 0) notesBuilder.append(" | ");
            notesBuilder.append(String.join("; ", confidence.getWarnings()));
        }
        String notes = notesBuilder.toString();

        // Check if organization already exists in this task to prevent duplicate inserts
        List<Organization> existingOrgs = organizationRepository.findByNormalizedNameAndScrapingTaskId(normalizedName, task.getId());
        Organization organization;
        if (!existingOrgs.isEmpty()) {
            organization = existingOrgs.get(0);
        } else {
            organization = new Organization();
            organization.setScrapingTask(task);
            organization.setBusinessName(verifiedBusinessName);
            organization.setNormalizedName(normalizedName);
            organization.setCategory(category);
            organization.setCity(city);
            organization.setAddress(primaryAddress);
            organization.setSourceUrl(candidate.getSourceUrl());
            organization.setConfidenceScore(confidence.getScore());
            organization.setVerificationStatus(confidence.getStatus());
            organization.setMissingFields(notes);
            organization.setProcessingStatus(ProcessingStatus.PROCESSED);
            organization.setScrapingTimestamp(LocalDateTime.now());

            // Extract pincode/ZIP from addresses
            String extractedPincode = extractPincodeFromAddresses(normalizedAddresses);
            if (extractedPincode != null) {
                organization.setPincode(extractedPincode);
                log.debug("[PINCODE] Extracted pincode '{}' for org '{}'", extractedPincode, verifiedBusinessName);
            }

            organization = organizationRepository.save(organization);
        }

        // Save Official Website Domain
        String normalizedRootUrl = crawl.getRootUrl();
        Optional<Website> existingWebsite = websiteRepository.findByOrganizationIdAndNormalizedUrl(organization.getId(), normalizedRootUrl);
        Website website;
        if (existingWebsite.isPresent()) {
            website = existingWebsite.get();
            website.setLastCrawledAt(LocalDateTime.now());
            website.setStatus(WebsiteStatus.CRAWLED);
            website = websiteRepository.save(website);
        } else {
            website = new Website();
            website.setOrganization(organization);
            website.setUrl(normalizedRootUrl);
            website.setNormalizedUrl(normalizedRootUrl);
            website.setIsOfficial(true);
            website.setStatus(WebsiteStatus.CRAWLED);
            website.setLastCrawledAt(LocalDateTime.now());
            website = websiteRepository.save(website);
        }

        // Save SourcePages
        Map<String, SourcePage> savedPagesByUrl = new HashMap<>();
        if (crawl.getPages() != null) {
            for (PageExtractDto pageDto : crawl.getPages()) {
                Optional<SourcePage> existingPage = sourcePageRepository.findByWebsiteIdAndPageUrl(website.getId(), pageDto.getUrl());
                SourcePage sourcePage;
                if (existingPage.isPresent()) {
                    sourcePage = existingPage.get();
                    sourcePage.setHttpStatus(pageDto.getHttpStatus());
                    sourcePage.setFetchedAt(pageDto.getFetchedAt());
                    sourcePage = sourcePageRepository.save(sourcePage);
                } else {
                    sourcePage = new SourcePage();
                    sourcePage.setWebsite(website);
                    sourcePage.setPageUrl(pageDto.getUrl());
                    sourcePage.setPageType(pageDto.getPageType() != null ? pageDto.getPageType() : SourcePageType.OTHER);
                    sourcePage.setHttpStatus(pageDto.getHttpStatus());
                    sourcePage.setFetchedAt(pageDto.getFetchedAt());
                    sourcePage = sourcePageRepository.save(sourcePage);
                }
                savedPagesByUrl.put(pageDto.getUrl(), sourcePage);
            }
        }

        String officialDomain = crawl.getDomain();

        // Save Email Addresses (deduplicated per organization, with full provenance)
        for (String email : normalizedEmails) {
            Optional<EmailAddress> existingEmail = emailAddressRepository.findByOrganizationIdAndNormalizedValue(organization.getId(), email);
            if (existingEmail.isEmpty()) {
                EmailAddress emailEntity = new EmailAddress();
                emailEntity.setOrganization(organization);
                emailEntity.setRawValue(email);
                emailEntity.setNormalizedValue(email);
                SourcePage sourcePage = findSourcePageForEmail(crawl.getPages(), email, savedPagesByUrl);
                emailEntity.setSourcePage(sourcePage);
                String sourceDomain = sourcePage != null ? UrlFilterUtils.extractDomain(sourcePage.getPageUrl()) : officialDomain;
                emailEntity.setSourceDomain(sourceDomain);
                ContactVerificationStatus verificationStatus = ConfidenceScoringService.evaluateEmailVerification(email, officialDomain);
                emailEntity.setVerificationStatus(verificationStatus);
                emailAddressRepository.save(emailEntity);
            }
        }

        // Save Phone Numbers (deduplicated per organization, with full provenance)
        for (String phone : normalizedPhones) {
            Optional<PhoneNumber> existingPhone = phoneNumberRepository.findByOrganizationIdAndNormalizedValue(organization.getId(), phone);
            if (existingPhone.isEmpty()) {
                PhoneNumber phoneEntity = new PhoneNumber();
                phoneEntity.setOrganization(organization);
                phoneEntity.setRawValue(phone);
                phoneEntity.setNormalizedValue(phone);
                phoneEntity.setPhoneType(PhoneType.PHONE);
                SourcePage sourcePage = findSourcePageForPhone(crawl.getPages(), phone, savedPagesByUrl);
                phoneEntity.setSourcePage(sourcePage);
                String sourceDomain = sourcePage != null ? UrlFilterUtils.extractDomain(sourcePage.getPageUrl()) : officialDomain;
                phoneEntity.setSourceDomain(sourceDomain);
                phoneEntity.setVerificationStatus(ContactVerificationStatus.VERIFIED);
                phoneNumberRepository.save(phoneEntity);
            }
        }

        // Save WhatsApp Numbers (phone_type = WHATSAPP, with provenance)
        if (crawl.getAggregatedWhatsAppNumbers() != null) {
            for (String rawWa : crawl.getAggregatedWhatsAppNumbers()) {
                String cleanWa = DataNormalizationUtils.normalizePhoneNumber(rawWa);
                if (cleanWa == null) continue;
                Optional<PhoneNumber> existingWa = phoneNumberRepository.findByOrganizationIdAndNormalizedValue(organization.getId(), cleanWa);
                if (existingWa.isEmpty()) {
                    PhoneNumber waEntity = new PhoneNumber();
                    waEntity.setOrganization(organization);
                    waEntity.setRawValue(rawWa);
                    waEntity.setNormalizedValue(cleanWa);
                    waEntity.setPhoneType(PhoneType.WHATSAPP);
                    waEntity.setSourceDomain(officialDomain);
                    waEntity.setVerificationStatus(ContactVerificationStatus.VERIFIED);
                    phoneNumberRepository.save(waEntity);
                    log.debug("[WHATSAPP] Saved WhatsApp number {} for org '{}'", cleanWa, verifiedBusinessName);
                }
            }
        }


        // Save Social Links (deduplicated per organization, with full provenance)
        for (Map.Entry<String, String> entry : socialLinks.entrySet()) {
            String platform = entry.getKey();
            String socialUrl = entry.getValue();
            Optional<SocialLink> existingSocial = socialLinkRepository.findByOrganizationIdAndPlatform(organization.getId(), platform);
            if (existingSocial.isEmpty()) {
                SocialLink socialEntity = new SocialLink();
                socialEntity.setOrganization(organization);
                socialEntity.setPlatform(platform);
                socialEntity.setUrl(socialUrl);
                SourcePage sourcePage = findSourcePageForSocial(crawl.getPages(), socialUrl, savedPagesByUrl);
                socialEntity.setSourcePage(sourcePage != null ? sourcePage : savedPagesByUrl.get(crawl.getRootUrl()));
                socialEntity.setSourceDomain(officialDomain);
                socialEntity.setVerificationStatus(ContactVerificationStatus.VERIFIED);
                socialLinkRepository.save(socialEntity);
            }
        }

        return new LeadSummaryDto(
                organization.getId(),
                verifiedBusinessName,
                normalizedRootUrl,
                city,
                category,
                normalizedEmails,
                normalizedPhones,
                socialLinks,
                primaryAddress,
                confidence.getScore(),
                confidence.getStatus().name(),
                crawl.getPagesCrawled()
        );
    }

    private void logScrapingEvent(ScrapingTask task, String level, String message) {
        try {
            ScrapingLog entry = new ScrapingLog();
            entry.setScrapingTask(task);
            entry.setLevel(level);
            entry.setMessage(message);
            entry.setContext("LeadDiscoveryPipelineService");
            logRepository.save(entry);
        } catch (Exception ignored) {
        }
    }

    private SourcePage findSourcePageForEmail(List<PageExtractDto> pages, String email, Map<String, SourcePage> savedPages) {
        if (pages == null || email == null) return null;
        for (PageExtractDto page : pages) {
            if (page.getEmails() != null && page.getEmails().stream().anyMatch(e -> e.equalsIgnoreCase(email))) {
                return savedPages.get(page.getUrl());
            }
        }
        return null;
    }

    private SourcePage findSourcePageForPhone(List<PageExtractDto> pages, String phone, Map<String, SourcePage> savedPages) {
        if (pages == null || phone == null) return null;
        for (PageExtractDto page : pages) {
            if (page.getPhoneNumbers() != null && page.getPhoneNumbers().contains(phone)) {
                return savedPages.get(page.getUrl());
            }
        }
        return null;
    }

    private SourcePage findSourcePageForSocial(List<PageExtractDto> pages, String socialUrl, Map<String, SourcePage> savedPages) {
        if (pages == null || socialUrl == null) return null;
        for (PageExtractDto page : pages) {
            if (page.getSocialLinks() != null && page.getSocialLinks().containsValue(socialUrl)) {
                return savedPages.get(page.getUrl());
            }
        }
        return null;
    }

    /**
     * Extracts a pincode/ZIP code (5-6 digits) from a set of address strings.
     * Returns the first match found, or null if none.
     */
    private String extractPincodeFromAddresses(Set<String> addresses) {
        if (addresses == null || addresses.isEmpty()) return null;
        for (String address : addresses) {
            Matcher matcher = PINCODE_PATTERN.matcher(address);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }
}
