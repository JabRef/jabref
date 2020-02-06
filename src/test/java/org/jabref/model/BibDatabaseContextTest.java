package org.jabref.model;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.IEEETranEntryType;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BibDatabaseContextTest {
    @Test
    void testTypeBasedOnDefaultBiblatex() {
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(new BibDatabase(), new MetaData());
        assertEquals(BibDatabaseMode.BIBLATEX, bibDatabaseContext.getMode());

        bibDatabaseContext.setMode(BibDatabaseMode.BIBLATEX);
        assertEquals(BibDatabaseMode.BIBLATEX, bibDatabaseContext.getMode());
    }

    @Test
    void testTypeBasedOnDefaultBibtex() {
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(new BibDatabase(), new MetaData());
        assertEquals(BibDatabaseMode.BIBLATEX, bibDatabaseContext.getMode());

        bibDatabaseContext.setMode(BibDatabaseMode.BIBTEX);
        assertEquals(BibDatabaseMode.BIBTEX, bibDatabaseContext.getMode());
    }

    @Test
    void testTypeBasedOnInferredModeBiblatex() {
        BibDatabase db = new BibDatabase();
        BibEntry e1 = new BibEntry(IEEETranEntryType.Electronic);
        db.insertEntry(e1);

        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(db);
        assertEquals(BibDatabaseMode.BIBLATEX, bibDatabaseContext.getMode());
    }
}
