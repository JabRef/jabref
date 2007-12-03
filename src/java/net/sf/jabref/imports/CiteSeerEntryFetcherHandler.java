/*
 * Created on Jun 29, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package net.sf.jabref.imports;

import net.sf.jabref.BibtexEntry;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAXHandler used with CiteSeerEntryFetcher.
 */
public class CiteSeerEntryFetcherHandler extends DefaultHandler {

    BibtexEntry entry = null;

    String nextField = null;

    boolean nextAssign = false;

    String newAuthors = null;

    int citeseerCitationCount = 0;

    public CiteSeerEntryFetcherHandler(BibtexEntry be) {
        entry = be;
    }

    public void characters(char[] ch, int start, int length) {
        if (nextAssign == true) {
            String target = new String(ch, start, length);
            if (nextField.equals("title")) {
                entry.setField(nextField, target);
            } else if (nextField.equals("year")) {
                entry.setField(nextField, String.valueOf(target.substring(0, 4)));
            } else if (nextField.equals("citeseerurl")) {
                entry.setField(nextField, target);
            }
            nextAssign = false;
        }
    }

    public void startElement(String name, String localName, String qName, Attributes attrs)
        throws SAXException {
        if (qName.equals("oai_citeseer:relation")) {
            for (int i = 0; i < attrs.getLength(); i++) {
                String attrName = attrs.getQName(i);
                String attrValue = attrs.getValue(i);
                if (attrName.equals("type") && attrValue.equals("Is Referenced By")) {
                    citeseerCitationCount++;
                }
            }
        } else if (qName.equals("oai_citeseer:author")) {
            if (newAuthors == null) {
                newAuthors = attrs.getValue("name");
            } else {
                newAuthors = newAuthors + " and " + attrs.getValue("name");
            }
        } else if (qName.equals("dc:title")) {
            nextField = "title";
            nextAssign = true;
        } else if (qName.equals("dc:date")) {
            nextField = "year";
            nextAssign = true;
        } else if (qName.equals("dc:identifier")) {
            nextField = "citeseerurl";
            nextAssign = true;
        }
    }

    public void endDocument() {
        if (newAuthors != null) {
            entry.setField("author", newAuthors);
        }
        entry.setField("citeseercitationcount", String.valueOf(citeseerCitationCount));
    }
}