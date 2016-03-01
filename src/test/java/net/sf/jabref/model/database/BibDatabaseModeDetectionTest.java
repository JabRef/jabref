package net.sf.jabref.model.database;

import net.sf.jabref.model.entry.BibLatexEntryTypes;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import net.sf.jabref.model.entry.CustomEntryType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

public class BibDatabaseModeDetectionTest {
    @Test
    public void detectBiblatex() {
        Collection<BibEntry> entries = Arrays.asList(new BibEntry("someid", BibLatexEntryTypes.MVBOOK.getName()));

        assertEquals(BibDatabaseMode.BIBLATEX, BibDatabaseModeDetection.inferMode(BibDatabases.createDatabase(entries)));
    }

    @Test
    public void detectUndistinguishableAsBibtex() {
        BibEntry entry = new BibEntry("someid", BibtexEntryTypes.ARTICLE.getName());
        entry.setField("title", "My cool paper");
        Collection<BibEntry> entries = Arrays.asList(entry);

        assertEquals(BibDatabaseMode.BIBTEX, BibDatabaseModeDetection.inferMode(BibDatabases.createDatabase(entries)));
    }

    @Test
    public void detectMixedModeAsBiblatex() {
        BibEntry bibtex = new BibEntry("someid", BibtexEntryTypes.ARTICLE.getName());
        bibtex.setField("journal", "IEEE Trans. Services Computing");
        BibEntry biblatex = new BibEntry("someid", BibLatexEntryTypes.ARTICLE.getName());
        biblatex.setField("translator", "Stefan Kolb");
        Collection<BibEntry> entries = Arrays.asList(bibtex, biblatex);

        assertEquals(BibDatabaseMode.BIBTEX, BibDatabaseModeDetection.inferMode(BibDatabases.createDatabase(entries)));
    }

    @Test
    public void detectUnknownTypeAsBibtex() {
        BibEntry entry = new BibEntry("someid", new CustomEntryType("unknowntype", new ArrayList<>(0), new ArrayList<>(0)).getName());
        Collection<BibEntry> entries = Arrays.asList(entry);

        assertEquals(BibDatabaseMode.BIBTEX, BibDatabaseModeDetection.inferMode(BibDatabases.createDatabase(entries)));
    }

    @Test
    public void ignoreUnknownTypesForBibtexDecision() {
        BibEntry custom = new BibEntry("someid", new CustomEntryType("unknowntype", new ArrayList<>(0), new ArrayList<>(0)).getName());
        BibEntry bibtex = new BibEntry("someid", BibtexEntryTypes.ARTICLE.getName());
        BibEntry biblatex = new BibEntry("someid", BibLatexEntryTypes.ARTICLE.getName());
        Collection<BibEntry> entries = Arrays.asList(custom, bibtex, biblatex);

        assertEquals(BibDatabaseMode.BIBTEX, BibDatabaseModeDetection.inferMode(BibDatabases.createDatabase(entries)));
    }

    @Test
    public void ignoreUnknownTypesForBiblatexDecision() {
        BibEntry custom = new BibEntry("someid", new CustomEntryType("unknowntype", new ArrayList<>(0), new ArrayList<>(0)).getName());
        BibEntry bibtex = new BibEntry("someid", BibtexEntryTypes.ARTICLE.getName());
        BibEntry biblatex = new BibEntry("someid", BibLatexEntryTypes.MVBOOK.getName());
        Collection<BibEntry> entries = Arrays.asList(custom, bibtex, biblatex);

        assertEquals(BibDatabaseMode.BIBLATEX, BibDatabaseModeDetection.inferMode(BibDatabases.createDatabase(entries)));
    }

}
