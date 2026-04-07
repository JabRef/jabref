package org.jabref.gui.desktop.os;

import java.io.IOException;
import java.util.Set;

import org.jabref.gui.externalfiletype.CustomExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.icon.IconTheme;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

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
    void openFileForPdfWithSkimConfiguredUsesConfiguredApplication() throws IOException {
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

        OSX osx = spy(new OSX());
        doNothing().when(osx).openFileWithApplication(anyString(), anyString(), anyInt());
        String filePath = "/tmp/test.pdf";

        try (MockedStatic<NativeDesktop> nativeDesktop = mockStatic(NativeDesktop.class)) {
            osx.openFile(filePath, "pdf", preferences, 73);

            nativeDesktop.verifyNoInteractions();
        }

        ArgumentCaptor<String> filePathCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> applicationCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> pageNumberCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(osx).openFileWithApplication(filePathCaptor.capture(), applicationCaptor.capture(), pageNumberCaptor.capture());
        assertEquals(filePath, filePathCaptor.getValue());
        assertEquals("Skim", applicationCaptor.getValue());
        assertEquals(73, pageNumberCaptor.getValue());
    }
}
