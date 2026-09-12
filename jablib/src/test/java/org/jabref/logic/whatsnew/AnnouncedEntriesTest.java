package org.jabref.logic.whatsnew;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [utest->req~whats-new.checkout-news~1]
class AnnouncedEntriesTest {

    private static final ChangelogEntry ADDED = new ChangelogEntry("Unreleased", "Added", "We added a button.");
    private static final ChangelogEntry FIXED = new ChangelogEntry("6.0-alpha.6 (2026-05-14)", "Fixed", "We fixed `a crash` [#1](https://example.org/1).");

    @Test
    void nothingIsAnnouncedBeforeTheFirstWrite(@TempDir Path directory) throws IOException {
        assertEquals(Optional.empty(), new AnnouncedEntries(directory.resolve("announced.tsv")).read());
    }

    @Test
    void writtenEntriesAreReadBack(@TempDir Path directory) throws IOException {
        AnnouncedEntries announced = new AnnouncedEntries(directory.resolve("state").resolve("announced.tsv"));

        announced.write(List.of(ADDED, FIXED));

        assertEquals(Optional.of(Set.of(ADDED, FIXED)), announced.read());
    }

    @Test
    void aWriteReplacesTheEntriesAnnouncedBefore(@TempDir Path directory) throws IOException {
        AnnouncedEntries announced = new AnnouncedEntries(directory.resolve("announced.tsv"));
        announced.write(List.of(ADDED));

        announced.write(List.of(FIXED));

        assertEquals(Optional.of(Set.of(FIXED)), announced.read());
    }

    @Test
    void aDamagedLineIsSkipped(@TempDir Path directory) throws IOException {
        Path file = directory.resolve("announced.tsv");
        Files.write(file, List.of("Unreleased\tAdded\tWe added a button.", "not an entry"));

        assertEquals(Optional.of(Set.of(ADDED)), new AnnouncedEntries(file).read());
    }
}
