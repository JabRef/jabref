package org.jabref.gui.collab;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.jabref.logic.sync.LibraryBaseline;
import org.jabref.logic.sync.LibraryBaseline.Side;
import org.jabref.model.database.BibDatabaseContext;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Sorts the external changes of a library, as computed by [DatabaseChangeList#compareAndGetChanges], by the side
/// that changed according to a [LibraryBaseline]. The policy lives in the baseline; this class only maps the GUI's
/// [DatabaseChange] objects onto it.
@NullMarked
public final class ChangeTriage {

    /// Outcome of comparing external changes against the baseline.
    ///
    /// @param diskOnly   changes to items untouched in memory; already accepted, to be applied without asking
    /// @param bothSides  changes to items that were modified in memory as well, in a way that cannot be merged automatically; need a review
    /// @param memoryOnly not external changes at all: differences caused by unsaved in-memory edits; to be dropped
    public record Triage(List<DatabaseChange> diskOnly, List<DatabaseChange> bothSides, List<DatabaseChange> memoryOnly) {
    }

    private ChangeTriage() {
    }

    /// Entries modified on both sides in different fields are merged field by field into a new, accepted [EntryChange].
    public static Triage triage(LibraryBaseline baseline, List<DatabaseChange> changes, BibDatabaseContext local, @Nullable DatabaseChangeResolverFactory resolverFactory) {
        Triage triage = new Triage(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        // A group change is always accompanied by the metadata change it is part of, which precedes it in the list
        Side metaDataSide = Side.BOTH;
        LibraryBaseline.Lookup lookup = baseline.lookup();
        for (DatabaseChange change : pairSplitEntries(baseline, lookup, changes, local, resolverFactory)) {
            Side side = switch (change) {
                case EntryChange entryChange -> {
                    Side entrySide = baseline.sideOfEntry(entryChange.getOldEntry(), entryChange.getNewEntry());
                    if (entrySide == Side.BOTH) {
                        BibEntryMerge merge = mergeEntry(baseline, entryChange, local, resolverFactory);
                        change = merge.change();
                        entrySide = merge.side();
                    }
                    yield entrySide;
                }
                case EntryAdd entryAdd ->
                        lookup.sideOfAddedEntry(entryAdd.getAddedEntry());
                case EntryDelete entryDelete ->
                        baseline.sideOfDeletedEntry(entryDelete.getDeletedEntry());
                case MetadataChange metadataChange -> {
                    metaDataSide = baseline.sideOfMetaData(local.getMetaData(), metadataChange.getMetaDataDiff().getNewMetaData());
                    yield metaDataSide;
                }
                case GroupChange _ ->
                        metaDataSide;
                case PreambleChange preambleChange ->
                        baseline.sideOfPreamble(local.getDatabase().getPreamble().orElse(null), preambleChange.getPreambleDiff().getNewPreamble());
                case BibTexStringAdd stringAdd ->
                        baseline.sideOfAddedString(stringAdd.getAddedString().getName(), stringAdd.getAddedString().getContent(),
                                name -> local.getDatabase().getStringByName(name).isPresent());
                case BibTexStringDelete stringDelete ->
                        baseline.sideOfString(stringDelete.getDeletedString().getName(), stringDelete.getDeletedString().getContent(), null);
                case BibTexStringChange stringChange ->
                        baseline.sideOfString(stringChange.getOldString().getName(), stringChange.getOldString().getContent(), stringChange.getNewString().getContent());
                case BibTexStringRename stringRename ->
                        baseline.sideOfStringRename(stringRename.getOldString().getName(), stringRename.getOldString().getContent(), stringRename.getNewString().getName());
            };
            switch (side) {
                case DISK -> {
                    change.accept();
                    triage.diskOnly().add(change);
                }
                case BOTH ->
                        triage.bothSides().add(change);
                case MEMORY ->
                        triage.memoryOnly().add(change);
            }
        }
        return triage;
    }

    /// Carries over the baseline of every item whose external change was not applied, so that the next scan sees
    /// the same divergence again instead of mistaking the current in-memory version for the common ancestor.
    public static void keepUnresolved(LibraryBaseline updated, LibraryBaseline previous, List<DatabaseChange> unresolved) {
        LibraryBaseline.Lookup previousLookup = previous.lookup();
        for (DatabaseChange change : unresolved) {
            switch (change) {
                case EntryChange entryChange ->
                        updated.keepEntry(previous, entryChange.getOldEntry().getId());
                case EntryDelete entryDelete ->
                        updated.keepEntry(previous, entryDelete.getDeletedEntry().getId());
                case EntryAdd entryAdd ->
                        updated.keepEntryFor(previous, previousLookup, entryAdd.getAddedEntry());
                case MetadataChange _,
                     GroupChange _ ->
                        updated.keepMetaData(previous);
                case PreambleChange _ ->
                        updated.keepPreamble(previous);
                case BibTexStringAdd stringAdd ->
                        updated.keepString(previous, stringAdd.getAddedString().getName());
                case BibTexStringDelete stringDelete ->
                        updated.keepString(previous, stringDelete.getDeletedString().getName());
                case BibTexStringChange stringChange ->
                        updated.keepString(previous, stringChange.getOldString().getName());
                case BibTexStringRename stringRename -> {
                    updated.keepString(previous, stringRename.getOldString().getName());
                    updated.keepString(previous, stringRename.getNewString().getName());
                }
            }
        }
    }

    private record BibEntryMerge(Side side, DatabaseChange change) {
    }

    private static BibEntryMerge mergeEntry(LibraryBaseline baseline, EntryChange entryChange, BibDatabaseContext local, @Nullable DatabaseChangeResolverFactory resolverFactory) {
        return baseline.mergeEntry(entryChange.getOldEntry(), entryChange.getNewEntry())
                       .map(merged -> new BibEntryMerge(Side.DISK, new EntryChange(entryChange.getOldEntry(), merged, local, resolverFactory)))
                       .orElse(new BibEntryMerge(Side.BOTH, entryChange));
    }

    /// The two-way diff pairs entries by similarity, so an entry whose citation key changed on one side while fields
    /// changed on the other can fall below the similarity threshold and show up as a deletion plus an addition. The
    /// baseline knows both belong to the same entry. One pass over the changes: a paired addition becomes the change,
    /// its deletion is dropped.
    private static List<DatabaseChange> pairSplitEntries(LibraryBaseline baseline, LibraryBaseline.Lookup lookup, List<DatabaseChange> changes, BibDatabaseContext local, @Nullable DatabaseChangeResolverFactory resolverFactory) {
        Map<String, EntryDelete> deletesByBaseId = new HashMap<>();
        for (DatabaseChange change : changes) {
            if (change instanceof EntryDelete entryDelete && baseline.hasEntry(entryDelete.getDeletedEntry().getId())) {
                deletesByBaseId.put(entryDelete.getDeletedEntry().getId(), entryDelete);
            }
        }
        if (deletesByBaseId.isEmpty()) {
            return changes;
        }
        Map<EntryAdd, DatabaseChange> replacements = new HashMap<>();
        Set<EntryDelete> pairedDeletes = new HashSet<>();
        for (DatabaseChange change : changes) {
            if (change instanceof EntryAdd entryAdd) {
                lookup.baseIdOf(entryAdd.getAddedEntry()).map(deletesByBaseId::remove).ifPresent(entryDelete -> {
                    pairedDeletes.add(entryDelete);
                    replacements.put(entryAdd, new EntryChange(entryDelete.getDeletedEntry(), entryAdd.getAddedEntry(), local, resolverFactory));
                });
            }
        }
        List<DatabaseChange> paired = new ArrayList<>(changes.size());
        for (DatabaseChange change : changes) {
            if (change instanceof EntryDelete entryDelete && pairedDeletes.contains(entryDelete)) {
                continue;
            }
            paired.add(change instanceof EntryAdd entryAdd ? replacements.getOrDefault(entryAdd, entryAdd) : change);
        }
        return paired;
    }
}
