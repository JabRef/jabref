package org.jabref.logic.crawler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.model.study.Study;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for migrating study.yml files between versions.
 * Provides common functionality for version detection
 * and handles the migration process
 */

public abstract class StudyYamlMigrator {
    protected static final String CURRENT_VERSION = "2.0";
    private static final Logger LOGGER = LoggerFactory.getLogger(StudyYamlMigrator.class);
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    public static StudyYamlMigrator getMigratorForVersion(String version) {
        return switch (version) {
            case "1.0",
                 "unknown" ->
                    new StudyYamlV1Migrator();
            default -> {
                LOGGER.warn("Unknown version {}, using V1 migrator", version);
                yield new StudyYamlV1Migrator();
            }
        };
    }

    /**
     * Main entry point for migration. Detects version and delegates to appropriate migrator.
     */
    public static Study migrateStudyYamlFile(Path studyYamlFile) throws IOException {
        String version = detectVersion(studyYamlFile);

        if (CURRENT_VERSION.equals(version)) {
            // Already current version, read the file normally
            try (InputStream fileInputStream = Files.newInputStream(studyYamlFile)) {
                return YAML_MAPPER.readValue(fileInputStream, Study.class);
            }
        }

        LOGGER.info("Migrating study.yml from version {} to {}", version, CURRENT_VERSION);

        StudyYamlMigrator migrator = getMigratorForVersion(version);
        Study migratedStudy = migrator.migrate(studyYamlFile);

        LOGGER.info("Successfully migrated study.yml to version {}", CURRENT_VERSION);
        return migratedStudy;
    }

    /**
     * Template method that subclasses must implement for specific version migration
     */
    protected abstract Study migrate(Path studyYamlFile) throws IOException;

    /**
     * Returns the version this migrator handles
     */
    protected abstract String getSourceVersion();

    /**
     * Returns the target version this migrator produces
     */
    protected String getTargetVersion() {
        return CURRENT_VERSION;
    }

    /**
     * Detects the version of a study.yml file
     */
    private static String detectVersion(Path studyYamlFile) throws IOException {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

        try (InputStream fileInputStream = Files.newInputStream(studyYamlFile)) {
            JsonNode rootNode = yamlMapper.readTree(fileInputStream);
            JsonNode versionNode = rootNode.get("version");

            if (versionNode != null) {
                return versionNode.asText();
            }

            // If no version field, check for old structure indicators
            if (rootNode.has("databases") && !rootNode.has("catalogues")) {
                return "1.0"; // Old format
            }

            return "unknown";
        }
    }
}
