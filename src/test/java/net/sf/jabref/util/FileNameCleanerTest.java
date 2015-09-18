package net.sf.jabref.util;

import net.sf.jabref.logic.util.io.FileNameCleaner;
import org.junit.Assert;
import org.junit.Test;

public class FileNameCleanerTest {

    @Test
    public void testCleanFileName() throws Exception {
        Assert.assertEquals("legalFilename.txt", FileNameCleaner.cleanFileName("legalFilename.txt"));
        Assert.assertEquals("illegalFilename______.txt", FileNameCleaner.cleanFileName("illegalFilename/?*<>|.txt"));
    }
}