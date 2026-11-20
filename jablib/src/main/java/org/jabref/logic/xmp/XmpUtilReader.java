package org.jabref.logic.xmp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.SequencedCollection;

import org.jabref.logic.bibtex.comparator.BibEntryCompare;
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

/// Readings on XMP are available at docs/code-howtos/xmp-parsing.md
///
/// See also [org.jabref.logic.xmp.XmpUtilWriter#writeDocumentInformation]
public class XmpUtilReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(XmpUtilReader.class);

    private static final String START_TAG = "<rdf:Description";
    private static final String END_TAG = "</rdf:Description>";

    private static final XmpUtilShared XMP_UTIL_SHARED = new XmpUtilShared();

    public XmpUtilReader() {
        // See: https://pdfbox.apache.org/2.0/getting-started.html
        System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider"); // To get higher rendering speed on java 8 oder 9 for images
    }

    /// Will read the XMPMetadata from the given pdf file, closing the file afterwards.
    ///
    /// @param path The path to read the XMPMetadata from.
    /// @return The XMPMetadata object found in the file
    public List<XMPMetadata> readRawXmp(Path path) throws IOException {
        try (PDDocument document = loadWithAutomaticDecryption(path)) {
            return getXmpMetadata(document);
        }
    }

    /// @param path The path to read from.
    /// @return list of a single BibEntry retrieved by merging the data from the stream
    public List<BibEntry> readXmp(Path path, XmpPreferences xmpPreferences) throws IOException {
        try (PDDocument document = loadWithAutomaticDecryption(path)) {
            return readXmp(path, document, xmpPreferences);
        }
    }

    /// Merges all XMP data together in one entry.
    ///
    /// Try to read the given BibTexEntry from the given PDF file.
    ///
    /// Looks at the DocumentInformation and the XMP metadata.
    /// Regarding the XMP metadata, only Dublin Core is supported.
    ///
    /// @param path     the path to the PDF file
    /// @param document the PDF document to read from (should have been created from `path`
    public List<BibEntry> readXmp(Path path, PDDocument document, XmpPreferences xmpPreferences) {
        final SequencedCollection<BibEntry> result = new LinkedHashSet<>();

        PDDocumentInformation documentInformation = document.getDocumentInformation();
        Optional<BibEntry> documentInformationEntry = new DocumentInformationExtractor(documentInformation).extractBibtexEntry();

        List<XMPMetadata> xmpMetaList = getXmpMetadata(document);
        if (!xmpMetaList.isEmpty()) {
            // Only support Dublin Core since JabRef 4.2
            for (XMPMetadata xmpMeta : xmpMetaList) {
                DublinCoreSchema dcSchema = DublinCoreSchemaCustom.copyDublinCoreSchema(xmpMeta.getDublinCoreSchema());
                if (dcSchema != null) {
                    DublinCoreExtractor dcExtractor = new DublinCoreExtractor(dcSchema, xmpPreferences, new BibEntry());
                    dcExtractor.extractBibtexEntry().ifPresent(dublinCoreEntry -> {
                        // The document information is written from the first entry only, so it belongs to the first Dublin Core entry.
                        // Restore before adding: modifying an entry already in the LinkedHashSet would change its hash code
                        if (result.isEmpty()) {
                            documentInformationEntry.ifPresent(entry -> restoreLatexFromDocumentInformation(dublinCoreEntry, entry));
                        }
                        result.add(dublinCoreEntry);
                    });
                }
            }
        }

        documentInformationEntry.ifPresent(entry -> {
            if (result.isEmpty()) {
                result.add(entry);
                return;
            }
            BibEntry first = result.getFirst();
            switch (BibEntryCompare.compareEntries(first, entry)) {
                case SUBSET -> {
                    result.removeFirst();
                    result.addFirst(entry);
                }
                case DIFFERENT ->
                        result.addFirst(entry);
                case DISJUNCT_OR_EQUAL_FIELDS,
                     DISJUNCT ->
                        first.mergeWith(entry);
                // in all other cases (EQUAL, SUPERSET), the documentInformation is ignored
            }
        });

        result.forEach(entry -> entry.addFile(new LinkedFile("", path, "PDF")));

        return result.stream().toList();
    }

    /// Dublin Core stores LaTeX fields as plain text, whereas the document information keeps the original LaTeX under `bibtex/` keys.
    /// Restoring the original value lets both sources be recognized as the same entry and keeps the LaTeX on re-import.
    private static void restoreLatexFromDocumentInformation(BibEntry dublinCoreEntry, BibEntry documentInformationEntry) {
        documentInformationEntry.getFieldMap().forEach((field, originalValue) ->
                dublinCoreEntry.getField(field)
                               .filter(plainText -> plainText.equals(XmpUtilShared.toPlainText(originalValue)))
                               .ifPresent(_ -> dublinCoreEntry.setField(field, originalValue)));
    }

    ///
    /// This method is a hack to generate multiple XMPMetadata objects, because the
    /// implementation of the pdfbox does not support methods for reading multiple
    /// DublinCoreSchemas from a single metadata entry.
    ///
    ///
    /// Moreover, DomXmpParser does not handle unknown namespaces
    ///
    /// @return empty List if no metadata has been found, or cannot properly find start or end tag in metadata
    private List<XMPMetadata> getXmpMetadata(PDDocument document) {
        PDDocumentCatalog catalog = document.getDocumentCatalog();
        PDMetadata metaRaw = catalog.getMetadata();
        if (metaRaw == null) {
            return List.of();
        }

        List<XMPMetadata> metaList = new ArrayList<>();

        String xmp;
        try {
            xmp = new String(metaRaw.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.debug("Could not read XMP metadata stream. Returning empty list.", e);
            return List.of();
        }

        // Simple-string based solution to check for XML elements
        //   <rdf:Description xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">...
        // The heuristics works as most PDf tools use this syntax (i.e., XML prefix names for RDF)

        int startDescriptionSection = xmp.indexOf(START_TAG);
        int endDescriptionSection = xmp.lastIndexOf(END_TAG) + END_TAG.length();

        if ((startDescriptionSection < 0) || (startDescriptionSection > endDescriptionSection) || (endDescriptionSection == (END_TAG.length() - 1))) {
            LOGGER.debug("Cannot find start or end tag in metadata. Returning empty list.");
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

            LOGGER.debug("Parsing RDF Description {}", xmpMetaString);

            try {
                metaList.add(XMP_UTIL_SHARED.parseXmpMetadata(new ByteArrayInputStream(xmpMetaString.getBytes(StandardCharsets.UTF_8))));
            } catch (IOException ex) {
                LOGGER.debug("Problem parsing XMP schema. Continuing with other schemas.", ex);
            }
        }
        return metaList;
    }

    /// Loads the specified file with the basic pdfbox functionality and uses an empty string as default password.
    ///
    /// @param path The path to load.
    /// @throws IOException from the underlying @link PDDocument#load(File)
    public PDDocument loadWithAutomaticDecryption(Path path) throws IOException {
        // try to load the document
        // also uses an empty string as default password
        return Loader.loadPDF(path.toFile());
    }
}
