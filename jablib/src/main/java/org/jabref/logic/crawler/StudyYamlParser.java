package org.jabref.logic.crawler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.jabref.model.study.Study;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

/// Example use: `new StudyYamlParser().parseStudyYamlFile(studyDefinitionFile);`
public class StudyYamlParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(StudyYamlParser.class);

    /// Parses the given yaml study definition file into a study instance
    public Study parseStudyYamlFile(Path studyYamlFile) throws IOException {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        String rawString = Files.readString(studyYamlFile);
        Map<String, Object> rawMap = yamlMapper.readValue(rawString, new TypeReference<>() {
        });
        String yamlToDeserialize;
        if (!rawMap.containsKey("version")) {
            LOGGER.debug("Migrating study.yml from v1 to v2 format");
            yamlToDeserialize = StudyYamlV1Migrator.migrate(rawString);
        } else {
            yamlToDeserialize = rawString;
        }
        return yamlMapper.readValue(yamlToDeserialize, Study.class);
    }

    /// Writes the given study instance into a yaml file to the given path
    public void writeStudyYamlFile(Study study, Path studyYamlFile) throws IOException {

        ObjectMapper yamlMapper = new YAMLMapper(YAMLFactory.builder()
                                                            .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER)
                                                            .enable(YAMLWriteFeature.MINIMIZE_QUOTES).build());
        yamlMapper.writeValue(studyYamlFile.toFile(), study);
    }
}
