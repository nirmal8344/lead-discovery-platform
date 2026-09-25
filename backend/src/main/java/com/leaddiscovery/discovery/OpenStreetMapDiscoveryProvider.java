package com.leaddiscovery.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leaddiscovery.dto.DiscoveredBusinessDto;
import com.leaddiscovery.util.GeoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
@Order(1)
public class OpenStreetMapDiscoveryProvider implements BusinessDiscoveryProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenStreetMapDiscoveryProvider.class);

    private static final String PROVIDER_NAME = "OpenStreetMap-Overpass";
    private static final String NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org/search";
    private static final String USER_AGENT = "BusinessDiscoveryPlatform/1.0 (support@leaddiscovery.com)";
    private static final String REFERER = "https://leaddiscovery.com";

    private static final List<String> OVERPASS_ENDPOINTS = List.of(
            "https://overpass-api.de/api/interpreter",
            "https://lz4.overpass-api.de/api/interpreter",
            "https://z.overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter"
    );

    private static final int CONNECT_TIMEOUT_MS = 4000;
    private static final int READ_TIMEOUT_MS = 6000;
    private static final int DEFAULT_RADIUS_METERS = 25000;

    private final ObjectMapper objectMapper;

    public OpenStreetMapDiscoveryProvider() {
        this.objectMapper = new ObjectMapper();
    }

    public OpenStreetMapDiscoveryProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public List<DiscoveredBusinessDto> discover(String location, String keyword, int maxResults) {
        return discover(location, keyword, maxResults, null);
    }

    @Override
    public List<DiscoveredBusinessDto> discover(String location, String keyword, int maxResults, Integer searchRadiusKm) {
        String cleanLocation = location != null ? location.trim() : "";
        String cleanKeyword = keyword != null ? keyword.trim() : "";

        if (cleanLocation.isBlank() || cleanKeyword.isBlank()) {
            return Collections.emptyList();
        }

        log.info("[DISCOVERY_QUERY] Executing OpenStreetMap discovery: keyword='{}', location='{}', maxResults={}, radiusKm={}",
                cleanKeyword, cleanLocation, maxResults, searchRadiusKm);

        double[] centerCoords = geocodeLocationWithRetry(cleanLocation);

        List<DiscoveredBusinessDto> results = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();
        Set<String> seenDomains = new HashSet<>();

        // Strategy 1: Overpass API if geocoded coordinates are available
        if (centerCoords != null) {
            int radiusMeters = (searchRadiusKm != null && searchRadiusKm > 0)
                    ? (searchRadiusKm * 1000)
                    : DEFAULT_RADIUS_METERS;

            String overpassQuery = buildOverpassQuery(cleanKeyword, centerCoords[0], centerCoords[1], radiusMeters, Math.max(maxResults * 2, 40));
            List<DiscoveredBusinessDto> overpassResults = executeOverpassQuery(overpassQuery, centerCoords, cleanLocation, searchRadiusKm, maxResults);
            
            for (DiscoveredBusinessDto dto : overpassResults) {
                String nameKey = dto.getBusinessName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
                String domain = UrlFilterUtils.extractDomain(dto.getWebsiteUrl());
                if (!seenNames.contains(nameKey) && (domain.isBlank() || !seenDomains.contains(domain))) {
                    seenNames.add(nameKey);
                    if (!domain.isBlank()) seenDomains.add(domain);
                    results.add(dto);
                }
            }
        }

        // Strategy 2: Fallback / Enrichment with Nominatim Direct Search if results < maxResults
        if (results.size() < maxResults) {
            List<DiscoveredBusinessDto> nominatimResults = searchNominatimDirect(cleanKeyword, cleanLocation, centerCoords, searchRadiusKm, maxResults - results.size());
            for (DiscoveredBusinessDto dto : nominatimResults) {
                String nameKey = dto.getBusinessName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
                String domain = UrlFilterUtils.extractDomain(dto.getWebsiteUrl());
                if (!seenNames.contains(nameKey) && (domain.isBlank() || !seenDomains.contains(domain))) {
                    seenNames.add(nameKey);
                    if (!domain.isBlank()) seenDomains.add(domain);
                    results.add(dto);
                }
            }
        }

        log.info("[DISCOVERY_RESULTS] OpenStreetMap discovered {} candidates for '{}' in '{}'",
                results.size(), cleanKeyword, cleanLocation);
        return results;
    }

    public double[] geocodeLocation(String location) {
        return geocodeLocationWithRetry(location);
    }

    private double[] geocodeLocationWithRetry(String location) {
        if (location == null || location.isBlank()) {
            return null;
        }

        int maxRetries = 2;
        long backoffMs = 500;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String encoded = URLEncoder.encode(location.trim(), StandardCharsets.UTF_8);
                String urlStr = NOMINATIM_BASE_URL + "?q=" + encoded + "&format=json&limit=1";
                URI uri = URI.create(urlStr);
                HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setRequestProperty("Referer", REFERER);
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(READ_TIMEOUT_MS);

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    try (InputStream is = conn.getInputStream()) {
                        JsonNode root = objectMapper.readTree(is);
                        if (root != null && root.isArray() && !root.isEmpty()) {
                            JsonNode first = root.get(0);
                            if (first.hasNonNull("lat") && first.hasNonNull("lon")) {
                                return new double[]{
                                        Double.parseDouble(first.get("lat").asText()),
                                        Double.parseDouble(first.get("lon").asText())
                                };
                            }
                        }
                    }
                } else if (responseCode == 429 && attempt < maxRetries) {
                    Thread.sleep(backoffMs);
                    backoffMs *= 2;
                }
            } catch (Exception e) {
                log.debug("[DISCOVERY_GEOCODE] Geocode error for '{}' on attempt {}: {}", location, attempt, e.getMessage());
            }
        }
        return null;
    }

    private List<DiscoveredBusinessDto> searchNominatimDirect(String keyword, String location, double[] centerCoords,
                                                              Integer searchRadiusKm, int limit) {
        List<DiscoveredBusinessDto> results = new ArrayList<>();
        String[] queries = new String[]{
                keyword + " in " + location,
                keyword + " " + location
        };

        for (String q : queries) {
            if (results.size() >= limit) break;
            try {
                String encoded = URLEncoder.encode(q, StandardCharsets.UTF_8);
                String urlStr = NOMINATIM_BASE_URL + "?q=" + encoded + "&format=json&limit=" + Math.max(limit * 2, 20) + "&addressdetails=1&extratags=1";
                URI uri = URI.create(urlStr);
                HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setRequestProperty("Referer", REFERER);
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(READ_TIMEOUT_MS);

                if (conn.getResponseCode() == 200) {
                    try (InputStream is = conn.getInputStream()) {
                        JsonNode root = objectMapper.readTree(is);
                        if (root != null && root.isArray()) {
                            for (JsonNode item : root) {
                                if (results.size() >= limit) break;

                                String rawName = extractNameFromNominatim(item);
                                if (rawName == null || rawName.isBlank()) continue;

                                Double itemLat = item.hasNonNull("lat") ? item.get("lat").asDouble() : null;
                                Double itemLon = item.hasNonNull("lon") ? item.get("lon").asDouble() : null;
                                Double distanceKm = null;

                                if (itemLat != null && itemLon != null && centerCoords != null) {
                                    distanceKm = GeoUtils.haversineDistanceKm(centerCoords[0], centerCoords[1], itemLat, itemLon);
                                    if (searchRadiusKm != null && searchRadiusKm > 0 && distanceKm > searchRadiusKm) {
                                        continue;
                                    }
                                }

                                String websiteUrl = extractWebsiteFromNominatim(item);
                                String normalizedUrl = null;
                                String domain = null;

                                if (websiteUrl != null && !websiteUrl.isBlank()) {
                                    normalizedUrl = UrlFilterUtils.normalizeUrl(websiteUrl);
                                    if (normalizedUrl != null && UrlFilterUtils.isValidOfficialWebsite(normalizedUrl)) {
                                        domain = UrlFilterUtils.extractDomain(normalizedUrl);
                                    } else {
                                        normalizedUrl = null;
                                    }
                                }

                                if (normalizedUrl == null) {
                                    String domainSlug = rawName.toLowerCase(Locale.ROOT)
                                            .replaceAll("[^a-z0-9]+", "-")
                                            .replaceAll("^-+|-+$", "");
                                    if (domainSlug.isBlank()) continue;
                                    domain = domainSlug + ".org";
                                    normalizedUrl = "https://" + domain;
                                }

                                String cleanedTitle = UrlFilterUtils.cleanBrandFromTitle(rawName, domain);
                                results.add(new DiscoveredBusinessDto(
                                        cleanedTitle,
                                        normalizedUrl,
                                        NOMINATIM_BASE_URL + "?q=" + encoded,
                                        PROVIDER_NAME,
                                        0.70,
                                        "CANDIDATE",
                                        itemLat,
                                        itemLon,
                                        distanceKm
                                ));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("[DISCOVERY_NOMINATIM] Error querying Nominatim for '{}': {}", q, e.getMessage());
            }
        }
        return results;
    }

    private String buildOverpassQuery(String keyword, double lat, double lon, int radiusMeters, int limit) {
        String lower = keyword.toLowerCase(Locale.ROOT);
        String safeKeyword = keyword.replaceAll("[^a-zA-Z0-9\\s]", "").trim();
        if (safeKeyword.isBlank()) safeKeyword = keyword;

        List<String> clauses = new ArrayList<>();

        if (lower.contains("school") || lower.contains("college") || lower.contains("universit") || lower.contains("education") || lower.contains("academy")) {
            clauses.add(String.format(Locale.ROOT, "node(around:%d,%.6f,%.6f)[\"amenity\"~\"school|college|university|kindergarten\"];", radiusMeters, lat, lon));
            clauses.add(String.format(Locale.ROOT, "way(around:%d,%.6f,%.6f)[\"amenity\"~\"school|college|university|kindergarten\"];", radiusMeters, lat, lon));
        } else if (lower.contains("hospital") || lower.contains("clinic") || lower.contains("doctor") || lower.contains("health") || lower.contains("medical") || lower.contains("pharmacy")) {
            clauses.add(String.format(Locale.ROOT, "node(around:%d,%.6f,%.6f)[\"amenity\"~\"hospital|clinic|doctors|pharmacy\"];", radiusMeters, lat, lon));
            clauses.add(String.format(Locale.ROOT, "way(around:%d,%.6f,%.6f)[\"amenity\"~\"hospital|clinic|doctors|pharmacy\"];", radiusMeters, lat, lon));
        } else if (lower.contains("hotel") || lower.contains("resort") || lower.contains("lodge") || lower.contains("motel") || lower.contains("hostel") || lower.contains("stay")) {
            clauses.add(String.format(Locale.ROOT, "node(around:%d,%.6f,%.6f)[\"tourism\"~\"hotel|motel|guest_house|resort|hostel\"];", radiusMeters, lat, lon));
            clauses.add(String.format(Locale.ROOT, "way(around:%d,%.6f,%.6f)[\"tourism\"~\"hotel|motel|guest_house|resort|hostel\"];", radiusMeters, lat, lon));
        } else if (lower.contains("software") || lower.contains("it") || lower.contains("tech") || lower.contains("comput") || lower.contains("consulting") || lower.contains("develop")) {
            clauses.add(String.format(Locale.ROOT, "node(around:%d,%.6f,%.6f)[\"office\"~\"it|company|software|telecommunication|consulting\"];", radiusMeters, lat, lon));
            clauses.add(String.format(Locale.ROOT, "way(around:%d,%.6f,%.6f)[\"office\"~\"it|company|software|telecommunication|consulting\"];", radiusMeters, lat, lon));
            clauses.add(String.format(Locale.ROOT, "node(around:%d,%.6f,%.6f)[\"office\"];", radiusMeters, lat, lon));
            clauses.add(String.format(Locale.ROOT, "way(around:%d,%.6f,%.6f)[\"office\"];", radiusMeters, lat, lon));
        } else if (lower.contains("restaurant") || lower.contains("cafe") || lower.contains("food") || lower.contains("bakery") || lower.contains("dining")) {
            clauses.add(String.format(Locale.ROOT, "node(around:%d,%.6f,%.6f)[\"amenity\"~\"restaurant|cafe|fast_food|bar|pub|bakery\"];", radiusMeters, lat, lon));
            clauses.add(String.format(Locale.ROOT, "way(around:%d,%.6f,%.6f)[\"amenity\"~\"restaurant|cafe|fast_food|bar|pub|bakery\"];", radiusMeters, lat, lon));
        } else if (lower.contains("gym") || lower.contains("fitness") || lower.contains("sport")) {
            clauses.add(String.format(Locale.ROOT, "node(around:%d,%.6f,%.6f)[\"leisure\"~\"fitness_centre|sports_centre\"];", radiusMeters, lat, lon));
            clauses.add(String.format(Locale.ROOT, "way(around:%d,%.6f,%.6f)[\"leisure\"~\"fitness_centre|sports_centre\"];", radiusMeters, lat, lon));
        } else if (lower.contains("bank") || lower.contains("atm") || lower.contains("finance")) {
            clauses.add(String.format(Locale.ROOT, "node(around:%d,%.6f,%.6f)[\"amenity\"~\"bank|atm\"];", radiusMeters, lat, lon));
            clauses.add(String.format(Locale.ROOT, "way(around:%d,%.6f,%.6f)[\"amenity\"~\"bank|atm\"];", radiusMeters, lat, lon));
        } else if (lower.contains("shop") || lower.contains("store") || lower.contains("market") || lower.contains("retail") || lower.contains("mall")) {
            clauses.add(String.format(Locale.ROOT, "node(around:%d,%.6f,%.6f)[\"shop\"];", radiusMeters, lat, lon));
            clauses.add(String.format(Locale.ROOT, "way(around:%d,%.6f,%.6f)[\"shop\"];", radiusMeters, lat, lon));
        }

        clauses.add(String.format(Locale.ROOT, "node(around:%d,%.6f,%.6f)[\"name\"~\"%s\",i];", radiusMeters, lat, lon, safeKeyword));
        clauses.add(String.format(Locale.ROOT, "way(around:%d,%.6f,%.6f)[\"name\"~\"%s\",i];", radiusMeters, lat, lon, safeKeyword));

        String[] words = safeKeyword.split("\\s+");
        for (String w : words) {
            String cleanW = w.replaceAll("[^a-zA-Z0-9]", "").trim();
            if (cleanW.length() >= 4 && !cleanW.equalsIgnoreCase(safeKeyword)) {
                clauses.add(String.format(Locale.ROOT, "node(around:%d,%.6f,%.6f)[\"name\"~\"%s\",i];", radiusMeters, lat, lon, cleanW));
                clauses.add(String.format(Locale.ROOT, "way(around:%d,%.6f,%.6f)[\"name\"~\"%s\",i];", radiusMeters, lat, lon, cleanW));
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[out:json][timeout:8];\n(\n");
        for (String clause : clauses) {
            sb.append("  ").append(clause).append("\n");
        }
        sb.append(");\nout center tags ").append(limit).append(";");
        return sb.toString();
    }

    private List<DiscoveredBusinessDto> executeOverpassQuery(String overpassQuery, double[] centerCoords,
                                                             String location, Integer searchRadiusKm, int maxResults) {
        List<DiscoveredBusinessDto> results = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();
        Set<String> seenDomains = new HashSet<>();

        for (String endpoint : OVERPASS_ENDPOINTS) {
            try {
                URI uri = URI.create(endpoint);
                HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("User-Agent", USER_AGENT);
                conn.setRequestProperty("Referer", REFERER);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(READ_TIMEOUT_MS);
                conn.setDoOutput(true);

                String postBody = "data=" + URLEncoder.encode(overpassQuery, StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(postBody.getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    try (InputStream is = conn.getInputStream()) {
                        JsonNode root = objectMapper.readTree(is);
                        if (root != null && root.has("elements") && root.get("elements").isArray()) {
                            for (JsonNode elem : root.get("elements")) {
                                if (results.size() >= maxResults) break;

                                JsonNode tags = elem.get("tags");
                                if (tags == null) continue;

                                String rawName = extractNameFromTags(tags);
                                if (rawName == null || rawName.isBlank()) continue;

                                String nameKey = rawName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
                                if (seenNames.contains(nameKey)) continue;

                                Double itemLat = null;
                                Double itemLon = null;
                                if (elem.hasNonNull("lat") && elem.hasNonNull("lon")) {
                                    itemLat = elem.get("lat").asDouble();
                                    itemLon = elem.get("lon").asDouble();
                                } else if (elem.has("center")) {
                                    JsonNode center = elem.get("center");
                                    if (center.hasNonNull("lat") && center.hasNonNull("lon")) {
                                        itemLat = center.get("lat").asDouble();
                                        itemLon = center.get("lon").asDouble();
                                    }
                                }

                                Double distanceKm = null;
                                if (itemLat != null && itemLon != null && centerCoords != null) {
                                    distanceKm = GeoUtils.haversineDistanceKm(centerCoords[0], centerCoords[1], itemLat, itemLon);
                                    if (searchRadiusKm != null && searchRadiusKm > 0 && distanceKm > searchRadiusKm) {
                                        continue;
                                    }
                                }

                                String websiteUrl = extractWebsiteFromTags(tags);
                                String normalizedUrl = null;
                                String domain = null;

                                if (websiteUrl != null && !websiteUrl.isBlank()) {
                                    normalizedUrl = UrlFilterUtils.normalizeUrl(websiteUrl);
                                    if (normalizedUrl != null && UrlFilterUtils.isValidOfficialWebsite(normalizedUrl)) {
                                        domain = UrlFilterUtils.extractDomain(normalizedUrl);
                                        if (!domain.isBlank() && seenDomains.contains(domain)) {
                                            continue;
                                        }
                                    } else {
                                        normalizedUrl = null;
                                    }
                                }

                                if (normalizedUrl == null) {
                                    String domainSlug = rawName.toLowerCase(Locale.ROOT)
                                            .replaceAll("[^a-z0-9]+", "-")
                                            .replaceAll("^-+|-+$", "");
                                    if (domainSlug.isBlank()) continue;
                                    domain = domainSlug + ".org";
                                    normalizedUrl = "https://" + domain;
                                }

                                seenNames.add(nameKey);
                                if (domain != null && !domain.isBlank()) seenDomains.add(domain);

                                String cleanedTitle = UrlFilterUtils.cleanBrandFromTitle(rawName, domain);

                                results.add(new DiscoveredBusinessDto(
                                        cleanedTitle,
                                        normalizedUrl,
                                        endpoint,
                                        PROVIDER_NAME,
                                        0.75,
                                        "CANDIDATE",
                                        itemLat,
                                        itemLon,
                                        distanceKm
                                ));
                            }
                        }
                    }

                    if (!results.isEmpty()) {
                        return results;
                    }
                }
            } catch (Exception e) {
                log.debug("[DISCOVERY_OVERPASS] Overpass endpoint '{}' skipped: {}", endpoint, e.getMessage());
            }
        }

        return results;
    }

    private String extractNameFromTags(JsonNode tags) {
        if (tags.hasNonNull("name")) {
            String name = tags.get("name").asText().trim();
            if (!name.isBlank()) return name;
        }
        if (tags.hasNonNull("name:en")) {
            String name = tags.get("name:en").asText().trim();
            if (!name.isBlank()) return name;
        }
        if (tags.hasNonNull("brand")) {
            String brand = tags.get("brand").asText().trim();
            if (!brand.isBlank()) return brand;
        }
        if (tags.hasNonNull("operator")) {
            String operator = tags.get("operator").asText().trim();
            if (!operator.isBlank()) return operator;
        }
        return null;
    }

    private String extractWebsiteFromTags(JsonNode tags) {
        if (tags.hasNonNull("website")) return tags.get("website").asText().trim();
        if (tags.hasNonNull("contact:website")) return tags.get("contact:website").asText().trim();
        if (tags.hasNonNull("url")) return tags.get("url").asText().trim();
        if (tags.hasNonNull("contact:url")) return tags.get("contact:url").asText().trim();
        return null;
    }

    private String extractNameFromNominatim(JsonNode item) {
        if (item.hasNonNull("name")) {
            String name = item.get("name").asText().trim();
            if (!name.isBlank()) return name;
        }
        if (item.hasNonNull("display_name")) {
            String displayName = item.get("display_name").asText().trim();
            String[] parts = displayName.split(",");
            if (parts.length > 0 && !parts[0].isBlank()) {
                return parts[0].trim();
            }
        }
        return null;
    }

    private String extractWebsiteFromNominatim(JsonNode item) {
        if (item.has("extratags")) {
            JsonNode extra = item.get("extratags");
            if (extra.hasNonNull("website")) return extra.get("website").asText().trim();
            if (extra.hasNonNull("contact:website")) return extra.get("contact:website").asText().trim();
            if (extra.hasNonNull("url")) return extra.get("url").asText().trim();
            if (extra.hasNonNull("contact:url")) return extra.get("contact:url").asText().trim();
        }
        return null;
    }
}
