package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Stream;

import javafx.collections.FXCollections;

import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.util.FileNodeViewModel;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.externalfiles.DateRange;
import org.jabref.logic.externalfiles.ExternalFileSorter;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.io.AutoLinkPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static java.nio.file.DirectoryStream.Filter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UnlinkedFilesCrawlerTest {

    @Test
    void ignoresFilesInNamedSubdirectoryAccordingToGitignore(@TempDir Path testRoot) throws IOException {
        // This mirrors GitIgnoreFileFilterTest::checkDirectoryGitIgnoreSubDir but tests via the crawler
        Files.writeString(testRoot.resolve(".gitignore"), """
                ignore/.*
                ignore/*
                ignore/// *
                """);
        Path subDir = testRoot.resolve("ignore");
        Files.createDirectories(subDir);
        Files.createFile(subDir.resolve("test.png"));
        // also create a deeper subdirectory with a file which must be ignored as well
        Path deepSubDir = subDir.resolve("nested");
        Files.createDirectories(deepSubDir);
        Files.createFile(deepSubDir.resolve("deep.png"));

        // Allow all files at the PDF filter level so only the gitignore filter applies
        UnlinkedPDFFileFilter unlinkedPDFFileFilter = mock(UnlinkedPDFFileFilter.class);
        when(unlinkedPDFFileFilter.accept(any(Path.class))).thenReturn(true);

        UnlinkedFilesCrawler unlinkedFilesCrawler = newCrawler(testRoot, unlinkedPDFFileFilter, mock(BibDatabaseContext.class), mock(FilePreferences.class));

        FileNodeViewModel fileNodeViewModel = unlinkedFilesCrawler.searchDirectory(testRoot, unlinkedPDFFileFilter);

        // The ignored files must not appear in the results; thus the root node has no children
        assertEquals(new FileNodeViewModel(testRoot), fileNodeViewModel);
    }

    @Test
    void minimalGitIgnore(@TempDir Path testRoot) throws IOException {
        Files.writeString(testRoot.resolve(".gitignore"), """
                *.png
                """);
        Path subDir = testRoot.resolve("subdir");
        Files.createDirectories(subDir);
        Files.createFile(subDir.resolve("test.png"));

        UnlinkedPDFFileFilter unlinkedPDFFileFilter = mock(UnlinkedPDFFileFilter.class);
        when(unlinkedPDFFileFilter.accept(any(Path.class))).thenReturn(true);

        UnlinkedFilesCrawler unlinkedFilesCrawler = newCrawler(testRoot, unlinkedPDFFileFilter, mock(BibDatabaseContext.class), mock(FilePreferences.class));

        FileNodeViewModel fileNodeViewModel = unlinkedFilesCrawler.searchDirectory(testRoot, unlinkedPDFFileFilter);

        assertEquals(new FileNodeViewModel(testRoot), fileNodeViewModel);
    }

    @Test
    void excludingTheCurrentLibraryTest(@TempDir Path testRoot) throws IOException {
        // Adding 3 files one of which is the database file
        Files.createFile(testRoot.resolve("unlinkedPdf.pdf"));
        Files.createFile(testRoot.resolve("another-unlinkedPdf.pdf"));
        Path databasePath = testRoot.resolve("test.bib");
        Files.createFile(databasePath);

        BibDatabaseContext databaseContext = new BibDatabaseContext();
        databaseContext.setDatabasePath(databasePath);

        ExternalApplicationsPreferences externalApplicationsPreferences = mock(ExternalApplicationsPreferences.class);
        Filter<Path> fileExtensionFilter = new FileExtensionViewModel(StandardFileType.ANY_FILE, externalApplicationsPreferences).dirFilter();
        FilePreferences filePreferences = mock(FilePreferences.class);
        UnlinkedPDFFileFilter unlinkedPdfFileFilter = new UnlinkedPDFFileFilter(fileExtensionFilter, databaseContext, filePreferences);

        UnlinkedFilesCrawler unlinkedFilesCrawler = newCrawler(testRoot, unlinkedPdfFileFilter, databaseContext, filePreferences);
        FileNodeViewModel fileNodeViewModel = unlinkedFilesCrawler.searchDirectory(testRoot, unlinkedPdfFileFilter);

        // checking to see if the database file has been filtered
        try (Stream<Path> filesInitially = Files.list(testRoot)) {
            int count = (int) filesInitially.count();
            assertEquals(fileNodeViewModel.getFileCount(), count - 1);
        }
    }

    /// [utest->req~jabgui.externalfiles.unlinked-files.search.non-blocking-results~1]
    @Test
    void cachesRelatedEntriesForUnlinkedFiles(@TempDir Path testRoot) throws IOException {
        Path file = testRoot.resolve("citeKey.pdf");
        Files.createFile(file);

        BibEntry entry = new BibEntry(StandardEntryType.Article).withCitationKey("citeKey");
        BibDatabase database = new BibDatabase();
        database.insertEntry(entry);
        BibDatabaseContext databaseContext = mock(BibDatabaseContext.class);

        FilePreferences filePreferences = mock(FilePreferences.class);
        when(databaseContext.getDatabase()).thenReturn(database);
        when(databaseContext.getFileDirectories(filePreferences)).thenReturn(List.of(testRoot));

        UnlinkedFilesSearchResult result = newCrawler(
                testRoot,
                path -> true,
                databaseContext,
                filePreferences).call();

        assertEquals(List.of(entry), result.relatedEntriesByFile().get(file));
    }

    private static UnlinkedFilesCrawler newCrawler(Path directory,
                                                    Filter<Path> fileFilter,
                                                    BibDatabaseContext databaseContext,
                                                    FilePreferences filePreferences) {
        return new UnlinkedFilesCrawler(
                directory,
                fileFilter,
                DateRange.ALL_TIME,
                ExternalFileSorter.DEFAULT,
                databaseContext,
                filePreferences,
                externalApplicationsPreferences(),
                autoLinkPreferences());
    }

    private static ExternalApplicationsPreferences externalApplicationsPreferences() {
        ExternalApplicationsPreferences externalApplicationsPreferences = mock(ExternalApplicationsPreferences.class);
        when(externalApplicationsPreferences.getExternalFileTypes())
                .thenReturn(FXCollections.observableSet(new TreeSet<>(ExternalFileTypes.getDefaultExternalFileTypes())));
        return externalApplicationsPreferences;
    }

    private static AutoLinkPreferences autoLinkPreferences() {
        AutoLinkPreferences autoLinkPreferences = mock(AutoLinkPreferences.class);
        when(autoLinkPreferences.getCitationKeyDependency()).thenReturn(AutoLinkPreferences.CitationKeyDependency.START);
        return autoLinkPreferences;
    }
}
