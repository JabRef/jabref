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

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

/**
 * @author Nosh&Dan
 * @version 09.11.2008 | 19:41:40
 */
public class ImportDataTest {

    public static final File FILE_IN_DATABASE = new File("src/test/resources/net/sf/jabref/imports/unlinkedFilesTestFolder/pdfInDatabase.pdf");
    public static final File FILE_NOT_IN_DATABASE = new File("src/test/resources/net/sf/jabref/imports/unlinkedFilesTestFolder/pdfNotInDatabase.pdf");
    public static final File EXISTING_FOLDER = new File("src/test/resources/net/sf/jabref/imports/unlinkedFilesTestFolder");
    public static final File NOT_EXISTING_FOLDER = new File("notexistingfolder");
    public static final File NOT_EXISTING_PDF = new File("src/test/resources/net/sf/jabref/imports/unlinkedFilesTestFolder/null.pdf");
    public static final File UNLINKED_FILES_TEST_BIB = new File("src/test/resources/net/sf/jabref/util/unlinkedFilesTestBib.bib");


    /**
     * Tests the testing environment.
     */
    @Test
    public void testTestingEnvironment() {
        Assert.assertTrue(ImportDataTest.EXISTING_FOLDER.exists());
        Assert.assertTrue(ImportDataTest.EXISTING_FOLDER.isDirectory());

        Assert.assertTrue(ImportDataTest.FILE_IN_DATABASE.exists());
        Assert.assertTrue(ImportDataTest.FILE_IN_DATABASE.isFile());

        Assert.assertTrue(ImportDataTest.FILE_NOT_IN_DATABASE.exists());
        Assert.assertTrue(ImportDataTest.FILE_NOT_IN_DATABASE.isFile());
    }

    @Test
    public void testOpenNotExistingDirectory() {
        Assert.assertFalse(ImportDataTest.NOT_EXISTING_FOLDER.exists());
        Assert.assertFalse(ImportDataTest.NOT_EXISTING_PDF.exists());
    }

}
