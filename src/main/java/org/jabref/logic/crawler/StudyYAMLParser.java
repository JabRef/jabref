package org.jabref.logic.crawler;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import org.jabref.model.study.Study;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class StudyYAMLParser {

    /**
     * Parses the given yaml study definition file into a study instance
     */
    public Study parseStudyYAMLFile(Path studyYAMLFile) throws IOException {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        yamlMapper.registerModule(new JavaTimeModule());
        try (InputStream fileInputStream = new FileInputStream(studyYAMLFile.toFile())) {
            return yamlMapper.readValue(fileInputStream, Study.class);
        }
    }

    /**
     * Writes the given study instance into a yaml file to the given path
     */
    public void writeStudyYAMLFile(Study study, Path studyYAMLFile) throws IOException {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                                                                    .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES));
        yamlMapper.registerModule(new JavaTimeModule());
        yamlMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        yamlMapper.writeValue(studyYAMLFile.toFile(), study);
    }
}
