package net.sf.jabref.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.AuthorList;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.EntryType;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.entry.FieldProperty;
import net.sf.jabref.model.entry.InternalBibtexFields;

import info.debatty.java.stringsimilarity.Levenshtein;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class contains utility method for duplicate checking of entries.
 */
public class DuplicateCheck {
    private static final Log LOGGER = LogFactory.getLog(DuplicateCheck.class);

    /**
     * Enumeration for indicating the result of a duplicate check
     */
    private enum CheckResult {
        NOT_EQUAL,
        EQUAL,
        EMPTY_IN_ONE,
        EMPTY_IN_TWO,
        EMPTY_IN_BOTH
    }

    public static double duplicateThreshold = 0.75; // The overall threshold to signal a duplicate pair
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

    /**
     * Checks if the two entries represent the same publication.
     *
     * @param one BibEntry
     * @param two BibEntry
     * @return boolean
     */
    public static boolean isDuplicate(BibEntry one, BibEntry two, BibDatabaseMode bibDatabaseMode) {
        // same type is mandatory
        if (!one.getType().equals(two.getType())) {
            return false;
        }

        // check for equal required fields
        EntryType entryType = EntryTypes.getTypeOrDefault(one.getType(), bibDatabaseMode);
        List<String> requiredFields = entryType.getRequiredFieldsFlat();
        double[] req;
        if (requiredFields == null) {
            req = new double[]{0., 0.};
        } else {
            req = DuplicateCheck.compareFieldSet(requiredFields, one, two);
        }

        if (Math.abs(req[0] - DuplicateCheck.duplicateThreshold) > DuplicateCheck.DOUBT_RANGE) {
            // Far from the threshold value, so we base our decision on the req. fields only
            return req[0] >= DuplicateCheck.duplicateThreshold;
        }
        // Close to the threshold value, so we take a look at the optional fields, if any:
        // check for equal optional fields
        List<String> optionalFields = entryType.getOptionalFields();
        if (optionalFields != null) {
            double[] opt = DuplicateCheck.compareFieldSet(optionalFields, one, two);
            double totValue = ((DuplicateCheck.REQUIRED_WEIGHT * req[0] * req[1]) + (opt[0] * opt[1])) / ((req[1] * DuplicateCheck.REQUIRED_WEIGHT) + opt[1]);
            return totValue >= DuplicateCheck.duplicateThreshold;
        }
        return req[0] >= DuplicateCheck.duplicateThreshold;
    }

    private static double[] compareFieldSet(List<String> fields, BibEntry one, BibEntry two) {
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

            CheckResult result = DuplicateCheck.compareSingleField(field, one, two);
            if (result == CheckResult.EQUAL) {
                res += weight;
            } else if (result == CheckResult.EMPTY_IN_BOTH) {
                totWeights -= weight;
            }
        }
        if (totWeights > 0) {
            return new double[]{res / totWeights, totWeights};
        }
        return new double[] {0.5, 0.0};
    }

    private static CheckResult compareSingleField(String field, BibEntry one, BibEntry two) {
        Optional<String> optionalStringOne = one.getField(field);
        Optional<String> optionalStringTwo = two.getField(field);
        if (!optionalStringOne.isPresent()) {
            if (!optionalStringTwo.isPresent()) {
                return CheckResult.EMPTY_IN_BOTH;
            }
            return CheckResult.EMPTY_IN_ONE;
        } else if (!optionalStringTwo.isPresent()) {
            return CheckResult.EMPTY_IN_TWO;
        }

        // Both strings present
        String stringOne = optionalStringOne.get();
        String stringTwo = optionalStringTwo.get();

        if (InternalBibtexFields.getFieldProperties(field).contains(FieldProperty.PERSON_NAMES)) {
            // Specific for name fields.
            // Harmonise case:
            String authorOne = AuthorList.fixAuthorLastNameOnlyCommas(stringOne, false).replace(" and ", " ").toLowerCase();
            String authorTwo = AuthorList.fixAuthorLastNameOnlyCommas(stringTwo, false).replace(" and ", " ").toLowerCase();
            double similarity = DuplicateCheck.correlateByWords(authorOne, authorTwo);
            if (similarity > 0.8) {
                return CheckResult.EQUAL;
            }
            return CheckResult.NOT_EQUAL;
        } else if (FieldName.PAGES.equals(field)) {
            // Pages can be given with a variety of delimiters, "-", "--", " - ", " -- ".
            // We do a replace to harmonize these to a simple "-":
            // After this, a simple test for equality should be enough:
            stringOne = stringOne.replaceAll("[- ]+", "-");
            stringTwo = stringTwo.replaceAll("[- ]+", "-");
            if (stringOne.equals(stringTwo)) {
                return CheckResult.EQUAL;
            }
            return CheckResult.NOT_EQUAL;
        } else if (FieldName.JOURNAL.equals(field)) {
            // We do not attempt to harmonize abbreviation state of the journal names,
            // but we remove periods from the names in case they are abbreviated with
            // and without dots:
            stringOne = stringOne.replace(".", "").toLowerCase();
            stringTwo = stringTwo.replace(".", "").toLowerCase();
            double similarity = DuplicateCheck.correlateByWords(stringOne, stringTwo);
            if (similarity > 0.8) {
                return CheckResult.EQUAL;
            }
            return CheckResult.NOT_EQUAL;
        } else {
            stringOne = stringOne.toLowerCase();
            stringTwo = stringTwo.toLowerCase();
            double similarity = DuplicateCheck.correlateByWords(stringOne, stringTwo);
            if (similarity > 0.8) {
                return CheckResult.EQUAL;
            }
            return CheckResult.NOT_EQUAL;
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
     * TODO: strange algorithm as when there are only words inserted this gives a bad value, e.g.,
     * a test -> this a test (0.0)
     * characterization -> characterization of me (1.0)
     *
     * @param s1 The first string
     * @param s2 The second string
     * @return a value in the interval [0, 1] indicating the degree of match.
     */
    public static double correlateByWords(String s1, String s2) {
        String[] words1 = s1.split("\\s");
        String[] words2 = s2.split("\\s");
        int n = Math.min(words1.length, words2.length);
        int misses = 0;
        for (int i = 0; i < n; i++) {
            double corr = similarity(words1[i], words2[i]);
            if (corr < 0.75) {
                misses++;
            }
        }
        double missRate = (double) misses / (double) n;
        return 1 - missRate;
    }


    /**
     * Calculates the similarity between two strings.
     * <p>
     * The result will be in the interval [0;1].
     */
    private static double similarity(String s1, String s2) {
        // method is performance optimized
        String longerString = s1;
        String shorterString = s2;

        // determine longer string
        if (s1.length() < s2.length()) {
            longerString = s2;
            shorterString = s1;
        }

        int longerLength = longerString.length();
        // both strings are zero length
        if (longerLength == 0) {
            return 1.0;
        }

        return (longerLength - levenshteinDistance(longerString, shorterString)) / longerLength;
    }

    private static double levenshteinDistance(String s1, String s2) {
        return new Levenshtein().distance(s1.toLowerCase(Locale.ENGLISH), s2.toLowerCase(Locale.ENGLISH));
    }
}
