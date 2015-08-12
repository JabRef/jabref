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
package net.sf.jabref.imports;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.JabRefPreferences;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

/**
 * @version 11.11.2008 | 22:16
 */
public class EntryFromPDFCreatorTest {

    private final EntryFromPDFCreator entryCreator = new EntryFromPDFCreator();


    @Before
    public void setUp() throws Exception {
        // externalFileTypes are needed for the EntryFromPDFCreator
        JabRefPreferences.getInstance().updateExternalFileTypes();
    }

    @Test
    public void testPDFFileFilter() {
        Assert.assertTrue(entryCreator.accept(new File("aPDF.pdf")));
        Assert.assertTrue(entryCreator.accept(new File("aPDF.PDF")));
        Assert.assertFalse(entryCreator.accept(new File("foo.jpg")));
    }

    @Test
    @Ignore
    public void testCreationOfEntry() {
        BibtexEntry entry = entryCreator.createEntry(ImportDataTest.NOT_EXISTING_PDF, false);
        Assert.assertNull(entry);

        entry = entryCreator.createEntry(ImportDataTest.FILE_NOT_IN_DATABASE, false);
        Assert.assertNotNull(entry);
        Assert.assertTrue(entry.getField("file").endsWith(":PDF"));
        Assert.assertEquals(ImportDataTest.FILE_NOT_IN_DATABASE.getName(), entry.getField("title"));

    }
}