package org.jabref.logic.exporter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtomicFileWriterTest {

    @Test
    void encodingIssueDoesNotLeadToCrash(@TempDir Path tempDir) throws Exception {
        Path target = tempDir.resolve("test.txt");
        AtomicFileWriter atomicFileWriter = new AtomicFileWriter(target, StandardCharsets.US_ASCII);
        atomicFileWriter.write("ñ");
        atomicFileWriter.close();
        assertTrue(atomicFileWriter.hasEncodingProblems());
        assertEquals(Set.of('ñ'), atomicFileWriter.getEncodingProblems());
    }
}
