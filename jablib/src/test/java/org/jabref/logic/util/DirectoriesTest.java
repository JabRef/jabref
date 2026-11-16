package org.jabref.logic.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DirectoriesTest {

    @TempDir
    Path userDirectory;

    @Test
    void existingXdgDocumentsDirectoryIsPreferred() throws IOException {
        Files.createDirectory(userDirectory.resolve("Documents"));
        Path xdgDocuments = Files.createDirectory(userDirectory.resolve("Dokumente"));

        assertEquals(xdgDocuments, Directories.getDocumentsDirectory(xdgDocuments.toString(), userDirectory, Optional::empty));
    }

    @Test
    void xdgLookupFindsLocalizedDocumentsDirectory() throws IOException {
        Files.createDirectory(userDirectory.resolve("Documents"));
        Path localizedDocuments = Files.createDirectory(userDirectory.resolve("Dokumente"));

        assertEquals(localizedDocuments, Directories.getDocumentsDirectory(null, userDirectory, () -> Optional.of(localizedDocuments)));
    }

    @Test
    void invalidXdgLookupFallsBackToDocuments() throws IOException {
        Path documents = Files.createDirectory(userDirectory.resolve("Documents"));

        assertEquals(documents, Directories.getDocumentsDirectory(null, userDirectory, () -> Optional.of(userDirectory.resolve("missing"))));
    }

    @Test
    void documentsInUserDirectoryIsUsedWithoutXdgDocumentsDirectory() throws IOException {
        Path documents = Files.createDirectory(userDirectory.resolve("Documents"));

        assertEquals(documents, Directories.getDocumentsDirectory(null, userDirectory, Optional::empty));
    }

    @Test
    void documentsInUserDirectoryIsUsedIfXdgDocumentsDirectoryIsMissing() throws IOException {
        Path documents = Files.createDirectory(userDirectory.resolve("Documents"));

        assertEquals(documents, Directories.getDocumentsDirectory(userDirectory.resolve("Dokumente").toString(), userDirectory, Optional::empty));
    }

    @Test
    void userDirectoryIsUsedWithoutAnyDocumentsDirectory() {
        assertEquals(userDirectory, Directories.getDocumentsDirectory(null, userDirectory, Optional::empty));
    }
}
