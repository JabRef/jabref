/*
 * Created on Jun 13, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package net.sf.jabref.imports;

import net.sf.jabref.*;

import org.xml.sax.AttributeList;
import org.xml.sax.HandlerBase;
import org.xml.sax.SAXException;

/**
 * @author mspiegel
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class CiteSeerImportHandler extends HandlerBase {

	BibtexEntry bibEntry = null;
	String nextField = null;
	boolean nextAssign = false;
	
	/**
	 * @param be
	 * 
	 * We must remember to clobber the author field,
	 * because of the current implementation of addAuthor()
	 */
	public CiteSeerImportHandler(BibtexEntry be) {
		bibEntry = be;		
		bibEntry.setField("author", null);
	}
	
	public void characters(char[] ch, int start, int length) {
		if (nextAssign == true) {
			String target = new String(ch, start, length);
			if (nextField.equals("title")) {
				bibEntry.setField(nextField, target);
			} else if (nextField.equals("year")) {
				bibEntry.setField(nextField, String.valueOf(target.substring(0,4)));
			} else if (nextField.equals("url")) {
				bibEntry.setField(nextField, target);				
			}
			nextAssign = false;
		}
	}		

		
	public void startElement(String name, AttributeList attrs) throws SAXException {
		if (name.equals("oai_citeseer:author")) {
			addAuthor(attrs.getValue("name"));				
		} else if (name.equals("dc:title")) {
			nextField = "title";
			nextAssign = true;
		} else if (name.equals("dc:date")) {
			nextField = "year";
			nextAssign = true;
		} else if (name.equals("dc:identifier")) {
			nextField = "url";
			nextAssign = true;
		}
	}

	/**
	 * @param string
	 */
	private void addAuthor(String newAuthor) {
		if (bibEntry.getField("author") == null) {
			bibEntry.setField("author", newAuthor);
		} else {
			bibEntry.setField("author", bibEntry.getField("author") + " and " + newAuthor);			
		}
	}
}
