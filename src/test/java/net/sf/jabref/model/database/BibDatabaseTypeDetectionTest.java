package net.sf.jabref.model.database;

import net.sf.jabref.model.entry.BibLatexEntryTypes;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

public class BibDatabaseTypeDetectionTest {
    @Test
    public void detectBiblatexOnly() {
        Collection<BibEntry> entries = Arrays.asList(new BibEntry("someid", BibLatexEntryTypes.MVBOOK));

        assertEquals(BibDatabaseType.BIBLATEX, BibDatabaseTypeDetection.inferType(entries));
    }

    @Test
    public void detectBiblatexFieldBased() {
        BibEntry entry = new BibEntry("someid", BibtexEntryTypes.ARTICLE);
        entry.setField("translator", "Stefan Kolb");
        Collection<BibEntry> entries = Arrays.asList(entry);

        assertEquals(BibDatabaseType.BIBLATEX, BibDatabaseTypeDetection.inferType(entries));
    }

    @Test
    public void detectBibtexFieldBased() {
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
}