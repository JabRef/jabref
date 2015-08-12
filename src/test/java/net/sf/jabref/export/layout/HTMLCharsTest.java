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
package net.sf.jabref.export.layout;

import net.sf.jabref.export.layout.format.HTMLChars;

import org.junit.Assert;
import org.junit.Test;

public class HTMLCharsTest {

    @Test
    public void testBasicFormat() {

        LayoutFormatter layout = new HTMLChars();

        Assert.assertEquals("", layout.format(""));

        Assert.assertEquals("hallo", layout.format("hallo"));

        Assert.assertEquals("Réflexions sur le timing de la quantité", layout
                .format("Réflexions sur le timing de la quantité"));

        Assert.assertEquals("h&aacute;llo", layout.format("h\\'allo"));

        Assert.assertEquals("&#305; &#305;", layout.format("\\i \\i"));
        Assert.assertEquals("&#305;", layout.format("\\i"));
        Assert.assertEquals("&#305;", layout.format("\\{i}"));
        Assert.assertEquals("&#305;&#305;", layout.format("\\i\\i"));

        Assert.assertEquals("&#319;&#305;", layout.format("\\Lmidot\\i"));

        Assert.assertEquals("&ntilde; &ntilde; &iacute; &#305; &#305;", layout.format("\\~{n} \\~n \\'i \\i \\i"));
    }

    @Test
    public void testLaTeXHighlighting() {

        LayoutFormatter layout = new HTMLChars();

        Assert.assertEquals("<em>hallo</em>", layout.format("\\emph{hallo}"));
        Assert.assertEquals("<em>hallo</em>", layout.format("{\\emph hallo}"));

        Assert.assertEquals("<em>hallo</em>", layout.format("\\textit{hallo}"));
        Assert.assertEquals("<em>hallo</em>", layout.format("{\\textit hallo}"));

        Assert.assertEquals("<b>hallo</b>", layout.format("\\textbf{hallo}"));
        Assert.assertEquals("<b>hallo</b>", layout.format("{\\textbf hallo}"));
    }

    /*
     * Is missing a lot of test cases for the individual chars...
     */
}