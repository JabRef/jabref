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

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.layout.format.NameFormatter;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RegExpFileSearchTests {

    private static final String filesDirectory = "src/test/resources/net/sf/jabref/imports/unlinkedFilesTestFolder";
    private BibDatabase database;
    private BibEntry entry;

    @Before
    public void setUp() throws IOException {
        Globals.prefs = JabRefPreferences.getInstance();

        StringReader reader = new StringReader(
                "@ARTICLE{HipKro03," + "\n" + "  author = {Eric von Hippel and Georg von Krogh}," + "\n"
                        + "  title = {Open Source Software and the \"Private-Collective\" Innovation Model: Issues for Organization Science},"
                        + "\n" + "  journal = {Organization Science}," + "\n" + "  year = {2003}," + "\n"
                        + "  volume = {14}," + "\n" + "  pages = {209--223}," + "\n" + "  number = {2}," + "\n"
                        + "  address = {Institute for Operations Research and the Management Sciences (INFORMS), Linthicum, Maryland, USA},"
                        + "\n" + "  doi = {http://dx.doi.org/10.1287/orsc.14.2.209.14992}," + "\n"
                        + "  issn = {1526-5455}," + "\n" + "  publisher = {INFORMS}" + "\n" + "}");

        BibtexParser parser = new BibtexParser(reader);
        ParserResult result = null;

        result = parser.parse();

        database = result.getDatabase();
        entry = database.getEntryByKey("HipKro03");

        Assert.assertNotNull(database);
        Assert.assertNotNull(entry);
    }

    @Test
    public void testFindFiles() {
        //given
        List<BibEntry> entries = new ArrayList<>();
        BibEntry localEntry = new BibEntry("123", BibtexEntryTypes.ARTICLE.getName());
        localEntry.setCiteKey("pdfInDatabase");
        localEntry.setField("year", "2001");
        entries.add(localEntry);

        List<String> extensions = Arrays.asList("pdf");

        List<File> dirs = Arrays.asList(new File(filesDirectory));

        //when
        Map<BibEntry, java.util.List<File>> result = RegExpFileSearch.findFilesForSet(entries, extensions, dirs,
                "**/[bibtexkey].*\\\\.[extension]");

        //then
        assertEquals(1, result.keySet().size());
    }

    @Test
    public void testFieldAndFormat() {
        assertEquals("Eric von Hippel and Georg von Krogh",
                RegExpFileSearch.getFieldAndFormat("[author]", entry, database));

        assertEquals("Eric von Hippel and Georg von Krogh",
                RegExpFileSearch.getFieldAndFormat("author", entry, database));

        assertEquals("", RegExpFileSearch.getFieldAndFormat("[unknownkey]", entry, database));

        assertEquals("", RegExpFileSearch.getFieldAndFormat("[:]", entry, database));

        assertEquals("", RegExpFileSearch.getFieldAndFormat("[:lower]", entry, database));

        assertEquals("eric von hippel and georg von krogh",
                RegExpFileSearch.getFieldAndFormat("[author:lower]", entry, database));

        assertEquals("HipKro03", RegExpFileSearch.getFieldAndFormat("[bibtexkey]", entry, database));

        assertEquals("HipKro03", RegExpFileSearch.getFieldAndFormat("[bibtexkey:]", entry, database));
    }

    @Test
    @Ignore
    public void testUserFieldAndFormat() {

        List<String> names = Globals.prefs.getStringList(NameFormatter.NAME_FORMATER_KEY);

        List<String> formats = Globals.prefs.getStringList(NameFormatter.NAME_FORMATTER_VALUE);

        try {

            List<String> f = new LinkedList<>(formats);
            List<String> n = new LinkedList<>(names);

            n.add("testMe123454321");
            f.add("*@*@test");

            Globals.prefs.putStringList(NameFormatter.NAME_FORMATER_KEY, n);
            Globals.prefs.putStringList(NameFormatter.NAME_FORMATTER_VALUE, f);

            assertEquals("testtest", RegExpFileSearch.getFieldAndFormat("[author:testMe123454321]", entry, database));

        } finally {
            Globals.prefs.putStringList(NameFormatter.NAME_FORMATER_KEY, names);
            Globals.prefs.putStringList(NameFormatter.NAME_FORMATTER_VALUE, formats);
        }
    }

    @Test
    public void testExpandBrackets() {

        assertEquals("", RegExpFileSearch.expandBrackets("", entry, database));

        assertEquals("dropped", RegExpFileSearch.expandBrackets("drop[unknownkey]ped", entry, database));

        assertEquals("Eric von Hippel and Georg von Krogh",
                RegExpFileSearch.expandBrackets("[author]", entry, database));

        assertEquals("Eric von Hippel and Georg von Krogh are two famous authors.",
                RegExpFileSearch.expandBrackets("[author] are two famous authors.", entry, database));

        assertEquals("Eric von Hippel and Georg von Krogh are two famous authors.",
                RegExpFileSearch.expandBrackets("[author] are two famous authors.", entry, database));

        assertEquals(
                "Eric von Hippel and Georg von Krogh have published Open Source Software and the \"Private-Collective\" Innovation Model: Issues for Organization Science in Organization Science.",
                RegExpFileSearch.expandBrackets("[author] have published [title] in [journal].", entry, database));
    }

    @After
    public void tearDown(){
        Globals.prefs = null;
    }

}
