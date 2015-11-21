/**
 * License: GPLv2, but Jan Frederik Maas agreed to change license upon request
 */
package net.sf.jabref.importer.fetcher;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.sf.jabref.bibtex.EntryTypes;
import net.sf.jabref.importer.ImportFormatReader;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.model.entry.IdGenerator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class GVKParser {

    public List<BibtexEntry> parseEntries(InputStream is)
            throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilder dbuild = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document content = dbuild.parse(is);
        return this.parseEntries(content);
    }

    public List<BibtexEntry> parseEntries(Document content) {
        List<BibtexEntry> result = new LinkedList<>();

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
            e = getChild("record", e);
            result.add(parseEntry(e));
        }
        return result;
    }

    private BibtexEntry parseEntry(Element e) {
        String author = null;
        String editor = null;
        String title = null;
        String publisher = null;
        String date = null;
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
        Iterator<Element> iter = datafields.iterator();
        while (iter.hasNext()) {
            Element datafield = iter.next();

            // System.out.println(datafield.getAttributeValue("tag"));

            // mak
            if (datafield.getAttribute("tag").equals("002@")) {
                mak = getSubfield("0", datafield);
            }

            //ppn
            if (datafield.getAttribute("tag").equals("003@")) {
                ppn = getSubfield("0", datafield);
            }

            //author
            if (datafield.getAttribute("tag").equals("028A")) {
                String vorname = getSubfield("d", datafield);
                String nachname = getSubfield("a", datafield);

                if (author != null) {
                    author = author.concat(" and ");
                } else {
                    author = "";
                }
                author = author.concat(vorname + " " + nachname);
            }
            //author (weiterer)
            if (datafield.getAttribute("tag").equals("028B")) {
                String vorname = getSubfield("d", datafield);
                String nachname = getSubfield("a", datafield);

                if (author != null) {
                    author = author.concat(" and ");
                } else {
                    author = "";
                }
                author = author.concat(vorname + " " + nachname);
            }

            //editor
            if (datafield.getAttribute("tag").equals("028C")) {
                String vorname = getSubfield("d", datafield);
                String nachname = getSubfield("a", datafield);

                if (editor != null) {
                    editor = editor.concat(" and ");
                } else {
                    editor = "";
                }
                editor = editor.concat(vorname + " " + nachname);
            }

            //title and subtitle
            if (datafield.getAttribute("tag").equals("021A")) {
                title = getSubfield("a", datafield);
                subtitle = getSubfield("d", datafield);
            }

            //publisher and address
            if (datafield.getAttribute("tag").equals("033A")) {
                publisher = getSubfield("n", datafield);
                address = getSubfield("p", datafield);
            }

            //date
            if (datafield.getAttribute("tag").equals("011@")) {
                date = getSubfield("a", datafield);
            }

            //date, volume, number, pages (year bei Zeitschriften (evtl. redundant mit 011@))
            if (datafield.getAttribute("tag").equals("031A")) {
                date = getSubfield("j", datafield);
                volume = getSubfield("e", datafield);
                number = getSubfield("a", datafield);
                pages = getSubfield("h", datafield);

            }

            // 036D seems to contain more information than the other fields
            // overwrite information using that field
            // 036D also contains information normally found in 036E
            if (datafield.getAttribute("tag").equals("036D")) {
                // 021 might have been present
                if (title != null) {
                    // convert old title (contained in "a" of 021A) to volume
                    if (title.startsWith("@")) {
                        // "@" indicates a number
                        title = title.substring(1);
                    } else {
                        // we nevertheless keep the old title data
                    }
                    number = title;
                }
                //title and subtitle
                title = getSubfield("a", datafield);
                subtitle = getSubfield("d", datafield);
                volume = getSubfield("l", datafield);
            }

            //series and number
            if (datafield.getAttribute("tag").equals("036E")) {
                series = getSubfield("a", datafield);
                number = getSubfield("l", datafield);
                String kor = getSubfield("b", datafield);

                if (kor != null) {
                    series = series + " / " + kor;
                }
            }

            //note
            if (datafield.getAttribute("tag").equals("037A")) {
                note = getSubfield("a", datafield);
            }

            //edition
            if (datafield.getAttribute("tag").equals("032@")) {
                edition = getSubfield("a", datafield);
            }

            //isbn
            if (datafield.getAttribute("tag").equals("004A")) {
                String isbn_10 = getSubfield("0", datafield);
                String isbn_13 = getSubfield("A", datafield);

                if (isbn_10 != null) {
                    isbn = isbn_10;
                }

                if (isbn_13 != null) {
                    isbn = isbn_13;
                }

            }

            // Hochschulschriftenvermerk
            // Bei einer Verlagsdissertation ist der Ort schon eingetragen
            if (datafield.getAttribute("tag").equals("037C")) {
                if (address == null) {
                    address = getSubfield("b", datafield);
                    address = removeSortCharacters(address);
                }

                String st = getSubfield("a", datafield);
                if (st != null) {
                    if (st.contains("Diss")) {
                        entryType = "phdthesis";
                    }
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
            if (datafield.getAttribute("tag").equals("027D")) {
                journal = getSubfield("a", datafield);
                booktitle = getSubfield("a", datafield);
                address = getSubfield("p", datafield);
                publisher = getSubfield("n", datafield);
            }

            //pagetotal
            if (datafield.getAttribute("tag").equals("034D")) {
                pagetotal = getSubfield("a", datafield);

                // S, S. etc. entfernen
                pagetotal = pagetotal.replaceAll(" S\\.?$", "");
            }

            // Behandlung von Konferenzen
            if (datafield.getAttribute("tag").equals("030F")) {
                address = getSubfield("k", datafield);

                if (!entryType.equals("proceedings")) {
                    subtitle = getSubfield("a", datafield);
                }

                entryType = "proceedings";
            }

            // Wenn eine Verlagsdiss vorliegt
            if (entryType.equals("phdthesis")) {
                if (isbn != null) {
                    entryType = "book";
                }
            }

            //Hilfskategorien zur Entscheidung @article
            //oder @incollection; hier könnte man auch die
            //ISBN herausparsen als Erleichterung für das
            //Auffinden der Quelle, die über die
            //SRU-Schnittstelle gelieferten Daten zur
            //Quelle unvollständig sind (z.B. nicht Serie
            //und Nummer angegeben werden)
            if (datafield.getAttribute("tag").equals("039B")) {
                quelle = getSubfield("8", datafield);
            }
            if (datafield.getAttribute("tag").equals("046R")) {
                if (quelle.equals("") || (quelle == null)) {
                    quelle = getSubfield("a", datafield);
                }
            }

            // URLs behandeln
            if (datafield.getAttribute("tag").equals("009P")) {
                if (datafield.getAttribute("occurrence").equals("03")
                        || datafield.getAttribute("occurrence").equals("05")) {
                    if (url == null) {
                        url = getSubfield("a", datafield);
                    }
                }
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
            entryType = "misc";

            if (quelle.contains("ISBN")) {
                entryType = "incollection";
            }
            if (quelle.contains("ZDB-ID")) {
                entryType = "article";
            }
        } else if (mak.equals("")) {
            entryType = "misc";
        } else if (mak.startsWith("O")) {
            entryType = "online";
        }

        /*
         * Wahrscheinlichkeit, dass ZDB-ID
         * vorhanden ist, ist größer als ISBN bei
         * Buchbeiträgen. Daher bei As?-Sätzen am besten immer
         * dann @incollection annehmen, wenn weder ISBN noch
         * ZDB-ID vorhanden sind.
         */
        BibtexEntry result = new BibtexEntry(IdGenerator.next(), EntryTypes.getType(entryType));

        // Zuordnung der Felder in Abhängigkeit vom Dokumenttyp
        if (author != null) {
            result.setField("author", ImportFormatReader.expandAuthorInitials(author));
        }
        if (editor != null) {
            result.setField("editor", ImportFormatReader.expandAuthorInitials(editor));
        }
        if (title != null) {
            result.setField("title", title);
        }
        if (subtitle != null) {
            result.setField("subtitle", subtitle);
        }
        if (publisher != null) {
            result.setField("publisher", publisher);
        }
        if (date != null) {
            result.setField("date", date);
        }
        if (address != null) {
            result.setField("address", address);
        }
        if (series != null) {
            result.setField("series", series);
        }
        if (edition != null) {
            result.setField("edition", edition);
        }
        if (isbn != null) {
            result.setField("isbn", isbn);
        }
        if (issn != null) {
            result.setField("issn", issn);
        }
        if (number != null) {
            result.setField("number", number);
        }
        if (pagetotal != null) {
            result.setField("pagetotal", pagetotal);
        }
        if (pages != null) {
            result.setField("pages", pages);
        }
        if (volume != null) {
            result.setField("volume", volume);
        }
        if (journal != null) {
            result.setField("journal", journal);
        }
        if (ppn != null) {
            result.setField("ppn_GVK", ppn);
        }
        if (url != null) {
            result.setField("url", url);
        }
        if (note != null) {
            result.setField("note", note);
        }

        if (entryType.equals("article")) {
            if (journal != null) {
                result.setField("journal", journal);
            }
        } else if (entryType.equals("incollection")) {
            if (booktitle != null) {
                result.setField("booktitle", booktitle);
            }
        }

        return result;
    }

    private String getSubfield(String a, Element datafield) {
        List<Element> liste = getChildren("subfield", datafield);
        Iterator<Element> iter = liste.iterator();

        while (iter.hasNext()) {
            Element subfield = iter.next();
            if (subfield.getAttribute("code").equals(a)) {
                return (subfield.getTextContent());
            }
        }
        return null;
    }

    private Element getChild(String name, Element e) {
        Element result = null;

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
        return result;
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
        input = input.replaceAll("\\@", "");
        return input;
    }

}
