package org.jabref.logic.crawler;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import org.jabref.model.study.Study;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

public class StudyYamlParser {

    /**
     * Parses the given yaml study definition file into a study instance
     */
    public Study parseStudyYamlFile(Path studyYamlFile) throws IOException {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        try (InputStream fileInputStream = new FileInputStream(studyYamlFile.toFile())) {
            return yamlMapper.readValue(fileInputStream, Study.class);
        }
    }

    /**
     * Writes the given study instance into a yaml file to the given path
     */
    public void writeStudyYamlFile(Study study, Path studyYamlFile) throws IOException {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                                                                    .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES));
        yamlMapper.writeValue(studyYamlFile.toFile(), study);
    }
}
