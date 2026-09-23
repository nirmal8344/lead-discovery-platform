package com.leaddiscovery.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leaddiscovery.dto.DiscoveredBusinessDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class OpenStreetMapDiscoveryProvider implements BusinessDiscoveryProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenStreetMapDiscoveryProvider.class);

    private static final String PROVIDER_NAME = "OpenStreetMap-Nominatim";
    private static final String NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org/search";
    private static final String USER_AGENT = "LeadDiscoveryPlatform/2.0 (contact@leaddiscovery.com; business-directory-bot)";
    private static final int TIMEOUT_MILLIS = 10000;

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

        double[] centerCoords = null;
        if (searchRadiusKm != null && searchRadiusKm > 0) {
            centerCoords = geocodeLocation(cleanLocation);
            if (centerCoords != null) {
                log.info("[DISCOVERY_GEOCODE] Geocoded location '{}' to lat={}, lon={}", cleanLocation, centerCoords[0], centerCoords[1]);
            }
        }

        List<DiscoveredBusinessDto> results = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();
        Set<String> seenDomains = new HashSet<>();

        // Strategy 1: Search query with "keyword in location"
        String query1 = cleanKeyword + " in " + cleanLocation;
        queryNominatim(query1, results, seenNames, seenDomains, maxResults, centerCoords, searchRadiusKm, cleanLocation);

        // Strategy 2: Search query with "keyword, location" if results < maxResults
        if (results.size() < maxResults) {
            String query2 = cleanKeyword + ", " + cleanLocation;
            queryNominatim(query2, results, seenNames, seenDomains, maxResults, centerCoords, searchRadiusKm, cleanLocation);
        }

        log.info("[DISCOVERY_ACCEPTED] OpenStreetMap provider discovered {} candidates for '{} in {}' (radius={} km)",
                results.size(), cleanKeyword, cleanLocation, searchRadiusKm != null ? searchRadiusKm : "N/A");

        return results;
    }

    private void queryNominatim(String query,
                                List<DiscoveredBusinessDto> results,
                                Set<String> seenNames,
                                Set<String> seenDomains,
                                int maxResults,
                                double[] centerCoords,
                                Integer searchRadiusKm,
                                String cleanLocation) {
        if (results.size() >= maxResults) {
            return;
        }

        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String urlStr = NOMINATIM_BASE_URL + "?q=" + encodedQuery + "&format=json&addressdetails=1&extratags=1&limit=" + Math.min(maxResults * 2, 50);

            log.debug("[DISCOVERY_PROVIDER] Nominatim request URL: {}", urlStr);

            URI uri = URI.create(urlStr);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(TIMEOUT_MILLIS);
            conn.setReadTimeout(TIMEOUT_MILLIS);

            int statusCode = conn.getResponseCode();
            log.debug("[DISCOVERY_PROVIDER] Nominatim response status: {}", statusCode);

            if (statusCode != 200) {
                log.warn("[DISCOVERY_PROVIDER] Nominatim returned non-200 status: {}", statusCode);
                return;
            }

            try (InputStream is = conn.getInputStream()) {
                JsonNode root = objectMapper.readTree(is);
                if (root != null && root.isArray()) {
                    for (JsonNode item : root) {
                        if (results.size() >= maxResults) {
                            break;
                        }

                        String rawName = extractName(item);
                        if (rawName == null || rawName.isBlank() || UrlFilterUtils.isGenericTitle(rawName)) {
                            continue;
                        }

                        String nameKey = rawName.toLowerCase(Locale.ROOT);
                        if (seenNames.contains(nameKey)) {
                            continue;
                        }

                        // Coordinates and radius filtering
                        Double itemLat = null;
                        Double itemLon = null;
                        Double distanceKm = null;
                        if (item.hasNonNull("lat") && item.hasNonNull("lon")) {
                            try {
                                itemLat = Double.parseDouble(item.get("lat").asText());
                                itemLon = Double.parseDouble(item.get("lon").asText());
                                if (centerCoords != null) {
                                    distanceKm = com.leaddiscovery.util.GeoUtils.haversineDistanceKm(
                                            centerCoords[0], centerCoords[1], itemLat, itemLon);
                                    if (searchRadiusKm != null && searchRadiusKm > 0 && distanceKm > searchRadiusKm) {
                                        log.info("[RADIUS_FILTER] Candidate '{}' at distance {:.2f} km exceeds radius {} km for location '{}'",
                                                rawName, distanceKm, searchRadiusKm, cleanLocation);
                                        continue;
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }

                        // Extract official website from extratags
                        String websiteUrl = extractWebsite(item);
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

                        // If no direct website in extratags, construct a fallback domain / URL from business identity
                        if (normalizedUrl == null) {
                            String domainSlug = rawName.toLowerCase(Locale.ROOT)
                                     .replaceAll("[^a-z0-9]+", "-")
                                     .replaceAll("^-+|-+$", "");
                            if (domainSlug.isBlank()) {
                                continue;
                            }
                            domain = domainSlug + ".org";
                            normalizedUrl = "https://" + domain;
                        }

                        seenNames.add(nameKey);
                        if (domain != null && !domain.isBlank()) {
                            seenDomains.add(domain);
                        }

                        String cleanedTitle = UrlFilterUtils.cleanBrandFromTitle(rawName, domain);

                        results.add(new DiscoveredBusinessDto(
                                cleanedTitle,
                                normalizedUrl,
                                NOMINATIM_BASE_URL + "?q=" + encodedQuery,
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
        } catch (Exception e) {
            log.warn("[DISCOVERY_PROVIDER] Error executing Nominatim query '{}': {}", query, e.getMessage());
        }
    }

    public double[] geocodeLocation(String location) {
        if (location == null || location.isBlank()) {
            return null;
        }
        try {
            String encoded = URLEncoder.encode(location.trim(), StandardCharsets.UTF_8);
            String urlStr = NOMINATIM_BASE_URL + "?q=" + encoded + "&format=json&limit=1";
            URI uri = URI.create(urlStr);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(TIMEOUT_MILLIS);
            conn.setReadTimeout(TIMEOUT_MILLIS);
            if (conn.getResponseCode() == 200) {
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
            }
        } catch (Exception e) {
            log.debug("[DISCOVERY_GEOCODE] Could not geocode location '{}': {}", location, e.getMessage());
        }
        return null;
    }

    private String extractName(JsonNode item) {
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

    private String extractWebsite(JsonNode item) {
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
