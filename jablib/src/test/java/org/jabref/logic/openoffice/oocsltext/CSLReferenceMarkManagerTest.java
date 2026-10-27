package org.jabref.logic.openoffice.oocsltext;

import java.util.List;
import java.util.stream.Stream;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
class CSLReferenceMarkManagerTest {

    @ParameterizedTest
    @MethodSource
    void renumberingPreservesMarkup(String currentText, List<Integer> newNumbers, String expectedText) {
        String updatedText = CSLReferenceMarkManager.getUpdatedCitationTextWithNewNumbers(currentText, newNumbers);

        assertEquals(expectedText, updatedText);
    }

    static Stream<Arguments> renumberingPreservesMarkup() {
        return Stream.of(
                Arguments.of(
                        "<span lang=\"zxx\">(<i>1</i>)</span>",
                        List.of(3),
                        "<span lang=\"zxx\">(<i>3</i>)</span>"),
                Arguments.of(
                        "<span lang=\"zxx\"><sup>1</sup>, <sup>2</sup></span>",
                        List.of(4, 7),
                        "<span lang=\"zxx\"><sup>4</sup>, <sup>7</sup></span>"),
                Arguments.of(
                        "<span lang=\"zxx\">[<b>1</b>]</span>",
                        List.of(5),
                        "<span lang=\"zxx\">[<b>5</b>]</span>"),
                Arguments.of(
                        "<span lang=\"zxx\"><sub>1</sub></span>",
                        List.of(9),
                        "<span lang=\"zxx\"><sub>9</sub></span>"),
                Arguments.of(
                        "<span oo:CharStyleName=\"Citation 1\"><i>1</i></span>",
                        List.of(8),
                        "<span oo:CharStyleName=\"Citation 1\"><i>8</i></span>"),
                Arguments.of(
                        "<span lang=\"zxx\">(<i><sup>1</sup></i>; <b>2</b>)</span>",
                        List.of(6, 10),
                        "<span lang=\"zxx\">(<i><sup>6</sup></i>; <b>10</b>)</span>"));
    }
}
