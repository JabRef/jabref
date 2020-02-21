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
 * XMPUtilShared provides support for reading (@link XMPUtilReader) and writing (@link XMPUtilWriter) BibTex data as XMP metadata
 * in PDF-documents.
 */
public class XmpUtilShared {

    private static final Logger LOGGER = LoggerFactory.getLogger(XmpUtilShared.class);

    private XmpUtilShared() {
    }

    protected static XMPMetadata parseXmpMetadata(InputStream is) throws IOException {
        XMPMetadata meta = null;
        try {
            DomXmpParser parser = new DomXmpParser();
            meta = parser.parse(is);
            return meta;
        } catch (XmpParsingException e) {
            throw new IOException(e);
        }
    }

    /**
     * Will try to read XMP metadata from the given file, returning whether
     * metadata was found.
     *
     * Caution: This method is as expensive as it is reading the actual metadata
     * itself from the PDF.
     *
     * @param path the path to the PDF.
     * @return whether a BibEntry was found in the given PDF.
     */
    public static boolean hasMetadata(Path path, XmpPreferences xmpPreferences) {
        try {
            List<BibEntry> bibEntries = XmpUtilReader.readXmp(path, xmpPreferences);
            return !bibEntries.isEmpty();
        } catch (EncryptedPdfsNotSupportedException ex) {
            LOGGER.info("Encryption not supported by XMPUtil");
            return false;
        } catch (IOException e) {
            XmpUtilShared.LOGGER.debug("No metadata was found. Path: " + path.toString());
            return false;
        }
    }
}
