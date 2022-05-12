package org.jabref.logic.xmp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.xml.transform.TransformerException;

import org.jabref.logic.bibtex.FieldContentFormatter;
import org.jabref.logic.exporter.EmbeddedBibFilePdfExporter;
import org.jabref.logic.formatter.casechanger.UnprotectTermsFormatter;
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

/**
 * Writes given BibEntries into the XMP part of a PDF file.
 *
 * The conversion of a BibEntry to the XMP data (using Dublin Core) is done at
 * {@link DublinCoreExtractor#fillDublinCoreSchema()}
 */
public class XmpUtilWriter {

    private static final String XMP_BEGIN_END_TAG = "?xpacket";

    private static final Logger LOGGER = LoggerFactory.getLogger(XmpUtilWriter.class);

    private UnprotectTermsFormatter unprotectTermsFormatter = new UnprotectTermsFormatter();

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
     * @param file     The path to write to.
     * @param entry    The entry to write.
     * @param database maybenull An optional database which the given bibtex entries belong to, which will be used to
     *                 resolve strings. If the database is null the strings will not be resolved.
     * @throws TransformerException If the entry was malformed or unsupported.
     * @throws IOException          If the file could not be written to or could not be found.
     */
    public void writeXmp(Path file, BibEntry entry,
                                BibDatabase database, XmpPreferences xmpPreferences)
        throws IOException, TransformerException {
        writeXmp(file, List.of(entry), database, xmpPreferences);
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
    private void writeToDCSchema(DublinCoreSchema dcSchema, BibEntry entry, BibDatabase database,
                                        XmpPreferences xmpPreferences) {
        BibEntry resolvedEntry = getDefaultOrDatabaseEntry(entry, database);
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
    private void writeToDCSchema(DublinCoreSchema dcSchema, BibEntry entry, XmpPreferences xmpPreferences) {
        DublinCoreExtractor dcExtractor = new DublinCoreExtractor(dcSchema, xmpPreferences, entry);
        dcExtractor.fillDublinCoreSchema();
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
    private void writeDublinCore(PDDocument document,
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
            writeToDCSchema(dcSchema, entry, null, xmpPreferences);
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
    private String generateXmpStringWithXmpDeclaration(List<BibEntry> entries, XmpPreferences xmpPreferences) {
        XMPMetadata meta = XMPMetadata.createXMPMetadata();
        for (BibEntry entry : entries) {
            DublinCoreSchema dcSchema = meta.createAndAddDublinCoreSchema();
            writeToDCSchema(dcSchema, entry, xmpPreferences);
        }
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            XmpSerializer serializer = new XmpSerializer();
            serializer.serialize(meta, os, true);
            return os.toString(StandardCharsets.UTF_8);
        } catch (TransformerException e) {
            LOGGER.warn("Transformation into XMP not possible: " + e.getMessage(), e);
            return "";
        } catch (UnsupportedEncodingException e) {
            LOGGER.warn("Unsupported encoding to UTF-8 of bib entries in XMP metadata.", e);
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
    public String generateXmpStringWithoutXmpDeclaration(List<BibEntry> entries, XmpPreferences xmpPreferences) {
        String xmpContent = generateXmpStringWithXmpDeclaration(entries, xmpPreferences);
        // remove the <?xpacket *> tags to enable the usage of the CTAN package xmpincl
        Predicate<String> isBeginOrEndTag = s -> s.contains(XMP_BEGIN_END_TAG);

        return Arrays.stream(xmpContent.split(System.lineSeparator()))
                     .filter(isBeginOrEndTag.negate())
                     .collect(Collectors.joining(System.lineSeparator()));
    }

    /**
     * Try to write the given BibTexEntry in the Document Information (the
     * properties of the pdf).
     * <p>
     * Existing fields values are overridden if the bibtex entry has the
     * corresponding value set.
     * <p>
     * The method to write DublineCore is {@link DublinCoreExtractor#fillDublinCoreSchema()}
     *
     * @param document The pdf document to write to.
     * @param entry    The Bibtex entry that is written into the PDF properties. *
     * @param database maybenull An optional database which the given bibtex entries belong to, which will be used to
     *                 resolve strings. If the database is null the strings will not be resolved.
     */
    private void writeDocumentInformation(PDDocument document,
                                          BibEntry entry, BibDatabase database, XmpPreferences xmpPreferences) {
        PDDocumentInformation di = document.getDocumentInformation();
        BibEntry resolvedEntry = getDefaultOrDatabaseEntry(entry, database);

        boolean useXmpPrivacyFilter = xmpPreferences.shouldUseXmpPrivacyFilter();
        for (Field field : resolvedEntry.getFields()) {
            if (useXmpPrivacyFilter && xmpPreferences.getXmpPrivacyFilter().contains(field)) {
                // erase field instead of adding it
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
                continue;
            }

            // LaTeX content is removed from the string for "standard" fields in the PDF
            String value = unprotectTermsFormatter.format(resolvedEntry.getField(field).get());

            if (StandardField.AUTHOR.equals(field)) {
                di.setAuthor(value);
            } else if (StandardField.TITLE.equals(field)) {
                di.setTitle(value);
            } else if (StandardField.KEYWORDS.equals(field)) {
                di.setKeywords(value);
            } else if (StandardField.ABSTRACT.equals(field)) {
                di.setSubject(value);
            } else {
                // We hit the case of an PDF-unsupported field --> write it directly
                di.setCustomMetadataValue("bibtex/" + field, resolvedEntry.getField(field).get());
            }
        }
        di.setCustomMetadataValue("bibtex/entrytype", resolvedEntry.getType().getDisplayName());
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
     * The code for using PDFBox is also used at {@link EmbeddedBibFilePdfExporter#embedBibTex(java.lang.String, java.nio.file.Path)}.
     *
     * @param path          The file to write the entries to.
     * @param bibtexEntries The entries to write to the file. *
     * @param database      maybenull An optional database which the given bibtex entries belong to, which will be used
     *                      to resolve strings. If the database is null the strings will not be resolved.
     * @param xmpPreferences  Write information also in PDF document properties
     * @throws TransformerException If the entry was malformed or unsupported.
     * @throws IOException          If the file could not be written to or could not be found.
     */
    public void writeXmp(Path path,
                                List<BibEntry> bibtexEntries, BibDatabase database,
                                XmpPreferences xmpPreferences)
        throws IOException, TransformerException {
        List<BibEntry> resolvedEntries;
        if (database == null) {
            resolvedEntries = bibtexEntries;
        } else {
            resolvedEntries = database.resolveForStrings(bibtexEntries, false);
        }

        // Read from another file
        // Reason: Apache PDFBox does not support writing while the file is opened
        // See https://issues.apache.org/jira/browse/PDFBOX-4028
        Path newFile = Files.createTempFile("JabRef", "pdf");
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            if (document.isEncrypted()) {
                throw new EncryptedPdfsNotSupportedException();
            }

            // Write schemas (PDDocumentInformation and DublinCoreSchema) to the document metadata
            if (resolvedEntries.size() > 0) {
                writeDocumentInformation(document, resolvedEntries.get(0), null, xmpPreferences);
                writeDublinCore(document, resolvedEntries, null, xmpPreferences);
            }

            // Save updates to original file
            try {
                document.save(newFile.toFile());
                FileUtil.copyFile(newFile, path, true);
            } catch (IOException e) {
                LOGGER.debug("Could not write XMP metadata", e);
                throw new TransformerException("Could not write XMP metadata: " + e.getLocalizedMessage(), e);
            }
        }
        Files.delete(newFile);
    }

    private BibEntry getDefaultOrDatabaseEntry(BibEntry defaultEntry, BibDatabase database) {
        if (database == null) {
            return defaultEntry;
        } else {
            return database.resolveForStrings(defaultEntry, false);
        }
    }
}
