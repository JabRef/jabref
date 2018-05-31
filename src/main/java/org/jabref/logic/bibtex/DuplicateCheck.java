package org.jabref.logic.bibtex;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.model.entry.FieldName.AUTHOR;
import static org.jabref.model.entry.FieldName.CHAPTER;
import static org.jabref.model.entry.FieldName.EDITION;
import static org.jabref.model.entry.FieldName.EDITOR;
import static org.jabref.model.entry.FieldName.JOURNAL;
import static org.jabref.model.entry.FieldName.PAGES;
import static org.jabref.model.entry.FieldName.TITLE;

/**
 * This class contains utility method for duplicate checking of entries.
 */
public class DuplicateCheck {
    private static final double DUPLICATE_THRESHOLD = 0.75; // The overall threshold to signal a duplicate pair

    private static final Logger LOGGER = LoggerFactory.getLogger(DuplicateCheck.class);
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
        DuplicateCheck.FIELD_WEIGHTS.put(AUTHOR, 2.5);
        DuplicateCheck.FIELD_WEIGHTS.put(EDITOR, 2.5);
        DuplicateCheck.FIELD_WEIGHTS.put(TITLE, 3.);
        DuplicateCheck.FIELD_WEIGHTS.put(JOURNAL, 2.);
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
    public static boolean isDuplicate(final BibEntry one, final BibEntry two, final BibDatabaseMode bibDatabaseMode) {
        if (haveSameIdentifier(one, two)) {
            return true;
        }

        if (haveDifferentEntryType(one, two) ||
                haveDifferentEditions(one, two) ||
                haveDifferentChaptersOrPagesOfTheSameBook(one, two)) {
            return false;
        }

        final EntryType type = EntryTypes.getTypeOrDefault(one.getType(), bibDatabaseMode);
        final double[] reqCmpResult = compareRequiredFields(type, one, two);

        if (isFarFromThreshold(reqCmpResult[0])) {
            // Far from the threshold value, so we base our decision on the required fields only
            return reqCmpResult[0] >= DuplicateCheck.DUPLICATE_THRESHOLD;
        }

        // Close to the threshold value, so we take a look at the optional fields, if any:
        return compareOptionalFields(type, one, two, reqCmpResult);
    }

    private static boolean haveSameIdentifier(final BibEntry one, final BibEntry two) {
        for (final String name : FieldName.getIdentifierFieldNames()) {
            if (one.getField(name).isPresent() && one.getField(name).equals(two.getField(name))) {
                return true;
            }
        }
        return false;
    }

    private static boolean haveDifferentEntryType(final BibEntry one, final BibEntry two) {
        return !one.getType().equals(two.getType());
    }

    private static boolean haveDifferentEditions(final BibEntry one, final BibEntry two) {
        final Optional<String> editionOne = one.getField(EDITION);
        final Optional<String> editionTwo = two.getField(EDITION);
        return editionOne.isPresent() &&
                editionTwo.isPresent() &&
                !editionOne.get().equals(editionTwo.get());
    }

    private static boolean haveDifferentChaptersOrPagesOfTheSameBook(final BibEntry one, final BibEntry two) {
        return compareSingleField(AUTHOR, one, two) == EQUAL &&
                compareSingleField(TITLE, one, two) == EQUAL &&
                (compareSingleField(CHAPTER, one, two) == NOT_EQUAL ||
                        compareSingleField(PAGES, one, two) == NOT_EQUAL);

    }

    private static double[] compareRequiredFields(final EntryType type, final BibEntry one, final BibEntry two) {
        final Collection<String> requiredFields = type.getRequiredFieldsFlat();
        return requiredFields == null
                ? new double[]{0., 0.}
                : DuplicateCheck.compareFieldSet(requiredFields, one, two);
    }

    private static boolean isFarFromThreshold(double value) {
        return Math.abs(value - DuplicateCheck.DUPLICATE_THRESHOLD) > DuplicateCheck.DOUBT_RANGE;
    }

    private static boolean compareOptionalFields(final EntryType type,
                                                 final BibEntry one,
                                                 final BibEntry two,
                                                 final double[] req) {
        final Collection<String> optionalFields = type.getOptionalFields();
        if (optionalFields == null) {
            return req[0] >= DuplicateCheck.DUPLICATE_THRESHOLD;
        }
        final double[] opt = DuplicateCheck.compareFieldSet(optionalFields, one, two);
        final double numerator = (DuplicateCheck.REQUIRED_WEIGHT * req[0] * req[1]) + (opt[0] * opt[1]);
        final double denominator = (req[1] * DuplicateCheck.REQUIRED_WEIGHT) + opt[1];
        final double totValue = numerator / denominator;
        return totValue >= DuplicateCheck.DUPLICATE_THRESHOLD;
    }

    private static double[] compareFieldSet(final Collection<String> fields, final BibEntry one, final BibEntry two) {
        double res = 0;
        double totWeights = 0.;
        for (final String field : fields) {
            final double weight = DuplicateCheck.FIELD_WEIGHTS.getOrDefault(field, 1.0);
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

    private static int compareSingleField(final String field, final BibEntry one, final BibEntry two) {
        final Optional<String> optionalStringOne = one.getField(field);
        final Optional<String> optionalStringTwo = two.getField(field);
        if (!optionalStringOne.isPresent()) {
            if (!optionalStringTwo.isPresent()) {
                return EMPTY_IN_BOTH;
            }
            return EMPTY_IN_ONE;
        } else if (!optionalStringTwo.isPresent()) {
            return EMPTY_IN_TWO;
        }

        // Both strings present
        final String stringOne = optionalStringOne.get();
        final String stringTwo = optionalStringTwo.get();

        if (InternalBibtexFields.getFieldProperties(field).contains(FieldProperty.PERSON_NAMES)) {
            return compareAuthorField(stringOne, stringTwo);

        } else if (PAGES.equals(field)) {
            return comparePagesField(stringOne, stringTwo);

        } else if (JOURNAL.equals(field)) {
            return compareJournalField(stringOne, stringTwo);

        } else if (CHAPTER.equals(field)) {
            return compareChapterField(stringOne, stringTwo);
        }

        return compareField(stringOne, stringTwo);
    }

    private static int compareAuthorField(final String stringOne, final String stringTwo) {
        // Specific for name fields.
        // Harmonise case:
        final String authorOne = AuthorList.fixAuthorLastNameOnlyCommas(stringOne, false).replace(" and ", " ").toLowerCase(Locale.ROOT);
        final String authorTwo = AuthorList.fixAuthorLastNameOnlyCommas(stringTwo, false).replace(" and ", " ").toLowerCase(Locale.ROOT);
        final double similarity = DuplicateCheck.correlateByWords(authorOne, authorTwo);
        if (similarity > 0.8) {
            return EQUAL;
        }
        return NOT_EQUAL;
    }

    /**
     * Pages can be given with a variety of delimiters, "-", "--", " - ", " -- ".
     * We do a replace to harmonize these to a simple "-"
     * After this, a simple test for equality should be enough
     */
    private static int comparePagesField(final String stringOne, final String stringTwo) {
        final String processedStringOne = stringOne.replaceAll("[- ]+", "-");
        final String processedStringTwo = stringTwo.replaceAll("[- ]+", "-");
        if (processedStringOne.equals(processedStringTwo)) {
            return EQUAL;
        }
        return NOT_EQUAL;
    }

    /**
     * We do not attempt to harmonize abbreviation state of the journal names,
     * but we remove periods from the names in case they are abbreviated with and without dots:
     */
    private static int compareJournalField(final String stringOne, final String stringTwo) {
        final String processedStringOne = stringOne.replace(".", "").toLowerCase(Locale.ROOT);
        final String processedStringTwo = stringTwo.replace(".", "").toLowerCase(Locale.ROOT);
        final double similarity = DuplicateCheck.correlateByWords(processedStringOne, processedStringTwo);
        if (similarity > 0.8) {
            return EQUAL;
        }
        return NOT_EQUAL;
    }

    private static int compareChapterField(final String stringOne, final String stringTwo) {
        final String processedStringOne = stringOne.replaceAll("(?i)chapter", "").trim();
        final String processedStringTwo = stringTwo.replaceAll("(?i)chapter", "").trim();
        return compareField(processedStringOne, processedStringTwo);
    }

    private static int compareField(final String stringOne, final String stringTwo) {
        final String processedStringOne = stringOne.toLowerCase(Locale.ROOT).trim();
        final String processedStringTwo = stringTwo.toLowerCase(Locale.ROOT).trim();
        final double similarity = DuplicateCheck.correlateByWords(processedStringOne, processedStringTwo);
        if (similarity > 0.8) {
            return EQUAL;
        }
        return NOT_EQUAL;
    }

    public static double compareEntriesStrictly(BibEntry one, BibEntry two) {
        final Set<String> allFields = new HashSet<>();
        allFields.addAll(one.getFieldNames());
        allFields.addAll(two.getFieldNames());

        int score = 0;
        for (final String field : allFields) {
            final Optional<String> stringOne = one.getField(field);
            final Optional<String> stringTwo = two.getField(field);
            if (stringOne.equals(stringTwo)) {
                score++;
            }
        }
        if (score == allFields.size()) {
            return 1.01; // Just to make sure we can use score > 1 without trouble.
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
    public static Optional<BibEntry> containsDuplicate(final BibDatabase database,
                                                       final BibEntry entry,
                                                       final BibDatabaseMode bibDatabaseMode) {
        for (final BibEntry other : database.getEntries()) {
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
    public static double correlateByWords(final String s1, final String s2) {
        final String[] w1 = s1.split("\\s");
        final String[] w2 = s2.split("\\s");
        final int n = Math.min(w1.length, w2.length);
        int misses = 0;
        for (int i = 0; i < n; i++) {
            double corr = similarity(w1[i], w2[i]);
            if (corr < 0.75) {
                misses++;
            }
        }
        final double missRate = (double) misses / (double) n;
        return 1 - missRate;
    }


    /*
     * Calculates the similarity (a number within 0 and 1) between two strings.
     * http://stackoverflow.com/questions/955110/similarity-string-comparison-in-java
     */
    private static double similarity(final String first, final String second) {
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
        final double distanceIgnoredCase = new StringSimilarity().editDistanceIgnoreCase(longer, shorter);
        final double similarity = (longerLength - distanceIgnoredCase) / (double) longerLength;
        LOGGER.debug("Longer string: " + longer + " Shorter string: " + shorter + " Similarity: " + similarity);
        return similarity;
    }
}
