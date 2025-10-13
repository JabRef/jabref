package org.jabref.logic.crawler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.jabref.model.study.Study;
import org.jabref.model.study.StudyCatalog;
import org.jabref.model.study.StudyMetadata;
import org.jabref.model.study.StudyQuery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudyYamlV1MigratorTest {

    @TempDir
    Path tempDir;

    private StudyYamlV1Migrator migrator;

    @BeforeEach
    void setUp() {
        migrator = new StudyYamlV1Migrator();
    }

    @Test
    void getSourceVersionReturnsCorrectVersion() {
        assertEquals("1.0", migrator.getSourceVersion());
    }

    @Test
    void migrateBasicV1StudySuccessfully() throws IOException {
        // Create a basic v1.0 study file
        String v1Content = """
                authors:
                  - "John Doe"
                  - "Jane Smith"
                title: "Test Study"
                research-questions:
                  - "What is the impact of X?"
                  - "How does Y affect Z?"
                queries:
                  - "machine learning"
                  - "artificial intelligence"
                databases:
                  - name: "IEEE"
                  - name: "ACM"
                    enabled: false
                """;

        Path studyFile = tempDir.resolve("study.yml");
        Files.writeString(studyFile, v1Content);

        Study migratedStudy = migrator.migrate(studyFile);

        assertEquals("2.0", migratedStudy.getVersion());
        assertEquals("Test Study", migratedStudy.getTitle());
        assertEquals(List.of("John Doe", "Jane Smith"), migratedStudy.getAuthors());
        assertEquals(List.of("What is the impact of X?", "How does Y affect Z?"), migratedStudy.getResearchQuestions());

        // Check queries are converted correctly
        List<StudyQuery> queries = migratedStudy.getQueries();
        assertEquals(2, queries.size());
        assertEquals("machine learning", queries.getFirst().getQuery());
        assertEquals("artificial intelligence", queries.get(1).getQuery());

        // Check databases are converted to catalogs
        List<StudyCatalog> catalogs = migratedStudy.getCatalogs();
        assertEquals(2, catalogs.size());
        assertEquals("IEEE", catalogs.getFirst().getName());
        assertTrue(catalogs.getFirst().isEnabled()); // Should default to true
        assertEquals("ACM", catalogs.get(1).getName());
        assertFalse(catalogs.get(1).isEnabled()); // Should preserve false
    }

    @Test
    void migrateComplexQueriesSuccessfully() throws IOException {
        String v1Content = """
                authors:
                  - "Researcher"
                title: "Complex Query Study"
                research-questions:
                  - "Research question"
                queries:
                  - "simple query"
                  - query: "complex query"
                    description: "A complex search query"
                    lucene: "title:complex"
                    catalogSpecific:
                      IEEE: "ieee specific query"
                      ACM: "acm specific query"
                databases:
                  - name: "Springer"
                    enabled: true
                """;

        Path studyFile = tempDir.resolve("study.yml");
        Files.writeString(studyFile, v1Content);

        Study migratedStudy = migrator.migrate(studyFile);

        List<StudyQuery> queries = migratedStudy.getQueries();
        assertEquals(2, queries.size());

        // Simple query
        StudyQuery simpleQuery = queries.getFirst();
        assertEquals("simple query", simpleQuery.getQuery());

        // Complex query
        StudyQuery complexQuery = queries.get(1);
        Map<String, String> expectedCatalogSpecific = Map.of("IEEE", "ieee specific query", "ACM", "acm specific query");
        assertEquals(expectedCatalogSpecific, complexQuery.getCatalogSpecific());
    }

    @Test
    void migrateWithNullEnabledDatabase() throws IOException {
        String v1Content = """
                authors:
                  - "Author"
                title: "Null Enabled Test"
                research-questions:
                  - "Question"
                queries:
                  - "query"
                databases:
                  - name: "Database1"
                  - name: "Database2"
                    enabled: true
                  - name: "Database3"
                    enabled: false
                """;

        Path studyFile = tempDir.resolve("study.yml");
        Files.writeString(studyFile, v1Content);

        Study migratedStudy = migrator.migrate(studyFile);

        List<StudyCatalog> catalogs = migratedStudy.getCatalogs();
        assertEquals(3, catalogs.size());

        // First database should default to enabled=true
        assertTrue(catalogs.getFirst().isEnabled());
        assertEquals("Database1", catalogs.getFirst().getName());

        // Second database explicitly enabled
        assertTrue(catalogs.get(1).isEnabled());
        assertEquals("Database2", catalogs.get(1).getName());

        // Third database explicitly disabled
        assertFalse(catalogs.get(2).isEnabled());
        assertEquals("Database3", catalogs.get(2).getName());
    }

    @Test
    void createMetadataSuccessfully() throws IOException {
        String v1Content = """
                authors:
                  - "Author"
                title: "Metadata Test"
                research-questions:
                  - "What is the research question?"
                queries:
                  - "test query"
                databases:
                  - name: "PubMed"
                    enabled: true
                  - name: "Embase"
                    enabled: false
                """;

        Path studyFile = tempDir.resolve("study.yml");
        Files.writeString(studyFile, v1Content);

        Study migratedStudy = migrator.migrate(studyFile);

        StudyMetadata metadata = migratedStudy.getMetadata().orElseThrow(() ->
                new AssertionError("Expected metadata to be present after migration"));

        // Check migration notes
        String notes = metadata.getNotes().orElse("");
        assertTrue(notes.contains("Migrated from v1.0 format"));
        assertTrue(notes.contains("1 search queries"));
        assertTrue(notes.contains("1 active databases"));
    }

    @Test
    void migrateEmptyStudySuccessfully() throws IOException {
        String v1Content = """
                authors: []
                title: ""
                research-questions: []
                queries: []
                databases: []
                """;

        Path studyFile = tempDir.resolve("empty_study.yml");
        Files.writeString(studyFile, v1Content);

        Study migratedStudy = migrator.migrate(studyFile);

        assertNotNull(migratedStudy);
        assertEquals("2.0", migratedStudy.getVersion());
        assertEquals("", migratedStudy.getTitle());
        assertTrue(migratedStudy.getAuthors().isEmpty());
        assertTrue(migratedStudy.getResearchQuestions().isEmpty());
        assertTrue(migratedStudy.getQueries().isEmpty());
        assertTrue(migratedStudy.getCatalogs().isEmpty());

        // Should still have metadata
        assertNotNull(migratedStudy.getMetadata());
    }

    @Test
    void generateMigrationNotesCorrectly() throws IOException {
        String v1Content = """
                authors:
                  - "Author"
                title: "Notes Test"
                research-questions:
                  - "Question"
                queries:
                  - "query1"
                  - "query2"
                  - "query3"
                databases:
                  - name: "DB1"
                    enabled: true
                  - name: "DB2"
                    enabled: true
                  - name: "DB3"
                    enabled: false
                """;

        Path studyFile = tempDir.resolve("notes_test.yml");
        Files.writeString(studyFile, v1Content);

        Study migratedStudy = migrator.migrate(studyFile);

        String notes = migratedStudy.getMetadata()
                                    .flatMap(StudyMetadata::getNotes)
                                    .orElse("");
        assertTrue(notes.contains("Migrated from v1.0 format"));
        assertTrue(notes.contains("3 search queries"));
        assertTrue(notes.contains("2 active databases"));
    }
}
