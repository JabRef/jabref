package org.jabref.languageserver.util;

import java.util.List;
import java.util.stream.Stream;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
class LspParserHandlerTest {

    static Stream<Arguments> getBibliographiesFromFrontMatter() {
        return Stream.of(
                Arguments.of(List.of("ws-2026-se.bib"), """
                        ---
                        title: "Seminar Topics"
                        bibliography: ws-2026-se.bib
                        ---

                        Text [@Nygard2011]
                        """),
                Arguments.of(List.of("a.bib", "sub/b.bib"), """
                        ---
                        bibliography:
                          - a.bib
                          - sub/b.bib
                        ...
                        """),
                Arguments.of(List.of("a.bib", "b.bib"), """
                        ---
                        bibliography: [a.bib, "b.bib"]
                        ---
                        """),
                Arguments.of(List.of(), """
                        ---
                        title: No bibliography
                        ---
                        """),
                Arguments.of(List.of(), """
                        # No front matter

                        ---
                        bibliography: a.bib
                        ---
                        """)
        );
    }

    @ParameterizedTest
    @MethodSource
    void getBibliographiesFromFrontMatter(List<String> expected, String markdown) {
        assertEquals(expected, LspParserHandler.getBibliographiesFromFrontMatter(markdown));
    }
}
