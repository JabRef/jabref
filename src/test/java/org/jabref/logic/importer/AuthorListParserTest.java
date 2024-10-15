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
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void parseMultipleCorrectly(AuthorList expected, String authorsString) {
        assertEquals(expected, parser.parse(authorsString));
    }

    // Extended ests
    private static Stream<Arguments> parseExtendedFormatAuthors() {
        return Stream.of(
                Arguments.of(
                        "family=Hasselt, given=Hado P., prefix=van, useprefix=false and family=Guez, given=Arthur and family=Hessel, given=Matteo and family=Mnih, given=Volodymyr and family=Silver, given=David",
                        AuthorList.of(
                                new Author("Hado P.", "H. P.", null, "Hasselt", null),
                                new Author("Arthur", "A.", null, "Guez", null),
                                new Author("Matteo", "M.", null, "Hessel", null),
                                new Author("Volodymyr", "V.", null, "Mnih", null),
                                new Author("David", "D.", null, "Silver", null)
                        )
                ),
                Arguments.of(
                        "family=Hasselt, given=Hado P., prefix=van, useprefix=true and family=Smith, given=John",
                        AuthorList.of(
                                new Author("Hado P.", "H. P.", "van", "Hasselt", null),
                                new Author("John", "J.", null, "Smith", null)
                        )
                ),
                Arguments.of(
                        "family=Ree, given=Michiel, prefix=van der and family=Wiering, given=Marco",
                        AuthorList.of(
                                new Author("Michiel", "M.", "van der", "Ree", null),
                                new Author("Marco", "M.", null, "Wiering", null)
                        )
                ),
                Arguments.of(
                        "family=al-Ṣāliḥ, given=Abdallāh",
                        AuthorList.of(
                                new Author("Abdallāh", "A.", null, "al-Ṣāliḥ", null)
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void parseExtendedFormatAuthors(String authorsString, AuthorList expected) {
        assertEquals(expected, parser.parse(authorsString));
    }

    @Test
    void parseMixedFormatAuthors() {
        String authorsString = "family=Hasselt, given=Hado P., prefix=van, useprefix=false and Guez, Arthur";
        AuthorList expected = AuthorList.of(
                new Author("Hado P.", "H. P.", null, "Hasselt", null),
                new Author("Arthur", "A.", null, "Guez", null)
        );
        assertEquals(expected, parser.parse(authorsString));
    }

    @Test
    void parseAuthorWithUsePrefixFalse() {
        String authorsString = "family=Hasselt, given=Hado P., prefix=van, useprefix=false";
        AuthorList expected = AuthorList.of(
                new Author("Hado P.", "H. P.", null, "Hasselt", null)
        );
        assertEquals(expected, parser.parse(authorsString));
    }

    @Test
    void parseAuthorWithUsePrefixTrue() {
        String authorsString = "family=Hasselt, given=Hado P., prefix=van, useprefix=true";
        AuthorList expected = AuthorList.of(
                new Author("Hado P.", "H. P.", "van", "Hasselt", null)
        );
        assertEquals(expected, parser.parse(authorsString));
    }

    @Test
    void parseAuthorWithMissingComma() {
        String authorsString = "family=Hasselt, given=Hado P., prefix=van useprefix=false";
        // Since there is a missing comma between 'prefix=van' and 'useprefix=false', the parser should handle this gracefully
        // In this test, we expect that the parser will parse 'prefix=van useprefix=false' as one field
        AuthorList expected = AuthorList.of(
                new Author("Hado P.", "H. P.", "van useprefix=false", "Hasselt", null)
        );
        assertEquals(expected, parser.parse(authorsString));
    }

    @Test
    void parseExtendedFormatWithIncompleteData() {
        String authorsString = "family=Smith, given=John and family=Doe";
        AuthorList expected = AuthorList.of(
                new Author("John", "J.", null, "Smith", null),
                new Author(null, null, null, "Doe", null)
        );
        assertEquals(expected, parser.parse(authorsString));
    }
}

