package org.jabref.logic.crawler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jabref.model.study.Study;
import org.jabref.model.study.StudyCatalog;
import org.jabref.model.study.StudyMetadata;
import org.jabref.model.study.StudyQuery;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Migrates study.yml files from version 1.0 format to the current format.
 * Handles:
 * - Renaming "databases" to "catalogues"
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
            // Parse as old format
            V1StudyFormat oldStudy = yamlMapper.readValue(fileInputStream, V1StudyFormat.class);

            // Convert to new format
            return convertToCurrentFormat(oldStudy);
        }
    }

    @Override
    protected String getSourceVersion() {
        return "1.0";
    }

    private Study convertToCurrentFormat(V1StudyFormat oldStudy) {
        // Convert queries - handle both simple strings and complex objects
        List<StudyQuery> newQueries = oldStudy.getQueries().stream()
                                              .map(this::convertQuery)
                                              .collect(Collectors.toList());

        // Convert databases to catalogues
        List<StudyCatalog> catalogues = oldStudy.getDatabases().stream()
                                                .map(this::convertDatabase)
                                                .collect(Collectors.toList());

        // Create new study with migrated data
        Study newStudy = getNewStudy(oldStudy, newQueries, catalogues);

        LOGGER.debug("Successfully converted v1.0 study with {} queries and {} catalogues",
                newQueries.size(), catalogues.size());

        return newStudy;
    }

    private @NotNull Study getNewStudy(V1StudyFormat oldStudy, List<StudyQuery> newQueries, List<StudyCatalog> catalogues) {
        Study newStudy = new Study(
                oldStudy.getAuthors(),
                oldStudy.getTitle(),
                oldStudy.getResearchQuestions(),
                newQueries,
                catalogues
        );

        // Set version and add default metadata
        newStudy.setVersion(getTargetVersion());

        // Add basic metadata
        StudyMetadata metadata = new StudyMetadata();
        metadata.setStudyType("systematic-literature-review");
        newStudy.setMetadata(metadata);
        return newStudy;
    }

    private StudyQuery convertQuery(Object queryObj) {
        if (queryObj instanceof String) {
            // Simple string query
            return new StudyQuery((String) queryObj);
        } else if (queryObj instanceof Map) {
            // Complex query object from v1
            @SuppressWarnings("unchecked")
            Map<String, Object> queryMap = (Map<String, Object>) queryObj;

            String query = (String) queryMap.get("query");
            String description = (String) queryMap.get("description");
            String lucene = (String) queryMap.get("lucene");

            @SuppressWarnings("unchecked")
            Map<String, String> catalogueSpecific = (Map<String, String>) queryMap.get("catalogueSpecific");

            return new StudyQuery(query, description, lucene, catalogueSpecific);
        }

        // Fallback for unexpected types
        LOGGER.warn("Unexpected query type: {}, converting to string", queryObj.getClass());
        return new StudyQuery(queryObj.toString());
    }

    private StudyCatalog convertDatabase(V1Database oldDb) {
        // Set enabled=true as default if not specified
        boolean enabled = oldDb.isEnabled() != null ? oldDb.isEnabled() : true;
        StudyCatalog newDb = new StudyCatalog(oldDb.getName(), enabled);

        // If there was a comment field in v1, we could preserve it here
        // For now, we just log if there are unexpected fields

        return newDb;
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
