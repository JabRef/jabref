package net.sf.jabref.imports;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexFields;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX-Handler to parse OAI2-xml files.
 * 
 * @author Ulrich St&auml;rk
 * @author Christian Kopf
 * 
 * @version $Revision$ ($Date$)
 *
 */
public class OAI2Handler extends DefaultHandler {

	BibtexEntry be;
	String nextField;
	String authors = "";
	String keyname;
	String forenames;
	String key = "";
	String year = "";
	boolean assigned = false;
	
	public OAI2Handler(BibtexEntry be) {
		this.be = be;
	}
	
	public void endDocument() throws SAXException {
		be.setField("author",authors);
		if(year.length() > 0)
			key += year.substring(year.length()-2, year.length());
		be.setField(BibtexFields.KEY_FIELD,key);
	}

	
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		
        if(localName.equals("id")) {
			nextField = "id";
			assigned = true;
		} else if(localName.equals("keyname")) {
			nextField = "keyname";
			assigned = true;
		} else if(localName.equals("author")) {
			keyname = "";
			forenames = "";
		} else if(localName.equals("forenames")) {
			nextField = "forenames";
			assigned = true;
		} else if(localName.equals("journal-ref")) {
			nextField = "journal-ref";
			assigned = true;
		} else if(localName.equals("datestamp")) {
			nextField = "datestamp";
			assigned = true;
		} else if(localName.equals("title")) {
			nextField = "title";
			assigned = true;
		} else if(localName.equals("abstract")) {
			nextField = "abstract";
			assigned = true;
		} else if(localName.equals("comments")) {
			nextField = "comments";
			assigned = true;
		} else if(localName.equals("report-no")) {
			nextField = "reportno";
			assigned = true;
		} else if(localName.equals("error")) {
		    nextField = "error";
            assigned = true;
        }
	}

	
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if(localName.equals("author")) {
			String temp = forenames + " " + keyname;
			if(!authors.equals(""))
				authors += " and ";
			authors += temp;
		}
	}

	
	public void characters(char[] ch, int start, int length) throws SAXException {
		String content = new String(ch,start,length);
		if(assigned) {
		    if (nextField.equals("error")) {
                throw new RuntimeException(content);
            } else if(nextField.equals("id")) {
				be.setField("eprint",content);
			} else if(nextField.equals("keyname")) {
				keyname = content;
				key += content.substring(0,3);
			} else if(nextField.equals("forenames")) {
				forenames = content;
			} else if(nextField.equals("journal-ref")) {
				String journal = content.replaceFirst("[0-9].*", "");
				be.setField("journal", journal);
				String volume = content.replaceFirst(journal,"");
				volume = volume.replaceFirst(" .*", "");
				be.setField("volume", volume);
				year = content.replaceFirst(".*?\\(", "");
				year = year.replaceFirst("\\).*", "");
				be.setField("year", year);
				String pages = content.replaceFirst(journal,"");
				pages = pages.replaceFirst(volume, "");
				pages = pages.replaceFirst("\\("+year+"\\)", "");
				pages = pages.replaceAll(" ", "");
				be.setField("pages", pages);
			} else if(nextField.equals("datestamp")) {
				if(year.equals(""))
					year = content.replaceFirst("-.*", "");
				be.setField("year", year);
			} else if(nextField.equals("title")) {
				be.setField("title",content);
			} else if(nextField.equals("abstract")) {
				be.setField("abstract",content);
			} else if(nextField.equals("comments")) {
				be.setField("comments",content);
			} else if(nextField.equals("reportno")) {
				be.setField("reportno",content);
			}
			assigned = false;
		}
	}
}
