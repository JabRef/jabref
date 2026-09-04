package org.jabref.gui.desktop.os;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.externalfiletype.CustomExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.icon.IconTheme;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// The "browser" in these tests is a shell script recording its argument, so the assertions catch
/// any URL mangling on the way to the external application (the regression fixed here: routing
/// online links through `Path.of`, which collapses `https://` and truncates query strings).
@NullMarked
@DisabledOnOs(OS.WINDOWS)
class NativeDesktopTest {

    private static final String URL_WITH_QUERY = "https://journals.plos.org/plosmedicine/article/file?id=10.1371/journal.pmed.1004085&type=printable";

    @TempDir Path tempDir;

    private Path recorder;
    private Path recorded;

    @BeforeEach
    void setUp() throws IOException {
        recorded = tempDir.resolve("recorded.txt");
        recorder = tempDir.resolve("recorder.sh");
        Files.writeString(recorder, "#!/bin/sh\nprintf '%s' \"$1\" > " + recorded + "\n");
        Files.setPosixFilePermissions(recorder, EnumSet.allOf(PosixFilePermission.class));
    }

    private String recordedArgument() throws IOException, InterruptedException {
        for (int i = 0; i < 100; i++) {
            if (Files.exists(recorded) && !Files.readString(recorded).isEmpty()) {
                return Files.readString(recorded);
            }
            Thread.sleep(50);
        }
        return "";
    }

    @Test
    void openBrowserPassesFullUrlToCustomBrowser() throws IOException, InterruptedException {
        ExternalFileType htmlType = new CustomExternalFileType("URL", "html", "text/html", recorder.toString(), "www", IconTheme.JabRefIcons.WWW);
        ExternalApplicationsPreferences preferences = mock(ExternalApplicationsPreferences.class);
        when(preferences.getExternalFileTypes()).thenReturn(FXCollections.observableSet(htmlType));

        NativeDesktop.openBrowser(URL_WITH_QUERY, preferences);

        assertEquals(URL_WITH_QUERY, recordedArgument());
    }

    @Test
    void windowsOpenFileWithApplicationKeepsUrlIntact() throws IOException, InterruptedException {
        // The Windows implementation is executable on POSIX, which is enough to pin down that the
        // URL is passed through verbatim instead of being run through Path.of
        new Windows().openFileWithApplication(URL_WITH_QUERY, recorder.toString());

        assertEquals(URL_WITH_QUERY, recordedArgument());
    }

    @Test
    void openBrowserPassesFullUrlToDesktopBrowse() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        FakeDesktop desktop = new FakeDesktop(true, false, false);

        NativeDesktop.openBrowser(URL_WITH_QUERY, noCustomBrowser(), FakeDesktop.NO_FAILURE_EXPECTED, desktop);

        assertEquals(URL_WITH_QUERY, desktop.browsed.get(5, TimeUnit.SECONDS));
    }

    @Test
    void openBrowserFallsBackToSystemHandlerWhenBrowseFails() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        FakeDesktop desktop = new FakeDesktop(true, true, false);

        NativeDesktop.openBrowser(URL_WITH_QUERY, noCustomBrowser(), FakeDesktop.NO_FAILURE_EXPECTED, desktop);

        assertEquals(URL_WITH_QUERY, desktop.systemHandled.get(5, TimeUnit.SECONDS));
    }

    @Test
    void openBrowserReportsAsyncFailureWhenAllMechanismsFail() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        FakeDesktop desktop = new FakeDesktop(true, true, true);
        CompletableFuture<IOException> failure = new CompletableFuture<>();

        NativeDesktop.openBrowser(URL_WITH_QUERY, noCustomBrowser(), failure::complete, desktop);

        assertEquals("system handler failed", failure.get(5, TimeUnit.SECONDS).getMessage());
    }

    @Test
    void openBrowserUsesSystemHandlerWhenBrowseUnsupported() throws IOException {
        FakeDesktop desktop = new FakeDesktop(false, false, false);

        NativeDesktop.openBrowser(URL_WITH_QUERY, noCustomBrowser(), FakeDesktop.NO_FAILURE_EXPECTED, desktop);

        assertEquals(URL_WITH_QUERY, desktop.systemHandled.getNow(""));
    }

    @Test
    void openBrowserUsesSystemHandlerForUnparseableUrl() throws IOException {
        FakeDesktop desktop = new FakeDesktop(true, false, false);
        String urlWithSpace = "https://example.org/some path?x=1&y=2";

        NativeDesktop.openBrowser(urlWithSpace, noCustomBrowser(), FakeDesktop.NO_FAILURE_EXPECTED, desktop);

        assertEquals(urlWithSpace, desktop.systemHandled.getNow(""));
    }

    private static ExternalApplicationsPreferences noCustomBrowser() {
        ExternalApplicationsPreferences preferences = mock(ExternalApplicationsPreferences.class);
        when(preferences.getExternalFileTypes()).thenReturn(FXCollections.observableSet());
        return preferences;
    }

    private static final class FakeDesktop extends NativeDesktop {
        static final Consumer<IOException> NO_FAILURE_EXPECTED = e -> {
            throw new AssertionError("Unexpected async failure", e);
        };

        final CompletableFuture<String> browsed = new CompletableFuture<>();
        final CompletableFuture<String> systemHandled = new CompletableFuture<>();

        private final boolean browseSupported;
        private final boolean browseFails;
        private final boolean systemHandlerFails;

        private FakeDesktop(boolean browseSupported, boolean browseFails, boolean systemHandlerFails) {
            this.browseSupported = browseSupported;
            this.browseFails = browseFails;
            this.systemHandlerFails = systemHandlerFails;
        }

        @Override
        boolean supportsDesktopBrowse() {
            return browseSupported;
        }

        @Override
        void desktopBrowse(URI uri) throws IOException {
            if (browseFails) {
                throw new IOException("browse failed");
            }
            browsed.complete(uri.toASCIIString());
        }

        @Override
        public void openUrlWithSystemHandler(String url) throws IOException {
            if (systemHandlerFails) {
                throw new IOException("system handler failed");
            }
            systemHandled.complete(url);
        }

        @Override
        public void openFile(String filePath, String fileType, ExternalApplicationsPreferences externalApplicationsPreferences) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void openFileWithApplication(String filePath, String application) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void openFolderAndSelectFile(Path file) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void openConsole(String absolutePath, DialogService dialogService) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Path getApplicationDirectory() {
            throw new UnsupportedOperationException();
        }
    }
}
