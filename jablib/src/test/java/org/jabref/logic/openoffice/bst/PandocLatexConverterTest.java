package org.jabref.logic.openoffice.bst;

import java.util.List;

import org.jabref.logic.os.OS;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PandocLatexConverterTest {

    @Test
    void windowsAutoDetectCandidatesIncludeProgramFilesPandoc() {
        try (MockedStatic<OS> os = Mockito.mockStatic(OS.class, Mockito.CALLS_REAL_METHODS)) {
            os.when(() -> OS.detectProgramPath("pandoc", "Pandoc"))
              .thenReturn("C:\\Program Files\\Pandoc\\pandoc.exe");

            List<String> candidates = PandocLatexConverter.getAutoDetectCandidates(true, false);

            assertEquals(List.of("pandoc", "C:\\Program Files\\Pandoc\\pandoc.exe"), candidates);
        }
    }

    @Test
    void windowsAutoDetectCandidatesOmitBlankDetectedPath() {
        try (MockedStatic<OS> os = Mockito.mockStatic(OS.class, Mockito.CALLS_REAL_METHODS)) {
            os.when(() -> OS.detectProgramPath("pandoc", "Pandoc"))
              .thenReturn("");

            List<String> candidates = PandocLatexConverter.getAutoDetectCandidates(true, false);

            assertEquals(List.of("pandoc"), candidates);
        }
    }

    @Test
    void macAutoDetectCandidatesUseMacDefaults() {
        List<String> candidates = PandocLatexConverter.getAutoDetectCandidates(false, true);

        assertEquals(List.of("pandoc", "/usr/local/bin/pandoc", "/opt/homebrew/bin/pandoc"), candidates);
    }

    @Test
    void linuxAutoDetectCandidatesUseLinuxDefaults() {
        List<String> candidates = PandocLatexConverter.getAutoDetectCandidates(false, false);

        assertEquals(List.of("pandoc", "/usr/bin/pandoc", "/usr/local/bin/pandoc"), candidates);
    }
}
