/*
 * Copyright (C) 2016 Jabref-Team
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

package net.sf.jabref.openoffice;

import static org.junit.Assert.*;

import org.junit.Test;


public class OOPreFormatterTest {

    @Test
    public void testPlainFormat() {
        assertEquals("aaa", new OOPreFormatter().format("aaa"));
    }

    @Test
    public void testFormatUmlaut() {
        assertEquals("ä", new OOPreFormatter().format("{\\\"{a}}"));
        assertEquals("Ä", new OOPreFormatter().format("{\\\"{A}}"));
    }

    @Test
    public void testSpecialCommands() {
        assertEquals("å", new OOPreFormatter().format("{\\aa}"));
        assertEquals("bb", new OOPreFormatter().format("{\\bb}"));
        assertEquals("å a", new OOPreFormatter().format("\\aa a"));
        assertEquals("bb a", new OOPreFormatter().format("\\bb a"));
    }

    @Test
    public void testFormatStripLatexCommands() {
        assertEquals("-", new OOPreFormatter().format("\\mbox{-}"));
    }

    @Test
    public void testFormatting() {
        assertEquals("<em>kkk</em>", new OOPreFormatter().format("\\textit{kkk}"));
        assertEquals("<em>kkk</em>", new OOPreFormatter().format("\\emph{kkk}"));
        assertEquals("<b>kkk</b>", new OOPreFormatter().format("\\textbf{kkk}"));
    }
}
