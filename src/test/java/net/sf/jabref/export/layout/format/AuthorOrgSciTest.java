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

public class AuthorOrgSciTest {

    @Test
    public void testOrgSci() {
        LayoutFormatter f = new AuthorOrgSci();

        Assert.assertEquals("Flynn, J., S. Gartska", f.format("John Flynn and Sabine Gartska"));
        Assert.assertEquals("Garvin, D. A.", f.format("David A. Garvin"));
        Assert.assertEquals("Makridakis, S., S. C. Wheelwright, V. E. McGee", f.format("Sa Makridakis and Sa Ca Wheelwright and Va Ea McGee"));

    }

    @Test
    public void testOrgSciPlusAbbreviation() {
        LayoutFormatter f = new CompositeFormat(new AuthorOrgSci(), new NoSpaceBetweenAbbreviations());
        Assert.assertEquals("Flynn, J., S. Gartska", f.format("John Flynn and Sabine Gartska"));
        Assert.assertEquals("Garvin, D.A.", f.format("David A. Garvin"));
        Assert.assertEquals("Makridakis, S., S.C. Wheelwright, V.E. McGee", f.format("Sa Makridakis and Sa Ca Wheelwright and Va Ea McGee"));
    }
}
