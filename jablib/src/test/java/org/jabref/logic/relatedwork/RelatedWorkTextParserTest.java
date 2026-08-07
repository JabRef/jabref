package org.jabref.logic.relatedwork;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RelatedWorkTextParserTest {

    private RelatedWorkTextParser parser;

    @BeforeEach
    void setUp() {
        parser = new RelatedWorkTextParser();
    }

    @ParameterizedTest
    @MethodSource
    void parseRelatedWork(String input, List<RelatedWorkSnippet> expected) {
        assertEquals(expected, parser.parseRelatedWork(input));
    }

    private static Stream<Arguments> parseRelatedWork() {
        return Stream.of(
                Arguments.of(
                        "A study [1] on the Colombian context [2].",
                        List.of(new RelatedWorkSnippet("A study on the Colombian context.", "[1]"),
                                new RelatedWorkSnippet("A study on the Colombian context.", "[2]"))
                ),
                Arguments.of(
                        "A study on the Colombian context [1] and [2].",
                        List.of(
                                new RelatedWorkSnippet("A study on the Colombian context.", "[1]"),
                                new RelatedWorkSnippet("A study on the Colombian context.", "[2]")
                        )
                ),
                Arguments.of(
                        "Visually organize different experiments [1] [7] [8].",
                        List.of(
                                new RelatedWorkSnippet("Visually organize different experiments.", "[1]"),
                                new RelatedWorkSnippet("Visually organize different experiments.", "[7]"),
                                new RelatedWorkSnippet("Visually organize different experiments.", "[8]")
                        )
                ),
                Arguments.of(
                        "RQ1: explore past experiments [1] [8], i.e.",
                        List.of(
                                new RelatedWorkSnippet("RQ1: explore past experiments, i.e.", "[1]"),
                                new RelatedWorkSnippet("RQ1: explore past experiments, i.e.", "[8]")
                        )
                ),
                Arguments.of(
                        "A study on the Colombian context [1]. Another work focuses on Brazil [2].",
                        List.of(
                                new RelatedWorkSnippet("A study on the Colombian context.", "[1]"),
                                new RelatedWorkSnippet("Another work focuses on Brazil.", "[2]")
                        )
                ),
                Arguments.of(
                        "Background information without citations. A study on the Colombian context [1].",
                        List.of(
                                new RelatedWorkSnippet("A study on the Colombian context.", "[1]")
                        )
                ),
                Arguments.of(
                        """
                                A study on the Colombian context [1]. Another work focuses on Brazil [2] and [3].
                                """,
                        List.of(
                                new RelatedWorkSnippet("A study on the Colombian context.", "[1]"),
                                new RelatedWorkSnippet("Another work focuses on Brazil.", "[2]"),
                                new RelatedWorkSnippet("Another work focuses on Brazil.", "[3]")
                        )
                ),
                Arguments.of(
                        "A study on the Colombian context.",
                        List.of()
                ),
                Arguments.of(
                        "A sentence contains multiple citations [1-3]. Another sentence contains multiple citations [4, 7]. Blah blah [4-6, 9, 10].",
                        List.of(
                                new RelatedWorkSnippet("A sentence contains multiple citations.", "[1]"),
                                new RelatedWorkSnippet("A sentence contains multiple citations.", "[2]"),
                                new RelatedWorkSnippet("A sentence contains multiple citations.", "[3]"),
                                new RelatedWorkSnippet("Another sentence contains multiple citations.", "[4]"),
                                new RelatedWorkSnippet("Another sentence contains multiple citations.", "[7]"),
                                new RelatedWorkSnippet("Blah blah.", "[4]"),
                                new RelatedWorkSnippet("Blah blah.", "[5]"),
                                new RelatedWorkSnippet("Blah blah.", "[6]"),
                                new RelatedWorkSnippet("Blah blah.", "[9]"),
                                new RelatedWorkSnippet("Blah blah.", "[10]")
                        )
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void parseTextWithLineBreaks(String input, String expected) {
        assertEquals(expected, parser.getNormalText(input));
    }

    private static Stream<Arguments> parseTextWithLineBreaks() {
        return Stream.of(
                Arguments.of(
                        """
                                They are facing several chal-
                                lenges in this transformation described along the BAPO framework [80].
                                """,
                        "They are facing several challenges in this transformation described along the BAPO framework [80]."
                ),
                Arguments.of(
                        """
                                Colombia is a middle-income country
                                with a population of approximately 50 million [1].
                                """,
                        "Colombia is a middle-income country with a population of approximately 50 million [1]."
                )
        );
    }
}
