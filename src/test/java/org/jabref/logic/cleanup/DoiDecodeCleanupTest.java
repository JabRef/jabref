package org.jabref.logic.cleanup;

import java.util.stream.Stream;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DoiDecodeCleanupTest {

    @ParameterizedTest
    @MethodSource("provideDoiForAllLowers")
    public void testChangeDoi(BibEntry expected, BibEntry doiInfoField) {
        DoiCleanup cleanUp = new DoiCleanup();
        cleanUp.cleanup(doiInfoField);

        assertEquals(expected, doiInfoField);
    }

    private static Stream<Arguments> provideDoiForAllLowers() {
        UnknownField unknownField = new UnknownField("ee");
        BibEntry doiResult = new BibEntry().withField(StandardField.DOI, "10.18726/2018_3");

        return Stream.of(
                // cleanup for Doi field only
                Arguments.of(doiResult, new BibEntry().withField(
                        StandardField.URL, "https://doi.org/10.18726/2018%7B%5Ctextunderscore%7D3")),

                // cleanup with Doi and URL to all entries
                Arguments.of(doiResult, new BibEntry()
                        .withField(StandardField.DOI, "10.18726/2018%7B%5Ctextunderscore%7D3")
                        .withField(StandardField.URL, "https://doi.org/10.18726/2018%7B%5Ctextunderscore%7D3")
                        .withField(StandardField.NOTE, "https://doi.org/10.18726/2018%7B%5Ctextunderscore%7D3")
                        .withField(unknownField, "https://doi.org/10.18726/2018%7B%5Ctextunderscore%7D3")),

                // cleanup with Doi and no URL to entries
                Arguments.of(
                        new BibEntry()
                        .withField(StandardField.DOI, "10.18726/2018_3")
                        .withField(StandardField.NOTE, "This is a random note to this Doi")
                        .withField(unknownField, "This is a random ee field for this Doi"),
                        new BibEntry()
                        .withField(StandardField.DOI, "10.18726/2018_3")
                        .withField(StandardField.NOTE, "This is a random note to this Doi")
                        .withField(unknownField, "This is a random ee field for this Doi")),

                // cleanup with spaced Doi
                Arguments.of(doiResult, new BibEntry()
                        .withField(StandardField.DOI, "10.18726/2018%7B%5Ctextunderscore%7D3")),

                // cleanup just Note field with URL
                Arguments.of(doiResult, new BibEntry()
                        .withField(StandardField.NOTE, "https://doi.org/10.18726/2018%7B%5Ctextunderscore%7D3")),

                // cleanup just ee field with URL
                Arguments.of(doiResult, new BibEntry()
                        .withField(unknownField, "https://doi.org/10.18726/2018%7B%5Ctextunderscore%7D3"))
        );
    }
}
