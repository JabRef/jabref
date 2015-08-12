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

import net.sf.jabref.imports.BibtexParser;
import net.sf.jabref.imports.ParserResult;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class BibtexDatabaseTest {

    @Before
    public void setup() {
        Globals.prefs = JabRefPreferences.getInstance(); // set preferences for this test
    }

    @After
    public void teardown() {
        Globals.prefs = null;
    }

    /**
     * Some basic test cases for resolving strings.
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    @Test
    public void testResolveStrings() throws IOException {

        ParserResult result = BibtexParser.parse(new FileReader("src/test/resources/net/sf/jabref/util/twente.bib"));

        BibtexDatabase db = result.getDatabase();

        Assert.assertEquals("Arvind", db.resolveForStrings("#Arvind#"));
        Assert.assertEquals("Patterson, David", db.resolveForStrings("#Patterson#"));
        Assert.assertEquals("Arvind and Patterson, David", db.resolveForStrings("#Arvind# and #Patterson#"));

        // Strings that are not found return just the given string.
        Assert.assertEquals("#unknown#", db.resolveForStrings("#unknown#"));

    }

}
