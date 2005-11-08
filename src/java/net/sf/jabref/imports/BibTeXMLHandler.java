package net.sf.jabref.imports;

import org.xml.sax.*;
import org.xml.sax.helpers.*;
import java.util.*;
import net.sf.jabref.*;
/*
  Copyright (C) 2000-2004 E.L. Willighagen <egonw@sci.kun.nl>

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or (at
  your option) any later version.

  This program is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
  USA

*/

/**
 * Reader for the BibTeXML format. See
 * <a href="http://bibtexml.sourceforge.net/">bibtexml.sf.net</a>.
 *
 * @author Egon Willighagen
 */
public class BibTeXMLHandler extends DefaultHandler {

    private ArrayList bibitems;

    private BibtexEntry b; // the entry being read

    // XML parsing stuff
    private String name; // the current element name
    private String currentChars;

    public BibTeXMLHandler() {
        super();
    }

    public ArrayList getItems(){ return bibitems;}

    // SAX parsing methods

    public void doctypeDecl(String name, String publicId,
        String systemId) {}

    public void startDocument() {
        bibitems = new ArrayList();
    }

    public void endDocument() {
    }

    public void characters(char ch[], int start, int length) {
        String s = new String(ch, start, length).trim();
        currentChars += s;
    }

    public void startElement(String uri, String local, String raw, Attributes atts) {
        String name = raw;
        this.name = name;
        if (name.equals("bibtex:entry")) {
            String articleID = null;
            for (int i = 0; i < atts.getLength(); i++) {
                if (atts.getQName(i).equals("bibtex:id") ||
                    atts.getQName(i).equals("id")) {
                    articleID = atts.getValue(i);
                }
            }
            b = new BibtexEntry(Util.createNeutralId());
	    b.setField(Globals.KEY_FIELD, articleID);
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


