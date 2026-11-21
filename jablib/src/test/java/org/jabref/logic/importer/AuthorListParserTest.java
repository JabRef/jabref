package org.jabref.logic.importer;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Similar tests are available in [org.jabref.model.entry.AuthorListTest]
class AuthorListParserTest {

    AuthorListParser parser = new AuthorListParser();

    private static Stream<Arguments> parseSingleAuthorCorrectly() {
        return Stream.of(
                Arguments.of("王, 军", new Author("军", "军.", null, "王", null)),
                Arguments.of("हिंदी, परीक्षण", new Author("परीक्षण", "प.", null, "हिंदी", null)),
                Arguments.of("العربية, اختبار", new Author("اختبار", "ا.", null, "العربية", null)),
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
                // No tokenization within braces even with lowercase letters in the beginning of the words
                // (lowercase letters can be treated as starts of name prefixes, which changes how the author
                // last name is parsed). This case checks that the parser prioritises {} as token boundaries and
                // does not get sidetracked by the lowercase letters even in many words.
                Arguments.of("{society of automotive engineers}", new Author("", "", null, "{society of automotive engineers}", null)),

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
    void parseBiblatexExtendedNameFollowedByConventionalNames() {
        AuthorList authors = parser.parse("family=Hasselt, given=Hado P., prefix=van, useprefix=false and Guez, Arthur");

        assertEquals(2, authors.getNumberOfAuthors());
        assertEquals(Optional.of("Hasselt"), authors.getAuthor(0).getFamilyName());
        assertEquals(Optional.of("Hado P."), authors.getAuthor(0).getGivenName());
        assertEquals(Optional.of("van"), authors.getAuthor(0).getNamePrefix());
        assertEquals(Optional.of("Guez"), authors.getAuthor(1).getFamilyName());
        assertEquals(Optional.of("Arthur"), authors.getAuthor(1).getGivenName());
    }

    @Test
    void parseSingleBiblatexExtendedName() {
        Author author = parser.parse("given=Jean, prefix=de la, family=Rousse").getAuthor(0);

        assertEquals(Optional.of("Jean"), author.getGivenName());
        assertEquals(Optional.of("J."), author.getGivenNameAbbreviated());
        assertEquals(Optional.of("de la"), author.getNamePrefix());
        assertEquals(Optional.of("Rousse"), author.getFamilyName());
    }

    @Test
    void parseConventionalNameFollowedByBiblatexExtendedName() {
        AuthorList authors = parser.parse("Doe, Jane and given=Simon, family=Beumont");

        assertEquals(2, authors.getNumberOfAuthors());
        assertEquals(Optional.of("Doe"), authors.getAuthor(0).getFamilyName());
        assertEquals(Optional.of("Beumont"), authors.getAuthor(1).getFamilyName());
        assertEquals(Optional.of("Simon"), authors.getAuthor(1).getGivenName());
    }

    @Test
    void parseQuotedBiblatexExtendedFamilyContainingComma() {
        Author author = parser.parse("\"family={Robert and Sons, Inc.}\"").getAuthor(0);

        assertEquals(Optional.of("{Robert and Sons, Inc.}"), author.getFamilyName());
    }

    // https://github.com/plk/biblatex/blob/dev/doc/latex/biblatex/biblatex.tex#L3399-L3445
    private static Stream<Arguments> biblatexManualNames() {
        return Stream.of(
                Arguments.of("given=Hans, family=Harman and given=Simon, prefix=de, family=Beumont",
                        List.of("Harman", "Beumont")),
                Arguments.of("given=Jean, prefix=de la, prefix-i=d, family=Rousse", List.of("Rousse")),
                Arguments.of("given={Jean Pierre Simon}, given-i=JPS", List.of("")),
                Arguments.of("given={Jean Pierre}", List.of("")),
                Arguments.of("\"family={Robert and Sons, Inc.}\"", List.of("{Robert and Sons, Inc.}")),
                Arguments.of("Hans Harman and given=Simon, prefix=de, family=Beumont", List.of("Harman", "Beumont")),
                Arguments.of("nosortothers=true and Hans Harman and given=Simon, family=Beumont, prefix=de, useprefix=true",
                        List.of("Harman", "Beumont")),
                Arguments.of("id=person1, given=Simon, family=Beumont", List.of("Beumont")),
                Arguments.of("id=person1, given=Simone, family=Beumont", List.of("Beumont")));
    }

    @ParameterizedTest
    @MethodSource("biblatexManualNames")
    void parsesBiblatexManualExamples(String input, List<String> families) {
        AuthorList authors = parser.parse(input);

        assertEquals(families, authors.getAuthors().stream().map(author -> author.getFamilyName().orElse("")).toList());
        assertEquals(true, authors.hasValidExtendedNames());
        assertEquals(authors, parser.parse(authors.getAsBibtex()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"family=Smith   Jones", "family=Smith\nJones", "\"family=Smith and Jones\""})
    void keepsProtectedFamilyTogether(String input) {
        AuthorList authors = parser.parse(input);

        assertEquals(1, authors.getNumberOfAuthors());
        assertEquals(input, authors.getAsBibtex());
    }

    @Test
    void dashedNamesWithoutSpaceNormalized() {
        assertEquals(Optional.of("Z. Yao and D. S. Weld and W-P. Chen and H. Sun"), AuthorListParser.normalizeSimply("Z. Yao, D. S. Weld, W-P. Chen, and H. Sun"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Z. Yao, D. S. Weld, et al.",
            "Z. Yao,D. S. Weld, et al.",
            "Z. Yao,   D. S. Weld, et al."
    })
    void etAlNormalizedToAndOthers(String input) {
        assertEquals(Optional.of("Z. Yao and D. S. Weld and others"), AuthorListParser.normalizeSimply(input));
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
                                new Author("Alexander", "A.", null, "Artemenko", null),
                                Author.OTHERS
                        ),
                        "Alexander Artemenko et al."),
                Arguments.of(
                        AuthorList.of(
                                new Author("John", "J.", null, "Smith", null),
                                new Author("Jane", "J.", null, "Doe", null),
                                Author.OTHERS
                        ),
                        "Smith, John and Doe, Jane, et al"),
                Arguments.of(
                        AuthorList.of(
                                new Author("J.", "J.", null, "Smith", null),
                                new Author("A.", "A.", null, "Doe", null),
                                Author.OTHERS
                        ),
                        "J. Smith and A. Doe ET AL."),
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
