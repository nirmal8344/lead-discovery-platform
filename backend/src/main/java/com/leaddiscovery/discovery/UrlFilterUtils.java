package com.leaddiscovery.discovery;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class UrlFilterUtils {

    private UrlFilterUtils() {
    }

    private static final Set<String> EXCLUDED_DOMAINS = Set.of(
            "facebook.com", "instagram.com", "linkedin.com", "twitter.com", "x.com",
            "youtube.com", "pinterest.com", "tiktok.com", "reddit.com", "wikipedia.org",
            "github.com", "gitlab.com", "medium.com", "quora.com", "google.com",
            "bing.com", "yahoo.com", "duckduckgo.com", "baidu.com", "yandex.com",
            "tripadvisor.com", "yelp.com", "yellowpages.com", "justdial.com", "sulekha.com",
            "indiamart.com", "tradeindia.com", "crunchbase.com", "glassdoor.com", "indeed.com",
            "citybusinessdirectory.in", "businessdirectory.com", "clutch.co", "goodfirms.co",
            "zoominfo.com", "mapsofindia.com", "dial4trade.com", "topcompanies.com",
            "designrush.com", "upcity.com", "g2.com", "capterra.com", "softwareadvice.com",
            "trustpilot.com", "mouthshut.com", "ambitionbox.com", "foundit.in", "naukri.com",
            "shine.com", "freshersworld.com", "glassdoor.co.in", "indiaonline.in", "askdaman.com",
            "marketmystique.com", "buzz4ai.com", "lexifo.com", "brandveda.in", "wikimapia.org",
            "wordpress.com", "blogger.com", "blogspot.com", "wixsite.com"
    );

    private static final Set<String> NON_WEBSITE_EXTENSIONS = Set.of(
            ".pdf", ".jpg", ".jpeg", ".png", ".gif", ".svg", ".webp", ".ico", ".mp4", ".mp3",
            ".avi", ".mov", ".zip", ".tar", ".gz", ".rar", ".7z", ".doc", ".docx", ".xls",
            ".xlsx", ".ppt", ".pptx", ".csv", ".xml", ".json", ".js", ".css", ".woff", ".woff2", ".ttf"
    );

    private static final Pattern LISTICLE_TITLE_PATTERN = Pattern.compile(
            "^(?:\\d+\\s+)?(?:top|best|list of|leading|popular|ranking|find|reviews of|guide to)\\s+.*(?:companies|agencies|firms|services|hospitals|schools|colleges|developers|businesses|near me|in\\s+[a-zA-Z\\s]+).*$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern LISTICLE_PATH_PATTERN = Pattern.compile(
            "/(?:blog|blogs|article|articles|news|post|posts|list-of|top-\\d+|best-\\d+|ranking|directory|categories|category|tag|tags|companies-in|it-companies|software-companies|near-me|find)(?:/.*)?$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Set<String> GENERIC_PAGE_TITLES = Set.of(
            "home", "home page", "homepage", "welcome", "welcome to our website",
            "about", "about us", "about company", "who we are", "company profile",
            "contact", "contact us", "reach us", "get in touch", "enquiry",
            "services", "our services", "what we do", "solutions", "products",
            "privacy policy", "terms and conditions", "disclaimer", "careers",
            "login", "sign in", "register", "signup", "dashboard", "index",
            "software development", "software company", "it companies", "it solutions"
    );

    /**
     * Normalizes a raw URL by decoding wrappers, trimming, removing fragments and tracking query params.
     */
    public static String normalizeUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }

        String unescaped = unescapeSearchRedirect(rawUrl.trim());
        if (unescaped == null || unescaped.isBlank()) {
            return null;
        }

        // Add default scheme if missing
        if (!unescaped.startsWith("http://") && !unescaped.startsWith("https://")) {
            if (unescaped.startsWith("//")) {
                unescaped = "https:" + unescaped;
            } else {
                unescaped = "https://" + unescaped;
            }
        }

        try {
            URI uri = URI.create(unescaped);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                return null;
            }

            String host = uri.getHost();
            if (host == null || host.isBlank() || !host.contains(".")) {
                return null;
            }
            host = host.toLowerCase(Locale.ROOT);
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }

            String path = uri.getPath();
            if (path == null || path.isBlank() || path.equals("/")) {
                path = "";
            } else if (path.length() > 1 && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }

            // Strip tracking query parameters
            String query = uri.getQuery();
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

            int port = uri.getPort();
            String portPart = (port != -1 && port != 80 && port != 443) ? ":" + port : "";

            return scheme.toLowerCase(Locale.ROOT) + "://" + host + portPart + path + query;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Unescapes redirect wrappers like DuckDuckGo uddg parameter, Google /url?q= parameter,
     * Yahoo /RU= parameter, or Bing /ck/ redirect parameter.
     */
    public static String unescapeSearchRedirect(String url) {
        if (url == null) return null;

        if (url.contains("uddg=")) {
            int start = url.indexOf("uddg=") + 5;
            int end = url.indexOf("&", start);
            String encoded = end != -1 ? url.substring(start, end) : url.substring(start);
            try {
                return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
            } catch (Exception ignored) {
            }
        }

        if (url.contains("/url?q=")) {
            int start = url.indexOf("/url?q=") + 7;
            int end = url.indexOf("&", start);
            String encoded = end != -1 ? url.substring(start, end) : url.substring(start);
            try {
                return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
            } catch (Exception ignored) {
            }
        }

        if (url.contains("/url?url=")) {
            int start = url.indexOf("/url?url=") + 9;
            int end = url.indexOf("&", start);
            String encoded = end != -1 ? url.substring(start, end) : url.substring(start);
            try {
                return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
            } catch (Exception ignored) {
            }
        }

        if (url.contains("/RU=")) {
            int start = url.indexOf("/RU=") + 4;
            int end = url.indexOf("/RK=", start);
            if (end == -1) end = url.indexOf("&", start);
            String encoded = end != -1 ? url.substring(start, end) : url.substring(start);
            try {
                return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
            } catch (Exception ignored) {
            }
        }

        return url;
    }

    /**
     * Extracts base domain name without 'www.' (e.g. "example.com").
     */
    public static String extractDomain(String url) {
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

    /**
     * Checks if a URL belongs to an excluded aggregator, social network, or search engine.
     */
    public static boolean isExcludedDomain(String url) {
        String domain = extractDomain(url);
        if (domain.isBlank()) {
            return true;
        }
        for (String excluded : EXCLUDED_DOMAINS) {
            if (domain.equals(excluded) || domain.endsWith("." + excluded)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a URL points to a non-HTML document (e.g. PDF, image, media).
     */
    public static boolean isMediaOrDocument(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String lower = url.toLowerCase(Locale.ROOT);
        for (String ext : NON_WEBSITE_EXTENSIONS) {
            if (lower.endsWith(ext) || lower.contains(ext + "?")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a title is a generic non-company phrase (e.g., "About Us", "Home", "Top 10 Colleges").
     */
    public static boolean isGenericTitle(String title) {
        if (title == null || title.isBlank()) {
            return true;
        }
        String lower = title.trim().toLowerCase(Locale.ROOT);
        if (GENERIC_PAGE_TITLES.contains(lower)) {
            return true;
        }
        if (lower.startsWith("about us") || lower.startsWith("contact us") || lower.startsWith("welcome to") || lower.startsWith("home page")) {
            return true;
        }
        if (lower.matches(".*\\btop\\s+\\d+.*") || lower.matches(".*\\bbest\\s+\\d+.*")
                || lower.matches(".*\\blist\\s+of\\s+.*") || lower.contains("near me")
                || lower.contains("to work for") || lower.contains("ranking)")) {
            return true;
        }
        return false;
    }

    /**
     * Determines whether a search result title or URL represents an article listicle or directory roundup.
     */
    public static boolean isListicleOrDirectory(String title, String url) {
        if (title != null && !title.isBlank()) {
            String trimmedTitle = title.trim();
            if (LISTICLE_TITLE_PATTERN.matcher(trimmedTitle).matches()) {
                return true;
            }
            String lowerTitle = trimmedTitle.toLowerCase(Locale.ROOT);
            if (lowerTitle.contains("near me") ||
                    lowerTitle.matches(".*\\btop\\s+\\d+.*") ||
                    lowerTitle.matches(".*\\bbest\\s+\\d+.*") ||
                    lowerTitle.matches(".*\\blist\\s+of\\s+(?:best|top|popular|leading)?\\s*.*") ||
                    lowerTitle.contains("to work for") ||
                    lowerTitle.contains("ranking)")) {
                return true;
            }
        }

        if (url != null && !url.isBlank()) {
            try {
                URI uri = URI.create(url);
                String path = uri.getPath();
                if (path != null && !path.isBlank() && LISTICLE_PATH_PATTERN.matcher(path).find()) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }

        return false;
    }

    /**
     * Validates whether a URL is a legitimate official website candidate.
     */
    public static boolean isValidOfficialWebsite(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String normalized = normalizeUrl(url);
        if (normalized == null) {
            return false;
        }
        if (isExcludedDomain(normalized)) {
            return false;
        }
        if (isMediaOrDocument(normalized)) {
            return false;
        }
        return true;
    }

    /**
     * Clean brand name from title by stripping delimiters, slogans, generic tags, and suffixes.
     */
    public static String cleanBrandFromTitle(String rawTitle, String domain) {
        return cleanBrandFromTitle(rawTitle, domain, null);
    }

    /**
     * Clean brand name from title by stripping delimiters, slogans, generic tags, and dynamic location suffixes.
     */
    public static String cleanBrandFromTitle(String rawTitle, String domain, String location) {
        if (rawTitle == null || rawTitle.isBlank()) {
            return formatDomainAsBrand(domain);
        }

        String cleaned = rawTitle.trim();

        // Strip common generic suffixes
        String[] suffixes = {
                " - Home", " : Home", " | Home", " - Official Website", " | Official Site", " - Official Site",
                " - About Us", " | About Us", " : About Us", " - Contact Us", " | Contact Us",
                " | LinkedIn", " - Facebook", " - Twitter", " | Instagram", " | YouTube", " - YouTube"
        };
        for (String suffix : suffixes) {
            if (cleaned.toLowerCase(Locale.ROOT).endsWith(suffix.toLowerCase(Locale.ROOT))) {
                cleaned = cleaned.substring(0, cleaned.length() - suffix.length()).trim();
            }
        }

        // Dynamically strip user-entered location suffix if provided
        if (location != null && !location.isBlank()) {
            String loc = location.trim();
            String[] locSuffixes = {
                    " - " + loc, " | " + loc, " in " + loc, " (" + loc + ")"
            };
            for (String ls : locSuffixes) {
                if (cleaned.toLowerCase(Locale.ROOT).endsWith(ls.toLowerCase(Locale.ROOT))) {
                    cleaned = cleaned.substring(0, cleaned.length() - ls.length()).trim();
                }
            }
        }

        // Split on standard title delimiters: " | ", " - ", " : ", " • ", " — ", " – "
        String[] parts = cleaned.split("\\s+(?:\\||-|—|–|•|::|:)\\s+");
        if (parts.length > 1) {
            // Find part that is not generic and matches the brand or domain best
            String bestPart = null;
            for (String part : parts) {
                String candidate = part.trim();
                if (!isGenericTitle(candidate) && candidate.length() >= 2 && candidate.length() <= 60) {
                    // Prefer candidate that contains domain name root if possible
                    if (domain != null && !domain.isBlank()) {
                        String domainRoot = domain.split("\\.")[0].toLowerCase(Locale.ROOT);
                        if (candidate.toLowerCase(Locale.ROOT).contains(domainRoot)) {
                            bestPart = candidate;
                            break;
                        }
                    }
                    if (bestPart == null) {
                        bestPart = candidate;
                    }
                }
            }
            if (bestPart != null) {
                cleaned = bestPart;
            } else {
                cleaned = parts[0].trim();
            }
        }

        if (isGenericTitle(cleaned)) {
            return formatDomainAsBrand(domain);
        }

        return cleaned;
    }

    /**
     * Converts a domain name like "apextechsolutions.com" or "salem-cloud.in" into "Apex Tech Solutions".
     */
    public static String formatDomainAsBrand(String domain) {
        if (domain == null || domain.isBlank()) {
            return "";
        }
        String root = domain.split("\\.")[0];
        root = root.replace("-", " ").replace("_", " ");
        StringBuilder sb = new StringBuilder();
        for (String word : root.split("\\s+")) {
            if (!word.isBlank()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    sb.append(word.substring(1).toLowerCase(Locale.ROOT));
                }
            }
        }
        return sb.toString();
    }
}
