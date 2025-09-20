package org.jabref.logic.crawler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.model.study.Study;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

/**
 * Example use: <code>new StudyYamlParser().parseStudyYamlFile(studyDefinitionFile);</code>
 */
public class StudyYamlParser {

    /**
     * Parses the given yaml study definition file into a study instance
     */
    public Study parseStudyYamlFile(Path studyYamlFile) throws IOException {
        YAMLMapper yamlMapper = YAMLMapper.builder()
                                          .disable(MapperFeature.REQUIRE_HANDLERS_FOR_JAVA8_OPTIONALS)
                                          .build();
        try (InputStream fileInputStream = Files.newInputStream(studyYamlFile)) {
            return yamlMapper.readValue(fileInputStream, Study.class);
        }
    }

    /**
     * Writes the given study instance into a yaml file to the given path
     */
    public void writeStudyYamlFile(Study study, Path studyYamlFile) throws IOException {
        YAMLMapper yamlMapper = YAMLMapper.builder()
                                          .disable(MapperFeature.REQUIRE_HANDLERS_FOR_JAVA8_OPTIONALS)
                                          .build();
        yamlMapper.writeValue(studyYamlFile.toFile(), study);
    }
}
