package net.sf.jabref.model.database;

import net.sf.jabref.model.entry.BibLatexEntryTypes;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

public class BibTypeDetectionTest {
    @Test
    public void detectBiblatexOnly() {
        Collection<BibtexEntry> entries = Arrays.asList(new BibtexEntry("someid", BibLatexEntryTypes.MVBOOK));

        assertEquals(BibType.BIBLATEX, BibTypeDetection.inferType(entries));
    }

    @Test
    public void detectBibtexOnly() {
        Collection<BibtexEntry> entries = Arrays.asList(new BibtexEntry("someid", BibtexEntryTypes.MASTERSTHESIS));

        assertEquals(BibType.BIBTEX, BibTypeDetection.inferType(entries));
    }

    @Test
    public void detectBiblatexFieldBased() {
        BibtexEntry entry = new BibtexEntry("someid", BibtexEntryTypes.ARTICLE);
        entry.setField("translator", "Stefan Kolb");
        Collection<BibtexEntry> entries = Arrays.asList(entry);

        assertEquals(BibType.BIBLATEX, BibTypeDetection.inferType(entries));
    }

    @Test
    public void detectBibtexFieldBased() {
        BibtexEntry entry = new BibtexEntry("someid", BibtexEntryTypes.ARTICLE);
        entry.setField("journal", "IEEE Trans. Services Computing");
        Collection<BibtexEntry> entries = Arrays.asList(entry);

        assertEquals(BibType.BIBTEX, BibTypeDetection.inferType(entries));
    }

    @Test
    public void detectUndistinguishableAsBibtex() {
        BibtexEntry entry = new BibtexEntry("someid", BibtexEntryTypes.ARTICLE);
        entry.setField("title", "My cool paper");
        Collection<BibtexEntry> entries = Arrays.asList(entry);

        assertEquals(BibType.BIBTEX, BibTypeDetection.inferType(entries));
    }

    // TODO: what about custom entry types?, Define type detection on type field in jabref bib file?
}