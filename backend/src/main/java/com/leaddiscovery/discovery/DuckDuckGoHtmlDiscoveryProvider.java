package com.leaddiscovery.discovery;

import com.leaddiscovery.dto.DiscoveredBusinessDto;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class DuckDuckGoHtmlDiscoveryProvider implements BusinessDiscoveryProvider {

    private static final Logger log = LoggerFactory.getLogger(DuckDuckGoHtmlDiscoveryProvider.class);

    private static final String PROVIDER_NAME = "DuckDuckGo-HTML";
    private static final String HTML_ENDPOINT = "https://html.duckduckgo.com/html/";
    private static final String LITE_ENDPOINT = "https://lite.duckduckgo.com/lite/";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    private static final int TIMEOUT_MILLIS = 5000;

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public List<DiscoveredBusinessDto> discover(String location, String keyword, int maxResults) {
        String baseQuery = keyword.trim() + " in " + location.trim();
        log.info("[DISCOVERY_SEARCH] Starting DuckDuckGo search: baseQuery='{}', maxResults={}", baseQuery, maxResults);

        List<String> queryVariants = List.of(
                baseQuery,
                keyword.trim() + " " + location.trim() + " official website",
                keyword.trim() + " " + location.trim() + " contact"
        );

        List<DiscoveredBusinessDto> allResults = new ArrayList<>();
        Set<String> seenDomains = new HashSet<>();

        for (String query : queryVariants) {
            if (allResults.size() >= maxResults) {
                break;
            }

            // Strategy 1: POST to html.duckduckgo.com/html/
            try {
                List<DiscoveredBusinessDto> results = executePostSearch(HTML_ENDPOINT, query, "https://html.duckduckgo.com/", maxResults - allResults.size());
                if (results.isEmpty()) {
                    // Strategy 2: Fallback to POST lite.duckduckgo.com/lite/
                    results = executePostSearch(LITE_ENDPOINT, query, "https://lite.duckduckgo.com/", maxResults - allResults.size());
                }
                if (results.isEmpty()) {
                    // Strategy 3: Fallback to GET html.duckduckgo.com/html/?q=...
                    results = executeGetSearch(HTML_ENDPOINT + "?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8), query, maxResults - allResults.size());
                }

                for (DiscoveredBusinessDto item : results) {
                    String domain = UrlFilterUtils.extractDomain(item.getWebsiteUrl());
                    if (!domain.isBlank() && seenDomains.add(domain)) {
                        allResults.add(item);
                        if (allResults.size() >= maxResults) {
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("[DIAGNOSTIC] DuckDuckGo overall request failed or timed out for query '{}': {}. Skipping further DDG variants.", query, e.getMessage());
                break;
            }
        }

        log.info("[DISCOVERY_ACCEPTED] DuckDuckGo provider returned {} unique candidate websites", allResults.size());
        return allResults;
    }

    private List<DiscoveredBusinessDto> executePostSearch(String endpoint, String query, String referer, int maxResults) {
        try {
            Connection connection = Jsoup.connect(endpoint)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MILLIS)
                    .followRedirects(true)
                    .referrer(referer)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "same-origin")
                    .header("Sec-Fetch-User", "?1")
                    .header("Upgrade-Insecure-Requests", "1")
                    .data("q", query)
                    .data("kl", "wt-wt")
                    .method(Connection.Method.POST);

            Connection.Response response = connection.execute();
            int statusCode = response.statusCode();
            String contentType = response.contentType();
            String responseBody = response.body();
            boolean challengeDetected = isChallengeOrBlocked(responseBody);

            if (challengeDetected) {
                log.warn("[DIAGNOSTIC] DuckDuckGo POST -> URL: {}, Status: {}, Content-Type: {}, IsChallenge/Blocked: true, BodySnippet: '{}'",
                        endpoint, statusCode, contentType != null ? contentType : "unknown", extractBodySnippet(responseBody));
                return Collections.emptyList();
            }

            Document doc = response.parse();
            List<DiscoveredBusinessDto> results = parseHtmlResults(doc, endpoint, maxResults);

            log.info("[DIAGNOSTIC] DuckDuckGo POST -> URL: {}, Status: {}, Content-Type: {}, IsChallenge/Blocked: false, ParsedResults: {}",
                    endpoint, statusCode, contentType != null ? contentType : "unknown", results.size());

            return results;
        } catch (java.net.SocketTimeoutException e) {
            log.warn("[DIAGNOSTIC] DuckDuckGo POST request timed out -> URL: {}, Query: '{}'", endpoint, query);
            throw new RuntimeException("DuckDuckGo timeout", e);
        } catch (Exception e) {
            log.warn("[DIAGNOSTIC] DuckDuckGo POST request failed -> URL: {}, Query: '{}', Error: {}", endpoint, query, e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<DiscoveredBusinessDto> executeGetSearch(String searchUrl, String query, int maxResults) {
        try {
            Connection connection = Jsoup.connect(searchUrl)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MILLIS)
                    .followRedirects(true)
                    .referrer("https://duckduckgo.com/")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Sec-Fetch-Dest", "document")
                    .header("Sec-Fetch-Mode", "navigate")
                    .header("Sec-Fetch-Site", "cross-site")
                    .header("Upgrade-Insecure-Requests", "1")
                    .method(Connection.Method.GET);

            Connection.Response response = connection.execute();
            int statusCode = response.statusCode();
            String contentType = response.contentType();
            String responseBody = response.body();
            boolean challengeDetected = isChallengeOrBlocked(responseBody);

            if (challengeDetected) {
                log.warn("[DIAGNOSTIC] DuckDuckGo GET -> URL: {}, Status: {}, Content-Type: {}, IsChallenge/Blocked: true, BodySnippet: '{}'",
                        searchUrl, statusCode, contentType != null ? contentType : "unknown", extractBodySnippet(responseBody));
                return Collections.emptyList();
            }

            Document doc = response.parse();
            List<DiscoveredBusinessDto> results = parseHtmlResults(doc, searchUrl, maxResults);

            log.info("[DIAGNOSTIC] DuckDuckGo GET -> URL: {}, Status: {}, Content-Type: {}, IsChallenge/Blocked: false, ParsedResults: {}",
                    searchUrl, statusCode, contentType != null ? contentType : "unknown", results.size());

            return results;
        } catch (java.net.SocketTimeoutException e) {
            log.warn("[DIAGNOSTIC] DuckDuckGo GET search timed out -> URL: {}, Query: '{}'", searchUrl, query);
            throw new RuntimeException("DuckDuckGo timeout", e);
        } catch (Exception e) {
            log.warn("[DIAGNOSTIC] DuckDuckGo GET search failed -> URL: {}, Query: '{}', Error: {}", searchUrl, query, e.getMessage());
            return Collections.emptyList();
        }
    }

    public boolean isChallengeOrBlocked(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return false;
        }
        String lower = responseBody.toLowerCase(Locale.ROOT);
        return lower.contains("anomaly-modal") ||
                lower.contains("challenge-form") ||
                lower.contains("js-anomaly-modal-submit") ||
                lower.contains("error-lite+") ||
                lower.contains("challenge-submit") ||
                lower.contains("are you human") ||
                lower.contains("unusual traffic") ||
                lower.contains("g-recaptcha") ||
                lower.contains("cf-challenge");
    }

    private String extractBodySnippet(String body) {
        if (body == null) return "null";
        int len = Math.min(body.length(), 300);
        return body.substring(0, len).replaceAll("\\s+", " ").trim();
    }

    public List<DiscoveredBusinessDto> parseHtmlResults(Document doc, String sourceSearchUrl, int maxResults) {
        List<DiscoveredBusinessDto> results = new ArrayList<>();
        Set<String> seenDomains = new HashSet<>();

        // Standard DDG HTML selectors
        Elements resultElements = doc.select(".results .result, .web-result, .result__body");

        // DDG Lite layout selectors: table rows containing a.result-link
        if (resultElements.isEmpty()) {
            resultElements = doc.select("a.result-link, a.result__a, tr:has(a.result-link)");
        }

        // Generic fallback if empty
        if (resultElements.isEmpty()) {
            resultElements = doc.select("a[href*='uddg='], a.result__url");
        }

        for (Element el : resultElements) {
            if (results.size() >= maxResults) {
                break;
            }

            Element linkAnchor = el.is("a") ? el : el.selectFirst("a.result-link, a.result__a, a.result__url, a[href*='uddg='], a[href]");
            if (linkAnchor == null) {
                continue;
            }

            String rawHref = linkAnchor.attr("href");
            String normalizedUrl = UrlFilterUtils.normalizeUrl(rawHref);

            if (normalizedUrl == null || !UrlFilterUtils.isValidOfficialWebsite(normalizedUrl)) {
                continue;
            }

            String domain = UrlFilterUtils.extractDomain(normalizedUrl);
            if (domain.isBlank() || seenDomains.contains(domain)) {
                continue;
            }

            // Extract raw search candidate title text
            String rawTitle = linkAnchor.text();
            if (rawTitle.isBlank() && el != linkAnchor) {
                Element titleEl = el.selectFirst(".result__title, .result-link, h2, h3");
                if (titleEl != null) {
                    rawTitle = titleEl.text();
                }
            }

            // Reject search-result articles, listicles, blog posts, or directory listings early
            if (UrlFilterUtils.isListicleOrDirectory(rawTitle, normalizedUrl)) {
                log.debug("Skipping listicle/directory candidate: '{}' ({})", rawTitle, normalizedUrl);
                continue;
            }

            String candidateName = cleanBusinessTitle(rawTitle, domain);
            if (UrlFilterUtils.isListicleOrDirectory(candidateName, normalizedUrl)) {
                continue;
            }

            seenDomains.add(domain);
            results.add(new DiscoveredBusinessDto(
                    candidateName,
                    normalizedUrl,
                    sourceSearchUrl,
                    PROVIDER_NAME,
                    0.50,
                    "CANDIDATE"
            ));
        }

        log.debug("Parsed {} candidate websites from DuckDuckGo response", results.size());
        return results;
    }

    public List<DiscoveredBusinessDto> parseHtmlResultsFromContent(String html, String sourceUrl, int maxResults) {
        Document doc = Jsoup.parse(html);
        return parseHtmlResults(doc, sourceUrl, maxResults);
    }

    public String cleanBusinessTitle(String rawTitle, String fallbackDomain) {
        return UrlFilterUtils.cleanBrandFromTitle(rawTitle, fallbackDomain);
    }
}
