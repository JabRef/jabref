package org.jabref.logic.crawler;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.study.Study;
import org.jabref.model.study.StudyCatalog;
import org.jabref.model.study.StudyQuery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Execution(ExecutionMode.SAME_THREAD)
class StudyYamlParserTest {
    @TempDir
    static Path testDirectory;

    Study expectedStudy;

    @BeforeEach
    void setupStudy() throws URISyntaxException {
        Path destination = testDirectory.resolve(StudyRepository.STUDY_DEFINITION_FILE_NAME);
        URL studyDefinition = StudyYamlParser.class.getResource(StudyRepository.STUDY_DEFINITION_FILE_NAME);
        FileUtil.copyFile(Path.of(studyDefinition.toURI()), destination, true);

        List<String> authors = List.of("Haruki Murakami");
        String studyName = "TestStudyName";
        List<String> researchQuestions = List.of("Question1", "Question2");
        List<StudyQuery> queryEntries = List.of(new StudyQuery("Quantum"), new StudyQuery("Cloud Computing"), new StudyQuery("\"Software Engineering\""));
        List<StudyCatalog> libraryEntries = List.of(new StudyCatalog("Springer", true), new StudyCatalog("ArXiv", true),
                new StudyCatalog("Medline/PubMed", true), new StudyCatalog("IEEEXplore", false));

        expectedStudy = new Study(authors, studyName, researchQuestions, queryEntries, libraryEntries);
        expectedStudy.setVersion("2.0.0");
    }

    @Test
    void parseStudyFileSuccessfully() throws IOException {
        Study study = new StudyYamlParser().parseStudyYamlFile(testDirectory.resolve(StudyRepository.STUDY_DEFINITION_FILE_NAME));
        assertEquals(expectedStudy, study);
    }

    @Test
    void writeStudyFileSuccessfully() throws IOException {
        new StudyYamlParser().writeStudyYamlFile(expectedStudy, testDirectory.resolve(StudyRepository.STUDY_DEFINITION_FILE_NAME));
        Study study = new StudyYamlParser().parseStudyYamlFile(testDirectory.resolve(StudyRepository.STUDY_DEFINITION_FILE_NAME));
        assertEquals(expectedStudy, study);
    }

    @Test
    void readsJabRef57StudySuccessfully() throws URISyntaxException, IOException {
        // The field "last-search-date" was removed
        // If the field is "just" removed from the datamodel, one gets following exception:
        //   com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException: Unrecognized field "last-search-date" (class org.jabref.model.study.Study), not marked as ignorable (5 known properties: "authors", "research-questions", "queries", "title", "databases"])
        // This tests ensures that this exception does not occur
        URL studyDefinition = StudyYamlParser.class.getResource("study-jabref-5.7.yml");
        Study study = new StudyYamlParser().parseStudyYamlFile(Path.of(studyDefinition.toURI()));
        assertEquals(expectedStudy, study);
    }

    @Test
    void parseV2NativeStudyFileSuccessfully() throws URISyntaxException, IOException {
        URL studyDefinition = StudyYamlParser.class.getResource("study-v2-full.yml");

        Study study = new StudyYamlParser().parseStudyYamlFile(Path.of(studyDefinition.toURI()));

        assertEquals("2.0.0", study.getVersion());
        assertEquals(List.of("Donna Tartt", "Virginia Woolf"), study.getAuthors());
        assertEquals("TestStudyName", study.getTitle());
        assertEquals(List.of("Question1", "Question2"), study.getResearchQuestions());
        assertEquals(3, study.getQueries().size());
        assertEquals("Quantum", study.getQueries().getFirst().getQuery());
        assertEquals(Map.of(
                        "IEEEXplore", "(Document Title:Quantum)",
                        "ACM Portal", "[Title: Quantum]"),
                study.getQueries().getFirst().getCatalogSpecific());
        assertEquals(4, study.getCatalogs().size());
        assertEquals("Primary source",
                study.getCatalogs().stream()
                     .filter(c -> "Springer".equals(c.getName()))
                     .findFirst()
                     .orElseThrow()
                     .getReason());
    }

    @Test
    void writeAndReadStudyWithCatalogSpecificPreservesData() throws IOException {
        StudyQuery queryWithOverrides = new StudyQuery("Quantum");
        queryWithOverrides.setCatalogSpecific(Map.of(
                "IEEEXplore", "(Document Title:Quantum)",
                "ACM Portal", "[Title: Quantum]"));
        Study original = new Study(
                List.of("Oscar Wilde"),
                "Round-trip Study",
                List.of("Q1"),
                List.of(queryWithOverrides, new StudyQuery("Cloud Computing")),
                List.of(new StudyCatalog("Springer", true, "Primary source")));
        Path destination = testDirectory.resolve("round-trip-study.yml");

        new StudyYamlParser().writeStudyYamlFile(original, destination);
        Study roundTripped = new StudyYamlParser().parseStudyYamlFile(destination);

        assertEquals(original, roundTripped);
    }
}
