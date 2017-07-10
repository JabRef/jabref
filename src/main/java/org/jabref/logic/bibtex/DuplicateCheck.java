package org.jabref.logic.bibtex;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.util.strings.StringSimilarity;
import org.jabref.model.EntryTypes;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.EntryType;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.FieldProperty;
import org.jabref.model.entry.InternalBibtexFields;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class contains utility method for duplicate checking of entries.
 */
public class DuplicateCheck {
    private static final double DUPLICATE_THRESHOLD = 0.75; // The overall threshold to signal a duplicate pair

    private static final Log LOGGER = LogFactory.getLog(DuplicateCheck.class);
    /*
     * Integer values for indicating result of duplicate check (for entries):
     */
    private static final int NOT_EQUAL = 0;
    private static final int EQUAL = 1;
    private static final int EMPTY_IN_ONE = 2;
    private static final int EMPTY_IN_TWO = 3;

    private static final int EMPTY_IN_BOTH = 4;
    // Non-required fields are investigated only if the required fields give a value within
    // the doubt range of the threshold:
    private static final double DOUBT_RANGE = 0.05;

    private static final double REQUIRED_WEIGHT = 3; // Weighting of all required fields

    // Extra weighting of those fields that are most likely to provide correct duplicate detection:
    private static final Map<String, Double> FIELD_WEIGHTS = new HashMap<>();

    static {
        DuplicateCheck.FIELD_WEIGHTS.put(FieldName.AUTHOR, 2.5);
        DuplicateCheck.FIELD_WEIGHTS.put(FieldName.EDITOR, 2.5);
        DuplicateCheck.FIELD_WEIGHTS.put(FieldName.TITLE, 3.);
        DuplicateCheck.FIELD_WEIGHTS.put(FieldName.JOURNAL, 2.);
    }

    private DuplicateCheck() {
    }

    /**
     * Checks if the two entries represent the same publication.
     *
     * @param one BibEntry
     * @param two BibEntry
     * @return boolean
     */
    public static boolean isDuplicate(BibEntry one, BibEntry two, BibDatabaseMode bibDatabaseMode) {
        if (haveSameIdentifier(one, two)) {
            return true;
        }

        // same entry type
        if (!one.getType().equals(two.getType())) {
            return false;
        }

        if (haveDifferentEditions(one, two)) {
            return false;
        }

        EntryType type = EntryTypes.getTypeOrDefault(one.getType(), bibDatabaseMode);
        // The check if they have the same required fields:
        List<String> var = type.getRequiredFieldsFlat();
        double[] req;
        if (var == null) {
            req = new double[]{0., 0.};
        } else {
            req = DuplicateCheck.compareFieldSet(var, one, two);
        }

        if (Math.abs(req[0] - DuplicateCheck.DUPLICATE_THRESHOLD) > DuplicateCheck.DOUBT_RANGE) {
            // Far from the threshold value, so we base our decision on the req. fields only
            return req[0] >= DuplicateCheck.DUPLICATE_THRESHOLD;
        }
        // Close to the threshold value, so we take a look at the optional fields, if any:
        List<String> optionalFields = type.getOptionalFields();
        if (optionalFields != null) {
            double[] opt = DuplicateCheck.compareFieldSet(optionalFields, one, two);
            double totValue = ((DuplicateCheck.REQUIRED_WEIGHT * req[0] * req[1]) + (opt[0] * opt[1])) / ((req[1] * DuplicateCheck.REQUIRED_WEIGHT) + opt[1]);
            return totValue >= DuplicateCheck.DUPLICATE_THRESHOLD;
        }
        return req[0] >= DuplicateCheck.DUPLICATE_THRESHOLD;
    }

    private static boolean haveDifferentEditions(BibEntry one, BibEntry two) {
        if (one.getField(FieldName.EDITION).isPresent() && two.getField(FieldName.EDITION).isPresent()) {
            if (!one.getField(FieldName.EDITION).get().equals(two.getField(FieldName.EDITION).get())) {
                return true;
            }
        }
        return false;
    }

    private static boolean haveSameIdentifier(BibEntry one, BibEntry two) {
        for (String name : FieldName.getIdentifierFieldNames()) {
            if (one.getField(name).isPresent() && one.getField(name).equals(two.getField(name))) {
                return true;
            }
        }
        return false;
    }

    private static double[] compareFieldSet(List<String> fields, BibEntry one, BibEntry two) {
        double res = 0;
        double totWeights = 0.;
        for (String field : fields) {
            double weight;
            weight = DuplicateCheck.FIELD_WEIGHTS.getOrDefault(field, 1.0);
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
        return new double[]{0.5, 0.0};
    }

    private static int compareSingleField(String field, BibEntry one, BibEntry two) {
        Optional<String> optionalStringOne = one.getField(field);
        Optional<String> optionalStringTwo = two.getField(field);
        if (!optionalStringOne.isPresent()) {
            if (!optionalStringTwo.isPresent()) {
                return EMPTY_IN_BOTH;
            }
            return EMPTY_IN_ONE;
        } else if (!optionalStringTwo.isPresent()) {
            return EMPTY_IN_TWO;
        }

        // Both strings present
        String stringOne = optionalStringOne.get();
        String stringTwo = optionalStringTwo.get();

        if (InternalBibtexFields.getFieldProperties(field).contains(FieldProperty.PERSON_NAMES)) {
            // Specific for name fields.
            // Harmonise case:
            String authorOne = AuthorList.fixAuthorLastNameOnlyCommas(stringOne, false).replace(" and ", " ").toLowerCase(Locale.ROOT);
            String authorTwo = AuthorList.fixAuthorLastNameOnlyCommas(stringTwo, false).replace(" and ", " ").toLowerCase(Locale.ROOT);
            double similarity = DuplicateCheck.correlateByWords(authorOne, authorTwo);
            if (similarity > 0.8) {
                return EQUAL;
            }
            return NOT_EQUAL;
        } else if (FieldName.PAGES.equals(field)) {
            // Pages can be given with a variety of delimiters, "-", "--", " - ", " -- ".
            // We do a replace to harmonize these to a simple "-":
            // After this, a simple test for equality should be enough:
            stringOne = stringOne.replaceAll("[- ]+", "-");
            stringTwo = stringTwo.replaceAll("[- ]+", "-");
            if (stringOne.equals(stringTwo)) {
                return EQUAL;
            }
            return NOT_EQUAL;
        } else if (FieldName.JOURNAL.equals(field)) {
            // We do not attempt to harmonize abbreviation state of the journal names,
            // but we remove periods from the names in case they are abbreviated with
            // and without dots:
            stringOne = stringOne.replace(".", "").toLowerCase(Locale.ROOT);
            stringTwo = stringTwo.replace(".", "").toLowerCase(Locale.ROOT);
            double similarity = DuplicateCheck.correlateByWords(stringOne, stringTwo);
            if (similarity > 0.8) {
                return EQUAL;
            }
            return NOT_EQUAL;
        } else {
            stringOne = stringOne.toLowerCase(Locale.ROOT);
            stringTwo = stringTwo.toLowerCase(Locale.ROOT);
            double similarity = DuplicateCheck.correlateByWords(stringOne, stringTwo);
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
            Optional<String> stringOne = one.getField(field);
            Optional<String> stringTwo = two.getField(field);
            if (stringOne.equals(stringTwo)) {
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
     * @return The first duplicate entry found. Empty Optional if no duplicates are found.
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
     * @param s1 The first string
     * @param s2 The second string
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


    /*
     * Calculates the similarity (a number within 0 and 1) between two strings.
     * http://stackoverflow.com/questions/955110/similarity-string-comparison-in-java
     */
    private static double similarity(String first, String second) {
        String longer = first;
        String shorter = second;

        if (first.length() < second.length()) {
            longer = second;
            shorter = first;
        }

        int longerLength = longer.length();
        // both strings are zero length
        if (longerLength == 0) {
            return 1.0;
        }
        double sim = (longerLength - new StringSimilarity().editDistanceIgnoreCase(longer, shorter)) / (double) longerLength;
        LOGGER.debug("Longer string: " + longer + " Shorter string: " + shorter + " Similarity: " + sim);
        return sim;
    }
}
