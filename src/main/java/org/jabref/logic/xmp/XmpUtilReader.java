package org.jabref.logic.xmp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

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
    public List<BibEntry> readXmp(Path path, XmpPreferences xmpPreferences)
            throws IOException {

        List<BibEntry> result = new LinkedList<>();

        try (PDDocument document = loadWithAutomaticDecryption(path)) {
            List<XMPMetadata> xmpMetaList = getXmpMetadata(document);

            if (!xmpMetaList.isEmpty()) {
                // Only support Dublin Core since JabRef 4.2
                for (XMPMetadata xmpMeta : xmpMetaList) {
                    DublinCoreSchema dcSchema = DublinCoreSchemaCustom.copyDublinCoreSchema(xmpMeta.getDublinCoreSchema());
                    if (dcSchema != null) {
                        DublinCoreExtractor dcExtractor = new DublinCoreExtractor(dcSchema, xmpPreferences, new BibEntry());
                        Optional<BibEntry> entry = dcExtractor.extractBibtexEntry();
                        entry.ifPresent(result::add);
                    }
                }
            }
            if (result.isEmpty()) {
                // If we did not find any XMP metadata, search for non XMP metadata
                PDDocumentInformation documentInformation = document.getDocumentInformation();
                DocumentInformationExtractor diExtractor = new DocumentInformationExtractor(documentInformation);
                Optional<BibEntry> entry = diExtractor.extractBibtexEntry();
                entry.ifPresent(result::add);
            }
        }

        result.forEach(entry -> entry.addFile(new LinkedFile("", path.toAbsolutePath(), "PDF")));
        return result;
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
        List<XMPMetadata> metaList = new ArrayList<>();

        if (metaRaw == null) {
            return metaList;
        }

        String xmp = metaRaw.getCOSObject().toTextString();

        int startDescriptionSection = xmp.indexOf(START_TAG);
        int endDescriptionSection = xmp.lastIndexOf(END_TAG) + END_TAG.length();

        if ((startDescriptionSection < 0) || (startDescriptionSection > endDescriptionSection) || (endDescriptionSection == (END_TAG.length() - 1))) {
            return metaList;
        }

        // XML header for the xmpDomParser
        String start = xmp.substring(0, startDescriptionSection);
        // descriptionArray - mid part of the textual metadata
        String[] descriptionsArray = xmp.substring(startDescriptionSection, endDescriptionSection).split(END_TAG);
        // XML footer for the xmpDomParser
        String end = xmp.substring(endDescriptionSection);

        for (String s : descriptionsArray) {
            // END_TAG is appended, because of the split operation above
            String xmpMetaString = start + s + END_TAG + end;
            try {
                metaList.add(XmpUtilShared.parseXmpMetadata(new ByteArrayInputStream(xmpMetaString.getBytes())));
            } catch (IOException ex) {
                LOGGER.warn("Problem parsing XMP schema. Continuing with other schemas.", ex);
            }
        }
        return metaList;
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
