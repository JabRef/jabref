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
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.sf.jabref.importer.ParserResult;
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
        Objects.requireNonNull(in);

        /*
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

    @Override
    public ParserResult importDatabase(InputStream in) throws IOException {
        Objects.requireNonNull(in);

        MSBibDatabase dbase = new MSBibDatabase();
        return new ParserResult(dbase.importEntries(in));
    }

    @Override
    public String getFormatName() {
        return "MSBib";
    }

    @Override
    public List<String> getExtensions() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

}
