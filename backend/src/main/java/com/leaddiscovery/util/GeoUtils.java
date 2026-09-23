package com.leaddiscovery.util;

public final class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoUtils() {
    }

    /**
     * Calculates the great-circle distance between two geographic coordinates
     * on Earth using the Haversine formula.
     *
     * @param lat1 Latitude of point 1 (in degrees)
     * @param lon1 Longitude of point 1 (in degrees)
     * @param lat2 Latitude of point 2 (in degrees)
     * @param lon2 Longitude of point 2 (in degrees)
     * @return Distance in kilometers
     */
    public static double haversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Determines whether target coordinate is within the specified radius (in km)
     * of a center coordinate.
     */
    public static boolean isWithinRadius(double centerLat, double centerLon,
                                         double targetLat, double targetLon,
                                         double radiusKm) {
        if (radiusKm <= 0) {
            return true;
        }
        return haversineDistanceKm(centerLat, centerLon, targetLat, targetLon) <= radiusKm;
    }
}
