/*
 * Created on Jun 29, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package net.sf.jabref.imports;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.undo.UndoableFieldChange;

import org.xml.sax.AttributeList;
import org.xml.sax.HandlerBase;
import org.xml.sax.SAXException;

/**
 * @author mspiegel
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class CiteSeerUndoHandler extends HandlerBase {

	NamedCompound citeseerNamedCompound = null;
	BibtexEntry bibEntry = null;
	String nextField = null;
	boolean nextAssign = false;
	
	/**
	 * @param be
	 */
	public CiteSeerUndoHandler(NamedCompound newCompound, BibtexEntry be) {
		citeseerNamedCompound = newCompound;
		bibEntry = be;		
	}
	
	public void characters(char[] ch, int start, int length) {
		if (nextAssign == true) {
			String target = new String(ch, start, length);
			if (nextField.equals("title")) {
				UndoableFieldChange fieldChange = new UndoableFieldChange(bibEntry, nextField,
					bibEntry.getField(nextField), target);				
				citeseerNamedCompound.addEdit(fieldChange);	
				bibEntry.setField(nextField, target);				
			} else if (nextField.equals("year")) {
				UndoableFieldChange fieldChange = new UndoableFieldChange(bibEntry, nextField,
					bibEntry.getField(nextField), String.valueOf(target.substring(0,4)));				
				citeseerNamedCompound.addEdit(fieldChange);				
				bibEntry.setField(nextField, String.valueOf(target.substring(0,4)));			
			} else if (nextField.equals("url")) {
				UndoableFieldChange fieldChange = new UndoableFieldChange(bibEntry, nextField,
					bibEntry.getField(nextField), target);				
				citeseerNamedCompound.addEdit(fieldChange);
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
			UndoableFieldChange fieldChange = new UndoableFieldChange(bibEntry, "author",
				bibEntry.getField("author"), newAuthor);				
			citeseerNamedCompound.addEdit(fieldChange);				
			bibEntry.setField("author", newAuthor);			
		} else {
			UndoableFieldChange fieldChange = new UndoableFieldChange(bibEntry, "author",
				bibEntry.getField("author"), bibEntry.getField("author") + " and " + newAuthor);				
			citeseerNamedCompound.addEdit(fieldChange);
			bibEntry.setField("author", bibEntry.getField("author") + " and " + newAuthor);				
		}
	}



}
