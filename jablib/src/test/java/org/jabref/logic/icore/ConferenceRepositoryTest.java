package org.jabref.logic.icore;

import java.io.InputStream;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.JabRefException;
import org.jabref.model.icore.ConferenceEntry;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConferenceRepositoryTest {
    private static final String TEST_DATA_FILE = "ICORETestData.csv";
    private static ConferenceRepository TEST_REPO;

    @BeforeAll
    static void loadTestConferenceDataIntoRepo() throws JabRefException {
        InputStream inputStream = ConferenceRepositoryTest.class.getResourceAsStream(TEST_DATA_FILE);
        TEST_REPO = new ConferenceRepository(inputStream);
    }

    static Stream<Arguments> generateConferenceTestData() {
        return Stream.of(
                // Acronym match tests
                Arguments.of("HCOMP", "2264", "AAAI Conference on Human Computation and Crowdsourcing", "HCOMP", "B"),  // acronym only exact match
                Arguments.of("BBBBB", null, null, null, null),    // no match found in conference data
                Arguments.of("''", null, null, null, null),    // empty string should return empty
                Arguments.of("(iiWAS 2010)", "822", "Information Integration and Web-based Applications and Services", "IIWAS", "C"),   // acronym with other text separated by space; acronym first
                Arguments.of("(2010 iiWAS)", "822", "Information Integration and Web-based Applications and Services", "IIWAS", "C"), // acronym with other text separated by space; acronym second
                Arguments.of("(CLOSER'12)", "2300", "International Conference on Cloud Computing and Services Science", "CLOSER", "C"), // acronym with other text separated by quote
                Arguments.of("(IEEE CCNC)", "616", "IEEE Consumer Communications and Networking Conference", "IEEE CCNC", "B"),  // acronym with space in it
                Arguments.of("(ACM_WiSec 2019)", "2313", "ACM Conference on Security and Privacy in Wireless and Mobile Networks", "ACM_WiSec", "B"), // acronym with special character with extra text separated by space
                Arguments.of("(EC-TEL'2023)", "2262", "European Conference on Technology Enhanced Learning", "EC-TEL", "B"), // acronym with special character and quote
                Arguments.of("(IEEE-IV'2023)", "2062", "Intelligent Vehicles Conference", "IEEE-IV", "B"), // longer acronym with delimiter matches (IEEE-IV) instead of nested one (IV)
                Arguments.of("CoopIS 2009 (OTM 2009)", "979", "International Conference on Cooperative Information Systems", "CoopIS", "B"), // acronym outside parentheses matches post-normalization
                Arguments.of("Cloud Computing and Services Science (CLOSER 2016): 6th International Conference",    // Acronym in middle of title
                        "2300", "International Conference on Cloud Computing and Services Science", "CLOSER", "C"),

                // Title match tests
                Arguments.of("AAAI Conference on Human Computation and Crowdsourcing",                           // Exact match without normalization
                        "2264", "AAAI Conference on Human Computation and Crowdsourcing", "HCOMP", "B"),
                Arguments.of("Proceedings of the 2nd AAAI Conference on Human Computation and Crowdsourcing",    // Exact match post-normalization
                        "2264", "AAAI Conference on Human Computation and Crowdsourcing", "HCOMP", "B"),
                Arguments.of(
                        // Long booktitle with lots of filler text has conference title as a substring; an exact LCS match
                        "22nd IEEE International Enterprise Distributed Object Computing Conference, EDOC 2018, Stockholm, Sweden, October 16-19, 2018",
                        "682", "IEEE International Enterprise Distributed Object Computing Conference", "EDOC", "B"),
                Arguments.of(
                        // Long misspelled booktitle with lots of filler text matches on combined Lev+LCS score above threshold
                        "Proceedings of the 3\\textsuperscript{rd} International Conference on Cloud Computing and Service Science, CLOSER 2013, 8-10 May 2013, Aachen, Germany",
                        "2300", "International Conference on Cloud Computing and Services Science", "CLOSER", "C"),
                Arguments.of(
                        // Long booktitle with lots of filler text and jumbled conference title does not match due to low combined score
                        "Advances in Information Retrieval - 41st European Conference on IR Research, ECIR 2019, Cologne, Germany, April 14-18, 2019, Proceedings, Part II",
                        null, null, null, null)
        );
    }

    @ParameterizedTest(name = "Booktitle \"{0}\" should match conference \"{2}\"")
    @MethodSource("generateConferenceTestData")
    void getConferenceFromBookTitle(
            String testBookTitle,
            String expectedId,
            String expectedTitle,
            String expectedAcronym,
            String expectedRank
    ) {
        Optional<ConferenceEntry> expectedResult = Optional.ofNullable(expectedId)
                                                           .map(_ -> new ConferenceEntry(expectedId, expectedTitle.toLowerCase(), expectedAcronym.toLowerCase(), expectedRank));

        assertEquals(expectedResult, TEST_REPO.getConferenceFromBookTitle(testBookTitle));
    }
}
