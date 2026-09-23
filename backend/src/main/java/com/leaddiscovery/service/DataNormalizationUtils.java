package com.leaddiscovery.service;

import java.util.Set;
import java.util.regex.Pattern;

public final class DataNormalizationUtils {

    private DataNormalizationUtils() {
    }

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}"
    );

    private static final Set<String> INVALID_EMAIL_EXTENSIONS = Set.of(
            "png", "jpg", "jpeg", "gif", "svg", "webp", "ico", "bmp", "tiff", "js", "css", "woff", "woff2", "ttf"
    );

    private static final String[] LEGAL_SUFFIXES = {
            " pvt ltd", " pvt. ltd.", " private limited", " ltd", " ltd.", " limited",
            " llc", " inc", " inc.", " corp", " corp.", " corporation", " co.", " co"
    };

    /**
     * Cleans and normalizes an email address.
     */
    public static String normalizeEmail(String rawEmail) {
        if (rawEmail == null || rawEmail.isBlank()) {
            return null;
        }
        String cleaned = rawEmail.trim().toLowerCase();
        if (cleaned.length() > 254) {
            return null;
        }

        int lastDotIndex = cleaned.lastIndexOf('.');
        if (lastDotIndex != -1 && lastDotIndex < cleaned.length() - 1) {
            String ext = cleaned.substring(lastDotIndex + 1);
            if (INVALID_EMAIL_EXTENSIONS.contains(ext)) {
                return null;
            }
        }

        if (EMAIL_PATTERN.matcher(cleaned).matches()) {
            return cleaned;
        }
        return null;
    }

    /**
     * Cleans and normalizes a phone number to standard international/digit format.
     */
    public static String normalizePhoneNumber(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return null;
        }
        String trimmed = rawPhone.trim();
        boolean hasPlus = trimmed.startsWith("+");
        String digitsOnly = trimmed.replaceAll("[^0-9]", "");

        if (digitsOnly.length() < 7 || digitsOnly.length() > 15) {
            return null;
        }
        // Reject repeating single digits (0000000, 1111111)
        if (digitsOnly.matches("^(\\d)\\1+$")) {
            return null;
        }

        return hasPlus ? "+" + digitsOnly : digitsOnly;
    }

    /**
     * Normalizes a business name for comparison and duplicate detection.
     */
    public static String normalizeBusinessName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return "";
        }

        String cleaned = rawName.trim().replaceAll("\\s+", " ");

        // Remove common search title noise
        String[] titleSuffixes = {
                " - Home", " - Official Website", " | Official Site", " - Official Site",
                " - About Us", " : Home", " | LinkedIn", " - Facebook"
        };
        for (String suffix : titleSuffixes) {
            if (cleaned.endsWith(suffix)) {
                cleaned = cleaned.substring(0, cleaned.length() - suffix.length()).trim();
            }
        }

        String lower = cleaned.toLowerCase();
        for (String legal : LEGAL_SUFFIXES) {
            if (lower.endsWith(legal)) {
                cleaned = cleaned.substring(0, cleaned.length() - legal.length()).trim();
                break;
            }
        }

        // Remove special punctuation like commas, quotes, brackets
        cleaned = cleaned.replaceAll("[,.'\"()\\-\\[\\]]", " ").replaceAll("\\s+", " ").trim();
        return cleaned.toLowerCase();
    }

    /**
     * Normalizes a physical address by collapsing whitespace and line breaks.
     */
    public static String normalizeAddress(String rawAddress) {
        if (rawAddress == null || rawAddress.isBlank()) {
            return null;
        }
        String cleaned = rawAddress.replaceAll("[\\r\\n\\t]+", " ").replaceAll("\\s+", " ").trim();
        if (cleaned.length() < 5 || cleaned.length() > 500) {
            return null;
        }
        return cleaned;
    }
}
