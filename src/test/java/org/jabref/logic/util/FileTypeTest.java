package org.jabref.logic.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileTypeTest {

    @Test
    public void testSingleFileExtensionDescription() {
        String singleDescription = "INSPEC file (*.txt)";
        assertEquals(singleDescription, BasicFileType.INSPEC.getDescription());
    }

    @Test
    public void testMultiFileExtensionsDescription() {
        String multiDescription = "MedlinePlain file (*.nbib, *.txt)";
        assertEquals(multiDescription, BasicFileType.MEDLINE_PLAIN.getDescription());
    }

    @Test
    public void testFirstExtensionWithDot() {
        assertEquals(".nbib", BasicFileType.MEDLINE_PLAIN.getFirstExtensionWithDot());
    }

}
