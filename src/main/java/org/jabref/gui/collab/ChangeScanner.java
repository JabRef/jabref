package org.jabref.gui.collab;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.bibtex.DuplicateCheck;
import org.jabref.logic.bibtex.comparator.BibDatabaseDiff;
import org.jabref.logic.bibtex.comparator.BibEntryDiff;
import org.jabref.logic.bibtex.comparator.BibStringDiff;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangeScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeScanner.class);

    private final BibDatabaseContext oldDatabase;
    private final BibDatabaseContext newDatabase;

    public ChangeScanner(BibDatabaseContext oldDatabase, BibDatabaseContext newDatabase) {
        this.oldDatabase = oldDatabase;
        this.newDatabase = newDatabase;
    }

    public List<DatabaseChangeViewModel> scanForChanges() {
        List changes = new ArrayList();
        BibDatabaseDiff differences = BibDatabaseDiff.compare(oldDatabase, newDatabase);
        differences.getMetaDataDifferences().ifPresent(diff -> {
            changes.add(new MetaDataChangeViewModel(diff));
            diff.getGroupDifferences().ifPresent(groupDiff -> changes.add(new GroupChangeViewModel(groupDiff)));
        });
        differences.getPreambleDifferences().ifPresent(diff -> changes.add(new PreambleChangeViewModel(diff)));
        differences.getBibStringDifferences().forEach(diff -> changes.add(createBibStringDiff(diff)));
        differences.getEntryDifferences().forEach(diff -> changes.add(createBibEntryDiff(diff)));
        return changes;
    }

    /**
     * Finds the entry in the list best fitting the specified entry. Even if no entries get a score above zero, an entry
     * is still returned.
     */
    private static BibEntry bestFit(BibEntry targetEntry, List<BibEntry> entries) {
        return entries.stream()
                      .max(Comparator.comparingDouble(candidate -> DuplicateCheck.compareEntriesStrictly(targetEntry, candidate)))
                      .orElse(null);
    }

    private DatabaseChangeViewModel createBibStringDiff(BibStringDiff diff) {
        if (diff.getOriginalString() == null) {
            return new StringAddChangeViewModel(diff.getNewString());
        }

        if (diff.getNewString() == null) {
            Optional<BibtexString> current = oldDatabase.getDatabase().getStringByName(diff.getOriginalString().getName());
            return new StringRemoveChangeViewModel(diff.getOriginalString(), current.orElse(null));
        }

        if (diff.getOriginalString().getName().equals(diff.getNewString().getName())) {
            Optional<BibtexString> current = oldDatabase.getDatabase().getStringByName(diff.getOriginalString().getName());
            return new StringChangeViewModel(current.orElse(null), diff.getOriginalString(), diff.getNewString().getContent());
        }

        Optional<BibtexString> current = oldDatabase.getDatabase().getStringByName(diff.getOriginalString().getName());
        return new StringNameChangeViewModel(current.orElse(null), diff.getOriginalString(), current.map(BibtexString::getName).orElse(""), diff.getNewString().getName());
    }

    private DatabaseChangeViewModel createBibEntryDiff(BibEntryDiff diff) {
        if (diff.getOriginalEntry() == null) {
            return new EntryAddChangeViewModel(diff.getNewEntry());
        }

        if (diff.getNewEntry() == null) {
            return new EntryDeleteChangeViewModel(bestFit(diff.getOriginalEntry(), oldDatabase.getEntries()), diff.getOriginalEntry());
        }

        return new EntryChangeViewModel(bestFit(diff.getOriginalEntry(), oldDatabase.getEntries()), diff.getOriginalEntry(), diff.getNewEntry());
    }
}
