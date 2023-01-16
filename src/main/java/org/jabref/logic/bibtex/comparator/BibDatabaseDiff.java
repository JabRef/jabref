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

public class BibDatabaseDiff {

    private static final double MATCH_THRESHOLD = 0.4;
    private final MetaDataDiff metaDataDiff;
    private final PreambleDiff preambleDiff;
    private final List<BibStringDiff> bibStringDiffs;
    private final List<BibEntryDiff> entryDiffs;

    private BibDatabaseDiff(BibDatabaseContext originalDatabase, BibDatabaseContext newDatabase) {
        metaDataDiff = MetaDataDiff.compare(originalDatabase.getMetaData(), newDatabase.getMetaData()).orElse(null);
        preambleDiff = PreambleDiff.compare(originalDatabase, newDatabase).orElse(null);
        bibStringDiffs = BibStringDiff.compare(originalDatabase.getDatabase(), newDatabase.getDatabase());

        // Sort both databases according to a common sort key.
        EntryComparator comparator = getEntryComparator();
        List<BibEntry> originalEntriesSorted = originalDatabase.getDatabase().getEntriesSorted(comparator);
        List<BibEntry> newEntriesSorted = newDatabase.getDatabase().getEntriesSorted(comparator);

        entryDiffs = compareEntries(originalEntriesSorted, newEntriesSorted, originalDatabase.getMode());
    }

    private static EntryComparator getEntryComparator() {
        EntryComparator comparator = new EntryComparator(false, true, StandardField.TITLE);
        comparator = new EntryComparator(false, true, StandardField.AUTHOR, comparator);
        comparator = new EntryComparator(false, true, StandardField.YEAR, comparator);
        return comparator;
    }

    private static List<BibEntryDiff> compareEntries(List<BibEntry> originalEntries, List<BibEntry> newEntries, BibDatabaseMode mode) {
        List<BibEntryDiff> differences = new ArrayList<>();

        // Create a HashSet where we can put references to entries in the new
        // database that we have matched. This is to avoid matching them twice.
        Set<Integer> used = new HashSet<>(newEntries.size());
        Set<BibEntry> notMatched = new HashSet<>(originalEntries.size());

        // Loop through the entries of the original database, looking for exact matches in the new one.
        // We must finish scanning for exact matches before looking for near matches, to avoid an exact
        // match being "stolen" from another entry.
        mainLoop:
        for (BibEntry originalEntry : originalEntries) {
            for (int i = 0; i < newEntries.size(); i++) {
                if (!used.contains(i)) {
                    double score = DuplicateCheck.compareEntriesStrictly(originalEntry, newEntries.get(i));
                    if (score > 1) {
                        used.add(i);
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
                if (!used.contains(i)) {
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
                used.add(bestMatchIndex);
                differences.add(new BibEntryDiff(originalEntry, newEntries.get(bestMatchIndex)));
            } else {
                differences.add(new BibEntryDiff(originalEntry, null));
            }
        }

        // Finally, look if there are still untouched entries in the new database. These may have been added.
        for (int i = 0; i < newEntries.size(); i++) {
            if (!used.contains(i)) {
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
        return Optional.ofNullable(metaDataDiff);
    }

    public Optional<PreambleDiff> getPreambleDifferences() {
        return Optional.ofNullable(preambleDiff);
    }

    public List<BibStringDiff> getBibStringDifferences() {
        return bibStringDiffs;
    }

    public List<BibEntryDiff> getEntryDifferences() {
        return entryDiffs;
    }
}
