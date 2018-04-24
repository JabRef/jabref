package org.jabref.logic.xmp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.xml.transform.TransformerException;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.strings.StringUtil;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.DublinCoreSchema;
import org.apache.xmpbox.xml.XmpSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmpUtilWriter {

    private static final String XMP_BEGIN_END_TAG = "?xpacket";

    private static final Logger LOGGER = LoggerFactory.getLogger(XmpUtilWriter.class);

    /**
     * Try to write the given BibTexEntry in the XMP-stream of the given
     * PDF-file.
     *
     * Throws an IOException if the file cannot be read or written, so the user
     * can remove a lock or cancel the operation.
     *
     * The method will overwrite existing BibTeX-XMP-data, but keep other
     * existing metadata.
     *
     * This is a convenience method for writeXMP(File, BibEntry).
     *
     * @param filename The filename from which to open the file.
     * @param entry    The entry to write.
     * @param database maybenull An optional database which the given bibtex entries belong to, which will be used to
     *                 resolve strings. If the database is null the strings will not be resolved.
     * @throws TransformerException If the entry was malformed or unsupported.
     * @throws IOException          If the file could not be written to or could not be found.
     */
    public static void writeXmp(String fileName, BibEntry entry,
            BibDatabase database, XmpPreferences xmpPreferences) throws IOException, TransformerException {
        XmpUtilWriter.writeXmp(Paths.get(fileName), entry, database, xmpPreferences);
    }

    /**
     * Try to write the given BibTexEntry in the XMP-stream of the given
     * PDF-file.
     *
     * Throws an IOException if the file cannot be read or written, so the user
     * can remove a lock or cancel the operation.
     *
     * The method will overwrite existing BibTeX-XMP-data, but keep other
     * existing metadata.
     *
     * This is a convenience method for writeXMP(File, Collection).
     *
     * @param path     The path to write to.
     * @param entry    The entry to write.
     * @param database maybenull An optional database which the given bibtex entries belong to, which will be used to
     *                 resolve strings. If the database is null the strings will not be resolved.
     * @throws TransformerException If the entry was malformed or unsupported.
     * @throws IOException          If the file could not be written to or could not be found.
     */
    public static void writeXmp(Path file, BibEntry entry,
            BibDatabase database, XmpPreferences xmpPreferences) throws IOException, TransformerException {
        List<BibEntry> bibEntryList = new ArrayList<>();
        bibEntryList.add(entry);
        XmpUtilWriter.writeXmp(file, bibEntryList, database, xmpPreferences);
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
    private static void writeToDCSchema(DublinCoreSchema dcSchema, BibEntry entry, BibDatabase database,
            XmpPreferences xmpPreferences) {

        BibEntry resolvedEntry = XmpUtilWriter.getDefaultOrDatabaseEntry(entry, database);

        writeToDCSchema(dcSchema, resolvedEntry, xmpPreferences);
    }

    /**
     * Writes the information of the bib entry to the dublin core schema using
     * a custom extractor.
     *
     * @param dcSchema  Dublin core schema, which is filled with the bib entry.
     * @param entry     The entry, which is added to the dublin core metadata.
     * @param xmpPreferences    The user's xmp preferences.
     */
    private static void writeToDCSchema(DublinCoreSchema dcSchema, BibEntry entry,
            XmpPreferences xmpPreferences) {

        DublinCoreExtractor dcExtractor = new DublinCoreExtractor(dcSchema, xmpPreferences, entry);
        dcExtractor.fillDublinCoreSchema();
    }

    /**
     * Try to write the given BibTexEntry as a DublinCore XMP Schema
     *
     * Existing DublinCore schemas in the document are not modified.
     *
     * @param document The pdf document to write to.
     * @param entry    The BibTeX entry that is written as a schema.
     * @param database maybenull An optional database which the given BibTeX entries belong to, which will be used to
     *                 resolve strings. If the database is null the strings will not be resolved.
     */
    public static void writeDublinCore(PDDocument document, BibEntry entry,
            BibDatabase database, XmpPreferences xmpPreferences) throws IOException, TransformerException {

        List<BibEntry> entries = new ArrayList<>();
        entries.add(entry);

        XmpUtilWriter.writeDublinCore(document, entries, database, xmpPreferences);
    }

    /**
     * Try to write the given BibTexEntries as DublinCore XMP Schemas
     *
     * Existing DublinCore schemas in the document are removed
     *
     * @param document The pdf document to write to.
     * @param entries  The BibTeX entries that are written as schemas
     * @param database maybenull An optional database which the given BibTeX entries belong to, which will be used to
     *                 resolve strings. If the database is null the strings will not be resolved.
     */
    private static void writeDublinCore(PDDocument document,
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
            DublinCoreSchema dcSchema = meta.createAndAddDublinCoreSchema();
            XmpUtilWriter.writeToDCSchema(dcSchema, entry, null, xmpPreferences);
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
     * This method generates an xmp metadata string in dublin core format.
     * <br/>
     *
     * @param entries   A list of entries, which are added to the dublin core metadata.
     * @param xmpPreferences    The user's xmp preferences.
     *
     * @return  If something goes wrong (e.g. an exception is thrown), the method returns an empty string,
     *          otherwise it returns the xmp metadata as a string in dublin core format.
     */
    public static String generateXmpStringWithXmpDeclaration(List<BibEntry> entries, XmpPreferences xmpPreferences) {
        XMPMetadata meta = XMPMetadata.createXMPMetadata();
        for (BibEntry entry : entries) {
            DublinCoreSchema dcSchema = meta.createAndAddDublinCoreSchema();
            XmpUtilWriter.writeToDCSchema(dcSchema, entry, xmpPreferences);
        }
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            XmpSerializer serializer = new XmpSerializer();
            serializer.serialize(meta, os, true);
            return os.toString(StandardCharsets.UTF_8.name());
        } catch (TransformerException e) {
            LOGGER.warn("Tranformation into xmp not possible: " + e.getMessage(), e);
            return "";
        } catch (UnsupportedEncodingException e) {
            LOGGER.warn("Unsupported encoding to UTF-8 of bib entries in xmp metadata.", e);
            return "";
        } catch (IOException e) {
            LOGGER.warn("IO Exception thrown by closing the output stream.", e);
            return "";
        }
    }

    /**
     * This method generates an xmp metadata string in dublin core format without the
     * metadata section <?xpacket begin=...>.
     * <br/>
     *
     * @param entries   A list of entries, which are added to the dublin core metadata.
     * @param xmpPreferences    The user's xmp preferences.
     *
     * @return  If something goes wrong (e.g. an exception is thrown), the method returns an empty string,
     *          otherwise it returns the xmp metadata without metadata description as a string in dublin core format.
     */
    public static String generateXmpStringWithoutXmpDeclaration(List<BibEntry> entries, XmpPreferences xmpPreferences) {
        String xmpContent = XmpUtilWriter.generateXmpStringWithXmpDeclaration(entries, xmpPreferences);
        // remove the <?xpacket *> tags to enable the usage of the CTAN package xmpincl
        Predicate<String> isBeginOrEndTag = s -> s.contains(XMP_BEGIN_END_TAG);
        String updatedXmpContent = Arrays.stream(xmpContent.split(System.lineSeparator()))
                .filter(isBeginOrEndTag.negate())
                .map(line -> line.toString())
                .collect(Collectors.joining(System.lineSeparator()));

        return updatedXmpContent;
    }

    /**
     * Try to write the given BibTexEntry in the Document Information (the
     * properties of the pdf).
     *
     * Existing fields values are overriden if the bibtex entry has the
     * corresponding value set.
     *
     * @param document The pdf document to write to.
     * @param entry    The Bibtex entry that is written into the PDF properties. *
     * @param database maybenull An optional database which the given bibtex entries belong to, which will be used to
     *                 resolve strings. If the database is null the strings will not be resolved.
     */
    private static void writeDocumentInformation(PDDocument document,
            BibEntry entry, BibDatabase database, XmpPreferences xmpPreferences) {

        PDDocumentInformation di = document.getDocumentInformation();

        BibEntry resolvedEntry = XmpUtilWriter.getDefaultOrDatabaseEntry(entry, database);

        // Query privacy filter settings
        boolean useXmpPrivacyFilter = xmpPreferences.isUseXMPPrivacyFilter();
        // Fields for which not to write XMP data later on:
        Set<String> filters = new TreeSet<>(xmpPreferences.getXmpPrivacyFilter());

        // Set all the values including key and entryType
        for (Entry<String, String> field : resolvedEntry.getFieldMap().entrySet()) {

            String fieldName = field.getKey();
            String fieldContent = field.getValue();

            if (useXmpPrivacyFilter && filters.contains(fieldName)) {
                // erase field instead of adding it
                if (FieldName.AUTHOR.equals(fieldName)) {
                    di.setAuthor(null);
                } else if (FieldName.TITLE.equals(fieldName)) {
                    di.setTitle(null);
                } else if (FieldName.KEYWORDS.equals(fieldName)) {
                    di.setKeywords(null);
                } else if (FieldName.ABSTRACT.equals(fieldName)) {
                    di.setSubject(null);
                } else {
                    di.setCustomMetadataValue("bibtex/" + fieldName, null);
                }
                continue;
            }

            if (FieldName.AUTHOR.equals(fieldName)) {
                di.setAuthor(fieldContent);
            } else if (FieldName.TITLE.equals(fieldName)) {
                di.setTitle(fieldContent);
            } else if (FieldName.KEYWORDS.equals(fieldName)) {
                di.setKeywords(fieldContent);
            } else if (FieldName.ABSTRACT.equals(fieldName)) {
                di.setSubject(fieldContent);
            } else {
                di.setCustomMetadataValue("bibtex/" + fieldName, fieldContent);
            }
        }
        di.setCustomMetadataValue("bibtex/entrytype", StringUtil.capitalizeFirst(resolvedEntry.getType()));
    }

    /**
     * Try to write the given BibTexEntry in the XMP-stream of the given
     * PDF-file.
     *
     * Throws an IOException if the file cannot be read or written, so the user
     * can remove a lock or cancel the operation.
     *
     * The method will overwrite existing BibTeX-XMP-data, but keep other
     * existing metadata.
     *
     * @param file          The file to write the entries to.
     * @param bibtexEntries The entries to write to the file. *
     * @param database      maybenull An optional database which the given bibtex entries belong to, which will be used
     *                      to resolve strings. If the database is null the strings will not be resolved.
     * @param writePDFInfo  Write information also in PDF document properties
     * @throws TransformerException If the entry was malformed or unsupported.
     * @throws IOException          If the file could not be written to or could not be found.
     */
    public static void writeXmp(Path path,
            List<BibEntry> bibtexEntries, BibDatabase database,
            XmpPreferences xmpPreferences) throws IOException, TransformerException {

        List<BibEntry> resolvedEntries;
        if (database == null) {
            resolvedEntries = bibtexEntries;
        } else {
            resolvedEntries = database.resolveForStrings(bibtexEntries, false);
        }

        try (PDDocument document = PDDocument.load(path.toFile())) {

            if (document.isEncrypted()) {
                throw new EncryptedPdfsNotSupportedException();
            }

            // Write schemas (PDDocumentInformation and DublinCoreSchema) to the document metadata
            if (resolvedEntries.size() > 0) {
                XmpUtilWriter.writeDocumentInformation(document, resolvedEntries.get(0), null, xmpPreferences);
                XmpUtilWriter.writeDublinCore(document, resolvedEntries, null, xmpPreferences);
            }

            // Save
            try {
                document.save(path.toFile());
            } catch (IOException e) {
                LOGGER.debug("Could not write XMP metadata", e);
                throw new TransformerException("Could not write XMP metadata: " + e.getLocalizedMessage(), e);
            }
        }
    }

    private static BibEntry getDefaultOrDatabaseEntry(BibEntry defaultEntry, BibDatabase database) {
        if (database == null) {
            return defaultEntry;
        } else {
            return database.resolveForStrings(defaultEntry, false);
        }
    }
}
