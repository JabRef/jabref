package org.jabref.logic.util;

import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DirectoriesTest {

    @Test
    void documentsDirectoryExists() {
        assertTrue(Files.isDirectory(Directories.getDocumentsDirectory()));
    }
}
