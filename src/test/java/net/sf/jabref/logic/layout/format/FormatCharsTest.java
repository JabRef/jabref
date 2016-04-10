/*
 * Copyright (C) 2015 Jabref-Team
 *
 * All programs in this directory and subdirectories are published under the GNU
 * General Public License as described below.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * Further information about the GNU GPL is available at:
 * http://www.gnu.org/copyleft/gpl.ja.html
 *
 */

package net.sf.jabref.logic.layout.format;

import static org.junit.Assert.*;

import org.junit.Test;

import net.sf.jabref.logic.formatter.bibtexfields.LatexToUnicodeFormatter;
import net.sf.jabref.logic.layout.LayoutFormatter;


public class FormatCharsTest {

    @Test
    public void testPlainFormat() {
        assertEquals("aaa", new LatexToUnicodeFormatter().format("aaa"));
    }

    @Test
    public void testFormatUmlaut() {
        assertEquals("ä", new LatexToUnicodeFormatter().format("{\\\"{a}}"));
        assertEquals("Ä", new LatexToUnicodeFormatter().format("{\\\"{A}}"));
    }

    @Test
    public void testFormatStripLatexCommands() {
        assertEquals("-", new LatexToUnicodeFormatter().format("\\mbox{-}"));
    }

    @Test
    public void testEquations() {
        LayoutFormatter layout = new FormatChars();

        assertEquals("$", layout.format("\\$"));
        assertEquals("σ", layout.format("$\\sigma$"));
        assertEquals("A 32\u00A0mA ΣΔ-modulator",
                layout.format("A 32~{mA} {$\\Sigma\\Delta$}-modulator"));
    }

}
