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
package net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.LayoutFormatter;

import org.junit.Assert;
import org.junit.Test;

public class AuthorLastFirstTest {

    @Test
    public void testFormat() {
        LayoutFormatter a = new AuthorLastFirst();

        // Empty case
        Assert.assertEquals("", a.format(""));

        // Single Names
        Assert.assertEquals("Someone, Van Something", a.format("Van Something Someone"));

        // Two names
        Assert.assertEquals("von Neumann, John and Black Brown, Peter", a
                .format("John von Neumann and Black Brown, Peter"));

        // Three names
        Assert.assertEquals("von Neumann, John and Smith, John and Black Brown, Peter", a
                .format("von Neumann, John and Smith, John and Black Brown, Peter"));

        Assert.assertEquals("von Neumann, John and Smith, John and Black Brown, Peter", a
                .format("John von Neumann and John Smith and Black Brown, Peter"));
    }

}
