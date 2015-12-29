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
package net.sf.jabref.logic.mods;

import java.io.StringWriter;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.exporter.layout.LayoutFormatter;
import net.sf.jabref.exporter.layout.format.XMLChars;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author Michael Wrighton
 *
 */
class MODSEntry {

    private String entryType = "mods"; // could also be relatedItem
    private String id;
    private List<PersonName> authors;

    // should really be handled with an enum
    private String issuance = "monographic";
    private PageNumbers pages;

    private String publisher;
    private String date;

    private String title;

    private String number;
    private String volume;
    private String genre;
    private String place;
    private final Set<String> handledExtensions;

    private MODSEntry host;
    private final Map<String, String> extensionFields;

    private static final String BIBTEX = "bibtex_";

    private static final boolean CHARFORMAT = false;

    private static final Log LOGGER = LogFactory.getLog(MODSEntry.class);

    private final LayoutFormatter chars = new XMLChars();


    private MODSEntry() {
        extensionFields = new HashMap<>();
        handledExtensions = new HashSet<>();

    }

    public MODSEntry(BibEntry bibtex) {
        this();
        handledExtensions.add(MODSEntry.BIBTEX + "publisher");
        handledExtensions.add(MODSEntry.BIBTEX + "title");
        handledExtensions.add(MODSEntry.BIBTEX + "bibtexkey");
        handledExtensions.add(MODSEntry.BIBTEX + "author");
        populateFromBibtex(bibtex);
    }

    private void populateFromBibtex(BibEntry bibtex) {
        if (bibtex.hasField("title")) {
            if (CHARFORMAT) {
                title = chars.format(bibtex.getField("title"));
            } else {
                title = bibtex.getField("title");
            }
        }

        if (bibtex.hasField("publisher")) {
            if (CHARFORMAT) {
                publisher = chars.format(bibtex.getField("publisher"));
            } else {
                publisher = bibtex.getField("publisher");
            }
        }

        if (bibtex.hasField("bibtexkey")) {
            id = bibtex.getField("bibtexkey");
        }
        if (bibtex.hasField("place")) {
            if (CHARFORMAT) {
                place = chars.format(bibtex.getField("place"));
            } else {
                place = bibtex.getField("place");
            }
        }

        date = getDate(bibtex);
        genre = getMODSgenre(bibtex);
        if (bibtex.hasField("author")) {
            authors = getAuthors(bibtex.getField("author"));
        }
        if ((bibtex.getType().equals("article")) || (bibtex.getType().equals("inproceedings"))) {
            host = new MODSEntry();
            host.entryType = "relatedItem";
            host.title = bibtex.getField("booktitle");
            host.publisher = bibtex.getField("publisher");
            host.number = bibtex.getField("number");
            if (bibtex.hasField("volume")) {
                host.volume = bibtex.getField("volume");
            }
            host.issuance = "continuing";
            if (bibtex.hasField("pages")) {
                host.pages = new PageNumbers(bibtex.getField("pages"));
            }
        }

        populateExtensionFields(bibtex);

    }

    private void populateExtensionFields(BibEntry e) {

        for (String field : e.getFieldNames()) {
            String value = e.getField(field);
            extensionFields.put(MODSEntry.BIBTEX + field, value);
        }
    }

    private List<PersonName> getAuthors(String authors) {
        List<PersonName> result = new LinkedList<>();

        if (authors.contains(" and ")) {
            String[] names = authors.split(" and ");
            for (String name : names) {
                if (CHARFORMAT) {
                    result.add(new PersonName(chars.format(name)));
                } else {
                    result.add(new PersonName(name));
                }
            }
        } else {
            if (CHARFORMAT) {
                result.add(new PersonName(chars.format(authors)));
            } else {
                result.add(new PersonName(authors));
            }
        }
        return result;
    }

    /* construct a MODS date object */
    private static String getDate(BibEntry bibtex) {
        StringBuilder result = new StringBuilder();
        bibtex.getFieldOptional("year").ifPresent(year -> result.append(year));
        bibtex.getFieldOptional("month").ifPresent(month -> result.append('-').append(month));
        return result.toString();
    }

    // must be from http://www.loc.gov/marc/sourcecode/genre/genrelist.html
    private static String getMODSgenre(BibEntry bibtex) {
        /**
         * <pre> String result; if (bibtexType.equals("Mastersthesis")) result =
         * "theses"; else result = "conference publication"; // etc... </pre>
         */
        return bibtex.getType();
    }

    private Node getDOMrepresentation() {
        Node result = null;
        try {
            DocumentBuilder d = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            result = getDOMrepresentation(d.newDocument());
        } catch (Exception e) {
            throw new Error(e);
        }
        return result;
    }

    public Element getDOMrepresentation(Document d) {
        try {
            Element mods = d.createElement(entryType);
            mods.setAttribute("version", "3.0");
            // mods.setAttribute("xmlns:xlink:", "http://www.w3.org/1999/xlink");
            // title
            if (title != null) {
                Element titleInfo = d.createElement("titleInfo");
                Element mainTitle = d.createElement("title");
                mainTitle.appendChild(d.createTextNode(stripNonValidXMLCharacters(title)));
                titleInfo.appendChild(mainTitle);
                mods.appendChild(titleInfo);
            }
            if (authors != null) {
                for (PersonName name : authors) {
                    Element modsName = d.createElement("name");
                    modsName.setAttribute("type", "personal");
                    if (name.getSurname() != null) {
                        Element namePart = d.createElement("namePart");
                        namePart.setAttribute("type", "family");
                        namePart.appendChild(d.createTextNode(stripNonValidXMLCharacters(name.getSurname())));
                        modsName.appendChild(namePart);
                    }
                    if (name.getGivenNames() != null) {
                        Element namePart = d.createElement("namePart");
                        namePart.setAttribute("type", "given");
                        namePart.appendChild(d.createTextNode(stripNonValidXMLCharacters(name.getGivenNames())));
                        modsName.appendChild(namePart);
                    }
                    Element role = d.createElement("role");
                    Element roleTerm = d.createElement("roleTerm");
                    roleTerm.setAttribute("type", "text");
                    roleTerm.appendChild(d.createTextNode("author"));
                    role.appendChild(roleTerm);
                    modsName.appendChild(role);
                    mods.appendChild(modsName);
                }
            }
            //publisher
            Element originInfo = d.createElement("originInfo");
            mods.appendChild(originInfo);
            if (this.publisher != null) {
                Element publisher = d.createElement("publisher");
                publisher.appendChild(d.createTextNode(stripNonValidXMLCharacters(this.publisher)));
                originInfo.appendChild(publisher);
            }
            if (date != null) {
                Element dateIssued = d.createElement("dateIssued");
                dateIssued.appendChild(d.createTextNode(stripNonValidXMLCharacters(date)));
                originInfo.appendChild(dateIssued);
            }
            Element issuance = d.createElement("issuance");
            issuance.appendChild(d.createTextNode(stripNonValidXMLCharacters(this.issuance)));
            originInfo.appendChild(issuance);

            if (id != null) {
                Element idref = d.createElement("identifier");
                idref.appendChild(d.createTextNode(stripNonValidXMLCharacters(id)));
                mods.appendChild(idref);
                mods.setAttribute("ID", id);

            }
            Element typeOfResource = d.createElement("typeOfResource");
            String type = "text";
            typeOfResource.appendChild(d.createTextNode(stripNonValidXMLCharacters(type)));
            mods.appendChild(typeOfResource);

            if (genre != null) {
                Element genreElement = d.createElement("genre");
                genreElement.setAttribute("authority", "marc");
                genreElement.appendChild(d.createTextNode(stripNonValidXMLCharacters(genre)));
                mods.appendChild(genreElement);
            }

            if (host != null) {
                Element relatedItem = host.getDOMrepresentation(d);
                relatedItem.setAttribute("type", "host");
                mods.appendChild(relatedItem);
            }
            if (pages != null) {
                mods.appendChild(pages.getDOMrepresentation(d));
            }

            /* now generate extension fields for unhandled data */
            for (Map.Entry<String, String> theEntry : extensionFields.entrySet()) {
                Element extension = d.createElement("extension");
                String field = theEntry.getKey();
                String value = theEntry.getValue();
                if (handledExtensions.contains(field)) {
                    continue;
                }
                Element theData = d.createElement(field);
                theData.appendChild(d.createTextNode(stripNonValidXMLCharacters(value)));
                extension.appendChild(theData);
                mods.appendChild(extension);
            }
            return mods;
        } catch (Exception e)
        {
            LOGGER.warn("Exception caught...", e);
            throw new Error(e);
        }
        // return result;
    }

    /**
     * This method ensures that the output String has only
     * valid XML unicode characters as specified by the
     * XML 1.0 standard. For reference, please see
     * <a href="http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char">the
     * standard</a>. This method will return an empty
     * String if the input is null or empty.
     *
     * URL: http://cse-mjmcl.cse.bris.ac.uk/blog/2007/02/14/1171465494443.html
     *
     * @param in The String whose non-valid characters we want to remove.
     * @return The in String, stripped of non-valid characters.
     */
    private static String stripNonValidXMLCharacters(String in) {
        if (com.google.common.base.Strings.isNullOrEmpty(in)) {
            return ""; // vacancy test.
        }

        StringBuffer out = new StringBuffer(); // Used to hold the output.
        char current; // Used to reference the current character.

        for (int i = 0; i < in.length(); i++) {
            current = in.charAt(i); // NOTE: No IndexOutOfBoundsException caught here; it should not happen.
            if ((current == 0x9) || (current == 0xA) || (current == 0xD) || ((current >= 0x20) && (current <= 0xD7FF))
                    || ((current >= 0xE000) && (current <= 0xFFFD))) {
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
        } catch (Exception e) {
            throw new Error(e);
        }
        return sresult.toString();
    }

}
