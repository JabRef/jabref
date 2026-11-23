package org.jabref.logic.importer.plaincitation;

import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RuleBasedPlainCitationParserTest {

    private final RuleBasedPlainCitationParser parser = new RuleBasedPlainCitationParser();

    static Stream<Arguments> parsePlainCitation() {
        return Stream.of(
                Arguments.of(
                        "Vendome C, Bavota G, Di Penta M, Linares-Vásquez M, German DM, Poshyvanyk D (2017) License usage and changes: A large-scale study on GitHub. Empirical Software Engineering 22(3):1537–1577. https://doi.org/10.1007/s10664-016-9438-4",
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Vendome, C. and Bavota, G. and Di Penta, M. and Linares-Vásquez, M. and German, D. M. and Poshyvanyk, D.")
                                .withField(StandardField.TITLE, "License usage and changes: A large-scale study on GitHub")
                                .withField(StandardField.JOURNAL, "Empirical Software Engineering")
                                .withField(StandardField.YEAR, "2017")
                                .withField(StandardField.VOLUME, "22")
                                .withField(StandardField.NUMBER, "3")
                                .withField(StandardField.PAGES, "1537--1577")
                                .withField(StandardField.DOI, "10.1007/s10664-016-9438-4")),
                Arguments.of(
                        "Smith, J. and Doe, A. (2020). A study of things. Journal of Stuff, 5(2), 10-20.",
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Smith, J. and Doe, A.")
                                .withField(StandardField.TITLE, "A study of things")
                                .withField(StandardField.JOURNAL, "Journal of Stuff")
                                .withField(StandardField.YEAR, "2020")
                                .withField(StandardField.VOLUME, "5")
                                .withField(StandardField.NUMBER, "2")
                                .withField(StandardField.PAGES, "10--20"))
        );
    }

    @ParameterizedTest
    @MethodSource
    void parsePlainCitation(String citation, BibEntry expected) {
        BibEntry actual = parser.parsePlainCitation(citation).orElseThrow();
        // The comment keeps the unparsed remainder; only internal placeholders must not leak into it
        assertFalse(actual.getField(StandardField.COMMENT).orElse("").contains("_tag]"));
        actual.clearField(StandardField.COMMENT);
        assertEquals(expected, actual);
    }

    @Test
    void yearInTitleDoesNotReplacePublicationYear() {
        BibEntry entry = parser.parsePlainCitation("Doe, J. 2023. Effects of treatment (2019) outcomes. Journal of Medicine 10(2):1-5.").orElseThrow();
        assertEquals(Optional.of("2023"), entry.getField(StandardField.YEAR));
    }

    @Test
    void numbersInTitleDoNotBecomeVolume() {
        BibEntry entry = parser.parsePlainCitation("Doe, J. (2020). Results from 2(3) experiments on 10-20 subjects. Journal of Testing, 5(2), 110-120.").orElseThrow();
        assertEquals(Optional.of("5"), entry.getField(StandardField.VOLUME));
        assertEquals(Optional.of("2"), entry.getField(StandardField.NUMBER));
        assertEquals(Optional.of("110--120"), entry.getField(StandardField.PAGES));
    }

    @Test
    void noLowerCaseLetterIsTakenForAnInitial() {
        BibEntry entry = parser.parsePlainCitation("Doe J (2017) Studying GitHub. Empirical Software Engineering 22(3):1537–1577.").orElseThrow();
        assertEquals(Optional.of("Doe, J."), entry.getField(StandardField.AUTHOR));
    }
}
