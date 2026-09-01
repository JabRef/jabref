package org.jabref.gui.collab;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.jabref.gui.collab.entryadd.EntryAdd;
import org.jabref.gui.collab.entrychange.EntryChange;
import org.jabref.gui.collab.entrydelete.EntryDelete;
import org.jabref.gui.collab.metedatachange.MetadataChange;
import org.jabref.gui.collab.stringadd.BibTexStringAdd;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LibraryBaselineTest {

    private static final GlobalCitationKeyPatterns PATTERNS = GlobalCitationKeyPatterns.fromPattern("[auth][year]");

    private BibEntry local;
    private BibDatabaseContext localContext;
    private BibEntry disk;
    private BibDatabaseContext diskContext;
    private LibraryBaseline baseline;

    @BeforeEach
    void setUp() {
        local = new BibEntry(StandardEntryType.Article).withCitationKey("Key")
                                                       .withField(StandardField.TITLE, "Title")
                                                       .withField(StandardField.YEAR, "2020");
        // A second, never touched entry keeps the file from becoming empty, which the two-way diff cannot handle
        BibEntry other = new BibEntry(StandardEntryType.Book).withCitationKey("Other").withField(StandardField.TITLE, "Other");
        localContext = new BibDatabaseContext(new BibDatabase(List.of(local, other)));
        baseline = LibraryBaseline.of(localContext, PATTERNS);
        // the file as it would be parsed: same content, different entry objects
        disk = new BibEntry(local);
        diskContext = new BibDatabaseContext(new BibDatabase(List.of(disk, new BibEntry(other))));
    }

    private LibraryBaseline.Triage triage() {
        return baseline.triage(DatabaseChangeList.compareAndGetChanges(localContext, diskContext, null), localContext, null);
    }

    @Test
    void changeOnDiskOnlyIsAccepted() {
        disk.setField(StandardField.TITLE, "Disk title");

        LibraryBaseline.Triage triage = triage();

        assertEquals(1, triage.diskOnly().size());
        assertTrue(triage.diskOnly().getFirst().isAccepted());
        assertTrue(triage.bothSides().isEmpty());
        assertTrue(triage.memoryOnly().isEmpty());
    }

    @Test
    void unsavedChangeInMemoryIsNotAnExternalChange() {
        local.setField(StandardField.TITLE, "Memory title");

        LibraryBaseline.Triage triage = triage();

        assertEquals(1, triage.memoryOnly().size());
        assertTrue(triage.diskOnly().isEmpty());
        assertTrue(triage.bothSides().isEmpty());
    }

    @Test
    void differentFieldsChangedOnBothSidesAreMerged() {
        local.setField(StandardField.TITLE, "Memory title");
        disk.setField(StandardField.YEAR, "2021");
        disk.setField(StandardField.AUTHOR, "Disk author");

        LibraryBaseline.Triage triage = triage();

        assertTrue(triage.bothSides().isEmpty());
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

        LibraryBaseline.Triage triage = triage();

        assertEquals(1, triage.bothSides().size());
        assertTrue(triage.diskOnly().isEmpty());
    }

    @Test
    void citationKeyChangedInMemoryStillMergesDiskChange() {
        local.setCitationKey("NewKey");
        disk.setField(StandardField.TITLE, "Disk title");

        LibraryBaseline.Triage triage = triage();

        EntryChange merged = assertInstanceOf(EntryChange.class, triage.diskOnly().getFirst());
        assertEquals("NewKey", merged.getNewEntry().getCitationKey().orElseThrow());
        assertEquals("Disk title", merged.getNewEntry().getField(StandardField.TITLE).orElseThrow());
    }

    @Test
    void citationKeyChangedOnDiskIsMergedIntoTheSameEntry() {
        disk.setCitationKey("Renamed");
        disk.setField(StandardField.YEAR, "2021");

        LibraryBaseline.Triage triage = triage();

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

        LibraryBaseline.Triage triage = triage();

        assertInstanceOf(MetadataChange.class, triage.diskOnly().getFirst());
        assertTrue(triage.memoryOnly().isEmpty());
    }

    @Test
    void entryAddedOnDiskIsAccepted() {
        diskContext.getDatabase().insertEntry(new BibEntry().withCitationKey("New").withField(StandardField.TITLE, "New on disk"));

        LibraryBaseline.Triage triage = triage();

        assertInstanceOf(EntryAdd.class, triage.diskOnly().getFirst());
        assertTrue(triage.bothSides().isEmpty());
    }

    @Test
    void entryDeletedInMemoryStaysDeletedWhenUnchangedOnDisk() {
        localContext.getDatabase().removeEntry(local);

        LibraryBaseline.Triage triage = triage();

        assertInstanceOf(EntryAdd.class, triage.memoryOnly().getFirst());
        assertTrue(triage.diskOnly().isEmpty());
    }

    @Test
    void entryDeletedInMemoryButChangedOnDiskNeedsReview() {
        localContext.getDatabase().removeEntry(local);
        disk.setField(StandardField.TITLE, "Disk title");

        LibraryBaseline.Triage triage = triage();

        assertInstanceOf(EntryAdd.class, triage.bothSides().getFirst());
    }

    @Test
    void entryDeletedOnDiskIsAcceptedWhenUnchangedInMemory() {
        diskContext.getDatabase().removeEntry(disk);

        LibraryBaseline.Triage triage = triage();

        assertInstanceOf(EntryDelete.class, triage.diskOnly().getFirst());
    }

    @Test
    void entryDeletedOnDiskButChangedInMemoryNeedsReview() {
        diskContext.getDatabase().removeEntry(disk);
        local.setField(StandardField.TITLE, "Memory title");

        LibraryBaseline.Triage triage = triage();

        assertInstanceOf(EntryDelete.class, triage.bothSides().getFirst());
    }

    @Test
    void entryAddedInMemoryIsNotAnExternalChange() {
        localContext.getDatabase().insertEntry(new BibEntry().withCitationKey("New").withField(StandardField.TITLE, "New in memory"));

        LibraryBaseline.Triage triage = triage();

        assertInstanceOf(EntryDelete.class, triage.memoryOnly().getFirst());
        assertTrue(triage.diskOnly().isEmpty());
    }

    @Test
    void stringAddedOnDiskIsAccepted() {
        diskContext.getDatabase().addString(new BibtexString("jan", "January"));

        LibraryBaseline.Triage triage = triage();

        assertInstanceOf(BibTexStringAdd.class, triage.diskOnly().getFirst());
    }

    @Test
    void keepUnresolvedPreservesBaselineOfUnsavedMemoryChange() {
        local.setField(StandardField.TITLE, "Memory title");
        LibraryBaseline.Triage first = triage();
        LibraryBaseline updated = LibraryBaseline.of(localContext, PATTERNS);
        updated.keepUnresolved(baseline, first.memoryOnly());
        baseline = updated;

        // The file now changes the same field: this must be reported as a conflict, not taken over silently
        disk.setField(StandardField.TITLE, "Disk title");
        LibraryBaseline.Triage second = triage();

        assertEquals(1, second.bothSides().size());
        assertTrue(second.diskOnly().isEmpty());
    }
}
