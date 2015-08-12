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
import org.junit.Ignore;
import org.junit.Test;

public class DOICheckTest {

    @Test
    @Ignore
    public void testFormat() {
        LayoutFormatter lf = new DOICheck();

        Assert.assertEquals("", lf.format(""));
        Assert.assertEquals(null, lf.format(null));

        Assert.assertEquals("http://dx.doi.org/10.1000/ISBN1-900512-44-0", lf
                .format("10.1000/ISBN1-900512-44-0"));
        Assert.assertEquals("http://dx.doi.org/10.1000/ISBN1-900512-44-0", lf
                .format("http://dx.doi.org/10.1000/ISBN1-900512-44-0"));

        Assert.assertEquals("http://doi.acm.org/10.1000/ISBN1-900512-44-0", lf
                .format("http://doi.acm.org/10.1000/ISBN1-900512-44-0"));

        Assert.assertEquals("http://doi.acm.org/10.1145/354401.354407", lf
                .format("http://doi.acm.org/10.1145/354401.354407"));
        Assert.assertEquals("http://dx.doi.org/10.1145/354401.354407", lf.format("10.1145/354401.354407"));

        // Works even when having a / at the front
        Assert.assertEquals("http://dx.doi.org/10.1145/354401.354407", lf.format("/10.1145/354401.354407"));

        // Obviously a wrong doi, but we still accept it.
        Assert.assertEquals("http://dx.doi.org/10", lf.format("10"));

        // Obviously a wrong doi, but we still accept it.
        Assert.assertEquals("1", lf.format("1"));
    }

}
