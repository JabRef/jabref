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
package net.sf.jabref.importer.fileformat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.InputSource;

/**
 * Importer for the Refer/Endnote format.
 *
 * check here for details on the format
 * http://www.ecst.csuchico.edu/~jacobsd/bib/formats/endnote.html
 */
public class MedlineImporter extends ImportFormat {

    private static final Log LOGGER = LogFactory.getLog(MedlineImporter.class);


    @Override
    public String getFormatName() {
        return "Medline";
    }

    @Override
    public List<String> getExtensions() {
        return null;
    }

    @Override
    public String getId() {
        return "medline";
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader reader) throws IOException {
        String str;
        int i = 0;
        while (((str = reader.readLine()) != null) && (i < 50)) {

            if (str.toLowerCase().contains("<pubmedarticle>")) {
                return true;
            }

            i++;
        }
        return false;
    }

    @Override
    public ParserResult importDatabase(BufferedReader reader) throws IOException {
        Objects.requireNonNull(reader);

        // Obtain a factory object for creating SAX parsers
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();

        // Configure the factory object to specify attributes of the parsers it
        // creates
        parserFactory.setValidating(true);
        parserFactory.setNamespaceAware(true);

        // Now create a SAXParser object
        List<BibEntry> bibItems = new ArrayList<>();
        try {
            SAXParser parser = parserFactory.newSAXParser(); // May throw
            // exceptions
            MedlineHandler handler = new MedlineHandler();
            // Start the parser. It reads the file and calls methods of the
            // handler.
            parser.parse(new InputSource(reader), handler);

            // Switch this to true if you want to make a local copy for testing.
            if (false) {
                reader.reset();
                try (FileOutputStream out = new FileOutputStream(new File("/home/alver/ut.txt"))) {
                    int c;
                    while ((c = reader.read()) != -1) {
                        out.write((char) c);
                    }
                }
            }

            // When you're done, report the results stored by your handler
            // object
            bibItems.addAll(handler.getItems());
        } catch (javax.xml.parsers.ParserConfigurationException e) {
            LOGGER.error("Error with XML parser configuration", e);
            return ParserResult.fromErrorMessage(e.getLocalizedMessage());
        } catch (org.xml.sax.SAXException e) {
            LOGGER.error("Error during XML parsing", e);
            return ParserResult.fromErrorMessage(e.getLocalizedMessage());
        } catch (IOException e) {
            LOGGER.error("Error during file import", e);
            return ParserResult.fromErrorMessage(e.getLocalizedMessage());
        }

        return new ParserResult(bibItems);
    }

}
