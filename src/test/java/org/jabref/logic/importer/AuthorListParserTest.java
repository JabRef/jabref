package org.jabref.logic.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AuthorListParserTest {

    AuthorListParser parser = new AuthorListParser();

    private static Stream<Arguments> parseSingleAuthorCorrectly() {
        return Stream.of(
            Arguments.of("王, 军", new Author("军", "军.", null, "王", null)),
            Arguments.of("Doe, John", new Author("John", "J.", null, "Doe", null)),
            Arguments.of(
                "von Berlichingen zu Hornberg, Johann Gottfried",
                new Author("Johann Gottfried", "J. G.", "von", "Berlichingen zu Hornberg", null)
            ),
            Arguments.of(
                "{Robert and Sons, Inc.}",
                new Author(null, null, null, "Robert and Sons, Inc.", null)
            ),
            Arguments.of(
                "al-Ṣāliḥ, Abdallāh",
                new Author("Abdallāh", "A.", null, "al-Ṣāliḥ", null)
            ),
            Arguments.of(
                "de la Vallée Poussin, Jean Charles Gabriel",
                new Author("Jean Charles Gabriel", "J. C. G.", "de la", "Vallée Poussin", null)
            ),
            Arguments.of(
                "de la Vallée Poussin, J. C. G.",
                new Author("J. C. G.", "J. C. G.", "de la", "Vallée Poussin", null)
            ),
            Arguments.of(
                "{K}ent-{B}oswell, E. S.",
                new Author("E. S.", "E. S.", null, "{K}ent-{B}oswell", null)
            ),
            Arguments.of(
                "Uhlenhaut, N Henriette",
                new Author("N Henriette", "N. H.", null, "Uhlenhaut", null)
            ),
            Arguments.of(
                "Nu{\\~{n}}ez, Jose",
                new Author("Jose", "J.", null, "Nu{\\~{n}}ez", null)
            ),
            // parseAuthorWithFirstNameAbbreviationContainingUmlaut
            Arguments.of(
                "{\\OE}rjan Umlauts",
                new Author("{\\OE}rjan", "{\\OE}.", null, "Umlauts", null)
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void parseSingleAuthorCorrectly(String authorsString, Author authorsParsed) {
        assertEquals(AuthorList.of(authorsParsed), parser.parse(authorsString));
    }

    private static Stream<Arguments> parseMultipleCorrectly() {
        return Stream.of(
            Arguments.of(
                AuthorList.of(
                    new Author("Alexander", "A.", null, "Artemenko", null),
                    Author.OTHERS
                ),
                "Alexander Artemenko and others"
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    void parseMultipleCorrectly(AuthorList expected, String authorsString) {
        assertEquals(expected, parser.parse(authorsString));
    }
}
