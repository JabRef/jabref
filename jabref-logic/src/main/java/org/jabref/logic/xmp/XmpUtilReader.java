package org.jabref.logic.xmp;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.DublinCoreSchema;

public class XmpUtilReader {

    private static final String START_TAG = "<rdf:Description";
    private static final String END_TAG = "</rdf:Description>";

    private XmpUtilReader() {
    }

    /**
     * Will read the XMPMetadata from the given pdf file, closing the file
     * afterwards.
     *
     * @param path The path to read the XMPMetadata from.
     * @return The XMPMetadata object found in the file
     */
    public static List<XMPMetadata> readRawXmp(Path path) throws IOException {
        try (PDDocument document = XmpUtilReader.loadWithAutomaticDecryption(path)) {
            return XmpUtilReader.getXmpMetadata(document);
        }
    }

    /**
     * Convenience method for readXMP(File).
     *
     * @param filename The filename from which to open the file.
     * @return BibtexEntryies found in the PDF or an empty list
     */
    public static List<BibEntry> readXmp(String filename, XmpPreferences xmpPreferences) throws IOException {
        return XmpUtilReader.readXmp(Paths.get(filename), xmpPreferences);
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
    public static List<BibEntry> readXmp(Path path, XmpPreferences xmpPreferences)
            throws IOException {

        List<BibEntry> result = new LinkedList<>();

        try (PDDocument document = loadWithAutomaticDecryption(path)) {
            List<XMPMetadata> xmpMetaList = XmpUtilReader.getXmpMetadata(document);

            if (!xmpMetaList.isEmpty()) {
                // Only support Dublin Core since JabRef 4.2
                for (XMPMetadata xmpMeta : xmpMetaList) {
                    DublinCoreSchema dcSchema = xmpMeta.getDublinCoreSchema();

                    if (dcSchema != null) {
                        DublinCoreExtractor dcExtractor = new DublinCoreExtractor(dcSchema, xmpPreferences, new BibEntry());
                        Optional<BibEntry> entry = dcExtractor.extractBibtexEntry();

                        if (entry.isPresent()) {
                            result.add(entry.get());
                        }
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

        // return empty list, if no metadata was found
        if (result.isEmpty()) {
            return Collections.emptyList();
        }
        return result;
    }

    /**
     * This method is a hack to generate multiple XMPMetadata objects, because the
     * implementation of the pdfbox does not support methods for reading multiple
     * DublinCoreSchemas from a single metadata entry.
     * <p/>
     *
     *
     * @return empty Optional if no metadata has been found
     */
    private static List<XMPMetadata> getXmpMetadata(PDDocument document) throws IOException {
        PDDocumentCatalog catalog = document.getDocumentCatalog();
        PDMetadata metaRaw = catalog.getMetadata();
        List<XMPMetadata> metaList = new ArrayList<>();

        if (metaRaw == null) {
            return metaList;
        }

        String xmp = metaRaw.getCOSObject().toTextString();

        int startDescriptionSection = xmp.indexOf(START_TAG);
        int endDescriptionSection = xmp.lastIndexOf(END_TAG) + END_TAG.length();

        // XML header for the xmpDomParser
        String start = xmp.substring(0, startDescriptionSection);
        // descriptionArray - mid part of the textual metadata
        String[] descriptionsArray = xmp.substring(startDescriptionSection, endDescriptionSection).split(END_TAG);
        // XML footer for the xmpDomParser
        String end = xmp.substring(endDescriptionSection);

        for (String s : descriptionsArray) {
            // END_TAG is appended, because of the split operation above
            String xmpMetaString = start + s + END_TAG + end;
            metaList.add(XmpUtilShared.parseXmpMetadata(new ByteArrayInputStream(xmpMetaString.getBytes())));
        }
        return metaList;
    }

    /**
     * Loads the specified file with the basic pdfbox functionality and uses an empty string as default password.
     *
     * @param path The path to load.
     * @return
     * @throws IOException from the underlying {@link PDDocument#load(File)}
     */
    public static PDDocument loadWithAutomaticDecryption(Path path) throws IOException {
        // try to load the document
        // also uses an empty string as default password
        PDDocument doc = PDDocument.load(path.toFile());
        return doc;
    }
}
