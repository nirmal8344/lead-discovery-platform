package com.leaddiscovery.service;

import com.leaddiscovery.discovery.BusinessDiscoveryProvider;
import com.leaddiscovery.discovery.DuckDuckGoHtmlDiscoveryProvider;
import com.leaddiscovery.discovery.UrlFilterUtils;
import com.leaddiscovery.dto.DiscoveredBusinessDto;
import com.leaddiscovery.dto.LeadDiscoveryRequest;
import com.leaddiscovery.dto.LeadDiscoveryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LeadDiscoveryServiceTest {

    @Test
    @DisplayName("Should normalize URLs and unescape search engine redirect wrappers")
    void testUrlFilterNormalizationAndUnescape() {
        String ddgRedirect = "//duckduckgo.com/l/?uddg=https%3A%2F%2Fwww.innovativesoftware.com%2Fabout%2F%3Futm_source%3Dddg&rut=123";
        String normalized = UrlFilterUtils.normalizeUrl(ddgRedirect);

        assertEquals("https://innovativesoftware.com/about", normalized);
    }

    @Test
    @DisplayName("Should filter out social media platforms and directory aggregators as official business websites")
    void testSocialMediaAndAggregatorExclusion() {
        assertFalse(UrlFilterUtils.isValidOfficialWebsite("https://www.facebook.com/acmecompany"));
        assertFalse(UrlFilterUtils.isValidOfficialWebsite("https://www.linkedin.com/company/acme"));
        assertFalse(UrlFilterUtils.isValidOfficialWebsite("https://twitter.com/acme"));
        assertFalse(UrlFilterUtils.isValidOfficialWebsite("https://instagram.com/acme"));
        assertFalse(UrlFilterUtils.isValidOfficialWebsite("https://youtube.com/@acme"));
        assertFalse(UrlFilterUtils.isValidOfficialWebsite("https://en.wikipedia.org/wiki/Acme"));
        assertFalse(UrlFilterUtils.isValidOfficialWebsite("https://yelp.com/biz/acme"));
        assertFalse(UrlFilterUtils.isValidOfficialWebsite("https://example.com/brochure.pdf"));

        assertTrue(UrlFilterUtils.isValidOfficialWebsite("https://acme-software.com"));
        assertTrue(UrlFilterUtils.isValidOfficialWebsite("https://www.greenhealthclinic.org/contact"));
    }

    @Test
    @DisplayName("Should parse search results HTML fixture and extract clean businesses")
    void testParseSearchResultsHtmlFixture() {
        String mockHtml = """
                <div class="results">
                    <div class="result">
                        <h2 class="result__title">
                            <a class="result__a" href="//duckduckgo.com/l/?uddg=https%3A%2F%2Fapextechsolutions.com%2F&rut=1">Apex Tech Solutions - Home</a>
                        </h2>
                    </div>
                    <div class="result">
                        <h2 class="result__title">
                            <a class="result__a" href="//duckduckgo.com/l/?uddg=https%3A%2F%2Ffacebook.com%2Fapextech%2F&rut=1">Apex Tech on Facebook</a>
                        </h2>
                    </div>
                    <div class="result">
                        <h2 class="result__title">
                            <a class="result__a" href="//duckduckgo.com/l/?uddg=https%3A%2F%2Fapextechsolutions.com%2Fcontact&rut=1">Apex Tech Solutions Contact</a>
                        </h2>
                    </div>
                    <div class="result">
                        <h2 class="result__title">
                            <a class="result__a" href="//duckduckgo.com/l/?uddg=https%3A%2F%2Fsalemcloudsystems.in%2F&rut=1">Salem Cloud Systems | Official Site</a>
                        </h2>
                    </div>
                </div>
                """;

        DuckDuckGoHtmlDiscoveryProvider provider = new DuckDuckGoHtmlDiscoveryProvider();
        List<DiscoveredBusinessDto> results = provider.parseHtmlResultsFromContent(mockHtml, "https://html.duckduckgo.com/html/?q=test", 10);

        assertNotNull(results);
        assertEquals(2, results.size()); // 1 for apextechsolutions.com (deduplicated), 1 for salemcloudsystems.in, facebook.com excluded

        assertEquals("Apex Tech Solutions", results.get(0).getBusinessName());
        assertEquals("https://apextechsolutions.com", results.get(0).getWebsiteUrl());

        assertEquals("Salem Cloud Systems", results.get(1).getBusinessName());
        assertEquals("https://salemcloudsystems.in", results.get(1).getWebsiteUrl());
    }

    @Test
    @DisplayName("Should enforce maxResults limit and deduplicate across providers")
    void testLeadDiscoveryServiceLimitAndDeduplication() {
        BusinessDiscoveryProvider mockProvider1 = new BusinessDiscoveryProvider() {
            @Override
            public List<DiscoveredBusinessDto> discover(String location, String keyword, int maxResults) {
                return List.of(
                        new DiscoveredBusinessDto("Company One", "https://companyone.com", "src1", "mock1", 0.9, "DISCOVERED"),
                        new DiscoveredBusinessDto("Company Two", "https://companytwo.com", "src1", "mock1", 0.9, "DISCOVERED")
                );
            }
            @Override
            public String getProviderName() { return "Mock1"; }
        };

        BusinessDiscoveryProvider mockProvider2 = new BusinessDiscoveryProvider() {
            @Override
            public List<DiscoveredBusinessDto> discover(String location, String keyword, int maxResults) {
                return List.of(
                        new DiscoveredBusinessDto("Company One Duplicate", "https://companyone.com/about", "src2", "mock2", 0.9, "DISCOVERED"),
                        new DiscoveredBusinessDto("Company Three", "https://companythree.com", "src2", "mock2", 0.9, "DISCOVERED")
                );
            }
            @Override
            public String getProviderName() { return "Mock2"; }
        };

        WebScraperService webScraperService = new WebScraperService();
        LeadDiscoveryService service = new LeadDiscoveryService(List.of(mockProvider1, mockProvider2), webScraperService);

        LeadDiscoveryRequest request = new LeadDiscoveryRequest("Salem", "Software", 2);
        LeadDiscoveryResponse response = service.discoverLeads(request);

        assertNotNull(response);
        assertEquals("Salem", response.getLocation());
        assertEquals("Software", response.getKeyword());
        assertEquals(2, response.getTotalDiscovered()); // Enforces maxResults = 2
        assertEquals(2, response.getBusinesses().size());

        // First two unique companies
        assertEquals("https://companyone.com", response.getBusinesses().get(0).getWebsiteUrl());
        assertEquals("https://companytwo.com", response.getBusinesses().get(1).getWebsiteUrl());
    }

    @Test
    @DisplayName("Should parse DuckDuckGo Lite table results HTML fixture and extract clean businesses")
    void testParseLiteSearchResultsHtmlFixture() {
        String mockLiteHtml = """
                <html>
                <body>
                  <table border="0">
                    <tr>
                      <td valign="top">1.&nbsp;</td>
                      <td>
                        <a rel="nofollow" href="https://salemtechnologies.com/" class="result-link">Salem Technologies - Custom Software Development</a>
                      </td>
                    </tr>
                    <tr>
                      <td>&nbsp;&nbsp;&nbsp;</td>
                      <td class="result-snippet">Leading custom software development and cloud consulting in Salem, Tamil Nadu.</td>
                    </tr>
                    <tr>
                      <td valign="top">2.&nbsp;</td>
                      <td>
                        <a rel="nofollow" href="https://www.linkedin.com/company/salemtech" class="result-link">Salem Technologies on LinkedIn</a>
                      </td>
                    </tr>
                    <tr>
                      <td valign="top">3.&nbsp;</td>
                      <td>
                        <a rel="nofollow" href="https://innovatesolutions.in/about" class="result-link">Innovate Solutions India | Official Site</a>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """;

        DuckDuckGoHtmlDiscoveryProvider provider = new DuckDuckGoHtmlDiscoveryProvider();
        List<DiscoveredBusinessDto> results = provider.parseHtmlResultsFromContent(mockLiteHtml, "https://lite.duckduckgo.com/lite/", 10);

        assertNotNull(results);
        assertEquals(2, results.size()); // linkedin excluded, 2 unique domains

        assertEquals("Salem Technologies", results.get(0).getBusinessName());
        assertEquals("https://salemtechnologies.com", results.get(0).getWebsiteUrl());

        assertEquals("Innovate Solutions India", results.get(1).getBusinessName());
        assertEquals("https://innovatesolutions.in/about", results.get(1).getWebsiteUrl());
    }

    @Test
    @DisplayName("Should handle provider failures gracefully and return warnings")
    void testProviderFailureHandling() {
        BusinessDiscoveryProvider failingProvider = new BusinessDiscoveryProvider() {
            @Override
            public List<DiscoveredBusinessDto> discover(String location, String keyword, int maxResults) {
                throw new RuntimeException("Simulated connection timeout to search engine");
            }
            @Override
            public String getProviderName() { return "FailingProvider"; }
        };

        WebScraperService webScraperService = new WebScraperService();
        LeadDiscoveryService service = new LeadDiscoveryService(List.of(failingProvider), webScraperService);

        LeadDiscoveryRequest request = new LeadDiscoveryRequest("Salem", "Software", 5);
        LeadDiscoveryResponse response = service.discoverLeads(request);

        assertNotNull(response);
        assertEquals(0, response.getTotalDiscovered());
        assertTrue(response.getBusinesses().isEmpty());
        assertFalse(response.getWarnings().isEmpty());
        assertTrue(response.getWarnings().stream().anyMatch(w -> w.contains("Simulated connection timeout") || w.contains("FailingProvider")));
    }

    @Test
    @DisplayName("Should accurately detect DDG anomaly/challenge captcha pages")
    void testDetectChallengePage() {
        DuckDuckGoHtmlDiscoveryProvider provider = new DuckDuckGoHtmlDiscoveryProvider();

        String challengeHtml = """
                <html>
                <head><title>DuckDuckGo</title></head>
                <body>
                  <form id="challenge-form" action="/html/" method="POST">
                    <input type="checkbox" class="anomaly-modal__check" name="image-check_123">
                    <button name="challenge-submit" class="js-anomaly-modal-submit">Submit</button>
                    <p class="feedback-instructions">Please email the following code to: error-lite+edd8@duckduckgo.com</p>
                  </form>
                </body>
                </html>
                """;

        assertTrue(provider.isChallengeOrBlocked(challengeHtml));
        assertFalse(provider.isChallengeOrBlocked("<html><body><div class='results'><a class='result-link' href='https://test.com'>Test</a></div></body></html>"));
        assertFalse(provider.isChallengeOrBlocked(null));
        assertFalse(provider.isChallengeOrBlocked(""));
    }

    @Test
    @DisplayName("Should detect and filter out listicles, roundups, and search-result directory titles")
    void testListicleDetection() {
        assertTrue(UrlFilterUtils.isListicleOrDirectory("15 Best IT Companies in Salem in 2025", "https://marketmystique.com/it-companies-in-salem/"));
        assertTrue(UrlFilterUtils.isListicleOrDirectory("Top 12 IT Companies in Salem To Work For in 2026", "https://buzz4ai.com/it-companies-in-salem/"));
        assertTrue(UrlFilterUtils.isListicleOrDirectory("Top 15 IT Companies in Salem (2026) - Brandveda", "https://brandveda.in/blog/it-companies-in-salem"));
        assertTrue(UrlFilterUtils.isListicleOrDirectory("Best Software Companies in Salem | IT & Development", "https://salem.citybusinessdirectory.in/software-companies"));
        assertTrue(UrlFilterUtils.isListicleOrDirectory("Software Companies near me in Salem", "https://indiaonline.in/salem/software-companies/34738"));

        assertFalse(UrlFilterUtils.isListicleOrDirectory("Apex Tech Solutions", "https://apextechsolutions.com"));
        assertFalse(UrlFilterUtils.isListicleOrDirectory("Salem Cloud Systems", "https://salemcloudsystems.in/about"));
    }

    @Test
    @DisplayName("Should reject search result articles and only extract genuine business homepages with accurate source URLs")
    void testFilterListiclesFromHtmlParsing() {
        String mockHtmlWithListicles = """
                <html>
                <body>
                  <table border="0">
                    <tr>
                      <td><a class="result-link" href="https://marketmystique.com/it-companies-in-salem/">15 Best IT Companies in Salem in 2025 - marketmystique.com</a></td>
                    </tr>
                    <tr>
                      <td><a class="result-link" href="https://salemcloud.in">Salem Cloud Technologies | Official Site</a></td>
                    </tr>
                    <tr>
                      <td><a class="result-link" href="https://citybusinessdirectory.in/salem/software-companies">Software Companies near me in Salem - IndiaOnline</a></td>
                    </tr>
                  </table>
                </body>
                </html>
                """;

        DuckDuckGoHtmlDiscoveryProvider provider = new DuckDuckGoHtmlDiscoveryProvider();
        String searchUrl = "https://lite.duckduckgo.com/lite/";
        List<DiscoveredBusinessDto> results = provider.parseHtmlResultsFromContent(mockHtmlWithListicles, searchUrl, 10);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Salem Cloud Technologies", results.get(0).getBusinessName());
        assertEquals("https://salemcloud.in", results.get(0).getWebsiteUrl());
        assertEquals(searchUrl, results.get(0).getSourceUrl());
    }
}



