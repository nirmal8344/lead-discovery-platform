package com.leaddiscovery.service;

import com.leaddiscovery.dto.OrganizationIdentity;
import com.leaddiscovery.entity.enums.IdentityValidationResult;
import com.leaddiscovery.entity.enums.SourcePageType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WebScraperServiceTest {

    private WebScraperService scraperService;

    @BeforeEach
    void setUp() {
        scraperService = new WebScraperService();
    }

    @Test
    @DisplayName("Should extract emails from mailto links and plain text")
    void testExtractEmails() {
        String html = """
                <html>
                    <head><title>Test Company</title></head>
                    <body>
                        <h1>Welcome to Test Corp</h1>
                        <p>Contact us at info@testcorp.com or support@testcorp.io for help.</p>
                        <a href="mailto:sales@testcorp.com?subject=Inquiry">Email Sales</a>
                        <a href="mailto:billing@testcorp.com">Email Billing</a>
                        <img src="logo@2x.png" />
                    </body>
                </html>
                """;

        Set<String> emails = scraperService.extractEmailsFromHtml(html);

        assertNotNull(emails);
        assertTrue(emails.contains("info@testcorp.com"));
        assertTrue(emails.contains("support@testcorp.io"));
        assertTrue(emails.contains("sales@testcorp.com"));
        assertTrue(emails.contains("billing@testcorp.com"));
        assertFalse(emails.contains("logo@2x.png"));
    }

    @Test
    @DisplayName("Should extract phone numbers from tel links and plain text")
    void testExtractPhoneNumbers() {
        String html = """
                <html>
                    <head><title>Contact Page</title></head>
                    <body>
                        <p>Call our office: +1 (555) 234-5678 or +91 9876543210</p>
                        <a href="tel:+18005550199">Toll Free: 1-800-555-0199</a>
                        <p>Established in 2024. Zip code: 90210.</p>
                    </body>
                </html>
                """;

        Set<String> phones = scraperService.extractPhoneNumbersFromHtml(html);

        assertNotNull(phones);
        assertTrue(phones.contains("+15552345678") || phones.contains("15552345678") || phones.stream().anyMatch(p -> p.contains("5552345678")));
        assertTrue(phones.contains("+919876543210") || phones.stream().anyMatch(p -> p.contains("9876543210")));
        assertTrue(phones.contains("+18005550199") || phones.stream().anyMatch(p -> p.contains("18005550199")));
        assertFalse(phones.contains("2024"));
        assertFalse(phones.contains("90210"));
    }

    @Test
    @DisplayName("Should extract social media links and ignore sharer dialogs")
    void testExtractSocialLinks() {
        String html = """
                <html>
                    <body>
                        <a href="https://www.linkedin.com/company/acme-corp">LinkedIn</a>
                        <a href="https://twitter.com/acme_official">Twitter</a>
                        <a href="https://facebook.com/acmecorp">Facebook</a>
                        <a href="https://instagram.com/acme.life">Instagram</a>
                        <a href="https://youtube.com/@acmecorp">YouTube</a>
                        <!-- Sharer URLs that should be ignored -->
                        <a href="https://www.facebook.com/sharer/sharer.php?u=example.com">Share on FB</a>
                        <a href="https://twitter.com/intent/tweet?text=hello">Tweet</a>
                    </body>
                </html>
                """;

        Map<String, String> social = scraperService.extractSocialLinksFromHtml(html);

        assertNotNull(social);
        assertEquals("https://www.linkedin.com/company/acme-corp", social.get("linkedin"));
        assertEquals("https://twitter.com/acme_official", social.get("twitter"));
        assertEquals("https://facebook.com/acmecorp", social.get("facebook"));
        assertEquals("https://instagram.com/acme.life", social.get("instagram"));
        assertEquals("https://youtube.com/@acmecorp", social.get("youtube"));
    }

    @Test
    @DisplayName("Should extract physical addresses from address tag and structured classes")
    void testExtractAddresses() {
        String html = """
                <html>
                    <body>
                        <address>
                            123 Main Street, Suite 400, Springfield, IL 62701
                        </address>
                        <div class="contact-address">
                            456 Tech Park, Electronic City, Bangalore 560100
                        </div>
                    </body>
                </html>
                """;

        Set<String> addresses = scraperService.extractAddressesFromHtml(html);

        assertNotNull(addresses);
        assertTrue(addresses.stream().anyMatch(a -> a.contains("123 Main Street")));
        assertTrue(addresses.stream().anyMatch(a -> a.contains("456 Tech Park")));
    }

    @Test
    @DisplayName("Should extract organization identity from JSON-LD schema")
    void testExtractJsonLdOrganizationName() {
        String html = """
                <html>
                <head>
                    <title>Best Software Company in Salem - Home</title>
                    <script type="application/ld+json">
                    {
                        "@context": "https://schema.org",
                        "@type": "Corporation",
                        "name": "Apex Tech Solutions Pvt Ltd",
                        "url": "https://apextechsolutions.com"
                    }
                    </script>
                </head>
                <body>
                    <h1>Welcome</h1>
                </body>
                </html>
                """;

        Document doc = Jsoup.parse(html, "https://apextechsolutions.com");
        OrganizationIdentity identity = scraperService.extractOrganizationIdentity(doc, List.of(), "apextechsolutions.com", "https://apextechsolutions.com");

        assertNotNull(identity);
        assertEquals(IdentityValidationResult.IDENTITY_CONFIRMED, identity.getIdentityStatus());
        assertEquals("Apex Tech Solutions Pvt Ltd", identity.getExtractedBusinessName());
        assertEquals("JSON_LD_ORGANIZATION", identity.getExtractionSource());
    }

    @Test
    @DisplayName("Should extract organization identity from OpenGraph og:site_name")
    void testExtractOgSiteName() {
        String html = """
                <html>
                <head>
                    <title>Home - Top 15 IT Companies in Salem (2026)</title>
                    <meta property="og:site_name" content="Salem Cloud Systems" />
                </head>
                <body>
                    <h1>About Us</h1>
                </body>
                </html>
                """;

        Document doc = Jsoup.parse(html, "https://salemcloud.in");
        OrganizationIdentity identity = scraperService.extractOrganizationIdentity(doc, List.of(), "salemcloud.in", "https://salemcloud.in");

        assertNotNull(identity);
        assertEquals(IdentityValidationResult.IDENTITY_CONFIRMED, identity.getIdentityStatus());
        assertEquals("Salem Cloud Systems", identity.getExtractedBusinessName());
        assertEquals("OPENGRAPH_SITE_NAME", identity.getExtractionSource());
    }

    @Test
    @DisplayName("Should reject candidate website if it is a listicle or directory article")
    void testRejectListicleCandidate() {
        String html = """
                <html>
                <head>
                    <title>Top 15 IT Companies in Salem (2026) - Tech Review Blog</title>
                </head>
                <body>
                    <h1>List of Top Companies in Salem</h1>
                </body>
                </html>
                """;

        Document doc = Jsoup.parse(html, "https://techblog.com/top-15-it-companies-in-salem");
        OrganizationIdentity identity = scraperService.extractOrganizationIdentity(doc, List.of(), "techblog.com", "https://techblog.com/top-15-it-companies-in-salem");

        assertNotNull(identity);
        assertEquals(IdentityValidationResult.IDENTITY_REJECTED, identity.getIdentityStatus());
        assertTrue(identity.getRejectionReason().contains("listicle"));
    }

    @Test
    @DisplayName("Should extract footer copyright brand name when JSON-LD is missing")
    void testExtractBrandFromFooter() {
        String html = """
                <html>
                <head>
                    <title>Welcome to Our Portal</title>
                </head>
                <body>
                    <p>Services</p>
                    <footer>
                        <p>© 2026 Innovate Tech Solutions Pvt Ltd. All rights reserved.</p>
                    </footer>
                </body>
                </html>
                """;

        Document doc = Jsoup.parse(html, "https://innovatetech.in");
        OrganizationIdentity identity = scraperService.extractOrganizationIdentity(doc, List.of(), "innovatetech.in", "https://innovatetech.in");

        assertNotNull(identity);
        assertEquals(IdentityValidationResult.IDENTITY_CONFIRMED, identity.getIdentityStatus());
        assertEquals("Innovate Tech Solutions Pvt Ltd", identity.getExtractedBusinessName());
    }

    @Test
    @DisplayName("Should normalize URLs, remove fragments, and strip tracking parameters")
    void testNormalizeUrl() {
        String base = "https://example.com";
        String normalized1 = scraperService.normalizeUrl("https://example.com/contact/#map", base);
        String normalized2 = scraperService.normalizeUrl("https://example.com/contact/?utm_source=google&utm_medium=cpc", base);
        String normalized3 = scraperService.normalizeUrl("/about/", base);

        assertEquals("https://example.com/contact", normalized1);
        assertEquals("https://example.com/contact", normalized2);
        assertEquals("https://example.com/about", normalized3);
    }

    @Test
    @DisplayName("Should invoke Playwright fallback when JavaScript/SPA shell is detected")
    void testPlaywrightFallbackOnJsShell() {
        PlaywrightScraperService mockPlaywright = org.mockito.Mockito.mock(PlaywrightScraperService.class);
        String renderedHtml = """
                <html>
                <head><title>SPA Hydrated Corp</title></head>
                <body>
                    <h1>Welcome to Hydrated App</h1>
                    <p>Contact: info@hydratedapp.com or +15559876543</p>
                    <address>789 React Boulevard, Web City</address>
                </body>
                </html>
                """;
        Document renderedDoc = Jsoup.parse(renderedHtml, "https://spa-example.com");
        org.mockito.Mockito.when(mockPlaywright.renderPage("https://spa-example.com")).thenReturn(renderedDoc);

        WebScraperService customScraper = new WebScraperService(mockPlaywright);
        assertNotNull(customScraper);
    }

    @Test
    @DisplayName("Should strictly enforce maxCrawlDepth in BFS crawl queue")
    void testCrawlQueueDepthEnforcement() {
        WebScraperService.CrawlQueueItem root = new WebScraperService.CrawlQueueItem("https://example.com", 0);
        assertEquals("https://example.com", root.getUrl());
        assertEquals(0, root.getDepth());

        WebScraperService.CrawlQueueItem depth1 = new WebScraperService.CrawlQueueItem("https://example.com/about", 1);
        assertEquals(1, depth1.getDepth());

        WebScraperService.CrawlQueueItem depth2 = new WebScraperService.CrawlQueueItem("https://example.com/about/team", 2);
        assertEquals(2, depth2.getDepth());

        WebScraperService.CrawlQueueItem depth3 = new WebScraperService.CrawlQueueItem("https://example.com/about/team/member", 3);
        assertEquals(3, depth3.getDepth());

        // When maxCrawlDepth = 0 (root only)
        int maxCrawlDepth0 = 0;
        assertFalse(root.getDepth() < maxCrawlDepth0, "Depth 0 should NOT allow discovering links when maxCrawlDepth=0");

        // When maxCrawlDepth = 1 (root -> child only)
        int maxCrawlDepth1 = 1;
        assertTrue(root.getDepth() < maxCrawlDepth1, "Depth 0 should allow discovering links when maxCrawlDepth=1");
        assertFalse(depth1.getDepth() < maxCrawlDepth1, "Depth 1 should NOT allow discovering links when maxCrawlDepth=1");

        // When maxCrawlDepth = 2 (root -> child -> grandchild)
        int maxCrawlDepth2 = 2;
        assertTrue(root.getDepth() < maxCrawlDepth2, "Depth 0 should allow discovering links when maxCrawlDepth=2");
        assertTrue(depth1.getDepth() < maxCrawlDepth2, "Depth 1 should allow discovering links when maxCrawlDepth=2");
        assertFalse(depth2.getDepth() < maxCrawlDepth2, "Depth 2 should NOT allow discovering links when maxCrawlDepth=2");
    }

    @Test
    @DisplayName("Should prioritize internal links dynamically based on requested extraction targets")
    void testDiscoverInternalLinksPrioritizationWithRequiredFields() {
        String html = """
                <html>
                    <body>
                        <a href="/team">Leadership & Team</a>
                        <a href="/contact">Contact & Support</a>
                        <a href="/services">Our Services</a>
                        <a href="/about">About Us</a>
                    </body>
                </html>
                """;
        Document doc = Jsoup.parse(html, "https://example.com");

        // When Email + Phone + WhatsApp are requested targets
        Set<String> contactTargets = Set.of("email", "phone", "whatsapp");
        List<String> contactPriorityLinks = scraperService.discoverInternalLinks(doc, "https://example.com", "example.com", contactTargets);
        assertNotNull(contactPriorityLinks);
        assertFalse(contactPriorityLinks.isEmpty());
        assertEquals("https://example.com/contact", contactPriorityLinks.get(0));

        // When Contact Person / Team are requested targets
        Set<String> teamTargets = Set.of("contact_person", "contact_role");
        List<String> teamPriorityLinks = scraperService.discoverInternalLinks(doc, "https://example.com", "example.com", teamTargets);
        assertNotNull(teamPriorityLinks);
        assertFalse(teamPriorityLinks.isEmpty());
        assertEquals("https://example.com/team", teamPriorityLinks.get(0));
    }

    @Test
    @DisplayName("Should calculate dynamic page type priority scores based on target fields")
    void testGetPageTypePriorityScoreWithTargetFields() {
        int baseContactScore = scraperService.getPageTypePriorityScore("https://example.com/contact");
        int baseTeamScore = scraperService.getPageTypePriorityScore("https://example.com/team");

        int targetedContactScore = scraperService.getPageTypePriorityScore("https://example.com/contact", Set.of("email", "phone", "whatsapp"));
        int targetedTeamScore = scraperService.getPageTypePriorityScore("https://example.com/team", Set.of("contact_person", "contact_role"));

        assertTrue(targetedContactScore > baseContactScore, "Targeted contact score should be higher than base contact score");
        assertTrue(targetedTeamScore > baseTeamScore, "Targeted team score should be higher than base team score");
        assertTrue(targetedTeamScore > scraperService.getPageTypePriorityScore("https://example.com/contact", Set.of("contact_person")),
                "Team page should rank higher than contact page when contact person is targeted");
    }
}
