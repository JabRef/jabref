package org.jabref.logic.xmp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.transform.TransformerException;

import org.jabref.logic.TypedBibEntry;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.Month;
import org.jabref.model.strings.StringUtil;

import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.xmpbox.DateConverter;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.DublinCoreSchema;
import org.apache.xmpbox.xml.DomXmpParser;
import org.apache.xmpbox.xml.XmpParsingException;
import org.apache.xmpbox.xml.XmpSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * XMPUtils provide support for reading and writing BibTex data as XMP-Metadata
 * in PDF-documents.
 */
public class XMPUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(XMPUtil.class);

    private XMPUtil() {
    }

    /**
     * Convenience method for readXMP(File).
     *
     * @param filename The filename from which to open the file.
     * @return BibtexEntryies found in the PDF or an empty list
     */
    public static List<BibEntry> readXMP(String filename, XMPPreferences xmpPreferences) throws IOException {
        return XMPUtil.readXMP(new File(filename), xmpPreferences);
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
     * This is a convenience method for writeXMP(File, BibEntry).
     *
     * @param filename The filename from which to open the file.
     * @param entry    The entry to write.
     * @param database maybenull An optional database which the given bibtex entries belong to, which will be used to
     *                 resolve strings. If the database is null the strings will not be resolved.
     * @throws TransformerException If the entry was malformed or unsupported.
     * @throws IOException          If the file could not be written to or could not be found.
     */
    public static void writeXMP(String fileName, BibEntry entry,
            BibDatabase database, XMPPreferences xmpPreferences) throws IOException, TransformerException {
        XMPUtil.writeXMP(Paths.get(fileName), entry, database, xmpPreferences);
    }

    /**
     * Loads the specified file with the basic pdfbox functionality and uses an empty string as default password.
     *
     * @param file The file to load.
     * @return
     * @throws IOException from the underlying {@link PDDocument#load(File)}
     */
    public static PDDocument loadWithAutomaticDecryption(File file) throws IOException {
        // try to load the document
        // also uses an empty string as default password
        PDDocument doc = PDDocument.load(file);
        return doc;
    }

    /**
     * Try to read the given BibTexEntry from the XMP-stream of the given
     * inputstream containing a PDF-file.
     *
     * Only supports Dublin Core as a metadata format.
     *
     * @param file The file to read from.
     * @return list of BibEntries retrieved from the stream. May be empty, but never null
     * @throws IOException Throws an IOException if the file cannot be read, so the user than remove a lock or cancel
     *                     the operation.
     */
    public static List<BibEntry> readXMP(File file, XMPPreferences xmpPreferences)
            throws IOException {

        List<BibEntry> result = new LinkedList<>();

        try (PDDocument document = loadWithAutomaticDecryption(file)) {
            Optional<XMPMetadata> meta = XMPUtil.getXMPMetadata(document);

            if (meta.isPresent()) {
                // Only support Dublin Core since JabRef 4.2
                DublinCoreSchema schema = meta.get().getDublinCoreSchema();
                if (schema != null) {
                    Optional<BibEntry> entry = XMPUtil.getBibtexEntry(schema, xmpPreferences);

                    if (entry.isPresent()) {
                        if (entry.get().getType() == null) {
                            entry.get().setType(BibEntry.DEFAULT_TYPE);
                        }
                        result.add(entry.get());
                    }
                }
            }
            if (result.isEmpty()) {
                // If we did not find any XMP metadata, search for non XMP metadata
                PDDocumentInformation documentInformation = document.getDocumentInformation();
                Optional<BibEntry> entry = XMPUtil.getBibtexEntryFromDocumentInformation(documentInformation);
                entry.ifPresent(result::add);
            }
        }

        // return empty list, if no metadata was found
        if (result.isEmpty()) {
            return Collections.emptyList();
        }
        return result;
    }

    public static Collection<BibEntry> readXMP(Path filePath, XMPPreferences xmpPreferences) throws IOException {
        return readXMP(filePath.toFile(), xmpPreferences);
    }

    /**
     * Helper function for retrieving a BibEntry from the
     * PDDocumentInformation in a PDF file.
     *
     * To understand how to get hold of a PDDocumentInformation have a look in
     * the test cases for XMPUtil.
     *
     * The BibEntry is build by mapping individual fields in the document
     * information (like author, title, keywords) to fields in a bibtex entry.
     *
     * @param di The document information from which to build a BibEntry.
     * @return The bibtex entry found in the document information.
     */
    public static Optional<BibEntry> getBibtexEntryFromDocumentInformation(
            PDDocumentInformation di) {

        BibEntry entry = new BibEntry();
        entry.setType(BibEntry.DEFAULT_TYPE);

        String s = di.getAuthor();
        if (s != null) {
            entry.setField(FieldName.AUTHOR, s);
        }

        s = di.getTitle();
        if (s != null) {
            entry.setField(FieldName.TITLE, s);
        }

        s = di.getKeywords();
        if (s != null) {
            entry.setField(FieldName.KEYWORDS, s);
        }

        s = di.getSubject();
        if (s != null) {
            entry.setField(FieldName.ABSTRACT, s);
        }

        COSDictionary dict = di.getCOSObject();
        for (Map.Entry<COSName, COSBase> o : dict.entrySet()) {
            String key = o.getKey().getName();
            if (key.startsWith("bibtex/")) {
                String value = dict.getString(key);
                key = key.substring("bibtex/".length());
                if (BibEntry.TYPE_HEADER.equals(key)) {
                    entry.setType(value);
                } else {
                    entry.setField(key, value);
                }
            }
        }

        // Return empty Optional if no values were found
        if (entry.getFieldNames().isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(entry);
        }
    }

    /**
     * Editor in BibTex - Contributor in DublinCore
     *
     * @param entry The BibEntry object, which is filled during metadata extraction.
     * @param dcSchema Metadata in DublinCore format.
     */
    private static void setEditor(BibEntry entry, DublinCoreSchema dcSchema) {
        List<String> contributors = dcSchema.getContributors();
        if ((contributors != null) && !contributors.isEmpty()) {
            entry.setField(FieldName.EDITOR, String.join(" and ", contributors));
        }
    }

    /**
     * Author in BibTex - Creator in DublinCore
     *
     * @param entry The BibEntry object, which is filled during metadata extraction.
     * @param dcSchema Metadata in DublinCore format.
     */
    private static void setAuthor(BibEntry entry, DublinCoreSchema dcSchema) {
        List<String> creators = dcSchema.getCreators();
        if ((creators != null) && !creators.isEmpty()) {
            entry.setField(FieldName.AUTHOR, String.join(" and ", creators));
        }
    }

    /**
     * Year + Month in BibTex - Date in DublinCore is a combination of year and month information
     *
     * @param entry The BibEntry object, which is filled during metadata extraction.
     * @param dcSchema Metadata in DublinCore format.
     */
    private static void setYearAndMonth(BibEntry entry, DublinCoreSchema dcSchema) {
        List<String> dates = dcSchema.getUnqualifiedSequenceValueList("date");
        if ((dates != null) && !dates.isEmpty()) {
            String date = dates.get(0).trim();
            Calendar c = null;
            try {
                c = DateConverter.toCalendar(date);
            } catch (IOException ignored) {
                // Ignored
            }
            if (c != null) {
                entry.setField(FieldName.YEAR, String.valueOf(c.get(Calendar.YEAR)));
                if (date.length() > 4) {
                    Optional<Month> month = Month.getMonthByNumber(c.get(Calendar.MONTH) + 1);
                    month.ifPresent(entry::setMonth);
                }
            }
        }
    }

    /**
     * Abstract in BibTex - Description in DublinCore
     *
     * @param entry The BibEntry object, which is filled during metadata extraction.
     * @param dcSchema Metadata in DublinCore format.
     */
    private static void setAbstract(BibEntry entry, DublinCoreSchema dcSchema) {
        String s = dcSchema.getDescription();
        if (s != null) {
            entry.setField(FieldName.ABSTRACT, s);
        }
    }

    /**
     * DOI in BibTex - Identifier in DublinCore
     *
     * @param entry The BibEntry object, which is filled during metadata extraction.
     * @param dcSchema Metadata in DublinCore format.
     */
    private static void setDOI(BibEntry entry, DublinCoreSchema dcSchema) {
        String s = dcSchema.getIdentifier();
        if (s != null) {
            entry.setField(FieldName.DOI, s);
        }
    }

    /**
     * Publisher are equivalent in both formats (BibTex and DublinCore)
     *
     * @param entry The BibEntry object, which is filled during metadata extraction.
     * @param dcSchema Metadata in DublinCore format.
     */
    private static void setPublisher(BibEntry entry, DublinCoreSchema dcSchema) {
        List<String> publishers = dcSchema.getPublishers();
        if ((publishers != null) && !publishers.isEmpty()) {
            entry.setField(FieldName.PUBLISHER, String.join(" and ", publishers));
        }
    }

    /**
     * This method sets all fields, which are custom in bibtext and therefore supported by jabref, but which are not included in the DublinCore format.
     * <p/>
     * The relation attribute of DublinCore is abused to insert these custom fields.
     *
     * @param entry The BibEntry object, which is filled during metadata extraction.
     * @param dcSchema Metadata in DublinCore format.
     */
    private static void setBibTexFields(BibEntry entry, DublinCoreSchema dcSchema) {
        List<String> relationships = dcSchema.getRelations();
        if (relationships != null) {
            for (String r : relationships) {
                if (r.startsWith("bibtex/")) {
                    r = r.substring("bibtex/".length());
                    int i = r.indexOf('/');
                    if (i != -1) {
                        entry.setField(r.substring(0, i), r.substring(i + 1));
                    }
                }
            }
        }
    }

    /**
     * Rights are equivalent in both formats (BibTex and DublinCore)
     *
     * @param entry The BibEntry object, which is filled during metadata extraction.
     * @param dcSchema Metadata in DublinCore format.
     */
    private static void setRights(BibEntry entry, DublinCoreSchema dcSchema) {
        String s = dcSchema.getRights();
        if (s != null) {
            entry.setField("rights", s);
        }
    }

    /**
     * Source is equivalent in both formats (BibTex and DublinCore)
     *
     * @param entry The BibEntry object, which is filled during metadata extraction.
     * @param dcSchema Metadata in DublinCore format.
     */
    private static void setSource(BibEntry entry, DublinCoreSchema dcSchema) {
        String s = dcSchema.getSource();
        if (s != null) {
            entry.setField("source", s);
        }
    }

    /**
     * Keywords in BibTex - Subjects in DublinCore
     * @param entry The BibEntry object, which is filled during metadata extraction.
     * @param dcSchema Metadata in DublinCore format.
     */
    private static void setSubject(BibEntry entry, DublinCoreSchema dcSchema,
            Character keyWordSeparator) {
        List<String> subjects = dcSchema.getSubjects();
        if (subjects != null) {
            entry.addKeywords(subjects, keyWordSeparator);
        }
    }

    /**
     * Title is equivalent in both formats (BibTex and DublinCore)
     *
     * @param entry The BibEntry object, which is filled during metadata extraction.
     * @param dcSchema Metadata in DublinCore format.
     */
    private static void setTitle(BibEntry entry, DublinCoreSchema dcSchema) {
        String s = dcSchema.getTitle();
        if (s != null) {
            entry.setField(FieldName.TITLE, s);
        }
    }

    /**
     * Type is equivalent in both formats (BibTex and DublinCore)
     *
     * @param entry The BibEntry object, which is filled during metadata extraction.
     * @param dcSchema Metadata in DublinCore format.
     */
    private static void setType(BibEntry entry, DublinCoreSchema dcSchema) {
        List<String> l = dcSchema.getTypes();
        if ((l != null) && !l.isEmpty()) {
            String s = l.get(0);
            if (s != null) {
                entry.setType(s);
            }
        }
    }

    /**
     * Helper function for retrieving a BibEntry from the DublinCore metadata
     * in a PDF file.
     *
     * To understand how to get hold of a DublinCore have a look in the
     * test cases for XMPUtil.
     *
     * The BibEntry is build by mapping individual fields in the dublin core
     * (like creator, title, subject) to fields in a bibtex entry.
     *
     * @param dcSchema The document information from which to build a BibEntry.
     * @return The bibtex entry found in the document information.
     */
    public static Optional<BibEntry> getBibtexEntry(DublinCoreSchema dcSchema,
            XMPPreferences xmpPreferences) {

        BibEntry entry = new BibEntry();

        XMPUtil.setEditor(entry, dcSchema);

        XMPUtil.setAuthor(entry, dcSchema);

        XMPUtil.setYearAndMonth(entry, dcSchema);

        XMPUtil.setAbstract(entry, dcSchema);

        XMPUtil.setDOI(entry, dcSchema);

        XMPUtil.setPublisher(entry, dcSchema);

        XMPUtil.setBibTexFields(entry, dcSchema);

        XMPUtil.setRights(entry, dcSchema);

        XMPUtil.setSource(entry, dcSchema);

        XMPUtil.setSubject(entry, dcSchema, xmpPreferences.getKeywordSeparator());

        XMPUtil.setTitle(entry, dcSchema);

        XMPUtil.setType(entry, dcSchema);

        if (entry.getFieldNames().isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(entry);
        }
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
     * @param file     The file to write to.
     * @param entry    The entry to write.
     * @param database maybenull An optional database which the given bibtex entries belong to, which will be used to
     *                 resolve strings. If the database is null the strings will not be resolved.
     * @throws TransformerException If the entry was malformed or unsupported.
     * @throws IOException          If the file could not be written to or could not be found.
     */
    public static void writeXMP(Path file, BibEntry entry,
            BibDatabase database, XMPPreferences xmpPreferences) throws IOException, TransformerException {
        List<BibEntry> l = new LinkedList<>();
        l.add(entry);
        XMPUtil.writeXMP(file.toFile(), l, database, true, xmpPreferences);
    }

    /**
     * @return empty Optional if no metadata has been found
     */
    private static Optional<XMPMetadata> getXMPMetadata(PDDocument document) throws IOException {
        PDDocumentCatalog catalog = document.getDocumentCatalog();
        PDMetadata metaRaw = catalog.getMetadata();

        if (metaRaw == null) {
            return Optional.empty();
        }

        XMPMetadata meta = XMPUtil.parseXMPMetadata(metaRaw.createInputStream());

        return Optional.of(meta);
    }

    /**
     * Will read the XMPMetadata from the given pdf file, closing the file
     * afterwards.
     *
     * @param file The file to read the XMPMetadata from.
     * @return The XMPMetadata object found in the file
     */
    public static Optional<XMPMetadata> readRawXMP(File file) throws IOException {
        try (PDDocument document = loadWithAutomaticDecryption(file)) {
            return XMPUtil.getXMPMetadata(document);
        }
    }

    private static void writeToDCSchema(DublinCoreSchema dcSchema, BibEntry entry, BibDatabase database,
            XMPPreferences xmpPreferences) {

        BibEntry resolvedEntry;
        if (database == null) {
            resolvedEntry = entry;
        } else {
            resolvedEntry = database.resolveForStrings(entry, false);
        }

        // Query privacy filter settings
        boolean useXmpPrivacyFilter = xmpPreferences.isUseXMPPrivacyFilter();
        // Fields for which not to write XMP data later on:
        Set<String> filters = new TreeSet<>(xmpPreferences.getXmpPrivacyFilter());

        // Set all the values including key and entryType

        for (Entry<String, String> field : resolvedEntry.getFieldMap().entrySet()) {

            if (useXmpPrivacyFilter && filters.contains(field.getKey())) {
                continue;
            }

            // Bibtex-Fields used: editor, Field: 'dc:contributor'
            if (FieldName.EDITOR.equals(field.getKey())) {
                String authors = field.getValue();
                AuthorList list = AuthorList.parse(authors);
                for (Author author : list.getAuthors()) {
                    dcSchema.addContributor(author.getFirstLast(false));
                }
            }
            // Bibtex-Fields used: author, Field: 'dc:creator'
            else if (FieldName.AUTHOR.equals(field.getKey())) {
                String authors = field.getValue();
                AuthorList list = AuthorList.parse(authors);

                for (Author author : list.getAuthors()) {
                    dcSchema.addCreator(author.getFirstLast(false));
                }
            }
            // Bibtex-Fields used: year, month, Field: 'dc:date'
            else if (FieldName.YEAR.equals(field.getKey())) {
                entry.getFieldOrAlias(FieldName.DATE)
                        .ifPresent(publicationDate -> dcSchema.addUnqualifiedSequenceValue("date", publicationDate));
            }
            // Bibtex-Fields used: abstract, Field: 'dc:description'
            else if (FieldName.ABSTRACT.equals(field.getKey())) {
                dcSchema.setDescription(field.getValue());
            }
            // Bibtex-Fields used: doi, Field: 'dc:identifier'
            else if (FieldName.DOI.equals(field.getKey())) {
                dcSchema.setIdentifier(field.getValue());
            }
            // Bibtex-Fields used: publisher, Field: dc:publisher
            else if (FieldName.PUBLISHER.equals(field.getKey())) {
                dcSchema.addPublisher(field.getValue());
            }
            // Bibtex-Fields used: keywords, Field: 'dc:subject'
            else if (FieldName.KEYWORDS.equals(field.getKey())) {
                String o = field.getValue();
                String[] keywords = o.split(",");
                for (String keyword : keywords) {
                    dcSchema.addSubject(keyword.trim());
                }
            }
            // Bibtex-Fields used: title, Field: 'dc:title'
            else if (FieldName.TITLE.equals(field.getKey())) {
                dcSchema.setTitle(field.getValue());
            }
            // All others (+ bibtex key) get packaged in the relation attribute
            else {
                String o = field.getValue();
                dcSchema.addRelation("bibtex/" + field.getKey() + '/' + o);
            }
        }

        dcSchema.setFormat("application/pdf");

        // Bibtex-Fields used: entrytype, Field: 'dc:type'
        TypedBibEntry typedEntry = new TypedBibEntry(entry, BibDatabaseMode.BIBTEX);
        String o = typedEntry.getTypeForDisplay();
        if (!o.isEmpty()) {
            dcSchema.addType(o);
        }
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
            BibDatabase database, XMPPreferences xmpPreferences) throws IOException, TransformerException {

        List<BibEntry> entries = new ArrayList<>();
        entries.add(entry);

        XMPUtil.writeDublinCore(document, entries, database, xmpPreferences);
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
            Collection<BibEntry> entries, BibDatabase database, XMPPreferences xmpPreferences)
            throws IOException, TransformerException {

        Collection<BibEntry> resolvedEntries;
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
            meta = XMPUtil.parseXMPMetadata(metaRaw.createInputStream());
        }

        // Remove all current Dublin-Core schemas
        meta.removeSchema(meta.getDublinCoreSchema());

        for (BibEntry entry : resolvedEntries) {
            DublinCoreSchema dcSchema = meta.createAndAddDublinCoreSchema();
            XMPUtil.writeToDCSchema(dcSchema, entry, null, xmpPreferences);
            meta.addSchema(dcSchema);
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
            BibEntry entry, BibDatabase database, XMPPreferences xmpPreferences) {

        PDDocumentInformation di = document.getDocumentInformation();

        BibEntry resolvedEntry;
        if (database == null) {
            resolvedEntry = entry;
        } else {
            resolvedEntry = database.resolveForStrings(entry, false);
        }

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
    public static void writeXMP(File file,
            Collection<BibEntry> bibtexEntries, BibDatabase database,
            boolean writePDFInfo, XMPPreferences xmpPreferences) throws IOException, TransformerException {

        Collection<BibEntry> resolvedEntries;
        if (database == null) {
            resolvedEntries = bibtexEntries;
        } else {
            resolvedEntries = database.resolveForStrings(bibtexEntries, false);
        }

        try (PDDocument document = PDDocument.load(file.getAbsoluteFile())) {
            if (document.isEncrypted()) {
                throw new EncryptedPdfsNotSupportedException();
            }

            if (writePDFInfo && (resolvedEntries.size() == 1)) {
                XMPUtil.writeDocumentInformation(document, resolvedEntries
                        .iterator()
                        .next(), null, xmpPreferences);
                XMPUtil.writeDublinCore(document, resolvedEntries, null, xmpPreferences);
            }

            PDDocumentCatalog catalog = document.getDocumentCatalog();
            PDMetadata metaRaw = catalog.getMetadata();

            XMPMetadata meta;
            if (metaRaw == null) {
                meta = XMPMetadata.createXMPMetadata();
            } else {
                meta = XMPUtil.parseXMPMetadata(metaRaw.createInputStream());
            }

            // Save to stream and then input that stream to the PDF
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            XmpSerializer serializer = new XmpSerializer();
            serializer.serialize(meta, os, true);
            ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
            PDMetadata metadataStream = new PDMetadata(document, is);
            catalog.setMetadata(metadataStream);

            // Save
            try {
                document.save(file.getAbsolutePath());
            } catch (IOException e) {
                LOGGER.debug("Could not write XMP metadata", e);
                throw new TransformerException("Could not write XMP metadata: " + e.getLocalizedMessage(), e);
            }
        }
    }

    private static XMPMetadata parseXMPMetadata(InputStream is) throws IOException {
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
     * see XMPUtil.hasMetadata(InputStream)
     */
    public static boolean hasMetadata(Path path, XMPPreferences xmpPreferences) {
        try (InputStream inputStream = Files.newInputStream(path, StandardOpenOption.READ)) {
            return hasMetadata(path.toFile(), xmpPreferences);
        } catch (IOException e) {
            // happens if no metadata is found, no reason to log the exception
            return false;
        }
    }

    /**
     * Will try to read XMP metadata from the given file, returning whether
     * metadata was found.
     *
     * Caution: This method is as expensive as it is reading the actual metadata
     * itself from the PDF.
     *
     * @param inputStream The inputStream to read the PDF from.
     * @return whether a BibEntry was found in the given PDF.
     */
    public static boolean hasMetadata(File file, XMPPreferences xmpPreferences) {
        try {
            List<BibEntry> bibEntries = XMPUtil.readXMP(file, xmpPreferences);
            return !bibEntries.isEmpty();
        } catch (EncryptedPdfsNotSupportedException ex) {
            LOGGER.info("Encryption not supported by XMPUtil");
            return false;
        } catch (IOException e) {
            // happens if no metadata is found, no reason to log the exception
            return false;
        }
    }
}
