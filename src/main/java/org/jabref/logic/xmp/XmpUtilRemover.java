package org.jabref.logic.xmp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabase;
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
     * The method will only overwrite BibTeX-XMP-data specified in the Xmp Preference
     * Tab, all other metadata are untouched.
     *
     * @param file The filename from which to open the file.
     * @param entry    The entries for file to reference given a select all.
     * @param database (maybe null) An optional database which the given bibtex entries belong to, which will be used to
     *                 resolve strings. If the database is null the strings will not be resolved.
     * @throws TransformerException If the entry was malformed or unsupported.
     * @throws IOException          If the file could not be written to or could not be found.
     */

    public static void deleteXmp(Path file, BibEntry entry,
                                 BibDatabase database, XmpPreferences xmpPreferences) throws IOException, TransformerException {
        List<BibEntry> bibEntryList = new ArrayList<>();
        bibEntryList.add(entry);
        XmpUtilRemover.deleteXmp(file, bibEntryList, database, xmpPreferences);
    }

    /**
     * Tries to delete the given BibTexEntry in the XMP-stream of the given
     * PDF-file.
     *
     * Throws an IOException if the file cannot be read or written, so the user
     * can remove a lock or cancel the operation.
     *
     * The method will only overwrite BibTeX-XMP-data specified in the Xmp Preference
     * Tab, all other metadata are untouched.
     *
     * @param path          The file to write the entries to.
     * @param bibtexEntries The entries for file to reference given a select all.
     * @param database      maybenull An optional database which the given bibtex entries belong to, which will be used
     *                      to resolve strings. If the database is null the strings will not be resolved.
     * @param xmpPreferences  Write information also in PDF document properties
     * @throws TransformerException If the entry was malformed or unsupported.
     * @throws IOException          If the file could not be written to or could not be found.
     */
    public static void deleteXmp(Path path,
                                 List<BibEntry> bibtexEntries, BibDatabase database,
                                 XmpPreferences xmpPreferences) throws IOException, TransformerException {

        List<BibEntry> resolvedEntries;
        if (database == null) {
            resolvedEntries = bibtexEntries;
        } else {
            resolvedEntries = database.resolveForStrings(bibtexEntries, false);
        }
        // uses same hack as PR #8658
        // See: src/main/java/org/jabref/logic/xmp/XmpUtilWriter.java
        // Restating the comment
        // Read from another file
        // Reason: Apache PDFBox does not support writing while the file is opened
        // See https://issues.apache.org/jira/browse/PDFBOX-4028
        Path newFile = Files.createTempFile("JabRef", "pdf");
        try (PDDocument document = Loader.loadPDF(path.toFile())) {

            if (document.isEncrypted()) {
                throw new EncryptedPdfsNotSupportedException();
            }

            // Delete schemas (PDDocumentInformation) from the document metadata
            if (resolvedEntries.size() > 0) {
                XmpUtilRemover.deleteDocumentInformation(document, resolvedEntries.get(0), null, xmpPreferences);
                XmpUtilRemover.rewriteDublinCore(document, resolvedEntries, null, xmpPreferences);
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
     * @param entry    The Bibtex entry that is written into the PDF properties. *
     * @param database maybenull An optional database which the given bibtex entries belong to, which will be used to
     *                 resolve strings. If the database is null the strings will not be resolved.
     */

    private static void deleteDocumentInformation(PDDocument document,
                                                  BibEntry entry, BibDatabase database, XmpPreferences xmpPreferences) {

        PDDocumentInformation di = document.getDocumentInformation();

        BibEntry resolvedEntry = XmpUtilRemover.getDefaultOrDatabaseEntry(entry, database);

        // Query privacy filter settings
        boolean useXmpPrivacyFilter = xmpPreferences.shouldUseXmpPrivacyFilter();

        // Set all the values including key and entryType
        for (Map.Entry<Field, String> fieldValuePair : resolvedEntry.getFieldMap().entrySet()) {
            Field field = fieldValuePair.getKey();
            String fieldContent = fieldValuePair.getValue();

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
     * @param field The field to delete.
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
     * Resolve entry with database
     *
     * @param defaultEntry  The entry to resolve.
     * @param database      The database to check for resolutions.
     */

    private static BibEntry getDefaultOrDatabaseEntry(BibEntry defaultEntry, BibDatabase database) {
        if (database == null) {
            return defaultEntry;
        } else {
            return database.resolveForStrings(defaultEntry, false);
        }
    }

    static void rewriteDublinCore(PDDocument document,
                                List<BibEntry> entries, BibDatabase database, XmpPreferences xmpPreferences)
            throws IOException, TransformerException {

        List<BibEntry> resolvedEntries;
        if (database == null) {
            resolvedEntries = entries;
        } else {
            resolvedEntries = database.resolveForStrings(entries, false);
        }

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

        for (BibEntry entry : resolvedEntries) {
            DublinCoreSchema dcSchema = DublinCoreSchemaCustom.copyDublinCoreSchema(meta.createAndAddDublinCoreSchema());
            XmpUtilRemover.rewriteToDCSchema(dcSchema, entry, null, xmpPreferences);
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
     * @param database  maybenull An optional database which the given bibtex entries belong to, which will be used to
     *                  resolve strings. If the database is null the strings will not be resolved.
     * @param xmpPreferences    The user's xmp preferences.
     */
    private static void rewriteToDCSchema(DublinCoreSchema dcSchema, BibEntry entry, BibDatabase database,
                                        XmpPreferences xmpPreferences) {

        BibEntry resolvedEntry = XmpUtilRemover.getDefaultOrDatabaseEntry(entry, database);

        rewriteToDCSchema(dcSchema, resolvedEntry, xmpPreferences);
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
