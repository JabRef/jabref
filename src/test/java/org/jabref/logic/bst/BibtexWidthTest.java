package org.jabref.logic.bst;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * How to create these test using Bibtex:
 * <p/>
 * Execute this charWidth.bst with the following charWidth.aux:
 * <p/>
 * <p/>
 * <code>
 * ENTRY{}{}{}
 * FUNCTION{test}
 * {
 * "i" width$ int.to.str$ write$ newline$
 * "0I~ " width$ int.to.str$ write$ newline$
 * "Hi Hi " width$ int.to.str$ write$ newline$
 * "{\oe}" width$ int.to.str$ write$ newline$
 * "Hi {\oe   }Hi " width$ int.to.str$ write$ newline$
 * }
 * READ
 * EXECUTE{test}
 * </code>
 * <p/>
 * <code>
 * \bibstyle{charWidth}
 * \citation{canh05}
 * \bibdata{test}
 * \bibcite{canh05}{CMM{$^{+}$}05}
 * </code>
 */
public class BibtexWidthTest {

    @ParameterizedTest
    @MethodSource("provideTestWidth")
    public void testWidth(int i, String str) {
        assertEquals(i, BibtexWidth.width(str));
    }

    private static Stream<Arguments> provideTestWidth() {
        return Stream.of(
                Arguments.of(278, "i"),
                Arguments.of(1639, "0I~ "),
                Arguments.of(2612, "Hi Hi "),
                Arguments.of(778, "{\\oe}"),
                Arguments.of(3390, "Hi {\\oe   }Hi "),
                Arguments.of(444, "{\\'e}"),
                Arguments.of(19762, "Ulrich {\\\"{U}}nderwood and Ned {\\~N}et and Paul {\\={P}}ot"),
                Arguments.of(7861, "{\\'{E}}douard Masterly"),
                Arguments.of(30514, "Jonathan Meyer and Charles Louis Xavier Joseph de la Vall{\\'e}e Poussin")
        );
    }

    @ParameterizedTest
    @MethodSource("provideTestGetCharWidth")
    public void testGetCharWidth(int i, Character c) {
        assertEquals(i, BibtexWidth.getCharWidth(c));
    }

    private static Stream<Arguments> provideTestGetCharWidth() {
        return Stream.of(
                Arguments.of(500, '0'),
                Arguments.of(361, 'I'),
                Arguments.of(500, '~'),
                Arguments.of(500, '}'),
                Arguments.of(278, ' ')
        );
    }
}
