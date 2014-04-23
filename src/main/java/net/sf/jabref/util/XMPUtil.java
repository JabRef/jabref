/*  Copyright (C) 2003-2011 JabRef contributors.
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
package net.sf.jabref.util;

import java.io.*;
import java.util.*;

import javax.xml.transform.TransformerException;

import net.sf.jabref.*;
import net.sf.jabref.export.LatexFieldFormatter;
import net.sf.jabref.imports.BibtexParser;
import net.sf.jabref.imports.ParserResult;

import org.apache.jempbox.impl.DateConverter;
import org.apache.jempbox.impl.XMLUtil;
import org.apache.jempbox.xmp.XMPMetadata;
import org.apache.jempbox.xmp.XMPSchema;
import org.apache.jempbox.xmp.XMPSchemaDublinCore;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDMetadata;

/**
 * XMPUtils provide support for reading and writing BibTex data as XMP-Metadata
 * in PDF-documents.
 * 
 * @author Christopher Oezbek <oezi@oezi.de>
 * 
 * TODO:
 * 
 * Synchronization
 * 
 * @version $Revision$ ($Date$)
 */
public class XMPUtil {

	/**
	 * Convenience method for readXMP(File).
	 * 
	 * @param filename
	 *            The filename from which to open the file.
	 * @return BibtexEntryies found in the PDF or an empty list
	 * @throws IOException
	 */
	public static List<BibtexEntry> readXMP(String filename) throws IOException {
		return readXMP(new File(filename));
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
	 * This is a convenience method for writeXMP(File, BibtexEntry).
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
	public static void writeXMP(String filename, BibtexEntry entry,
			BibtexDatabase database) throws IOException, TransformerException {
		writeXMP(new File(filename), entry, database);
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
	public static List<BibtexEntry> readXMP(File file) throws IOException {
		FileInputStream is = new FileInputStream(file);
		try {
			return readXMP(is);
		} finally {
			is.close();
		}
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
	 */
	@SuppressWarnings("unchecked")
	public static List<BibtexEntry> readXMP(InputStream inputStream)
			throws IOException {

		List<BibtexEntry> result = new LinkedList<BibtexEntry>();

		PDDocument document = null;

		try {
			document = PDDocument.load(inputStream);
			if (document.isEncrypted()) {
				throw new EncryptionNotSupportedException(
						"Error: Cannot read metadata from encrypted document.");
			}

			XMPMetadata meta = getXMPMetadata(document);

			// If we did not find any XMP metadata, search for non XMP metadata
			if (meta != null) {

        			List<XMPSchema> schemas = meta
        					.getSchemasByNamespaceURI(XMPSchemaBibtex.NAMESPACE);

                for (XMPSchema schema : schemas) {
                    XMPSchemaBibtex bib = (XMPSchemaBibtex) schema;

                    result.add(bib.getBibtexEntry());
                }
        
        			// If we did not find anything have a look if a Dublin Core exists
        			if (result.size() == 0) {
        				schemas = meta
        						.getSchemasByNamespaceURI(XMPSchemaDublinCore.NAMESPACE);
                        for (XMPSchema schema : schemas) {
                            XMPSchemaDublinCore dc = (XMPSchemaDublinCore) schema;

                            BibtexEntry entry = getBibtexEntryFromDublinCore(dc);

                            if (entry != null)
                                result.add(entry);
                        }
        			}
			}
			if (result.size() == 0) {
				BibtexEntry entry = getBibtexEntryFromDocumentInformation(document
						.getDocumentInformation());

				if (entry != null)
					result.add(entry);
			}
		} finally {
			if (document != null)
				document.close();
		}
		
		// return null, if no metadata was found
		if (result.size()==0) return null;
		return result;
	}

	/**
	 * Helper function for retrieving a BibtexEntry from the
	 * PDDocumentInformation in a PDF file.
	 * 
	 * To understand how to get hold of a PDDocumentInformation have a look in
	 * the test cases for XMPUtil.
	 * 
	 * The BibtexEntry is build by mapping individual fields in the document
	 * information (like author, title, keywords) to fields in a bibtex entry.
	 * 
	 * @param di
	 *            The document information from which to build a BibtexEntry.
	 * 
	 * @return The bibtex entry found in the document information.
	 */
	@SuppressWarnings("unchecked")
	public static BibtexEntry getBibtexEntryFromDocumentInformation(
			PDDocumentInformation di) {

		BibtexEntry entry = new BibtexEntry();

		String s = di.getAuthor();
		if (s != null)
			entry.setField("author", s);

		s = di.getTitle();
		if (s != null)
			entry.setField("title", s);

		s = di.getKeywords();
		if (s != null)
			entry.setField("keywords", s);

		s = di.getSubject();
		if (s != null)
			entry.setField("abstract", s);

		COSDictionary dict = di.getDictionary();
        for (Map.Entry<COSName,COSBase> o : dict.entrySet()) {
            String key = o.getKey().getName();
            if (key.startsWith("bibtex/")) {
                String value = dict.getString(key);
                key = key.substring("bibtex/".length());
                if (key.equals("entrytype")) {
                    BibtexEntryType type = BibtexEntryType
                            .getStandardType(value);
                    if (type != null)
                        entry.setType(type);
                } else
                    entry.setField(key, value);
            }
        }

		// Return null if no values were found
		return (entry.getAllFields().size() > 0 ? entry : null);
	}

	/**
	 * Helper function for retrieving a BibtexEntry from the DublinCore metadata
	 * in a PDF file.
	 * 
	 * To understand how to get hold of a XMPSchemaDublinCore have a look in the
	 * test cases for XMPUtil.
	 * 
	 * The BibtexEntry is build by mapping individual fields in the dublin core
	 * (like creator, title, subject) to fields in a bibtex entry.
	 * 
	 * @param dcSchema
	 *            The document information from which to build a BibtexEntry.
	 * 
	 * @return The bibtex entry found in the document information.
	 */
	@SuppressWarnings("unchecked")
	public static BibtexEntry getBibtexEntryFromDublinCore(
			XMPSchemaDublinCore dcSchema) {

		BibtexEntry entry = new BibtexEntry();

		/**
		 * Contributor -> Editor
		 */
		List<String> contributors = dcSchema.getContributors();
		if (contributors != null) {
			Iterator<String> it = contributors.iterator();
			StringBuffer sb = null;
			while (it.hasNext()) {
				if (sb != null) {
					sb.append(" and ");
				} else {
					sb = new StringBuffer();
				}
				sb.append(it.next());
			}
			if (sb != null)
				entry.setField("editor", sb.toString());
		}

		/**
		 * Author -> Creator
		 */
		List<String> creators = dcSchema.getCreators();
		if (creators != null) {
			Iterator<String> it = creators.iterator();
			StringBuffer sb = null;
			while (it.hasNext()) {
				if (sb != null) {
					sb.append(" and ");
				} else {
					sb = new StringBuffer();
				}
				sb.append(it.next());
			}
			if (sb != null)
				entry.setField("author", sb.toString());
		}

		/**
		 * Year + Month -> Date
		 */
		List<String> dates = dcSchema.getSequenceList("dc:date");
		if (dates != null && dates.size() > 0) {
			String date = dates.get(0).trim();
			Calendar c = null;
			try {
				c = DateConverter.toCalendar(date);
			} catch (Exception ignored) {

			}
			if (c != null) {
				entry.setField("year", String.valueOf(c.get(Calendar.YEAR)));
				if (date.length() > 4) {
					entry.setField("month", "#"
							+ Globals.MONTHS[c.get(Calendar.MONTH)] + "#");
				}
			}
		}

		/**
		 * Abstract -> Description
		 */
		String s = dcSchema.getDescription();
		if (s != null)
			entry.setField("abstract", s);

		/**
		 * Identifier -> DOI
		 */
		s = dcSchema.getIdentifier();
		if (s != null)
			entry.setField("doi", s);

		/**
		 * Publisher -> Publisher
		 */
		List<String> publishers = dcSchema.getPublishers();
		if (publishers != null) {
			Iterator<String> it = dcSchema.getPublishers().iterator();
			StringBuffer sb = null;
			while (it.hasNext()) {
				if (sb != null) {
					sb.append(" and ");
				} else {
					sb = new StringBuffer();
				}
				sb.append(it.next());
			}
			if (sb != null)
				entry.setField("publishers", sb.toString());
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
		if (s != null)
			entry.setField("rights", s);

		/**
		 * Source -> Source
		 */
		s = dcSchema.getSource();
		if (s != null)
			entry.setField("source", s);

		/**
		 * Subject -> Keywords
		 */
		List<String> subjects = dcSchema.getSubjects();
		if (subjects != null) {
			Iterator<String> it = subjects.iterator();
			StringBuffer sb = null;
			while (it.hasNext()) {
				if (sb != null) {
					sb.append(",");
				} else {
					sb = new StringBuffer();
				}
				sb.append(it.next());
			}
			if (sb != null)
				entry.setField("keywords", sb.toString());
		}

		/**
		 * Title -> Title
		 */
		s = dcSchema.getTitle();
		if (s != null)
			entry.setField("title", s);

		/**
		 * Type -> Type
		 */
		List<String> l = dcSchema.getTypes();
		if (l != null && l.size() > 0) {
			s = l.get(0);
			if (s != null) {
				BibtexEntryType type = BibtexEntryType.getStandardType(s);
				if (type != null)
					entry.setType(type);
			}
		}

		return (entry.getAllFields().size() > 0 ? entry : null);
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
	public static void writeXMP(File file, BibtexEntry entry,
			BibtexDatabase database) throws IOException, TransformerException {
		List<BibtexEntry> l = new LinkedList<BibtexEntry>();
		l.add(entry);
		writeXMP(file, l, database, true);
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
	 * @see #toXMP(java.util.Collection, net.sf.jabref.BibtexDatabase) if you don't need strings to be
	 *      resolved.
	 */
	public static void toXMP(Collection<BibtexEntry> bibtexEntries,
			BibtexDatabase database, OutputStream outputStream)
			throws IOException, TransformerException {

		if (database != null)
			bibtexEntries = database.resolveForStrings(bibtexEntries, true);

		XMPMetadata x = new XMPMetadata();

        for (BibtexEntry e : bibtexEntries) {
            XMPSchemaBibtex schema = new XMPSchemaBibtex(x);
            x.addSchema(schema);
            schema.setBibtexEntry(e);
        }

		x.save(outputStream);
	}

	/**
	 * Convenience method for toXMP(Collection<BibtexEntry>, BibtexDatabase,
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
	public static String toXMP(Collection<BibtexEntry> bibtexEntries,
			BibtexDatabase database) throws TransformerException {
		try {
			ByteArrayOutputStream bs = new ByteArrayOutputStream();
			toXMP(bibtexEntries, database, bs);
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
	 * @return The XMPMetadata object found in the file or null if none is
	 *         found.
	 * @throws IOException
	 */
	public static XMPMetadata readRawXMP(InputStream inputStream)
			throws IOException {
		PDDocument document = null;

		try {
			document = PDDocument.load(inputStream);
			if (document.isEncrypted()) {
				throw new EncryptionNotSupportedException(
						"Error: Cannot read metadata from encrypted document.");
			}

			return getXMPMetadata(document);

		} finally {
			if (document != null)
				document.close();
		}
	}

	static XMPMetadata getXMPMetadata(PDDocument document) throws IOException {
		PDDocumentCatalog catalog = document.getDocumentCatalog();
		PDMetadata metaRaw = catalog.getMetadata();

		if (metaRaw == null) {
			return null;
		}

		XMPMetadata meta = new XMPMetadata(XMLUtil.parse(metaRaw
				.createInputStream()));
		meta.addXMLNSMapping(XMPSchemaBibtex.NAMESPACE, XMPSchemaBibtex.class);
		return meta;
	}

	/**
	 * Will read the XMPMetadata from the given pdf file, closing the file
	 * afterwards.
	 * 
	 * @param file
	 *            The file to read the XMPMetadata from.
	 * @return The XMPMetadata object found in the file or null if none is
	 *         found.
	 * @throws IOException
	 */
	public static XMPMetadata readRawXMP(File file) throws IOException {
		FileInputStream is = new FileInputStream(file);
		try {
			return readRawXMP(is);
		} finally {
			is.close();
		}
	}

	static void writeToDCSchema(XMPSchemaDublinCore dcSchema,
			BibtexEntry entry, BibtexDatabase database) {

		if (database != null)
			entry = database.resolveForStrings(entry, false);

		// Query privacy filter settings
		JabRefPreferences prefs = JabRefPreferences.getInstance();
		boolean useXmpPrivacyFilter =
			prefs.getBoolean("useXmpPrivacyFilter");
		// Fields for which not to write XMP data later on:
		TreeSet<String> filters = new TreeSet<String>(Arrays.asList(prefs.getStringArray(JabRefPreferences.XMP_PRIVACY_FILTERS)));

		// Set all the values including key and entryType
		
		for (String field : entry.getAllFields()){

			if (useXmpPrivacyFilter && filters.contains(field)) {
				continue;
			}

			if (field.equals("editor")) {
				String authors = entry.getField(field);

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

				AuthorList list = AuthorList.getAuthorList(authors);

				int n = list.size();
				for (int i = 0; i < n; i++) {
					dcSchema.addContributor(list.getAuthor(i).getFirstLast(
							false));
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
			if (field.equals("author")) {
				String authors = entry.getField(field);
				AuthorList list = AuthorList.getAuthorList(authors);

				int n = list.size();
				for (int i = 0; i < n; i++) {
					dcSchema.addCreator(list.getAuthor(i).getFirstLast(false));
				}
				continue;
			}

			if (field.equals("month")) {
				// Dealt with in year
				continue;
			}

			if (field.equals("year")) {

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
				String publicationDate = Util.getPublicationDate(entry);
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
			if (field.equals("abstract")) {
				String o = entry.getField(field);
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
			if (field.equals("doi")) {
				String o = entry.getField(field);
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
			if (field.equals("publisher")) {
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
			if (field.equals("keywords")) {
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
			if (field.equals("title")) {
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
			dcSchema.addRelation("bibtex/" + field + "/" + o);
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
		Object o = entry.getType().getName();
		if (o != null)
			dcSchema.addType(o.toString());
	}

	/**
	 * Try to write the given BibTexEntry as a DublinCore XMP Schema
	 * 
	 * Existing DublinCore schemas in the document are not modified.
	 * 
	 * @param document
	 *            The pdf document to write to.
	 * @param entry
	 *            The Bibtex entry that is written as a schema.
	 * @param database
	 *            maybenull An optional database which the given bibtex entries
	 *            belong to, which will be used to resolve strings. If the
	 *            database is null the strings will not be resolved.
	 * @throws IOException
	 * @throws TransformerException
	 */
	public static void writeDublinCore(PDDocument document, BibtexEntry entry,
			BibtexDatabase database) throws IOException, TransformerException {

		List<BibtexEntry> entries = new ArrayList<BibtexEntry>();
		entries.add(entry);

		writeDublinCore(document, entries, database);
	}

	/**
	 * Try to write the given BibTexEntries as DublinCore XMP Schemas
	 * 
	 * Existing DublinCore schemas in the document are removed
	 * 
	 * @param document
	 *            The pdf document to write to.
	 * @param entries
	 *            The Bibtex entries that are written as schemas
	 * @param database
	 *            maybenull An optional database which the given bibtex entries
	 *            belong to, which will be used to resolve strings. If the
	 *            database is null the strings will not be resolved.
	 * @throws IOException
	 * @throws TransformerException
	 */
	@SuppressWarnings("unchecked")
	public static void writeDublinCore(PDDocument document,
			Collection<BibtexEntry> entries, BibtexDatabase database)
			throws IOException, TransformerException {

		if (database != null)
			entries = database.resolveForStrings(entries, false);

		PDDocumentCatalog catalog = document.getDocumentCatalog();
		PDMetadata metaRaw = catalog.getMetadata();

		XMPMetadata meta;
		if (metaRaw != null) {
			meta = new XMPMetadata(XMLUtil.parse(metaRaw.createInputStream()));
		} else {
			meta = new XMPMetadata();
		}

		// Remove all current Dublin-Core schemas
		List<XMPSchema> schemas = meta
				.getSchemasByNamespaceURI(XMPSchemaDublinCore.NAMESPACE);
        for (XMPSchema schema : schemas) {
            schema.getElement().getParentNode().removeChild(schema.getElement());
        }

		for (BibtexEntry entry : entries) {
			XMPSchemaDublinCore dcSchema = new XMPSchemaDublinCore(meta);
			writeToDCSchema(dcSchema, entry, null);
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
	public static void writeDocumentInformation(PDDocument document,
			BibtexEntry entry, BibtexDatabase database) {

		PDDocumentInformation di = document.getDocumentInformation();

		if (database != null)
			entry = database.resolveForStrings(entry, false);

		// Query privacy filter settings
		JabRefPreferences prefs = JabRefPreferences.getInstance();
		boolean useXmpPrivacyFilter =
			prefs.getBoolean("useXmpPrivacyFilter");
		// Fields for which not to write XMP data later on:
		TreeSet<String> filters = new TreeSet<String>(Arrays.asList(prefs.getStringArray(JabRefPreferences.XMP_PRIVACY_FILTERS)));

		// Set all the values including key and entryType
		Set<String> fields = entry.getAllFields();

		for (String field : fields){

			if (useXmpPrivacyFilter && filters.contains(field)) {
                            // erase field instead of adding it
                            if (field.equals("author")) {
				di.setAuthor(null);
                            } else if (field.equals("title")) {
				di.setTitle(null);
                            } else if (field.equals("keywords")) {
				di.setKeywords(null);
                            } else if (field.equals("abstract")) {
				di.setSubject(null);
                            } else {
				di.setCustomMetadataValue("bibtex/" + field,
                                                          null);
                            }
                            continue;
			}

			if (field.equals("author")) {
				di.setAuthor(entry.getField("author"));
			} else if (field.equals("title")) {
				di.setTitle(entry.getField("title"));
			} else if (field.equals("keywords")) {
				di.setKeywords(entry.getField("keywords"));
			} else if (field.equals("abstract")) {
				di.setSubject(entry.getField("abstract"));
			} else {
				di.setCustomMetadataValue("bibtex/" + field,
                        entry.getField(field));
			}
		}
		di
				.setCustomMetadataValue("bibtex/entrytype", entry.getType()
						.getName());
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
	@SuppressWarnings("unchecked")
	public static void writeXMP(File file,
			Collection<BibtexEntry> bibtexEntries, BibtexDatabase database,
			boolean writePDFInfo) throws IOException, TransformerException {

		if (database != null)
			bibtexEntries = database.resolveForStrings(bibtexEntries, false);

		PDDocument document = null;

		try {
			document = PDDocument.load(file.getAbsoluteFile());
			if (document.isEncrypted()) {
				throw new EncryptionNotSupportedException(
						"Error: Cannot add metadata to encrypted document.");
			}

			if (writePDFInfo && bibtexEntries.size() == 1) {
				writeDocumentInformation(document, bibtexEntries
						.iterator().next(), null);
				writeDublinCore(document, bibtexEntries, null);
			}

			PDDocumentCatalog catalog = document.getDocumentCatalog();
			PDMetadata metaRaw = catalog.getMetadata();

			XMPMetadata meta;
			if (metaRaw != null) {
				meta = new XMPMetadata(XMLUtil.parse(metaRaw
						.createInputStream()));
			} else {
				meta = new XMPMetadata();
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

            for (BibtexEntry e : bibtexEntries) {
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
				throw new TransformerException("Could not write XMP-metadata: "
						+ e.getLocalizedMessage());
			}

		} finally {
			if (document != null) {
				document.close();
			}
		}
	}

	/**
	 * Print usage information for the command line tool xmpUtil.
	 * 
	 * @see XMPUtil#main(String[])
	 */
	protected static void usage() {
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
	 *             If the given BibtexEntry is malformed.
	 */
	public static void main(String[] args) throws IOException,
			TransformerException {

		// Don't forget to initialize the preferences
		if (Globals.prefs == null) {
			Globals.prefs = JabRefPreferences.getInstance();
		}

		switch (args.length) {
		case 0:
			usage();
			break;
		case 1: {

			if (args[0].endsWith(".pdf")) {
				// Read from pdf and write as BibTex
				List<BibtexEntry> l = XMPUtil.readXMP(new File(args[0]));

                for (BibtexEntry e : l) {
                    StringWriter sw = new StringWriter();
                    e.write(sw, new LatexFieldFormatter(),
                            false);
                    System.out.println(sw.getBuffer().toString());
                }

			} else if (args[0].endsWith(".bib")) {
				// Read from bib and write as XMP

				ParserResult result = BibtexParser
						.parse(new FileReader(args[0]));
				Collection<BibtexEntry> entries = result.getDatabase()
						.getEntries();

				if (entries.size() == 0) {
					System.err.println("Could not find BibtexEntry in "
							+ args[0]);
				} else {
					System.out.println(XMPUtil.toXMP(entries, result
							.getDatabase()));
				}

			} else {
				usage();
			}
			break;
		}
		case 2: {
			if (args[0].equals("-x") && args[1].endsWith(".pdf")) {
				// Read from pdf and write as BibTex
				XMPMetadata meta = XMPUtil.readRawXMP(new File(args[1]));

				if (meta == null) {
					System.err
							.println("The given pdf does not contain any XMP-metadata.");
				} else {
					XMLUtil.save(meta.getXMPDocument(), System.out, "UTF-8");
				}
				break;
			}

			if (args[0].endsWith(".bib") && args[1].endsWith(".pdf")) {
				ParserResult result = BibtexParser
						.parse(new FileReader(args[0]));

				Collection<BibtexEntry> entries = result.getDatabase()
						.getEntries();

				if (entries.size() == 0) {
					System.err.println("Could not find BibtexEntry in "
							+ args[0]);
				} else {
					XMPUtil.writeXMP(new File(args[1]), entries, result
							.getDatabase(), false);
					System.out.println("XMP written.");
				}
				break;
			}

			usage();
			break;
		}
		case 3: {
			if (!args[1].endsWith(".bib") && !args[2].endsWith(".pdf")) {
				usage();
				break;
			}

			ParserResult result = BibtexParser.parse(new FileReader(args[1]));

			BibtexEntry e = result.getDatabase().getEntryByKey(args[0]);

			if (e == null) {
				System.err.println("Could not find BibtexEntry " + args[0]
						+ " in " + args[0]);
			} else {
				XMPUtil.writeXMP(new File(args[2]), e, result.getDatabase());

				System.out.println("XMP written.");
			}
			break;
		}

		default:
			usage();
		}
	}

	/**
	 * Will try to read XMP metadata from the given file, returning whether
	 * metadata was found.
	 * 
	 * Caution: This method is as expensive as it is reading the actual metadata
	 * itself from the PDF.
	 * 
	 * @param is
	 *            The inputstream to read the PDF from.
	 * @return whether a BibtexEntry was found in the given PDF.
	 */
	public static boolean hasMetadata(InputStream is) {
		try {
			List<BibtexEntry> l = XMPUtil.readXMP(is);
			return l.size() > 0;
		} catch (Exception e) {
			return false;
		}
	}
}
