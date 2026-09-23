package com.leaddiscovery.service;

import com.leaddiscovery.discovery.UrlFilterUtils;
import com.leaddiscovery.dto.OrganizationIdentity;
import com.leaddiscovery.dto.PageExtractDto;
import com.leaddiscovery.entity.enums.ContactVerificationStatus;
import com.leaddiscovery.entity.enums.IdentityValidationResult;
import com.leaddiscovery.entity.enums.SourcePageType;
import com.leaddiscovery.entity.enums.VerificationStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class ConfidenceScoringService {

    private static final Set<String> FREE_EMAIL_PROVIDERS = Set.of(
            "gmail.com", "yahoo.com", "hotmail.com", "outlook.com", "live.com",
            "icloud.com", "aol.com", "protonmail.com", "zoho.com", "mail.com",
            "yandex.com", "gmx.com", "rediffmail.com"
    );

    public static boolean isFreeEmailProvider(String domain) {
        if (domain == null || domain.isBlank()) return false;
        return FREE_EMAIL_PROVIDERS.contains(domain.toLowerCase(Locale.ROOT));
    }

    public static ContactVerificationStatus evaluateEmailVerification(String email, String officialDomain) {
        if (email == null || email.isBlank()) return ContactVerificationStatus.UNVERIFIED;
        int at = email.indexOf('@');
        if (at == -1) return ContactVerificationStatus.UNVERIFIED;

        String emailDomain = email.substring(at + 1).toLowerCase(Locale.ROOT).trim();
        if (officialDomain == null || officialDomain.isBlank()) {
            return isFreeEmailProvider(emailDomain) ? ContactVerificationStatus.UNVERIFIED : ContactVerificationStatus.EXTERNAL;
        }

        String cleanOfficial = officialDomain.toLowerCase(Locale.ROOT).trim();
        if (cleanOfficial.startsWith("www.")) {
            cleanOfficial = cleanOfficial.substring(4);
        }

        if (emailDomain.equals(cleanOfficial) || emailDomain.endsWith("." + cleanOfficial)) {
            return ContactVerificationStatus.VERIFIED;
        }

        if (isFreeEmailProvider(emailDomain)) {
            return ContactVerificationStatus.UNVERIFIED;
        }

        return ContactVerificationStatus.EXTERNAL;
    }

    public static class ConfidenceResult {
        private final BigDecimal score;
        private final VerificationStatus status;
        private final String missingFields;
        private final List<String> factors;
        private final List<String> warnings;

        public ConfidenceResult(BigDecimal score, VerificationStatus status, String missingFields,
                                List<String> factors, List<String> warnings) {
            this.score = score;
            this.status = status;
            this.missingFields = missingFields;
            this.factors = factors != null ? factors : new ArrayList<>();
            this.warnings = warnings != null ? warnings : new ArrayList<>();
        }

        public BigDecimal getScore() { return score; }
        public VerificationStatus getStatus() { return status; }
        public String getMissingFields() { return missingFields; }
        public List<String> getFactors() { return factors; }
        public List<String> getWarnings() { return warnings; }
    }

    /**
     * Safely normalizes user-provided required fields into canonical field identifiers.
     */
    public static Set<String> parseAndNormalizeRequiredFields(String requiredFields) {
        if (requiredFields == null || requiredFields.isBlank()) {
            return Collections.emptySet();
        }
        Set<String> normalized = new LinkedHashSet<>();
        String[] tokens = requiredFields.split("[,;|]+");
        for (String token : tokens) {
            String clean = token.trim().toLowerCase(Locale.ROOT).replaceAll("[\\s_-]+", "");
            if (clean.isEmpty()) continue;
            switch (clean) {
                case "companyname":
                case "company":
                case "businessname":
                case "business":
                case "name":
                    normalized.add("company_name");
                    break;
                case "category":
                case "industry":
                    normalized.add("category");
                    break;
                case "website":
                case "websiteurl":
                case "domain":
                case "url":
                    normalized.add("website");
                    break;
                case "address":
                case "addresses":
                case "location":
                    normalized.add("address");
                    break;
                case "pincode":
                case "pin":
                case "postalcode":
                case "zip":
                case "zipcode":
                    normalized.add("pincode");
                    break;
                case "city":
                    normalized.add("city");
                    break;
                case "state":
                case "province":
                case "region":
                    normalized.add("state");
                    break;
                case "phone":
                case "phones":
                case "phonenumber":
                case "telephone":
                case "tel":
                    normalized.add("phone");
                    break;
                case "email":
                case "emails":
                case "emailaddress":
                    normalized.add("email");
                    break;
                case "whatsapp":
                case "wa":
                    normalized.add("whatsapp");
                    break;
                case "contactperson":
                case "contact":
                case "person":
                case "leadcontact":
                    normalized.add("contact_person");
                    break;
                case "contactrole":
                case "role":
                case "designation":
                case "title":
                    normalized.add("contact_role");
                    break;
                case "social":
                case "sociallinks":
                case "sociallink":
                case "socialmedia":
                    normalized.add("social_links");
                    break;
                default:
                    normalized.add(token.trim().toLowerCase(Locale.ROOT).replace(" ", "_"));
                    break;
            }
        }
        return Collections.unmodifiableSet(normalized);
    }

    public ConfidenceResult calculateConfidence(String websiteUrl,
                                                List<PageExtractDto> pages,
                                                Set<String> emails,
                                                Set<String> phones,
                                                Set<String> addresses,
                                                Map<String, String> socialLinks) {
        return calculateConfidence(null, websiteUrl, pages, emails, phones, addresses, socialLinks, null);
    }

    public ConfidenceResult calculateConfidence(OrganizationIdentity identity,
                                                String websiteUrl,
                                                List<PageExtractDto> pages,
                                                Set<String> emails,
                                                Set<String> phones,
                                                Set<String> addresses,
                                                Map<String, String> socialLinks) {
        return calculateConfidence(identity, websiteUrl, pages, emails, phones, addresses, socialLinks, null);
    }

    public ConfidenceResult calculateConfidence(OrganizationIdentity identity,
                                                String websiteUrl,
                                                List<PageExtractDto> pages,
                                                Set<String> emails,
                                                Set<String> phones,
                                                Set<String> addresses,
                                                Map<String, String> socialLinks,
                                                String requiredFields) {
        return calculateConfidence(identity, websiteUrl, pages, emails, phones, Collections.emptySet(), addresses, socialLinks, requiredFields);
    }

    public ConfidenceResult calculateConfidence(OrganizationIdentity identity,
                                                String websiteUrl,
                                                List<PageExtractDto> pages,
                                                Set<String> emails,
                                                Set<String> phones,
                                                Set<String> whatsAppNumbers,
                                                Set<String> addresses,
                                                Map<String, String> socialLinks,
                                                String requiredFields) {
        double score = 0.0;
        List<String> factors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        Set<String> requested = parseAndNormalizeRequiredFields(requiredFields);
        boolean hasFilter = !requested.isEmpty();

        String targetDomain = UrlFilterUtils.extractDomain(websiteUrl);

        // 1. Official domain check (+15 pts)
        if (targetDomain != null && !targetDomain.isBlank()) {
            score += 15.0;
            factors.add("Valid official domain established (+15)");
        } else {
            if (!hasFilter || requested.contains("website") || requested.contains("domain")) {
                missing.add("website");
            }
        }

        // 2. Reachability check (+20 pts)
        boolean isReachable = pages != null && pages.stream().anyMatch(p -> p.getHttpStatus() == 200);
        if (isReachable) {
            score += 20.0;
            factors.add("Official website reachable with HTTP 200 (+20)");
        } else {
            if (!hasFilter || requested.contains("website")) {
                missing.add("website_reachability");
            }
            warnings.add("Website is unreachable or returned non-200 HTTP status.");
        }

        // 3. Organization Identity Verification (+25 pts)
        if (identity != null) {
            if (identity.getIdentityStatus() == IdentityValidationResult.IDENTITY_CONFIRMED) {
                score += 25.0;
                factors.add("Organization identity confirmed via " + identity.getExtractionSource() + " (+25)");
            } else if (identity.getIdentityStatus() == IdentityValidationResult.IDENTITY_UNCERTAIN) {
                score += 5.0;
                factors.add("Organization identity inferred from title/domain (" + identity.getExtractionSource() + ") (+5)");
                warnings.add("Identity is unconfirmed by structured metadata.");
            } else {
                warnings.add("Organization identity was rejected: " + identity.getRejectionReason());
            }
        } else {
            if (targetDomain != null && !targetDomain.isBlank()) {
                score += 15.0;
                factors.add("Default domain identity assigned (+15)");
            }
        }

        // 4. Contact or About page crawled (+10 pts)
        boolean hasContactOrAbout = pages != null && pages.stream().anyMatch(p ->
                (p.getPageType() == SourcePageType.CONTACT || p.getPageType() == SourcePageType.ABOUT) && p.getHttpStatus() == 200);
        if (hasContactOrAbout) {
            score += 10.0;
            factors.add("Contact/About page crawled successfully (+10)");
        }

        // 5. Emails and Domain Match / Mismatch
        boolean hasDomainMismatch = false;
        if (emails != null && !emails.isEmpty()) {
            score += 10.0;
            factors.add("Public email address extracted (+10)");

            boolean domainMatch = false;
            boolean hasThirdPartyMismatch = false;

            for (String email : emails) {
                ContactVerificationStatus eval = evaluateEmailVerification(email, targetDomain);
                if (eval == ContactVerificationStatus.VERIFIED) {
                    domainMatch = true;
                } else if (eval == ContactVerificationStatus.EXTERNAL) {
                    hasThirdPartyMismatch = true;
                }
            }

            if (domainMatch) {
                score += 10.0;
                factors.add("Corporate email domain matches website domain (+10)");
            } else if (hasThirdPartyMismatch) {
                score -= 15.0;
                hasDomainMismatch = true;
                warnings.add("Cross-company email domain mismatch detected (-15).");
            }
        } else {
            if (!hasFilter || requested.contains("email")) {
                missing.add("email");
            }
        }

        // 6. Phone Numbers (+5 pts)
        if (phones != null && !phones.isEmpty()) {
            score += 5.0;
            factors.add("Public phone number extracted (+5)");
        } else {
            if (!hasFilter || requested.contains("phone")) {
                missing.add("phone");
            }
        }

        // 6b. WhatsApp Contact (+5 pts)
        if (whatsAppNumbers != null && !whatsAppNumbers.isEmpty()) {
            score += 5.0;
            factors.add("WhatsApp contact number extracted (+5)");
        } else {
            if (hasFilter && requested.contains("whatsapp")) {
                missing.add("whatsapp");
            }
        }

        // 7. Physical Address (+5 pts)
        if (addresses != null && !addresses.isEmpty()) {
            score += 5.0;
            factors.add("Physical location address extracted (+5)");
        } else {
            if (!hasFilter || requested.contains("address")) {
                missing.add("address");
            }
        }

        // 8. Social Media Links (+5 pts)
        if (socialLinks != null && !socialLinks.isEmpty()) {
            score += 5.0;
            factors.add("Company social media channels found (+5)");
        } else {
            if (!hasFilter || requested.contains("social_links")) {
                missing.add("social_links");
            }
        }

        score = Math.min(100.0, Math.max(0.0, score));
        BigDecimal finalScore = BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);

        VerificationStatus status;
        if (targetDomain == null || targetDomain.isBlank() || !isReachable) {
            status = VerificationStatus.UNVERIFIED;
        } else if (identity != null && identity.getIdentityStatus() == IdentityValidationResult.IDENTITY_REJECTED) {
            status = VerificationStatus.UNVERIFIED;
        } else if (finalScore.compareTo(BigDecimal.valueOf(75.0)) >= 0 && isReachable && !hasDomainMismatch
                && (identity == null || identity.getIdentityStatus() == IdentityValidationResult.IDENTITY_CONFIRMED)) {
            status = VerificationStatus.VERIFIED;
        } else if (finalScore.compareTo(BigDecimal.valueOf(40.0)) >= 0 && isReachable) {
            status = VerificationStatus.PARTIALLY_VERIFIED;
        } else {
            status = VerificationStatus.UNVERIFIED;
        }

        String missingFieldsStr = String.join(",", missing);
        return new ConfidenceResult(finalScore, status, missingFieldsStr, factors, warnings);
    }
}
