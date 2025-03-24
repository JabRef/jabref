package org.jabref.logic.bibtex.comparator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public class GitBibDatabaseDiff {
    private final BibDatabaseDiff baseToLocalDiff;
    private final BibDatabaseDiff baseToRemoteDiff;
    private final List<GitBibEntryDiff> entryDifferences;
    private final Optional<MetaDataDiff> metaDataDifferences;

    private GitBibDatabaseDiff(BibDatabaseContext base, BibDatabaseContext local, BibDatabaseContext remote) {
        this.baseToLocalDiff = BibDatabaseDiff.compare(base, local);
        this.baseToRemoteDiff = BibDatabaseDiff.compare(base, remote);

        this.entryDifferences = findEntryDifferences(base, local, remote);
        this.metaDataDifferences = findMetaDataDifferences();
    }

    public static GitBibDatabaseDiff compare(BibDatabaseContext base, BibDatabaseContext local, BibDatabaseContext remote) {
        return new GitBibDatabaseDiff(base, local, remote);
    }

    private List<GitBibEntryDiff> findEntryDifferences(BibDatabaseContext base, BibDatabaseContext local, BibDatabaseContext remote) {
        List<GitBibEntryDiff> result = new ArrayList<>();

        List<BibEntryDiff> localDiffs = baseToLocalDiff.getEntryDifferences();
        List<BibEntryDiff> remoteDiffs = baseToRemoteDiff.getEntryDifferences();

        for (BibEntryDiff localDiff : localDiffs) {
            BibEntryDiff matchingRemoteDiff = findMatchingDiff(localDiff, remoteDiffs);
            if (matchingRemoteDiff != null) {
                result.add(new GitBibEntryDiff(
                        localDiff.originalEntry(),
                        localDiff.newEntry(),
                        matchingRemoteDiff.newEntry()

                ));
            } else {
                result.add(new GitBibEntryDiff(
                        localDiff.originalEntry(),
                        localDiff.newEntry(),
                        localDiff.originalEntry()
                ));
            }
        }
        for (BibEntryDiff remoteDiff : remoteDiffs) {
            if (findMatchingDiff(remoteDiff, localDiffs) == null) {
                result.add(new GitBibEntryDiff(
                        remoteDiff.originalEntry(),
                        remoteDiff.originalEntry(),
                        remoteDiff.newEntry()
                ));
            }
        }
        return result;
    }

    private BibEntryDiff findMatchingDiff(BibEntryDiff diff, List<BibEntryDiff> diffs) {
        for (BibEntryDiff otherDiff : diffs) {
            if (entriesMatch(diff.originalEntry(), otherDiff.originalEntry())) {
                return otherDiff;
            }
        }
        return null;
    }

    private boolean entriesMatch(BibEntry entry1, BibEntry entry2) {
        if (entry1 == null || entry2 == null) {
            return false;
        }

        if (entry1.getId().equals(entry2.getId())) {
            return true;
        }

        return entry1.hasCitationKey() && entry2.hasCitationKey() &&
                entry1.getCitationKey().equals(entry2.getCitationKey());
    }

    private Optional<MetaDataDiff> findMetaDataDifferences() {
        return baseToLocalDiff.getMetaDataDifferences();
    }

    public List<GitBibEntryDiff> getEntryDifferences() {
        return this.entryDifferences;
    }

    public Optional<MetaDataDiff> getMetaDataDifferences() {
        return this.metaDataDifferences;
    }
}
