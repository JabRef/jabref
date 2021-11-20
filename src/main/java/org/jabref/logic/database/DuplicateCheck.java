package org.jabref.logic.database;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.logic.util.OS;
import org.jabref.logic.util.strings.StringSimilarity;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.FieldProperty;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.identifier.ISBN;
import org.jabref.model.strings.StringUtil;

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Map<Field, Double> FIELD_WEIGHTS = new HashMap<>();

    static {
        DuplicateCheck.FIELD_WEIGHTS.put(StandardField.AUTHOR, 2.5);
        DuplicateCheck.FIELD_WEIGHTS.put(StandardField.EDITOR, 2.5);
        DuplicateCheck.FIELD_WEIGHTS.put(StandardField.TITLE, 3.);
        DuplicateCheck.FIELD_WEIGHTS.put(StandardField.JOURNAL, 2.);
        DuplicateCheck.FIELD_WEIGHTS.put(StandardField.NOTE, 0.1);
        DuplicateCheck.FIELD_WEIGHTS.put(StandardField.COMMENT, 0.1);
        DuplicateCheck.FIELD_WEIGHTS.put(StandardField.DOI, 3.);
    }

    private final BibEntryTypesManager entryTypesManager;

    public DuplicateCheck(BibEntryTypesManager entryTypesManager) {
        this.entryTypesManager = entryTypesManager;
    }

    private static boolean haveSameIdentifier(final BibEntry one, final BibEntry two) {
        for (final Field name : FieldFactory.getIdentifierFieldNames()) {
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
        final Optional<String> editionOne = one.getField(StandardField.EDITION);
        final Optional<String> editionTwo = two.getField(StandardField.EDITION);
        return editionOne.isPresent() &&
                editionTwo.isPresent() &&
                !editionOne.get().equals(editionTwo.get());
    }

    private static boolean haveDifferentChaptersOrPagesOfTheSameBook(final BibEntry one, final BibEntry two) {
        return (compareSingleField(StandardField.AUTHOR, one, two) == EQUAL) &&
                (compareSingleField(StandardField.TITLE, one, two) == EQUAL) &&
                ((compareSingleField(StandardField.CHAPTER, one, two) == NOT_EQUAL) ||
                        (compareSingleField(StandardField.PAGES, one, two) == NOT_EQUAL));
    }

    private static double[] compareRequiredFields(final BibEntryType type, final BibEntry one, final BibEntry two) {
        final Set<OrFields> requiredFields = type.getRequiredFields();
        return requiredFields.isEmpty()
                ? new double[] {0., 0.}
                : DuplicateCheck.compareFieldSet(requiredFields.stream().map(OrFields::getPrimary).collect(Collectors.toSet()), one, two);
    }

    private static boolean isFarFromThreshold(double value) {
        if (value < 0.0) {
            LOGGER.debug("Value {} is below zero. Should not happen", value);
        }
        return value - DuplicateCheck.DUPLICATE_THRESHOLD > DuplicateCheck.DOUBT_RANGE;
    }

    private static boolean compareOptionalFields(final BibEntryType type,
                                                 final BibEntry one,
                                                 final BibEntry two,
                                                 final double[] req) {
        final Set<BibField> optionalFields = type.getOptionalFields();
        if (optionalFields.isEmpty()) {
            return req[0] >= DuplicateCheck.DUPLICATE_THRESHOLD;
        }
        final double[] opt = DuplicateCheck.compareFieldSet(optionalFields.stream().map(BibField::getField).collect(Collectors.toSet()), one, two);
        final double numerator = (DuplicateCheck.REQUIRED_WEIGHT * req[0] * req[1]) + (opt[0] * opt[1]);
        final double denominator = (req[1] * DuplicateCheck.REQUIRED_WEIGHT) + opt[1];
        final double totValue = numerator / denominator;
        return totValue >= DuplicateCheck.DUPLICATE_THRESHOLD;
    }

    private static double[] compareFieldSet(final Collection<Field> fields, final BibEntry one, final BibEntry two) {
        if (fields.isEmpty()) {
            return new double[] {0.0, 0.0};
        }
        double equalWeights = 0;
        double totalWeights = 0.;
        for (final Field field : fields) {
            final double currentWeight = DuplicateCheck.FIELD_WEIGHTS.getOrDefault(field, 1.0);
            totalWeights += currentWeight;
            int result = DuplicateCheck.compareSingleField(field, one, two);
            if (result == EQUAL) {
                equalWeights += currentWeight;
            } else if (result == EMPTY_IN_BOTH) {
                totalWeights -= currentWeight;
            }
        }
        if (totalWeights > 0) {
            return new double[] {equalWeights / totalWeights, totalWeights};
        }
        // all fields are empty in both --> have no difference at all
        return new double[] {0.0, 0.0};
    }

    private static int compareSingleField(final Field field, final BibEntry one, final BibEntry two) {
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

        if (field.getProperties().contains(FieldProperty.PERSON_NAMES)) {
            return compareAuthorField(stringOne, stringTwo);
        } else if (StandardField.PAGES.equals(field)) {
            return comparePagesField(stringOne, stringTwo);
        } else if (StandardField.JOURNAL.equals(field)) {
            return compareJournalField(stringOne, stringTwo);
        } else if (StandardField.CHAPTER.equals(field)) {
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
        final String processedStringOne = StringUtil.unifyLineBreaks(stringOne.toLowerCase(Locale.ROOT).trim(), OS.NEWLINE);
        final String processedStringTwo = StringUtil.unifyLineBreaks(stringTwo.toLowerCase(Locale.ROOT).trim(), OS.NEWLINE);
        final double similarity = DuplicateCheck.correlateByWords(processedStringOne, processedStringTwo);
        if (similarity > 0.8) {
            return EQUAL;
        }
        return NOT_EQUAL;
    }

    public static double compareEntriesStrictly(BibEntry one, BibEntry two) {
        final Set<Field> allFields = new HashSet<>();
        allFields.addAll(one.getFields());
        allFields.addAll(two.getFields());

        int score = 0;
        for (final Field field : allFields) {
            if (isSingleFieldEqual(one, two, field)) {
                score++;
            }
        }
        if (score == allFields.size()) {
            return 1.01; // Just to make sure we can use score > 1 without trouble.
        }
        return (double) score / allFields.size();
    }

    private static boolean isSingleFieldEqual(BibEntry one, BibEntry two, Field field) {
        final Optional<String> stringOne = one.getField(field);
        final Optional<String> stringTwo = two.getField(field);
        if (stringOne.isEmpty() && stringTwo.isEmpty()) {
            return true;
        }
        if (stringOne.isEmpty() || stringTwo.isEmpty()) {
            return false;
        }
        return (StringUtil.unifyLineBreaks(stringOne.get(), OS.NEWLINE).equals(
                StringUtil.unifyLineBreaks(stringTwo.get(), OS.NEWLINE)));
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

    /**
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
        final double similarity = (longerLength - distanceIgnoredCase) / longerLength;
        LOGGER.debug("Longer string: {} Shorter string: {} Similarity: {}", longer, shorter, similarity);
        return similarity;
    }

    /**
     * Checks if the two entries represent the same publication.
     *
     * @param one BibEntry
     * @param two BibEntry
     * @return boolean
     */
    public boolean isDuplicate(final BibEntry one, final BibEntry two, final BibDatabaseMode bibDatabaseMode) {
        if (haveSameIdentifier(one, two)) {
            return true;
        }

        // check DOI
        Optional<DOI> oneDOI = one.getDOI();
        Optional<DOI> twoDOI = two.getDOI();
        if (oneDOI.isPresent() && twoDOI.isPresent()) {
            return Objects.equals(oneDOI, twoDOI);
        }
        // check ISBN
        Optional<ISBN> oneISBN = one.getISBN();
        Optional<ISBN> twoISBN = two.getISBN();
        if (oneISBN.isPresent() && twoISBN.isPresent()) {
            return Objects.equals(oneISBN, twoISBN);
        }

        if (haveDifferentEntryType(one, two) ||
                haveDifferentEditions(one, two) ||
                haveDifferentChaptersOrPagesOfTheSameBook(one, two)) {
            return false;
        }

        final Optional<BibEntryType> type = entryTypesManager.enrich(one.getType(), bibDatabaseMode);
        if (type.isPresent()) {
            BibEntryType entryType = type.get();
            final double[] reqCmpResult = compareRequiredFields(entryType, one, two);

            if (isFarFromThreshold(reqCmpResult[0])) {
                // Far from the threshold value, so we base our decision on the required fields only
                return reqCmpResult[0] >= DuplicateCheck.DUPLICATE_THRESHOLD;
            }

            // Close to the threshold value, so we take a look at the optional fields, if any:
            if (compareOptionalFields(type.get(), one, two, reqCmpResult)) {
                return true;
            }
        }
        // if type is not present, so simply compare fields without any distinction between optional/required
        // In case both required and optional fields are equal, we also use this fallback
        return compareFieldSet(Sets.union(one.getFields(), two.getFields()), one, two)[0] >= DuplicateCheck.DUPLICATE_THRESHOLD;
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
    public Optional<BibEntry> containsDuplicate(final BibDatabase database,
                                                final BibEntry entry,
                                                final BibDatabaseMode bibDatabaseMode) {

        return database.getEntries().stream().filter(other -> isDuplicate(entry, other, bibDatabaseMode)).findFirst();
    }
}
