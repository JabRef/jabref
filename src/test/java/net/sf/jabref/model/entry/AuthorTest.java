/*
 * Copyright (C) 2003-2016 JabRef contributors.
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

package net.sf.jabref.model.entry;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AuthorTest {

    @Test
    public void addDotIfAbbreviationAddDot() {
        assertEquals("O.", Author.addDotIfAbbreviation("O"));
        assertEquals("A. O.", Author.addDotIfAbbreviation("AO"));
        assertEquals("A. O.", Author.addDotIfAbbreviation("AO."));
        assertEquals("A. O.", Author.addDotIfAbbreviation("A.O."));
        assertEquals("A.-O.", Author.addDotIfAbbreviation("A-O"));
    }

    @Test
    public void addDotIfAbbreviationDoNotAddDot() {
        assertEquals("O.", Author.addDotIfAbbreviation("O."));
        assertEquals("A. O.", Author.addDotIfAbbreviation("A. O."));
        assertEquals("A.-O.", Author.addDotIfAbbreviation("A.-O."));
        assertEquals("O. Moore", Author.addDotIfAbbreviation("O. Moore"));
        assertEquals("A. O. Moore", Author.addDotIfAbbreviation("A. O. Moore"));
        assertEquals("O. von Moore", Author.addDotIfAbbreviation("O. von Moore"));
        assertEquals("A.-O. Moore", Author.addDotIfAbbreviation("A.-O. Moore"));
        assertEquals("Moore, O.", Author.addDotIfAbbreviation("Moore, O."));
        assertEquals("Moore, O., Jr.", Author.addDotIfAbbreviation("Moore, O., Jr."));
        assertEquals("Moore, A. O.", Author.addDotIfAbbreviation("Moore, A. O."));
        assertEquals("Moore, A.-O.", Author.addDotIfAbbreviation("Moore, A.-O."));
        assertEquals("MEmre", Author.addDotIfAbbreviation("MEmre"));
        assertEquals("{\\'{E}}douard", Author.addDotIfAbbreviation("{\\'{E}}douard"));
        assertEquals("J{\\\"o}rg", Author.addDotIfAbbreviation("J{\\\"o}rg"));
        assertEquals("Moore, O. and O. Moore", Author.addDotIfAbbreviation("Moore, O. and O. Moore"));
        assertEquals("Moore, O. and O. Moore and Moore, O. O.",
                Author.addDotIfAbbreviation("Moore, O. and O. Moore and Moore, O. O."));
    }
    
}
