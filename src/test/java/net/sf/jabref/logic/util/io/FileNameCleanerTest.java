package net.sf.jabref.logic.util.io;

import org.junit.Assert;
import org.junit.Test;

public class FileNameCleanerTest {

    @Test
    public void testCleanFileName() {
        Assert.assertEquals("legalFilename.txt", FileNameCleaner.cleanFileName("legalFilename.txt"));
        Assert.assertEquals("illegalFilename______.txt", FileNameCleaner.cleanFileName("illegalFilename/?*<>|.txt"));
    }
}