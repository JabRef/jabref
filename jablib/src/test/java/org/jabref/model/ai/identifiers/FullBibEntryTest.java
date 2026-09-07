package org.jabref.model.ai.identifiers;

import java.util.List;
import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FullBibEntryTest {

    @Test
    void findEntryByLinkFindsEntryEvenIfDatabaseContextIsEmpty() {
        BibEntry entry = new BibEntry()
                .withCitationKey("Smith2024")
                .withFiles(List.of(new LinkedFile("", "path/to/file.pdf", "PDF")));

        FullBibEntry fullEntry = new FullBibEntry(new BibDatabaseContext(), entry);

        Optional<BibEntry> result = FullBibEntry.findEntryByLink(List.of(fullEntry), "path/to/file.pdf");

        assertEquals(Optional.of(entry), result);
    }

    @Test
    void findEntryByLinkReturnsEmptyForNonMatchingLink() {
        BibEntry entry = new BibEntry()
                .withCitationKey("Smith2024")
                .withFiles(List.of(new LinkedFile("", "path/to/file.pdf", "PDF")));

        FullBibEntry fullEntry = new FullBibEntry(new BibDatabaseContext(), entry);

        Optional<BibEntry> result = FullBibEntry.findEntryByLink(List.of(fullEntry), "other/path.pdf");

        assertEquals(Optional.empty(), result);
    }
}
