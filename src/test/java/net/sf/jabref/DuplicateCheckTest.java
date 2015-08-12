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
package net.sf.jabref;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Nov 9, 2007
 * Time: 7:04:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class DuplicateCheckTest {

    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test
    @Ignore
    public void testDuplicateDetection() {
        BibtexEntry one = new BibtexEntry(IdGenerator.next(), BibtexEntryTypes.ARTICLE);

        BibtexEntry two = new BibtexEntry(IdGenerator.next(), BibtexEntryTypes.ARTICLE);

        one.setField("author", "Billy Bob");
        two.setField("author", "Billy Bob");
        Assert.assertTrue(DuplicateCheck.isDuplicate(one, two));

        //TODO algorithm things bob and joyce is the same with high accuracy
        two.setField("author", "James Joyce");
        Assert.assertFalse(DuplicateCheck.isDuplicate(one, two));

        two.setField("author", "Billy Bob");
        two.setType(BibtexEntryTypes.BOOK);
        Assert.assertFalse(DuplicateCheck.isDuplicate(one, two));

        two.setType(BibtexEntryTypes.ARTICLE);
        one.setField("year", "2005");
        two.setField("year", "2005");
        one.setField("title", "A title");
        two.setField("title", "A title");
        one.setField("journal", "A");
        two.setField("journal", "A");
        one.setField("number", "1");
        two.setField("number", "1");
        one.setField("volume", "21");
        two.setField("volume", "21");
        Assert.assertTrue(DuplicateCheck.isDuplicate(one, two));

        two.setField("volume", "22");
        Assert.assertTrue(DuplicateCheck.isDuplicate(one, two));

        two.setField("title", "Another title");
        two.setField("journal", "B");
        Assert.assertFalse(DuplicateCheck.isDuplicate(one, two));
    }

}
