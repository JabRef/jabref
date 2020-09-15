package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jabref.logic.importer.AuthorListParser;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
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

public class MarcParser implements Parser {
    private static final Logger LOGGER = LoggerFactory.getLogger(MarcParser.class);

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

        // Schleife ueber allen Teilergebnissen
        // Element root = content.getDocumentElement();
        Element root = (Element) content.getElementsByTagName("zs:searchRetrieveResponse").item(0);
        Element srwrecords = getChild("zs:records", root);
        if (srwrecords == null) {
            // no records found -> return empty list
            return result;
        }
        List<Element> records = getChildren("zs:record", srwrecords);
        for (Element record : records) {
            Element e = getChild("zs:recordData", record);
            if (e != null) {
                e = getChild("record", e);
                if (e != null) {
                    result.add(parseEntry(e));
                }
            }
        }
        return result;
    }

    private BibEntry parseEntry(Element e) {
        BibEntry bibEntry = new BibEntry(StandardEntryType.Misc);

        List<Element> datafields = getChildren("datafield", e);
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
                        LOGGER.debug("Malformed ISBN recieved, lenght: " + length);
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
                case "264" -> { // ind2 == 1 -> Publisher
                    String ind2 = datafield.getAttribute("ind2");
                    if (StringUtil.isNotBlank(ind2) && ind2.equals("1")) {
                        String place = getSubfield("a", datafield);
                        String name = getSubfield("b", datafield);
                        String date = getSubfield("c", datafield);

                        if (StringUtil.isNotBlank(place)) {
                            bibEntry.setField(StandardField.LOCATION, place);
                        }

                        if (StringUtil.isNotBlank(name)) {
                            String ind1 = datafield.getAttribute("ind1");
                            AuthorList parsedName = AuthorList.parse(name);
                            String brackedName;
                            if (StringUtil.isNotBlank(ind1) && ind1.equals("2")) {
                                // ind == 2 -> Corporate publisher
                                brackedName = "{" + parsedName.getAsFirstLastNamesWithAnd() + "}";
                            } else {
                                brackedName = parsedName.getAsLastFirstNamesWithAnd(false);
                            }

                            bibEntry.setField(StandardField.PUBLISHER, brackedName);
                        }

                        if (StringUtil.isNotBlank(date)) {
                            bibEntry.setField(StandardField.DATE, date);
                        }
                    }
                }
                case "490", "830" -> { // Series
                    String name = getSubfield("a", datafield);
                    String volume = getSubfield("v", datafield);

                    if (StringUtil.isNotBlank(name)) {
                        bibEntry.setField(StandardField.SERIES, name);
                    }

                    if (StringUtil.isNotBlank(volume)) {
                        bibEntry.setField(StandardField.VOLUME, volume);
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
                case "546" -> { // -- 599
                    // Notes
                }
                case "653" -> { // "a" - keywords
                    String keyword = getSubfield("a", datafield);

                    Optional<String> keywords = bibEntry.getField(StandardField.KEYWORDS);
                    if (keywords.isPresent()) {
                        bibEntry.setField(StandardField.KEYWORDS, keywords.get() + ", " + keyword);
                    } else {
                        bibEntry.setField(StandardField.KEYWORDS, keyword);
                    }
                }
                case "856" -> { // electronic location (ind1==4, ind==0)
                    // "u" - url resource
                    // ind2 = related

                    // ind2="4" ind2="0" subfield3="Volltext"
                }
                case "966" -> {
                    // "u" -  doi (ind1==e)
                }
                // 966 ind1=e
                //  subfield u url
                //  subfield 3 "Volltext"
            }
        }

        // summary

            /*
            // mak
            if ("002@".equals(tag)) {
                mak = getSubfield("0", datafield);
                if (mak == null) {
                    mak = "";
                }
            }

            // ppn
            if ("003@".equals(tag)) {
                ppn = getSubfield("0", datafield);
            }

            // year, volume, number, pages (year bei Zeitschriften (evtl. redundant mit 011@))
            if ("031A".equals(tag)) {
                year = getSubfield("j", datafield);

                volume = getSubfield("e", datafield);
                number = getSubfield("a", datafield);
                pages = getSubfield("h", datafield);
            }

            // series and number
            if ("036E".equals(tag)) {
                series = getSubfield("a", datafield);
                number = getSubfield("l", datafield);
                String kor = getSubfield("b", datafield);

                if (kor != null) {
                    series = series + " / " + kor;
                }
            }

            // note
            if ("037A".equals(tag)) {
                note = getSubfield("a", datafield);
            }

            // Hochschulschriftenvermerk
            // Bei einer Verlagsdissertation ist der Ort schon eingetragen
            if ("037C".equals(tag)) {
                if (address == null) {
                    address = getSubfield("b", datafield);
                    if (address != null) {
                        address = removeSortCharacters(address);
                    }
                }

                String st = getSubfield("a", datafield);
                if ((st != null) && st.contains("Diss")) {
                    entryType = StandardEntryType.PhdThesis;
                }
            }

            // journal oder booktitle

            // Problematiken hier: Sowohl für Artikel in Zeitschriften als für Beiträge in Büchern wird 027D verwendet.
            // Der Titel muß je nach Fall booktitle oder journal zugeordnet werden. Auch bei Zeitschriften werden hier
            // ggf. Verlag und Ort angegeben (sind dann eigentlich überflüssig), während bei  Buchbeiträgen Verlag und
            // Ort wichtig sind (sonst in Kategorie 033A).

            if ("027D".equals(tag)) {
                journal = getSubfield("a", datafield);
                booktitle = getSubfield("a", datafield);
                address = getSubfield("p", datafield);
                publisher = getSubfield("n", datafield);
            }

            // pagetotal
            if ("034D".equals(tag)) {
                pagetotal = getSubfield("a", datafield);

                if (pagetotal != null) {
                    // S, S. etc. entfernen
                    pagetotal = pagetotal.replaceAll(" S\\.?$", "");
                }
            }

            // Behandlung von Konferenzen
            if ("030F".equals(tag)) {
                address = getSubfield("k", datafield);

                if (!"proceedings".equals(entryType)) {
                    subtitle = getSubfield("a", datafield);
                }

                entryType = StandardEntryType.Proceedings;
            }

            // Wenn eine Verlagsdiss vorliegt
            if (entryType.equals(StandardEntryType.PhdThesis) && (isbn != null)) {
                entryType = StandardEntryType.Book;
            }

            // Hilfskategorien zur Entscheidung @article
            // oder @incollection; hier könnte man auch die
            // ISBN herausparsen als Erleichterung für das
            // Auffinden der Quelle, die über die
            // SRU-Schnittstelle gelieferten Daten zur
            // Quelle unvollständig sind (z.B. nicht Serie
            // und Nummer angegeben werden)
            if ("039B".equals(tag)) {
                quelle = getSubfield("8", datafield);
            }
            if ("046R".equals(tag) && ((quelle == null) || quelle.isEmpty())) {
                quelle = getSubfield("a", datafield);
            }

            // URLs behandeln
            if ("009P".equals(tag) && ("03".equals(datafield.getAttribute("occurrence"))
                    || "05".equals(datafield.getAttribute("occurrence"))) && (url == null)) {
                url = getSubfield("a", datafield);
            }
        }

        // Dokumenttyp bestimmen und Eintrag anlegen

        if (mak.startsWith("As")) {
            entryType = BibEntry.DEFAULT_TYPE;

            if (quelle.contains("ISBN")) {
                entryType = StandardEntryType.InCollection;
            }
            if (quelle.contains("ZDB-ID")) {
                entryType = StandardEntryType.Article;
            }
        } else if (mak.isEmpty()) {
            entryType = BibEntry.DEFAULT_TYPE;
        } else if (mak.startsWith("O")) {
            entryType = BibEntry.DEFAULT_TYPE;
            // entryType = "online";
        }

        // Wahrscheinlichkeit, dass ZDB-ID vorhanden ist, ist größer als ISBN bei Buchbeiträgen. Daher bei As?-Sätzen am
        // besten immer dann @incollection annehmen, wenn weder ISBN noch ZDB-ID vorhanden sind.
        BibEntry result = new BibEntry(entryType);

        // Zuordnung der Felder in Abhängigkeit vom Dokumenttyp
        if (author != null) {
            result.setField(StandardField.AUTHOR, author);
        }
        if (editor != null) {
            result.setField(StandardField.EDITOR, editor);
        }
        if (title != null) {
            result.setField(StandardField.TITLE, title);
        }
        if (!Strings.isNullOrEmpty(subtitle)) {
            // ensure that first letter is an upper case letter
            // there could be the edge case that the string is only one character long, therefore, this special treatment
            // this is Apache commons lang StringUtils.capitalize (https://commons.apache.org/proper/commons-lang/javadocs/api-release/org/apache/commons/lang3/StringUtils.html#capitalize%28java.lang.String%29), but we don't want to add an additional dependency  ('org.apache.commons:commons-lang3:3.4')
            StringBuilder newSubtitle = new StringBuilder(
                    Character.toString(Character.toUpperCase(subtitle.charAt(0))));
            if (subtitle.length() > 1) {
                newSubtitle.append(subtitle.substring(1));
            }
            result.setField(StandardField.SUBTITLE, newSubtitle.toString());
        }
        if (publisher != null) {
            result.setField(StandardField.PUBLISHER, publisher);
        }
        if (year != null) {
            result.setField(StandardField.YEAR, year);
        }
        if (address != null) {
            result.setField(StandardField.ADDRESS, address);
        }
        if (series != null) {
            result.setField(StandardField.SERIES, series);
        }
        if (edition != null) {
            result.setField(StandardField.EDITION, edition);
        }
        if (isbn != null) {
            result.setField(StandardField.ISBN, isbn);
        }
        if (issn != null) {
            result.setField(StandardField.ISSN, issn);
        }
        if (number != null) {
            result.setField(StandardField.NUMBER, number);
        }
        if (pagetotal != null) {
            result.setField(StandardField.PAGETOTAL, pagetotal);
        }
        if (pages != null) {
            result.setField(StandardField.PAGES, pages);
        }
        if (volume != null) {
            result.setField(StandardField.VOLUME, volume);
        }
        if (journal != null) {
            result.setField(StandardField.JOURNAL, journal);
        }
        if (ppn != null) {
            result.setField(new UnknownField("ppn_GVK"), ppn);
        }
        if (url != null) {
            result.setField(StandardField.URL, url);
        }
        if (note != null) {
            result.setField(StandardField.NOTE, note);
        }

        if ("article".equals(entryType) && (journal != null)) {
            result.setField(StandardField.JOURNAL, journal);
        } else if ("incollection".equals(entryType) && (booktitle != null)) {
            result.setField(StandardField.BOOKTITLE, booktitle);
        } */

        return bibEntry;
    }

    private String getSubfield(String a, Element datafield) {
        List<Element> liste = getChildren("subfield", datafield);

        for (Element subfield : liste) {
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
