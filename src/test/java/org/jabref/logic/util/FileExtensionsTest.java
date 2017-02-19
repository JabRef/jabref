package org.jabref.logic.util;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FileExtensionsTest {

    @Test
    public void testSingleFileExtensionDescription() {
        String singleDescription = "INSPEC file (*.txt)";
        assertEquals(singleDescription, FileExtensions.INSPEC.getDescription());
    }

    @Test
    public void testMultiFileExtensionsDescription() {
        String multiDescription = "MedlinePlain file (*.nbib, *.txt)";
        assertEquals(multiDescription, FileExtensions.MEDLINE_PLAIN.getDescription());
    }

    @Test
    public void testExtensions() {
        List<String> extensions = Arrays.asList("nbib", "txt");
        Assert.assertArrayEquals(extensions.toArray(), FileExtensions.MEDLINE_PLAIN.getExtensions());
    }

    @Test
    public void testFirstExtensionWithDot() {
        assertEquals(".nbib", FileExtensions.MEDLINE_PLAIN.getFirstExtensionWithDot());
    }

}
