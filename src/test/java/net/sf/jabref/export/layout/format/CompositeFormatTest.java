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

public class CompositeFormatTest {

    @Test
    public void testComposite() {

        {
            LayoutFormatter f = new CompositeFormat();
            Assert.assertEquals("No Change", f.format("No Change"));
        }
        {
            LayoutFormatter f = new CompositeFormat(new LayoutFormatter[] {new LayoutFormatter() {

                @Override
                public String format(String fieldText) {
                    return fieldText + fieldText;
                }

            }, new LayoutFormatter() {

                @Override
                public String format(String fieldText) {
                    return "A" + fieldText;
                }

            }, new LayoutFormatter() {

                @Override
                public String format(String fieldText) {
                    return "B" + fieldText;
                }

            }});

            Assert.assertEquals("BAff", f.format("f"));
        }

        LayoutFormatter f = new CompositeFormat(new AuthorOrgSci(),
                new NoSpaceBetweenAbbreviations());
        LayoutFormatter first = new AuthorOrgSci();
        LayoutFormatter second = new NoSpaceBetweenAbbreviations();

        Assert.assertEquals(second.format(first.format("John Flynn and Sabine Gartska")), f.format("John Flynn and Sabine Gartska"));
        Assert.assertEquals(second.format(first.format("Sa Makridakis and Sa Ca Wheelwright and Va Ea McGee")), f.format("Sa Makridakis and Sa Ca Wheelwright and Va Ea McGee"));
    }

}
