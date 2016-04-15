/*  Copyright (C) 2003-2016 JabRef contributors.
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
import java.util.Collections;
import java.util.List;

import net.sf.jabref.model.entry.IdGenerator;
import net.sf.jabref.model.entry.BibEntry;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Reader for the BibTeXML format. See
 * <a href="http://bibtexml.sourceforge.net/">bibtexml.sf.net</a>.
 *
 * @author Egon Willighagen
 */
class BibTeXMLHandler extends DefaultHandler {

    private static final String BIBTEXML_URI = "http://bibtexml.sf.net/";

    private List<BibEntry> bibitems;

    private BibEntry b; // the entry being read

    // XML parsing stuff
    private String currentChars;


    public List<BibEntry> getItems() {
        if (bibitems == null) {
            return Collections.emptyList();
        }
        return bibitems;
    }

    // SAX parsing methods

    @Override
    public void startDocument() {
        bibitems = new ArrayList<>();
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        String s = new String(ch, start, length).trim();
        currentChars += s;
    }

    @Override
    public void startElement(String uri, String local, String raw, Attributes atts) {
        if (BIBTEXML_URI.equals(uri)) {
            if ("entry".equals(local)) {
                b = new BibEntry(IdGenerator.next());
                // Determine and-set bibtex key
                String bibtexKey = null;
                for (int i = 0; i < atts.getLength(); i++) {
                    String attrURI = atts.getURI(i);
                    if ((BIBTEXML_URI.equals(attrURI) || "".equals(attrURI)) && "id".equals(atts.getLocalName(i))) {
                        bibtexKey = atts.getValue(i);
                    }
                }
                if (bibtexKey != null) {
                    b.setField(BibEntry.KEY_FIELD, bibtexKey);
                }
            } else if ("article".equals(local) || "inbook".equals(local) || "book".equals(local)
                    || "booklet".equals(local) || "incollection".equals(local) || "inproceedings".equals(local)
                    || "proceedings".equals(local) || "manual".equals(local) || "mastersthesis".equals(local)
                    || "phdthesis".equals(local) || "techreport".equals(local) || "unpublished".equals(local)
                    || "misc".equals(local)) {
                b.setType(local);
            }
        }
        currentChars = "";
    }

    @Override
    public void endElement(String uri, String local, String raw) {
        if (BIBTEXML_URI.equals(uri)) {
            if ("entry".equals(local)) {
                bibitems.add(b);
            } else {
                if (!currentChars.trim().isEmpty()) {
                    b.setField(local, currentChars);
                }
            }
        }
        currentChars = "";
    }

}
