package org.jabref.gui.collab;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.jabref.Globals;
import org.jabref.logic.bibtex.DuplicateCheck;
import org.jabref.logic.bibtex.comparator.BibDatabaseDiff;
import org.jabref.logic.bibtex.comparator.BibEntryDiff;
import org.jabref.logic.bibtex.comparator.BibStringDiff;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexString;

public class ChangeScanner {

    private final Path referenceFile;
    private final BibDatabaseContext database;
    private final List<DatabaseChangeViewModel> changes = new ArrayList<>();
    private BibDatabaseContext referenceDatabase;

    public ChangeScanner(BibDatabaseContext database, Path referenceFile) {
        this.database = database;
        this.referenceFile = referenceFile;
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

    public List<DatabaseChangeViewModel> scanForChanges() {
        database.getDatabasePath().ifPresent(diskdb -> {
            // Parse the temporary file.
            ImportFormatPreferences importFormatPreferences = Globals.prefs.getImportFormatPreferences();
            ParserResult result = OpenDatabase.loadDatabase(referenceFile.toAbsolutePath().toString(), importFormatPreferences, Globals.getFileUpdateMonitor());
            referenceDatabase = result.getDatabaseContext();

            // Parse the modified file.
            result = OpenDatabase.loadDatabase(diskdb.toAbsolutePath().toString(), importFormatPreferences, Globals.getFileUpdateMonitor());
            BibDatabaseContext databaseOnDisk = result.getDatabaseContext();

            // Start looking at changes.
            BibDatabaseDiff differences = BibDatabaseDiff.compare(referenceDatabase, databaseOnDisk);
            differences.getMetaDataDifferences().ifPresent(diff -> {
                changes.add(new MetaDataChangeViewModel(diff));
                diff.getGroupDifferences().ifPresent(groupDiff -> changes.add(new GroupChangeViewModel(groupDiff)));
            });
            differences.getPreambleDifferences().ifPresent(diff -> changes.add(new PreambleChangeViewModel(diff)));
            differences.getBibStringDifferences().forEach(diff -> changes.add(createBibStringDiff(diff)));
            differences.getEntryDifferences().forEach(diff -> changes.add(createBibEntryDiff(diff)));
        });
        return changes;
    }

    private DatabaseChangeViewModel createBibStringDiff(BibStringDiff diff) {
        if (diff.getOriginalString() == null) {
            return new StringAddChangeViewModel(diff.getNewString());
        }

        if (diff.getNewString() == null) {
            Optional<BibtexString> current = database.getDatabase().getStringByName(diff.getOriginalString().getName());
            return new StringRemoveChangeViewModel(diff.getOriginalString(), current.orElse(null));
        }

        if (diff.getOriginalString().getName().equals(diff.getNewString().getName())) {
            Optional<BibtexString> current = database.getDatabase().getStringByName(diff.getOriginalString().getName());
            return new StringChangeViewModel(current.orElse(null), diff.getOriginalString(), diff.getNewString().getContent());
        }

        Optional<BibtexString> current = database.getDatabase().getStringByName(diff.getOriginalString().getName());
        return new StringNameChangeViewModel(current.orElse(null), diff.getOriginalString(), current.map(BibtexString::getName).orElse(""), diff.getNewString().getName());
    }

    private DatabaseChangeViewModel createBibEntryDiff(BibEntryDiff diff) {
        if (diff.getOriginalEntry() == null) {
            return new EntryAddChangeViewModel(diff.getNewEntry());
        }

        if (diff.getNewEntry() == null) {
            return new EntryDeleteChangeViewModel(bestFit(diff.getOriginalEntry(), database.getEntries()), diff.getOriginalEntry());
        }

        return new EntryChangeViewModel(bestFit(diff.getOriginalEntry(), database.getEntries()), diff.getOriginalEntry(), diff.getNewEntry());
    }
}
