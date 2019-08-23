package org.jabref.model.database;

import java.util.Arrays;
import java.util.Collection;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.BiblatexEntryType;
import org.jabref.model.entry.types.BibtexEntryType;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.UnknownEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BibDatabaseModeDetectionTest {

    private static final EntryType UNKNOWN_TYPE = new UnknownEntryType("unknowntype");

    @Test
    public void detectBiblatex() {
        Collection<BibEntry> entries = Arrays.asList(new BibEntry(BiblatexEntryType.MvBook));

        assertEquals(BibDatabaseMode.BIBLATEX, BibDatabaseModeDetection.inferMode(BibDatabases.createDatabase(entries)));
    }

    @Test
    public void detectUndistinguishableAsBibtex() {
        BibEntry entry = new BibEntry(BibtexEntryType.Article);
        entry.setField(StandardField.TITLE, "My cool paper");
        Collection<BibEntry> entries = Arrays.asList(entry);

        assertEquals(BibDatabaseMode.BIBTEX, BibDatabaseModeDetection.inferMode(BibDatabases.createDatabase(entries)));
    }

    @Test
    public void detectMixedModeAsBiblatex() {
        BibEntry bibtex = new BibEntry(BibtexEntryType.Article);
        bibtex.setField(StandardField.JOURNAL, "IEEE Trans. Services Computing");
        BibEntry biblatex = new BibEntry(BibtexEntryType.Article);
        biblatex.setField(StandardField.TRANSLATOR, "Stefan Kolb");
        Collection<BibEntry> entries = Arrays.asList(bibtex, biblatex);

        assertEquals(BibDatabaseMode.BIBTEX, BibDatabaseModeDetection.inferMode(BibDatabases.createDatabase(entries)));
    }

    @Test
    public void detectUnknownTypeAsBibtex() {
        BibEntry entry = new BibEntry(UNKNOWN_TYPE);
        Collection<BibEntry> entries = Arrays.asList(entry);

        assertEquals(BibDatabaseMode.BIBTEX, BibDatabaseModeDetection.inferMode(BibDatabases.createDatabase(entries)));
    }

    @Test
    public void ignoreUnknownTypesForBibtexDecision() {
        BibEntry custom = new BibEntry(UNKNOWN_TYPE);
        BibEntry bibtex = new BibEntry(BibtexEntryType.Article);
        BibEntry biblatex = new BibEntry(BibtexEntryType.Article);
        Collection<BibEntry> entries = Arrays.asList(custom, bibtex, biblatex);

        assertEquals(BibDatabaseMode.BIBTEX, BibDatabaseModeDetection.inferMode(BibDatabases.createDatabase(entries)));
    }

    @Test
    public void ignoreUnknownTypesForBiblatexDecision() {
        BibEntry custom = new BibEntry(UNKNOWN_TYPE);
        BibEntry bibtex = new BibEntry(BibtexEntryType.Article);
        BibEntry biblatex = new BibEntry(BiblatexEntryType.MvBook);
        Collection<BibEntry> entries = Arrays.asList(custom, bibtex, biblatex);

        assertEquals(BibDatabaseMode.BIBLATEX, BibDatabaseModeDetection.inferMode(BibDatabases.createDatabase(entries)));
    }
}
