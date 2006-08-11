package net.sf.jabref.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.transform.TransformerException;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.imports.BibtexParser;
import net.sf.jabref.imports.ParserResult;

import org.jempbox.impl.XMLUtil;
import org.jempbox.xmp.XMPMetadata;
import org.pdfbox.exceptions.COSVisitorException;
import org.pdfbox.pdmodel.PDDocument;
import org.pdfbox.pdmodel.PDDocumentCatalog;
import org.pdfbox.pdmodel.common.PDMetadata;

/**
 * XMPUtils provide support for reading and writing BibTex data as XMP-Metadata
 * in PDF-documents.
 * 
 * @author Christopher Oezbek <oezi@oezi.de>
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
	public static List readXMP(String filename) throws IOException {
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
	 * @throws TransformerException
	 *             If the entry was malformed or unsupported.
	 * @throws IOException
	 *             If the file could not be written to or could not be found.
	 */
	public static void writeXMP(String filename, BibtexEntry entry) throws IOException,
		TransformerException {
		writeXMP(new File(filename), entry);
	}

	/**
	 * Try to read the given BibTexEntry from the XMP-stream of the given
	 * PDF-file.
	 * 
	 * @param file
	 *            The file to read from.
	 * 
	 * @throws IOException
	 *             Throws an IOException if the file cannot be read, so the user
	 *             than remove a lock or cancel the operation.
	 */
	public static List readXMP(File file) throws IOException {

		XMPMetadata meta = readRawXMP(file);

		// If we did not find any metadata, there is nothing to return.
		if (meta == null)
			return null;

		List schemas = meta.getSchemasByNamespaceURI(XMPSchemaBibtex.NAMESPACE);
		List result = new LinkedList();

		Iterator it = schemas.iterator();
		while (it.hasNext()) {
			XMPSchemaBibtex bib = (XMPSchemaBibtex) it.next();

			result.add(bib.getBibtexEntry());
		}
		return result;
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
	 * @throws TransformerException
	 *             If the entry was malformed or unsupported.
	 * @throws IOException
	 *             If the file could not be written to or could not be found.
	 */
	public static void writeXMP(File file, BibtexEntry entry) throws IOException,
		TransformerException {
		List l = new LinkedList();
		l.add(entry);
		writeXMP(file, l);
	}

	/**
	 * Write the given BibtexEntries as XMP-metadata text to the given stream.
	 * 
	 * The text that is written to the stream contains a complete XMP-document.
	 * 
	 * @param bibtexEntries
	 *            The BibtexEntries to write XMP-metadata for.
	 * @throws TransformerException
	 *             Thrown if the bibtexEntries could not transformed to XMP.
	 * @throws IOException
	 *             Thrown if an IOException occured while writing to the stream.
	 */
	public static void toXMP(Collection bibtexEntries, OutputStream outputStream)
		throws IOException, TransformerException {

		XMPMetadata x = new XMPMetadata();

		Iterator it = bibtexEntries.iterator();
		while (it.hasNext()) {
			BibtexEntry e = (BibtexEntry) it.next();
			XMPSchemaBibtex schema = new XMPSchemaBibtex(x);
			x.addSchema(schema);
			schema.setBibtexEntry(e);
		}

		x.save(outputStream);

	}

	/**
	 * Convenience method for toXMP(Collection, OutputStream) returning a String
	 * containing the XMP-metadata of the given collection of BibtexEntries.
	 * 
	 * The resulting metadata string is wrapped as a complete XMP-document.
	 * 
	 * @param bibtexEntries
	 *            The BibtexEntries to return XMP-metadata for.
	 * @return The XMP representation of the given bibtexEntries.
	 * @throws TransformerException
	 *             Thrown if the bibtexEntries could not transformed to XMP.
	 */
	public static String toXMP(Collection bibtexEntries) throws TransformerException {
		try {
			ByteArrayOutputStream bs = new ByteArrayOutputStream();
			toXMP(bibtexEntries, bs);
			return bs.toString();
		} catch (IOException e) {
			throw new TransformerException(e);
		}
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
		PDDocument document = null;

		try {
			document = PDDocument.load(file.getAbsoluteFile());
			if (document.isEncrypted()) {
				throw new EncryptionNotSupportedException(
					"Error: Cannot read metadata from encrypted document.");
			}
			PDDocumentCatalog catalog = document.getDocumentCatalog();
			PDMetadata metaRaw = catalog.getMetadata();

			if (metaRaw == null) {
				return null;
			}

			XMPMetadata meta = new XMPMetadata(XMLUtil.parse(metaRaw.createInputStream()));
			meta.addXMLNSMapping(XMPSchemaBibtex.NAMESPACE, XMPSchemaBibtex.class);
			return meta;

		} finally {
			if (document != null)
				document.close();
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
	 * @param file
	 *            The file to write the entries to.
	 * @param bibtexEntries
	 *            The entries to write to the file.
	 * @throws TransformerException
	 *             If the entry was malformed or unsupported.
	 * @throws IOException
	 *             If the file could not be written to or could not be found.
	 */
	public static void writeXMP(File file, Collection bibtexEntries) throws IOException,
		TransformerException {

		PDDocument document = null;

		try {
			document = PDDocument.load(file.getAbsoluteFile());
			if (document.isEncrypted()) {
				throw new EncryptionNotSupportedException(
					"Error: Cannot add metadata to encrypted document.");
			}
			PDDocumentCatalog catalog = document.getDocumentCatalog();
			PDMetadata metaRaw = catalog.getMetadata();

			XMPMetadata meta;
			if (metaRaw != null) {
				meta = new XMPMetadata(XMLUtil.parse(metaRaw.createInputStream()));
			} else {
				meta = new XMPMetadata();
			}
			meta.addXMLNSMapping(XMPSchemaBibtex.NAMESPACE, XMPSchemaBibtex.class);

			// Remove all current Bibtex-schemas
			List schemas = meta.getSchemasByNamespaceURI(XMPSchemaBibtex.NAMESPACE);
			Iterator it = schemas.iterator();
			while (it.hasNext()) {
				XMPSchemaBibtex bib = (XMPSchemaBibtex) it.next();
				bib.getElement().getParentNode().removeChild(bib.getElement());
			}

			it = bibtexEntries.iterator();
			while (it.hasNext()) {
				BibtexEntry e = (BibtexEntry) it.next();
				XMPSchemaBibtex bibtex = new XMPSchemaBibtex(meta);
				meta.addSchema(bibtex);
				bibtex.setBibtexEntry(e);
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
	 * @see net.sf.jabref.util.XMUtil.main()
	 */
	protected static void usage() {
		System.out.println("Read or write XMP-metadata from or to pdf file.");
		System.out.println("");
		System.out.println("Usage:");
		System.out.println("Read from PDF and print as bibtex:");
		System.out.println("  xmpUtil <pdf>");
		System.out.println("Read from PDF and print raw XMP:");
		System.out.println("  xmpUtil -x <pdf>");
		System.out.println("Write the entry in <bib> given by <key> to the PDF:");
		System.out.println("  xmpUtil <key> <bib> <pdf>");
		System.out.println("Write all entries in <bib> to the PDF:");
		System.out.println("  xmpUtil <bib> <pdf>");
		System.out.println("");
		System.out.println("To report bugs visit http://jabref.sourceforge.net");
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
	public static void main(String[] args) throws IOException, TransformerException {

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
				List l = XMPUtil.readXMP(new File(args[0]));

				Iterator it = l.iterator();
				while (it.hasNext()) {
					BibtexEntry e = (BibtexEntry) it.next();
					StringWriter sw = new StringWriter();
					e.write(sw, new net.sf.jabref.export.LatexFieldFormatter(), false);
					System.out.println(sw.getBuffer().toString());
				}

			} else if (args[0].endsWith(".bib")) {
				// Read from bib and write as XMP

				ParserResult result = BibtexParser.parse(new FileReader(args[0]));
				Collection c = result.getDatabase().getEntries();

				if (c.size() == 0) {
					System.err.println("Could not find BibtexEntry in " + args[0]);
				} else {
					System.out.println(XMPUtil.toXMP(c));
				}

			} else {
				usage();
			}
			break;
		}
		case 2: {
			if (args[0].equals("-x") && args[1].endsWith(".pdf")){
				// Read from pdf and write as BibTex
				XMPMetadata meta = XMPUtil.readRawXMP(new File(args[1]));
				
				if (meta == null){
					System.err.println("The given pdf does not contain any XMP-metadata.");
				} else {
					XMLUtil.save(meta.getXMPDocument(), System.out, "UTF-8");
				}
				break;
			} 
			
			if (args[0].endsWith(".bib") && args[1].endsWith(".pdf")) {
				ParserResult result = BibtexParser.parse(new FileReader(args[0]));

				Collection c = result.getDatabase().getEntries();

				if (c.size() == 0) {
					System.err.println("Could not find BibtexEntry in " + args[0]);
				} else {
					XMPUtil.writeXMP(new File(args[1]), c);
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
				System.err.println("Could not find BibtexEntry " + args[0] + " in " + args[0]);
			} else {
				XMPUtil.writeXMP(new File(args[2]), e);

				System.out.println("XMP written.");
			}
			break;
		}

		default:
			usage();
		}
	}
}