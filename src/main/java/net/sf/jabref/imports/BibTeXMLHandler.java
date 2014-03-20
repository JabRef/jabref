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

import java.util.ArrayList;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.BibtexFields;
import net.sf.jabref.Util;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Reader for the BibTeXML format. See
 * <a href="http://bibtexml.sourceforge.net/">bibtexml.sf.net</a>.
 *
 * @author Egon Willighagen
 */
public class BibTeXMLHandler extends DefaultHandler {

    private ArrayList<BibtexEntry> bibitems;

    private BibtexEntry b; // the entry being read

    // XML parsing stuff
    private String currentChars;

    public BibTeXMLHandler() {
        super();
    }

    public ArrayList<BibtexEntry> getItems(){ return bibitems;}

    // SAX parsing methods

    public void doctypeDecl(String name, String publicId,
        String systemId) {}

    public void startDocument() {
        bibitems = new ArrayList<BibtexEntry>();
    }

    public void endDocument() {
    }

    public void characters(char ch[], int start, int length) {
        String s = new String(ch, start, length).trim();
        currentChars += s;
    }

    public void startElement(String uri, String local, String raw, Attributes atts) {
        String name = raw;
        if (name.equals("bibtex:entry")) {
            String articleID = null;
            for (int i = 0; i < atts.getLength(); i++) {
                if (atts.getQName(i).equals("bibtex:id") ||
                    atts.getQName(i).equals("id")) {
                    articleID = atts.getValue(i);
                }
            }
            b = new BibtexEntry(Util.createNeutralId());
            b.setField(BibtexFields.KEY_FIELD, articleID);
        } else if (
            name.equals("bibtex:article") ||
            name.equals("bibtex:inbook") ||
            name.equals("bibtex:book") ||
            name.equals("bibtex:booklet") ||
            name.equals("bibtex:incollection") ||
            name.equals("bibtex:inproceedings") ||
            name.equals("bibtex:proceedings") ||
            name.equals("bibtex:manual") ||
            name.equals("bibtex:mastersthesis") ||
            name.equals("bibtex:phdthesis") ||
            name.equals("bibtex:techreport") ||
            name.equals("bibtex:unpublished") ||
            name.equals("bibtex:misc") ||
            name.equals("bibtex:other")) {
            BibtexEntryType tp = BibtexEntryType.getType(local);
            b.setType(tp);
        }
        currentChars = "";
    }

    public void endElement(String uri, String local, String raw) {
        String name = raw;
        if (name.equals("bibtex:entry")) {
            bibitems.add( b  );
        } else if (name.startsWith("bibtex:")) {
            b.setField(local, currentChars);
            // Util.pr(local+ " "+currentChars);
        }
        currentChars = "";
    }

}
