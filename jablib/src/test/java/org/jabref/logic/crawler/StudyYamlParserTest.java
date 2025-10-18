package org.jabref.logic.crawler;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.study.Study;
import org.jabref.model.study.StudyCatalog;
import org.jabref.model.study.StudyQuery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StudyYamlParserTest {
    @TempDir
    static Path testDirectory;

    Study expectedStudy;

    @BeforeEach
    void setupStudy() throws URISyntaxException {
        Path destination = testDirectory.resolve(StudyRepository.STUDY_DEFINITION_FILE_NAME);
        URL studyDefinition = StudyYamlParser.class.getResource(StudyRepository.STUDY_DEFINITION_FILE_NAME);
        FileUtil.copyFile(Path.of(studyDefinition.toURI()), destination, true);

        List<String> authors = List.of("Jab Ref");
        String studyName = "TestStudyName";
        List<String> researchQuestions = List.of("Question1", "Question2");
        List<StudyQuery> queryEntries = List.of(new StudyQuery("Quantum"), new StudyQuery("Cloud Computing"), new StudyQuery("\"Software Engineering\""));
        List<StudyCatalog> libraryEntries = List.of(new StudyCatalog("Springer", true), new StudyCatalog("ArXiv", true),
                new StudyCatalog("Medline/PubMed", true), new StudyCatalog("IEEEXplore", false));

        expectedStudy = new Study(authors, studyName, researchQuestions, queryEntries, libraryEntries);
    }

    @Test
    void parseStudyFileSuccessfully() throws IOException {
        Study study = new StudyYamlService().parseStudyYamlFile(testDirectory.resolve(StudyRepository.STUDY_DEFINITION_FILE_NAME));
        assertEquals(expectedStudy, study);
    }

    @Test
    void writeStudyFileSuccessfully() throws IOException {
        new StudyYamlService().writeStudyYamlFile(expectedStudy, testDirectory.resolve(StudyRepository.STUDY_DEFINITION_FILE_NAME));
        Study study = new StudyYamlService().parseStudyYamlFile(testDirectory.resolve(StudyRepository.STUDY_DEFINITION_FILE_NAME));
        assertEquals(expectedStudy, study);
    }

    @Test
    void readsJabRef57StudySuccessfully() throws URISyntaxException, IOException {
        // The field "last-search-date" was removed
        // If the field is "just" removed from the datamodel, one gets following exception:
        //   com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException: Unrecognized field "last-search-date" (class org.jabref.model.study.Study), not marked as ignorable (5 known properties: "authors", "research-questions", "queries", "title", "databases"])
        // This tests ensures that this exception does not occur
        URL studyDefinition = StudyYamlService.class.getResource("study-jabref-5.7.yml");
        Study study = new StudyYamlService().parseStudyYamlFile(Path.of(studyDefinition.toURI()));
        assertEquals(expectedStudy, study);
    }
}
