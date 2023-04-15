package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.jabref.gui.util.FileNodeViewModel;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.FilePreferences;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static java.nio.file.DirectoryStream.Filter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UnlinkedFilesCrawlerTest {

    @Test
    public void minimalGitIgnore(@TempDir Path testRoot) throws Exception {
        Files.writeString(testRoot.resolve(".gitignore"), """
                *.png
                """);
        Path subDir = testRoot.resolve("subdir");
        Files.createDirectories(subDir);
        Files.createFile(subDir.resolve("test.png"));

        UnlinkedPDFFileFilter unlinkedPDFFileFilter = mock(UnlinkedPDFFileFilter.class);
        when(unlinkedPDFFileFilter.accept(any(Path.class))).thenReturn(true);

        UnlinkedFilesCrawler unlinkedFilesCrawler = new UnlinkedFilesCrawler(testRoot, unlinkedPDFFileFilter, DateRange.ALL_TIME, ExternalFileSorter.DEFAULT, mock(BibDatabaseContext.class), mock(FilePreferences.class));

        FileNodeViewModel fileNodeViewModel = unlinkedFilesCrawler.searchDirectory(testRoot, unlinkedPDFFileFilter);

        assertEquals(new FileNodeViewModel(testRoot), fileNodeViewModel);
    }

    @Test
    public void excludingTheCurrentLibraryTest(@TempDir Path testRoot) throws IOException {
        // Adding 3 files one of which is the database file
        Files.createFile(testRoot.resolve("unlinkedPdf.pdf"));
        Files.createFile(testRoot.resolve("another-unlinkedPdf.pdf"));
        Path databasePath = testRoot.resolve("test.bib");
        Files.createFile(databasePath);

        BibDatabaseContext databaseContext = new BibDatabaseContext();
        databaseContext.setDatabasePath(databasePath);

        FilePreferences filePreferences = mock(FilePreferences.class);
        Filter<Path> fileExtensionFilter = new FileExtensionViewModel(StandardFileType.ANY_FILE, filePreferences).dirFilter();
        UnlinkedPDFFileFilter unlinkedPdfFileFilter = new UnlinkedPDFFileFilter(fileExtensionFilter, databaseContext, filePreferences);

        UnlinkedFilesCrawler unlinkedFilesCrawler = new UnlinkedFilesCrawler(testRoot, unlinkedPdfFileFilter, DateRange.ALL_TIME, ExternalFileSorter.DEFAULT, databaseContext, filePreferences);
        FileNodeViewModel fileNodeViewModel = unlinkedFilesCrawler.searchDirectory(testRoot, unlinkedPdfFileFilter);

        // checking to see if the database file has been filtered
        try (Stream<Path> filesInitially = Files.list(testRoot)) {
            int count = (int) filesInitially.count();
            assertEquals(fileNodeViewModel.getFileCount(), count - 1);
        }
    }
}
