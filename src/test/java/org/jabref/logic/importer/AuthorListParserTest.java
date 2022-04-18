package org.jabref.logic.importer;

import java.util.stream.Stream;

import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthorListParserTest {

    private static Stream<Arguments> parseCorrectlySingleAuthors() {
        return Stream.of(
                Arguments.of("王, 军", new Author("军", "军.", null, "王", null)),
                Arguments.of("Doe, John", new Author("John", "J.", null, "Doe", null)),
                Arguments.of("von Berlichingen zu Hornberg, Johann Gottfried", new Author("Johann Gottfried", "J. G.", "von", "Berlichingen zu Hornberg", null)),
                Arguments.of("{Robert and Sons, Inc.}", new Author(null, null, null, "Robert and Sons, Inc.", null)),
                Arguments.of("al-Ṣāliḥ, Abdallāh", new Author("Abdallāh", "A.", null, "al-Ṣāliḥ", null)),
                Arguments.of("de la Vallée Poussin, Jean Charles Gabriel", new Author("Jean Charles Gabriel", "J. C. G.", "de la", "Vallée Poussin", null)),
                Arguments.of("de la Vallée Poussin, J. C. G.", new Author("J. C. G.", "J. C. G.", "de la", "Vallée Poussin", null)),
                Arguments.of("{K}ent-{B}oswell, E. S.", new Author("E. S.", "E. S.", null, "{K}ent-{B}oswell", null)),
                Arguments.of("Uhlenhaut, N Henriette", new Author("N Henriette", "N. H.", null, "Uhlenhaut", null))
        );
    }

    @ParameterizedTest
    @MethodSource
    void parseCorrectlySingleAuthors(String authorsString, Author authorsParsed) {
        AuthorListParser parser = new AuthorListParser();
        Assertions.assertEquals(AuthorList.of(authorsParsed), parser.parse(authorsString));
    }

    private static Stream<Arguments> parseCorrectlyMultipleAuthors() {
        return Stream.of(
                Arguments.of("First Habermann, Frank Leymann",
                        AuthorList.of(
                                new Author("First", "F.", null, "Habermann", null),
                                new Author("Frank", "F.", null, "Leymann", null)
                        )),
                Arguments.of("Hans-Joachim Habermann, Frank Leymann",
                        AuthorList.of(
                                new Author("Hans-Joachim", "H.-J.", null, "Habermann", null),
                                new Author("Frank", "F.", null, "Leymann", null)
                        )),
                // Example from code comment
                Arguments.of("Ali Babar, M., Dingsøyr, T., Lago, P., van der Vliet, H.",
                        AuthorList.of(
                                new Author("M.", "M.", null, "Ali Babar", null),
                                new Author("T.", "T.", null, "Dingsøyr", null),
                                new Author("P.", "P.", null, "Lago", null),
                                new Author("H.", "H.", "van der", "Vliet", null)
                        )));
    }

    @ParameterizedTest
    @MethodSource
    void parseCorrectlyMultipleAuthors(String authorsString, AuthorList authorsParsed) {
        AuthorListParser parser = new AuthorListParser();
        Assertions.assertEquals(authorsParsed, parser.parse(authorsString));
    }

    @Test
    public void parseAuthorWithFirstNameAbbreviationContainingUmlaut() {
        assertEquals(AuthorList.of(new Author("{\\OE}rjan", "{\\OE}.", null, "Umlauts", null)),
                new AuthorListParser().parse("{\\OE}rjan Umlauts"));
    }
}
