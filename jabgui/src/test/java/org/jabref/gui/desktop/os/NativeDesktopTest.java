package org.jabref.gui.desktop.os;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;

import javafx.collections.FXCollections;

import org.jabref.gui.externalfiletype.CustomExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.icon.IconTheme;

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
}
