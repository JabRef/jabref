package org.jabref.logic.xmp;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedCollection;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.schema.DublinCoreSchemaCustom;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.DublinCoreSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Readings on XMP are available at docs/code-howtos/xmp-parsing.md
 */
public class XmpUtilReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(XmpUtilReader.class);

    private static final String START_TAG = "<rdf:Description";
    private static final String END_TAG = "</rdf:Description>";

    public XmpUtilReader() {
        // See: https://pdfbox.apache.org/2.0/getting-started.html
        System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider"); // To get higher rendering speed on java 8 oder 9 for images
    }

    /**
     * Will read the XMPMetadata from the given pdf file, closing the file afterwards.
     *
     * @param path The path to read the XMPMetadata from.
     * @return The XMPMetadata object found in the file
     */
    public List<XMPMetadata> readRawXmp(Path path) throws IOException {
        try (PDDocument document = loadWithAutomaticDecryption(path)) {
            return getXmpMetadata(document);
        }
    }

    /**
     * Try to read the given BibTexEntry from the XMP-stream of the given
     * inputstream containing a PDF-file.
     *
     * Only supports Dublin Core as a metadata format.
     *
     * @param path The path to read from.
     * @return list of BibEntries retrieved from the stream. May be empty, but never null
     * @throws IOException Throws an IOException if the file cannot be read, so the user than remove a lock or cancel
     *                     the operation.
     */
    public List<BibEntry> readXmp(Path path, XmpPreferences xmpPreferences) throws IOException {
        try (PDDocument document = loadWithAutomaticDecryption(path)) {
            return readXmp(path, document, xmpPreferences);
        }
    }

    public List<BibEntry> readXmp(Path path, PDDocument document, XmpPreferences xmpPreferences) {
        SequencedCollection<BibEntry> result = new LinkedHashSet<>();

        // We add PDDocumentInformation in call cases
        PDDocumentInformation documentInformation = document.getDocumentInformation();
        new DocumentInformationExtractor(documentInformation).extractBibtexEntry().ifPresent(result::add);

        List<XMPMetadata> xmpMetaList = getXmpMetadata(document);
        if (!xmpMetaList.isEmpty()) {
            // Only support Dublin Core since JabRef 4.2
            for (XMPMetadata xmpMeta : xmpMetaList) {
                DublinCoreSchema dcSchema = DublinCoreSchemaCustom.copyDublinCoreSchema(xmpMeta.getDublinCoreSchema());
                if (dcSchema != null) {
                    DublinCoreExtractor dcExtractor = new DublinCoreExtractor(dcSchema, xmpPreferences, new BibEntry());
                    dcExtractor.extractBibtexEntry().ifPresent(result::add);
                }
            }
        }

        result.forEach(entry -> entry.addFile(new LinkedFile("", path.toAbsolutePath(), "PDF")));
        return result.stream().toList();
    }

    /**
     * This method is a hack to generate multiple XMPMetadata objects, because the
     * implementation of the pdfbox does not support methods for reading multiple
     * DublinCoreSchemas from a single metadata entry.
     * <p/>
     *
     *
     * @return empty List if no metadata has been found, or cannot properly find start or end tag in metadata
     */
    private List<XMPMetadata> getXmpMetadata(PDDocument document) {
        PDDocumentCatalog catalog = document.getDocumentCatalog();
        PDMetadata metaRaw = catalog.getMetadata();
        if (metaRaw == null) {
            return List.of();
        }

        try (InputStream is = metaRaw.exportXMPMetadata()) {
            return List.of(XmpUtilShared.parseXmpMetadata(is));
        } catch (IOException e) {
            LOGGER.debug("Problem parsing XMP metadata.", e);
            return List.of();
        }
    }

    /**
     * Loads the specified file with the basic pdfbox functionality and uses an empty string as default password.
     *
     * @param path The path to load.
     * @throws IOException from the underlying @link PDDocument#load(File)
     */
    public PDDocument loadWithAutomaticDecryption(Path path) throws IOException {
        // try to load the document
        // also uses an empty string as default password
        return Loader.loadPDF(path.toFile());
    }
}
