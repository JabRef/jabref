package org.jabref.logic.crawler;

import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.study.LibraryEntry;
import org.jabref.model.study.QueryEntry;
import org.jabref.model.study.Study;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StudyYAMLParserTest {
    @TempDir
    static Path testDirectory;
    Study expectedStudy;

    @BeforeAll
    static void copyStudyYAMLIntoTestDirectory() throws Exception {
        Path destination = testDirectory.resolve("study.yml");
        URL studyDefinition = StudyYAMLParser.class.getResource("study.yml");
        FileUtil.copyFile(Path.of(studyDefinition.toURI()), destination, false);
    }

    @BeforeEach
    void setupStudy() {
        List<String> authors = List.of("Jab Ref");
        String studyName = "TestStudyName";
        List<String> researchQuestions = List.of("Question1", "Question2");
        List<QueryEntry> queryEntries = List.of(new QueryEntry("Quantum"), new QueryEntry("Cloud Computing"), new QueryEntry("\"Software Engineering\""));
        List<LibraryEntry> libraryEntries = List.of(new LibraryEntry("Springer", true), new LibraryEntry("ArXiv", true));

        expectedStudy = new Study(authors, studyName, researchQuestions, queryEntries, libraryEntries);
        expectedStudy.setLastSearchDate(LocalDate.parse("2020-11-26"));
    }

    @Test
    public void parseStudyFileSuccessfully() throws Exception {
        Study study = new StudyYAMLParser().parseStudyYAMLFile(testDirectory.resolve("study.yml"));

        assertEquals(expectedStudy, study);
    }

    @Test
    public void writeStudyFileSuccessfully() throws Exception {
        new StudyYAMLParser().writeStudyYAMLFile(expectedStudy, testDirectory.resolve("study.yml"));

        Study study = new StudyYAMLParser().parseStudyYAMLFile(testDirectory.resolve("study.yml"));

        assertEquals(expectedStudy, study);
    }
}
