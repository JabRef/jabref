package org.jabref.gui.externalfiletype;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import javafx.collections.FXCollections;

import org.jabref.gui.icon.IconTheme;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.FilePreferences;

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

    private final FilePreferences filePreferences = mock(FilePreferences.class);

    @BeforeEach
    void setUp() {
        when(filePreferences.getExternalFileTypes()).thenReturn(FXCollections.observableSet(TEST_LIST));
    }

    @Test
    void getExternalFileTypeByName() {
        assertEquals(Optional.of(StandardExternalFileType.PDF), ExternalFileTypes.getExternalFileTypeByName("PDF", filePreferences));
    }

    @Test
    void getExternalFileTypeByExt() {
        assertEquals(Optional.of(StandardExternalFileType.URL), ExternalFileTypes.getExternalFileTypeByExt("html", filePreferences));
    }

    @Test
    void isExternalFileTypeByExt() {
        assertTrue(ExternalFileTypes.isExternalFileTypeByExt("html", filePreferences));
        assertFalse(ExternalFileTypes.isExternalFileTypeByExt("tst", filePreferences));
    }

    @Test
    void getExternalFileTypeForName() {
        assertEquals(Optional.of(StandardExternalFileType.JPG), ExternalFileTypes.getExternalFileTypeForName("testfile.jpg", filePreferences));
    }

    @Test
    void getExternalFileTypeByMimeType() {
        assertEquals(Optional.of(StandardExternalFileType.TXT), ExternalFileTypes.getExternalFileTypeByMimeType("text/plain", filePreferences));
    }

    @Test
    void getExternalFileTypeByFile() {
        Path testfile = Path.of("testfile.txt");
        assertEquals(Optional.of(StandardExternalFileType.TXT), ExternalFileTypes.getExternalFileTypeByFile(testfile, filePreferences));
    }

    @Test
    void getExternalFileTypeByLinkedFile() {
        LinkedFile testfile = new LinkedFile("A testfile", "https://testserver.com/testfile.pdf", "PDF");
        assertEquals(Optional.of(StandardExternalFileType.PDF), ExternalFileTypes.getExternalFileTypeByLinkedFile(testfile, false, filePreferences));
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
