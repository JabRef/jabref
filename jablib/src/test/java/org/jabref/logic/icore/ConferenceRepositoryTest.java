package org.jabref.logic.icore;

import java.io.InputStream;
import java.util.Optional;

import org.jabref.logic.JabRefException;
import org.jabref.model.icore.ConferenceEntry;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConferenceRepositoryTest {
    private static final String TEST_DATA_FILE = "ICORETestData.csv";
    private static ConferenceRepository TEST_REPO;

    @BeforeAll
    static void loadTestConferenceDataIntoRepo() throws JabRefException {
        InputStream inputStream = ConferenceRepositoryTest.class.getResourceAsStream(TEST_DATA_FILE);
        TEST_REPO = new ConferenceRepository(inputStream);
    }

    @ParameterizedTest(name = "Acronym \"{0}\" should match conference \"{2}\" in test repo")
    @CsvSource({
            // testAcronym, expectedId, expectedTitle, expectedAcronym, expectedRank
            "HCOMP, 2264, AAAI Conference on Human Computation and Crowdsourcing, HCOMP, B",    // exact match
            "BBBBB,,,,",    // no match found in conference data
            "'',,,,"        // empty string should return empty
    })
    void getConferenceFromAcronym(
            String testAcronym,
            String expectedId,
            String expectedTitle,
            String expectedAcronym,
            String expectedRank
    ) {
        Optional<ConferenceEntry> expectedResult = Optional.ofNullable(expectedId)
                .map(_ -> new ConferenceEntry(expectedId, expectedTitle.toLowerCase(), expectedAcronym, expectedRank));

        assertEquals(expectedResult, TEST_REPO.getConferenceFromAcronym(testAcronym));
    }

    @ParameterizedTest(name = "Booktitle \"{0}\" should match conference \"{2}\"")
    @CsvSource({
        // testBookTitle, expectedId, expectedTitle, expectedAcronym, expectedRank
        "AAAI Conference on Human Computation and Crowdsourcing, 2264, AAAI Conference on Human Computation and Crowdsourcing, HCOMP, B", // exact match
        "asdjkhasd,,,,",   // no match in conference data
        "'',,,,",          // empty string should return empty
        "2nd AAAI Conference on Human Computation and Crowdsourcing,2264,AAAI Conference on Human Computation and Crowdsourcing,HCOMP,B", // fuzzy match above threshold
        "International AAAI Conference on Human Computation and Crowdsourcing,,,,", // fuzzy match below threshold
        "Conference with similarity ADC,9,Conference With Similarity ABC,SERA,C"    // highest similarity result in fuzzy match
    })
    void getConferenceFromBookTitle(
            String testBookTitle,
            String expectedId,
            String expectedTitle,
            String expectedAcronym,
            String expectedRank
    ) {
        Optional<ConferenceEntry> expectedResult = Optional.ofNullable(expectedId)
                .map(_ -> new ConferenceEntry(expectedId, expectedTitle.toLowerCase(), expectedAcronym, expectedRank));

        assertEquals(expectedResult, TEST_REPO.getConferenceFromBookTitle(testBookTitle));
    }
}
