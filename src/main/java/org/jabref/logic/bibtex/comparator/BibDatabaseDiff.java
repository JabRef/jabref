package org.jabref.logic.bibtex.comparator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.bibtex.DuplicateCheck;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

public class BibDatabaseDiff {

    private static final double MATCH_THRESHOLD = 0.4;
    private final Optional<MetaDataDiff> metaDataDiff;
    private final Optional<PreambleDiff> preambleDiff;
    private final List<BibStringDiff> bibStringDiffs;
    private final List<BibEntryDiff> entryDiffs;

    private BibDatabaseDiff(BibDatabaseContext originalDatabase, BibDatabaseContext newDatabase) {
        metaDataDiff = MetaDataDiff.compare(originalDatabase.getMetaData(), newDatabase.getMetaData());
        preambleDiff = PreambleDiff.compare(originalDatabase, newDatabase);
        bibStringDiffs = BibStringDiff.compare(originalDatabase.getDatabase(), newDatabase.getDatabase());

        // Sort both databases according to a common sort key.
        EntryComparator comparator = getEntryComparator();
        List<BibEntry> originalEntriesSorted = originalDatabase.getDatabase().getEntriesSorted(comparator);
        List<BibEntry> newEntriesSorted = newDatabase.getDatabase().getEntriesSorted(comparator);

        entryDiffs = compareEntries(originalEntriesSorted, newEntriesSorted);
    }

    private static EntryComparator getEntryComparator() {
        EntryComparator comparator = new EntryComparator(false, true, FieldName.TITLE);
        comparator = new EntryComparator(false, true, FieldName.AUTHOR, comparator);
        comparator = new EntryComparator(false, true, FieldName.YEAR, comparator);
        return comparator;
    }

    private static List<BibEntryDiff> compareEntries(List<BibEntry> originalEntries, List<BibEntry> newEntries) {
        List<BibEntryDiff> differences = new ArrayList<>();

        // Create pointers that are incremented as the entries of each base are used in
        // successive order from the beginning. Entries "further down" in the new database
        // can also be matched.
        int positionNew = 0;

        // Create a HashSet where we can put references to entries in the new
        // database that we have matched. This is to avoid matching them twice.
        Set<Integer> used = new HashSet<>(newEntries.size());
        Set<BibEntry> notMatched = new HashSet<>(originalEntries.size());

        // Loop through the entries of the original database, looking for exact matches in the new one.
        // We must finish scanning for exact matches before looking for near matches, to avoid an exact
        // match being "stolen" from another entry.
        mainLoop:
        for (BibEntry originalEntry : originalEntries) {
            // First check if the similarly placed entry in the other base matches exactly.
            if (!used.contains(positionNew) && (positionNew < newEntries.size())) {
                double score = DuplicateCheck.compareEntriesStrictly(originalEntry, newEntries.get(positionNew));
                if (score > 1) {
                    used.add(positionNew);
                    positionNew++;
                    continue;
                }
            }
            // No? Then check if another entry matches exactly.
            for (int i = positionNew + 1; i < newEntries.size(); i++) {
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
        for (Iterator<BibEntry> iteratorNotMatched = notMatched.iterator(); iteratorNotMatched.hasNext(); ) {
            BibEntry originalEntry = iteratorNotMatched.next();

            // These two variables will keep track of which entry most closely matches the one we're looking at.
            double bestMatch = 0;
            int bestMatchIndex = -1;
            if (positionNew < (newEntries.size() - 1)) {
                for (int i = positionNew; i < newEntries.size(); i++) {
                    if (!used.contains(i)) {
                        double score = DuplicateCheck.compareEntriesStrictly(originalEntry, newEntries.get(i));
                        if (score > bestMatch) {
                            bestMatch = score;
                            bestMatchIndex = i;
                        }
                    }
                }
            }

            if (bestMatch > MATCH_THRESHOLD) {
                used.add(bestMatchIndex);
                iteratorNotMatched.remove();

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
