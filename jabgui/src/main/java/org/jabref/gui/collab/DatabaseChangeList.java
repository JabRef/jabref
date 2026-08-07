package org.jabref.gui.collab;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.collab.entryadd.EntryAdd;
import org.jabref.gui.collab.entrychange.EntryChange;
import org.jabref.gui.collab.entrydelete.EntryDelete;
import org.jabref.gui.collab.groupchange.GroupChange;
import org.jabref.gui.collab.metedatachange.MetadataChange;
import org.jabref.gui.collab.preamblechange.PreambleChange;
import org.jabref.gui.collab.stringadd.BibTexStringAdd;
import org.jabref.gui.collab.stringchange.BibTexStringChange;
import org.jabref.gui.collab.stringdelete.BibTexStringDelete;
import org.jabref.gui.collab.stringrename.BibTexStringRename;
import org.jabref.logic.bibtex.comparator.BibDatabaseDiff;
import org.jabref.logic.bibtex.comparator.BibEntryDiff;
import org.jabref.logic.bibtex.comparator.BibStringDiff;
import org.jabref.model.database.BibDatabaseContext;

public final class DatabaseChangeList {
    private DatabaseChangeList() {
        throw new UnsupportedOperationException("cannot instantiate a utility class");
    }

    /// Compares the given two databases, and returns the list of changes required to change the `originalDatabase` into the `otherDatabase`
    ///
    /// @param originalDatabase This is the original database
    /// @param otherDatabase    This is the other database.
    /// @return an unmodifiable list of `DatabaseChange` required to change `originalDatabase` into `otherDatabase`
    public static List<DatabaseChange> compareAndGetChanges(BibDatabaseContext originalDatabase, BibDatabaseContext otherDatabase, DatabaseChangeResolverFactory databaseChangeResolverFactory) {
        List<DatabaseChange> changes = new ArrayList<>();

        BibDatabaseDiff differences = BibDatabaseDiff.compare(originalDatabase, otherDatabase);

        differences.getMetaDataDifferences().ifPresent(diff -> {
            changes.add(new MetadataChange(diff, originalDatabase, databaseChangeResolverFactory));
            diff.getGroupDifferences().ifPresent(groupDiff -> changes.add(new GroupChange(
                    groupDiff, originalDatabase, databaseChangeResolverFactory
            )));
        });

        differences.getPreambleDifferences().ifPresent(diff ->
                changes.add(new PreambleChange(diff, originalDatabase, databaseChangeResolverFactory)));

        differences.getBibStringDifferences().forEach(diff ->
                createBibStringDiff(originalDatabase, databaseChangeResolverFactory, diff)
                        .ifPresent(changes::add));

        differences.getEntryDifferences().forEach(diff ->
                createBibEntryDiff(originalDatabase, databaseChangeResolverFactory, diff)
                        .ifPresent(changes::add));

        return Collections.unmodifiableList(changes);
    }

    private static Optional<DatabaseChange> createBibStringDiff(BibDatabaseContext originalDatabase, DatabaseChangeResolverFactory databaseChangeResolverFactory, BibStringDiff diff) {
        if (diff.getNewString().isEmpty() && diff.getOriginalString().isEmpty()) {
            return Optional.empty();
        }

        if (diff.getOriginalString().isEmpty()) {
            return Optional.of(new BibTexStringAdd(diff.getNewString().get(), originalDatabase, databaseChangeResolverFactory));
        }

        if (diff.getNewString().isEmpty()) {
            return Optional.of(new BibTexStringDelete(diff.getOriginalString().get(), originalDatabase, databaseChangeResolverFactory));
        }

        if (diff.getOriginalString().get().getName().equals(diff.getNewString().get().getName())) {
            return Optional.of(new BibTexStringChange(diff.getOriginalString().get(), diff.getNewString().get(), originalDatabase, databaseChangeResolverFactory));
        }

        return Optional.of(new BibTexStringRename(diff.getOriginalString().get(), diff.getNewString().get(), originalDatabase, databaseChangeResolverFactory));
    }

    private static Optional<DatabaseChange> createBibEntryDiff(BibDatabaseContext originalDatabase, DatabaseChangeResolverFactory databaseChangeResolverFactory, BibEntryDiff diff) {
        if (diff.originalEntry() == null && diff.newEntry() == null) {
            return Optional.empty();
        }

        if (diff.originalEntry() == null) {
            return Optional.of(new EntryAdd(diff.newEntry(), originalDatabase, databaseChangeResolverFactory));
        }

        if (diff.newEntry() == null) {
            return Optional.of(new EntryDelete(diff.originalEntry(), originalDatabase, databaseChangeResolverFactory));
        }

        return Optional.of(new EntryChange(diff.originalEntry(), diff.newEntry(), originalDatabase, databaseChangeResolverFactory));
    }
}
