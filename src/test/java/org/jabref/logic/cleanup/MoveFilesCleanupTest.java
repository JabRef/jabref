package org.jabref.logic.cleanup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.bibtex.FileFieldWriter;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.metadata.MetaData;
import org.jabref.preferences.FilePreferences;

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
    private BibDatabaseContext databaseContext;

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
        databaseContext = new BibDatabaseContext(new BibDatabase(), metaData);
        Files.createFile(bibFolder.resolve("test.bib"));
        databaseContext.setDatabasePath(bibFolder.resolve("test.bib"));

        entry = new BibEntry();
        entry.setCiteKey("Toot");
        entry.setField(StandardField.TITLE, "test title");
        entry.setField(StandardField.YEAR, "1989");
        LinkedFile fileField = new LinkedFile("", fileBefore.toAbsolutePath().toString(), "");
        entry.setField(StandardField.FILE, FileFieldWriter.getStringRepresentation(fileField));

        filePreferences = mock(FilePreferences.class);
        when(filePreferences.isBibLocationAsPrimary()).thenReturn(false); // Biblocation as Primary overwrites all other dirs, therefore we set it to false here
        cleanup = new MoveFilesCleanup(databaseContext, filePreferences);
    }

    @Test
    void movesFile() throws Exception {
        when(filePreferences.getFileDirectoryPattern()).thenReturn("");
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

        when(filePreferences.getFileDirectoryPattern()).thenReturn("");
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
        when(filePreferences.getFileDirectoryPattern()).thenReturn("[entrytype]");
        cleanup.cleanup(entry);

        Path fileAfter = defaultFileFolder.resolve("Misc").resolve("test.pdf");
        assertEquals(
                Optional.of(FileFieldWriter.getStringRepresentation(new LinkedFile("", "Misc/test.pdf", ""))),
                entry.getField(StandardField.FILE));
        assertFalse(Files.exists(fileBefore));
        assertTrue(Files.exists(fileAfter));
    }

    @Test
    void doesNotMoveFileWithEmptyFileDirPattern() throws Exception {
        when(filePreferences.getFileDirectoryPattern()).thenReturn("");
        cleanup.cleanup(entry);

        Path fileAfter = defaultFileFolder.resolve("test.pdf");
        assertEquals(
                Optional.of(FileFieldWriter.getStringRepresentation(new LinkedFile("", "test.pdf", ""))),
                entry.getField(StandardField.FILE));
        assertFalse(Files.exists(fileBefore));
        assertTrue(Files.exists(fileAfter));
    }

    @Test
    void movesFileWithSubdirectoryPattern() throws Exception {
        when(filePreferences.getFileDirectoryPattern()).thenReturn("[entrytype]/[year]/[auth]");
        cleanup.cleanup(entry);

        Path fileAfter = defaultFileFolder.resolve("Misc").resolve("1989").resolve("test.pdf");
        assertEquals(
                Optional.of(FileFieldWriter.getStringRepresentation(new LinkedFile("", "Misc/1989/test.pdf", ""))),
                entry.getField(StandardField.FILE));
        assertFalse(Files.exists(fileBefore));
        assertTrue(Files.exists(fileAfter));
    }

    @Test
    void movesFileWithNoDirectory() throws Exception {
        databaseContext.setMetaData(new MetaData());
        when(filePreferences.getFileDirectoryPattern()).thenReturn("");
        List<FieldChange> changes = cleanup.cleanup(entry);
        assertEquals(Collections.emptyList(), changes);
    }
}
