package com.leaddiscovery.discovery;

import com.leaddiscovery.dto.DiscoveredBusinessDto;
import java.util.List;

public interface BusinessDiscoveryProvider {

    /**
     * Discovers businesses given a location and business keyword/category.
     *
     * @param location the target geographic location (e.g. "Salem", "New York")
     * @param keyword the business category or keyword (e.g. "Software Companies", "Hospitals")
     * @param maxResults maximum number of business results to discover
     * @return list of discovered businesses
     */
    List<DiscoveredBusinessDto> discover(String location, String keyword, int maxResults);

    /**
     * Discovers businesses given a location, keyword/category, and optional search radius.
     */
    default List<DiscoveredBusinessDto> discover(String location, String keyword, int maxResults, Integer searchRadiusKm) {
        return discover(location, keyword, maxResults);
    }

    /**
     * Unique name identifying this discovery provider.
     */
    String getProviderName();

    /**
     * Whether this discovery provider is currently active and enabled.
     */
    default boolean isEnabled() {
        return true;
    }
}
