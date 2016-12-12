package net.sf.jabref.logic.bibtex;

import java.util.Optional;

import net.sf.jabref.model.EntryTypes;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.BibLatexEntryTypes;
import net.sf.jabref.model.entry.BibtexEntryTypes;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EntryTypesTestBibLatex {
    @Test
    @Ignore("This fails of unknown reasons - will be fixed with https://github.com/JabRef/jabref/pull/2331")
    public void testBibLatexMode() {
        // BibLatex mode
        assertEquals(BibLatexEntryTypes.ARTICLE, EntryTypes.getType("article", BibDatabaseMode.BIBLATEX).get());
        assertEquals(Optional.empty(), EntryTypes.getType("aaaaarticle", BibDatabaseMode.BIBLATEX));
        assertEquals(Optional.empty(), EntryTypes.getStandardType("aaaaarticle", BibDatabaseMode.BIBLATEX));
        assertEquals(34, EntryTypes.getAllValues(BibDatabaseMode.BIBLATEX).size());
        assertEquals(34, EntryTypes.getAllTypes(BibDatabaseMode.BIBLATEX).size());

        EntryTypes.removeType("article", BibDatabaseMode.BIBLATEX);
        // Should not be possible to remove a standard type
        assertEquals(BibLatexEntryTypes.ARTICLE, EntryTypes.getType("article", BibDatabaseMode.BIBLATEX).get());
    }

    @Test
    public void defaultType() {
        assertEquals(BibLatexEntryTypes.MISC, EntryTypes.getTypeOrDefault("unknowntype", BibDatabaseMode.BIBLATEX));
    }


    @Test
    public void detectExclusiveBiblatexType() {
        assertTrue(EntryTypes.isExclusiveBibLatex(BibLatexEntryTypes.MVBOOK.getName()));
    }

    @Test
    public void detectUndistinguishableAsBibtex() {
        assertFalse(EntryTypes.isExclusiveBibLatex(BibtexEntryTypes.ARTICLE.getName()));
        assertFalse(EntryTypes.isExclusiveBibLatex(BibLatexEntryTypes.ARTICLE.getName()));
    }

    @Test
    public void detectUnknownTypeAsBibtex() {
        assertFalse(EntryTypes.isExclusiveBibLatex("unknowntype"));
    }
}
