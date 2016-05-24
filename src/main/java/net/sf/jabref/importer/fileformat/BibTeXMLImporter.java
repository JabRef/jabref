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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

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
public class BibTeXMLImporter extends ImportFormat {

    private static final Log LOGGER = LogFactory.getLog(BibTeXMLImporter.class);

    private static final Pattern START_PATTERN = Pattern.compile("<(bibtex:)?file .*");

    @Override
    public String getFormatName() {
        return "BibTeXML";
    }

    @Override
    public List<String> getExtensions() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader reader) throws IOException {
        // Our strategy is to look for the "<bibtex:file *" line.
        String str;
        while ((str = reader.readLine()) != null) {
            if (START_PATTERN.matcher(str).find()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ParserResult importDatabase(BufferedReader reader) throws IOException {
        Objects.requireNonNull(reader);

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
            parser.parse(new InputSource(reader), handler);
            // When you're done, report the results stored by your handler object
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
