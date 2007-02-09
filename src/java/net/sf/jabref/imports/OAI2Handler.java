package net.sf.jabref.imports;

import net.sf.jabref.BibtexEntry;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX-Handler to parse OAI2-xml files.
 * 
 * @author Ulrich St&auml;rk
 * @author Christian Kopf
 * @author Christopher Oezbek
 * 
 * @version $Revision$ ($Date$)
 * 
 */
public class OAI2Handler extends DefaultHandler {

	BibtexEntry entry;

	StringBuffer authors;

	String keyname;

	String forenames;

	StringBuffer characters;

	public OAI2Handler(BibtexEntry be) {
		this.entry = be;
	}
	
	public void startDocument() throws SAXException {
		authors = new StringBuffer();
	}

	public void characters(char[] ch, int start, int length) throws SAXException {
		characters.append(ch, start, length);
	}

	public void startElement(String uri, String localName, String qualifiedName,
		Attributes attributes) throws SAXException {

		characters = new StringBuffer();
	}

	public void endElement(String uri, String localName, String qualifiedName) throws SAXException {

		String content = characters.toString();

		if (qualifiedName.equals("error")) {
			throw new RuntimeException(content);
		} else if (qualifiedName.equals("id")) {
			entry.setField("eprint", content);
		} else if (qualifiedName.equals("keyname")) {
			keyname = content;
		} else if (qualifiedName.equals("forenames")) {
			forenames = content;
		} else if (qualifiedName.equals("journal-ref")) {
			String journal = content.replaceFirst("[0-9].*", "");
			entry.setField("journal", journal);
			String volume = content.replaceFirst(journal, "");
			volume = volume.replaceFirst(" .*", "");
			entry.setField("volume", volume);
			String year = content.replaceFirst(".*?\\(", "");
			year = year.replaceFirst("\\).*", "");
			entry.setField("year", year);
			String pages = content.replaceFirst(journal, "");
			pages = pages.replaceFirst(volume, "");
			pages = pages.replaceFirst("\\(" + year + "\\)", "");
			pages = pages.replaceAll(" ", "");
			entry.setField("pages", pages);
		} else if (qualifiedName.equals("datestamp")) {
			String year = (String) entry.getField("year");
			if (year == null || year.equals("")) {
				entry.setField("year", content.replaceFirst("-.*", ""));
			}
		} else if (qualifiedName.equals("title")) {
			entry.setField("title", content);
		} else if (qualifiedName.equals("abstract")) {
			entry.setField("abstract", content);
		} else if (qualifiedName.equals("comments")) {
			entry.setField("comments", content);
		} else if (qualifiedName.equals("report-no")) {
			entry.setField("reportno", content);
		} else if (qualifiedName.equals("author")) {
			String author = forenames + " " + keyname;
			if (authors.length() > 0)
				authors.append(" and ");
			authors.append(author);
		}
	}

	public void endDocument() throws SAXException {
		entry.setField("author", authors.toString());
	}

}
