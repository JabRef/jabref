/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.external;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RegExpFileSearchTests {

    private static final String filesDirectory = "src/test/resources/net/sf/jabref/imports/unlinkedFilesTestFolder";


    @Before
    public void setUp(){
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test
    public void testFindFiles() {
        //given
        List<BibEntry> entries = new ArrayList<>();
        BibEntry entry = new BibEntry("123", BibtexEntryTypes.ARTICLE.getName());
        entry.setField(BibEntry.KEY_FIELD, "pdfInDatabase");
        entry.setField("year", "2001");
        entries.add(entry);

        List<String> extensions = Arrays.asList("pdf");

        List<File> dirs = Arrays.asList(new File(filesDirectory));

        //when
        Map<BibEntry, java.util.List<File>> result = RegExpFileSearch.findFilesForSet(entries, extensions, dirs,
                "**/[bibtexkey].*\\\\.[extension]");

        //then
        assertEquals(1, result.keySet().size());
    }

    @After
    public void tearDown(){
        Globals.prefs = null;
    }

}
