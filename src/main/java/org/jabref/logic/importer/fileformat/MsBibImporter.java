package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.msbib.MSBibDatabase;
import org.jabref.logic.util.StandardFileType;

import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Importer for the MS Office 2007 XML bibliography format
 * By S. M. Mahbub Murshed & Nicholas S. Weatherley
 *
 * ...
 */
public class MsBibImporter extends Importer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MsBibImporter.class);

    @Override
    public boolean isRecognizedFormat(BufferedReader reader) throws IOException {
        Objects.requireNonNull(reader);

        /*
            The correct behavior is to return false if it is certain that the file is
            not of the MsBib type, and true otherwise. Returning true is the safe choice
            if not certain.
         */
        Document docin;
        try {
            DocumentBuilder dbuild = makeSafeDocBuilderFactory(DocumentBuilderFactory.newInstance()).newDocumentBuilder();
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
    public StandardFileType getFileType() {
        return StandardFileType.XML;
    }

    @Override
    public String getDescription() {
        return "Importer for the MS Office 2007 XML bibliography format.";
    }

    /**
     * DocumentBuilderFactory makes a XXE safe Builder factory from dBuild. If not supported by current
     * XML then returns original builder given and logs error.
     * @param dBuild | DocumentBuilderFactory to be made XXE safe.
     * @return If supported, XXE safe DocumentBuilderFactory. Else, returns original builder given
     */
    private DocumentBuilderFactory makeSafeDocBuilderFactory(DocumentBuilderFactory dBuild) {
        String FEATURE = null;
        try {
            FEATURE = "http://apache.org/xml/features/disallow-doctype-decl";
            dBuild.setFeature(FEATURE, true);

            FEATURE = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
            dBuild.setFeature(FEATURE, false);

            dBuild.setXIncludeAware(false);
            dBuild.setExpandEntityReferences(false);

        } catch (ParserConfigurationException e) {
            LOGGER.warn("Builder not fully configured. ParserConfigurationException was thrown. Feature:'" +
                        FEATURE + "' is probably not supported by current XML processor.");
        }

        return dBuild;
    }
}
