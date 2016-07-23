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
package net.sf.jabref.importer;

import java.util.Optional;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX-Handler to parse OAI2-xml files.
 *
 * @author Ulrich St&auml;rk
 * @author Christian Kopf
 * @author Christopher Oezbek
 */
public class OAI2Handler extends DefaultHandler {

    private final BibEntry entry;

    private StringBuffer authors;

    private String keyname;

    private String forenames;

    private StringBuffer characters;


    public OAI2Handler(BibEntry be) {
        this.entry = be;
    }

    @Override
    public void startDocument() throws SAXException {
        authors = new StringBuffer();
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        characters.append(ch, start, length);
    }

    @Override
    public void startElement(String uri, String localName, String qualifiedName,
            Attributes attributes) throws SAXException {

        characters = new StringBuffer();
    }

    @Override
    public void endElement(String uri, String localName, String qualifiedName) throws SAXException {

        String content = characters.toString();

        if ("error".equals(qualifiedName)) {
            throw new RuntimeException(content);
        } else if ("id".equals(qualifiedName)) {
            entry.setField(FieldName.EPRINT, content);
        } else if ("keyname".equals(qualifiedName)) {
            keyname = content;
        } else if ("forenames".equals(qualifiedName)) {
            forenames = content;
        } else if ("journal-ref".equals(qualifiedName)) {
            String journal = content.replaceFirst("[0-9].*", "");
            entry.setField(FieldName.JOURNAL, journal);
            String volume = content.replaceFirst(journal, "");
            volume = volume.replaceFirst(" .*", "");
            entry.setField(FieldName.VOLUME, volume);
            String year = content.replaceFirst(".*?\\(", "");
            year = year.replaceFirst("\\).*", "");
            entry.setField(FieldName.YEAR, year);
            String pages = content.replaceFirst(journal, "");
            pages = pages.replaceFirst(volume, "");
            pages = pages.replaceFirst("\\(" + year + "\\)", "");
            pages = pages.replace(" ", "");
            entry.setField(FieldName.PAGES, pages);
        } else if ("datestamp".equals(qualifiedName)) {
            Optional<String> year = entry.getFieldOptional(FieldName.YEAR);
            if (!year.isPresent() || year.get().isEmpty()) {
                entry.setField(FieldName.YEAR, content.replaceFirst("-.*", ""));
            }
        } else if (FieldName.TITLE.equals(qualifiedName)) {
            entry.setField(FieldName.TITLE, content);
        } else if ("abstract".equals(qualifiedName)) {
            entry.setField(FieldName.ABSTRACT, content);
        } else if ("comments".equals(qualifiedName)) {
            entry.setField("comments", content);
        } else if ("report-no".equals(qualifiedName)) {
            entry.setField("reportno", content);
        } else if(FieldName.DOI.equals(qualifiedName)) {
          entry.setField(FieldName.DOI, content);
        } else if (FieldName.AUTHOR.equals(qualifiedName)) {
            String author = forenames + " " + keyname;
            if (authors.length() > 0) {
                authors.append(" and ");
            }
            authors.append(author);
        }
    }

    @Override
    public void endDocument() throws SAXException {
        entry.setField(FieldName.AUTHOR, authors.toString());
    }

}
