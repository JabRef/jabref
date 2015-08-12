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

import net.sf.jabref.*;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Test cases for the RISImporter
 *
 * @author $Author: coezbek $
 */
public class RISImporterTest {

    @Before
    public void setUp() throws Exception {
        if (Globals.prefs == null) {
            Globals.prefs = JabRefPreferences.getInstance();
        }
    }

    @Test
    public void testIsRecognizedFormat() throws IOException {

        RisImporter importer = new RisImporter();
        Assert.assertTrue(importer.isRecognizedFormat(RISImporterTest.class
                .getResourceAsStream("RisImporterTest1.ris")));
    }

    @Test
    public void testProcessSubSup() {

        HashMap<String, String> hm = new HashMap<String, String>();
        hm.put("title", "/sub 3/");
        IsiImporter.processSubSup(hm);
        Assert.assertEquals("$_3$", hm.get("title"));

        hm.put("title", "/sub   3   /");
        IsiImporter.processSubSup(hm);
        Assert.assertEquals("$_3$", hm.get("title"));

        hm.put("title", "/sub 31/");
        IsiImporter.processSubSup(hm);
        Assert.assertEquals("$_{31}$", hm.get("title"));

        hm.put("abstract", "/sub 3/");
        IsiImporter.processSubSup(hm);
        Assert.assertEquals("$_3$", hm.get("abstract"));

        hm.put("review", "/sub 31/");
        IsiImporter.processSubSup(hm);
        Assert.assertEquals("$_{31}$", hm.get("review"));

        hm.put("title", "/sup 3/");
        IsiImporter.processSubSup(hm);
        Assert.assertEquals("$^3$", hm.get("title"));

        hm.put("title", "/sup 31/");
        IsiImporter.processSubSup(hm);
        Assert.assertEquals("$^{31}$", hm.get("title"));

        hm.put("abstract", "/sup 3/");
        IsiImporter.processSubSup(hm);
        Assert.assertEquals("$^3$", hm.get("abstract"));

        hm.put("review", "/sup 31/");
        IsiImporter.processSubSup(hm);
        Assert.assertEquals("$^{31}$", hm.get("review"));

        hm.put("title", "/sub $Hello/");
        IsiImporter.processSubSup(hm);
        Assert.assertEquals("$_{\\$Hello}$", hm.get("title"));
    }

    @Test
    public void testImportEntries() throws IOException {
        RisImporter importer = new RisImporter();

        List<BibtexEntry> entries = importer.importEntries(RISImporterTest.class
                .getResourceAsStream("RisImporterTest1.ris"), new OutputPrinterToNull());
        Assert.assertEquals(1, entries.size());
        BibtexEntry entry = entries.get(0);
        Assert.assertEquals("Editorial: Open Source and Empirical Software Engineering", entry
                .getField("title"));
        Assert.assertEquals(
                "Harrison, Warren",
                entry.getField("author"));

        Assert.assertEquals(BibtexEntryTypes.ARTICLE, entry.getType());
        Assert.assertEquals("Empirical Software Engineering", entry.getField("journal"));
        Assert.assertEquals("2001", entry.getField("year"));
        Assert.assertEquals("6", entry.getField("volume"));
        Assert.assertEquals("3", entry.getField("number"));
        Assert.assertEquals("193--194", entry.getField("pages"));
        Assert.assertEquals("#sep#", entry.getField("month"));
    }
}
