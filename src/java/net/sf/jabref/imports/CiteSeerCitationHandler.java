package net.sf.jabref.imports;
import java.util.Hashtable;
import java.util.Vector;

import org.xml.sax.AttributeList;
import org.xml.sax.HandlerBase;
import org.xml.sax.SAXException;

/*
 * Created on Jun 13, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

/**
 * @author mspiegel
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class CiteSeerCitationHandler extends HandlerBase {

	protected boolean correctDirection;
	protected boolean getCitation;
	protected Hashtable citationList;

	/**
	 * @param identifierVector
	 */
	public CiteSeerCitationHandler(Hashtable citationHashTable) {
		citationList = citationHashTable;		
	}

	public void characters(char[] ch, int start, int length) {
		if (getCitation == true) {
			String target = new String(ch, start, length);
			if (citationList.get(target) == null) {
				citationList.put(target,new Boolean(true));
			}
			getCitation = false;
		}
	}
	
	public void startDocument() throws SAXException {
		correctDirection = false;
		getCitation = false;
	}


	public void startElement(String name, AttributeList attrs) throws SAXException {
		if (name.equals("oai_citeseer:relation")) {
			for (int i = 0; i < attrs.getLength(); i++) {
			   String attrName = attrs.getName(i);
			   String attrValue = attrs.getValue(i);	   
			   if (attrName.equals("type") && attrValue.equals("Is Referenced By")) {
			   		correctDirection = true;
			   } else if (attrName.equals("type") && attrValue.equals("References")) {
			   		correctDirection = false;
			   }
			 }
		} else if (name.equals("oai_citeseer:uri") && correctDirection) {
			getCitation = true;
		}
	}
}