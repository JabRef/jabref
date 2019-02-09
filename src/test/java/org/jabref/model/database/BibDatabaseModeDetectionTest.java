package org.jabref.model.database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BiblatexEntryTypes;
import org.jabref.model.entry.BibtexEntryTypes;
import org.jabref.model.entry.CustomEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BibDatabaseModeDetectionTest {
    @Test
    public void detectBiblatex() {
        Collection<BibEntry> entries = Arrays.asList(new BibEntry(BiblatexEntryTypes.MVBOOK));

        assertEquals(BibDatabaseMode.BIBLATEX, BibDatabaseModeDetection.inferMode(BibDatabases.createDatabase(entries)));
    }

    @Test
    public void detectUndistinguishableAsBibtex() {
        BibEntry entry = new BibEntry(BibtexEntryTypes.ARTICLE);
        entry.setField("title", "My cool paper");
        Collection<BibEntry> entries = Arrays.asList(entry);

        assertEquals(BibDatabaseMode.BIBTEX, BibDatabaseModeDetection.inferMode(BibDatabases.createDatabase(entries)));
    }

    @Test
    public void detectMixedModeAsBiblatex() {
        BibEntry bibtex = new BibEntry(BibtexEntryTypes.ARTICLE);
        bibtex.setField("journal", "IEEE Trans. Services Computing");
        BibEntry biblatex = new BibEntry(BiblatexEntryTypes.ARTICLE);
        biblatex.setField("translator", "Stefan Kolb");
        Collection<BibEntry> entries = Arrays.asList(bibtex, biblatex);

        assertEquals(BibDatabaseMode.BIBTEX, BibDatabaseModeDetection.inferMode(BibDatabases.createDatabase(entries)));
    }

    @Test
    public void detectUnknownTypeAsBibtex() {
        BibEntry entry = new BibEntry(new CustomEntryType("unknowntype", new ArrayList<>(0), new ArrayList<>(0)));
        Collection<BibEntry> entries = Arrays.asList(entry);

        assertEquals(BibDatabaseMode.BIBTEX, BibDatabaseModeDetection.inferMode(BibDatabases.createDatabase(entries)));
    }

    @Test
    public void ignoreUnknownTypesForBibtexDecision() {
        BibEntry custom = new BibEntry(new CustomEntryType("unknowntype", new ArrayList<>(0), new ArrayList<>(0)));
        BibEntry bibtex = new BibEntry(BibtexEntryTypes.ARTICLE);
        BibEntry biblatex = new BibEntry(BiblatexEntryTypes.ARTICLE);
        Collection<BibEntry> entries = Arrays.asList(custom, bibtex, biblatex);

        assertEquals(BibDatabaseMode.BIBTEX, BibDatabaseModeDetection.inferMode(BibDatabases.createDatabase(entries)));
    }

    @Test
    public void ignoreUnknownTypesForBiblatexDecision() {
        BibEntry custom = new BibEntry(new CustomEntryType("unknowntype", new ArrayList<>(0), new ArrayList<>(0)));
        BibEntry bibtex = new BibEntry(BibtexEntryTypes.ARTICLE);
        BibEntry biblatex = new BibEntry(BiblatexEntryTypes.MVBOOK);
        Collection<BibEntry> entries = Arrays.asList(custom, bibtex, biblatex);

        assertEquals(BibDatabaseMode.BIBLATEX, BibDatabaseModeDetection.inferMode(BibDatabases.createDatabase(entries)));
    }
}
