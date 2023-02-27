package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.DateTimeException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jabref.logic.importer.AuthorListParser;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Date;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A parser for the bavarian flavour (Bibliotheksverbund Bayern) of the marc xml standard
 * <p>
 * See <a href="https://www.dnb.de/DE/Professionell/Metadatendienste/Exportformate/MARC21/marc21_node.html">Feldbeschreibung
 * der Titeldaten bei der Deutschen Nationalbibliothek</a>
 * <p>
 *
 * <p>
 * For further information see
 * <ul>
 * <li>https://www.bib-bvb.de/web/kkb-online/rda-felderverzeichnis-des-b3kat-aseq</li>
 * <li>https://www.loc.gov/marc/bibliographic/ for detailed documentation</li>
 * <li>for modifications in B3Kat https://www.bib-bvb.de/documents/10792/9f51a033-5ca1-42e2-b2d3-a75e7f1512d4</li>
 * <li>https://www.dnb.de/DE/Professionell/Metadatendienste/Exportformate/MARC21/marc21_node.html</li>
 * <li>https://www.dnb.de/SharedDocs/Downloads/DE/Professionell/Standardisierung/AGV/marc21VereinbarungDatentauschTeil1.pdf?__blob=publicationFile&v=2</li>
 * <li>about multiple books in a series https://www.dnb.de/SharedDocs/Downloads/DE/Professionell/Standardisierung/marc21FormatumstiegAbbildungBegrenzterWerke2008.pdf?__blob=publicationFile&v=2></li>
 * </ul>
 */
public class MarcXmlParser implements Parser {
    private static final Logger LOGGER = LoggerFactory.getLogger(MarcXmlParser.class);

    @Override
    public List<BibEntry> parseEntries(InputStream inputStream) throws ParseException {
        try {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document content = documentBuilder.parse(inputStream);
            return this.parseEntries(content);
        } catch (ParserConfigurationException | SAXException | IOException exception) {
            throw new ParseException(exception);
        }
    }

    private List<BibEntry> parseEntries(Document content) {
        List<BibEntry> result = new LinkedList<>();

        Element root = (Element) content.getElementsByTagName("zs:searchRetrieveResponse").item(0);
        Element srwrecords = getChild("zs:records", root);
        if (srwrecords == null) {
            // no records found, so return the empty list
            return result;
        }
        List<Element> records = getChildren("zs:record", srwrecords);
        for (Element element : records) {
            Element e = getChild("zs:recordData", element);
            if (e != null) {
                e = getChild("record", e);
                if (e != null) {
                    result.add(parseEntry(e));
                }
            }
        }
        return result;
    }

    private BibEntry parseEntry(Element element) {
        BibEntry bibEntry = new BibEntry(BibEntry.DEFAULT_TYPE);

        List<Element> datafields = getChildren("datafield", element);
        for (Element datafield : datafields) {
            String tag = datafield.getAttribute("tag");
            LOGGER.debug("tag: " + tag);

            if (tag.equals("020")) {
                putIsbn(bibEntry, datafield);
            } else if (tag.equals("100") || tag.equals("700") || tag.equals("710")) {
                putPersonalName(bibEntry, datafield); // Author, Editor, Publisher
            } else if (tag.equals("245")) {
                putTitle(bibEntry, datafield);
            } else if (tag.equals("250")) {
                putEdition(bibEntry, datafield);
            } else if (tag.equals("264")) {
                putPublication(bibEntry, datafield);
            } else if (tag.equals("300")) {
                putPhysicalDescription(bibEntry, datafield);
            } else if (tag.equals("490") || tag.equals("830")) {
                putSeries(bibEntry, datafield);
            } else if (tag.equals("520")) {
                putSummary(bibEntry, datafield);
            } else if (tag.equals("653")) {
                putKeywords(bibEntry, datafield);
            } else if (tag.equals("856")) {
                putElectronicLocation(bibEntry, datafield);
            } else if (tag.equals("966")) {
                putDoi(bibEntry, datafield);
            } else if (Integer.parseInt(tag) >= 546 && Integer.parseInt(tag) <= 599) {
                // Notes
                // FixMe: Some notes seem to have tags lower than 546
                putNotes(bibEntry, datafield);
            } else {
                LOGGER.debug("Unparsed tag: {}", tag);
            }
        }

        /*
         * ToDo:
         *  pages
         *  volume and number correct
         *  series and journals stored in different tags
         *  thesis
         *  proceedings
         */

        return bibEntry;
    }

    private void putIsbn(BibEntry bibEntry, Element datafield) {
        String isbn = getSubfield("a", datafield);
        if (StringUtil.isNullOrEmpty(isbn)) {
            LOGGER.debug("Empty ISBN recieved");
            return;
        }

        int length = isbn.length();
        if (length != 10 && length != 13) {
            LOGGER.debug("Malformed ISBN recieved, length: " + length);
            return;
        }

        Optional<String> field = bibEntry.getField(StandardField.ISBN);
        if (field.isPresent()) {
            // Only overwrite the field, if it's ISBN13
            if (field.get().length() == 13) {
                bibEntry.setField(StandardField.ISBN, isbn);
            }
        } else {
            bibEntry.setField(StandardField.ISBN, isbn);
        }
    }

    private void putPersonalName(BibEntry bibEntry, Element datafield) {
        String author = getSubfield("a", datafield);
        String relation = getSubfield("4", datafield);
        AuthorList name;

        if (StringUtil.isNotBlank(author) && StringUtil.isNotBlank(relation)) {
            name = new AuthorListParser().parse(author);
            Optional<StandardField> field = Optional.ofNullable(
                    switch (relation) {
                        case "aut" ->
                                StandardField.AUTHOR;
                        case "edt" ->
                                StandardField.EDITOR;
                        case "pbl" ->
                                StandardField.PUBLISHER;
                        default ->
                                null;
                    });

            if (field.isPresent()) {
                String ind1 = datafield.getAttribute("ind1");
                String brackedName;
                if (field.get() == StandardField.PUBLISHER && StringUtil.isNotBlank(ind1) && ind1.equals("2")) {
                    // ind == 2 -> Corporate publisher
                    brackedName = "{" + name.getAsFirstLastNamesWithAnd() + "}";
                } else {
                    brackedName = name.getAsLastFirstNamesWithAnd(false);
                }

                if (bibEntry.getField(field.get()).isPresent()) {
                    bibEntry.setField(field.get(), bibEntry.getField(field.get()).get().concat(" and " + brackedName));
                } else {
                    bibEntry.setField(field.get(), brackedName);
                }
            }
        }
    }

    private void putTitle(BibEntry bibEntry, Element datafield) {
        String title = getSubfield("a", datafield);
        String subtitle = getSubfield("b", datafield);
        String responsibility = getSubfield("c", datafield);
        String number = getSubfield("n", datafield);
        String part = getSubfield("p", datafield);

        if (StringUtil.isNotBlank(title)) {
            bibEntry.setField(StandardField.TITLE, title);
        }

        if (StringUtil.isNotBlank(subtitle)) {
            bibEntry.setField(StandardField.SUBTITLE, subtitle);
        }

        if (StringUtil.isNotBlank(responsibility)) {
            bibEntry.setField(StandardField.TITLEADDON, responsibility);
        }

        if (StringUtil.isNotBlank(number)) {
            bibEntry.setField(StandardField.NUMBER, number);
        }

        if (StringUtil.isNotBlank(part)) {
            bibEntry.setField(StandardField.PART, part);
        }
    }

    private void putEdition(BibEntry bibEntry, Element datafield) {
        String edition = getSubfield("a", datafield); // e.g. '1st ed. 2020'
        String editionAddendum = getSubfield("b", datafield); // e.g. 'revised by N.N.'

        if (StringUtil.isNullOrEmpty(edition)) {
            return;
        }

        if (StringUtil.isNotBlank(editionAddendum)) {
            edition = edition.concat(", " + editionAddendum);
        }

        bibEntry.setField(StandardField.EDITION, edition);
    }

    private void putPublication(BibEntry bibEntry, Element datafield) {
        String ind2 = datafield.getAttribute("ind2");
        if (StringUtil.isNotBlank(ind2) && ind2.equals("1")) { // Publisher
            String place = getSubfield("a", datafield);
            String name = getSubfield("b", datafield);
            String date = getSubfield("c", datafield);

            if (StringUtil.isNotBlank(place)) {
                bibEntry.setField(StandardField.LOCATION, place);
            }

            if (StringUtil.isNotBlank(name)) {
                bibEntry.setField(StandardField.PUBLISHER, "{" + name + "}");
            }

            if (StringUtil.isNotBlank(date)) {
                String strippedDate = StringUtil.stripBrackets(date);
                try {
                    Date.parse(strippedDate).ifPresent(bibEntry::setDate);
                } catch (DateTimeException exception) {
                    // cannot read date value, just copy it in plain text
                    LOGGER.info("Cannot parse date '{}'", strippedDate);
                    bibEntry.setField(StandardField.DATE, StringUtil.stripBrackets(strippedDate));
                }
            }
        }
    }

    private void putPhysicalDescription(BibEntry bibEntry, Element datafield) {
        String pagetotal = getSubfield("a", datafield);

        if (StringUtil.isNotBlank(pagetotal) && (pagetotal.contains("pages") || pagetotal.contains("p."))) {
            pagetotal = pagetotal.replaceAll(" p\\.?$", "");
            bibEntry.setField(StandardField.PAGETOTAL, pagetotal);
        }
    }

    private void putSeries(BibEntry bibEntry, Element datafield) {
        // tag 490 - Series
        // tag 830 - Series Added Entry

        String name = getSubfield("a", datafield);
        String volume = getSubfield("v", datafield);
        String issn = getSubfield("x", datafield);

        if (StringUtil.isNotBlank(name)) {
            bibEntry.setField(StandardField.SERIES, name);
        }

        if (StringUtil.isNotBlank(volume)) {
            bibEntry.setField(StandardField.VOLUME, volume);
        }

        if (StringUtil.isNotBlank(issn)) {
            bibEntry.setField(StandardField.ISSN, issn);
        }
    }

    private void putSummary(BibEntry bibEntry, Element datafield) {
        String summary = getSubfield("a", datafield);

        String ind1 = datafield.getAttribute("ind1");
        if (StringUtil.isNotBlank(summary) && StringUtil.isNotBlank(ind1) && ind1.equals("3")) { // Abstract
            if (bibEntry.getField(StandardField.ABSTRACT).isPresent()) {
                bibEntry.setField(StandardField.ABSTRACT, bibEntry.getField(StandardField.ABSTRACT).get().concat(summary));
            } else {
                bibEntry.setField(StandardField.ABSTRACT, summary);
            }
        }
    }

    private void putKeywords(BibEntry bibEntry, Element datafield) {
        String keyword = getSubfield("a", datafield);

        if (StringUtil.isNotBlank(keyword)) {
            Optional<String> keywords = bibEntry.getField(StandardField.KEYWORDS);
            if (keywords.isPresent()) {
                bibEntry.setField(StandardField.KEYWORDS, keywords.get() + ", " + keyword);
            } else {
                bibEntry.setField(StandardField.KEYWORDS, keyword);
            }
        }
    }

    private void putDoi(BibEntry bibEntry, Element datafield) {
        String ind1 = datafield.getAttribute("ind1");
        String resource = getSubfield("u", datafield);

        if ("e".equals(ind1) && StringUtil.isNotBlank("u") && StringUtil.isNotBlank(resource)) { // DOI
            String fulltext = getSubfield("3", datafield);

            if ("Volltext".equals(fulltext)) {
                try {
                    LinkedFile linkedFile = new LinkedFile(new URL(resource), "PDF");
                    bibEntry.setField(StandardField.FILE, linkedFile.toString());
                } catch (
                        MalformedURLException e) {
                    LOGGER.info("Malformed URL: {}", resource);
                }
            } else {
                bibEntry.setField(StandardField.DOI, resource);
            }
        }
    }

    private void putElectronicLocation(BibEntry bibEntry, Element datafield) {
        // 856 - fulltext pdf url
        String ind1 = datafield.getAttribute("ind1");
        String ind2 = datafield.getAttribute("ind2");

        if ("4".equals(ind1) && "0".equals(ind2)) {
            String fulltext = getSubfield("3", datafield);
            String resource = getSubfield("u", datafield);

            if ("Volltext".equals(fulltext) && StringUtil.isNotBlank(resource)) {
                try {
                    LinkedFile linkedFile = new LinkedFile(new URL(resource), "PDF");
                    bibEntry.setField(StandardField.FILE, linkedFile.toString());
                } catch (
                        MalformedURLException e) {
                    LOGGER.info("Malformed URL: {}", resource);
                }
            } else {
                bibEntry.setField(StandardField.URL, resource);
            }
        }
    }

    private void putNotes(BibEntry bibEntry, Element datafield) {
        String[] notes = new String[] {
                getSubfield("a", datafield),
                getSubfield("0", datafield),
                getSubfield("h", datafield),
                getSubfield("S", datafield),
                getSubfield("c", datafield),
                getSubfield("f", datafield),
                getSubfield("i", datafield),
                getSubfield("k", datafield),
                getSubfield("l", datafield),
                getSubfield("z", datafield),
                getSubfield("3", datafield),
                getSubfield("5", datafield)
        };

        String notesJoined = Arrays.stream(notes)
                                   .filter(StringUtil::isNotBlank)
                                   .collect(Collectors.joining("\n\n"));

        if (bibEntry.getField(StandardField.NOTE).isPresent()) {
            bibEntry.setField(StandardField.NOTE, bibEntry.getField(StandardField.NOTE).get().concat(notesJoined));
        } else {
            bibEntry.setField(StandardField.NOTE, notesJoined);
        }
    }

    private String getSubfield(String a, Element datafield) {
        List<Element> subfields = getChildren("subfield", datafield);

        for (Element subfield : subfields) {
            if (subfield.getAttribute("code").equals(a)) {
                return (subfield.getTextContent());
            }
        }
        return null;
    }

    private Element getChild(String name, Element e) {
        if (e == null) {
            return null;
        }
        NodeList children = e.getChildNodes();

        int j = children.getLength();
        for (int i = 0; i < j; i++) {
            Node test = children.item(i);
            if (test.getNodeType() == Node.ELEMENT_NODE) {
                Element entry = (Element) test;
                if (entry.getTagName().equals(name)) {
                    return entry;
                }
            }
        }
        return null;
    }

    private List<Element> getChildren(String name, Element e) {
        List<Element> result = new LinkedList<>();
        NodeList children = e.getChildNodes();

        int j = children.getLength();
        for (int i = 0; i < j; i++) {
            Node test = children.item(i);
            if (test.getNodeType() == Node.ELEMENT_NODE) {
                Element entry = (Element) test;
                if (entry.getTagName().equals(name)) {
                    result.add(entry);
                }
            }
        }

        return result;
    }
}
