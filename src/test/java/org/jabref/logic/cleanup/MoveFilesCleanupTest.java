package org.jabref.logic.cleanup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

import org.jabref.model.Defaults;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FileFieldWriter;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.metadata.FileDirectoryPreferences;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.TempDirectory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(TempDirectory.class)
class MoveFilesCleanupTest {

    private File pdfFolder;
    private BibDatabaseContext databaseContext;
    private MoveFilesCleanup cleanup;
    private BibEntry entry;
    private FileDirectoryPreferences fileDirPrefs;

    @BeforeEach
    void setUp(@TempDirectory.TempDir Path bibFolder) throws IOException {
        MetaData metaData = new MetaData();
        Path path = bibFolder.resolve("ARandomlyNamedFolder");
        Files.createDirectory(path);
        pdfFolder = path.toFile();
        metaData.setDefaultFileDirectory(pdfFolder.getAbsolutePath());
        databaseContext = new BibDatabaseContext(new BibDatabase(), metaData, new Defaults());
        Files.createFile(path.resolve("test.bib"));
        databaseContext.setDatabaseFile(path.resolve("test.bib").toFile());
        entry = new BibEntry();
        entry.setCiteKey("Toot");
        entry.setField("title", "test title");

        fileDirPrefs = mock(FileDirectoryPreferences.class);
        when(fileDirPrefs.isBibLocationAsPrimary()).thenReturn(false); //Biblocation as Primary overwrites all other dirs, therefore we set it to false here
    }

    @Test
    void movesFileFromSubfolder(@TempDirectory.TempDir Path bibFolder) throws IOException {
        Path path = bibFolder.resolve("AnotherRandomlyNamedFolder");
        Files.createDirectory(path);
        File fileBefore = path.resolve("test.pdf").toFile();
        assertTrue(fileBefore.createNewFile());
        assertTrue(fileBefore.exists());

        LinkedFile fileField = new LinkedFile("", fileBefore.getAbsolutePath(), "");
        entry.setField("file", FileFieldWriter.getStringRepresentation(fileField));
        cleanup = new MoveFilesCleanup(databaseContext, "", fileDirPrefs);

        cleanup.cleanup(entry);

        assertFalse(fileBefore.exists());
        File fileAfter = pdfFolder.toPath().resolve("test.pdf").toFile();
        assertTrue(fileAfter.exists());

        assertEquals(Optional.of(FileFieldWriter.getStringRepresentation(new LinkedFile("", fileAfter.getName(), ""))),
                entry.getField("file"));
    }

    @Test
    void movesFileFromSubfolderMultiple(@TempDirectory.TempDir Path bibFolder) throws IOException {
        Path path = bibFolder.resolve("AnotherRandomlyNamedFolder");
        Files.createDirectory(path);
        File fileBefore = path.resolve("test.pdf").toFile();
        assertTrue(fileBefore.createNewFile());
        assertTrue(fileBefore.exists());

        LinkedFile fileField = new LinkedFile("", fileBefore.getAbsolutePath(), "");
        entry.setField("file", FileFieldWriter.getStringRepresentation(
                Arrays.asList(new LinkedFile("", "", ""), fileField, new LinkedFile("", "", ""))));

        cleanup = new MoveFilesCleanup(databaseContext, "", fileDirPrefs);
        cleanup.cleanup(entry);

        assertFalse(fileBefore.exists());
        File fileAfter = pdfFolder.toPath().resolve("test.pdf").toFile();
        assertTrue(fileAfter.exists());

        assertEquals(
                Optional.of(FileFieldWriter.getStringRepresentation(new LinkedFile("", fileAfter.getName(), ""))),
                entry.getField("file"));
    }

    @Test
    void movesFileFromSubfolderWithFileDirPattern(@TempDirectory.TempDir Path bibFolder) throws IOException {
        Path path = bibFolder.resolve("AnotherRandomlyNamedFolder");
        Files.createDirectory(path);
        File fileBefore = path.resolve("test.pdf").toFile();

        assertTrue(fileBefore.createNewFile());
        assertTrue(fileBefore.exists());

        LinkedFile fileField = new LinkedFile("", fileBefore.getAbsolutePath(), "");
        entry.setField("file", FileFieldWriter.getStringRepresentation(fileField));

        cleanup = new MoveFilesCleanup(databaseContext, "[entrytype]", fileDirPrefs);

        cleanup.cleanup(entry);

        assertFalse(fileBefore.exists());
        Path after = pdfFolder.toPath().resolve("Misc").resolve("test.pdf");
        Path relativefileDir = pdfFolder.toPath().relativize(after);
        assertTrue(Files.exists(after));

        assertEquals(Optional
                        .of(FileFieldWriter.getStringRepresentation(new LinkedFile("", relativefileDir.toString(), ""))),
                entry.getField("file"));
    }

    @Test
    void movesFileFromSubfolderWithSubdirPattern(@TempDirectory.TempDir Path bibFolder) throws IOException {
        BibEntry local_entry = (BibEntry) entry.clone();
        local_entry.setField("year", "1989");
        Path path = bibFolder.resolve("AnotherRandomlyNamedFolder");
        Files.createDirectory(path);
        File fileBefore = path.resolve("test.pdf").toFile();

        assertTrue(fileBefore.createNewFile());
        assertTrue(fileBefore.exists());

        LinkedFile fileField = new LinkedFile("", fileBefore.getAbsolutePath(), "");
        local_entry.setField("file", FileFieldWriter.getStringRepresentation(fileField));

        cleanup = new MoveFilesCleanup(databaseContext, "[year]", fileDirPrefs);
        cleanup.cleanup(local_entry);

        assertFalse(fileBefore.exists());
        Path after = pdfFolder.toPath().resolve("1989").resolve("test.pdf");
        Path relativefileDir = pdfFolder.toPath().relativize(after);
        assertTrue(Files.exists(after));

        assertEquals(Optional
                        .of(FileFieldWriter.getStringRepresentation(new LinkedFile("", relativefileDir.toString(), ""))),
                local_entry.getField("file"));
    }

    @Test
    void movesFileFromSubfolderWithDeepSubdirPattern(@TempDirectory.TempDir Path bibFolder) throws IOException {
        BibEntry local_entry = (BibEntry) entry.clone();
        local_entry.setField("year", "1989");
        local_entry.setField("author", "O. Kitsune");
        Path path = bibFolder.resolve("AnotherRandomlyNamedFolder");
        Files.createDirectory(path);
        File fileBefore = path.resolve("test.pdf").toFile();

        assertTrue(fileBefore.createNewFile());
        assertTrue(fileBefore.exists());

        LinkedFile fileField = new LinkedFile("", fileBefore.getAbsolutePath(), "");
        local_entry.setField("file", FileFieldWriter.getStringRepresentation(fileField));

        cleanup = new MoveFilesCleanup(databaseContext, "[entrytype]/[year]/[auth]", fileDirPrefs);

        cleanup.cleanup(local_entry);

        assertFalse(fileBefore.exists());
        Path after = pdfFolder.toPath().resolve("Misc").resolve("1989").resolve("Kitsune").resolve("test.pdf");
        Path relativefileDir = pdfFolder.toPath().relativize(after);
        assertTrue(Files.exists(after));

        assertEquals(Optional
                        .of(FileFieldWriter.getStringRepresentation(new LinkedFile("", relativefileDir.toString(), ""))),
                local_entry.getField("file"));
    }
}
