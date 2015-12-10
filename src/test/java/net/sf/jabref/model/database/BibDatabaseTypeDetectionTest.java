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

public class BibDatabaseTypeDetectionTest {
    @Test
    public void detectBiblatex() {
        Collection<BibEntry> entries = Arrays.asList(new BibEntry("someid", BibLatexEntryTypes.MVBOOK));

        assertEquals(BibDatabaseType.BIBLATEX, BibDatabaseTypeDetection.inferType(entries));
    }

    @Test
    public void detectBiblatexBasedOnFields() {
        BibEntry entry = new BibEntry("someid", BibtexEntryTypes.ARTICLE);
        entry.setField("translator", "Stefan Kolb");
        Collection<BibEntry> entries = Arrays.asList(entry);

        assertEquals(BibDatabaseType.BIBLATEX, BibDatabaseTypeDetection.inferType(entries));
    }

    @Test
    public void detectBibtexBasedOnFields() {
        BibEntry entry = new BibEntry("someid", BibtexEntryTypes.ARTICLE);
        entry.setField("journal", "IEEE Trans. Services Computing");
        Collection<BibEntry> entries = Arrays.asList(entry);

        assertEquals(BibDatabaseType.BIBTEX, BibDatabaseTypeDetection.inferType(entries));
    }

    @Test
    public void detectUndistinguishableAsBibtex() {
        BibEntry entry = new BibEntry("someid", BibtexEntryTypes.ARTICLE);
        entry.setField("title", "My cool paper");
        Collection<BibEntry> entries = Arrays.asList(entry);

        assertEquals(BibDatabaseType.BIBTEX, BibDatabaseTypeDetection.inferType(entries));
    }

    @Test
    public void detectSingleUnknownTypeAsBibtex() {
        BibEntry entry = new BibEntry("someid", new CustomEntryType("unknowntype", new ArrayList<>(0), new ArrayList<>(0)));
        Collection<BibEntry> entries = Arrays.asList(entry);

        assertEquals(BibDatabaseType.BIBTEX, BibDatabaseTypeDetection.inferType(entries));
    }

    @Test
    public void ignoreUnknownTypesForDecision() {
        // BibTex
        BibEntry custom = new BibEntry("someid", new CustomEntryType("unknowntype", new ArrayList<>(0), new ArrayList<>(0)));
        BibEntry bibtex = new BibEntry("someid", BibtexEntryTypes.ARTICLE);
        Collection<BibEntry> entries = Arrays.asList(custom, bibtex);

        assertEquals(BibDatabaseType.BIBTEX, BibDatabaseTypeDetection.inferType(entries));

        // Biblatex
        BibEntry biblatex = new BibEntry("someid", BibLatexEntryTypes.MVBOOK);
        entries = Arrays.asList(custom, biblatex);

        assertEquals(BibDatabaseType.BIBLATEX, BibDatabaseTypeDetection.inferType(entries));

        // Field-based Biblatex
        biblatex = new BibEntry("someid", BibtexEntryTypes.ARTICLE);
        biblatex.setField("translator", "Stefan Kolb");
        entries = Arrays.asList(custom, biblatex, bibtex);

        assertEquals(BibDatabaseType.BIBLATEX, BibDatabaseTypeDetection.inferType(entries));

        // Field-based BibTex
        biblatex = new BibEntry("someid", BibtexEntryTypes.ARTICLE);
        bibtex.setField("journal", "IEEE Trans. Services Computing");
        entries = Arrays.asList(custom, biblatex, bibtex);

        assertEquals(BibDatabaseType.BIBTEX, BibDatabaseTypeDetection.inferType(entries));
    }
}