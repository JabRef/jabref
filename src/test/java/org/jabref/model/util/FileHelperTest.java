package org.jabref.model.util;

import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileHelperTest {
    @Test
    public void extractFileExtension() {
        final String filePath = FileHelperTest.class.getResource("pdffile.pdf").getPath();
        assertEquals(Optional.of("pdf"), FileHelper.getFileExtension(filePath));
    }

    @Test
    public void fileExtensionFromUrl() {
        final String filePath = "https://link.springer.com/content/pdf/10.1007%2Fs40955-018-0121-9.pdf";
        assertEquals(Optional.of("pdf"), FileHelper.getFileExtension(filePath));
    }

    @Test
    public void testFileNameEmpty() {
      Path path = Path.of("/");
      assertEquals(Optional.of(path), FileHelper.find("", path));
    }
}
