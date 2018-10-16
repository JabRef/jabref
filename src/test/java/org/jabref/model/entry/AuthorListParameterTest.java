package org.jabref.model.entry;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuthorListParameterTest {

    private static Stream<Arguments> data() {

        return Stream.of(
                Arguments.of("王, 军", authorList(new Author("军", "军.", null, "王", null))),
                Arguments.of("Doe, John", authorList(new Author("John", "J.", null, "Doe", null))),
                Arguments.of("von Berlichingen zu Hornberg, Johann Gottfried",
                        authorList(new Author("Johann Gottfried", "J. G.", "von", "Berlichingen zu Hornberg", null))),
                //Arguments.of("Robert and Sons, Inc.", authorList(new Author(null, null, null, "Robert and Sons, Inc.", null))),
                //Arguments.of("al-Ṣāliḥ, Abdallāh", authorList(new Author("Abdallāh", "A.", null, "al-Ṣāliḥ", null))),
                Arguments.of("de la Vallée Poussin, Jean Charles Gabriel",
                        authorList(new Author("Jean Charles Gabriel", "J. C. G.", "de la", "Vallée Poussin", null))),
                Arguments.of("de la Vallée Poussin, J. C. G.",
                        authorList(new Author("J. C. G.", "J. C. G.", "de la", "Vallée Poussin", null))),
                Arguments.of("{K}ent-{B}oswell, E. S.", authorList(new Author("E. S.", "E. S.", null, "{K}ent-{B}oswell", null))));
    }

    private static AuthorList authorList(Author author) {
        return new AuthorList(Arrays.asList(author));
    }

    @ParameterizedTest
    @MethodSource("data")
    void parseCorrectly(String authorsString, AuthorList authorsParsed) {
        AuthorListParser parser = new AuthorListParser();
        assertEquals(authorsParsed, parser.parse(authorsString));
    }
}
