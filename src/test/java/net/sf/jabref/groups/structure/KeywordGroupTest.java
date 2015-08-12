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
package net.sf.jabref.groups.structure;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class KeywordGroupTest {

    @Test
    public void testToString() {
        KeywordGroup group = new KeywordGroup("myExplicitGroup", "author","asdf", true, true,
                GroupHierarchyType.INDEPENDENT);
        assertEquals("KeywordGroup:myExplicitGroup;0;author;asdf;1;1;", group.toString());
    }

    @Test
    public void testToString2() {
        KeywordGroup group = new KeywordGroup("myExplicitGroup", "author","asdf", false, true,
                GroupHierarchyType.REFINING);
        assertEquals("KeywordGroup:myExplicitGroup;1;author;asdf;0;1;", group.toString());
    }

}