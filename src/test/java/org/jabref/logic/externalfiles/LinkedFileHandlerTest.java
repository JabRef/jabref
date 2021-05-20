package org.jabref.logic.externalfiles;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.metadata.MetaData;
import org.jabref.preferences.FilePreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LinkedFileHandlerTest {
    private LinkedFileHandler linkedFileHandler;
    private FilePreferences filePreferences;

    @BeforeEach
    void setUp(@TempDir Path testFolder) throws IOException {
        Path bibPath = testFolder.resolve("bib_test.bib");
        Path pdfPath = testFolder.resolve("pdf_test.pdf");
        Files.createFile(bibPath);
        Files.createFile(pdfPath);
        FileWriter fw = new FileWriter(bibPath.toFile());
        fw.write("% Encoding: UTF-8\n" +
                "\n" +
                "@Article{,\n" +
                "  file = {:" + pdfPath.toAbsolutePath() + "},\n" +
                "}\n" +
                "\n" +
                "@Comment{jabref-meta: databaseType:bibtex;}\n");
        fw.close();
        MetaData metaData = new MetaData();
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(), metaData);
        context.setDatabasePath(bibPath);
        LinkedFile fileEntry = new LinkedFile("", Path.of("pdf_test.pdf"), "PDF");
        BibEntry entry = new BibEntry();
        filePreferences = mock(FilePreferences.class);
        when(filePreferences.shouldStoreFilesRelativeToBib()).thenReturn(true);
        linkedFileHandler = new LinkedFileHandler(
                fileEntry, entry, context, filePreferences
        );
    }

    @Test
    void moveToDefaultDirectoryWithMainFileDirectory(@TempDir Path testFolder) throws IOException {
        when(filePreferences.getFileDirectoryPattern()).thenReturn("papers");
        when(filePreferences.getFileDirectory()).thenReturn(Optional.of(testFolder.resolve("main")));
        linkedFileHandler.moveToDefaultDirectory();
        File file = testFolder.resolve("main/papers/pdf_test.pdf").toFile();
        assert file.exists();
    }

    @Test
    void moveToDefaultDirectoryWithoutMainFileDirectory(@TempDir Path testFolder) throws IOException {
        when(filePreferences.getFileDirectoryPattern()).thenReturn("");
        when(filePreferences.getFileDirectory()).thenReturn(Optional.empty());
        linkedFileHandler.moveToDefaultDirectory();
        File file = testFolder.resolve("pdf_test.pdf").toFile();
        assert file.exists();
    }
}
