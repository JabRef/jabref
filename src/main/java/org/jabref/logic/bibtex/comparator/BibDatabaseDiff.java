package org.jabref.logic.bibtex.comparator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.database.DuplicateCheck;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BibDatabaseDiff {

    private static final Logger LOGGER = LoggerFactory.getLogger(BibDatabaseDiff.class);

    private static final double MATCH_THRESHOLD = 0.4;
    private final Optional<MetaDataDiff> metaDataDiff;
    private final Optional<PreambleDiff> preambleDiff;
    private final List<BibStringDiff> bibStringDiffs;
    private final List<BibEntryDiff> entryDiffs;

    private BibDatabaseDiff(BibDatabaseContext originalDatabase, BibDatabaseContext newDatabase) {
        metaDataDiff = MetaDataDiff.compare(originalDatabase.getMetaData(), newDatabase.getMetaData());
        preambleDiff = PreambleDiff.compare(originalDatabase, newDatabase);
        bibStringDiffs = BibStringDiff.compare(originalDatabase.getDatabase(), newDatabase.getDatabase());
        entryDiffs = getBibEntryDiffs(originalDatabase, newDatabase);
        if (LOGGER.isDebugEnabled() && !isEmpty()) {
            LOGGER.debug("Differences detected");
            metaDataDiff.ifPresent(diff -> LOGGER.debug("Metadata differences: {}", diff));
            preambleDiff.ifPresent(diff -> LOGGER.debug("Premble differences: {}", diff));
            LOGGER.debug("BibString differences: {}", bibStringDiffs);
            LOGGER.debug("Entry differences: {}", entryDiffs);
        }
    }

    private boolean isEmpty() {
        return !metaDataDiff.isPresent() && !preambleDiff.isPresent() && bibStringDiffs.isEmpty() && entryDiffs.isEmpty();
    }

    private List<BibEntryDiff> getBibEntryDiffs(BibDatabaseContext originalDatabase, BibDatabaseContext newDatabase) {
        final List<BibEntryDiff> entryDiffs;
        // Sort both databases according to a common sort key.
        EntryComparator comparator = getEntryComparator();
        List<BibEntry> originalEntriesSorted = originalDatabase.getDatabase().getEntriesSorted(comparator);
        List<BibEntry> newEntriesSorted = newDatabase.getDatabase().getEntriesSorted(comparator);

        // Ignore empty entries
        originalEntriesSorted.removeIf(BibEntry::isEmpty);
        newEntriesSorted.removeIf(BibEntry::isEmpty);

        entryDiffs = compareEntries(originalEntriesSorted, newEntriesSorted, originalDatabase.getMode());
        return entryDiffs;
    }

    private static EntryComparator getEntryComparator() {
        EntryComparator comparator = new EntryComparator(false, true, StandardField.TITLE);
        comparator = new EntryComparator(false, true, StandardField.AUTHOR, comparator);
        comparator = new EntryComparator(false, true, StandardField.YEAR, comparator);
        return comparator;
    }

    private static List<BibEntryDiff> compareEntries(List<BibEntry> originalEntries, List<BibEntry> newEntries, BibDatabaseMode mode) {
        List<BibEntryDiff> differences = new ArrayList<>();

        // Prevent IndexOutOfBoundException
        if (newEntries.isEmpty()) {
            return differences;
        }

        // Create a HashSet where we can put references to entries in the new
        // database that we have matched. This is to avoid matching them twice.
        Set<Integer> matchedEntries = new HashSet<>(newEntries.size());
        Set<BibEntry> notMatched = new HashSet<>(originalEntries.size());

        // Loop through the entries of the original database, looking for exact matches in the new one.
        // We must finish scanning for exact matches before looking for near matches, to avoid an exact
        // match being "stolen" from another entry.
        mainLoop:
        for (BibEntry originalEntry : originalEntries) {
            for (int i = 0; i < newEntries.size(); i++) {
                if (!matchedEntries.contains(i)) {
                    double score = DuplicateCheck.compareEntriesStrictly(originalEntry, newEntries.get(i));
                    if (score > 1) {
                        matchedEntries.add(i);
                        continue mainLoop;
                    }
                }
            }

            // No? Add this entry to the list of non-matched entries.
            notMatched.add(originalEntry);
        }

        // Now we've found all exact matches, look through the remaining entries, looking for close matches.
        DuplicateCheck duplicateCheck = new DuplicateCheck(new BibEntryTypesManager());
        for (BibEntry originalEntry : notMatched) {
            // These two variables will keep track of which entry most closely matches the one we're looking at.
            double bestMatch = 0;
            int bestMatchIndex = 0;
            for (int i = 0; i < newEntries.size(); i++) {
                if (!matchedEntries.contains(i)) {
                    double score = DuplicateCheck.compareEntriesStrictly(originalEntry, newEntries.get(i));
                    if (score > bestMatch) {
                        bestMatch = score;
                        bestMatchIndex = i;
                    }
                }
            }
            BibEntry bestEntry = newEntries.get(bestMatchIndex);
            if (bestMatch > MATCH_THRESHOLD
                    || hasEqualCitationKey(originalEntry, bestEntry)
                    || duplicateCheck.isDuplicate(originalEntry, bestEntry, mode)) {
                matchedEntries.add(bestMatchIndex);
                differences.add(new BibEntryDiff(originalEntry, newEntries.get(bestMatchIndex)));
            } else {
                differences.add(new BibEntryDiff(originalEntry, null));
            }
        }

        // Finally, look if there are still untouched entries in the new database. These may have been added.
        for (int i = 0; i < newEntries.size(); i++) {
            if (!matchedEntries.contains(i)) {
                differences.add(new BibEntryDiff(null, newEntries.get(i)));
            }
        }

        return differences;
    }

    private static boolean hasEqualCitationKey(BibEntry oneEntry, BibEntry twoEntry) {
        return oneEntry.hasCitationKey() && twoEntry.hasCitationKey() && oneEntry.getCitationKey().equals(twoEntry.getCitationKey());
    }

    public static BibDatabaseDiff compare(BibDatabaseContext base, BibDatabaseContext changed) {
        return new BibDatabaseDiff(base, changed);
    }

    public Optional<MetaDataDiff> getMetaDataDifferences() {
        return metaDataDiff;
    }

    public Optional<PreambleDiff> getPreambleDifferences() {
        return preambleDiff;
    }

    public List<BibStringDiff> getBibStringDifferences() {
        return bibStringDiffs;
    }

    public List<BibEntryDiff> getEntryDifferences() {
        return entryDiffs;
    }
}
