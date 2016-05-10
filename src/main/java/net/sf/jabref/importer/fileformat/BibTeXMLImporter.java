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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sf.jabref.importer.ImportFormatReader;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Importer for the Refer/Endnote format.
 *
 * check here for details on the format
 * http://www.ecst.csuchico.edu/~jacobsd/bib/formats/endnote.html
 */
public class BibTeXMLImporter extends ImportFormat {

    private static final Log LOGGER = LogFactory.getLog(BibTeXMLImporter.class);

    private static final Pattern START_PATTERN = Pattern.compile("<(bibtex:)?file .*");


    /**
     * Return the name of this import format.
     */

    @Override
    public String getFormatName() {
        return "BibTeXML";
    }

    /*
     *  (non-Javadoc)
     * @see net.sf.jabref.imports.ImportFormat#getCLIId()
     */
    @Override
    public String getCLIId() {
        return "bibtexml";
    }

    /**
     * Check whether the source is in the correct format for this importer.
     */
    @Override
    public boolean isRecognizedFormat(InputStream stream) throws IOException {

        // Our strategy is to look for the "<bibtex:file *" line.
        try (BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream))) {
            String str;
            while ((str = in.readLine()) != null) {
                if (START_PATTERN.matcher(str).find()) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Parse the entries in the source, and return a List of BibEntry
     * objects.
     */
    @Override
    public List<BibEntry> importEntries(InputStream stream, OutputPrinter status) throws IOException {

        List<BibEntry> bibItems = new ArrayList<>();

        // Obtain a factory object for creating SAX parsers
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        // Configure the factory object to specify attributes of the parsers it
        // creates
        // parserFactory.setValidating(true);
        parserFactory.setNamespaceAware(true);
        // Now create a SAXParser object

        try {
            SAXParser parser = parserFactory.newSAXParser(); //May throw exceptions
            BibTeXMLHandler handler = new BibTeXMLHandler();
            // Start the parser. It reads the file and calls methods of the handler.
            parser.parse(stream, handler);
            // When you're done, report the results stored by your handler object
            bibItems.addAll(handler.getItems());

        } catch (javax.xml.parsers.ParserConfigurationException e) {
            LOGGER.error("Error with XML parser configuration", e);
            status.showMessage(e.getLocalizedMessage());
        } catch (org.xml.sax.SAXException e) {
            LOGGER.error("Error during XML parsing", e);
            status.showMessage(e.getLocalizedMessage());
        } catch (IOException e) {
            LOGGER.error("Error during file import", e);
            status.showMessage(e.getLocalizedMessage());
        }
        return bibItems;

    }

}
