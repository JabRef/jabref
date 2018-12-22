package org.jabref.model.util;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileHelperTest {
    @Test
    public void extractFileExtension() {
        final String filePath = "somefilepath/file.pdf";
        assertEquals(Optional.of("pdf"), FileHelper.getFileExtension(filePath));
    }

    @Test
    public void urlIsNoFileExtension() {
        final String filePath = "https://someurl.io";
        assertEquals(Optional.empty(), FileHelper.getFileExtension(filePath));
    }
}
