package org.jabref.logic.crawler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.model.study.Study;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Service class that coordinates YAML parsing and migration.
 * Provides a clean separation between parsing and migration.
 */

public class StudyYamlService {
    private final StudyYamlParser parser;

    public StudyYamlService() {
        this.parser = new StudyYamlParser();
    }

    public StudyYamlService(StudyYamlParser parser) {
        Optional<StudyYamlParser> parserOpt = Optional.ofNullable(parser);
        this.parser = parserOpt.orElse(new StudyYamlParser());
    }

    /**
     * Parse study YAML file with automatic migration if needed
     */
    public Study parseStudyYamlFile(Path studyYamlFile) throws IOException {
        if (needsMigration(studyYamlFile)) {
            Study migratedStudy = StudyYamlMigrator.migrateStudyYamlFile(studyYamlFile);

            // Create backup before overwriting
            createBackup(studyYamlFile);

            // Use parser to write the migrated study back to file
            parser.writeStudyYamlFile(migratedStudy, studyYamlFile);

            return migratedStudy;
        }

        // Parse the (already current version) file normally
        return parser.parseStudyYamlFile(studyYamlFile);
    }

    /**
     * Write study YAML file
     */
    public void writeStudyYamlFile(Study study, Path studyYamlFile) throws IOException {
        parser.writeStudyYamlFile(study, studyYamlFile);
    }

    private Optional<String> getVersionFromFile(Path studyYamlFile) throws IOException {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

        try (InputStream fileInputStream = Files.newInputStream(studyYamlFile)) {
            JsonNode rootNode = yamlMapper.readTree(fileInputStream);
            JsonNode versionNode = rootNode.get("version");
            return Optional.ofNullable(versionNode).map(JsonNode::asText);
        }
    }

    /**
     * Check if file needs migration by detecting version
     */
    private boolean needsMigration(Path studyYamlFile) throws IOException {
        Optional<String> version = getVersionFromFile(studyYamlFile);

        if (version.isPresent()) {
            return !"2.0".equals(version.get());
        }

        // If no version field, check for old structure indicators
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

        try (InputStream fileInputStream = Files.newInputStream(studyYamlFile)) {
            JsonNode rootNode = yamlMapper.readTree(fileInputStream);
            return rootNode.has("databases") && !rootNode.has("catalogs");
        }
    }

    /**
     * Creates a backup of the original file
     */
    private void createBackup(Path originalFile) throws IOException {
        Path backupPath = originalFile.getParent().resolve(Path.of(originalFile.getFileName() + ".backup"));

        // Delete existing backup file if it exists
        if (Files.exists(backupPath)) {
            Files.delete(backupPath);
        }

        Files.copy(originalFile, backupPath);
    }
}
