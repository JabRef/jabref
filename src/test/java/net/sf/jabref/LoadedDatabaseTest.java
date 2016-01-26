package net.sf.jabref;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseMode;
import org.junit.Test;

import static org.junit.Assert.*;

public class LoadedDatabaseTest {

    @Test
    public void testReadWriteType() {
        LoadedDatabase loadedDatabase = new LoadedDatabase(new BibDatabase(), new MetaData());
        loadedDatabase.setMode(BibDatabaseMode.BIBLATEX);
        assertEquals(BibDatabaseMode.BIBLATEX, loadedDatabase.getMode());

        loadedDatabase.setMode(BibDatabaseMode.BIBTEX);
        assertEquals(BibDatabaseMode.BIBTEX, loadedDatabase.getMode());
    }

}
