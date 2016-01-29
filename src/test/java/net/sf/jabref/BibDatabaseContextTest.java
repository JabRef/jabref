package net.sf.jabref;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseMode;
import org.junit.Test;

import static org.junit.Assert.*;

public class BibDatabaseContextTest {

    @Test
    public void testReadWriteType() {
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(new BibDatabase(), new MetaData(), new Defaults(BibDatabaseMode.BIBTEX));
        bibDatabaseContext.setMode(BibDatabaseMode.BIBLATEX);
        assertEquals(BibDatabaseMode.BIBLATEX, bibDatabaseContext.getMode());

        bibDatabaseContext.setMode(BibDatabaseMode.BIBTEX);
        assertEquals(BibDatabaseMode.BIBTEX, bibDatabaseContext.getMode());
    }

}
