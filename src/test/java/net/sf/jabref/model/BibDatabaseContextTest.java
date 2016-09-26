package net.sf.jabref.model;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.metadata.MetaData;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BibDatabaseContextTest {
    @Test
    public void testTypeBasedOnDefaultBibtex() {
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(new BibDatabase(), new MetaData(), new Defaults(BibDatabaseMode.BIBTEX));
        assertEquals(BibDatabaseMode.BIBTEX, bibDatabaseContext.getMode());

        bibDatabaseContext.setMode(BibDatabaseMode.BIBLATEX);
        assertEquals(BibDatabaseMode.BIBLATEX, bibDatabaseContext.getMode());
    }

    @Test
    public void testTypeBasedOnDefaultBiblatex() {
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(new BibDatabase(), new MetaData(), new Defaults(BibDatabaseMode.BIBLATEX));
        assertEquals(BibDatabaseMode.BIBLATEX, bibDatabaseContext.getMode());

        bibDatabaseContext.setMode(BibDatabaseMode.BIBTEX);
        assertEquals(BibDatabaseMode.BIBTEX, bibDatabaseContext.getMode());
    }

    @Test
    public void testTypeBasedOnInferredModeBibTeX() {
        BibDatabase db = new BibDatabase();
        BibEntry e1 = new BibEntry("1");
        db.insertEntry(e1);

        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(db);
        assertEquals(BibDatabaseMode.BIBTEX, bibDatabaseContext.getMode());
    }

    @Test
    public void testTypeBasedOnInferredModeBiblatex() {
        BibDatabase db = new BibDatabase();
        BibEntry e1 = new BibEntry("1", "electronic");
        db.insertEntry(e1);

        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(db);
        assertEquals(BibDatabaseMode.BIBLATEX, bibDatabaseContext.getMode());
    }
}
