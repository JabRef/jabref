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
package net.sf.jabref.logic.mods;

import java.io.StringWriter;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.export.layout.LayoutFormatter;
import net.sf.jabref.export.layout.format.XMLChars;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author Michael Wrighton
 */
class MODSEntry {

    private static final Log LOGGER = LogFactory.getLog(MODSEntry.class);

    private String entryType = "mods"; // could also be relatedItem
    private String id;
    private List<PersonName> authors = null;

    // should really be handled with an enum
    private String issuance = "monographic";
    private PageNumbers pages = null;

    private String publisher = null;
    private String date = null;

    private String title = null;

    private String number;
    private String volume;
    private String genre = null;
    private final Set<String> handledExtensions;

    private MODSEntry host;
    private final Map<String, String> extensionFields;

    private static final String BIBTEX = "bibtex_";

    private final boolean CHARFORMAT = false;


    private MODSEntry() {
        extensionFields = new HashMap<>();
        handledExtensions = new HashSet<>();

    }

    public MODSEntry(BibtexEntry bibtex) {
        this();
        handledExtensions.add(MODSEntry.BIBTEX + "publisher");
        handledExtensions.add(MODSEntry.BIBTEX + "title");
        handledExtensions.add(MODSEntry.BIBTEX + "bibtexkey");
        handledExtensions.add(MODSEntry.BIBTEX + "author");
        populateFromBibtex(bibtex);
    }

    private void populateFromBibtex(BibtexEntry bibtex) {
        LayoutFormatter chars = new XMLChars();
        if (bibtex.getField("title") != null) {
            if (CHARFORMAT) {
                title = chars.format(bibtex.getField("title"));
            } else {
                title = bibtex.getField("title");
            }
        }

        if (bibtex.getField("publisher") != null) {
            if (CHARFORMAT) {
                publisher = chars.format(bibtex.getField("publisher"));
            } else {
                publisher = bibtex.getField("publisher");
            }
        }

        if (bibtex.getField("bibtexkey") != null) {
            id = bibtex.getField("bibtexkey");
        }
        if (bibtex.getField("place") != null) {
            String place = null;
            if (CHARFORMAT) {
                place = chars.format(bibtex.getField("place"));
            } else {
                place = bibtex.getField("place");
            }
        }

        date = getDate(bibtex);
        genre = getMODSgenre(bibtex);
        if (bibtex.getField("author") != null) {
            authors = getAuthors(bibtex.getField("author"));
        }
        if (bibtex.getType() == BibtexEntryType.ARTICLE ||
                bibtex.getType() == BibtexEntryType.INPROCEEDINGS) {
            host = new MODSEntry();
            host.entryType = "relatedItem";
            host.title = bibtex.getField("booktitle");
            host.publisher = bibtex.getField("publisher");
            host.number = bibtex.getField("number");
            if (bibtex.getField("pages") != null) {
                host.volume = bibtex.getField("volume");
            }
            host.issuance = "continuing";
            if (bibtex.getField("pages") != null) {
                host.pages = new PageNumbers(bibtex.getField("pages"));
            }
        }

        populateExtensionFields(bibtex);

    }

    private void populateExtensionFields(BibtexEntry e) {

        for (String field : e.getAllFields()) {
            String value = e.getField(field);
            field = MODSEntry.BIBTEX + field;
            extensionFields.put(field, value);
        }
    }

    private List<PersonName> getAuthors(String authors) {
        List<PersonName> result = new LinkedList<>();
        LayoutFormatter chars = new XMLChars();

        if (!authors.contains(" and ")) {
            if (CHARFORMAT) {
                result.add(new PersonName(chars.format(authors)));
            } else {
                result.add(new PersonName(authors));
            }
        } else {
            String[] names = authors.split(" and ");
            for (String name : names) {
                if (CHARFORMAT) {
                    result.add(new PersonName(chars.format(name)));
                } else {
                    result.add(new PersonName(name));
                }
            }
        }
        return result;
    }

    /* construct a MODS date object */
    private String getDate(BibtexEntry bibtex) {
        String result = "";
        if (bibtex.getField("year") != null) {
            result += bibtex.getField("year");
        }
        if (bibtex.getField("month") != null) {
            result += '-' + bibtex.getField("month");
        }

        return result;
    }

    // must be from http://www.loc.gov/marc/sourcecode/genre/genrelist.html
    private String getMODSgenre(BibtexEntry bibtex) {
        /**
         * <pre> String result; if (bibtexType.equals("Mastersthesis")) result =
         * "theses"; else result = "conference publication"; // etc... </pre>
         */
        return bibtex.getType().getName();
    }

    private Node getDOMrepresentation() {
        Node result = null;
        try {
            DocumentBuilder d = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            result = getDOMrepresentation(d.newDocument());
        } catch (ParserConfigurationException e) {
            LOGGER.warn("Cannot get DOM representation", e);
        }
        return result;
    }

    public Element getDOMrepresentation(Document document) {

        Element mods = document.createElement(entryType);
        mods.setAttribute("version", "3.0");
        // mods.setAttribute("xmlns:xlink:", "http://www.w3.org/1999/xlink");
        // title
        if (title != null) {
            Element titleInfo = document.createElement("titleInfo");
            Element mainTitle = document.createElement("title");
            mainTitle.appendChild(document.createTextNode(stripNonValidXMLCharacters(title)));
            titleInfo.appendChild(mainTitle);
            mods.appendChild(titleInfo);
        }
        if (authors != null) {
            for (PersonName name : authors) {
                Element modsName = document.createElement("name");
                modsName.setAttribute("type", "personal");
                if (name.getSurname() != null) {
                    Element namePart = document.createElement("namePart");
                    namePart.setAttribute("type", "family");
                    namePart.appendChild(document.createTextNode(stripNonValidXMLCharacters(name.getSurname())));
                    modsName.appendChild(namePart);
                }
                if (name.getGivenNames() != null) {
                    Element namePart = document.createElement("namePart");
                    namePart.setAttribute("type", "given");
                    namePart.appendChild(document.createTextNode(stripNonValidXMLCharacters(name.getGivenNames())));
                    modsName.appendChild(namePart);
                }
                Element role = document.createElement("role");
                Element roleTerm = document.createElement("roleTerm");
                roleTerm.setAttribute("type", "text");
                roleTerm.appendChild(document.createTextNode("author"));
                role.appendChild(roleTerm);
                modsName.appendChild(role);
                mods.appendChild(modsName);
            }
        }
        //publisher
        Element originInfo = document.createElement("originInfo");
        mods.appendChild(originInfo);
        if (this.publisher != null) {
            Element publisher = document.createElement("publisher");
            publisher.appendChild(document.createTextNode(stripNonValidXMLCharacters(this.publisher)));
            originInfo.appendChild(publisher);
        }
        if (date != null) {
            Element dateIssued = document.createElement("dateIssued");
            dateIssued.appendChild(document.createTextNode(stripNonValidXMLCharacters(date)));
            originInfo.appendChild(dateIssued);
        }
        Element issuance = document.createElement("issuance");
        issuance.appendChild(document.createTextNode(stripNonValidXMLCharacters(this.issuance)));
        originInfo.appendChild(issuance);

        if (id != null) {
            Element idref = document.createElement("identifier");
            idref.appendChild(document.createTextNode(stripNonValidXMLCharacters(id)));
            mods.appendChild(idref);
            mods.setAttribute("ID", id);

        }
        Element typeOfResource = document.createElement("typeOfResource");
        String type = "text";
        typeOfResource.appendChild(document.createTextNode(stripNonValidXMLCharacters(type)));
        mods.appendChild(typeOfResource);

        if (genre != null) {
            Element genreElement = document.createElement("genre");
            genreElement.setAttribute("authority", "marc");
            genreElement.appendChild(document.createTextNode(stripNonValidXMLCharacters(genre)));
            mods.appendChild(genreElement);
        }

        if (host != null) {
            Element relatedItem = host.getDOMrepresentation(document);
            relatedItem.setAttribute("type", "host");
            mods.appendChild(relatedItem);
        }
        if (pages != null) {
            mods.appendChild(pages.getDOMrepresentation(document));
        }

            /* now generate extension fields for unhandled data */
        for (Map.Entry<String, String> theEntry : extensionFields.entrySet()) {
            Element extension = document.createElement("extension");
            String field = theEntry.getKey();
            String value = theEntry.getValue();
            if (handledExtensions.contains(field)) {
                continue;
            }
            Element theData = document.createElement(field);
            theData.appendChild(document.createTextNode(stripNonValidXMLCharacters(value)));
            extension.appendChild(theData);
            mods.appendChild(extension);
        }
        return mods;

    }

    /**
     * This method ensures that the output String has only
     * valid XML unicode characters as specified by the
     * XML 1.0 standard. For reference, please see
     * <a href="http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char">the
     * standard</a>. This method will return an empty
     * String if the input is null or empty.
     * <p>
     * URL: http://cse-mjmcl.cse.bris.ac.uk/blog/2007/02/14/1171465494443.html
     *
     * @param in The String whose non-valid characters we want to remove.
     * @return The in String, stripped of non-valid characters.
     */
    private String stripNonValidXMLCharacters(String in) {
        StringBuffer out = new StringBuffer(); // Used to hold the output.
        char current; // Used to reference the current character.

        if (in == null || in != null && in.isEmpty()) {
            return ""; // vacancy test.
        }
        for (int i = 0; i < in.length(); i++) {
            current = in.charAt(i); // NOTE: No IndexOutOfBoundsException caught here; it should not happen.
            if (current == 0x9 ||
                    current == 0xA ||
                    current == 0xD ||
                    current >= 0x20 && current <= 0xD7FF ||
                    current >= 0xE000 && current <= 0xFFFD ||
                    current >= 0x10000 && current <= 0x10FFFF) {
                out.append(current);
            }
        }
        return out.toString();
    }

    /*
     * render as XML
     * 
     */
    @Override
    public String toString() {
        StringWriter sresult = new StringWriter();
        try {
            DOMSource source = new DOMSource(getDOMrepresentation());
            StreamResult result = new StreamResult(sresult);
            Transformer trans = TransformerFactory.newInstance().newTransformer();
            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            trans.transform(source, result);
        } catch (TransformerException e) {
            LOGGER.warn("Could not transform DOM", e);
        }
        return sresult.toString();
    }

}
