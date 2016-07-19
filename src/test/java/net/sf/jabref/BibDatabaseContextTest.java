package net.sf.jabref;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BibDatabaseContextTest {

    @Before
    public void setUp() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();
    }

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

}
