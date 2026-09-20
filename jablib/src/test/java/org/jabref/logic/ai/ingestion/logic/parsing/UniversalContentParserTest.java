package org.jabref.logic.ai.ingestion.logic.parsing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
class UniversalContentParserTest {

    private final UniversalContentParser parser = new UniversalContentParser();

    @Test
    void parseUnsupportedFileTypeReturnsEmptyList(@TempDir Path tempDir) throws IOException {
        Path textFile = tempDir.resolve("test.txt");
        Files.writeString(textFile, "Some text content");

        List<String> result = parser.parse(textFile);
        assertEquals(List.of(), result);
    }
}
