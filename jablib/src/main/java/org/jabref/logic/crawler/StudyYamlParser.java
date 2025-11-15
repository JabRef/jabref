package org.jabref.logic.crawler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.model.study.Study;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

/**
 * Example use: <code>new StudyYamlParser().parseStudyYamlFile(studyDefinitionFile);</code>
 */
public class StudyYamlParser {

    /**
     * Parses the given yaml study definition file into a study instance
     */
    public Study parseStudyYamlFile(Path studyYamlFile) throws IOException {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        try (InputStream fileInputStream = Files.newInputStream(studyYamlFile)) {
            return yamlMapper.readValue(fileInputStream, Study.class);
        }
    }

    /**
     * Writes the given study instance into a yaml file to the given path
     */
    public void writeStudyYamlFile(Study study, Path studyYamlFile) throws IOException {

        ObjectMapper yamlMapper = new YAMLMapper(YAMLFactory.builder()
                                                            .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER)
                                                            .enable(YAMLWriteFeature.MINIMIZE_QUOTES).build());
        yamlMapper.writeValue(studyYamlFile.toFile(), study);
    }
}
