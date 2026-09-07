package org.jabref.logic.sync;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.sync.LibraryBaseline.Side;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LibraryBaselineTest {

    private static final GlobalCitationKeyPatterns PATTERNS = GlobalCitationKeyPatterns.fromPattern("[auth][year]");

    private BibEntry local;
    private BibEntry remote;
    private LibraryBaseline baseline;

    @BeforeEach
    void setUp() {
        local = new BibEntry(StandardEntryType.Article).withCitationKey("Key").withField(StandardField.TITLE, "Title").withField(StandardField.YEAR, "2020");
        baseline = LibraryBaseline.of(new BibDatabaseContext(new BibDatabase(List.of(local))), PATTERNS);
        // the file as it would be parsed: same content, different entry object
        remote = new BibEntry(local);
    }

    @Test
    void entryChangedOnDiskOnly() {
        remote.setField(StandardField.TITLE, "Disk title");

        assertEquals(Side.DISK, baseline.sideOfEntry(local, remote));
    }

    @Test
    void entryChangedInMemoryOnly() {
        local.setField(StandardField.TITLE, "Memory title");

        assertEquals(Side.MEMORY, baseline.sideOfEntry(local, remote));
    }

    @Test
    void entryChangedOnBothSides() {
        local.setField(StandardField.TITLE, "Memory title");
        remote.setField(StandardField.TITLE, "Disk title");

        assertEquals(Side.BOTH, baseline.sideOfEntry(local, remote));
    }

    @Test
    void differentFieldsChangedOnBothSidesAreMerged() {
        local.setField(StandardField.TITLE, "Memory title");
        remote.setField(StandardField.YEAR, "2021");

        Optional<BibEntry> merged = baseline.mergeEntry(local, remote);

        assertEquals(Optional.of(new BibEntry(StandardEntryType.Article).withCitationKey("Key").withField(StandardField.TITLE, "Memory title").withField(StandardField.YEAR, "2021")), merged);
    }

    @Test
    void sameFieldChangedDifferentlyIsNotMerged() {
        local.setField(StandardField.TITLE, "Memory title");
        remote.setField(StandardField.TITLE, "Disk title");

        assertEquals(Optional.empty(), baseline.mergeEntry(local, remote));
    }

    @Test
    void entryDeletedOnDiskWhenUntouchedInMemory() {
        assertEquals(Side.DISK, baseline.sideOfDeletedEntry(local));
    }

    @Test
    void entryUnknownToBaselineWasAddedInMemory() {
        BibEntry added = new BibEntry(StandardEntryType.Misc).withCitationKey("New");

        assertEquals(Side.MEMORY, baseline.sideOfDeletedEntry(added));
    }

    @Test
    void addedEntryUnknownToBaselineIsNewOnDisk() {
        BibEntry added = new BibEntry(StandardEntryType.Misc).withCitationKey("New");

        assertEquals(Side.DISK, baseline.lookup().sideOfAddedEntry(added));
    }

    @Test
    void addedEntryEqualToBaselineWasDeletedInMemory() {
        assertEquals(Side.MEMORY, baseline.lookup().sideOfAddedEntry(remote));
    }

    @Test
    void renamedEntryIsFoundByContent() {
        remote.setCitationKey("Renamed");

        assertEquals(Optional.of(local.getId()), baseline.lookup().baseIdOf(remote));
    }

    @Test
    void duplicateKeysPreferIdenticalContent() {
        BibEntry duplicate = new BibEntry(StandardEntryType.Article).withCitationKey("Key").withField(StandardField.TITLE, "Duplicate");
        baseline = LibraryBaseline.of(new BibDatabaseContext(new BibDatabase(List.of(duplicate, local))), PATTERNS);

        assertEquals(Optional.of(local.getId()), baseline.lookup().baseIdOf(remote));
    }

    @Test
    void stringAddedOnDiskOnly() {
        assertEquals(Side.DISK, baseline.sideOfString("jan", null, "January"));
    }

    @Test
    void keptEntryStaysTheAncestorOfTheNextComparison() {
        local.setField(StandardField.TITLE, "Memory title");
        LibraryBaseline updated = LibraryBaseline.of(new BibDatabaseContext(new BibDatabase(List.of(local))), PATTERNS);
        updated.keepEntry(baseline, local.getId());

        // Against the kept ancestor, the memory edit is still visible as such
        assertEquals(Side.MEMORY, updated.sideOfEntry(local, remote));
    }

    @Test
    void commentChangedOnDiskOnly() {
        remote.setCommentsBeforeEntry("% new comment");

        assertEquals(Side.DISK, baseline.sideOfEntry(local, remote));
    }

    @Test
    void commentChangedOnDiskIsMergedWithFieldChangedInMemory() {
        local.setField(StandardField.TITLE, "Memory title");
        remote.setCommentsBeforeEntry("% new comment");

        Optional<BibEntry> merged = baseline.mergeEntry(local, remote);

        assertEquals(Optional.of("% new comment"), merged.map(BibEntry::getUserComments));
        assertEquals(Optional.of("Memory title"), merged.flatMap(entry -> entry.getField(StandardField.TITLE)));
    }

    @Test
    void ambiguousDuplicatesAreNotAssociated() {
        BibEntry duplicate = new BibEntry(local);
        baseline = LibraryBaseline.of(new BibDatabaseContext(new BibDatabase(List.of(duplicate, local))), PATTERNS);
        remote.setField(StandardField.TITLE, "Changed on disk");

        assertEquals(Optional.empty(), baseline.lookup().baseIdOf(remote));
    }

    @Test
    void stringDeletedInMemoryAndRenamedOnDiskIsAConflict() {
        BibDatabase database = new BibDatabase(List.of(local));
        database.addString(new BibtexString("jan", "January"));
        baseline = LibraryBaseline.of(new BibDatabaseContext(database), PATTERNS);

        // memory has no string anymore, disk has the same content under a new name
        assertEquals(Side.BOTH, baseline.sideOfAddedString("january", "January", _ -> false));
        assertEquals(Side.DISK, baseline.sideOfAddedString("feb", "February", _ -> false));
    }
}
