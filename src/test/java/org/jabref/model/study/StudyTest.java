package org.jabref.model.study;

import java.time.LocalDate;
import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.SystematicLiteratureReviewStudyEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StudyTest {
    Study testStudy;

    @BeforeEach
    public void setUpTestStudy() {
        BibEntry studyEntry = new BibEntry()
                .withField(new UnknownField("name"), "TestStudyName")
                .withField(StandardField.AUTHOR, "Jab Ref")
                .withField(new UnknownField("researchQuestions"), "Question1; Question2")
                .withField(new UnknownField("gitRepositoryURL"), "https://github.com/eclipse/jgit.git");
        studyEntry.setType(SystematicLiteratureReviewStudyEntryType.STUDY_ENTRY);

        // Create three SearchTerm entries.
        BibEntry searchQuery1 = new BibEntry()
                .withField(new UnknownField("query"), "TestSearchQuery1");
        searchQuery1.setType(SystematicLiteratureReviewStudyEntryType.SEARCH_QUERY_ENTRY);
        searchQuery1.setCitationKey("query1");

        BibEntry searchQuery2 = new BibEntry()
                .withField(new UnknownField("query"), "TestSearchQuery2");
        searchQuery2.setType(SystematicLiteratureReviewStudyEntryType.SEARCH_QUERY_ENTRY);
        searchQuery2.setCitationKey("query2");

        BibEntry searchQuery3 = new BibEntry()
                .withField(new UnknownField("query"), "TestSearchQuery3");
        searchQuery3.setType(SystematicLiteratureReviewStudyEntryType.SEARCH_QUERY_ENTRY);
        searchQuery3.setCitationKey("query3");

        // Create two Library entries
        BibEntry library1 = new BibEntry()
                .withField(new UnknownField("name"), "acm")
                .withField(new UnknownField("enabled"), "false")
                .withField(new UnknownField("comment"), "disabled, because no good results");
        library1.setType(SystematicLiteratureReviewStudyEntryType.LIBRARY_ENTRY);
        library1.setCitationKey("library1");

        BibEntry library2 = new BibEntry()
                .withField(new UnknownField("name"), "arxiv")
                .withField(new UnknownField("enabled"), "true")
                .withField(new UnknownField("Comment"), "");
        library2.setType(SystematicLiteratureReviewStudyEntryType.LIBRARY_ENTRY);
        library2.setCitationKey("library2");

        testStudy = new Study(studyEntry, List.of(searchQuery1, searchQuery2, searchQuery3), List.of(library1, library2));
    }

    @Test
    void getSearchTermsAsStrings() {
        List<String> expectedSearchTerms = List.of("TestSearchQuery1", "TestSearchQuery2", "TestSearchQuery3");
        assertEquals(expectedSearchTerms, testStudy.getSearchQueryStrings());
    }

    @Test
    void setLastSearchTime() {
        LocalDate date = LocalDate.now();
        testStudy.setLastSearchDate(date);
        assertEquals(date.toString(), testStudy.getStudyMetaDataField(StudyMetaDataField.STUDY_LAST_SEARCH).get());
    }

    @Test
    void getStudyName() {
        assertEquals("TestStudyName", testStudy.getStudyMetaDataField(StudyMetaDataField.STUDY_NAME).get());
    }

    @Test
    void getStudyAuthor() {
        assertEquals("Jab Ref", testStudy.getStudyMetaDataField(StudyMetaDataField.STUDY_AUTHORS).get());
    }

    @Test
    void getResearchQuestions() {
        assertEquals("Question1; Question2", testStudy.getStudyMetaDataField(StudyMetaDataField.STUDY_RESEARCH_QUESTIONS).get());
    }

    @Test
    void getGitRepositoryURL() {
        assertEquals("https://github.com/eclipse/jgit.git", testStudy.getStudyMetaDataField(StudyMetaDataField.STUDY_GIT_REPOSITORY).get());
    }
}
