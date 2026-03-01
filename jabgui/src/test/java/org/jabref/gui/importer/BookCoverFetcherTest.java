package org.jabref.gui.importer;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Optional;
import java.time.Duration;
import java.net.URL;

import org.jabref.gui.importer.BookCoverFetcher;
import org.jabref.model.entry.identifier.ISBN;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.importer.FetcherClientException;
import org.jabref.model.http.SimpleHttpResponse;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;


public class BookCoverFetcherTest {

    /**
     * Test for requirement RE02
     *
     * Should create a new file with the extension
     * ".not-available" if a book cover is not available.
     * The test mocks a URLDownloader, and makes it throw
     * an appropriate exception when trying to use the method
     * "toFile". This should then cause the expected file to
     * be created.
     */
    @Test
    public void flagTest(@TempDir Path path) throws Exception{
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
