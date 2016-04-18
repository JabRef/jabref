/*  Copyright (C) 2003-2016 JabRef contributors.
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
package net.sf.jabref.importer;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRef;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.model.entry.BibEntry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.mockito.Mockito.mock;

import java.io.File;
import java.util.Optional;

public class EntryFromPDFCreatorTest {

    private EntryFromPDFCreator entryCreator;


    @Before
    public void setUp() {
        Globals.prefs = JabRefPreferences.getInstance();
        // Needed to initialize ExternalFileTypes
        entryCreator = new EntryFromPDFCreator();
        // Needed for PdfImporter - still not enough
        JabRef.mainFrame = mock(JabRefFrame.class);
    }

    @Test
    public void testPDFFileFilter() {
        Assert.assertTrue(entryCreator.accept(new File("aPDF.pdf")));
        Assert.assertTrue(entryCreator.accept(new File("aPDF.PDF")));
        Assert.assertFalse(entryCreator.accept(new File("foo.jpg")));
    }

    @Test
    public void testCreationOfEntryNoPDF() {
        Optional<BibEntry> entry = entryCreator.createEntry(ImportDataTest.NOT_EXISTING_PDF, false);
        Assert.assertFalse(entry.isPresent());
    }

    @Test
    @Ignore
    public void testCreationOfEntryNotInDatabase() {
        Optional<BibEntry> entry = entryCreator.createEntry(ImportDataTest.FILE_NOT_IN_DATABASE, false);
        Assert.assertTrue(entry.isPresent());
        Assert.assertTrue(entry.get().getField("file").endsWith(":PDF"));
        Assert.assertEquals(ImportDataTest.FILE_NOT_IN_DATABASE.getName(), entry.get().getField("title"));

    }
}