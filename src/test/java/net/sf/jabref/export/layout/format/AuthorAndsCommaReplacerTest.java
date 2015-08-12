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

public class AuthorAndsCommaReplacerTest {

    /**
     * Test method for
     * {@link net.sf.jabref.export.layout.format.AuthorAndsCommaReplacer#format(java.lang.String)}.
     */
    @Test
    public void testFormat() {

        LayoutFormatter a = new AuthorAndsCommaReplacer();

        // Empty case
        Assert.assertEquals("", a.format(""));

        // Single Names don't change
        Assert.assertEquals("Someone, Van Something", a.format("Someone, Van Something"));

        // Two names just an &
        Assert.assertEquals("John von Neumann & Peter Black Brown",
                a.format("John von Neumann and Peter Black Brown"));

        // Three names put a comma:
        Assert.assertEquals("von Neumann, John, Smith, John & Black Brown, Peter",
                a.format("von Neumann, John and Smith, John and Black Brown, Peter"));
    }
}
