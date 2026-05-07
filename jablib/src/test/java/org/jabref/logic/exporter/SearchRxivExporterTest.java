package org.jabref.logic.exporter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.model.study.Study;
import org.jabref.model.study.StudyDatabase;
import org.jabref.model.study.StudyQuery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        ObjectMapper mapper = new ObjectMapper();
        JsonNode expected = mapper.readTree("""
                {
                  "search_string": "artificial intelligence AND health",
                  "platform": "ieee",
                  "authors": [{"name": "John Doe"}],
                  "record_info": {},
                  "date": {},
                  "database": []
                }
                """);

        assertEquals(expected, mapper.readTree(Files.readString(file)));
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

    @ParameterizedTest
    @CsvSource(textBlock = """
            'a/b:c*d'         , IEEE, IEEE-a_b_c_d-0.json
            '   '             , IEEE, IEEE-query-0.json
            'machine learning', IEEE, 'IEEE-machine learning-0.json'
            """)
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
