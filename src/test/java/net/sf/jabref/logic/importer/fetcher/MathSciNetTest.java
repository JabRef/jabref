/*
 * Copyright (C) 2003-2016 JabRef contributors.
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

package net.sf.jabref.logic.importer.fetcher;

import java.util.List;

import net.sf.jabref.logic.bibtex.FieldContentParserPreferences;
import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MathSciNetTest {

    MathSciNet fetcher;
    BibEntry ratiuEntry;

    @Before
    public void setUp() throws Exception {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class);
        when(importFormatPreferences.getFieldContentParserPreferences()).thenReturn(
                mock(FieldContentParserPreferences.class));
        fetcher = new MathSciNet(importFormatPreferences);

        ratiuEntry = new BibEntry();
        ratiuEntry.setType(BibtexEntryTypes.ARTICLE);
        ratiuEntry.setCiteKey("MR3537908");
        ratiuEntry.setField("author", "Chechkin, Gregory A. and Ratiu, Tudor S. and Romanov, Maxim S. and Samokhin, Vyacheslav N.");
        ratiuEntry.setField("title", "Existence and {U}niqueness {T}heorems for the {T}wo-{D}imensional {E}ricksen--{L}eslie {S}ystem");
        ratiuEntry.setField("journal", "Journal of Mathematical Fluid Mechanics");
        ratiuEntry.setField("volume", "18");
        ratiuEntry.setField("year", "2016");
        ratiuEntry.setField("number", "3");
        ratiuEntry.setField("pages", "571--589");
        ratiuEntry.setField("issn", "1422-6928");
        ratiuEntry.setField("keywords", "76A15 (35A01 35A02 35K61)");
        ratiuEntry.setField("mrnumber", "3537908");
        ratiuEntry.setField("doi", "10.1007/s00021-016-0250-0");
    }

    @Test
    public void searchByEntryFindsEntry() throws Exception {
        BibEntry searchEntry = new BibEntry();
        searchEntry.setField("title", "existence");
        searchEntry.setField("author", "Ratiu");
        searchEntry.setField("journal", "fluid");

        List<BibEntry> fetchedEntries = fetcher.performSearch(searchEntry);
        assertFalse(fetchedEntries.isEmpty());
        assertEquals(ratiuEntry, fetchedEntries.get(0));
    }

    @Test
    public void searchByQueryFindsEntry() throws Exception {
        List<BibEntry> fetchedEntries = fetcher.performSearch("Two-Dimensional Ericksen Leslie System");
        assertFalse(fetchedEntries.isEmpty());
        assertEquals(ratiuEntry, fetchedEntries.get(0));
    }
}
