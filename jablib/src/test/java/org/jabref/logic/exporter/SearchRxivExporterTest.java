package org.jabref.logic.exporter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.model.study.Study;
import org.jabref.model.study.StudyDatabase;
import org.jabref.model.study.StudyQuery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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

        assertEquals(1, Files.list(tempDir).count());
    }

    @Test
    void exportedJsonContainsCorrectFields(@TempDir Path tempDir) throws Exception {
        exporter.export(buildStudy(), tempDir);

        Path file = Files.list(tempDir).findFirst().orElseThrow();
        String content = Files.readString(file);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(content);

        assertEquals("artificial intelligence AND health", root.get("search_string").asText());
        assertEquals("ieee", root.get("platform").asText());
        assertEquals("John Doe", root.get("authors").get(0).get("name").asText());
        assertTrue(root.has("record_info"));
        assertTrue(root.has("date"));
        assertFalse(root.has("title"));
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

        assertEquals(2, Files.list(tempDir).count());
    }
}
