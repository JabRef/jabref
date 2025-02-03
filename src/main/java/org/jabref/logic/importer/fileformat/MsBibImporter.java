package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.msbib.MSBibDatabase;
import org.jabref.logic.util.StandardFileType;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Importer for the MS Office 2007 XML bibliography format
 */
@NullMarked
public class MsBibImporter extends Importer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MsBibImporter.class);
    private static final String DISABLEDTD = "http://apache.org/xml/features/disallow-doctype-decl";
    private static final String DISABLEEXTERNALDTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
    private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = makeSafeDocBuilderFactory(DocumentBuilderFactory.newInstance());

    /**
     * The correct behavior is to return false if it is certain that the file is
     * not of the MsBib type, and true otherwise. Returning true is the safe choice
     * if not certain.
     */
    @Override
    public boolean isRecognizedFormat(BufferedReader reader) throws IOException {
        Objects.requireNonNull(reader); // Required by test case
        Document docin;
        try {
            DocumentBuilder dbuild = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
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
        Objects.requireNonNull(reader); // Required by test case
        MSBibDatabase dbase = new MSBibDatabase();
        return new ParserResult(dbase.importEntriesFromXml(reader));
    }

    @Override
    public String getId() {
        return "msbib";
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
        return Localization.lang("Importer for the MS Office 2007 XML bibliography format.");
    }

    /**
     * DocumentBuilderFactory makes a XXE safe Builder factory from dBuild. If not supported by current
     * XML then returns original builder given and logs error.
     *
     * @param dBuild | DocumentBuilderFactory to be made XXE safe.
     * @return If supported, XXE safe DocumentBuilderFactory. Else, returns original builder given
     */
    private static DocumentBuilderFactory makeSafeDocBuilderFactory(DocumentBuilderFactory dBuild) {
        String feature = null;

        try {
            feature = DISABLEDTD;
            dBuild.setFeature(feature, true);

            feature = DISABLEEXTERNALDTD;
            dBuild.setFeature(feature, false);

            dBuild.setXIncludeAware(false);
            dBuild.setExpandEntityReferences(false);
        } catch (ParserConfigurationException e) {
            LOGGER.warn("Builder not fully configured. Feature:'{}' is probably not supported by current XML processor. {}", feature, e);
        }

        return dBuild;
    }
}
