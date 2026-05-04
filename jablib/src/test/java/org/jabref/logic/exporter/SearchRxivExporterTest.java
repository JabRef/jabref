package org.jabref.logic.exporter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.jabref.model.study.Study;
import org.jabref.model.study.StudyDatabase;
import org.jabref.model.study.StudyQuery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchRxivExporterTest {

    private final SearchRxivExporter exporter = new SearchRxivExporter();

    private Study buildStudy() {
        return new Study(
                List.of("John Doe"),
                "Test Study",
                List.of("What is AI?"),
                List.of(new StudyQuery("artificial intelligence AND health")),
                List.of(new StudyDatabase("IEEE", true)));
    }

    @Test
    void exportCreatesOneFilePerQueryAndDatabase(@TempDir Path tempDir) throws Exception {
        exporter.export(buildStudy(), tempDir);

        try (var files = Files.list(tempDir)) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void exportedJsonContainsCorrectFields(@TempDir Path tempDir) throws Exception {
        exporter.export(buildStudy(), tempDir);

        Path file;
        try (var files = Files.list(tempDir)) {
            file = files.findFirst().orElseThrow();
        }
        String content = Files.readString(file);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(content);

        assertEquals("artificial intelligence AND health", root.get("search_string").asText());
        assertEquals("ieee", root.get("platform").asText());
        assertEquals("John Doe", root.get("authors").get(0).get("name").asText());
        assertTrue(root.has("record_info"));
        assertTrue(root.has("date"));
        assertFalse(root.has("title"));
        assertTrue(root.has("database"));
        assertEquals(0, root.get("database").size());
    }

    @Test
    void exportCreatesMultipleFilesForMultipleDatabases(@TempDir Path tempDir) throws Exception {
        Study study = new Study(
                List.of("John Doe"),
                "Test Study",
                List.of("What is AI?"),
                List.of(new StudyQuery("machine learning")),
                List.of(new StudyDatabase("IEEE", true),
                        new StudyDatabase("ACM", true)));

        exporter.export(study, tempDir);

        try (var files = Files.list(tempDir)) {
            assertEquals(2, files.count());
        }
    }

    static Stream<Arguments> exportProducesExpectedFileName() {
        return Stream.of(
                Arguments.of("a/b:c*d", "IEEE", "IEEE-a_b_c_d-0.json"),
                Arguments.of("   ", "IEEE", "IEEE-query-0.json"),
                Arguments.of("machine learning", "IEEE", "IEEE-machine learning-0.json"));
    }

    @ParameterizedTest
    @MethodSource
    void exportProducesExpectedFileName(String query, String database, String expectedFileName, @TempDir Path tempDir) throws Exception {
        Study study = new Study(
                List.of("John Doe"),
                "Test Study",
                List.of("What is AI?"),
                List.of(new StudyQuery(query)),
                List.of(new StudyDatabase(database, true)));

        exporter.export(study, tempDir);

        try (var files = Files.list(tempDir)) {
            Path file = files.findFirst().orElseThrow();
            assertEquals(expectedFileName, file.getFileName().toString());
        }
    }
}
