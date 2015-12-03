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
package net.sf.jabref.importer.fileformat;

import java.util.ArrayList;

import net.sf.jabref.bibtex.EntryTypes;
import net.sf.jabref.model.entry.EntryType;
import net.sf.jabref.model.entry.IdGenerator;
import net.sf.jabref.model.entry.BibtexEntry;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Reader for the BibTeXML format. See
 * <a href="http://bibtexml.sourceforge.net/">bibtexml.sf.net</a>.
 *
 * @author Egon Willighagen
 */
class BibTeXMLHandler extends DefaultHandler {

    private ArrayList<BibtexEntry> bibitems;

    private BibtexEntry b; // the entry being read

    // XML parsing stuff
    private String currentChars;


    public BibTeXMLHandler() {
        super();
    }

    public ArrayList<BibtexEntry> getItems() {
        return bibitems;
    }

    // SAX parsing methods

    @Override
    public void startDocument() {
        bibitems = new ArrayList<>();
    }

    @Override
    public void endDocument() {
        // Empty method
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        String s = new String(ch, start, length).trim();
        currentChars += s;
    }

    @Override
    public void startElement(String uri, String local, String raw, Attributes atts) {
        if ("bibtex:entry".equals(raw)) {
            String articleID = null;
            for (int i = 0; i < atts.getLength(); i++) {
                if ("bibtex:id".equals(atts.getQName(i)) ||
                        "id".equals(atts.getQName(i))) {
                    articleID = atts.getValue(i);
                }
            }
            b = new BibtexEntry(IdGenerator.next());
            b.setField(BibtexEntry.KEY_FIELD, articleID);
        } else if ("bibtex:article".equals(raw) ||
                "bibtex:inbook".equals(raw) ||
                "bibtex:book".equals(raw) ||
                "bibtex:booklet".equals(raw) ||
                "bibtex:incollection".equals(raw) ||
                "bibtex:inproceedings".equals(raw) ||
                "bibtex:proceedings".equals(raw) ||
                "bibtex:manual".equals(raw) ||
                "bibtex:mastersthesis".equals(raw) ||
                "bibtex:phdthesis".equals(raw) ||
                "bibtex:techreport".equals(raw) ||
                "bibtex:unpublished".equals(raw) ||
                "bibtex:misc".equals(raw) ||
                "bibtex:other".equals(raw)) {
            EntryType tp = EntryTypes.getType(local);
            b.setType(tp);
        }
        currentChars = "";
    }

    @Override
    public void endElement(String uri, String local, String raw) {
        if ("bibtex:entry".equals(raw)) {
            bibitems.add(b);
        } else if (raw.startsWith("bibtex:")) {
            b.setField(local, currentChars);
            // Util.pr(local+ " "+currentChars);
        }
        currentChars = "";
    }

}
