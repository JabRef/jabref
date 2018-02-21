package org.jabref.logic.util.io;

import org.junit.Assert;
import org.junit.Test;

public class FileNameCleanerTest {

    @Test
    public void testCleanFileName() {
        Assert.assertEquals("legalFilename.txt", FileNameCleaner.cleanFileName("legalFilename.txt"));
        Assert.assertEquals("illegalFilename______.txt", FileNameCleaner.cleanFileName("illegalFilename/?*<>|.txt"));
    }

    @Test
    public void testCleanDirectoryName() {
        Assert.assertEquals("legalFilename.txt", FileNameCleaner.cleanDirectoryName("legalFilename.txt"));
        Assert.assertEquals("subdir/legalFilename.txt", FileNameCleaner.cleanDirectoryName("subdir/legalFilename.txt"));
        Assert.assertEquals("illegalFilename/_____.txt", FileNameCleaner.cleanDirectoryName("illegalFilename/?*<>|.txt"));
    }

    @Test
    public void testCleanDirectoryNameForWindows() {
        Assert.assertEquals("legalFilename.txt", FileNameCleaner.cleanDirectoryName("legalFilename.txt"));
        Assert.assertEquals("subdir\\legalFilename.txt", FileNameCleaner.cleanDirectoryName("subdir\\legalFilename.txt"));
        Assert.assertEquals("illegalFilename\\_____.txt", FileNameCleaner.cleanDirectoryName("illegalFilename\\?*<>|.txt"));
    }
}
