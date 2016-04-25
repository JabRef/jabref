/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.sf.jabref.bibtex.FieldProperties;
import net.sf.jabref.bibtex.InternalBibtexFields;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.AuthorList;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.EntryType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class contains utility method for duplicate checking of entries.
 */
public class DuplicateCheck {

    private static final Log LOGGER = LogFactory.getLog(DuplicateCheck.class);

    /*
     * Integer values for indicating result of duplicate check (for entries):
     *
     */
    private static final int NOT_EQUAL = 0;
    private static final int EQUAL = 1;
    private static final int EMPTY_IN_ONE = 2;
    private static final int EMPTY_IN_TWO = 3;
    private static final int EMPTY_IN_BOTH = 4;

    public static double duplicateThreshold = 0.75; // The overall threshold to signal a duplicate pair
    // Non-required fields are investigated only if the required fields give a value within
    // the doubt range of the threshold:
    private static final double DOUBT_RANGE = 0.05;

    private static final double REQUIRED_WEIGHT = 3; // Weighting of all required fields

    // Extra weighting of those fields that are most likely to provide correct duplicate detection:
    private static final Map<String, Double> FIELD_WEIGHTS = new HashMap<>();


    static {
        DuplicateCheck.FIELD_WEIGHTS.put("author", 2.5);
        DuplicateCheck.FIELD_WEIGHTS.put("editor", 2.5);
        DuplicateCheck.FIELD_WEIGHTS.put("title", 3.);
        DuplicateCheck.FIELD_WEIGHTS.put("journal", 2.);
    }


    /**
     * Checks if the two entries represent the same publication.
     *
     * @param one BibEntry
     * @param two BibEntry
     * @return boolean
     */
    public static boolean isDuplicate(BibEntry one, BibEntry two, BibDatabaseMode bibDatabaseMode) {

        // First check if they are of the same type - a necessary condition:
        if (!one.getType().equals(two.getType())) {
            return false;
        }
        EntryType type = EntryTypes.getTypeOrDefault(one.getType(), bibDatabaseMode);

        // The check if they have the same required fields:
        java.util.List<String> var = type.getRequiredFieldsFlat();
        String[] fields = var.toArray(new String[var.size()]);
        double[] req;
        if (fields == null) {
            req = new double[]{0., 0.};
        } else {
            req = DuplicateCheck.compareFieldSet(fields, one, two);
        }

        if (Math.abs(req[0] - DuplicateCheck.duplicateThreshold) > DuplicateCheck.DOUBT_RANGE) {
            // Far from the threshold value, so we base our decision on the req. fields only
            return req[0] >= DuplicateCheck.duplicateThreshold;
        }
        // Close to the threshold value, so we take a look at the optional fields, if any:
        java.util.List<String> optionalFields = type.getOptionalFields();
        fields = optionalFields.toArray(new String[optionalFields.size()]);
        if (fields != null) {
            double[] opt = DuplicateCheck.compareFieldSet(fields, one, two);
            double totValue = ((DuplicateCheck.REQUIRED_WEIGHT * req[0] * req[1]) + (opt[0] * opt[1])) / ((req[1] * DuplicateCheck.REQUIRED_WEIGHT) + opt[1]);
            return totValue >= DuplicateCheck.duplicateThreshold;
        }
        return req[0] >= DuplicateCheck.duplicateThreshold;
    }

    private static double[] compareFieldSet(String[] fields, BibEntry one, BibEntry two) {
        double res = 0;
        double totWeights = 0.;
        for (String field : fields) {
            double weight;
            if (DuplicateCheck.FIELD_WEIGHTS.containsKey(field)) {
                weight = DuplicateCheck.FIELD_WEIGHTS.get(field);
            } else {
                weight = 1.0;
            }
            totWeights += weight;
            int result = DuplicateCheck.compareSingleField(field, one, two);
            if (result == EQUAL) {
                res += weight;
            } else if (result == EMPTY_IN_BOTH) {
                totWeights -= weight;
            }
        }
        if (totWeights > 0) {
            return new double[]{res / totWeights, totWeights};
        }
        return new double[] {0.5, 0.0};
    }

    private static int compareSingleField(String field, BibEntry one, BibEntry two) {
        String s1 = one.getField(field);
        String s2 = two.getField(field);
        if (s1 == null) {
            if (s2 == null) {
                return EMPTY_IN_BOTH;
            }
            return EMPTY_IN_ONE;
        } else if (s2 == null) {
            return EMPTY_IN_TWO;
        }

        if (InternalBibtexFields.getFieldExtras(field).contains(FieldProperties.PERSON_NAMES)) {
            // Specific for name fields.
            // Harmonise case:
            String auth1 = AuthorList.fixAuthorLastNameOnlyCommas(s1, false).replace(" and ", " ").toLowerCase();
            String auth2 = AuthorList.fixAuthorLastNameOnlyCommas(s2, false).replace(" and ", " ").toLowerCase();
            double similarity = DuplicateCheck.correlateByWords(auth1, auth2);
            if (similarity > 0.8) {
                return EQUAL;
            }
            return NOT_EQUAL;
        } else if ("pages".equals(field)) {
            // Pages can be given with a variety of delimiters, "-", "--", " - ", " -- ".
            // We do a replace to harmonize these to a simple "-":
            // After this, a simple test for equality should be enough:
            s1 = s1.replaceAll("[- ]+", "-");
            s2 = s2.replaceAll("[- ]+", "-");
            if (s1.equals(s2)) {
                return EQUAL;
            }
            return NOT_EQUAL;
        } else if ("journal".equals(field)) {
            // We do not attempt to harmonize abbreviation state of the journal names,
            // but we remove periods from the names in case they are abbreviated with
            // and without dots:
            s1 = s1.replace(".", "").toLowerCase();
            s2 = s2.replace(".", "").toLowerCase();
            double similarity = DuplicateCheck.correlateByWords(s1, s2);
            if (similarity > 0.8) {
                return EQUAL;
            }
            return NOT_EQUAL;
        } else {
            s1 = s1.toLowerCase();
            s2 = s2.toLowerCase();
            double similarity = DuplicateCheck.correlateByWords(s1, s2);
            if (similarity > 0.8) {
                return EQUAL;
            }
            return NOT_EQUAL;
        }
    }

    public static double compareEntriesStrictly(BibEntry one, BibEntry two) {
        Set<String> allFields = new HashSet<>();
        allFields.addAll(one.getFieldNames());
        allFields.addAll(two.getFieldNames());

        int score = 0;
        for (String field : allFields) {
            Object en = one.getField(field);
            Object to = two.getField(field);
            if (((en != null) && (to != null) && en.equals(to)) || ((en == null) && (to == null))) {
                score++;
            }
        }
        if (score == allFields.size()) {
            return 1.01; // Just to make sure we can
            // use score>1 without
            // trouble.
        }
        return (double) score / allFields.size();
    }

    /**
     * Goes through all entries in the given database, and if at least one of
     * them is a duplicate of the given entry, as per
     * Util.isDuplicate(BibEntry, BibEntry), the duplicate is returned.
     * The search is terminated when the first duplicate is found.
     *
     * @param database The database to search.
     * @param entry    The entry of which we are looking for duplicates.
     * @return The first duplicate entry found. null if no duplicates are found.
     */
    public static Optional<BibEntry> containsDuplicate(BibDatabase database, BibEntry entry, BibDatabaseMode bibDatabaseMode) {
        for (BibEntry other : database.getEntries()) {
            if (DuplicateCheck.isDuplicate(entry, other, bibDatabaseMode)) {
                return Optional.of(other); // Duplicate found.
            }
        }
        return Optional.empty(); // No duplicate found.
    }

    /**
     * Compare two strings on the basis of word-by-word correlation analysis.
     *
     * @param s1       The first string
     * @param s2       The second string
     * @return a value in the interval [0, 1] indicating the degree of match.
     */
    public static double correlateByWords(String s1, String s2) {
        String[] w1 = s1.split("\\s");
        String[] w2 = s2.split("\\s");
        int n = Math.min(w1.length, w2.length);
        int misses = 0;
        for (int i = 0; i < n; i++) {
            double corr = similarity(w1[i], w2[i]);
            if (corr < 0.75) {
                misses++;
            }
        }
        double missRate = (double) misses / (double) n;
        return 1 - missRate;
    }


    /**
     * Calculates the similarity (a number within 0 and 1) between two strings.
     * http://stackoverflow.com/questions/955110/similarity-string-comparison-in-java
     */
    private static double similarity(String s1, String s2) {
        String longer = s1;
        String shorter = s2;

        if (s1.length() < s2.length()) { // longer should always have greater length
            longer = s2;
            shorter = s1;
        }
        int longerLength = longer.length();
        if (longerLength == 0) {
            return 1.0;
            /* both strings are zero length */ }
        double sim = (longerLength - editDistance(longer, shorter)) / (double) longerLength;
        LOGGER.debug("Longer string: " + longer + " Shorter string: " + shorter + " Similarity: " + sim);
        return sim;

    }

    /*
    * Levenshtein Edit Distance
    * http://stackoverflow.com/questions/955110/similarity-string-comparison-in-java
    */
    private static int editDistance(String s1, String s2) {
        String s1LowerCase = s1.toLowerCase();
        String s2LowerCase = s2.toLowerCase();

        int[] costs = new int[s2LowerCase.length() + 1];
        for (int i = 0; i <= s1LowerCase.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2LowerCase.length(); j++) {
                if (i == 0) {
                    costs[j] = j;
                } else if (j > 0) {
                    int newValue = costs[j - 1];
                    if (s1LowerCase.charAt(i - 1) != s2LowerCase.charAt(j - 1)) {
                        newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
                    }
                    costs[j - 1] = lastValue;
                    lastValue = newValue;

                }
            }
            if (i > 0) {
                costs[s2LowerCase.length()] = lastValue;
            }
        }
        LOGGER.debug("String 1: " + s1LowerCase + " String 2: " + s2LowerCase + " Distance: " + costs[s2LowerCase.length()]);
        return costs[s2LowerCase.length()];
    }


}
