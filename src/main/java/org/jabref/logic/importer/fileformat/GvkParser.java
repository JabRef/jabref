package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class GvkParser implements Parser {
    private static final Logger LOGGER = LoggerFactory.getLogger(GvkParser.class);

    @Override
    public List<BibEntry> parseEntries(InputStream inputStream) throws ParseException {
        try {
            DocumentBuilder dbuild = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document content = dbuild.parse(inputStream);
            return this.parseEntries(content);
        } catch (ParserConfigurationException | SAXException | IOException exception) {
            throw new ParseException(exception);
        }
    }

    private List<BibEntry> parseEntries(Document content) {
        List<BibEntry> result = new LinkedList<>();

        // used for creating test cases
        // XMLUtil.printDocument(content);

        // Namespace srwNamespace = Namespace.getNamespace("srw","http://www.loc.gov/zing/srw/");

        // Schleife ueber allen Teilergebnissen
        //Element root = content.getDocumentElement();
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
        String author = null;
        String editor = null;
        String title = null;
        String publisher = null;
        String year = null;
        String address = null;
        String series = null;
        String edition = null;
        String isbn = null;
        String issn = null;
        String number = null;
        String pagetotal = null;
        String volume = null;
        String pages = null;
        String journal = null;
        String ppn = null;
        String booktitle = null;
        String url = null;
        String note = null;

        String quelle = "";
        String mak = "";
        String subtitle = "";

        String entryType = "book"; // Default

        // Alle relevanten Informationen einsammeln

        List<Element> datafields = getChildren("datafield", e);
        for (Element datafield : datafields) {
            String tag = datafield.getAttribute("tag");
            LOGGER.debug("tag: " + tag);

            // mak
            if ("002@".equals(tag)) {
                mak = getSubfield("0", datafield);
                if (mak == null) {
                    mak = "";
                }
            }

            //ppn
            if ("003@".equals(tag)) {
                ppn = getSubfield("0", datafield);
            }

            //author
            if ("028A".equals(tag)) {
                String vorname = getSubfield("d", datafield);
                String nachname = getSubfield("a", datafield);

                if (author == null) {
                    author = "";
                } else {
                    author = author.concat(" and ");
                }
                author = author.concat(vorname + " " + nachname);
            }
            //author (weiterer)
            if ("028B".equals(tag)) {
                String vorname = getSubfield("d", datafield);
                String nachname = getSubfield("a", datafield);

                if (author == null) {
                    author = "";
                } else {
                    author = author.concat(" and ");
                }
                author = author.concat(vorname + " " + nachname);
            }

            //editor
            if ("028C".equals(tag)) {
                String vorname = getSubfield("d", datafield);
                String nachname = getSubfield("a", datafield);

                if (editor == null) {
                    editor = "";
                } else {
                    editor = editor.concat(" and ");
                }
                editor = editor.concat(vorname + " " + nachname);
            }

            //title and subtitle
            if ("021A".equals(tag)) {
                title = getSubfield("a", datafield);
                subtitle = getSubfield("d", datafield);
            }

            //publisher and address
            if ("033A".equals(tag)) {
                publisher = getSubfield("n", datafield);
                address = getSubfield("p", datafield);
            }

            //year
            if ("011@".equals(tag)) {
                year = getSubfield("a", datafield);
            }

            //year, volume, number, pages (year bei Zeitschriften (evtl. redundant mit 011@))
            if ("031A".equals(tag)) {
                year = getSubfield("j", datafield);

                volume = getSubfield("e", datafield);
                number = getSubfield("a", datafield);
                pages = getSubfield("h", datafield);

            }

            // 036D seems to contain more information than the other fields
            // overwrite information using that field
            // 036D also contains information normally found in 036E
            if ("036D".equals(tag)) {
                // 021 might have been present
                if (title != null) {
                    // convert old title (contained in "a" of 021A) to volume
                    if (title.startsWith("@")) {
                        // "@" indicates a number
                        title = title.substring(1);
                    }
                    number = title;
                }
                //title and subtitle
                title = getSubfield("a", datafield);
                subtitle = getSubfield("d", datafield);
                volume = getSubfield("l", datafield);
            }

            //series and number
            if ("036E".equals(tag)) {
                series = getSubfield("a", datafield);
                number = getSubfield("l", datafield);
                String kor = getSubfield("b", datafield);

                if (kor != null) {
                    series = series + " / " + kor;
                }
            }

            //note
            if ("037A".equals(tag)) {
                note = getSubfield("a", datafield);
            }

            //edition
            if ("032@".equals(tag)) {
                edition = getSubfield("a", datafield);
            }

            //isbn
            if ("004A".equals(tag)) {
                final String isbn10 = getSubfield("0", datafield);
                final String isbn13 = getSubfield("A", datafield);

                if (isbn10 != null) {
                    isbn = isbn10;
                }

                if (isbn13 != null) {
                    isbn = isbn13;
                }

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
                    entryType = "phdthesis";
                }
            }

            //journal oder booktitle

            /* Problematiken hier: Sowohl für Artikel in
             * Zeitschriften als für Beiträge in Büchern
             * wird 027D verwendet. Der Titel muß je nach
             * Fall booktitle oder journal zugeordnet
             * werden. Auch bei Zeitschriften werden hier
             * ggf. Verlag und Ort angegeben (sind dann
             * eigentlich überflüssig), während bei
             * Buchbeiträgen Verlag und Ort wichtig sind
             * (sonst in Kategorie 033A).
             */
            if ("027D".equals(tag)) {
                journal = getSubfield("a", datafield);
                booktitle = getSubfield("a", datafield);
                address = getSubfield("p", datafield);
                publisher = getSubfield("n", datafield);
            }

            //pagetotal
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

                entryType = "proceedings";
            }

            // Wenn eine Verlagsdiss vorliegt
            if ("phdthesis".equals(entryType) && (isbn != null)) {
                entryType = "book";
            }

            //Hilfskategorien zur Entscheidung @article
            //oder @incollection; hier könnte man auch die
            //ISBN herausparsen als Erleichterung für das
            //Auffinden der Quelle, die über die
            //SRU-Schnittstelle gelieferten Daten zur
            //Quelle unvollständig sind (z.B. nicht Serie
            //und Nummer angegeben werden)
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

        // Abfangen von Nulleintraegen
        if (quelle == null) {
            quelle = "";
        }

        // Nichtsortierzeichen entfernen
        if (author != null) {
            author = removeSortCharacters(author);
        }
        if (editor != null) {
            editor = removeSortCharacters(editor);
        }
        if (title != null) {
            title = removeSortCharacters(title);
        }
        if (subtitle != null) {
            subtitle = removeSortCharacters(subtitle);
        }

        // Dokumenttyp bestimmen und Eintrag anlegen

        if (mak.startsWith("As")) {
            entryType = BibEntry.DEFAULT_TYPE;

            if (quelle.contains("ISBN")) {
                entryType = "incollection";
            }
            if (quelle.contains("ZDB-ID")) {
                entryType = "article";
            }
        } else if (mak.isEmpty()) {
            entryType = BibEntry.DEFAULT_TYPE;
        } else if (mak.startsWith("O")) {
            entryType = BibEntry.DEFAULT_TYPE;
            // FIXME: online only available in Biblatex
            //entryType = "online";
        }

        /*
         * Wahrscheinlichkeit, dass ZDB-ID
         * vorhanden ist, ist größer als ISBN bei
         * Buchbeiträgen. Daher bei As?-Sätzen am besten immer
         * dann @incollection annehmen, wenn weder ISBN noch
         * ZDB-ID vorhanden sind.
         */
        BibEntry result = new BibEntry(entryType);

        // Zuordnung der Felder in Abhängigkeit vom Dokumenttyp
        if (author != null) {
            result.setField(FieldName.AUTHOR, author);
        }
        if (editor != null) {
            result.setField(FieldName.EDITOR, editor);
        }
        if (title != null) {
            result.setField(FieldName.TITLE, title);
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
            result.setField(FieldName.SUBTITLE, newSubtitle.toString());
        }
        if (publisher != null) {
            result.setField(FieldName.PUBLISHER, publisher);
        }
        if (year != null) {
            result.setField(FieldName.YEAR, year);
        }
        if (address != null) {
            result.setField(FieldName.ADDRESS, address);
        }
        if (series != null) {
            result.setField(FieldName.SERIES, series);
        }
        if (edition != null) {
            result.setField(FieldName.EDITION, edition);
        }
        if (isbn != null) {
            result.setField(FieldName.ISBN, isbn);
        }
        if (issn != null) {
            result.setField(FieldName.ISSN, issn);
        }
        if (number != null) {
            result.setField(FieldName.NUMBER, number);
        }
        if (pagetotal != null) {
            result.setField(FieldName.PAGETOTAL, pagetotal);
        }
        if (pages != null) {
            result.setField(FieldName.PAGES, pages);
        }
        if (volume != null) {
            result.setField(FieldName.VOLUME, volume);
        }
        if (journal != null) {
            result.setField(FieldName.JOURNAL, journal);
        }
        if (ppn != null) {
            result.setField("ppn_GVK", ppn);
        }
        if (url != null) {
            result.setField(FieldName.URL, url);
        }
        if (note != null) {
            result.setField(FieldName.NOTE, note);
        }

        if ("article".equals(entryType) && (journal != null)) {
            result.setField(FieldName.JOURNAL, journal);
        } else if ("incollection".equals(entryType) && (booktitle != null)) {
            result.setField(FieldName.BOOKTITLE, booktitle);
        }

        return result;
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

    private String removeSortCharacters(String input) {
        return input.replaceAll("\\@", "");
    }

}
