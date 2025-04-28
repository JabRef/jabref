package org.jabref.gui.externalfiletype;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import javafx.collections.FXCollections;

import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.icon.IconTheme;
import org.jabref.model.entry.LinkedFile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExternalFileTypesTest {
    private static final Set<ExternalFileType> TEST_LIST = Set.of(
            StandardExternalFileType.MARKDOWN,
            StandardExternalFileType.PDF,
            StandardExternalFileType.URL,
            StandardExternalFileType.JPG,
            StandardExternalFileType.TXT);

    private static final String TEST_STRINGLIST = "PostScript:REMOVED;" +
            "Word:REMOVED;" +
            "Word 2007+:REMOVED;" +
            "OpenDocument text:REMOVED;" +
            "Excel:REMOVED;" +
            "Excel 2007+:REMOVED;" +
            "OpenDocument spreadsheet:REMOVED;" +
            "PowerPoint:REMOVED;" +
            "PowerPoint 2007+:REMOVED;" +
            "OpenDocument presentation:REMOVED;" +
            "Rich Text Format:REMOVED;" +
            "PNG image:REMOVED;" +
            "GIF image:REMOVED;" +
            "Djvu:REMOVED;" +
            "LaTeX:REMOVED;" +
            "CHM:REMOVED;" +
            "TIFF image:REMOVED;" +
            "MHT:REMOVED;" +
            "ePUB:REMOVED";

    private final ExternalApplicationsPreferences externalApplicationsPreferences = mock(ExternalApplicationsPreferences.class);

    @BeforeEach
    void setUp() {
        when(externalApplicationsPreferences.getExternalFileTypes()).thenReturn(FXCollections.observableSet(TEST_LIST));
    }

    @Test
    void getExternalFileTypeByName() {
        assertEquals(Optional.of(StandardExternalFileType.PDF), ExternalFileTypes.getExternalFileTypeByName("PDF", externalApplicationsPreferences));
    }

    @Test
    void getExternalFileTypeByExt() {
        assertEquals(Optional.of(StandardExternalFileType.URL), ExternalFileTypes.getExternalFileTypeByExt("html", externalApplicationsPreferences));
    }

    @Test
    void isExternalFileTypeByExt() {
        assertTrue(ExternalFileTypes.isExternalFileTypeByExt("html", externalApplicationsPreferences));
        assertFalse(ExternalFileTypes.isExternalFileTypeByExt("tst", externalApplicationsPreferences));
    }

    @Test
    void getExternalFileTypeForName() {
        assertEquals(Optional.of(StandardExternalFileType.JPG), ExternalFileTypes.getExternalFileTypeForName("testfile.jpg", externalApplicationsPreferences));
    }

    @Test
    void getExternalFileTypeByMimeType() {
        assertEquals(Optional.of(StandardExternalFileType.TXT), ExternalFileTypes.getExternalFileTypeByMimeType("text/plain", externalApplicationsPreferences));
    }

    @Test
    void getExternalFileTypeByFile() {
        Path testfile = Path.of("testfile.txt");
        assertEquals(Optional.of(StandardExternalFileType.TXT), ExternalFileTypes.getExternalFileTypeByFile(testfile, externalApplicationsPreferences));
    }

    @Test
    void getExternalFileTypeByLinkedFile() {
        LinkedFile testfile = new LinkedFile("A testfile", "https://testserver.com/testfile.pdf", "PDF");
        assertEquals(Optional.of(StandardExternalFileType.PDF), ExternalFileTypes.getExternalFileTypeByLinkedFile(testfile, false, externalApplicationsPreferences));
    }

    @Test
    void toStringList() {
        String testString = ExternalFileTypes.toStringList(TEST_LIST);

        assertEquals(TEST_STRINGLIST, testString);
    }

    @Test
    void fromString() {
        Set<ExternalFileType> testList = ExternalFileTypes.fromString(TEST_STRINGLIST);

        assertEquals(TEST_LIST, testList);
    }

    @Test
    void externalFileTypetoStringArray() {
        ExternalFileType type = new CustomExternalFileType(
                "testEntry",
                "tst",
                "text/plain",
                "emacs",
                "close",
                IconTheme.JabRefIcons.CLOSE);

        assertEquals("[testEntry, tst, text/plain, emacs, CLOSE]", Arrays.toString(type.toStringArray()));
    }
}
