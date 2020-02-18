package org.jabref.logic.importer.fileformat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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
    private static boolean[] visited = new boolean[64];
    private static final Logger LOGGER = LoggerFactory.getLogger(GvkParser.class);

    private String author = null;
    private String editor = null;
    private String title = null;
    private String publisher = null;
    private String year = null;
    private String address = null;
    private String series = null;
    private String edition = null;
    private String isbn = null;
    private String issn = null;
    private String number = null;
    private String pagetotal = null;
    private String volume = null;
    private String pages = null;
    private String journal = null;
    private String ppn = null;
    private String booktitle = null;
    private String url = null;
    private String note = null;

    private String quelle = "";
    private String mak = "";
    private String subtitle = "";

    private EntryType entryType = StandardEntryType.Book; // Default

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
        // Alle relevanten Informationen einsammeln

        List<Element> datafields = getChildren("datafield", e);
        int SIZEOFDATAFIELDS = datafields.size();
        for (Element datafield : datafields) {
            visited[0] = true;
            String tag = datafield.getAttribute("tag");
            LOGGER.debug("tag: " + tag);

            // mak
            if ("002@".equals(tag)) {
                visited[1] = true;
                parseBibliographicTypeAndStatusData(datafield);
            } else {
                visited[2] = true;
            }

            //ppn
            if ("003@".equals(tag)) {
                visited[3] = true;
                parseRecordControlNumberData(datafield);
            } else {
                visited[4] = true;
            }

            //author
            if ("028A".equals(tag)) {
                visited[5] = true;  
                parsePrimaryAuthorData(datafield);              
            } else {
                visited[6] = true;
            }

            //author (weiterer)
            if ("028B".equals(tag)) {
                visited[7] = true;
                parseCoauthorData(datafield);
            } else {
                visited[8] = true;
            }

            //editor
            if ("028C".equals(tag)) {
                visited[9] = true;
                parseSecondaryAuthorData(datafield);
            } else {
                visited[10] = true;
            }

            //title and subtitle
            if ("021A".equals(tag)) {
                visited[11] = true;
                parseTitleAndStatementOfResponsibilityAreaData(datafield);
            } else {
                visited[12] = true;
            }

            //publisher and address
            if ("033A".equals(tag)) {
                visited[13] = true;
                parseFirstPublisherData(datafield);
            } else {
                visited[14] = true;
            }

            //year
            if ("011@".equals(tag)) {
                visited[15] = true;
                parseDateOfPublicationData(datafield);
            } else {
                visited[16] = true;
            }

            //year, volume, number, pages (year bei Zeitschriften (evtl. redundant mit 011@))
            if ("031A".equals(tag)) {
                visited[17] = true;
                parseNumberingAreaData(datafield);
            } else {
                visited[18] = true;
            }

            // 036D seems to contain more information than the other fields
            // overwrite information using that field
            // 036D also contains information normally found in 036E
            if ("036D".equals(tag)) {
                visited[19] = true;
                parseLinkToMultiVolumePublicationData(datafield);
            } else {
                visited[20] = true;
            }

            //series and number
            if ("036E".equals(tag)) {
                visited[21] = true;
                parseExtraLinkSerialPublicationData(datafield);
            } else {
                visited[22] = true;
            }

            //note
            if ("037A".equals(tag)) {
                visited[23] = true;
                parseGeneralNoteData(datafield);
            } else {
                visited[24] = true;
            }

            //edition
            if ("032@".equals(tag)) {
                visited[25] = true;
                parseEditionAreaData(datafield);
            } else {
                visited[26] = true;
            }

            //isbn
            if ("004A".equals(tag)) {
                visited[27] = true;
                parseISBNData(datafield);
            } else {
                visited[28] = true;
            }

            // Hochschulschriftenvermerk
            // Bei einer Verlagsdissertation ist der Ort schon eingetragen
            if ("037C".equals(tag)) {
                visited[29] = true;
                parseDissertationNoteData(datafield);
            } else {
                visited[30] = true;
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
                visited[31] = true;
                parseVolumeSetAndEssayData(datafield);
            } else {
                visited[32] = true;
            }

            //pagetotal
            if ("034D".equals(tag)) {
                visited[33] = true;
                parsePhysicalInformationData(datafield);
            } else {
                visited[34] = true;
            }

            // Behandlung von Konferenzen
            if ("030F".equals(tag)) {
                visited[35] = true;
                parseConferenceData(datafield);
            } else {
                visited[36] = true;
            }

            // Wenn eine Verlagsdiss vorliegt
            if (entryType.equals(StandardEntryType.PhdThesis) && (isbn != null)) {
                visited[37] = true;
                entryType = StandardEntryType.Book;
            } else {
                visited[38] = true;
            }

            //Hilfskategorien zur Entscheidung @article
            //oder @incollection; hier könnte man auch die
            //ISBN herausparsen als Erleichterung für das
            //Auffinden der Quelle, die über die
            //SRU-Schnittstelle gelieferten Daten zur
            //Quelle unvollständig sind (z.B. nicht Serie
            //und Nummer angegeben werden)
            if ("039B".equals(tag)) {
                visited[38] = true;
                parseRelationToParentLiteratureData(datafield);
            } else {
                visited[40] = true;
            }

            if ("046R".equals(tag) && ((quelle == null) || quelle.isEmpty())) {
                visited[41] = true;
                parseLiteratureSourceData(datafield);
            } else {
                visited[42] = true;
            }

            // URLs behandeln
            if ("009P".equals(tag) && ("03".equals(datafield.getAttribute("occurrence"))
                    || "05".equals(datafield.getAttribute("occurrence"))) && (url == null)) {
                visited[43] = true;
                parseOnlineResourceData(datafield);
            } else {
                visited[44] = true;
            }
        }
        // if we skipped the for loop completely
        if (SIZEOFDATAFIELDS == 0) {
            visited[45] = true;
        }
        // Abfangen von Nulleintraegen
        if (quelle == null) {
            visited[46] = true;
            quelle = "";
        } else {
            visited[47] = true;
        }

        // Nichtsortierzeichen entfernen
        if (author != null) {
            visited[48] = true;
            author = removeSortCharacters(author);
        } else {
            visited[49] = true;
        }

        if (editor != null) {
            visited[50] = true;
            editor = removeSortCharacters(editor);
        } else {
            visited[51] = true;
        }

        if (title != null) {
            visited[52] = true;
            title = removeSortCharacters(title);
        } else {
            visited[53] = true;
        }

        if (subtitle != null) {
            visited[54] = true;
            subtitle = removeSortCharacters(subtitle);
        } else {
            visited[55] = true;
        }

        // Dokumenttyp bestimmen und Eintrag anlegen

        if (mak.startsWith("As")) {
            visited[56] = true;
            entryType = BibEntry.DEFAULT_TYPE;

            if (quelle.contains("ISBN")) {
                visited[57] = true;
                entryType = StandardEntryType.InCollection;
            } else {
                visited[58] = true;
            }

            if (quelle.contains("ZDB-ID")) {
                visited[59] = true;
                entryType = StandardEntryType.Article;
            } else {
                visited[60] = true;
            }

        } else if (mak.isEmpty()) {
            visited[61] = true;
            entryType = BibEntry.DEFAULT_TYPE;
        } else if (mak.startsWith("O")) {
            visited[62] = true;
            entryType = BibEntry.DEFAULT_TYPE;
            // FIXME: online only available in Biblatex
            //entryType = "online";
        } else {
            visited[63] = true;
        }

        /*
         * Wahrscheinlichkeit, dass ZDB-ID
         * vorhanden ist, ist größer als ISBN bei
         * Buchbeiträgen. Daher bei As?-Sätzen am besten immer
         * dann @incollection annehmen, wenn weder ISBN noch
         * ZDB-ID vorhanden sind.
         */
        BibEntry result = new BibEntry(entryType);

        configureBibEntry(result);

        try {
            File f = new File("/tmp/parseEntry.txt");
            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            double frac = 0;
            for(int i = 0; i < visited.length; ++i) {
                frac += (visited[i] ? 1 : 0);
                bw.write("branch " + i + " was " + (visited[i] ? " visited." : " not visited.") + "\n");
            }
            bw.write("" + frac/visited.length);
            bw.close();
        } catch (Exception exc) {
            System.err.println("Could not open/write to file!");
            exc.printStackTrace();
        }

        return result;
    }

    /* HELPER FUNCTIONS FOR parseEntry */

    private void parseVolumeSetAndEssayData(Element datafield) {
        journal = getSubfield("a", datafield);
        booktitle = getSubfield("a", datafield);
        address = getSubfield("p", datafield);
        publisher = getSubfield("n", datafield);
    }

    private void parsePhysicalInformationData(Element datafield) {
        pagetotal = getSubfield("a", datafield);

        if (pagetotal != null) {
            // S, S. etc. entfernen
            pagetotal = pagetotal.replaceAll(" S\\.?$", "");
        }
    }

    private void parseConferenceData(Element datafield) {
        address = getSubfield("k", datafield);

        if (!"proceedings".equals(entryType)) {
            subtitle = getSubfield("a", datafield);
        }

        entryType = StandardEntryType.Proceedings;
    }

    private void parseRelationToParentLiteratureData(Element datafield) {
        quelle = getSubfield("8", datafield);
    }
    
    private void parseLiteratureSourceData(Element datafield) {
        quelle = getSubfield("a", datafield);
    }

    private void parseOnlineResourceData(Element datafield) {
        url = getSubfield("a", datafield);
    }

    private void configureBibEntry(BibEntry result) {
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
        }
    }

    private void parseBibliographicTypeAndStatusData(Element datafield) {
        mak = getSubfield("0", datafield);
        if (mak == null) {
            mak = "";
        } else {
        }
    }

    private void parseRecordControlNumberData(Element datafield) {
        ppn = getSubfield("0", datafield);
    }

    private void parsePrimaryAuthorData(Element datafield) {
        String vorname = getSubfield("d", datafield);
        String nachname = getSubfield("a", datafield);

        if (author == null) {
            author = "";
        } else {
            author = author.concat(" and ");
        }
        author = author.concat(vorname + " " + nachname);
    }

    private void parseCoauthorData(Element datafield) {
        String vorname = getSubfield("d", datafield);
        String nachname = getSubfield("a", datafield);

        if (author == null) {
            author = "";
        } else {
            author = author.concat(" and ");
        }
        author = author.concat(vorname + " " + nachname);        
    }

    private void parseSecondaryAuthorData(Element datafield) {
        String vorname = getSubfield("d", datafield);
        String nachname = getSubfield("a", datafield);

        if (editor == null) {
            editor = "";
        } else {
            editor = editor.concat(" and ");
        }
        editor = editor.concat(vorname + " " + nachname);
    }

    private void parseLinkToMultiVolumePublicationData(Element datafield) {
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

    private void parseNumberingAreaData(Element datafield) {
        year = getSubfield("j", datafield);
        volume = getSubfield("e", datafield);
        number = getSubfield("a", datafield);
        pages = getSubfield("h", datafield);
    }

    private void parseDateOfPublicationData(Element datafield) {
        year = getSubfield("a", datafield);
    }

    private void parseFirstPublisherData(Element datafield) {
        publisher = getSubfield("n", datafield);
        address = getSubfield("p", datafield);
    }

    private void parseTitleAndStatementOfResponsibilityAreaData(Element datafield) {
        title = getSubfield("a", datafield);
        subtitle = getSubfield("d", datafield);
    }

    private void parseExtraLinkSerialPublicationData(Element datafield) {
        series = getSubfield("a", datafield);
        number = getSubfield("l", datafield);
        String kor = getSubfield("b", datafield);
        if (kor != null) {
            series = series + " / " + kor;
        }      
    }

    private void parseGeneralNoteData(Element datafield)  {
        note = getSubfield("a", datafield);
    }

    private void parseEditionAreaData(Element datafield) {
        edition = getSubfield("a", datafield);
    }

    private void parseISBNData(Element datafield) {
        final String isbn10 = getSubfield("0", datafield);
        final String isbn13 = getSubfield("A", datafield);

        if (isbn10 != null) {
            isbn = isbn10;
        }

        if (isbn13 != null) {
            isbn = isbn13;
        }
    }
    
    private void parseDissertationNoteData(Element datafield) {
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
