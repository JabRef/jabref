package net.sf.jabref;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseType;
import org.junit.Test;

import static org.junit.Assert.*;

public class LoadedDatabaseTest {

    @Test
    public void testReadWriteType() {
        LoadedDatabase loadedDatabase = new LoadedDatabase(new BibDatabase(), new MetaData());
        loadedDatabase.setType(BibDatabaseType.BIBLATEX);
        assertEquals(BibDatabaseType.BIBLATEX, loadedDatabase.getType());

        loadedDatabase.setType(BibDatabaseType.BIBTEX);
        assertEquals(BibDatabaseType.BIBTEX, loadedDatabase.getType());
    }

}
