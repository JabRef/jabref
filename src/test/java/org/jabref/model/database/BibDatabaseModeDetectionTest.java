package org.jabref.model.database;

import java.util.Arrays;
import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.entry.types.UnknownEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BibDatabaseModeDetectionTest {

    private static final EntryType UNKNOWN_TYPE = new UnknownEntryType("unknowntype");

    @Test
    public void detectBiblatex() {
        List<BibEntry> entries = Arrays.asList(new BibEntry(StandardEntryType.MvBook));

        assertEquals(BibDatabaseMode.BIBLATEX, BibDatabaseModeDetection.inferMode(new BibDatabase(entries)));
    }

    @Test
    public void detectUndistinguishableAsBibtex() {
        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.setField(StandardField.TITLE, "My cool paper");
        List<BibEntry> entries = Arrays.asList(entry);

        assertEquals(BibDatabaseMode.BIBTEX, BibDatabaseModeDetection.inferMode(new BibDatabase(entries)));
    }

    @Test
    public void detectMixedModeAsBiblatex() {
        BibEntry bibtex = new BibEntry(StandardEntryType.Article);
        bibtex.setField(StandardField.JOURNAL, "IEEE Trans. Services Computing");
        BibEntry biblatex = new BibEntry(StandardEntryType.Article);
        biblatex.setField(StandardField.TRANSLATOR, "Stefan Kolb");
        List<BibEntry> entries = Arrays.asList(bibtex, biblatex);

        assertEquals(BibDatabaseMode.BIBTEX, BibDatabaseModeDetection.inferMode(new BibDatabase(entries)));
    }

    @Test
    public void detectUnknownTypeAsBibtex() {
        BibEntry entry = new BibEntry(UNKNOWN_TYPE);
        List<BibEntry> entries = Arrays.asList(entry);

        assertEquals(BibDatabaseMode.BIBTEX, BibDatabaseModeDetection.inferMode(new BibDatabase(entries)));
    }

    @Test
    public void ignoreUnknownTypesForBibtexDecision() {
        BibEntry custom = new BibEntry(UNKNOWN_TYPE);
        BibEntry bibtex = new BibEntry(StandardEntryType.Article);
        BibEntry biblatex = new BibEntry(StandardEntryType.Article);
        List<BibEntry> entries = Arrays.asList(custom, bibtex, biblatex);

        assertEquals(BibDatabaseMode.BIBTEX, BibDatabaseModeDetection.inferMode(new BibDatabase(entries)));
    }

    @Test
    public void ignoreUnknownTypesForBiblatexDecision() {
        BibEntry custom = new BibEntry(UNKNOWN_TYPE);
        BibEntry bibtex = new BibEntry(StandardEntryType.Article);
        BibEntry biblatex = new BibEntry(StandardEntryType.MvBook);
        List<BibEntry> entries = Arrays.asList(custom, bibtex, biblatex);

        assertEquals(BibDatabaseMode.BIBLATEX, BibDatabaseModeDetection.inferMode(new BibDatabase(entries)));
    }
}
