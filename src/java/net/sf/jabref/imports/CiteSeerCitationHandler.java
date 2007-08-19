package net.sf.jabref.imports;



import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author mspiegel
 *
 */
public class CiteSeerCitationHandler extends DefaultHandler {

	protected boolean correctDirection;
	protected boolean getCitation;
	protected Map<String, Boolean> citationList;

	/**
	 * @param identifierVector
	 */
	public CiteSeerCitationHandler(Map<String, Boolean> citationHashTable) {
		citationList = citationHashTable;		
	}

	public void characters(char[] ch, int start, int length) {
		if (getCitation == true) {
			String target = new String(ch, start, length);
			if (citationList.get(target) == null) {
				citationList.put(target, Boolean.TRUE);
			}
			getCitation = false;
		}
	}
	
	public void startDocument() throws SAXException {
		correctDirection = false;
		getCitation = false;
	}


	public void startElement(String name, String localName, String qName, Attributes attrs)
			throws SAXException {
		if (qName.equals("oai_citeseer:relation")) {
			for (int i = 0; i < attrs.getLength(); i++) {
			   String attrName = attrs.getQName(i);
			   String attrValue = attrs.getValue(i);	   
			   if (attrName.equals("type") && attrValue.equals("Is Referenced By")) {
			   		correctDirection = true;
			   } else if (attrName.equals("type") && attrValue.equals("References")) {
			   		correctDirection = false;
			   }
			 }
		} else if (qName.equals("oai_citeseer:uri") && correctDirection) {
			getCitation = true;
		}
	}
}