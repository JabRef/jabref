/*
 * Copyright (C) 2003-2006 JabRef contributors.
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
package net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.LayoutFormatter;

import org.junit.Assert;
import org.junit.Test;

/**
 * Is the save as the AuthorLastFirstAbbreviator.
 */
public class AuthorAbbreviatorTest {

    @Test
    public void testFormat() {

        LayoutFormatter a = new AuthorLastFirstAbbreviator();
        LayoutFormatter b = new AuthorAbbreviator();

        Assert.assertEquals(b.format(""), a.format(""));
        Assert.assertEquals(b.format("Someone, Van Something"), a.format("Someone, Van Something"));
        Assert.assertEquals(b.format("Smith, John"), a.format("Smith, John"));
        Assert.assertEquals(b.format("von Neumann, John and Smith, John and Black Brown, Peter"), a
                .format("von Neumann, John and Smith, John and Black Brown, Peter"));

    }

}
