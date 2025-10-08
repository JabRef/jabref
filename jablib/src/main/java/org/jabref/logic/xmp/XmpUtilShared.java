package org.jabref.logic.xmp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

import org.jabref.model.entry.BibEntry;

import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.xml.DomXmpParser;
import org.apache.xmpbox.xml.XmpParsingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * XMPUtilShared provides support for reading {@link org.jabref.logic.xmp.XmpUtilReader} and writing {@link org.jabref.logic.xmp.XmpUtilWriter} BibTeX data as XMP metadata
 * in PDF-documents.
 */
public class XmpUtilShared {

    public static final String BIBTEX_DI_FIELD_NAME_PREFIX = "bibtex/";

    private static final Logger LOGGER = LoggerFactory.getLogger(XmpUtilShared.class);

    private DomXmpParser DOM_XMP_PARSER;

    public XmpUtilShared() {
        try {
            DOM_XMP_PARSER = new DomXmpParser();
        } catch (XmpParsingException e) {
            LOGGER.error("Could not initialize DomXmpParser", e);
            DOM_XMP_PARSER = null;
        }
    }

    public XMPMetadata parseXmpMetadata(InputStream is) throws IOException {
        XMPMetadata meta;
        try {
            meta = DOM_XMP_PARSER.parse(is);
            return meta;
        } catch (XmpParsingException e) {
            // bad style to catch Exception but as this is called in a loop we do not want to break here when any schema encounters an error
            throw new IOException(e);
        }
    }

    /**
     * Will try to read XMP metadata from the given file, returning whether
     * metadata was found.
     * <p>
     * Caution: This method is as expensive as it is reading the actual metadata
     * itself from the PDF.
     *
     * @param path the path to the PDF.
     * @return whether a BibEntry was found in the given PDF.
     */
    public static boolean hasMetadata(Path path, XmpPreferences xmpPreferences) {
        try {
            List<BibEntry> bibEntries = new XmpUtilReader().readXmp(path, xmpPreferences);
            return !bibEntries.isEmpty();
        } catch (EncryptedPdfsNotSupportedException ex) {
            LOGGER.info("Encryption not supported by XMPUtil");
            return false;
        } catch (IOException e) {
            XmpUtilShared.LOGGER.debug("No metadata was found. Path: {}", path.toString());
            return false;
        }
    }
}
