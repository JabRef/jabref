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
