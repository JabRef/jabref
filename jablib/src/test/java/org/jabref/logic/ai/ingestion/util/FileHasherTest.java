package org.jabref.logic.ai.ingestion.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileHasherTest {

    @Test
    void computeHashProducesConsistentHash(@TempDir Path tempDir) throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Test content", StandardOpenOption.CREATE);

        Optional<String> hash1 = FileHasher.computeHash(testFile);
        Optional<String> hash2 = FileHasher.computeHash(testFile);

        assertEquals(hash1, hash2, "Same file should produce the same hash");
    }

    @Test
    void computeHashDifferentForDifferentContent(@TempDir Path tempDir) throws IOException {
        Path file1 = tempDir.resolve("file1.txt");
        Files.writeString(file1, "Content 1", StandardOpenOption.CREATE);

        Path file2 = tempDir.resolve("file2.txt");
        Files.writeString(file2, "Content 2", StandardOpenOption.CREATE);

        Path file3 = tempDir.resolve("file3.txt");
        Files.writeString(file3, "Content 1", StandardOpenOption.CREATE);

        Optional<String> hash1 = FileHasher.computeHash(file1);
        Optional<String> hash2 = FileHasher.computeHash(file2);
        Optional<String> hash3 = FileHasher.computeHash(file3);

        assertNotEquals(hash1, hash2, "Different files should produce different hashes");
        assertEquals(hash1, hash3, "Same file should produce the same hash");
    }

    @Test
    void computeHashProducesHexString(@TempDir Path tempDir) throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Test", StandardOpenOption.CREATE);

        Optional<String> hash = FileHasher.computeHash(testFile);

        assertTrue(hash.isPresent(), "Hash should be present");
        assertEquals(64, hash.get().length(), "SHA-256 hash should be 64 hex characters");

        assertTrue(hash.get().matches("[0-9a-f]+"), "Hash should contain only hexadecimal characters");
    }
}
