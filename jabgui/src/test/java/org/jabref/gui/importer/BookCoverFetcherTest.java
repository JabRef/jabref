package org.jabref.gui.importer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.TreeSet;

import javafx.collections.FXCollections;

import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.util.Directories;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BookCoverFetcherTest {
    private final ExternalApplicationsPreferences externalApplicationsPreferences = mock(ExternalApplicationsPreferences.class);
    private final BookCoverFetcher bookCoverFetcher = new BookCoverFetcher(externalApplicationsPreferences);
    private MockedStatic<Directories> mockedDirectories;
    // A valid isbn that has a downloadable cover
    private final String isbn = "9780141036144";
    private final BibEntry entry = new BibEntry(StandardEntryType.Book).withField(StandardField.ISBN, isbn);
    private Path coverPath;
    private Path notAvailablePath;
    // A valid isbn that does not have a downloadable cover
    private final String badIsbn = "1234567881";
    private final BibEntry badEntry = new BibEntry(StandardEntryType.Book).withField(StandardField.ISBN, badIsbn);
    private Path badCoverPath;
    private Path badNotAvailablePath;

    @BeforeEach
    void setUp(@TempDir Path temporaryFolder) throws MalformedURLException {
        mockedDirectories = Mockito.mockStatic(Directories.class);
        mockedDirectories.when(Directories::getCoverDirectory).thenReturn(temporaryFolder);
        coverPath = Directories.getCoverDirectory().resolve("isbn-" + isbn + ".jpg");
        notAvailablePath = Directories.getCoverDirectory().resolve("isbn-" + isbn + ".not-available");
        badCoverPath = Directories.getCoverDirectory().resolve("isbn-" + badIsbn + ".jpg");
        badNotAvailablePath = Directories.getCoverDirectory().resolve("isbn-" + badIsbn + ".not-available");

        when(externalApplicationsPreferences.getExternalFileTypes()).thenReturn(FXCollections.observableSet(new TreeSet<>(ExternalFileTypes.getDefaultExternalFileTypes())));
    }

    @AfterEach
    void tearDown() {
        mockedDirectories.close();
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
    void createNotAvailableFileAfterFailedDownload() throws Exception {
        bookCoverFetcher.downloadCoversForEntry(badEntry);

        assertTrue(Files.notExists(badCoverPath));
        assertTrue(Files.exists(badNotAvailablePath));
    }

    /// Test retrieval of downloaded book cover when there is no such file in
    /// the directory.
    ///
    /// We create an entry of a new book cover, but we don't create a file
    /// that corresponds to that entry, neither a ".not-available" file.
    /// When we try to get the book cover, we should not get anything since
    /// there is not such file in the directory.
    @Test
    void getNoCoverWhenDirectoryIsEmpty() {
        Optional<Path> optionalPath = bookCoverFetcher.getDownloadedCoverForEntry(entry);
        assertTrue(optionalPath.isEmpty());
    }

    /// Tests retrieval of downloaded book cover
    ///
    /// We create a new book cover in the cover directory.
    /// When we try to get the book cover we should get the same path as when we created it.
    @Test
    void getAlreadyDownloadedCover() throws IOException {
        Files.createFile(coverPath);

        Optional<Path> optionalPath = bookCoverFetcher.getDownloadedCoverForEntry(entry);

        assertTrue(optionalPath.isPresent());
        Path path = optionalPath.get();
        assertEquals(path, coverPath);
    }

    /// Tests retrieval of downloaded book cover when there is .not-available file present
    ///
    /// We create a new .not-available file in the cover directory.
    /// When we try to get the book cover with the same isbn we should not get anything
    /// since it is not a real image.
    @Test
    void getNoCoverWhenNotAvailableFilePresent() throws IOException {
        Files.createFile(notAvailablePath);

        Optional<Path> optionalPath = bookCoverFetcher.getDownloadedCoverForEntry(entry);
        assertTrue(optionalPath.isEmpty());
    }

    /// Tests the update of modification time when it is more than 24 hours ago
    ///
    /// We create a new .not-available file in the cover directory with a modification time more than 24 hours ago
    /// When we try to download the book and fail to do so, the modification time should be set to now.
    @Test
    void modificationTimeChangesWhenMoreThan24Hours() throws IOException, FetcherException {
        Instant now = Instant.now();
        Files.createFile(badNotAvailablePath);
        // Set the last modification time of the file to be 25 hours ago
        Files.setLastModifiedTime(badNotAvailablePath, FileTime.from(now.minus(25, ChronoUnit.HOURS)));

        bookCoverFetcher.downloadCoversForEntry(badEntry);

        assertTrue(Files.notExists(badCoverPath));
        assertTrue(Files.exists(badNotAvailablePath));
        Instant fileTimeAfterAttempt = Files.getLastModifiedTime(badNotAvailablePath).toInstant();
        // We pad with a few second since the file system might not be precise
        assertTrue(now.minusSeconds(10).isBefore(fileTimeAfterAttempt));
    }

    /// Tests the update of modification time when it is less than 24 hours ago
    ///
    /// We create a new .not-available file in the cover directory with a modification time less than 24 hours ago
    /// When we try to download the book, the modification time should not change.
    @Test
    void modificationTimeDoesNotChangeWhenLessThan24Hours() throws IOException, FetcherException {
        Files.createFile(badNotAvailablePath);
        // Set the last modification time of the file to be 23 hours ago
        Files.setLastModifiedTime(badNotAvailablePath, FileTime.from(Instant.now().minus(23, ChronoUnit.HOURS)));
        Instant fileTimeBeforeAttempt = Files.getLastModifiedTime(badNotAvailablePath).toInstant();

        bookCoverFetcher.downloadCoversForEntry(badEntry);

        assertTrue(Files.notExists(badCoverPath));
        Instant fileTimeAfterAttempt = Files.getLastModifiedTime(badNotAvailablePath).toInstant();
        assertEquals(fileTimeAfterAttempt, fileTimeBeforeAttempt);
    }

    /// Tests the deletion of .not-available file after a successful download.
    ///
    /// We create a new .not-available file in the cover directory with a modification time more than 24 hours ago
    /// When we try to download the book and succeed, the file should be deleted.
    @Test
    void notAvailableFileIsDeletedAfterSuccessfulDownload() throws IOException, FetcherException {
        Files.createFile(notAvailablePath);
        Files.setLastModifiedTime(notAvailablePath, FileTime.from(Instant.now().minus(25, ChronoUnit.HOURS)));

        bookCoverFetcher.downloadCoversForEntry(entry);

        assertTrue(Files.exists(coverPath));
        assertTrue(Files.notExists(notAvailablePath));
    }
}
