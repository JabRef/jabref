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
package net.sf.jabref.logic.util;

import net.sf.jabref.logic.util.CaseChangers;
import org.junit.Assert;
import org.junit.Test;

public class CaseChangersTest {

    @Test
    public void testNumberOfModes() {
        Assert.assertEquals("lower", CaseChangers.LOWER.getName());
        Assert.assertEquals("UPPER", CaseChangers.UPPER.getName());
        Assert.assertEquals("Upper first", CaseChangers.UPPER_FIRST.getName());
        Assert.assertEquals("Upper Each First", CaseChangers.UPPER_EACH_FIRST.getName());
        Assert.assertEquals("Title", CaseChangers.TITLE.getName());
    }

    @Test
    public void testChangeCaseLower() {
        Assert.assertEquals("", CaseChangers.LOWER.changeCase(""));
        Assert.assertEquals("lower", CaseChangers.LOWER.changeCase("LOWER"));
    }

    @Test
    public void testChangeCaseUpper() {
        Assert.assertEquals("", CaseChangers.UPPER.changeCase(""));
        Assert.assertEquals("LOWER", CaseChangers.UPPER.changeCase("LOWER"));
        Assert.assertEquals("UPPER", CaseChangers.UPPER.changeCase("upper"));
        Assert.assertEquals("UPPER", CaseChangers.UPPER.changeCase("UPPER"));
    }

    @Test
    public void testChangeCaseUpperFirst() {
        Assert.assertEquals("", CaseChangers.UPPER_FIRST.changeCase(""));
        Assert.assertEquals("Upper first", CaseChangers.UPPER_FIRST.changeCase("upper First"));
    }

    @Test
    public void testChangeCaseUpperEachFirst() {
        Assert.assertEquals("", CaseChangers.UPPER_EACH_FIRST.changeCase(""));
        Assert.assertEquals("Upper Each First", CaseChangers.UPPER_EACH_FIRST.changeCase("upper each First"));
    }

    @Test
    public void testChangeCaseTitle() {
        Assert.assertEquals("", CaseChangers.TITLE.changeCase(""));
        Assert.assertEquals("Upper Each First", CaseChangers.TITLE.changeCase("upper each first"));
        Assert.assertEquals("An Upper Each First And", CaseChangers.TITLE.changeCase("an upper each first and"));
        Assert.assertEquals("An Upper Each of the and First And", CaseChangers.TITLE.changeCase("an upper each of the and first and"));
    }
}
