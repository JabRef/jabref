package org.jabref.gui.importer;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.logic.importer.FetcherClientException;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.identifier.ISBN;
import org.jabref.model.http.SimpleHttpResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

public class BookCoverFetcherTest {

    /**
     * Test the cooldown for trying to download a bookcover
     * <p>
     * Asserts that the system does not try to download
     * a book cover again if timeSincePrevious returns
     * a time in hours < 24
     */
    @Test
    public void checkBookCoverFetchCooldown(@TempDir Path path) {
        ExternalApplicationsPreferences preferences = mock(ExternalApplicationsPreferences.class);
        BookCoverFetcher fetcher = spy(new BookCoverFetcher(preferences));

        ISBN isbn = new ISBN("123");

        doReturn(Optional.empty()).when(fetcher).findExistingImage(any(), any());
        doNothing().when(fetcher).downloadCoverImage(any(), any(), any());
        doReturn(Optional.of(Duration.ofHours(25))).doReturn(Optional.of(Duration.ofHours(1))).when(fetcher).timeSincePreviousAttempt(any(), any());
        fetcher.downloadCoverForISBN(isbn, path);
        fetcher.downloadCoverForISBN(isbn, path);

        verify(fetcher, times(1)).downloadCoverImage(any(), any(), any());
    }

    /**
     * Test creation of a not-available file
     * <p>
     * Should create a new file with the extension
     * ".not-available" if a book cover is not available.
     * The test mocks a URLDownloader, and makes it throw
     * an appropriate exception when trying to use the method
     * "toFile". This should then cause the expected file to
     * be created.
     */
    @Test
    public void flagAsAvailableTest(@TempDir Path path) throws Exception {
        ExternalApplicationsPreferences preferences = mock(ExternalApplicationsPreferences.class);
        BookCoverFetcher fetcher = spy(new BookCoverFetcher(preferences));
        String name = "testCover";
        String urlString = "https://example.com/thisisabookcoverthatdoesntexist.jpg";

        URLDownload download = mock(URLDownload.class);
        Path destination = path.resolve(name + ".not-available");

        doThrow(new FetcherClientException(mock(URL.class), mock(SimpleHttpResponse.class))).when(download).toFile(any());

        assertFalse(Files.exists(destination));
        fetcher.downloadCoverHelper(download, destination, path, name, urlString);
        assertTrue(Files.exists(destination));
    }
}
