package org.jabref.logic.crawler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.jabref.model.study.Study;
import org.jabref.model.study.StudyCatalog;
import org.jabref.model.study.StudyMetadata;
import org.jabref.model.study.StudyQuery;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Migrates study.yml files from version 1.0 format to the current format.
 * Handles:
 * - Renaming "databases" to "catalogs"
 * - Adding default "enabled" field for databases
 * - Converting simple query strings to StudyQuery objects
 * - Adding version field and metadata section
 */
public class StudyYamlV1Migrator extends StudyYamlMigrator {
    private static final Logger LOGGER = LoggerFactory.getLogger(StudyYamlV1Migrator.class);

    @Override
    protected Study migrate(Path studyYamlFile) throws IOException {
        LOGGER.debug("Migrating from v1.0 format: {}", studyYamlFile);

        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

        try (InputStream fileInputStream = Files.newInputStream(studyYamlFile)) {
            V1StudyFormat oldStudy = yamlMapper.readValue(fileInputStream, V1StudyFormat.class);
            return convertToCurrentFormat(oldStudy, studyYamlFile);
        }
    }

    @Override
    @NonNull
    protected String getSourceVersion() {
        return "1.0";
    }

    private Study convertToCurrentFormat(V1StudyFormat oldStudy, Path studyYamlFile) {
        // Convert queries - handle both simple strings and complex objects
        List<StudyQuery> newQueries = oldStudy.getQueries().stream()
                                              .map(this::convertQuery)
                                              .collect(Collectors.toList());

        // Convert databases to catalogs
        List<StudyCatalog> catalogs = oldStudy.getDatabases().stream()
                                              .map(this::convertDatabase)
                                              .collect(Collectors.toList());

        // Create new study with migrated data
        Study newStudy = new Study(
                oldStudy.getAuthors(),
                oldStudy.getTitle(),
                oldStudy.getResearchQuestions(),
                newQueries,
                catalogs
        );

        // Set version
        newStudy.setVersion(getTargetVersion());
        try {
            StudyMetadata metadata = createMetadata(studyYamlFile, oldStudy);
            newStudy.setMetadata(metadata);
        } catch (IOException e) {
            LOGGER.warn("Could not create metadata for study YAML file {}", studyYamlFile, e);
        }

        LOGGER.debug("Successfully converted v1.0 study with {} queries and {} catalogs",
                newQueries.size(), catalogs.size());

        return newStudy;
    }

    /**
     * Creates metadata by extracting information from file and existing data
     */
    @NonNull
    private StudyMetadata createMetadata(Path studyYamlFile, V1StudyFormat oldStudy) throws IOException {
        StudyMetadata metadata = new StudyMetadata();

        // Get file creation and modification times
        BasicFileAttributes attrs = Files.readAttributes(studyYamlFile, BasicFileAttributes.class);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

        // Use file creation time as created date
        Instant creationInstant = attrs.creationTime().toInstant();
        LocalDateTime createdDate = LocalDateTime.ofInstant(creationInstant, ZoneId.systemDefault());
        metadata.setCreatedDate(createdDate.format(formatter));

        // Use file modification time as last modified date
        Instant modificationInstant = attrs.lastModifiedTime().toInstant();
        LocalDateTime modifiedDate = LocalDateTime.ofInstant(modificationInstant, ZoneId.systemDefault());
        metadata.setLastModified(modifiedDate.format(formatter));

        // Generate notes
        String notes = generateMigrationNotes(oldStudy);
        metadata.setNotes(notes);

        return metadata;
    }

    // Remove the inferStudyType method entirely

    /**
     * Generate notes for the migration
     */
    private String generateMigrationNotes(V1StudyFormat oldStudy) {
        StringJoiner notes = new StringJoiner(" ");
        notes.add("Migrated from v1.0 format.");

        if (oldStudy.getQueries() != null) {
            notes.add("Contains " + oldStudy.getQueries().size() + " search queries.");
        }

        if (oldStudy.getDatabases() != null) {
            long enabledDatabases = oldStudy.getDatabases().stream()
                                            .filter(db -> db.isEnabled() == null || db.isEnabled())
                                            .count();
            notes.add("Configured for " + enabledDatabases + " active databases.");
        }

        return notes.toString();
    }

    @SuppressWarnings("unchecked")
    private StudyQuery convertQuery(Object queryObj) {
        if (queryObj instanceof String string) {
            // Simple string query
            return new StudyQuery(string);
        } else if (queryObj instanceof Map) {
            Map<String, Object> queryMap = (Map<String, Object>) queryObj;

            String query = (String) queryMap.get("query");
            String description = (String) queryMap.get("description");
            String lucene = (String) queryMap.get("lucene");

            Map<String, String> catalogSpecific = (Map<String, String>) queryMap.get("catalogSpecific");

            return new StudyQuery(query, description, lucene, catalogSpecific);
        }

        LOGGER.warn("Unexpected query type: {}, converting to string", queryObj.getClass());
        return new StudyQuery(queryObj.toString());
    }

    private StudyCatalog convertDatabase(V1Database oldDb) {
        // Set enabled=true as default if not specified

        boolean enabled = oldDb.isEnabled() != null ? oldDb.isEnabled() : true;
        return new StudyCatalog(oldDb.getName(), enabled);
    }

    /**
     * Represents the V1 study format for migration purposes
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class V1StudyFormat {
        private List<String> authors;
        private String title;

        @JsonProperty("research-questions")
        private List<String> researchQuestions;

        private List<Object> queries; // Can be String or Map in v1

        @JsonProperty("databases")
        private List<V1Database> databases;

        // Getters and setters
        public List<String> getAuthors() {
            return authors;
        }

        public void setAuthors(List<String> authors) {
            this.authors = authors;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public List<String> getResearchQuestions() {
            return researchQuestions;
        }

        public void setResearchQuestions(List<String> researchQuestions) {
            this.researchQuestions = researchQuestions;
        }

        public List<Object> getQueries() {
            return queries;
        }

        public void setQueries(List<Object> queries) {
            this.queries = queries;
        }

        public List<V1Database> getDatabases() {
            return databases;
        }

        public void setDatabases(List<V1Database> databases) {
            this.databases = databases;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class V1Database {
        private String name;
        private Boolean enabled;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
    }
}
