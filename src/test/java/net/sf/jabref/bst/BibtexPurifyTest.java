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

public class BibtexPurifyTest {

    @Test
    public void testPurify() {
        assertPurify("i", "i");
        assertPurify("0I  ", "0I~ ");
        assertPurify("Hi Hi ", "Hi Hi ");
        assertPurify("oe", "{\\oe}");
        assertPurify("Hi oeHi ", "Hi {\\oe   }Hi ");
        assertPurify("Jonathan Meyer and Charles Louis Xavier Joseph de la Vallee Poussin", "Jonathan Meyer and Charles Louis Xavier Joseph de la Vall{\\'e}e Poussin");
        assertPurify("e", "{\\'e}");
        assertPurify("Edouard Masterly", "{\\'{E}}douard Masterly");
        assertPurify("Ulrich Underwood and Ned Net and Paul Pot", "Ulrich {\\\"{U}}nderwood and Ned {\\~N}et and Paul {\\={P}}ot");
    }

    private void assertPurify(final String string, final String string2) {
        Assert.assertEquals(string, BibtexPurify.purify(string2, new Warn() {

            @Override
            public void warn(String s) {
                Assert.fail("Should not Warn (" + s + ")! purify should be " + string + " for " + string2);
            }
        }));
    }
}
