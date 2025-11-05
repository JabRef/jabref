package org.jabref.logic.importer.util;

import java.util.regex.Pattern;

public class AuthorHeuristics {

    private static final String[] AFFILIATION_HINTS = {
            "university", "institute", "department", "school",
            "college", "laboratory", "lab", "company", "corporation",
            "center", "centre", "faculty"
    };

    /**
     * Heuristic detection of author lines.
     * Accepts lines with initials, uppercase names, or "and"/"," separators.
     */
    public static boolean looksLikeAuthors(String line) {
        if (line == null) {
            return false;
        }

        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return false;
        }

        String lower = trimmed.toLowerCase();

        // Reject clear non-author lines
        if (lower.contains("abstract") || lower.contains("keywords") || lower.contains("introduction")) {
            return false;
        }

        for (String bad : AFFILIATION_HINTS) {
            if (lower.contains(bad)) {
                return false;
            }
        }

        if (lower.contains("@") || lower.contains("http") || lower.contains("doi")) {
            return false;
        }

        // Detect initials or capitalized words
        boolean hasInitials = Pattern.compile("[A-Z]\\. ?[A-Z]?[a-zA-Z]+").matcher(trimmed).find();

        // Count capitalized words
        String[] tokens = trimmed.split("\\s+|,|and");
        int capitalizedCount = 0;
        for (String token : tokens) {
            if (token.length() > 1 && Character.isUpperCase(token.charAt(0))) {
                capitalizedCount++;
            }
        }

        boolean capitalizedRatioOk = capitalizedCount >= Math.max(2, tokens.length / 3);
        boolean hasSeparator = trimmed.contains(",") || lower.contains(" and ");
        boolean notTooLong = tokens.length < 25;

        return (hasSeparator || hasInitials || capitalizedRatioOk) && notTooLong;
    }

    /**
     * Cleans detected author text (removes affiliations, numbers, emails, etc.).
     */
    public static String cleanAuthors(String line) {
        if (line == null) {
            return "";
        }

        // Remove digits and superscripts
        line = line.replaceAll("\\d+", "");
        // Remove email addresses
        line = line.replaceAll("\\S*@\\S*", "");
        // Remove parentheses (affiliations)
        line = line.replaceAll("\\([^)]*\\)", "");
        // Replace commas with "and"
        line = line.replaceAll("\\s*,\\s*", " and ");
        // Normalize "and"
        line = line.replaceAll("\\s+and\\s+", " and ");
        // Collapse multiple "and"
        line = line.replaceAll("(and\\s+)+", "and ");
        // Remove extra spaces
        line = line.replaceAll("\\s{2,}", " ").trim();

        // Normalize uppercase names
        if (line.equals(line.toUpperCase())) {
            line = line.toLowerCase();
            line = Character.toUpperCase(line.charAt(0)) + line.substring(1);
        }

        return line.trim();
    }
}
