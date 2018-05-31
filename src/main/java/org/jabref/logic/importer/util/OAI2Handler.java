package org.jabref.logic.importer.util;

import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

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

    private StringBuilder authors;

    private String keyname;

    private String forenames;

    private StringBuilder characters;


    public OAI2Handler(BibEntry be) {
        this.entry = be;
    }

    @Override
    public void startDocument() throws SAXException {
        authors = new StringBuilder();
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        characters.append(ch, start, length);
    }

    @Override
    public void startElement(String uri, String localName, String qualifiedName,
            Attributes attributes) throws SAXException {

        characters = new StringBuilder();
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
            Optional<String> year = entry.getField(FieldName.YEAR);
            if (!year.isPresent() || year.get().isEmpty()) {
                entry.setField(FieldName.YEAR, content.replaceFirst("-.*", ""));
            }
        } else if ("title".equals(qualifiedName)) {
            entry.setField(FieldName.TITLE, content);
        } else if ("abstract".equals(qualifiedName)) {
            entry.setField(FieldName.ABSTRACT, content);
        } else if ("comments".equals(qualifiedName)) {
            entry.setField(FieldName.COMMENT, content);
        } else if ("report-no".equals(qualifiedName)) {
            entry.setField(FieldName.REPORTNO, content);
        } else if ("doi".equals(qualifiedName)) {
          entry.setField(FieldName.DOI, content);
        } else if ("author".equals(qualifiedName)) {
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

    public static String correctLineBreaks(String s) {
        String result = s.replaceAll("\\n(?!\\s*\\n)", " ");
        result = result.replaceAll("\\s*\\n\\s*", "\n");
        return result.replaceAll(" {2,}", " ").replaceAll("(^\\s*|\\s+$)", "");
    }

}
