package org.jabref.logic.util.strings;

import java.util.Locale;

import info.debatty.java.stringsimilarity.Levenshtein;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StringSimilarity {
    private static final Logger LOGGER = LoggerFactory.getLogger(StringSimilarity.class);

    private final Levenshtein METRIC_DISTANCE = new Levenshtein();
    // edit distance threshold for entry title comparison
    private final int METRIC_THRESHOLD = 4;

    /**
     * String similarity based on Levenshtein, ignoreCase, and fixed metric threshold of 4.
     *
     * @param a String to compare
     * @param b String to compare
     * @return true if Strings are considered as similar by the algorithm
     */
    public boolean isSimilar(String a, String b) {
        return editDistanceIgnoreCase(a, b) <= METRIC_THRESHOLD;
    }

    public double editDistanceIgnoreCase(String a, String b) {
        // TODO: Locale is dependent on the language of the strings. English is a good denominator.
        return METRIC_DISTANCE.distance(a.toLowerCase(Locale.ENGLISH), b.toLowerCase(Locale.ENGLISH));
    }

    /**
     * Calculates the similarity (a number within 0 and 1) between two strings.
     * http://stackoverflow.com/questions/955110/similarity-string-comparison-in-java
     */
    public double similarity(final String first, final String second) {
        final String longer;
        final String shorter;

        if (first.length() < second.length()) {
            longer = second;
            shorter = first;
        } else {
            longer = first;
            shorter = second;
        }

        final int longerLength = longer.length();
        // both strings are zero length
        if (longerLength == 0) {
            return 1.0;
        }
        final double distanceIgnoredCase = editDistanceIgnoreCase(longer, shorter);
        final double similarity = (longerLength - distanceIgnoredCase) / longerLength;
        LOGGER.trace("Longer string: {} Shorter string: {} Similarity: {}", longer, shorter, similarity);
        return similarity;
    }

    /**
     * Returns the Longest Common Substring (LCS) similarity rating between two strings, ignoring case.
     * <p>
     * This function uses the following formula = <code>(length of longest substring) / (length of shorter string)</code>.
     * The longest common substring is calculated using a space-optimized dynamic programming implementation of the LCS
     * algorithm found <a href="https://en.wikipedia.org/wiki/Longest_common_substring">on Wikipedia</a>.
     * </p>
     */
    public static double LCSSimilarity(String first, String second) {
        if (first.isEmpty() && second.isEmpty()) {
            return 1.0;
        }

        if (first.isEmpty() || second.isEmpty()) {
            return 0.0;
        }

        first = first.toLowerCase();
        second = second.toLowerCase();
        int firstLength = first.length();
        int secondLength = second.length();

        int[] previousMatches = new int[secondLength + 1];

        int longestSubstringLength = 0;
        for (int i = 1; i <= firstLength; i++) {
            int[] currentMatches = new int[secondLength + 1];
            for (int j = 1; j <= secondLength; j++) {
                if (first.charAt(i - 1) == second.charAt(j - 1)) {
                    currentMatches[j] = previousMatches[j - 1] + 1;
                    longestSubstringLength = Math.max(longestSubstringLength, currentMatches[j]);
                } else {
                    currentMatches[j] = 0;
                }
            }

            // Move the current row's data to the previous row
            previousMatches = currentMatches;
        }
        return (double) longestSubstringLength / Math.min(firstLength, secondLength);
    }
}
