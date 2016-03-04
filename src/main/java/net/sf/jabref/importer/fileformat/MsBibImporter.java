/*  Copyright (C) 2003-2011 JabRef contributors.
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
package net.sf.jabref.importer.fileformat;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.logic.msbib.MSBibDatabase;

import org.w3c.dom.Document;

/**
 * Importer for the MS Office 2007 XML bibliography format
 * By S. M. Mahbub Murshed
 *
 * ...
 */
public class MsBibImporter extends ImportFormat {

    @Override
    public boolean isRecognizedFormat(InputStream in) throws IOException {

        /*
            This method is available for checking if a file can be of the MSBib type.
            The effect of this method is primarily to avoid unnecessary processing of
            files when searching for a suitable import format. If this method returns
            false, the import routine will move on to the next import format.

            The correct behaviour is to return false if it is certain that the file is
            not of the MsBib type, and true otherwise. Returning true is the safe choice
            if not certain.
         */
        Document docin;
        try {
            DocumentBuilder dbuild = DocumentBuilderFactory.
                    newInstance().
                    newDocumentBuilder();
            docin = dbuild.parse(in);
        } catch (Exception e) {
            return false;
        }
        return (docin == null) || docin.getDocumentElement().getTagName().contains("Sources");
    }

    /**
     * String used to identify this import filter on the command line.
     * @return "msbib"
     */
    public String getCommandLineId() {
        return "msbib";
    }

    @Override
    public List<BibEntry> importEntries(InputStream in, OutputPrinter status) throws IOException {

        MSBibDatabase dbase = new MSBibDatabase();

        return dbase.importEntries(in);
    }

    @Override
    public String getFormatName() {
        // This method should return the name of this import format.
        return "MSBib";
    }

}
