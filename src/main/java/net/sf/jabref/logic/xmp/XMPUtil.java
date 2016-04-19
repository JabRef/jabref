/*  Copyright (C) 2003-2016 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.logic.xmp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.transform.TransformerException;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.bibtex.BibEntryWriter;
import net.sf.jabref.exporter.LatexFieldFormatter;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.TypedBibEntry;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.Author;
import net.sf.jabref.model.entry.AuthorList;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.EntryUtil;
import net.sf.jabref.model.entry.MonthUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jempbox.impl.DateConverter;
import org.apache.jempbox.impl.XMLUtil;
import org.apache.jempbox.xmp.XMPMetadata;
import org.apache.jempbox.xmp.XMPSchema;
import org.apache.jempbox.xmp.XMPSchemaDublinCore;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.exceptions.CryptographyException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.encryption.BadSecurityHandlerException;
import org.apache.pdfbox.pdmodel.encryption.StandardDecryptionMaterial;
import org.w3c.dom.Document;

/**
 * XMPUtils provide support for reading and writing BibTex data as XMP-Metadata
 * in PDF-documents.
 */
public class XMPUtil {

    private static final Log LOGGER = LogFactory.getLog(XMPUtil.class);


    /**
     * Convenience method for readXMP(File).
     *
     * @param filename
     *            The filename from which to open the file.
     * @return BibtexEntryies found in the PDF or an empty list
     * @throws IOException
     */
    public static List<BibEntry> readXMP(String filename) throws IOException {
        return XMPUtil.readXMP(new File(filename));
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
     * @param filename
     *            The filename from which to open the file.
     * @param entry
     *            The entry to write.
     * @param database
     *            maybenull An optional database which the given bibtex entries
     *            belong to, which will be used to resolve strings. If the
     *            database is null the strings will not be resolved.
     * @throws TransformerException
     *             If the entry was malformed or unsupported.
     * @throws IOException
     *             If the file could not be written to or could not be found.
     */
    public static void writeXMP(String filename, BibEntry entry,
            BibDatabase database) throws IOException, TransformerException {
        XMPUtil.writeXMP(new File(filename), entry, database);
    }

    /**
     * Try to read the BibTexEntries from the XMP-stream of the given PDF-file.
     *
     * @param file
     *            The file to read from.
     *
     * @throws IOException
     *             Throws an IOException if the file cannot be read, so the user
     *             than remove a lock or cancel the operation.
     */
    public static List<BibEntry> readXMP(File file) throws IOException {
        List<BibEntry> result = Collections.emptyList();
        try (FileInputStream inputStream = new FileInputStream(file)) {
            result = XMPUtil.readXMP(inputStream);
        }
        return result;
    }

    public static PDDocument loadWithAutomaticDecryption(InputStream inputStream)
            throws IOException, EncryptedPdfsNotSupportedException {
        PDDocument doc = PDDocument.load(inputStream);
        if (doc.isEncrypted()) {
            // try the empty string as user password
            StandardDecryptionMaterial sdm = new StandardDecryptionMaterial("");
            try {
                doc.openProtection(sdm);
            } catch (BadSecurityHandlerException | CryptographyException e) {
                LOGGER.error("Cannot handle encrypted PDF: " + e.getMessage());
                throw new EncryptedPdfsNotSupportedException();
            }
        }
        return doc;
    }

    /**
     * Try to read the given BibTexEntry from the XMP-stream of the given
     * inputstream containing a PDF-file.
     *
     * @param inputStream
     *            The inputstream to read from.
     *
     * @throws IOException
     *             Throws an IOException if the file cannot be read, so the user
     *             than remove a lock or cancel the operation.
     *
     * @return list of BibEntries retrieved from the stream. May be empty, but never null
     */
    public static List<BibEntry> readXMP(InputStream inputStream)
            throws IOException {

        List<BibEntry> result = new LinkedList<>();

        try (PDDocument document = loadWithAutomaticDecryption(inputStream)) {
            Optional<XMPMetadata> meta = XMPUtil.getXMPMetadata(document);

            if (meta.isPresent()) {

                List<XMPSchema> schemas = meta.get().getSchemasByNamespaceURI(XMPSchemaBibtex.NAMESPACE);

                for (XMPSchema schema : schemas) {
                    XMPSchemaBibtex bib = (XMPSchemaBibtex) schema;

                    BibEntry entry = bib.getBibtexEntry();
                    if (entry.getType() == null) {
                        entry.setType("misc");
                    }
                    result.add(entry);
                }

                // If we did not find anything have a look if a Dublin Core exists
                if (result.isEmpty()) {
                    schemas = meta.get().getSchemasByNamespaceURI(XMPSchemaDublinCore.NAMESPACE);
                    for (XMPSchema schema : schemas) {
                        XMPSchemaDublinCore dc = (XMPSchemaDublinCore) schema;

                        Optional<BibEntry> entry = XMPUtil.getBibtexEntryFromDublinCore(dc);

                        if (entry.isPresent()) {
                            if (entry.get().getType() == null) {
                                entry.get().setType("misc");
                            }
                            result.add(entry.get());
                        }
                    }
                }
            }
            if (result.isEmpty()) {
                // If we did not find any XMP metadata, search for non XMP metadata
                PDDocumentInformation documentInformation = document.getDocumentInformation();
                Optional<BibEntry> entry = XMPUtil.getBibtexEntryFromDocumentInformation(documentInformation);
                if (entry.isPresent()) {
                    result.add(entry.get());
                }
            }
        }

        // return empty list, if no metadata was found
        if (result.isEmpty()) {
            return Collections.emptyList();
        }
        return result;
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
     * @param di
     *            The document information from which to build a BibEntry.
     *
     * @return The bibtex entry found in the document information.
     */
    public static Optional<BibEntry> getBibtexEntryFromDocumentInformation(
            PDDocumentInformation di) {

        BibEntry entry = new BibEntry();
        entry.setType("misc");

        String s = di.getAuthor();
        if (s != null) {
            entry.setField("author", s);
        }

        s = di.getTitle();
        if (s != null) {
            entry.setField("title", s);
        }

        s = di.getKeywords();
        if (s != null) {
            entry.setField("keywords", s);
        }

        s = di.getSubject();
        if (s != null) {
            entry.setField("abstract", s);
        }

        COSDictionary dict = di.getDictionary();
        for (Map.Entry<COSName, COSBase> o : dict.entrySet()) {
            String key = o.getKey().getName();
            if (key.startsWith("bibtex/")) {
                String value = dict.getString(key);
                key = key.substring("bibtex/".length());
                if ("entrytype".equals(key)) {
                    entry.setType(value);
                } else {
                    entry.setField(key, value);
                }
            }
        }

        // Return empty Optional if no values were found
        return entry.getFieldNames().isEmpty() ? Optional.empty() : Optional.of(entry);
    }

    /**
     * Helper function for retrieving a BibEntry from the DublinCore metadata
     * in a PDF file.
     *
     * To understand how to get hold of a XMPSchemaDublinCore have a look in the
     * test cases for XMPUtil.
     *
     * The BibEntry is build by mapping individual fields in the dublin core
     * (like creator, title, subject) to fields in a bibtex entry.
     *
     * @param dcSchema
     *            The document information from which to build a BibEntry.
     *
     * @return The bibtex entry found in the document information.
     */
    public static Optional<BibEntry> getBibtexEntryFromDublinCore(XMPSchemaDublinCore dcSchema) {

        BibEntry entry = new BibEntry();

        /**
         * Contributor -> Editor
         */
        List<String> contributors = dcSchema.getContributors();
        if ((contributors != null) && !contributors.isEmpty()) {
            entry.setField("editor", String.join(" and ", contributors));
        }

        /**
         * Author -> Creator
         */
        List<String> creators = dcSchema.getCreators();
        if ((creators != null) && !creators.isEmpty()) {
            entry.setField("author", String.join(" and ", creators));
        }

        /**
         * Year + Month -> Date
         */
        List<String> dates = dcSchema.getSequenceList("dc:date");
        if ((dates != null) && !dates.isEmpty()) {
            String date = dates.get(0).trim();
            Calendar c = null;
            try {
                c = DateConverter.toCalendar(date);
            } catch (IOException ignored) {
                // Ignored
            }
            if (c != null) {
                entry.setField("year", String.valueOf(c.get(Calendar.YEAR)));
                if (date.length() > 4) {
                    entry.setField("month", MonthUtil.getMonthByIndex(c.get(Calendar.MONTH)).bibtexFormat);
                }
            }
        }

        /**
         * Abstract -> Description
         */
        String s = dcSchema.getDescription();
        if (s != null) {
            entry.setField("abstract", s);
        }

        /**
         * Identifier -> DOI
         */
        s = dcSchema.getIdentifier();
        if (s != null) {
            entry.setField("doi", s);
        }

        /**
         * Publisher -> Publisher
         */
        List<String> publishers = dcSchema.getPublishers();
        if ((publishers != null) && !publishers.isEmpty()) {
            entry.setField("publishers", String.join(" and ", publishers));
        }

        /**
         * Relation -> bibtexkey
         *
         * We abuse the relationship attribute to store all other values in the
         * bibtex document
         */
        List<String> relationships = dcSchema.getRelationships();
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

        /**
         * Rights -> Rights
         */
        s = dcSchema.getRights();
        if (s != null) {
            entry.setField("rights", s);
        }

        /**
         * Source -> Source
         */
        s = dcSchema.getSource();
        if (s != null) {
            entry.setField("source", s);
        }

        /**
         * Subject -> Keywords
         */
        List<String> subjects = dcSchema.getSubjects();
        if (subjects != null) {
            entry.addKeywords(subjects);
        }

        /**
         * Title -> Title
         */
        s = dcSchema.getTitle();
        if (s != null) {
            entry.setField("title", s);
        }

        /**
         * Type -> Type
         */
        List<String> l = dcSchema.getTypes();
        if ((l != null) && !l.isEmpty()) {
            s = l.get(0);
            if (s != null) {
                entry.setType(s);
            }
        }

        return entry.getFieldNames().isEmpty() ? Optional.empty() : Optional.of(entry);
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
     * @param file
     *            The file to write to.
     * @param entry
     *            The entry to write.
     * @param database
     *            maybenull An optional database which the given bibtex entries
     *            belong to, which will be used to resolve strings. If the
     *            database is null the strings will not be resolved.
     * @throws TransformerException
     *             If the entry was malformed or unsupported.
     * @throws IOException
     *             If the file could not be written to or could not be found.
     */
    public static void writeXMP(File file, BibEntry entry,
            BibDatabase database) throws IOException, TransformerException {
        List<BibEntry> l = new LinkedList<>();
        l.add(entry);
        XMPUtil.writeXMP(file, l, database, true);
    }

    /**
     * Write the given BibtexEntries as XMP-metadata text to the given stream.
     *
     * The text that is written to the stream contains a complete XMP-document.
     *
     * @param bibtexEntries
     *            The BibtexEntries to write XMP-metadata for.
     * @param database
     *            maybenull An optional database which the given bibtex entries
     *            belong to, which will be used to resolve strings. If the
     *            database is null the strings will not be resolved.
     * @throws TransformerException
     *             Thrown if the bibtexEntries could not transformed to XMP.
     * @throws IOException
     *             Thrown if an IOException occured while writing to the stream.
     *
     * @see #toXMP(java.util.Collection, BibDatabase) if you don't need strings to be
     *      resolved.
     */
    private static void toXMP(Collection<BibEntry> bibtexEntries,
            BibDatabase database, OutputStream outputStream)
                    throws IOException, TransformerException {

        Collection<BibEntry> resolvedEntries;
        if (database == null) {
            resolvedEntries = bibtexEntries;
        } else {
            resolvedEntries = database.resolveForStrings(bibtexEntries, true);
        }

        XMPMetadata x = new XMPMetadata();

        for (BibEntry e : resolvedEntries) {
            XMPSchemaBibtex schema = new XMPSchemaBibtex(x);
            x.addSchema(schema);
            schema.setBibtexEntry(e);
        }

        x.save(outputStream);
    }

    /**
     * Convenience method for toXMP(Collection<BibEntry>, BibDatabase,
     * OutputStream) returning a String containing the XMP-metadata of the given
     * collection of BibtexEntries.
     *
     * The resulting metadata string is wrapped as a complete XMP-document.
     *
     * @param bibtexEntries
     *            The BibtexEntries to return XMP-metadata for.
     * @param database
     *            maybenull An optional database which the given bibtex entries
     *            belong to, which will be used to resolve strings. If the
     *            database is null the strings will not be resolved.
     * @return The XMP representation of the given bibtexEntries.
     * @throws TransformerException
     *             Thrown if the bibtexEntries could not transformed to XMP.
     */
    public static String toXMP(Collection<BibEntry> bibtexEntries,
            BibDatabase database) throws TransformerException {
        try {
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            XMPUtil.toXMP(bibtexEntries, database, bs);
            return bs.toString();
        } catch (IOException e) {
            throw new TransformerException(e);
        }
    }

    /**
     * Will read the XMPMetadata from the given pdf file, closing the file
     * afterwards.
     *
     * @param inputStream
     *            The inputStream representing a PDF-file to read the
     *            XMPMetadata from.
     * @return The XMPMetadata object found in the file
     */
    private static Optional<XMPMetadata> readRawXMP(InputStream inputStream) throws IOException {
        try (PDDocument document = loadWithAutomaticDecryption(inputStream)) {
            return XMPUtil.getXMPMetadata(document);
        }
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

        Document parseResult;
        try (InputStream is = metaRaw.createInputStream()) {
            parseResult = XMLUtil.parse(is);
        }
        XMPMetadata meta = new XMPMetadata(parseResult);
        meta.addXMLNSMapping(XMPSchemaBibtex.NAMESPACE, XMPSchemaBibtex.class);
        return Optional.of(meta);
    }

    /**
     * Will read the XMPMetadata from the given pdf file, closing the file
     * afterwards.
     *
     * @param file
     *            The file to read the XMPMetadata from.
     * @return The XMPMetadata object found in the file
     */
    public static Optional<XMPMetadata> readRawXMP(File file) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            return XMPUtil.readRawXMP(inputStream);
        }
    }

    private static void writeToDCSchema(XMPSchemaDublinCore dcSchema,
            BibEntry entry, BibDatabase database) {

        BibEntry resolvedEntry;
        if (database == null) {
            resolvedEntry = entry;
        } else {
            resolvedEntry = database.resolveForStrings(entry, false);
        }

        // Query privacy filter settings
        JabRefPreferences prefs = JabRefPreferences.getInstance();
        boolean useXmpPrivacyFilter =
                prefs.getBoolean(JabRefPreferences.USE_XMP_PRIVACY_FILTER);
        // Fields for which not to write XMP data later on:
        Set<String> filters = new TreeSet<>(prefs.getStringList(JabRefPreferences.XMP_PRIVACY_FILTERS));

        // Set all the values including key and entryType

        for (String field : resolvedEntry.getFieldNames()) {

            if (useXmpPrivacyFilter && filters.contains(field)) {
                continue;
            }

            if ("editor".equals(field)) {
                String authors = resolvedEntry.getField(field);

                /**
                 * Editor -> Contributor
                 *
                 * Field: dc:contributor
                 *
                 * Type: bag ProperName
                 *
                 * Category: External
                 *
                 * Description: Contributors to the resource (other than the
                 * authors).
                 *
                 * Bibtex-Fields used: editor
                 */

                AuthorList list = AuthorList.parse(authors);

                for (Author author : list.getAuthors()) {
                    dcSchema.addContributor(author.getFirstLast(false));
                }
                continue;
            }

            /**
             * ? -> Coverage
             *
             * Unmapped
             *
             * dc:coverage Text External The extent or scope of the resource.
             */

            /**
             * Author -> Creator
             *
             * Field: dc:creator
             *
             * Type: seq ProperName
             *
             * Category: External
             *
             * Description: The authors of the resource (listed in order of
             * precedence, if significant).
             *
             * Bibtex-Fields used: author
             */
            if ("author".equals(field)) {
                String authors = resolvedEntry.getField(field);
                AuthorList list = AuthorList.parse(authors);

                for (Author author : list.getAuthors()) {
                    dcSchema.addCreator(author.getFirstLast(false));
                }
                continue;
            }

            if ("month".equals(field)) {
                // Dealt with in year
                continue;
            }

            if ("year".equals(field)) {

                /**
                 * Year + Month -> Date
                 *
                 * Field: dc:date
                 *
                 * Type: seq Date
                 *
                 * Category: External
                 *
                 * Description: Date(s) that something interesting happened to
                 * the resource.
                 *
                 * Bibtex-Fields used: year, month
                 */
                String publicationDate = entry.getPublicationDate();
                if (publicationDate != null) {
                    dcSchema.addSequenceValue("dc:date", publicationDate);
                }
                continue;
            }
            /**
             * Abstract -> Description
             *
             * Field: dc:description
             *
             * Type: Lang Alt
             *
             * Category: External
             *
             * Description: A textual description of the content of the
             * resource. Multiple values may be present for different languages.
             *
             * Bibtex-Fields used: abstract
             */
            if ("abstract".equals(field)) {
                String o = resolvedEntry.getField(field);
                dcSchema.setDescription(o);
                continue;
            }

            /**
             * DOI -> identifier
             *
             * Field: dc:identifier
             *
             * Type: Text
             *
             * Category: External
             *
             * Description: Unique identifier of the resource.
             *
             * Bibtex-Fields used: doi
             */
            if ("doi".equals(field)) {
                String o = resolvedEntry.getField(field);
                dcSchema.setIdentifier(o);
                continue;
            }

            /**
             * ? -> Language
             *
             * Unmapped
             *
             * dc:language bag Locale Internal An unordered array specifying the
             * languages used in the resource.
             */

            /**
             * Publisher -> Publisher
             *
             * Field: dc:publisher
             *
             * Type: bag ProperName
             *
             * Category: External
             *
             * Description: Publishers.
             *
             * Bibtex-Fields used: doi
             */
            if ("publisher".equals(field)) {
                String o = entry.getField(field);
                dcSchema.addPublisher(o);
                continue;
            }

            /**
             * ? -> Rights
             *
             * Unmapped
             *
             * dc:rights Lang Alt External Informal rights statement, selected
             * by language.
             */

            /**
             * ? -> Source
             *
             * Unmapped
             *
             * dc:source Text External Unique identifier of the work from which
             * this resource was derived.
             */

            /**
             * Keywords -> Subject
             *
             * Field: dc:subject
             *
             * Type: bag Text
             *
             * Category: External
             *
             * Description: An unordered array of descriptive phrases or
             * keywords that specify the topic of the content of the resource.
             *
             * Bibtex-Fields used: doi
             */
            if ("keywords".equals(field)) {
                String o = entry.getField(field);
                String[] keywords = o.split(",");
                for (String keyword : keywords) {
                    dcSchema.addSubject(keyword.trim());
                }
                continue;
            }

            /**
             * Title -> Title
             *
             * Field: dc:title
             *
             * Type: Lang Alt
             *
             * Category: External
             *
             * Description: The title of the document, or the name given to the
             * resource. Typically, it will be a name by which the resource is
             * formally known.
             *
             * Bibtex-Fields used: title
             */
            if ("title".equals(field)) {
                String o = entry.getField(field);
                dcSchema.setTitle(o);
                continue;
            }

            /**
             * bibtextype -> relation
             *
             * Field: dc:relation
             *
             * Type: bag Text
             *
             * Category: External
             *
             * Description: Relationships to other documents.
             *
             * Bibtex-Fields used: bibtextype
             */
            /**
             * All others (including the bibtex key) get packaged in the
             * relation attribute
             */
            String o = entry.getField(field);
            dcSchema.addRelation("bibtex/" + field + '/' + o);
        }

        /**
         * ? -> Format
         *
         * Unmapped
         *
         * dc:format MIMEType Internal The file format used when saving the
         * resource. Tools and applications should set this property to the save
         * format of the data. It may include appropriate qualifiers.
         */
        dcSchema.setFormat("application/pdf");

        /**
         * Type -> Type
         *
         * Field: dc:type
         *
         * Type: bag open Choice
         *
         * Category: External
         *
         * Description: A document type; for example, novel, poem, or working
         * paper.
         *
         * Bibtex-Fields used: title
         */
        TypedBibEntry typedEntry = new TypedBibEntry(entry, Optional.empty(), BibDatabaseMode.BIBTEX);
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
     * @param document
     *            The pdf document to write to.
     * @param entry
     *            The BibTeX entry that is written as a schema.
     * @param database
     *            maybenull An optional database which the given BibTeX entries
     *            belong to, which will be used to resolve strings. If the
     *            database is null the strings will not be resolved.
     * @throws IOException
     * @throws TransformerException
     */
    public static void writeDublinCore(PDDocument document, BibEntry entry,
            BibDatabase database) throws IOException, TransformerException {

        List<BibEntry> entries = new ArrayList<>();
        entries.add(entry);

        XMPUtil.writeDublinCore(document, entries, database);
    }

    /**
     * Try to write the given BibTexEntries as DublinCore XMP Schemas
     *
     * Existing DublinCore schemas in the document are removed
     *
     * @param document
     *            The pdf document to write to.
     * @param entries
     *            The BibTeX entries that are written as schemas
     * @param database
     *            maybenull An optional database which the given BibTeX entries
     *            belong to, which will be used to resolve strings. If the
     *            database is null the strings will not be resolved.
     * @throws IOException
     * @throws TransformerException
     */
    private static void writeDublinCore(PDDocument document,
            Collection<BibEntry> entries, BibDatabase database)
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
            meta = new XMPMetadata();
        } else {
            meta = new XMPMetadata(XMLUtil.parse(metaRaw.createInputStream()));
        }

        // Remove all current Dublin-Core schemas
        List<XMPSchema> schemas = meta
                .getSchemasByNamespaceURI(XMPSchemaDublinCore.NAMESPACE);
        for (XMPSchema schema : schemas) {
            schema.getElement().getParentNode().removeChild(schema.getElement());
        }

        for (BibEntry entry : resolvedEntries) {
            XMPSchemaDublinCore dcSchema = new XMPSchemaDublinCore(meta);
            XMPUtil.writeToDCSchema(dcSchema, entry, null);
            meta.addSchema(dcSchema);
        }

        // Save to stream and then input that stream to the PDF
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        meta.save(os);
        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        PDMetadata metadataStream = new PDMetadata(document, is, false);
        catalog.setMetadata(metadataStream);
    }

    /**
     * Try to write the given BibTexEntry in the Document Information (the
     * properties of the pdf).
     *
     * Existing fields values are overriden if the bibtex entry has the
     * corresponding value set.
     *
     * @param document
     *            The pdf document to write to.
     * @param entry
     *            The Bibtex entry that is written into the PDF properties. *
     * @param database
     *            maybenull An optional database which the given bibtex entries
     *            belong to, which will be used to resolve strings. If the
     *            database is null the strings will not be resolved.
     */
    private static void writeDocumentInformation(PDDocument document,
            BibEntry entry, BibDatabase database) {

        PDDocumentInformation di = document.getDocumentInformation();

        BibEntry resolvedEntry;
        if (database == null) {
            resolvedEntry = entry;
        } else {
            resolvedEntry = database.resolveForStrings(entry, false);
        }

        // Query privacy filter settings
        JabRefPreferences prefs = JabRefPreferences.getInstance();
        boolean useXmpPrivacyFilter =
                prefs.getBoolean(JabRefPreferences.USE_XMP_PRIVACY_FILTER);
        // Fields for which not to write XMP data later on:
        Set<String> filters = new TreeSet<>(prefs.getStringList(JabRefPreferences.XMP_PRIVACY_FILTERS));

        // Set all the values including key and entryType
        Set<String> fields = resolvedEntry.getFieldNames();

        for (String field : fields) {

            if (useXmpPrivacyFilter && filters.contains(field)) {
                // erase field instead of adding it
                if ("author".equals(field)) {
                    di.setAuthor(null);
                } else if ("title".equals(field)) {
                    di.setTitle(null);
                } else if ("keywords".equals(field)) {
                    di.setKeywords(null);
                } else if ("abstract".equals(field)) {
                    di.setSubject(null);
                } else {
                    di.setCustomMetadataValue("bibtex/" + field, null);
                }
                continue;
            }

            if ("author".equals(field)) {
                di.setAuthor(resolvedEntry.getField("author"));
            } else if ("title".equals(field)) {
                di.setTitle(resolvedEntry.getField("title"));
            } else if ("keywords".equals(field)) {
                di.setKeywords(resolvedEntry.getField("keywords"));
            } else if ("abstract".equals(field)) {
                di.setSubject(resolvedEntry.getField("abstract"));
            } else {
                di.setCustomMetadataValue("bibtex/" + field, resolvedEntry.getField(field));
            }
        }
        di.setCustomMetadataValue("bibtex/entrytype", EntryUtil.capitalizeFirst(resolvedEntry.getType()));
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
     * @param file
     *            The file to write the entries to.
     * @param bibtexEntries
     *            The entries to write to the file. *
     * @param database
     *            maybenull An optional database which the given bibtex entries
     *            belong to, which will be used to resolve strings. If the
     *            database is null the strings will not be resolved.
     * @param writePDFInfo
     *            Write information also in PDF document properties
     * @throws TransformerException
     *             If the entry was malformed or unsupported.
     * @throws IOException
     *             If the file could not be written to or could not be found.
     */
    public static void writeXMP(File file,
            Collection<BibEntry> bibtexEntries, BibDatabase database,
            boolean writePDFInfo) throws IOException, TransformerException {

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
                        .iterator().next(), null);
                XMPUtil.writeDublinCore(document, resolvedEntries, null);
            }

            PDDocumentCatalog catalog = document.getDocumentCatalog();
            PDMetadata metaRaw = catalog.getMetadata();

            XMPMetadata meta;
            if (metaRaw == null) {
                meta = new XMPMetadata();
            } else {
                meta = new XMPMetadata(XMLUtil.parse(metaRaw.createInputStream()));
            }
            meta.addXMLNSMapping(XMPSchemaBibtex.NAMESPACE,
                    XMPSchemaBibtex.class);

            // Remove all current Bibtex-schemas
            List<XMPSchema> schemas = meta
                    .getSchemasByNamespaceURI(XMPSchemaBibtex.NAMESPACE);
            for (XMPSchema schema : schemas) {
                XMPSchemaBibtex bib = (XMPSchemaBibtex) schema;
                bib.getElement().getParentNode().removeChild(bib.getElement());
            }

            for (BibEntry e : resolvedEntries) {
                XMPSchemaBibtex bibtex = new XMPSchemaBibtex(meta);
                meta.addSchema(bibtex);
                bibtex.setBibtexEntry(e, null);
            }

            // Save to stream and then input that stream to the PDF
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            meta.save(os);
            ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
            PDMetadata metadataStream = new PDMetadata(document, is, false);
            catalog.setMetadata(metadataStream);

            // Save
            try {
                document.save(file.getAbsolutePath());
            } catch (COSVisitorException e) {
                LOGGER.debug("Could not write XMP metadata", e);
                throw new TransformerException("Could not write XMP metadata: " + e.getLocalizedMessage(), e);
            }
        }
    }

    /**
     * Print usage information for the command line tool xmpUtil.
     *
     * @see XMPUtil#main(String[])
     */
    private static void usage() {
        System.out.println("Read or write XMP-metadata from or to pdf file.");
        System.out.println("");
        System.out.println("Usage:");
        System.out.println("Read from PDF and print as bibtex:");
        System.out.println("  xmpUtil <pdf>");
        System.out.println("Read from PDF and print raw XMP:");
        System.out.println("  xmpUtil -x <pdf>");
        System.out
        .println("Write the entry in <bib> given by <key> to the PDF:");
        System.out.println("  xmpUtil <key> <bib> <pdf>");
        System.out.println("Write all entries in <bib> to the PDF:");
        System.out.println("  xmpUtil <bib> <pdf>");
        System.out.println("");
        System.out
        .println("To report bugs visit http://jabref.sourceforge.net");
    }

    /**
     * Command-line tool for working with XMP-data.
     *
     * Read or write XMP-metadata from or to pdf file.
     *
     * Usage:
     * <dl>
     * <dd>Read from PDF and print as bibtex:</dd>
     * <dt>xmpUtil PDF</dt>
     * <dd>Read from PDF and print raw XMP:</dd>
     * <dt>xmpUtil -x PDF</dt>
     * <dd>Write the entry in BIB given by KEY to the PDF:</dd>
     * <dt>xmpUtil KEY BIB PDF</dt>
     * <dd>Write all entries in BIB to the PDF:</dd>
     * <dt>xmpUtil BIB PDF</dt>
     * </dl>
     *
     * @param args
     *            Command line strings passed to utility.
     * @throws IOException
     *             If any of the given files could not be read or written.
     * @throws TransformerException
     *             If the given BibEntry is malformed.
     */
    public static void main(String[] args) throws IOException,
    TransformerException {

        // Don't forget to initialize the preferences
        if (Globals.prefs == null) {
            Globals.prefs = JabRefPreferences.getInstance();
        }

        switch (args.length) {
        case 0:
            XMPUtil.usage();
            break;
        case 1:

            if (args[0].endsWith(".pdf")) {
                // Read from pdf and write as BibTex
                List<BibEntry> l = XMPUtil.readXMP(new File(args[0]));

                BibEntryWriter bibtexEntryWriter = new BibEntryWriter(new LatexFieldFormatter(), false);

                for (BibEntry entry : l) {
                    StringWriter sw = new StringWriter();
                    bibtexEntryWriter.write(entry, sw, BibDatabaseMode.BIBTEX);
                    System.out.println(sw.getBuffer());
                }

            } else if (args[0].endsWith(".bib")) {
                // Read from bib and write as XMP
                try (FileReader fr = new FileReader(args[0])) {
                    ParserResult result = BibtexParser.parse(fr);
                    Collection<BibEntry> entries = result.getDatabase().getEntries();

                    if (entries.isEmpty()) {
                        System.err.println("Could not find BibEntry in " + args[0]);
                    } else {
                        System.out.println(XMPUtil.toXMP(entries, result.getDatabase()));
                    }
                }
            } else {
                XMPUtil.usage();
            }
            break;
        case 2:
            if ("-x".equals(args[0]) && args[1].endsWith(".pdf")) {
                // Read from pdf and write as BibTex
                Optional<XMPMetadata> meta = XMPUtil.readRawXMP(new File(args[1]));

                if (meta.isPresent()) {
                    XMLUtil.save(meta.get().getXMPDocument(), System.out, StandardCharsets.UTF_8.name());
                } else {
                    System.err
                    .println("The given pdf does not contain any XMP-metadata.");
                }
                break;
            }

            if (args[0].endsWith(".bib") && args[1].endsWith(".pdf")) {
                ParserResult result = BibtexParser
                        .parse(new FileReader(args[0]));

                Collection<BibEntry> entries = result.getDatabase()
                        .getEntries();

                if (entries.isEmpty()) {
                    System.err.println("Could not find BibEntry in "
                            + args[0]);
                } else {
                    XMPUtil.writeXMP(new File(args[1]), entries, result
                            .getDatabase(), false);
                    System.out.println("XMP written.");
                }
                break;
            }

            XMPUtil.usage();
            break;
        case 3:
            if (!args[1].endsWith(".bib") && !args[2].endsWith(".pdf")) {
                XMPUtil.usage();
                break;
            }

            ParserResult result = BibtexParser.parse(new FileReader(args[1]));

            BibEntry bibEntry = result.getDatabase().getEntryByKey(args[0]);

            if (bibEntry == null) {
                System.err.println("Could not find BibEntry " + args[0]
                        + " in " + args[0]);
            } else {
                XMPUtil.writeXMP(new File(args[2]), bibEntry, result.getDatabase());

                System.out.println("XMP written.");
            }
            break;

        default:
            XMPUtil.usage();
        }
    }

    /**
     * see XMPUtil.hasMetadata(InputStream)
     */
    public static boolean hasMetadata(Path path) {
        try (InputStream inputStream = Files.newInputStream(path, StandardOpenOption.READ)) {
            return hasMetadata(inputStream);
        } catch (IOException e) {
            LOGGER.error("XMP reading failed", e);
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
     * @param inputStream
     *            The inputStream to read the PDF from.
     * @return whether a BibEntry was found in the given PDF.
     */
    public static boolean hasMetadata(InputStream inputStream) {
        try {
            List<BibEntry> bibEntries = XMPUtil.readXMP(inputStream);
            return !bibEntries.isEmpty();
        } catch (EncryptedPdfsNotSupportedException ex) {
            LOGGER.info("Encryption not supported by XMPUtil");
            return false;
        } catch (IOException e) {
            LOGGER.error("XMP reading failed", e);
            return false;
        }
    }
}
