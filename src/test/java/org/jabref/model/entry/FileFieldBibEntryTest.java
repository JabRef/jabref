package org.jabref.model.entry;

import org.jabref.logic.exporter.BibtexDatabaseWriter;
import org.jabref.logic.exporter.SaveException;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.exporter.StringSaveSession;
import org.jabref.logic.util.OS;
import org.jabref.model.Defaults;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileFieldBibEntryTest {

    private BibEntry emptyEntry;

    @BeforeEach
    public void setUp() {
        emptyEntry = new BibEntry();
        emptyEntry.setType("article");
        emptyEntry.setChanged(false);
    }

    @Test
    public void testFileFieldSerialization() {
        LinkedFile file = new LinkedFile("test", "/home/uers/test.pdf", "PDF");
        emptyEntry.addFile(file);

        assertEquals("@article{,\n" +
                "  file = {test:/home/uers/test.pdf:PDF}\n" +
                "}", emptyEntry.toString());
    }

    @Test
    public void testFileFieldSerializationDatabase() throws SaveException {
        BibDatabase database = new BibDatabase();

        LinkedFile file = new LinkedFile("test", "/home/uers/test.pdf", "PDF");
        emptyEntry.addFile(file);
        database.insertEntries(emptyEntry);

        BibtexDatabaseWriter<StringSaveSession> databaseWriter = new BibtexDatabaseWriter<>(StringSaveSession::new);
        StringSaveSession saveSession = databaseWriter.savePartOfDatabase(
                new BibDatabaseContext(database, new MetaData(), new Defaults()), database.getEntries(),
                new SavePreferences());

        assertEquals(OS.NEWLINE +
                "@Article{,"
                + OS.NEWLINE
                + "  file = {test:/home/uers/test.pdf:PDF},"
                + OS.NEWLINE
                + "}" + OS.NEWLINE
                + OS.NEWLINE
                + "@Comment{jabref-meta: databaseType:bibtex;}" + OS.NEWLINE, saveSession.getStringValue());
    }
}
