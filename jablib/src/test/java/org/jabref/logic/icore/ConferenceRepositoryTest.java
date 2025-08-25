package org.jabref.logic.icore;

import java.io.InputStream;
import java.util.Optional;

import org.jabref.logic.JabRefException;
import org.jabref.model.icore.ConferenceEntry;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConferenceRepositoryTest {
    private static final String TEST_DATA_FILE = "ICORETestData.csv";
    private static ConferenceRepository TEST_REPO;

    @BeforeAll
    static void loadTestConferenceDataIntoRepo() throws JabRefException {
        InputStream inputStream = ConferenceRepositoryTest.class.getResourceAsStream(TEST_DATA_FILE);
        TEST_REPO = new ConferenceRepository(inputStream);
    }

    @AfterAll
    static void teardownRepo() {
        TEST_REPO = null;
    }

    @Test
    void getConferenceFromAcronymReturnsConferenceForExactMatch() {
        Optional<ConferenceEntry> conference = TEST_REPO.getConferenceFromAcronym("HCOMP");
        ConferenceEntry expectedResult = new ConferenceEntry(
                "2264",
                "AAAI Conference on Human Computation and Crowdsourcing".toLowerCase(),
                "HCOMP",
                "B"
        );

        assertTrue(conference.isPresent());
        assertEquals(expectedResult, conference.get());
    }

    @Test
    void getConferenceFromAcronymReturnsEmptyForNoMatch() {
        Optional<ConferenceEntry> conference = TEST_REPO.getConferenceFromAcronym("BBBBB");

        assertTrue(conference.isEmpty());
    }

    @Test
    void getConferenceFromAcronymReturnsEmptyForEmptyString() {
        Optional<ConferenceEntry> conference = TEST_REPO.getConferenceFromAcronym("");

        assertTrue(conference.isEmpty());
    }

    @Test
    void getConferenceFromBookTitleReturnsConferenceForExactMatch() {
        String bookTitle = "AAAI Conference on Human Computation and Crowdsourcing";
        Optional<ConferenceEntry> conference = TEST_REPO.getConferenceFromBookTitle(bookTitle);
        ConferenceEntry expectedResult = new ConferenceEntry(
                "2264",
                "AAAI Conference on Human Computation and Crowdsourcing".toLowerCase(),
                "HCOMP",
                "B"
        );

        assertTrue(conference.isPresent());
        assertEquals(expectedResult, conference.get());
    }

    @Test
    void getConferenceFromBookTitleReturnsEmptyForNoMatch() {
        Optional<ConferenceEntry> conference = TEST_REPO.getConferenceFromBookTitle("asdjkhasd");

        assertTrue(conference.isEmpty());
    }

    @Test
    void getConferenceFromBookTitleReturnsEmptyForEmptyString() {
        Optional<ConferenceEntry> conference = TEST_REPO.getConferenceFromBookTitle("");

        assertTrue(conference.isEmpty());
    }

    @Test
    void getConferenceFromBookTitleReturnsConferenceForFuzzyMatchAboveThreshold() {
        // String similarity > 0.9
        String bookTitle = "2nd AAAI Conference on Human Computation and Crowdsourcing";
        Optional<ConferenceEntry> conference = TEST_REPO.getConferenceFromBookTitle(bookTitle);
        ConferenceEntry expectedResult = new ConferenceEntry(
                "2264",
                "AAAI Conference on Human Computation and Crowdsourcing".toLowerCase(),
                "HCOMP",
                "B"
        );

        assertTrue(conference.isPresent());
        assertEquals(expectedResult, conference.get());
    }

    @Test
    void getConferenceFromBookTitleReturnsEmptyForFuzzyMatchBelowThreshold() {
        String bookTitle = "International AAAI Conference on Human Computation and Crowdsourcing";
        Optional<ConferenceEntry> conference = TEST_REPO.getConferenceFromBookTitle(bookTitle);

        assertTrue(conference.isEmpty());
    }

    @Test
    void getConferenceFromBookTitleReturnConferenceWithHigherSimilarity() {
        String bookTitle = "Conference with similarity ADC";
        Optional<ConferenceEntry> conference = TEST_REPO.getConferenceFromBookTitle(bookTitle);
        ConferenceEntry expectedResult = new ConferenceEntry(
                "9",
                "Conference With Similarity ABC".toLowerCase(),
                "SERA",
                "C"
        );

        assertTrue(conference.isPresent());
        assertEquals(expectedResult, conference.get());
    }
}
