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
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class GvkParser implements Parser {
    int branchIdx = 0; /*ASSI3: For branch coverage DIY*/
    public static boolean[] taken = new boolean[1000]; /*ASSI3: Temporary array for branch coverage DIY */
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

        // Schleife ueber alle Teilergebnisse
        // Element root = content.getDocumentElement();
        Element root = (Element) content.getElementsByTagName("zs:searchRetrieveResponse").item(0);
        Element srwrecords = getChild("zs:records", root);
        if (srwrecords == null) {
            // no records found -> return empty list
            return result;
        }
        List<Element> records = getChildren("zs:record", srwrecords);
        for (Element gvkRecord : records) {
            Element e = getChild("zs:recordData", gvkRecord);
            if (e != null) {
                e = getChild("record", e);
                if (e != null) {
                    BibEntry bibEntry = parseEntry(e);
                    // TODO: Add filtering on years (based on org.jabref.logic.importer.fetcher.transformers.YearRangeByFilteringQueryTransformer.getStartYear)
                    result.add(bibEntry);
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

        EntryType entryType = StandardEntryType.Book; // Default

        // Alle relevanten Informationen einsammeln

        List<Element> datafields = getChildren("datafield", e);
        for (Element datafield : datafields) {
            String tag = datafield.getAttribute("tag");
            LOGGER.debug("tag: " + tag);

            // mak
            if ("002@".equals(tag)) {
                taken[0] = true; /*ASSI3: For branch coverage DIY*/
                mak = getSubfield("0", datafield);
                if (mak == null) {
                    taken[1] = true; /*ASSI3: For branch coverage DIY*/
                    mak = "";
                }else{taken[2] = true;} /*ASSI3: For branch coverage DIY*/
            }else{taken[3] = true;} /*ASSI3: For branch coverage DIY*/

            // ppn
            if ("003@".equals(tag)) {
                taken[4] = true; /*ASSI3: For branch coverage DIY*/
                ppn = getSubfield("0", datafield);
            }else{taken[4] = true; /*ASSI3: For branch coverage DIY*/}

            // author
            if ("028A".equals(tag)) {
                taken[5] = true; /*ASSI3: For branch coverage DIY*/
                String vorname = getSubfield("d", datafield);
                String nachname = getSubfield("a", datafield);

                if (author == null) {
                    taken[6] = true; /*ASSI3: For branch coverage DIY*/
                    author = "";
                } else {
                    taken[7] = true; /*ASSI3: For branch coverage DIY*/
                    author = author.concat(" and ");
                }
                author = author.concat(vorname + " " + nachname);
            }else{taken[8] = true; /*ASSI3: For branch coverage DIY*/}
            // author (weiterer)
            if ("028B".equals(tag)) {
                taken[9] = true; /*ASSI3: For branch coverage DIY*/
                String vorname = getSubfield("d", datafield);
                String nachname = getSubfield("a", datafield);

                if (author == null) {
                    taken[10] = true; /*ASSI3: For branch coverage DIY*/
                    author = "";
                } else {
                    taken[11] = true; /*ASSI3: For branch coverage DIY*/
                    author = author.concat(" and ");
                }
                author = author.concat(vorname + " " + nachname);
            }else{taken[12] = true; /*ASSI3: For branch coverage DIY*/}

            // editor
            if ("028C".equals(tag)) {
                taken[13] = true; /*ASSI3: For branch coverage DIY*/
                String vorname = getSubfield("d", datafield);
                String nachname = getSubfield("a", datafield);

                if (editor == null) {
                    taken[14] = true; /*ASSI3: For branch coverage DIY*/
                    editor = "";
                } else {
                    taken[15] = true; /*ASSI3: For branch coverage DIY*/
                    editor = editor.concat(" and ");
                }
                editor = editor.concat(vorname + " " + nachname);
            }else{taken[16] = true; /*ASSI3: For branch coverage DIY*/}

            // title and subtitle
            if ("021A".equals(tag)) {
                taken[17] = true; /*ASSI3: For branch coverage DIY*/
                title = getSubfield("a", datafield);
                subtitle = getSubfield("d", datafield);
            }else{taken[18] = true; /*ASSI3: For branch coverage DIY*/}

            // publisher and address
            if ("033A".equals(tag)) {
                taken[19] = true; /*ASSI3: For branch coverage DIY*/
                publisher = getSubfield("n", datafield);
                address = getSubfield("p", datafield);
            }else{taken[20] = true; /*ASSI3: For branch coverage DIY*/}

            // year
            if ("011@".equals(tag)) {
                taken[21] = true; /*ASSI3: For branch coverage DIY*/
                year = getSubfield("a", datafield);
            }else{taken[22] = true; /*ASSI3: For branch coverage DIY*/}

            // year, volume, number, pages (year bei Zeitschriften (evtl. redundant mit 011@))
            if ("031A".equals(tag)) {
                taken[23] = true; /*ASSI3: For branch coverage DIY*/
                year = getSubfield("j", datafield);

                volume = getSubfield("e", datafield);
                number = getSubfield("a", datafield);
                pages = getSubfield("h", datafield);
            }else{taken[24] = true; /*ASSI3: For branch coverage DIY*/}

            // 036D seems to contain more information than the other fields
            // overwrite information using that field
            // 036D also contains information normally found in 036E
            if ("036D".equals(tag)) {
                taken[25] = true; /*ASSI3: For branch coverage DIY*/
                // 021 might have been present
                if (title != null) {
                    taken[26] = true; /*ASSI3: For branch coverage DIY*/
                    // convert old title (contained in "a" of 021A) to volume
                    if (title.startsWith("@")) {
                        taken[27] = true; /*ASSI3: For branch coverage DIY*/
                        // "@" indicates a number
                        title = title.substring(1);
                    }else{taken[28] = true; /*ASSI3: For branch coverage DIY*/}
                    number = title;
                }else{taken[29] = true; /*ASSI3: For branch coverage DIY*/}
                // title and subtitle
                title = getSubfield("a", datafield);
                subtitle = getSubfield("d", datafield);
                volume = getSubfield("l", datafield);
            }else{taken[30] = true; /*ASSI3: For branch coverage DIY*/}

            // series and number
            if ("036E".equals(tag)) {
                taken[31] = true; /*ASSI3: For branch coverage DIY*/
                series = getSubfield("a", datafield);
                number = getSubfield("l", datafield);
                String kor = getSubfield("b", datafield);

                if (kor != null) {
                    taken[32] = true; /*ASSI3: For branch coverage DIY*/
                    series = series + " / " + kor;
                }else{taken[33] = true; /*ASSI3: For branch coverage DIY*/}
            }else{taken[34] = true; /*ASSI3: For branch coverage DIY*/}

            // note
            if ("037A".equals(tag)) {
                taken[35] = true; /*ASSI3: For branch coverage DIY*/
                note = getSubfield("a", datafield);
            }else{taken[36] = true; /*ASSI3: For branch coverage DIY*/}

            // edition
            if ("032@".equals(tag)) {
                taken[37] = true; /*ASSI3: For branch coverage DIY*/
                edition = getSubfield("a", datafield);
            }else{taken[38] = true; /*ASSI3: For branch coverage DIY*/}

            // isbn
            if ("004A".equals(tag)) {
                taken[39] = true; /*ASSI3: For branch coverage DIY*/
                final String isbn10 = getSubfield("0", datafield);
                final String isbn13 = getSubfield("A", datafield);

                if (isbn10 != null) {
                    taken[40] = true; /*ASSI3: For branch coverage DIY*/
                    isbn = isbn10;
                }else{taken[41] = true; /*ASSI3: For branch coverage DIY*/}

                if (isbn13 != null) {
                    isbn = isbn13;
                }else{taken[42] = true; /*ASSI3: For branch coverage DIY*/}
            }else{taken[43] = true; /*ASSI3: For branch coverage DIY*/}

            // Hochschulschriftenvermerk
            // Bei einer Verlagsdissertation ist der Ort schon eingetragen
            if ("037C".equals(tag)) {
                taken[44] = true; /*ASSI3: For branch coverage DIY*/
                if (address == null) {
                    taken[45] = true; /*ASSI3: For branch coverage DIY*/
                    address = getSubfield("b", datafield);
                    if (address != null) {
                        taken[46] = true; /*ASSI3: For branch coverage DIY*/
                        address = removeSortCharacters(address);
                    }else{taken[47] = true; /*ASSI3: For branch coverage DIY*/}
                }else{taken[48] = true; /*ASSI3: For branch coverage DIY*/}

                String st = getSubfield("a", datafield);
                if ((st != null) && st.contains("Diss")) {
                    taken[49] = true; /*ASSI3: For branch coverage DIY*/
                    entryType = StandardEntryType.PhdThesis;
                }else{taken[50] = true; /*ASSI3: For branch coverage DIY*/}
            }else{taken[51] = true; /*ASSI3: For branch coverage DIY*/}

            // journal oder booktitle

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
                taken[52] = true; /*ASSI3: For branch coverage DIY*/
                journal = getSubfield("a", datafield);
                booktitle = getSubfield("a", datafield);
                address = getSubfield("p", datafield);
                publisher = getSubfield("n", datafield);
            }else{taken[53] = true; /*ASSI3: For branch coverage DIY*/}

            // pagetotal
            if ("034D".equals(tag)) {
                taken[54] = true; /*ASSI3: For branch coverage DIY*/
                pagetotal = getSubfield("a", datafield);

                if (pagetotal != null) {
                    taken[55] = true; /*ASSI3: For branch coverage DIY*/
                    // S, S. etc. entfernen
                    pagetotal = pagetotal.replaceAll(" S\\.?$", "");
                }else{taken[56] = true; /*ASSI3: For branch coverage DIY*/}
            }else{taken[57] = true; /*ASSI3: For branch coverage DIY*/}

            // Behandlung von Konferenzen
            if ("030F".equals(tag)) {
                taken[58] = true; /*ASSI3: For branch coverage DIY*/
                address = getSubfield("k", datafield);

                if (!"proceedings".equals(entryType)) {
                    taken[59] = true; /*ASSI3: For branch coverage DIY*/
                    subtitle = getSubfield("a", datafield);
                }else{taken[60] = true; /*ASSI3: For branch coverage DIY*/}

                entryType = StandardEntryType.Proceedings;
            }else{taken[61] = true; /*ASSI3: For branch coverage DIY*/}

            // Wenn eine Verlagsdiss vorliegt
            if (entryType.equals(StandardEntryType.PhdThesis) && (isbn != null)) {
                taken[62] = true; /*ASSI3: For branch coverage DIY*/
                entryType = StandardEntryType.Book;
            }else{taken[63] = true; /*ASSI3: For branch coverage DIY*/}

            // Hilfskategorien zur Entscheidung @article
            // oder @incollection; hier könnte man auch die
            // ISBN herausparsen als Erleichterung für das
            // Auffinden der Quelle, die über die
            // SRU-Schnittstelle gelieferten Daten zur
            // Quelle unvollständig sind (z.B. nicht Serie
            // und Nummer angegeben werden)
            if ("039B".equals(tag)) {
                taken[64] = true; /*ASSI3: For branch coverage DIY*/
                quelle = getSubfield("8", datafield);
            }else{taken[65] = true; /*ASSI3: For branch coverage DIY*/}
            if ("046R".equals(tag) && ((quelle == null) || quelle.isEmpty())) {
                taken[66] = true; /*ASSI3: For branch coverage DIY*/
                quelle = getSubfield("a", datafield);
            }else{taken[67] = true; /*ASSI3: For branch coverage DIY*/}

            // URLs behandeln
            if ("009P".equals(tag) && ("03".equals(datafield.getAttribute("occurrence"))
                    || "05".equals(datafield.getAttribute("occurrence"))) && (url == null)) {
                taken[68] = true; /*ASSI3: For branch coverage DIY*/
                url = getSubfield("a", datafield);
            }else{taken[69] = true; /*ASSI3: For branch coverage DIY*/}
        }

        // Abfangen von Nulleintraegen
        if (quelle == null) {
            taken[70] = true; /*ASSI3: For branch coverage DIY*/
            quelle = "";
        }else{taken[71] = true; /*ASSI3: For branch coverage DIY*/}

        // Nichtsortierzeichen entfernen
        if (author != null) {
            taken[72] = true; /*ASSI3: For branch coverage DIY*/
            author = removeSortCharacters(author);
        }else{taken[73] = true; /*ASSI3: For branch coverage DIY*/}
        if (editor != null) {
            taken[74] = true; /*ASSI3: For branch coverage DIY*/
            editor = removeSortCharacters(editor);
        }else{taken[75] = true; /*ASSI3: For branch coverage DIY*/}
        if (title != null) {
            taken[76] = true; /*ASSI3: For branch coverage DIY*/
            title = removeSortCharacters(title);
        }else{taken[77] = true; /*ASSI3: For branch coverage DIY*/}
        if (subtitle != null) {
            taken[78] = true; /*ASSI3: For branch coverage DIY*/
            subtitle = removeSortCharacters(subtitle);
        }else{taken[79] = true; /*ASSI3: For branch coverage DIY*/}

        // Dokumenttyp bestimmen und Eintrag anlegen

        if (mak.startsWith("As")) {
            taken[80] = true; /*ASSI3: For branch coverage DIY*/
            entryType = BibEntry.DEFAULT_TYPE;

            if (quelle.contains("ISBN")) {
                taken[81] = true; /*ASSI3: For branch coverage DIY*/
                entryType = StandardEntryType.InCollection;
            }else{taken[82] = true; /*ASSI3: For branch coverage DIY*/}
            if (quelle.contains("ZDB-ID")) {
                taken[83] = true; /*ASSI3: For branch coverage DIY*/
                entryType = StandardEntryType.Article;
            }else{taken[84] = true; /*ASSI3: For branch coverage DIY*/}
        } else if (mak.isEmpty()) {
            taken[85] = true; /*ASSI3: For branch coverage DIY*/
            entryType = BibEntry.DEFAULT_TYPE;
        } else if (mak.startsWith("O")) {
            taken[86] = true; /*ASSI3: For branch coverage DIY*/
            entryType = BibEntry.DEFAULT_TYPE;
            // FIXME: online only available in Biblatex
            // entryType = "online";
        }else{taken[87] = true; /*ASSI3: For branch coverage DIY*/}

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
            taken[88] = true; /*ASSI3: For branch coverage DIY*/
            result.setField(StandardField.AUTHOR, author);
        }else{taken[89] = true; /*ASSI3: For branch coverage DIY*/}
        if (editor != null) {
            taken[90] = true; /*ASSI3: For branch coverage DIY*/
            result.setField(StandardField.EDITOR, editor);
        }else{taken[91] = true; /*ASSI3: For branch coverage DIY*/}
        if (title != null) {
            taken[92] = true; /*ASSI3: For branch coverage DIY*/
            result.setField(StandardField.TITLE, title);
        }else{taken[93] = true; /*ASSI3: For branch coverage DIY*/}
        if (!Strings.isNullOrEmpty(subtitle)) {
            taken[94] = true; /*ASSI3: For branch coverage DIY*/
            // ensure that first letter is an upper case letter
            // there could be the edge case that the string is only one character long, therefore, this special treatment
            // this is Apache commons lang StringUtils.capitalize (https://commons.apache.org/proper/commons-lang/javadocs/api-release/org/apache/commons/lang3/StringUtils.html#capitalize%28java.lang.String%29), but we don't want to add an additional dependency  ('org.apache.commons:commons-lang3:3.4')
            StringBuilder newSubtitle = new StringBuilder(
                    Character.toString(Character.toUpperCase(subtitle.charAt(0))));
            if (subtitle.length() > 1) {
                taken[95] = true; /*ASSI3: For branch coverage DIY*/
                newSubtitle.append(subtitle.substring(1));
            }else{taken[96] = true; /*ASSI3: For branch coverage DIY*/}
            result.setField(StandardField.SUBTITLE, newSubtitle.toString());
        }else{taken[97] = true; /*ASSI3: For branch coverage DIY*/}
        if (publisher != null) {
            taken[98] = true; /*ASSI3: For branch coverage DIY*/
            result.setField(StandardField.PUBLISHER, publisher);
        }else{taken[99] = true; /*ASSI3: For branch coverage DIY*/}
        if (year != null) {
            taken[100] = true; /*ASSI3: For branch coverage DIY*/
            result.setField(StandardField.YEAR, year);
        }else{taken[101] = true; /*ASSI3: For branch coverage DIY*/}
        if (address != null) {
            taken[102] = true; /*ASSI3: For branch coverage DIY*/
            result.setField(StandardField.ADDRESS, address);
        }else{taken[103] = true; /*ASSI3: For branch coverage DIY*/}
        if (series != null) {
            taken[104] = true; /*ASSI3: For branch coverage DIY*/
            result.setField(StandardField.SERIES, series);
        }else{taken[105] = true; /*ASSI3: For branch coverage DIY*/}
        if (edition != null) {
            taken[106] = true; /*ASSI3: For branch coverage DIY*/
            result.setField(StandardField.EDITION, edition);
        }else{taken[107] = true; /*ASSI3: For branch coverage DIY*/}
        if (isbn != null) {
            taken[108] = true; /*ASSI3: For branch coverage DIY*/
            result.setField(StandardField.ISBN, isbn);
        }else{taken[109] = true; /*ASSI3: For branch coverage DIY*/}
        if (issn != null) {
            taken[110] = true; /*ASSI3: For branch coverage DIY*/
            result.setField(StandardField.ISSN, issn);
        }else{taken[111] = true; /*ASSI3: For branch coverage DIY*/}
        if (number != null) {
            taken[112] = true; /*ASSI3: For branch coverage DIY*/
            result.setField(StandardField.NUMBER, number);
        }else{taken[113] = true; /*ASSI3: For branch coverage DIY*/}
        if (pagetotal != null) {
            taken[114] = true; /*ASSI3: For branch coverage DIY*/
            result.setField(StandardField.PAGETOTAL, pagetotal);
        }else{taken[115] = true; /*ASSI3: For branch coverage DIY*/}
        if (pages != null) {
            taken[116] = true; /*ASSI3: For branch coverage DIY*/
            result.setField(StandardField.PAGES, pages);
        }else{taken[117] = true; /*ASSI3: For branch coverage DIY*/}
        if (volume != null) {
            taken[118] = true; /*ASSI3: For branch coverage DIY*/
            result.setField(StandardField.VOLUME, volume);
        }else{taken[119] = true; /*ASSI3: For branch coverage DIY*/}
        if (journal != null) {
            taken[120] = true; /*ASSI3: For branch coverage DIY*/
            result.setField(StandardField.JOURNAL, journal);
        }else{taken[121] = true; /*ASSI3: For branch coverage DIY*/}
        if (ppn != null) {
            taken[122] = true; /*ASSI3: For branch coverage DIY*/
            result.setField(new UnknownField("ppn_GVK"), ppn);
        }else{taken[123] = true; /*ASSI3: For branch coverage DIY*/}
        if (url != null) {
            taken[124] = true; /*ASSI3: For branch coverage DIY*/
            result.setField(StandardField.URL, url);
        }else{taken[125] = true; /*ASSI3: For branch coverage DIY*/}
        if (note != null) {
            taken[126] = true; /*ASSI3: For branch coverage DIY*/
            result.setField(StandardField.NOTE, note);
        }else{taken[127] = true; /*ASSI3: For branch coverage DIY*/}

        if ("article".equals(entryType) && (journal != null)) {
            taken[128] = true; /*ASSI3: For branch coverage DIY*/
            result.setField(StandardField.JOURNAL, journal);
        } else if ("incollection".equals(entryType) && (booktitle != null)) {
            taken[129] = true; /*ASSI3: For branch coverage DIY*/
            result.setField(StandardField.BOOKTITLE, booktitle);
        }else{taken[130] = true; /*ASSI3: For branch coverage DIY*/}

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
