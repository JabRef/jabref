package org.jabref.logic.xmp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.transform.TransformerException;

import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.schema.DublinCoreSchemaCustom;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.DublinCoreSchema;
import org.apache.xmpbox.xml.XmpSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmpUtilRemover {

    private static final String XMP_BEGIN_END_TAG = "?xpacket";

    private static final Logger LOGGER = LoggerFactory.getLogger(XmpUtilWriter.class);

    /**
     * Tries to delete the given BibTexEntry in the XMP-stream of the given
     * PDF-file.
     *
     * Throws an IOException if the file cannot be read or written, so the user
     * can remove a lock or cancel the operation.
     *
     * The method will only delete BibTeX-XMP-data specified in the Xmp Preference
     * Tab, all other metadata are untouched.
     *
     * @param path          The file to write the entries to.
     * @param xmpPreferences  Delete information also in PDF document properties.
     * @throws TransformerException If the entry was malformed or unsupported.
     * @throws IOException          If the file could not be written to or could not be found.
     */
    public static void deleteXmp(Path path, XmpPreferences xmpPreferences) throws IOException, TransformerException {
        // uses same hack as PR #8658
        // See: src/main/java/org/jabref/logic/xmp/XmpUtilWriter.java
        // Restating the comment
        // Read from another file
        // Reason: Apache PDFBox does not support writing while the file is opened
        // See https://issues.apache.org/jira/browse/PDFBOX-4028
        Path newFile = Files.createTempFile("JabRef", "pdf");

        // extract entries from PDF file's XMP metadata
        List<BibEntry> entries = XmpUtilReader.readXmp(path, xmpPreferences);

        // get the union of all entries' fields
        Set<Field> fields = entries.stream().flatMap(entry -> entry.getFieldMap().keySet().stream()).collect(Collectors.toSet());

        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            if (document.isEncrypted()) {
                throw new EncryptedPdfsNotSupportedException();
            }

            // Delete schemas (PDDocumentInformation) from the document metadata
            if (entries.size() > 0) {
                XmpUtilRemover.deleteDocumentInformation(document, fields, xmpPreferences);
                XmpUtilRemover.rewriteDublinCore(entries, document, xmpPreferences);
            }

            // Save updates to original file
            try {
                document.save(newFile.toFile());
                FileUtil.copyFile(newFile, path, true);
            } catch (IOException e) {
                LOGGER.debug("Could not delete XMP metadata", e);
                throw new TransformerException("Could not delete XMP metadata: " + e.getLocalizedMessage(), e);
            }
        }
    }
    /**
     * Tries to delete the entries of the fields listed in the privacy filter. If "select all fields" is checked,
     * any given fields in the BibTex Entry is deleted.
     *
     *
     * @param document The pdf document to write to.
     * @param fields    The XMP metadata fields to delete.
     * @param xmpPreferences  Delete information also in PDF document properties.
     */

    private static void deleteDocumentInformation(PDDocument document, Set<Field> fields, XmpPreferences xmpPreferences) {
        PDDocumentInformation di = document.getDocumentInformation();

        // Query privacy filter settings
        boolean useXmpPrivacyFilter = xmpPreferences.shouldUseXmpPrivacyFilter();

        // Set all the values including key and entryType
        for (Field field: fields) {
            if (useXmpPrivacyFilter && xmpPreferences.getSelectAllFields().getValue()) {
                // if delete all, no need to check if field is contained in xmp preference
                deleteField(di, field);
            } else if (useXmpPrivacyFilter && xmpPreferences.getXmpPrivacyFilter().contains(field)) {
                // erase field instead of adding it
                deleteField(di, field);
            }
        }
    }
    /**
     * Deletes field from document.
     *
     * @param di    The document to delete from.
     * @param field The name of field to delete.
     */

    private static void deleteField(PDDocumentInformation di, Field field) {
        if (StandardField.AUTHOR.equals(field)) {
            di.setAuthor(null);
        } else if (StandardField.TITLE.equals(field)) {
            di.setTitle(null);
        } else if (StandardField.KEYWORDS.equals(field)) {
            di.setKeywords(null);
        } else if (StandardField.ABSTRACT.equals(field)) {
            di.setSubject(null);
        } else {
            di.setCustomMetadataValue("bibtex/" + field, null);
        }
    }

    /**
     * Try to write the given BibTexEntries as DublinCore XMP Schemas
     *
     * Existing DublinCore schemas in the document are removed
     *
     * @param document The pdf document to write to.
     * @param entries  The entries in XMP metadata.
     */
    static void rewriteDublinCore(List<BibEntry> entries, PDDocument document, XmpPreferences xmpPreferences) throws IOException, TransformerException {
        PDDocumentCatalog catalog = document.getDocumentCatalog();
        PDMetadata metaRaw = catalog.getMetadata();

        XMPMetadata meta;
        if (metaRaw == null) {
            meta = XMPMetadata.createXMPMetadata();
        } else {
            try {
                meta = XmpUtilShared.parseXmpMetadata(metaRaw.createInputStream());
                // In case, that the pdf file has no namespace definition for xmp,
                // but metadata in a different format, the parser throws an exception
                // Creating an empty xmp metadata element solves this problem
            } catch (IOException e) {
                meta = XMPMetadata.createXMPMetadata();
            }
        }

        // Remove all current Dublin-Core schemas
        meta.removeSchema(meta.getDublinCoreSchema());

        for (BibEntry entry : entries) {
            DublinCoreSchema dcSchema = DublinCoreSchemaCustom.copyDublinCoreSchema(meta.createAndAddDublinCoreSchema());
            XmpUtilRemover.rewriteToDCSchema(dcSchema, entry, xmpPreferences);
        }

        // Save to stream and then input that stream to the PDF
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XmpSerializer serializer = new XmpSerializer();
        serializer.serialize(meta, os, true);
        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        PDMetadata metadataStream = new PDMetadata(document, is);
        catalog.setMetadata(metadataStream);
    }

    /**
     * Writes the information of the bib entry to the dublin core schema using
     * a custom extractor.
     *
     * @param dcSchema  Dublin core schema, which is filled with the bib entry.
     * @param entry     The entry, which is added to the dublin core metadata.
     * @param xmpPreferences    The user's xmp preferences.
     */
    private static void rewriteToDCSchema(DublinCoreSchema dcSchema, BibEntry entry, XmpPreferences xmpPreferences) {
        DublinCoreExtractor dcExtractor = new DublinCoreExtractor(dcSchema, xmpPreferences, entry);
        dcExtractor.fillDublinCoreSchema();
    }
}
