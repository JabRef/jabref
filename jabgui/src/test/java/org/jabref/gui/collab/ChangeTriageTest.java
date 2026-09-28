package org.jabref.gui.collab;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.collab.entryadd.EntryAdd;
import org.jabref.gui.collab.entrychange.EntryChange;
import org.jabref.gui.collab.entrydelete.EntryDelete;
import org.jabref.gui.collab.groupchange.GroupChange;
import org.jabref.gui.collab.metedatachange.MetadataChange;
import org.jabref.gui.collab.stringadd.BibTexStringAdd;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.groups.GroupsFactory;
import org.jabref.logic.sync.LibraryBaseline;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.undo.CompoundEdit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChangeTriageTest {

    private static final GlobalCitationKeyPatterns PATTERNS = GlobalCitationKeyPatterns.fromPattern("[auth][year]");

    private BibEntry local;
    private BibEntry other;
    private BibDatabaseContext localContext;
    private BibEntry disk;
    private BibEntry otherOnDisk;
    private BibDatabaseContext diskContext;
    private LibraryBaseline baseline;

    @BeforeEach
    void setUp() {
        local = new BibEntry(StandardEntryType.Article).withCitationKey("Key")
                                                       .withField(StandardField.TITLE, "Title")
                                                       .withField(StandardField.YEAR, "2020");
        // A second, mostly untouched entry keeps the file from becoming empty, which the two-way diff cannot handle
        other = new BibEntry(StandardEntryType.Book).withCitationKey("Other").withField(StandardField.TITLE, "Other");
        localContext = new BibDatabaseContext(new BibDatabase(List.of(local, other)));
        baseline = LibraryBaseline.of(localContext, PATTERNS);
        // the file as it would be parsed: same content, different entry objects
        disk = new BibEntry(local);
        otherOnDisk = new BibEntry(other);
        diskContext = new BibDatabaseContext(new BibDatabase(List.of(disk, otherOnDisk)));
    }

    private ChangeTriage.Triage triage() {
        return ChangeTriage.triage(baseline, DatabaseChangeList.compareAndGetChanges(localContext, diskContext, null), localContext, null);
    }

    @Test
    void changeOnDiskOnlyIsAccepted() {
        disk.setField(StandardField.TITLE, "Disk title");

        ChangeTriage.Triage triage = triage();

        assertEquals(1, triage.diskOnly().size());
        assertTrue(triage.diskOnly().getFirst().isAccepted());
        assertEquals(List.of(), triage.bothSides());
        assertEquals(List.of(), triage.memoryOnly());
    }

    @Test
    void unsavedChangeInMemoryIsNotAnExternalChange() {
        local.setField(StandardField.TITLE, "Memory title");

        ChangeTriage.Triage triage = triage();

        assertEquals(1, triage.memoryOnly().size());
        assertEquals(List.of(), triage.diskOnly());
        assertEquals(List.of(), triage.bothSides());
    }

    @Test
    void differentFieldsChangedOnBothSidesAreMerged() {
        local.setField(StandardField.TITLE, "Memory title");
        disk.setField(StandardField.YEAR, "2021");
        disk.setField(StandardField.AUTHOR, "Disk author");

        ChangeTriage.Triage triage = triage();

        assertEquals(List.of(), triage.bothSides());
        EntryChange merged = assertInstanceOf(EntryChange.class, triage.diskOnly().getFirst());
        assertTrue(merged.isAccepted());
        assertEquals(new BibEntry(StandardEntryType.Article).withCitationKey("Key")
                                                            .withField(StandardField.TITLE, "Memory title")
                                                            .withField(StandardField.YEAR, "2021")
                                                            .withField(StandardField.AUTHOR, "Disk author"),
                merged.getNewEntry());
    }

    @Test
    void sameFieldChangedDifferentlyNeedsReview() {
        local.setField(StandardField.TITLE, "Memory title");
        disk.setField(StandardField.TITLE, "Disk title");

        ChangeTriage.Triage triage = triage();

        assertEquals(1, triage.bothSides().size());
        assertEquals(List.of(), triage.diskOnly());
    }

    @Test
    void citationKeyChangedInMemoryStillMergesDiskChange() {
        local.setCitationKey("NewKey");
        disk.setField(StandardField.TITLE, "Disk title");

        ChangeTriage.Triage triage = triage();

        EntryChange merged = assertInstanceOf(EntryChange.class, triage.diskOnly().getFirst());
        assertEquals("NewKey", merged.getNewEntry().getCitationKey().orElseThrow());
        assertEquals("Disk title", merged.getNewEntry().getField(StandardField.TITLE).orElseThrow());
    }

    @Test
    void citationKeyChangedOnDiskIsMergedIntoTheSameEntry() {
        disk.setCitationKey("Renamed");
        disk.setField(StandardField.YEAR, "2021");

        ChangeTriage.Triage triage = triage();

        EntryChange merged = assertInstanceOf(EntryChange.class, triage.diskOnly().getFirst());
        assertEquals(local, merged.getOldEntry());
        assertEquals("Renamed", merged.getNewEntry().getCitationKey().orElseThrow());
        assertEquals(1, triage.diskOnly().size());
    }

    @Test
    void encodingChangedOnDiskIsAccepted() {
        localContext.getMetaData().setEncoding(StandardCharsets.UTF_8);
        baseline = LibraryBaseline.of(localContext, PATTERNS);
        diskContext.getMetaData().setEncoding(StandardCharsets.ISO_8859_1);

        ChangeTriage.Triage triage = triage();

        assertInstanceOf(MetadataChange.class, triage.diskOnly().getFirst());
        assertEquals(List.of(), triage.memoryOnly());
    }

    @Test
    void duplicateCitationKeysAreMatchedByContent() {
        BibEntry duplicate = new BibEntry(StandardEntryType.Article).withCitationKey("Key").withField(StandardField.TITLE, "Duplicate");
        localContext.getDatabase().insertEntry(duplicate);
        diskContext.getDatabase().insertEntry(new BibEntry(duplicate));
        baseline = LibraryBaseline.of(localContext, PATTERNS);
        localContext.getDatabase().removeEntry(local);

        ChangeTriage.Triage triage = triage();

        // The entry deleted in memory is still unchanged on disk, so nothing is reported; the duplicate must not be mistaken for it
        assertInstanceOf(EntryAdd.class, triage.memoryOnly().getFirst());
        assertEquals(List.of(), triage.bothSides());
    }

    @Test
    void commentChangedOnDiskIsAcceptedAndApplied() {
        disk.setCommentsBeforeEntry("% from disk");

        ChangeTriage.Triage triage = triage();

        EntryChange change = assertInstanceOf(EntryChange.class, triage.diskOnly().getFirst());
        local.setChanged(false);
        change.applyChange(new CompoundEdit("test"));
        assertEquals("% from disk", local.getUserComments());
        // otherwise a save would write the parsed serialization with the old comment
        assertTrue(local.hasChanged());
    }

    @Test
    void undoOfAppliedChangeRestoresTheParsedState() {
        disk.setField(StandardField.TITLE, "Disk title");
        ChangeTriage.Triage triage = triage();
        EntryChange change = assertInstanceOf(EntryChange.class, triage.diskOnly().getFirst());
        local.setChanged(false);
        CompoundEdit edit = new CompoundEdit("test");

        change.applyChange(edit);
        assertTrue(local.hasChanged());
        edit.toChangeSet().inverted().apply();

        assertEquals(Optional.of("Title"), local.getField(StandardField.TITLE));
        assertFalse(local.hasChanged());
    }

    @Test
    void pairEstablishedBySimilarityOnlyGoesToReview() {
        // memory edits the entry, disk replaces it by a similar one under another key
        local.setField(StandardField.YEAR, "2021");
        diskContext.getDatabase().removeEntry(disk);
        diskContext.getDatabase().insertEntry(new BibEntry(StandardEntryType.Article).withCitationKey("Renamed").withField(StandardField.TITLE, "Alpha title").withField(StandardField.YEAR, "2020"));

        ChangeTriage.Triage triage = triage();

        assertInstanceOf(EntryChange.class, triage.bothSides().getFirst());
        assertEquals(List.of(), triage.diskOnly());
    }

    @Test
    void metadataChangeKeepsTheLocalSynchronizationSetting() {
        localContext.getMetaData().setSynchronizeWithFile(true);
        baseline = LibraryBaseline.of(localContext, PATTERNS);
        diskContext.getMetaData().setSynchronizeWithFile(false);
        diskContext.getMetaData().setEncoding(StandardCharsets.ISO_8859_1);

        ChangeTriage.Triage triage = triage();
        MetadataChange change = assertInstanceOf(MetadataChange.class, triage.diskOnly().getFirst());
        change.applyChange(new CompoundEdit("test"));

        assertEquals(Optional.of(true), localContext.getMetaData().getSynchronizeWithFile());
        assertEquals(Optional.of(StandardCharsets.ISO_8859_1), localContext.getMetaData().getEncoding());
    }

    @Test
    void entryAddedOnDiskIsAccepted() {
        diskContext.getDatabase().insertEntry(new BibEntry().withCitationKey("New").withField(StandardField.TITLE, "New on disk"));

        ChangeTriage.Triage triage = triage();

        assertInstanceOf(EntryAdd.class, triage.diskOnly().getFirst());
        assertEquals(List.of(), triage.bothSides());
    }

    @Test
    void entryDeletedInMemoryStaysDeletedWhenUnchangedOnDisk() {
        localContext.getDatabase().removeEntry(local);

        ChangeTriage.Triage triage = triage();

        assertInstanceOf(EntryAdd.class, triage.memoryOnly().getFirst());
        assertEquals(List.of(), triage.diskOnly());
    }

    @Test
    void entryDeletedInMemoryButChangedOnDiskNeedsReview() {
        localContext.getDatabase().removeEntry(local);
        disk.setField(StandardField.TITLE, "Disk title");

        ChangeTriage.Triage triage = triage();

        assertInstanceOf(EntryAdd.class, triage.bothSides().getFirst());
    }

    @Test
    void entryDeletedOnDiskIsAcceptedWhenUnchangedInMemory() {
        diskContext.getDatabase().removeEntry(disk);

        ChangeTriage.Triage triage = triage();

        assertInstanceOf(EntryDelete.class, triage.diskOnly().getFirst());
    }

    @Test
    void entryDeletedOnDiskButChangedInMemoryNeedsReview() {
        diskContext.getDatabase().removeEntry(disk);
        local.setField(StandardField.TITLE, "Memory title");

        ChangeTriage.Triage triage = triage();

        assertInstanceOf(EntryDelete.class, triage.bothSides().getFirst());
    }

    @Test
    void entryAddedInMemoryIsNotAnExternalChange() {
        localContext.getDatabase().insertEntry(new BibEntry().withCitationKey("New").withField(StandardField.TITLE, "New in memory"));

        ChangeTriage.Triage triage = triage();

        assertInstanceOf(EntryDelete.class, triage.memoryOnly().getFirst());
        assertEquals(List.of(), triage.diskOnly());
    }

    @Test
    void stringAddedOnDiskIsAccepted() {
        diskContext.getDatabase().addString(new BibtexString("jan", "January"));

        ChangeTriage.Triage triage = triage();

        assertInstanceOf(BibTexStringAdd.class, triage.diskOnly().getFirst());
    }

    @Test
    void keepUnresolvedPreservesBaselineOfUnsavedMemoryChange() {
        local.setField(StandardField.TITLE, "Memory title");
        ChangeTriage.Triage first = triage();
        LibraryBaseline updated = LibraryBaseline.of(localContext, PATTERNS);
        ChangeTriage.keepUnresolved(updated, baseline, first.memoryOnly());
        baseline = updated;

        // The file now changes the same field: this must be reported as a conflict, not taken over silently
        disk.setField(StandardField.TITLE, "Disk title");
        ChangeTriage.Triage second = triage();

        assertEquals(1, second.bothSides().size());
        assertEquals(List.of(), second.diskOnly());
    }

    @Test
    void mergedEntryHasTheDiskVersionAsAncestor() {
        local.setField(StandardField.TITLE, "Memory title");
        disk.setField(StandardField.YEAR, "2021");
        ChangeTriage.Triage first = triage();
        assertInstanceOf(EntryChange.class, first.diskOnly().getFirst()).applyChange(new CompoundEdit("test"));
        LibraryBaseline updated = LibraryBaseline.of(localContext, PATTERNS);
        first.diskEntries().forEach(updated::recordEntry);
        baseline = updated;

        // The file changes elsewhere while the merged entry stays as it is on disk: the title kept from memory is
        // still an unsaved edit, not a disk change to take over
        otherOnDisk.setField(StandardField.TITLE, "Other on disk");
        ChangeTriage.Triage second = triage();

        assertEquals(1, second.diskOnly().size());
        assertEquals(1, second.memoryOnly().size());
        assertEquals(List.of(), second.bothSides());
    }

    @Test
    void reviewKeepsTheAncestorOfAnUnsavedMemoryChange() {
        // "Other" is changed differently on both sides and reviewed; the unsaved edit of "Key" is not part of the review
        local.setField(StandardField.TITLE, "Memory title");
        other.setField(StandardField.TITLE, "Other in memory");
        otherOnDisk.setField(StandardField.TITLE, "Other on disk");
        ChangeTriage.Triage first = triage();
        assertEquals(1, first.bothSides().size());
        first.bothSides().getFirst().accept();
        first.bothSides().getFirst().applyChange(new CompoundEdit("test"));
        LibraryBaseline updated = baseline.copy();
        ChangeTriage.advance(updated, first.bothSides(), first.bothSides());
        baseline = updated;

        // The file now changes the field edited in memory: a conflict, not a change to take over silently
        disk.setField(StandardField.TITLE, "Disk title");
        ChangeTriage.Triage second = triage();

        assertEquals(1, second.bothSides().size());
        assertEquals(List.of(), second.diskOnly());
    }

    @Test
    void metadataAcceptedAndGroupsDeclinedAreNotReportedAgain() {
        localContext.getMetaData().setEncoding(StandardCharsets.ISO_8859_1);
        GroupTreeNode root = new GroupTreeNode(GroupsFactory.createAllEntriesGroup());
        root.addSubgroup(new ExplicitGroup("Group", GroupHierarchyType.INDEPENDENT, ','));
        diskContext.getMetaData().setGroups(root);
        ChangeTriage.Triage first = triage();
        MetadataChange metadataChange = assertInstanceOf(MetadataChange.class, first.bothSides().get(0));
        assertInstanceOf(GroupChange.class, first.bothSides().get(1));
        metadataChange.accept();
        metadataChange.applyChange(new CompoundEdit("test"));
        // Applying installed the disk metadata object without its groups; the file itself still has them
        diskContext.getMetaData().setGroups(root);
        LibraryBaseline updated = baseline.copy();
        ChangeTriage.advance(updated, first.bothSides(), first.bothSides());
        baseline = updated;

        ChangeTriage.Triage second = triage();

        assertEquals(List.of(), second.bothSides());
        assertEquals(List.of(), second.diskOnly());
        assertEquals(2, second.memoryOnly().size());
    }

    @Test
    void onlyTheSynchronizationSettingDifferingCountsAsMatchingTheFile() {
        localContext.getMetaData().setSynchronizeWithFile(true);
        assertTrue(ChangeTriage.matchesFile(DatabaseChangeList.compareAndGetChanges(localContext, diskContext, null), PATTERNS));

        disk.setField(StandardField.TITLE, "Disk title");
        assertFalse(ChangeTriage.matchesFile(DatabaseChangeList.compareAndGetChanges(localContext, diskContext, null), PATTERNS));
    }
}
