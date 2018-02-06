package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.msbib.MSBibDatabase;
import org.jabref.logic.util.FileType;

import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Importer for the MS Office 2007 XML bibliography format
 * By S. M. Mahbub Murshed
 *
 * ...
 */
public class MsBibImporter extends Importer {

    @Override
    public boolean isRecognizedFormat(BufferedReader reader) throws IOException {
        Objects.requireNonNull(reader);

        /*
            The correct behaviour is to return false if it is certain that the file is
            not of the MsBib type, and true otherwise. Returning true is the safe choice
            if not certain.
         */
        Document docin;
        try {
            DocumentBuilder dbuild = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            dbuild.setErrorHandler(new ErrorHandler() {

                @Override
                public void warning(SAXParseException exception) throws SAXException {
                    // ignore warnings
                }

                @Override
                public void fatalError(SAXParseException exception) throws SAXException {
                    throw exception;
                }

                @Override
                public void error(SAXParseException exception) throws SAXException {
                    throw exception;
                }
            });
            docin = dbuild.parse(new InputSource(reader));
        } catch (Exception e) {
            return false;
        }
        return (docin == null) || docin.getDocumentElement().getTagName().contains("Sources");
    }

    @Override
    public ParserResult importDatabase(BufferedReader reader) throws IOException {
        Objects.requireNonNull(reader);

        MSBibDatabase dbase = new MSBibDatabase();
        return new ParserResult(dbase.importEntriesFromXml(reader));
    }

    @Override
    public String getName() {
        return "MSBib";
    }

    @Override
    public FileType getFileType() {
        return FileType.MSBIB;
    }

    @Override
    public String getDescription() {
        return "Importer for the MS Office 2007 XML bibliography format.";
    }

}
