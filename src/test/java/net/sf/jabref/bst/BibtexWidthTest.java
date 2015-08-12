/**
 * Copyright (C) 2015 JabRef contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.bst;

import org.junit.Assert;
import org.junit.Test;

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

    void assertBibtexWidth(final int i, final String string) {
        Assert.assertEquals(i, BibtexWidth.width(string, new Warn() {

            @Override
            public void warn(String s) {
                Assert.fail("Should not Warn! Width should be " + i + " for " + string);
            }
        }));
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
        Assert.assertEquals(500, BibtexWidth.getCharWidth('0'));
        Assert.assertEquals(361, BibtexWidth.getCharWidth('I'));
        Assert.assertEquals(500, BibtexWidth.getCharWidth('~'));
        Assert.assertEquals(500, BibtexWidth.getCharWidth('}'));
        Assert.assertEquals(278, BibtexWidth.getCharWidth(' '));
    }
}
