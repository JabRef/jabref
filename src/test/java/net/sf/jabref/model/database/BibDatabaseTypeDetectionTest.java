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
    public void detectMixedModeAsBiblatex() {
        BibEntry bibtex = new BibEntry("someid", BibtexEntryTypes.ARTICLE);
        bibtex.setField("journal", "IEEE Trans. Services Computing");
        BibEntry biblatex = new BibEntry("someid", BibLatexEntryTypes.ARTICLE);
        biblatex.setField("translator", "Stefan Kolb");
        Collection<BibEntry> entries = Arrays.asList(bibtex, biblatex);

        assertEquals(BibDatabaseType.BIBLATEX, BibDatabaseTypeDetection.inferType(entries));
    }

    @Test
    public void detectUnknownTypeAsBibtex() {
        BibEntry entry = new BibEntry("someid", new CustomEntryType("unknowntype", new ArrayList<>(0), new ArrayList<>(0)));
        Collection<BibEntry> entries = Arrays.asList(entry);

        assertEquals(BibDatabaseType.BIBTEX, BibDatabaseTypeDetection.inferType(entries));
    }

    @Test
    public void detectUnknownTypeAsBibtexBasedOnFields() {
        BibEntry entry = new BibEntry("someid", new CustomEntryType("unknowntype", new ArrayList<>(0), new ArrayList<>(0)));
        entry.setField("someunknownfield", "value");
        Collection<BibEntry> entries = Arrays.asList(entry);

        assertEquals(BibDatabaseType.BIBTEX, BibDatabaseTypeDetection.inferType(entries));
    }

    @Test
    public void ignoreUnknownTypesForBibtexDecision() {
        BibEntry custom = new BibEntry("someid", new CustomEntryType("unknowntype", new ArrayList<>(0), new ArrayList<>(0)));
        BibEntry bibtex = new BibEntry("someid", BibtexEntryTypes.ARTICLE);
        BibEntry biblatex = new BibEntry("someid", BibLatexEntryTypes.ARTICLE);
        Collection<BibEntry> entries = Arrays.asList(custom, bibtex, biblatex);

        assertEquals(BibDatabaseType.BIBTEX, BibDatabaseTypeDetection.inferType(entries));
    }

    @Test
    public void ignoreUnknownTypesForBibtexDecisionBasedOnFields() {
        BibEntry custom = new BibEntry("someid", new CustomEntryType("unknowntype", new ArrayList<>(0), new ArrayList<>(0)));
        custom.setField("someunknownfield", "value");
        BibEntry bibtex = new BibEntry("someid", BibtexEntryTypes.ARTICLE);
        bibtex.setField("journal", "IEEE Trans. Services Computing");
        BibEntry biblatex = new BibEntry("someid", BibLatexEntryTypes.ARTICLE);
        biblatex.setField("title", "someothertitle");
        Collection<BibEntry> entries = Arrays.asList(custom, bibtex, biblatex);

        assertEquals(BibDatabaseType.BIBTEX, BibDatabaseTypeDetection.inferType(entries));
    }

    @Test
    public void ignoreUnknownTypesForBiblatexDecision() {
        BibEntry custom = new BibEntry("someid", new CustomEntryType("unknowntype", new ArrayList<>(0), new ArrayList<>(0)));
        BibEntry bibtex = new BibEntry("someid", BibtexEntryTypes.ARTICLE);
        BibEntry biblatex = new BibEntry("someid", BibLatexEntryTypes.MVBOOK);
        Collection<BibEntry> entries = Arrays.asList(custom, bibtex, biblatex);

        assertEquals(BibDatabaseType.BIBLATEX, BibDatabaseTypeDetection.inferType(entries));
    }

    @Test
    public void ignoreUnknownTypesForBiblatexDecisionBasedOnFields() {
        BibEntry custom = new BibEntry("someid", new CustomEntryType("unknowntype", new ArrayList<>(0), new ArrayList<>(0)));
        custom.setField("someunknownfield", "value");
        BibEntry bibtex = new BibEntry("someid", BibtexEntryTypes.ARTICLE);
        bibtex.setField("title", "IEEE Trans. Services Computing");
        BibEntry biblatex = new BibEntry("someid", BibLatexEntryTypes.ARTICLE);
        biblatex.setField("translator", "Stefan Kolb");
        Collection<BibEntry> entries = Arrays.asList(custom, bibtex, biblatex);

        assertEquals(BibDatabaseType.BIBLATEX, BibDatabaseTypeDetection.inferType(entries));
    }
}