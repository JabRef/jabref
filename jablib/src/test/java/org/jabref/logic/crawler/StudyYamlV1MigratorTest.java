package org.jabref.logic.crawler;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.model.study.Study;
import org.jabref.model.study.StudyCatalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudyYamlV1MigratorTest {

    @Test
    void migratesDatabasesToCatalogs() throws Exception {
        URL inputUrl = StudyYamlV1Migrator.class.getResource("study-v1-minimal.yml");
        URL expectedUrl = StudyYamlV1Migrator.class.getResource("study-v1-minimal-expected.yml");

        String input = Files.readString(Path.of(inputUrl.toURI()));
        String expected = Files.readString(Path.of(expectedUrl.toURI()));

        assertEquals(expected.replace("\r\n", "\n").trim(), StudyYamlV1Migrator.migrate(input).replace("\r\n", "\n").trim());
    }

    @Test
    void preservesExistingFieldsOnMigration() throws Exception {
        URL inputUrl = StudyYamlV1Migrator.class.getResource("study-v1-full.yml");
        URL expectedUrl = StudyYamlV1Migrator.class.getResource("study-v1-full-expected.yml");

        String input = Files.readString(Path.of(inputUrl.toURI()));
        String expected = Files.readString(Path.of(expectedUrl.toURI()));

        assertEquals(expected.replace("\r\n", "\n").trim(), StudyYamlV1Migrator.migrate(input).replace("\r\n", "\n").trim());
    }

    @Test
    void migrationProducesParseableStudyWithExpectedFields(@TempDir Path tempDir) throws Exception {
        URL inputUrl = StudyYamlV1Migrator.class.getResource("study-v1-full.yml");
        String input = Files.readString(Path.of(inputUrl.toURI()));
        String migrated = StudyYamlV1Migrator.migrate(input);
        Path migratedFile = tempDir.resolve("migrated-study.yml");
        Files.writeString(migratedFile, migrated);

        Study study = new StudyYamlParser().parseStudyYamlFile(migratedFile);

        assertEquals(Study.CURRENT_SCHEMA_VERSION, study.getVersion());
        assertEquals(1, study.getCatalogs().size());
        assertEquals(
                List.of("Springer"),
                study.getCatalogs().stream().map(StudyCatalog::getName).toList());
        assertTrue(study.getCatalogs().getFirst().isEnabled());
        assertEquals(1, study.getQueries().size());
        assertEquals("Quantum", study.getQueries().getFirst().getQuery());
    }
}
