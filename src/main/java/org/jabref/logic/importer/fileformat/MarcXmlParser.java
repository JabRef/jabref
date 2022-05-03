package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
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
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
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
 *
 * https://www.bib-bvb.de/web/kkb-online/rda-felderverzeichnis-des-b3kat-aseq
 *
 * For further information see
 * https://www.loc.gov/marc/bibliographic/ for detailed documentation
 * for modifications in B3Kat https://www.bib-bvb.de/documents/10792/9f51a033-5ca1-42e2-b2d3-a75e7f1512d4
 * https://www.dnb.de/DE/Professionell/Metadatendienste/Exportformate/MARC21/marc21_node.html
 * https://www.dnb.de/SharedDocs/Downloads/DE/Professionell/Standardisierung/AGV/marc21VereinbarungDatentauschTeil1.pdf?__blob=publicationFile&v=2
 * about multiple books in a series https://www.dnb.de/SharedDocs/Downloads/DE/Professionell/Standardisierung/marc21FormatumstiegAbbildungBegrenzterWerke2008.pdf?__blob=publicationFile&v=2
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

            switch (tag) {
                case "020" -> { // ISBN
                    String isbn = getSubfield("a", datafield);
                    if (StringUtil.isNullOrEmpty(isbn)) {
                        LOGGER.debug("Empty ISBN recieved");
                        break;
                    }

                    int length = isbn.length();
                    if (length != 10 && length != 13) {
                        LOGGER.debug("Malformed ISBN recieved, length: " + length);
                        break;
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
                case "100", "700", "710" -> { // Author, Editor, Publisher
                    String author = getSubfield("a", datafield);
                    String relation = getSubfield("4", datafield);
                    AuthorList name;

                    if (StringUtil.isNotBlank(author) && StringUtil.isNotBlank(relation)) {
                        name = new AuthorListParser().parse(author);
                        Optional<StandardField> field = Optional.ofNullable(
                                switch (relation) {
                                    case "aut" -> StandardField.AUTHOR;
                                    case "edt" -> StandardField.EDITOR;
                                    case "pbl" -> StandardField.PUBLISHER;
                                    default -> null;
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
                case "245" -> { // Title, Subtitle
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
                case "250" -> { // Edition
                    String edition = getSubfield("a", datafield); // e.g. '1st ed. 2020'
                    String editionaddendum = getSubfield("b", datafield); // e.g. 'revised by N.N.'

                    if (StringUtil.isNullOrEmpty(edition)) {
                        break;
                    }

                    if (StringUtil.isNotBlank(editionaddendum)) {
                        edition = edition.concat(", " + editionaddendum);
                    }

                    bibEntry.setField(StandardField.EDITION, edition);
                }
                case "264" -> {
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
                            bibEntry.setField(StandardField.DATE, date);
                        }
                    }
                }
                case "300" -> {
                    String pagetotal = getSubfield("a", datafield);

                    if (StringUtil.isNotBlank(pagetotal) && (pagetotal.contains("pages") || pagetotal.contains("p."))) {
                        pagetotal = pagetotal.replaceAll(" p\\.?$", "");
                        bibEntry.setField(StandardField.PAGETOTAL, pagetotal);
                    }
                }
                case "490", "830" -> { // Series
                    String name = getSubfield("a", datafield);
                    String volume = getSubfield("v", datafield);
                    String issn = getSubfield("x", datafield);

                    if (StringUtil.isNotBlank(name)) {
                        bibEntry.setField(StandardField.SERIES, name);

                        bibEntry.setType(StandardEntryType.Article);
                    }

                    if (StringUtil.isNotBlank(volume)) {
                        bibEntry.setField(StandardField.VOLUME, volume);
                    }

                    if (StringUtil.isNotBlank(issn)) {
                        bibEntry.setField(StandardField.ISSN, issn);
                    }
                }
                case "520" -> { // Summary
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
                case "653" -> { // Keywords
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
                case "856" -> {
                    String ind1 = datafield.getAttribute("ind1");
                    String ind2 = datafield.getAttribute("ind2");

                    if ("4".equals(ind1) && "0".equals(ind2)) {
                        String fulltext = getSubfield("3", datafield);
                        String resource = getSubfield("u", datafield);

                        if ("Volltext".equals(fulltext) && StringUtil.isNotBlank(resource)) {
                            try {
                                LinkedFile linkedFile = new LinkedFile(new URL(resource), "PDF");
                                bibEntry.setField(StandardField.FILE, linkedFile.toString());
                            } catch (MalformedURLException e) {
                                LOGGER.info("Malformed URL: {}", resource);
                            }
                        } else {
                            bibEntry.setField(StandardField.URL, resource);
                        }
                    }
                }
                case "966" -> {
                    String ind1 = datafield.getAttribute("ind1");
                    String resource = getSubfield("u", datafield);

                    if ("e".equals(ind1) && StringUtil.isNotBlank("u") && StringUtil.isNotBlank(resource)) { // DOI
                        String fulltext = getSubfield("3", datafield);

                        if ("Volltext".equals(fulltext)) {
                            try {
                                LinkedFile linkedFile = new LinkedFile(new URL(resource), "PDF");
                                bibEntry.setField(StandardField.FILE, linkedFile.toString());
                            } catch (MalformedURLException e) {
                                LOGGER.info("Malformed URL: {}", resource);
                            }
                        } else {
                            bibEntry.setField(StandardField.DOI, resource);
                        }
                    }
                }
                default -> {
                    int tagNumber = Integer.parseInt(tag);

                    if (tagNumber >= 546 && tagNumber <= 599) { // notes
                        // FixMe: Some notes seem to have tags lower than 546

                        String[] notes = new String[]{
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
                    } else {
                        LOGGER.debug("Unparsed tag: {}", tag);
                    }
                }
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
