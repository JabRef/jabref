package org.jabref.gui.desktop.os;

import java.io.IOException;
import java.util.Set;

import org.jabref.gui.externalfiletype.CustomExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.icon.IconTheme;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

class OSXTest {

    @Test
    void openFileForPdfWithPageUsesBrowserHashJump() throws IOException {
        ExternalFileType pdfType = new CustomExternalFileType("PDF", "pdf", "application/pdf", "", "", IconTheme.JabRefIcons.FILE);
        ExternalApplicationsPreferences preferences = new ExternalApplicationsPreferences(
                "References",
                false,
                Set.of(pdfType),
                false,
                "",
                false,
                "",
                "");

        String filePath = "/tmp/test.pdf";
        String expectedUrl = "file:///tmp/test.pdf#page=73";

        try (MockedStatic<NativeDesktop> nativeDesktop = mockStatic(NativeDesktop.class)) {
            nativeDesktop.when(() -> NativeDesktop.openBrowser(anyString(), any(ExternalApplicationsPreferences.class)))
                         .thenAnswer(_ -> null);

            new OSX().openFile(filePath, "pdf", preferences, 73);

            nativeDesktop.verify(() -> NativeDesktop.openBrowser(eq(expectedUrl), eq(preferences)));
        }
    }

    @Test
    void openFileForPdfWithSkimConfiguredStillUsesBrowserHashJump() throws IOException {
        ExternalFileType pdfType = new CustomExternalFileType("PDF", "pdf", "application/pdf", "Skim", "", IconTheme.JabRefIcons.FILE);
        ExternalApplicationsPreferences preferences = new ExternalApplicationsPreferences(
                "References",
                false,
                Set.of(pdfType),
                false,
                "",
                false,
                "",
                "");

        String filePath = "/tmp/test.pdf";
        String expectedUrl = "file:///tmp/test.pdf#page=73";

        try (MockedStatic<NativeDesktop> nativeDesktop = mockStatic(NativeDesktop.class)) {
            nativeDesktop.when(() -> NativeDesktop.openBrowser(anyString(), any(ExternalApplicationsPreferences.class)))
                         .thenAnswer(_ -> null);

            new OSX().openFile(filePath, "pdf", preferences, 73);

            nativeDesktop.verify(() -> NativeDesktop.openBrowser(eq(expectedUrl), eq(preferences)));
        }
    }
}
