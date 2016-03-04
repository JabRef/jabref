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

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

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
public class MedlineImporter extends ImportFormat {

    private static final Log LOGGER = LogFactory.getLog(MedlineImporter.class);


    @Override
    public String getFormatName() {
        return "Medline";
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.jabref.imports.ImportFormat#getCLIId()
     */
    @Override
    public String getCLIId() {
        return "medline";
    }

    /**
     * Check whether the source is in the correct format for this importer.
     */
    @Override
    public boolean isRecognizedFormat(InputStream stream) throws IOException {

        try (BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream))) {
            String str;
            int i = 0;
            while (((str = in.readLine()) != null) && (i < 50)) {

                if (str.toLowerCase().contains("<pubmedarticle>")) {
                    return true;
                }

                i++;
            }
        }
        return false;
    }

    /**
     * Fetch and parse an medline item from eutils.ncbi.nlm.nih.gov.
     *
     * @param id One or several ids, separated by ","
     *
     * @return Will return an empty list on error.
     */
    public static List<BibEntry> fetchMedline(String id, OutputPrinter status) {
        String baseUrl = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&retmode=xml&rettype=citation&id=" +
                id;
        try {
            URL url = new URL(baseUrl);
            URLConnection data = url.openConnection();
            return new MedlineImporter().importEntries(data.getInputStream(), status);
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Parse the entries in the source, and return a List of BibEntry
     * objects.
     */
    @Override
    public List<BibEntry> importEntries(InputStream stream, OutputPrinter status) throws IOException {

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
            parser.parse(stream, handler);

            // Switch this to true if you want to make a local copy for testing.
            if (false) {
                stream.reset();
                try (FileOutputStream out = new FileOutputStream(new File("/home/alver/ut.txt"))) {
                    int c;
                    while ((c = stream.read()) != -1) {
                        out.write((char) c);
                    }
                }
            }

            // When you're done, report the results stored by your handler
            // object
            bibItems.addAll(handler.getItems());
        } catch (javax.xml.parsers.ParserConfigurationException e1) {
            LOGGER.error("Error with XML parser configuration", e1);
            status.showMessage(e1.getLocalizedMessage());
        } catch (org.xml.sax.SAXException e2) {
            LOGGER.error("Error during XML parsing", e2);
            status.showMessage(e2.getLocalizedMessage());
        } catch (IOException e3) {
            LOGGER.error("Error during file import", e3);
            status.showMessage(e3.getLocalizedMessage());
        }

        return bibItems;
    }

}
