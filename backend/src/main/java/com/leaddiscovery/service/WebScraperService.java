package com.leaddiscovery.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leaddiscovery.discovery.UrlFilterUtils;
import com.leaddiscovery.dto.CrawlWebsiteResponse;
import com.leaddiscovery.dto.OrganizationIdentity;
import com.leaddiscovery.dto.PageExtractDto;
import com.leaddiscovery.dto.ScrapeUrlResponse;
import com.leaddiscovery.entity.enums.IdentityValidationResult;
import com.leaddiscovery.entity.enums.SourcePageType;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class WebScraperService {

    private static final Logger log = LoggerFactory.getLogger(WebScraperService.class);

    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 LeadDiscoveryBot/2.0";
    private static final int TIMEOUT_MILLIS = 10000;

    /** Minimum inter-request delay (ms) to the same domain (rate limiting). */
    private static final long DOMAIN_RATE_LIMIT_MS = 400;

    /** Maximum retries on transient 5xx / timeout errors. */
    private static final int MAX_RETRIES = 2;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?:\\+?\\d{1,4}[\\s.-]?)?(?:\\(\\d{1,5}\\)[\\s.-]?)?\\d{2,5}[\\s.-]?\\d{2,5}[\\s.-]?\\d{2,9}"
    );

    private static final Pattern COPYRIGHT_PATTERN = Pattern.compile(
            "(?:©|&copy;|copyright)\\s*(?:\\d{4})?\\s*([A-Za-z0-9\\s.,&'-]{3,80}?)(?:\\.?\\s+all rights|\\.?\\s+privacy policy|\\.?\\s+terms|\\s*\\||\\s*\\n|$)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Set<String> INVALID_EMAIL_EXTENSIONS = Set.of(
            "png", "jpg", "jpeg", "gif", "svg", "webp", "ico", "bmp", "tiff", "js", "css", "woff", "woff2", "ttf"
    );

    private static final Set<String> NON_HTML_EXTENSIONS = Set.of(
            ".pdf", ".jpg", ".jpeg", ".png", ".gif", ".svg", ".webp", ".ico", ".mp4", ".mp3",
            ".avi", ".mov", ".zip", ".tar", ".gz", ".rar", ".7z", ".doc", ".docx", ".xls",
            ".xlsx", ".ppt", ".pptx", ".csv", ".xml", ".json", ".js", ".css", ".woff", ".woff2", ".ttf"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Domain-scoped robots.txt disallowed paths cache.
     * Key: domain, Value: Set of disallowed path prefixes for our bot.
     */
    private final Map<String, Set<String>> robotsCache = new ConcurrentHashMap<>();

    /**
     * Tracks last request timestamp per domain for rate limiting.
     * Key: domain, Value: System.currentTimeMillis() of last request.
     */
    private final Map<String, Long> domainLastRequestTime = new ConcurrentHashMap<>();

    private final PlaywrightScraperService playwrightScraperService;

    public WebScraperService() {
        this.playwrightScraperService = new PlaywrightScraperService();
    }

    public WebScraperService(PlaywrightScraperService playwrightScraperService) {
        this.playwrightScraperService = playwrightScraperService != null ? playwrightScraperService : new PlaywrightScraperService();
    }

    /**
     * Helper record representing a URL and its depth in the BFS crawl queue.
     */
    public static class CrawlQueueItem {
        private final String url;
        private final int depth;

        public CrawlQueueItem(String url, int depth) {
            this.url = url;
            this.depth = depth;
        }

        public String getUrl() { return url; }
        public int getDepth() { return depth; }
    }

    /**
     * Single-page scraping for quick diagnostic tests.
     */
    public ScrapeUrlResponse scrapeUrl(String url) {
        log.info("Fetching single URL: {}", url);
        try {
            Connection.Response response = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MILLIS)
                    .followRedirects(true)
                    .ignoreHttpErrors(false)
                    .execute();

            Document doc = response.parse();
            String pageTitle = doc.title() != null ? doc.title().trim() : "";
            int statusCode = response.statusCode();

            Set<String> emails = extractEmails(doc);
            Set<String> phoneNumbers = extractPhoneNumbers(doc);
            Map<String, String> socialLinks = extractSocialLinks(doc);
            Set<String> addresses = extractAddresses(doc);

            log.info("Scraped {} (HTTP {}): found {} emails, {} phones, {} social links, {} addresses",
                    url, statusCode, emails.size(), phoneNumbers.size(), socialLinks.size(), addresses.size());

            return new ScrapeUrlResponse(
                    url,
                    pageTitle,
                    emails,
                    phoneNumbers,
                    socialLinks,
                    addresses,
                    statusCode,
                    LocalDateTime.now()
            );
        } catch (IOException e) {
            log.error("Failed to scrape single URL {}: {}", url, e.getMessage());
            throw new RuntimeException("Failed to fetch and parse URL [" + url + "]: " + e.getMessage(), e);
        }
    }

    /**
     * Multi-page website crawling with default maxCrawlDepth.
     */
    public CrawlWebsiteResponse crawlWebsite(String startUrl, int maxPages) {
        return crawlWebsite(startUrl, maxPages, 3, null);
    }

    /**
     * Multi-page website crawling with default maxCrawlDepth.
     */
    public CrawlWebsiteResponse crawlWebsite(String startUrl, int maxPages, int maxCrawlDepth) {
        return crawlWebsite(startUrl, maxPages, maxCrawlDepth, null);
    }

    /**
     * Multi-page website crawling with multi-signal organization identity extraction,
     * targeted extraction for user requiredFields, strict BFS crawl depth enforcement,
     * and Playwright JavaScript fallback rendering.
     */
    public CrawlWebsiteResponse crawlWebsite(String startUrl, int maxPages, int maxCrawlDepth, String requiredFields) {
        String normalizedRoot = normalizeUrl(startUrl, startUrl);
        String rootDomain = extractDomain(normalizedRoot);
        int limit = Math.max(1, Math.min(maxPages, 20));
        int effectiveMaxDepth = Math.max(0, maxCrawlDepth);
        Set<String> targetFields = ConfidenceScoringService.parseAndNormalizeRequiredFields(requiredFields);

        log.info("Starting multi-page crawl on root: {} (domain: {}, maxPages: {}, maxCrawlDepth: {}, targetFields: {})",
                normalizedRoot, rootDomain, limit, effectiveMaxDepth, targetFields);

        // Pre-load robots.txt for domain
        fetchAndCacheRobots(normalizedRoot, rootDomain);

        Set<String> visitedUrls = new HashSet<>();
        Queue<CrawlQueueItem> crawlQueue = new LinkedList<>();
        crawlQueue.add(new CrawlQueueItem(normalizedRoot, 0));

        List<PageExtractDto> pageExtracts = new ArrayList<>();
        Set<String> allEmails = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Set<String> allPhones = new LinkedHashSet<>();
        Set<String> allWhatsAppNumbers = new LinkedHashSet<>();
        Map<String, String> allSocialLinks = new LinkedHashMap<>();
        Set<String> allAddresses = new LinkedHashSet<>();
        Document homepageDoc = null;

        while (!crawlQueue.isEmpty() && visitedUrls.size() < limit) {
            CrawlQueueItem currentItem = crawlQueue.poll();
            String currentUrl = currentItem.getUrl();
            int currentDepth = currentItem.getDepth();

            if (currentDepth > effectiveMaxDepth) {
                continue;
            }

            if (visitedUrls.contains(currentUrl)) {
                continue;
            }

            // robots.txt compliance: skip disallowed paths
            if (isRobotsDisallowed(currentUrl, rootDomain)) {
                log.debug("[ROBOTS] Skipping disallowed URL: {}", currentUrl);
                continue;
            }

            visitedUrls.add(currentUrl);
            log.debug("Crawling page {}/{} (depth {}): {}", visitedUrls.size(), limit, currentDepth, currentUrl);

            // Domain rate limiting: enforce minimum delay between requests
            applyDomainRateLimit(rootDomain);

            Connection.Response response = null;
            Document doc = null;
            boolean blocked = false;

            // Bounded retry on transient 5xx / timeout
            for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
                try {
                    response = Jsoup.connect(currentUrl)
                            .userAgent(USER_AGENT)
                            .timeout(TIMEOUT_MILLIS)
                            .followRedirects(true)
                            .ignoreHttpErrors(true) // handle HTTP errors manually
                            .execute();

                    int statusCode = response.statusCode();

                    // CAPTCHA / access denied detection
                    if (statusCode == 403 || statusCode == 429) {
                        log.warn("[BLOCKED] {} returned HTTP {} — CAPTCHA or rate-limit detected, skipping domain crawl.", currentUrl, statusCode);
                        blocked = true;
                        break;
                    }

                    // Server error — retry
                    if (statusCode >= 500 && attempt < MAX_RETRIES) {
                        log.warn("[RETRY {}/{}] {} returned HTTP {}", attempt + 1, MAX_RETRIES, currentUrl, statusCode);
                        Thread.sleep(600L * (attempt + 1));
                        continue;
                    }

                    if (statusCode >= 400) {
                        log.debug("Skipping {} — HTTP {}", currentUrl, statusCode);
                        break;
                    }

                    doc = response.parse();
                    break;

                } catch (IOException e) {
                    if (attempt < MAX_RETRIES) {
                        log.warn("[RETRY {}/{}] IOException on {}: {}", attempt + 1, MAX_RETRIES, currentUrl, e.getMessage());
                        try { Thread.sleep(600L * (attempt + 1)); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    } else {
                        log.warn("[FAILED] Gave up after {} retries for {}: {}", MAX_RETRIES, currentUrl, e.getMessage());
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // If blocked by CAPTCHA/WAF, abort entire domain crawl
            if (blocked) {
                PageExtractDto blockedDto = new PageExtractDto(
                        currentUrl, classifyPageType(currentUrl),
                        "BLOCKED: CAPTCHA or rate-limit (403/429)",
                        403, Collections.emptySet(), Collections.emptySet(),
                        Collections.emptyMap(), Collections.emptySet(), LocalDateTime.now());
                pageExtracts.add(blockedDto);
                break; // stop crawling this domain
            }

            if (doc != null) {
                try {
                    String title = doc.title() != null ? doc.title().trim() : "";
                    int status = response != null ? response.statusCode() : 0;
                    SourcePageType pageType = classifyPageType(currentUrl);

                    // Playwright JS fallback: detect empty JS-rendered shells (SPA)
                    String bodyText = doc.body() != null ? doc.body().text().trim() : "";
                    boolean isJsShell = bodyText.length() < 100
                            && (doc.selectFirst("div#root, div#app, div#__next") != null)
                            && doc.select("script").size() > 2;
                    if (isJsShell) {
                        log.info("[JS_SHELL] Page {} is a JavaScript-rendered SPA shell ({} chars body). Invoking Playwright fallback...",
                                currentUrl, bodyText.length());
                        Document renderedDoc = playwrightScraperService != null ? playwrightScraperService.renderPage(currentUrl) : null;
                        if (renderedDoc != null) {
                            doc = renderedDoc;
                            title = doc.title() != null ? doc.title().trim() : title;
                            log.info("[PLAYWRIGHT_RENDER_SUCCESS] Successfully rendered SPA page {}", currentUrl);
                        } else {
                            log.warn("[JS_SHELL] Playwright could not render {} — skipping JS shell page.", currentUrl);
                            PageExtractDto jsShellDto = new PageExtractDto(
                                    currentUrl, pageType,
                                    "JS-rendered shell (SPA) — Playwright render unavailable",
                                    status, Collections.emptySet(), Collections.emptySet(),
                                    Collections.emptyMap(), Collections.emptySet(), LocalDateTime.now());
                            pageExtracts.add(jsShellDto);
                            continue;
                        }
                    }

                    if (homepageDoc == null && currentUrl.equals(normalizedRoot)) {
                        homepageDoc = doc;
                    }

                    Set<String> pageEmails = extractEmails(doc);
                    Set<String> pagePhones = extractPhoneNumbers(doc);
                    Set<String> pageWhatsApp = extractWhatsAppNumbers(doc);
                    Map<String, String> pageSocial = extractSocialLinks(doc);
                    Set<String> pageAddresses = extractAddresses(doc);

                    allEmails.addAll(pageEmails);
                    allPhones.addAll(pagePhones);
                    allWhatsAppNumbers.addAll(pageWhatsApp);
                    allSocialLinks.putAll(pageSocial);
                    allAddresses.addAll(pageAddresses);

                    PageExtractDto pageDto = new PageExtractDto(
                            currentUrl, pageType, title, status,
                            pageEmails, pagePhones, pageSocial, pageAddresses, LocalDateTime.now());
                    pageExtracts.add(pageDto);

                    // Discover and enqueue additional internal links (only while currentDepth < effectiveMaxDepth and not at limit)
                    if (currentDepth < effectiveMaxDepth && visitedUrls.size() < limit) {
                        List<String> discoveredLinks = discoverInternalLinks(doc, currentUrl, rootDomain, targetFields);
                        for (String link : discoveredLinks) {
                            if (!visitedUrls.contains(link) && crawlQueue.stream().noneMatch(q -> q.getUrl().equals(link))) {
                                crawlQueue.add(new CrawlQueueItem(link, currentDepth + 1));
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error processing crawled page {}: {}", currentUrl, e.getMessage());
                }
            } else if (!blocked) {
                // Fetch failed after retries
                PageExtractDto failedDto = new PageExtractDto(
                        currentUrl, classifyPageType(currentUrl),
                        "Failed to load after retries",
                        0, Collections.emptySet(), Collections.emptySet(),
                        Collections.emptyMap(), Collections.emptySet(), LocalDateTime.now());
                pageExtracts.add(failedDto);
            }
        }

        // Extract structured organization identity from homepage doc / crawled pages
        OrganizationIdentity identity = extractOrganizationIdentity(homepageDoc, pageExtracts, rootDomain, normalizedRoot);

        log.info("Finished crawl for {}: {} pages visited, identity status: {} ('{}'), {} emails, {} phones, {} whatsapp",
                normalizedRoot, pageExtracts.size(), identity.getIdentityStatus(), identity.getExtractedBusinessName(),
                allEmails.size(), allPhones.size(), allWhatsAppNumbers.size());

        return new CrawlWebsiteResponse(
                normalizedRoot,
                rootDomain,
                pageExtracts.size(),
                pageExtracts,
                allEmails,
                allPhones,
                allWhatsAppNumbers,
                allSocialLinks,
                allAddresses,
                identity,
                LocalDateTime.now()
        );
    }

    // -----------------------------------------------------------------------
    // Responsible Crawling: robots.txt, rate limiting helpers
    // -----------------------------------------------------------------------

    /**
     * Fetches and caches robots.txt disallowed paths for the given domain.
     * Only the "*" or "LeadDiscoveryBot" user-agent directives are respected.
     */
    private void fetchAndCacheRobots(String rootUrl, String domain) {
        if (robotsCache.containsKey(domain)) return;

        Set<String> disallowed = new HashSet<>();
        try {
            String robotsUrl = rootUrl.replaceAll("(/[^/].*)?$", "") + "/robots.txt";
            // Derive base URL (scheme + host)
            URI uri = URI.create(rootUrl);
            robotsUrl = uri.getScheme() + "://" + uri.getHost() + "/robots.txt";

            Connection.Response response = Jsoup.connect(robotsUrl)
                    .userAgent(USER_AGENT)
                    .timeout(5000)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .execute();

            if (response.statusCode() == 200) {
                String body = response.body();
                boolean relevantSection = false;
                for (String line : body.split("\n")) {
                    line = line.trim();
                    if (line.startsWith("User-agent:")) {
                        String agent = line.substring(11).trim().toLowerCase(Locale.ROOT);
                        relevantSection = agent.equals("*") || agent.contains("leaddiscovery");
                    } else if (relevantSection && line.startsWith("Disallow:")) {
                        String path = line.substring(9).trim();
                        if (!path.isEmpty()) {
                            disallowed.add(path);
                        }
                    }
                }
                log.debug("[ROBOTS] {} — {} disallowed paths for domain {}", robotsUrl, disallowed.size(), domain);
            }
        } catch (Exception e) {
            log.debug("[ROBOTS] Could not fetch robots.txt for domain {}: {}", domain, e.getMessage());
        }

        robotsCache.put(domain, disallowed);
    }

    /**
     * Returns true if the URL's path is disallowed by the cached robots.txt rules.
     */
    private boolean isRobotsDisallowed(String url, String domain) {
        Set<String> disallowed = robotsCache.getOrDefault(domain, Collections.emptySet());
        if (disallowed.isEmpty()) return false;
        try {
            String path = URI.create(url).getPath();
            if (path == null) path = "/";
            for (String rule : disallowed) {
                if (path.startsWith(rule)) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    /**
     * Enforces a minimum delay (DOMAIN_RATE_LIMIT_MS) between consecutive requests to the same domain.
     */
    private void applyDomainRateLimit(String domain) {
        long last = domainLastRequestTime.getOrDefault(domain, 0L);
        long elapsed = System.currentTimeMillis() - last;
        if (elapsed < DOMAIN_RATE_LIMIT_MS) {
            try {
                Thread.sleep(DOMAIN_RATE_LIMIT_MS - elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        domainLastRequestTime.put(domain, System.currentTimeMillis());
    }

    /**
     * Extracts structured organization identity using multiple signals:
     * 1. JSON-LD Organization / LocalBusiness Schema
     * 2. OpenGraph og:site_name & application-name meta tags
     * 3. Cleaned Homepage Title (stripping suffixes, locations, slogans)
     * 4. Brand header logo & Footer copyright text
     * 5. Fallback clean domain name
     */
    public OrganizationIdentity extractOrganizationIdentity(Document doc, List<PageExtractDto> pages, String rootDomain, String startUrl) {
        if (rootDomain == null || rootDomain.isBlank()) {
            return OrganizationIdentity.rejected("No official domain provided", rootDomain);
        }

        if (doc != null) {
            // Signal 1: JSON-LD Structured Data Schema
            String jsonLdName = extractJsonLdOrganizationName(doc);
            if (jsonLdName != null && !jsonLdName.isBlank() && !UrlFilterUtils.isGenericTitle(jsonLdName)
                    && !UrlFilterUtils.isListicleOrDirectory(jsonLdName, null)) {
                log.debug("Found verified organization identity from JSON-LD schema: '{}'", jsonLdName);
                return OrganizationIdentity.confirmed(jsonLdName.trim(), rootDomain, "JSON_LD_ORGANIZATION", 0.95);
            }

            // Signal 2: OpenGraph og:site_name or application-name meta tags
            String ogSiteName = extractOgSiteName(doc);
            if (ogSiteName != null && !ogSiteName.isBlank() && !UrlFilterUtils.isGenericTitle(ogSiteName)
                    && !UrlFilterUtils.isListicleOrDirectory(ogSiteName, null)) {
                log.debug("Found verified organization identity from OpenGraph metadata: '{}'", ogSiteName);
                return OrganizationIdentity.confirmed(ogSiteName.trim(), rootDomain, "OPENGRAPH_SITE_NAME", 0.90);
            }

            // Check if the candidate URL or homepage title is a listicle or directory roundup when no JSON-LD / og:site_name
            String homepageTitle = doc.title() != null ? doc.title().trim() : "";
            if (UrlFilterUtils.isListicleOrDirectory(homepageTitle, startUrl)) {
                return OrganizationIdentity.rejected("Candidate website is a listicle or directory roundup: " + homepageTitle, rootDomain);
            }

            // Signal 3: Header Logo Alt / Site Brand Tag
            String brandLogoName = extractBrandFromHeader(doc);
            if (brandLogoName != null && !brandLogoName.isBlank() && !UrlFilterUtils.isGenericTitle(brandLogoName)
                    && !UrlFilterUtils.isListicleOrDirectory(brandLogoName, null)) {
                log.debug("Found organization identity from header/brand logo: '{}'", brandLogoName);
                return OrganizationIdentity.confirmed(brandLogoName.trim(), rootDomain, "BRAND_HEADER_LOGO", 0.85);
            }

            // Signal 4: Footer Copyright / Business entity name
            String footerName = extractBrandFromFooter(doc);
            if (footerName != null && !footerName.isBlank() && !UrlFilterUtils.isGenericTitle(footerName)
                    && !UrlFilterUtils.isListicleOrDirectory(footerName, null)) {
                log.debug("Found organization identity from footer copyright: '{}'", footerName);
                return OrganizationIdentity.confirmed(footerName.trim(), rootDomain, "BRAND_FOOTER_COPYRIGHT", 0.85);
            }

            // Signal 5: Homepage Title Cleaning
            if (!homepageTitle.isBlank()) {
                String cleanedTitle = UrlFilterUtils.cleanBrandFromTitle(homepageTitle, rootDomain);
                if (!cleanedTitle.isBlank() && !UrlFilterUtils.isGenericTitle(cleanedTitle)
                        && !UrlFilterUtils.isListicleOrDirectory(cleanedTitle, null)) {
                    // Check if cleaned title reasonably corresponds to the root domain
                    String domainRoot = rootDomain.split("\\.")[0].toLowerCase(Locale.ROOT);
                    if (cleanedTitle.toLowerCase(Locale.ROOT).contains(domainRoot) ||
                            domainRoot.contains(cleanedTitle.toLowerCase(Locale.ROOT).replaceAll("\\s+", ""))) {
                        return OrganizationIdentity.confirmed(cleanedTitle, rootDomain, "HOMEPAGE_BRAND_TITLE", 0.80);
                    }
                    return OrganizationIdentity.uncertain(cleanedTitle, rootDomain, "HOMEPAGE_TITLE_UNCONFIRMED", 0.60,
                            "Title extracted from homepage but lacks structured JSON-LD or OpenGraph identity confirmation.");
                }
            }
        }

        // Fallback: Format clean domain name into a readable brand
        String domainBrand = UrlFilterUtils.formatDomainAsBrand(rootDomain);
        if (!domainBrand.isBlank()) {
            return OrganizationIdentity.uncertain(domainBrand, rootDomain, "DOMAIN_NAME_FALLBACK", 0.40,
                    "Identity inferred from domain name because no structured organization metadata was found.");
        }

        return OrganizationIdentity.rejected("Unable to establish organization identity from webpage content", rootDomain);
    }

    /**
     * Parses JSON-LD <script type="application/ld+json"> tags for Organization or LocalBusiness schemas.
     */
    public String extractJsonLdOrganizationName(Document doc) {
        Elements scriptTags = doc.select("script[type=application/ld+json]");
        for (Element script : scriptTags) {
            String jsonContent = script.data();
            if (jsonContent == null || jsonContent.isBlank()) {
                jsonContent = script.html();
            }
            if (jsonContent == null || jsonContent.isBlank()) {
                continue;
            }

            try {
                JsonNode rootNode = objectMapper.readTree(jsonContent);
                String name = parseJsonNodeForOrganizationName(rootNode);
                if (name != null && !name.isBlank()) {
                    return name;
                }
            } catch (Exception e) {
                log.debug("Failed to parse JSON-LD script block: {}", e.getMessage());
            }
        }
        return null;
    }

    private String parseJsonNodeForOrganizationName(JsonNode node) {
        if (node == null) return null;

        if (node.isArray()) {
            for (JsonNode item : node) {
                String res = parseJsonNodeForOrganizationName(item);
                if (res != null) return res;
            }
        } else if (node.isObject()) {
            // Check @graph array if present
            if (node.has("@graph") && node.get("@graph").isArray()) {
                for (JsonNode graphItem : node.get("@graph")) {
                    String res = parseJsonNodeForOrganizationName(graphItem);
                    if (res != null) return res;
                }
            }

            // Check @type
            if (node.has("@type")) {
                JsonNode typeNode = node.get("@type");
                String typeStr = typeNode.asText("");
                if (typeNode.isArray()) {
                    StringBuilder sb = new StringBuilder();
                    typeNode.forEach(t -> sb.append(t.asText()).append(" "));
                    typeStr = sb.toString();
                }

                String lowerType = typeStr.toLowerCase(Locale.ROOT);
                if (lowerType.contains("organization") || lowerType.contains("localbusiness") ||
                        lowerType.contains("corporation") || lowerType.contains("professionalservice") ||
                        lowerType.contains("store") || lowerType.contains("company")) {

                    if (node.has("name") && !node.get("name").asText().isBlank()) {
                        return node.get("name").asText().trim();
                    }
                    if (node.has("legalName") && !node.get("legalName").asText().isBlank()) {
                        return node.get("legalName").asText().trim();
                    }
                    if (node.has("headline") && !node.get("headline").asText().isBlank()) {
                        String headline = node.get("headline").asText().trim();
                        if (!UrlFilterUtils.isGenericTitle(headline)) {
                            return headline;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Extracts company name from OpenGraph or application metadata tags.
     */
    public String extractOgSiteName(Document doc) {
        Element ogSiteName = doc.selectFirst("meta[property=og:site_name], meta[name=og:site_name], meta[name=application-name]");
        if (ogSiteName != null) {
            String content = ogSiteName.attr("content");
            if (content != null && !content.isBlank()) {
                return content.trim();
            }
        }
        return null;
    }

    /**
     * Extracts brand name from header logos or navbar branding.
     */
    public String extractBrandFromHeader(Document doc) {
        Element logoImg = doc.selectFirst("header img.logo, .navbar-brand img, .header-logo img, a.logo img, .site-logo img");
        if (logoImg != null) {
            String alt = logoImg.attr("alt");
            if (alt != null && !alt.isBlank() && !alt.equalsIgnoreCase("logo") && !alt.equalsIgnoreCase("image")) {
                String cleanAlt = alt.replaceAll("(?i)\\s*logo$", "").trim();
                if (cleanAlt.length() >= 2 && !UrlFilterUtils.isGenericTitle(cleanAlt)) {
                    return cleanAlt;
                }
            }
        }

        Element navBrandText = doc.selectFirst("header .navbar-brand, .site-title, .header__logo-text, .brand-name");
        if (navBrandText != null) {
            String text = navBrandText.text().trim();
            if (text.length() >= 2 && text.length() <= 50 && !UrlFilterUtils.isGenericTitle(text)) {
                return text;
            }
        }
        return null;
    }

    /**
     * Extracts registered business name from footer copyright text.
     */
    public String extractBrandFromFooter(Document doc) {
        Elements footers = doc.select("footer, .footer, #footer, .copyright, .footer-bottom");
        for (Element footer : footers) {
            String text = footer.text();
            if (text.contains("©") || text.toLowerCase(Locale.ROOT).contains("copyright")) {
                Matcher matcher = COPYRIGHT_PATTERN.matcher(text);
                if (matcher.find()) {
                    String candidate = matcher.group(1).trim();
                    candidate = candidate.replaceAll("(?i)^(\\d{4}|all rights reserved|by)\\s*", "").trim();
                    if (candidate.length() >= 3 && candidate.length() <= 60 && !UrlFilterUtils.isGenericTitle(candidate)) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Discovers internal links from document, prioritizes contact/about/team links,
     * and filters out external domains or non-crawlable media.
     */
    public List<String> discoverInternalLinks(Document doc, String baseUrl, String rootDomain) {
        return discoverInternalLinks(doc, baseUrl, rootDomain, Collections.emptySet());
    }

    /**
     * Discovers internal links from document, prioritizes links based on user requested targetFields,
     * and filters out external domains or non-crawlable media.
     */
    public List<String> discoverInternalLinks(Document doc, String baseUrl, String rootDomain, Set<String> targetFields) {
        Elements anchors = doc.select("a[href]");
        Set<String> uniqueInternalUrls = new HashSet<>();

        for (Element anchor : anchors) {
            String absUrl = anchor.absUrl("href");
            if (absUrl == null || absUrl.isBlank()) {
                continue;
            }

            String normalized = normalizeUrl(absUrl, baseUrl);
            if (normalized != null && isSameDomain(normalized, rootDomain) && isCrawlableUrl(normalized)) {
                uniqueInternalUrls.add(normalized);
            }
        }

        List<String> sortedLinks = new ArrayList<>(uniqueInternalUrls);
        sortedLinks.sort(Comparator.comparingInt((String url) -> getPageTypePriorityScore(url, targetFields)).reversed());
        return sortedLinks;
    }

    /**
     * Extracts social media profile links (LinkedIn, Twitter/X, Facebook, Instagram, YouTube),
     * filtering out sharing/intent dialog URLs.
     */
    public Map<String, String> extractSocialLinks(Document doc) {
        Map<String, String> socialLinks = new LinkedHashMap<>();
        Elements links = doc.select("a[href]");

        for (Element link : links) {
            String href = link.absUrl("href");
            if (href == null || href.isBlank()) {
                href = link.attr("href");
            }
            if (href == null || href.isBlank() || href.startsWith("#") || href.startsWith("javascript:")) {
                continue;
            }

            String lower = href.toLowerCase(Locale.ROOT);

            if (lower.contains("linkedin.com") && !lower.contains("/sharing") && !lower.contains("/sharearticle")) {
                if (lower.contains("/company/") || lower.contains("/in/") || lower.contains("/school/")) {
                    socialLinks.putIfAbsent("linkedin", cleanSocialUrl(href));
                }
            } else if ((lower.contains("twitter.com") || lower.contains("x.com"))
                    && !lower.contains("/intent/") && !lower.contains("/share") && !lower.contains("/widgets")) {
                socialLinks.putIfAbsent("twitter", cleanSocialUrl(href));
            } else if (lower.contains("facebook.com")
                    && !lower.contains("/sharer") && !lower.contains("/share.php") && !lower.contains("/tr?")) {
                socialLinks.putIfAbsent("facebook", cleanSocialUrl(href));
            } else if (lower.contains("instagram.com") && !lower.contains("/p/")) {
                socialLinks.putIfAbsent("instagram", cleanSocialUrl(href));
            } else if (lower.contains("youtube.com") && !lower.contains("/embed/") && !lower.contains("/watch?v=")) {
                socialLinks.putIfAbsent("youtube", cleanSocialUrl(href));
            }
        }

        return socialLinks;
    }

    /**
     * Extracts physical addresses heuristically from address tags and structured classes.
     */
    public Set<String> extractAddresses(Document doc) {
        Set<String> addresses = new LinkedHashSet<>();

        Elements addressTags = doc.select("address");
        for (Element el : addressTags) {
            String text = cleanAddressText(el.text());
            if (isValidAddress(text)) {
                addresses.add(text);
            }
        }

        Elements addressClasses = doc.select("[itemprop=address], .address, .contact-address, .footer-address, .location-address");
        for (Element el : addressClasses) {
            String text = cleanAddressText(el.text());
            if (isValidAddress(text)) {
                addresses.add(text);
            }
        }

        return addresses;
    }

    public Set<String> extractEmails(Document doc) {
        Set<String> emails = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        Elements mailtoLinks = doc.select("a[href^=mailto:]");
        for (Element link : mailtoLinks) {
            String href = link.attr("href");
            String email = href.substring(7); // Remove 'mailto:'
            if (email.contains("?")) {
                email = email.substring(0, email.indexOf("?"));
            }
            email = decodeUrl(email).trim().toLowerCase(Locale.ROOT);
            if (isValidEmail(email)) {
                emails.add(email);
            }
        }

        String content = doc.html();
        Matcher matcher = EMAIL_PATTERN.matcher(content);
        while (matcher.find()) {
            String email = matcher.group().trim().toLowerCase(Locale.ROOT);
            if (isValidEmail(email)) {
                emails.add(email);
            }
        }

        return emails;
    }

    public Set<String> extractPhoneNumbers(Document doc) {
        Set<String> phoneNumbers = new LinkedHashSet<>();

        Elements telLinks = doc.select("a[href^=tel:]");
        for (Element link : telLinks) {
            String href = link.attr("href");
            String rawPhone = href.substring(4); // Remove 'tel:'
            if (rawPhone.contains("?")) {
                rawPhone = rawPhone.substring(0, rawPhone.indexOf("?"));
            }
            rawPhone = decodeUrl(rawPhone).trim();
            String cleaned = cleanPhoneNumber(rawPhone);
            if (isValidPhoneNumber(cleaned)) {
                phoneNumbers.add(cleaned);
            }
        }

        String bodyText = doc.body() != null ? doc.body().text() : doc.text();
        Matcher matcher = PHONE_PATTERN.matcher(bodyText);
        while (matcher.find()) {
            String candidate = matcher.group().trim();
            String cleaned = cleanPhoneNumber(candidate);
            if (isValidPhoneNumber(cleaned)) {
                phoneNumbers.add(cleaned);
            }
        }

        return phoneNumbers;
    }

    /**
     * Extracts WhatsApp contact numbers from wa.me, api.whatsapp.com/send, and whatsapp:// links.
     * Returns normalized phone numbers (digits + leading +) only.
     */
    public Set<String> extractWhatsAppNumbers(Document doc) {
        Set<String> whatsappNumbers = new LinkedHashSet<>();
        Elements links = doc.select("a[href]");

        for (Element link : links) {
            String href = link.absUrl("href");
            if (href == null || href.isBlank()) {
                href = link.attr("href");
            }
            if (href == null || href.isBlank()) continue;

            String lower = href.toLowerCase(Locale.ROOT);
            String rawNumber = null;

            // wa.me/<number>
            if (lower.contains("wa.me/")) {
                int idx = lower.indexOf("wa.me/") + 6;
                String rest = href.substring(idx).split("[/?#]")[0].trim();
                rawNumber = rest.replaceAll("[^0-9+]", "");
            }
            // api.whatsapp.com/send?phone=<number>
            else if (lower.contains("api.whatsapp.com/send") && lower.contains("phone=")) {
                try {
                    URI uri = URI.create(href);
                    String query = uri.getQuery();
                    if (query != null) {
                        for (String param : query.split("&")) {
                            if (param.toLowerCase(Locale.ROOT).startsWith("phone=")) {
                                rawNumber = param.substring(6).replaceAll("[^0-9+]", "");
                                break;
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
            // whatsapp://send?phone=<number>
            else if (lower.startsWith("whatsapp://send") && lower.contains("phone=")) {
                int idx = lower.indexOf("phone=") + 6;
                String rest = href.substring(idx).split("[&]")[0].trim();
                rawNumber = rest.replaceAll("[^0-9+]", "");
            }

            if (rawNumber != null && rawNumber.length() >= 7 && rawNumber.length() <= 16) {
                if (!rawNumber.startsWith("+")) rawNumber = "+" + rawNumber;
                whatsappNumbers.add(rawNumber);
            }
        }

        return whatsappNumbers;
    }

    public SourcePageType classifyPageType(String url) {
        if (url == null || url.isBlank()) {
            return SourcePageType.OTHER;
        }

        try {
            URI uri = URI.create(url);
            String path = (uri.getPath() != null ? uri.getPath() : "").toLowerCase(Locale.ROOT);

            if (path.isEmpty() || path.equals("/") || path.endsWith("/index.html") || path.endsWith("/index.php")) {
                return SourcePageType.HOME;
            }

            if (path.contains("contact") || path.contains("reach-us") || path.contains("get-in-touch")
                    || path.contains("branch") || path.contains("location") || path.contains("enquiry")) {
                return SourcePageType.CONTACT;
            }

            if (path.contains("about") || path.contains("who-we-are") || path.contains("overview")
                    || path.contains("history") || path.contains("company-profile")) {
                return SourcePageType.ABOUT;
            }

            if (path.contains("team") || path.contains("management") || path.contains("faculty")
                    || path.contains("leadership") || path.contains("directors") || path.contains("admission")) {
                return SourcePageType.TEAM;
            }

            if (path.contains("service") || path.contains("solution") || path.contains("product")
                    || path.contains("course") || path.contains("program")) {
                return SourcePageType.SERVICES;
            }

            return SourcePageType.OTHER;
        } catch (Exception e) {
            return SourcePageType.OTHER;
        }
    }

    public int getPageTypePriorityScore(String url) {
        return getPageTypePriorityScore(url, Collections.emptySet());
    }

    public int getPageTypePriorityScore(String url, Set<String> targetFields) {
        SourcePageType type = classifyPageType(url);
        int baseScore = switch (type) {
            case CONTACT -> 10;
            case ABOUT -> 8;
            case TEAM -> 7;
            case SERVICES -> 5;
            case HOME -> 3;
            case OTHER -> 1;
        };

        if (targetFields == null || targetFields.isEmpty()) {
            return baseScore;
        }

        int score = baseScore;
        String lowerUrl = url != null ? url.toLowerCase(Locale.ROOT) : "";

        // If contact details (email, phone, whatsapp, address, pincode) requested, boost CONTACT pages
        if (targetFields.contains("email") || targetFields.contains("phone")
                || targetFields.contains("whatsapp") || targetFields.contains("address")
                || targetFields.contains("pincode")) {
            if (type == SourcePageType.CONTACT) {
                score += 8;
            }
        }

        // If team / contact person / leadership requested, boost TEAM and ABOUT pages
        if (targetFields.contains("contact_person") || targetFields.contains("contact_role")
                || targetFields.contains("team") || targetFields.contains("leadership")) {
            if (type == SourcePageType.TEAM) {
                score += 10;
            } else if (type == SourcePageType.ABOUT) {
                score += 4;
            }
        }

        // If WhatsApp requested, boost pages with wa/chat/reach/help indicators
        if (targetFields.contains("whatsapp")) {
            if (lowerUrl.contains("wa") || lowerUrl.contains("chat") || lowerUrl.contains("help") || lowerUrl.contains("support")) {
                score += 6;
            }
        }

        // If social links requested, boost contact, about, and home
        if (targetFields.contains("social_links") || targetFields.contains("social")) {
            if (type == SourcePageType.CONTACT || type == SourcePageType.ABOUT || type == SourcePageType.HOME) {
                score += 3;
            }
        }

        return score;
    }

    public String extractDomain(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null) {
                return "";
            }
            host = host.toLowerCase(Locale.ROOT);
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (Exception e) {
            return "";
        }
    }

    public boolean isSameDomain(String url, String rootDomain) {
        if (url == null || rootDomain == null || rootDomain.isBlank()) {
            return false;
        }
        String host = extractDomain(url);
        return host.equals(rootDomain) || host.endsWith("." + rootDomain);
    }

    public String normalizeUrl(String rawUrl, String baseUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }
        try {
            URI base = URI.create(baseUrl);
            URI resolved = base.resolve(rawUrl.trim());

            String scheme = resolved.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                return null;
            }

            String host = resolved.getHost();
            if (host == null || host.isBlank()) {
                return null;
            }

            String path = resolved.getPath();
            if (path == null || path.isBlank()) {
                path = "/";
            } else if (path.length() > 1 && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }

            String query = resolved.getQuery();
            if (query != null && !query.isBlank()) {
                String[] params = query.split("&");
                StringBuilder cleanQuery = new StringBuilder();
                for (String p : params) {
                    String paramName = p.split("=")[0].toLowerCase(Locale.ROOT);
                    if (!paramName.startsWith("utm_") && !paramName.equals("fbclid") && !paramName.equals("gclid") && !paramName.equals("ref")) {
                        if (cleanQuery.length() > 0) cleanQuery.append("&");
                        cleanQuery.append(p);
                    }
                }
                query = cleanQuery.length() > 0 ? "?" + cleanQuery : "";
            } else {
                query = "";
            }

            int port = resolved.getPort();
            String portPart = (port != -1 && port != 80 && port != 443) ? ":" + port : "";

            return scheme.toLowerCase(Locale.ROOT) + "://" + host.toLowerCase(Locale.ROOT) + portPart + path + query;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isCrawlableUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String lower = url.toLowerCase(Locale.ROOT);
        for (String ext : NON_HTML_EXTENSIONS) {
            if (lower.endsWith(ext) || lower.contains(ext + "?")) {
                return false;
            }
        }
        return true;
    }

    public Set<String> extractEmailsFromHtml(String html) {
        Document doc = Jsoup.parse(html);
        return extractEmails(doc);
    }

    public Set<String> extractPhoneNumbersFromHtml(String html) {
        Document doc = Jsoup.parse(html);
        return extractPhoneNumbers(doc);
    }

    public Map<String, String> extractSocialLinksFromHtml(String html) {
        Document doc = Jsoup.parse(html);
        return extractSocialLinks(doc);
    }

    public Set<String> extractAddressesFromHtml(String html) {
        Document doc = Jsoup.parse(html);
        return extractAddresses(doc);
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isBlank() || email.length() > 254) {
            return false;
        }
        int lastDotIndex = email.lastIndexOf('.');
        if (lastDotIndex != -1 && lastDotIndex < email.length() - 1) {
            String ext = email.substring(lastDotIndex + 1).toLowerCase(Locale.ROOT);
            if (INVALID_EMAIL_EXTENSIONS.contains(ext)) {
                return false;
            }
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    private String cleanPhoneNumber(String phone) {
        if (phone == null) {
            return "";
        }
        String trimmed = phone.trim();
        boolean hasPlus = trimmed.startsWith("+");
        String digitsOnly = trimmed.replaceAll("[^0-9]", "");
        return hasPlus ? "+" + digitsOnly : digitsOnly;
    }

    private boolean isValidPhoneNumber(String phone) {
        if (phone == null || phone.isBlank()) {
            return false;
        }
        String digitsOnly = phone.replaceAll("[^0-9]", "");
        int len = digitsOnly.length();
        if (len < 7 || len > 15) {
            return false;
        }
        if (digitsOnly.matches("^(\\d)\\1+$")) {
            return false;
        }
        return true;
    }

    private String cleanSocialUrl(String url) {
        if (url == null) return "";
        String trimmed = url.trim();
        if (trimmed.contains("?")) {
            trimmed = trimmed.substring(0, trimmed.indexOf("?"));
        }
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String cleanAddressText(String text) {
        if (text == null) return "";
        return text.replaceAll("\\s+", " ").trim();
    }

    private boolean isValidAddress(String text) {
        if (text == null || text.length() < 10 || text.length() > 250) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        return (lower.contains("st") || lower.contains("road") || lower.contains("street") || lower.contains("ave")
                || lower.contains("blvd") || lower.contains("lane") || lower.contains("nagar") || lower.contains("floor")
                || lower.contains("box") || lower.contains("pin") || lower.contains("zip") || lower.matches(".*\\d{3,}.*"));
    }

    private String decodeUrl(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }
}
