package org.jabref.logic.bst;



import org.junit.jupiter.api.Test;

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

    private void assertBibtexWidth(final int i, final String string) {
        assertEquals(i, BibtexWidth.width(string));
    }

    @Test
    public void testWidth() {

        assertBibtexWidth(278, "i");

        assertBibtexWidth(1639, "0I~ ");

        assertBibtexWidth(2612, "Hi Hi ");

        assertBibtexWidth(778, "{\\oe}");

        assertBibtexWidth(3390, "Hi {\\oe   }Hi ");

        assertBibtexWidth(444, "{\\'e}");

        assertBibtexWidth(19762, "Ulrich {\\\"{U}}nderwood and Ned {\\~N}et and Paul {\\={P}}ot");

        assertBibtexWidth(7861, "{\\'{E}}douard Masterly");

        assertBibtexWidth(30514, "Jonathan Meyer and Charles Louis Xavier Joseph de la Vall{\\'e}e Poussin");

    }

    @Test
    public void testGetCharWidth() {
        assertEquals(500, BibtexWidth.getCharWidth('0'));
        assertEquals(361, BibtexWidth.getCharWidth('I'));
        assertEquals(500, BibtexWidth.getCharWidth('~'));
        assertEquals(500, BibtexWidth.getCharWidth('}'));
        assertEquals(278, BibtexWidth.getCharWidth(' '));
    }
}
