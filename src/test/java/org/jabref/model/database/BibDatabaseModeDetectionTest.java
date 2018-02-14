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
        Collection<BibEntry> entries = Arrays.asList(new BibEntry(BiblatexEntryTypes.MVBOOK.getName()));

        assertEquals(BibDatabaseMode.BIBLATEX, BibDatabaseModeDetection.inferMode(BibDatabases.createDatabase(entries)));
    }

    @Test
    public void detectUndistinguishableAsBibtex() {
        BibEntry entry = new BibEntry(BibtexEntryTypes.ARTICLE.getName());
        entry.setField("title", "My cool paper");
        Collection<BibEntry> entries = Arrays.asList(entry);

        assertEquals(BibDatabaseMode.BIBTEX, BibDatabaseModeDetection.inferMode(BibDatabases.createDatabase(entries)));
    }

    @Test
    public void detectMixedModeAsBiblatex() {
        BibEntry bibtex = new BibEntry(BibtexEntryTypes.ARTICLE.getName());
        bibtex.setField("journal", "IEEE Trans. Services Computing");
        BibEntry biblatex = new BibEntry(BiblatexEntryTypes.ARTICLE.getName());
        biblatex.setField("translator", "Stefan Kolb");
        Collection<BibEntry> entries = Arrays.asList(bibtex, biblatex);

        assertEquals(BibDatabaseMode.BIBTEX, BibDatabaseModeDetection.inferMode(BibDatabases.createDatabase(entries)));
    }

    @Test
    public void detectUnknownTypeAsBibtex() {
        BibEntry entry = new BibEntry(new CustomEntryType("unknowntype", new ArrayList<>(0), new ArrayList<>(0)).getName());
        Collection<BibEntry> entries = Arrays.asList(entry);

        assertEquals(BibDatabaseMode.BIBTEX, BibDatabaseModeDetection.inferMode(BibDatabases.createDatabase(entries)));
    }

    @Test
    public void ignoreUnknownTypesForBibtexDecision() {
        BibEntry custom = new BibEntry(new CustomEntryType("unknowntype", new ArrayList<>(0), new ArrayList<>(0)).getName());
        BibEntry bibtex = new BibEntry(BibtexEntryTypes.ARTICLE.getName());
        BibEntry biblatex = new BibEntry(BiblatexEntryTypes.ARTICLE.getName());
        Collection<BibEntry> entries = Arrays.asList(custom, bibtex, biblatex);

        assertEquals(BibDatabaseMode.BIBTEX, BibDatabaseModeDetection.inferMode(BibDatabases.createDatabase(entries)));
    }

    @Test
    public void ignoreUnknownTypesForBiblatexDecision() {
        BibEntry custom = new BibEntry(new CustomEntryType("unknowntype", new ArrayList<>(0), new ArrayList<>(0)).getName());
        BibEntry bibtex = new BibEntry(BibtexEntryTypes.ARTICLE.getName());
        BibEntry biblatex = new BibEntry(BiblatexEntryTypes.MVBOOK.getName());
        Collection<BibEntry> entries = Arrays.asList(custom, bibtex, biblatex);

        assertEquals(BibDatabaseMode.BIBLATEX, BibDatabaseModeDetection.inferMode(BibDatabases.createDatabase(entries)));
    }

}
