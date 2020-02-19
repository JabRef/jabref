package org.jabref.logic.cleanup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FileFieldWriter;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.metadata.FilePreferences;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MoveFilesCleanupTest {

    private Path defaultFileFolder;
    private Path fileBefore;
    private MoveFilesCleanup cleanup;
    private BibEntry entry;
    private FilePreferences filePreferences;

    @BeforeEach
    void setUp(@TempDir Path bibFolder) throws IOException {
        // The folder where the files should be moved to
        defaultFileFolder = bibFolder.resolve("pdf");
        Files.createDirectory(defaultFileFolder);

        // The folder where the files are located originally
        Path fileFolder = bibFolder.resolve("files");
        Files.createDirectory(fileFolder);
        fileBefore = fileFolder.resolve("test.pdf");
        Files.createFile(fileBefore);

        MetaData metaData = new MetaData();
        metaData.setDefaultFileDirectory(defaultFileFolder.toAbsolutePath().toString());
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(), metaData);
        Files.createFile(bibFolder.resolve("test.bib"));
        databaseContext.setDatabaseFile(bibFolder.resolve("test.bib"));

        entry = new BibEntry();
        entry.setCiteKey("Toot");
        entry.setField(StandardField.TITLE, "test title");
        entry.setField(StandardField.YEAR, "1989");
        LinkedFile fileField = new LinkedFile("", fileBefore.toAbsolutePath().toString(), "");
        entry.setField(StandardField.FILE, FileFieldWriter.getStringRepresentation(fileField));

        filePreferences = mock(FilePreferences.class);
        when(filePreferences.isBibLocationAsPrimary()).thenReturn(false); //Biblocation as Primary overwrites all other dirs, therefore we set it to false here
        cleanup = new MoveFilesCleanup(databaseContext, filePreferences);
    }

    @Test
    void movesFile() throws Exception {
        when(filePreferences.getFileDirPattern()).thenReturn("");
        cleanup.cleanup(entry);

        Path fileAfter = defaultFileFolder.resolve("test.pdf");
        assertEquals(
                Optional.of(FileFieldWriter.getStringRepresentation(new LinkedFile("", "test.pdf", ""))),
                entry.getField(StandardField.FILE));
        assertFalse(Files.exists(fileBefore));
        assertTrue(Files.exists(fileAfter));
    }

    @Test
    void movesFileWithMulitpleLinked() throws Exception {
        LinkedFile fileField = new LinkedFile("", fileBefore.toAbsolutePath().toString(), "");
        entry.setField(StandardField.FILE, FileFieldWriter.getStringRepresentation(
                Arrays.asList(new LinkedFile("", "", ""), fileField, new LinkedFile("", "", ""))));

        when(filePreferences.getFileDirPattern()).thenReturn("");
        cleanup.cleanup(entry);

        Path fileAfter = defaultFileFolder.resolve("test.pdf");
        assertEquals(
                Optional.of(FileFieldWriter.getStringRepresentation(
                        Arrays.asList(new LinkedFile("", "", ""), new LinkedFile("", "test.pdf", ""), new LinkedFile("", "", "")))),
                entry.getField(StandardField.FILE));
        assertFalse(Files.exists(fileBefore));
        assertTrue(Files.exists(fileAfter));
    }

    @Test
    void movesFileWithFileDirPattern() throws Exception {
        when(filePreferences.getFileDirPattern()).thenReturn("[entrytype]");
        cleanup.cleanup(entry);

        Path fileAfter = defaultFileFolder.resolve("Misc").resolve("test.pdf");
        assertEquals(
                Optional.of(FileFieldWriter.getStringRepresentation(new LinkedFile("", "Misc/test.pdf", ""))),
                entry.getField(StandardField.FILE));
        assertFalse(Files.exists(fileBefore));
        assertTrue(Files.exists(fileAfter));
    }

    @Test
    void movesFileWithSubdirectoryPattern() throws Exception {
        when(filePreferences.getFileDirPattern()).thenReturn("[entrytype]/[year]/[auth]");
        cleanup.cleanup(entry);

        Path fileAfter = defaultFileFolder.resolve("Misc").resolve("1989").resolve("test.pdf");
        assertEquals(
                Optional.of(FileFieldWriter.getStringRepresentation(new LinkedFile("", "Misc/1989/test.pdf", ""))),
                entry.getField(StandardField.FILE));
        assertFalse(Files.exists(fileBefore));
        assertTrue(Files.exists(fileAfter));
    }
}
