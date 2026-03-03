package org.jabref.gui.importer;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.TreeSet;

import javafx.collections.FXCollections;

import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.logic.importer.FetcherClientException;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.Directories;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.ISBN;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.http.SimpleHttpResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookCoverFetcherTest {
    private final ExternalApplicationsPreferences externalApplicationsPreferences = mock(ExternalApplicationsPreferences.class);
    private BookCoverFetcher bookCoverFetcher;
    private MockedStatic<Directories> mockedDirectories;

    @BeforeEach
    void setUp(@TempDir Path temporaryFolder) {
        mockedDirectories = Mockito.mockStatic(Directories.class);

        mockedDirectories.when(Directories::getCoverDirectory).thenReturn(temporaryFolder);

        when(externalApplicationsPreferences.getExternalFileTypes()).thenReturn(FXCollections.observableSet(new TreeSet<>(ExternalFileTypes.getDefaultExternalFileTypes())));

        bookCoverFetcher = spy(new BookCoverFetcher(externalApplicationsPreferences));
    }

    @AfterEach
    void tearDown() {
        mockedDirectories.close();
    }

    /// Test the cooldown for trying to download a bookcover
    ///
    /// Asserts that the system does not try to download
    /// a book cover again if timeSincePrevious returns
    /// a time in hours < 24
    @Test
    void checkBookCoverFetchCooldown() {
        ISBN isbn = new ISBN("123");

        doReturn(Optional.empty()).when(bookCoverFetcher).findExistingImage(any(), any());
        doNothing().when(bookCoverFetcher).downloadCoverImage(any(), any(), any());
        doReturn(Optional.of(Duration.ofHours(25))).doReturn(Optional.of(Duration.ofHours(1))).when(bookCoverFetcher).timeSincePreviousAttempt(any(), any());
        bookCoverFetcher.downloadCoverForISBN(isbn, Directories.getCoverDirectory());
        bookCoverFetcher.downloadCoverForISBN(isbn, Directories.getCoverDirectory());

        verify(bookCoverFetcher, times(1)).downloadCoverImage(any(), any(), any());
    }

    /// Test creation of a not-available file
    ///
    /// Should create a new file with the extension
    /// ".not-available" if a book cover is not available.
    /// The test mocks a URLDownloader, and makes it throw
    /// an appropriate exception when trying to use the method
    /// "toFile". This should then cause the expected file to
    /// be created.
    @Test
    void flagAsAvailableTest() throws Exception {
        String name = "testCover";
        String urlString = "https://example.com/thisisabookcoverthatdoesntexist.jpg";

        URLDownload download = mock(URLDownload.class);
        Path destination = Directories.getCoverDirectory().resolve(name + ".not-available");

        doThrow(new FetcherClientException(mock(URL.class), mock(SimpleHttpResponse.class))).when(download).toFile(any());

        assertFalse(Files.exists(destination));
        bookCoverFetcher.downloadCoverHelper(download, destination, Directories.getCoverDirectory(), name, urlString);
        assertTrue(Files.exists(destination));
    }

    /// Test retrieval of downloaded book cover when there is no such file in
    /// the directory.
    ///
    /// We create an entry of a new book cover, but we don't create a file
    /// that corresponds to that entry, neither a ".not-available" file.
    /// When we try to get the book cover, we should not get anything since
    /// there is not such file in the directory.
    @Test
    void getNoCoverWhenDirectoryIsEmpty() throws IOException {
        String isbn = "9780141036144";
        BibEntry entry = new BibEntry(StandardEntryType.Book).withField(StandardField.ISBN, isbn);

        Optional<Path> optionalPath = bookCoverFetcher.getDownloadedCoverForEntry(entry);
        assertTrue(optionalPath.isEmpty());
    }

    /// Tests retrieval of downloaded book cover
    ///
    /// We create a new book cover in the cover directory.
    /// When we try to get the book cover we should get the same path as when we created it.
    @Test
    void getAlreadyDownloadedCover() throws IOException {
        String isbn = "9780141036144";
        BibEntry entry = new BibEntry(StandardEntryType.Book).withField(StandardField.ISBN, isbn);
        String fileName = "isbn-" + isbn + ".jpg";
        Path filePath = Directories.getCoverDirectory().resolve(fileName);
        Files.createFile(filePath);

        Optional<Path> optionalPath = bookCoverFetcher.getDownloadedCoverForEntry(entry);
        assertTrue(optionalPath.isPresent());
        Path path = optionalPath.get();
        assertEquals(path, filePath);
    }

    /// Tests retrieval of downloaded book cover when there is .not-available file present
    ///
    /// We create a new .not-available file in the cover directory.
    /// When we try to get the book cover with the same isbn we should not get anything
    /// since it is not a real image.
    @Test
    void getNoCoverWhenNotAvailableFilePresent() throws IOException {
        String isbn = "9780141036144";
        BibEntry entry = new BibEntry(StandardEntryType.Book).withField(StandardField.ISBN, isbn);
        String fileName = "isbn-" + isbn + ".not-available";
        Path filePath = Directories.getCoverDirectory().resolve(fileName);
        Files.createFile(filePath);

        Optional<Path> optionalPath = bookCoverFetcher.getDownloadedCoverForEntry(entry);
        assertTrue(optionalPath.isEmpty());
    }
}
