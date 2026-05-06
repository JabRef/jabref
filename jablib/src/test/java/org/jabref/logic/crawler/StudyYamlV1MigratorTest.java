package org.jabref.logic.crawler;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
