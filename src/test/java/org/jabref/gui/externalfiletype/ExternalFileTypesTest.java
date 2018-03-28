package org.jabref.gui.externalfiletype;

import java.util.Optional;

import org.jabref.Globals;
import org.jabref.preferences.JabRefPreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExternalFileTypesTest {
    private ExternalFileTypes types;

    @BeforeEach
    public void init() {
        Globals.prefs = Mockito.mock(JabRefPreferences.class);
        types = ExternalFileTypes.getInstance();
    }

    @Test
    void getTypeOfUnknownExtension() {
        assertEquals(Optional.empty(), types.getExternalFileTypeByExt(""));
    }

    @Test
    void getTypeOfPdfExtension() {
        assertEquals("PDF", types.getExternalFileTypeByExt("pdf").map(ExternalFileType::getName).get());
    }

    @Test
    void getTypeOfPdfMimeType() {
        assertEquals("PDF", types.getExternalFileTypeByMimeType("application/pdf").map(ExternalFileType::getName).get());
    }
}
