package org.jabref.logic.util.strings;

import java.util.Locale;

import info.debatty.java.stringsimilarity.Levenshtein;

public class StringSimilarity {
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
}
