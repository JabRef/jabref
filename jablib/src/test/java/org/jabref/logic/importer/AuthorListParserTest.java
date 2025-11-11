package org.jabref.logic.importer;

import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Similar tests are available in {@link org.jabref.model.entry.AuthorListTest}
 */
class AuthorListParserTest {

    AuthorListParser parser = new AuthorListParser();

    private static Stream<Arguments> parseSingleAuthorCorrectly() {
        return Stream.of(
                Arguments.of("王, 军", new Author("军", "军.", null, "王", null)),
                Arguments.of("Doe, John", new Author("John", "J.", null, "Doe", null)),
                Arguments.of("von Berlichingen zu Hornberg, Johann Gottfried", new Author("Johann Gottfried", "J. G.", "von", "Berlichingen zu Hornberg", null)),
                Arguments.of("{Robert and Sons, Inc.}", new Author(null, null, null, "{Robert and Sons, Inc.}", null)),
                Arguments.of("al-Ṣāliḥ, Abdallāh", new Author("Abdallāh", "A.", null, "al-Ṣāliḥ", null)),
                Arguments.of("de la Vallée Poussin, Jean Charles Gabriel", new Author("Jean Charles Gabriel", "J. C. G.", "de la", "Vallée Poussin", null)),
                Arguments.of("de la Vallée Poussin, J. C. G.", new Author("J. C. G.", "J. C. G.", "de la", "Vallée Poussin", null)),
                Arguments.of("{K}ent-{B}oswell, E. S.", new Author("E. S.", "E. S.", null, "{K}ent-{B}oswell", null)),
                Arguments.of("Uhlenhaut, N Henriette", new Author("N Henriette", "N. H.", null, "Uhlenhaut", null)),
                Arguments.of("Nu{\\~{n}}ez, Jose", new Author("Jose", "J.", null, "Nu{\\~{n}}ez", null)),
                // parseAuthorWithFirstNameAbbreviationContainingUmlaut
                Arguments.of("{\\OE}rjan Umlauts", new Author("{\\OE}rjan", "{\\OE}.", null, "Umlauts", null)),
                Arguments.of("{Company Name, LLC}", new Author("", "", null, "{Company Name, LLC}", null)),
                Arguments.of("{Society of Automotive Engineers}", new Author("", "", null, "{Society of Automotive Engineers}", null)),

                // Demonstrate the "von" part parsing of a non-braced name
                Arguments.of("Society of Automotive Engineers", new Author("Society", "S.", "of", "Automotive Engineers", null))
        );
    }

    @ParameterizedTest
    @MethodSource
    void parseSingleAuthorCorrectly(String authorsString, Author authorsParsed) {
        assertEquals(AuthorList.of(authorsParsed), parser.parse(authorsString));
    }

    @Test
    void dashedNamesWithoutSpaceNormalized() {
        assertEquals(Optional.of("Z. Yao and D. S. Weld and W-P. Chen and H. Sun"), AuthorListParser.normalizeSimply("Z. Yao, D. S. Weld, W-P. Chen, and H. Sun"));
    }

    @Test
    void dashedNamesWithSpaceNormalized() {
        assertEquals(Optional.of("Z. Yao and D. S. Weld and W.-P. Chen and H. Sun"), AuthorListParser.normalizeSimply("Z. Yao, D. S. Weld, W.-P. Chen, and H. Sun"));
    }

    private static Stream<Arguments> parseMultipleCorrectly() {
        return Stream.of(
                Arguments.of(
                        AuthorList.of(
                                new Author("Alexander", "A.", null, "Artemenko", null),
                                Author.OTHERS
                        ),
                        "Alexander Artemenko and others"),
                Arguments.of(
                        AuthorList.of(
                                new Author("I.", "I.", null, "Podadera", null),
                                new Author("J. M.", "J. M.", null, "Carmona", null),
                                new Author("A.", "A.", null, "Ibarra", null),
                                new Author("J.", "J.", null, "Molla", null)
                        ),
                        "I. Podadera, J. M. Carmona, A. Ibarra, and J. Molla"),
                Arguments.of(AuthorList.of(
                                new Author("Vivian", "V.", null, "U", null),
                                new Author("Thomas", "T.", null, "Lai", null)
                        ),
                        "U, Vivian and Lai, Thomas"
                ),
                Arguments.of(
                        AuthorList.of(
                                new Author("I.", "I.", null, "Podadera", null),
                                new Author("J. M.", "J. M.", null, "Carmona", null),
                                new Author("A.", "A.", null, "Ibarra", null),
                                new Author("J.", "J.", null, "Molla", null)
                        ),
                        "I. Podadera      J. M. Carmona     A. Ibarra     J. Molla"),
                Arguments.of(
                        AuthorList.of(
                                new Author("Alexander", "A.", null, "Artemenko", null),
                                new Author("I.", "I.", null, "Podadera", null),
                                new Author("J. M.", "J. M.", null, "Carmona", null)
                        ),
                        """
                                Alexander Artemenko
                                I. Podadera
                                J. M. Carmona
                                """),
                Arguments.of(
                        AuthorList.of(
                                new Author("First1", "F.", null, "Last1", null),
                                new Author("First2", "F.", null, "Last2", null),
                                new Author("First3", "F.", null, "Last3", null)
                        ),
                        """
                                First1 Last1
                                    First2 Last2
                                    First3 Last3
                                """)
        );
    }

    @ParameterizedTest
    @MethodSource
    void parseMultipleCorrectly(AuthorList expected, String authorsString) {
        assertEquals(expected, parser.parse(authorsString));
    }
}
