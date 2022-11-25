package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

/**
 * Imports a Biblioscape Tag File. The format is described on
 * http://www.biblioscape.com/download/Biblioscape8.pdf Several
 * Biblioscape field types are ignored. Others are only included in the BibTeX
 * field "comment".
 */
public class BiblioscapeImporter extends Importer {

    @Override
    public String getName() {
        return "Biblioscape";
    }

    @Override
    public StandardFileType getFileType() {
        return StandardFileType.TXT;
    }

    @Override
    public String getDescription() {
        return "Imports a Biblioscape Tag File.\n" +
                "Several Biblioscape field types are ignored. Others are only included in the BibTeX field \"comment\".";
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader reader) {
        Objects.requireNonNull(reader);
        return true;
    }

    @Override
    public ParserResult importDatabase(BufferedReader reader) throws IOException {
        List<BibEntry> bibItems = new ArrayList<>();
        String line;
        Map<Field, String> hm = new HashMap<>();
        Map<String, StringBuilder> lines = new HashMap<>();
        StringBuilder previousLine = null;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) {
                continue; // ignore empty lines, e.g. at file
            }
            // end
            // entry delimiter -> item complete
            if ("------".equals(line)) {
                String[] type = new String[2];
                String[] pages = new String[2];
                String country = null;
                String address = null;
                String titleST = null;
                String titleTI = null;
                List<String> comments = new ArrayList<>();
                // add item
                for (Map.Entry<String, StringBuilder> entry : lines.entrySet()) {
                    if ("AU".equals(entry.getKey())) {
                        hm.put(StandardField.AUTHOR, entry.getValue()
                                                          .toString());
                    } else if ("TI".equals(entry.getKey())) {
                        titleTI = entry.getValue()
                                       .toString();
                    } else if ("ST".equals(entry.getKey())) {
                        titleST = entry.getValue()
                                       .toString();
                    } else if ("YP".equals(entry.getKey())) {
                        hm.put(StandardField.YEAR, entry
                                .getValue().toString());
                    } else if ("VL".equals(entry.getKey())) {
                        hm.put(StandardField.VOLUME, entry
                                .getValue().toString());
                    } else if ("NB".equals(entry.getKey())) {
                        hm.put(StandardField.NUMBER, entry
                                .getValue().toString());
                    } else if ("PS".equals(entry.getKey())) {
                        pages[0] = entry.getValue()
                                        .toString();
                    } else if ("PE".equals(entry.getKey())) {
                        pages[1] = entry.getValue()
                                        .toString();
                    } else if ("KW".equals(entry.getKey())) {
                        hm.put(StandardField.KEYWORDS, entry
                                .getValue().toString());
                    } else if ("RT".equals(entry.getKey())) {
                        type[0] = entry.getValue()
                                       .toString();
                    } else if ("SB".equals(entry.getKey())) {
                        comments.add("Subject: "
                                + entry.getValue());
                    } else if ("SA".equals(entry.getKey())) {
                        comments
                                .add("Secondary Authors: " + entry.getValue());
                    } else if ("NT".equals(entry.getKey())) {
                        hm.put(StandardField.NOTE, entry
                                .getValue().toString());
                    } else if ("PB".equals(entry.getKey())) {
                        hm.put(StandardField.PUBLISHER, entry
                                .getValue().toString());
                    } else if ("TA".equals(entry.getKey())) {
                        comments
                                .add("Tertiary Authors: " + entry.getValue());
                    } else if ("TT".equals(entry.getKey())) {
                        comments
                                .add("Tertiary Title: " + entry.getValue());
                    } else if ("ED".equals(entry.getKey())) {
                        hm.put(StandardField.EDITION, entry
                                .getValue().toString());
                    } else if ("TW".equals(entry.getKey())) {
                        type[1] = entry.getValue()
                                       .toString();
                    } else if ("QA".equals(entry.getKey())) {
                        comments
                                .add("Quaternary Authors: " + entry.getValue());
                    } else if ("QT".equals(entry.getKey())) {
                        comments
                                .add("Quaternary Title: " + entry.getValue());
                    } else if ("IS".equals(entry.getKey())) {
                        hm.put(StandardField.ISBN, entry
                                .getValue().toString());
                    } else if ("AB".equals(entry.getKey())) {
                        hm.put(StandardField.ABSTRACT, entry
                                .getValue().toString());
                    } else if ("AD".equals(entry.getKey())) {
                        address = entry.getValue()
                                       .toString();
                    } else if ("LG".equals(entry.getKey())) {
                        hm.put(StandardField.LANGUAGE, entry
                                .getValue().toString());
                    } else if ("CO".equals(entry.getKey())) {
                        country = entry.getValue()
                                       .toString();
                    } else if ("UR".equals(entry.getKey()) || "AT".equals(entry.getKey())) {
                        String s = entry.getValue().toString().trim();
                        hm.put(s.startsWith("http://") || s.startsWith("ftp://") ? StandardField.URL
                                : StandardField.PDF, entry.getValue().toString());
                    } else if ("C1".equals(entry.getKey())) {
                        comments.add("Custom1: "
                                + entry.getValue());
                    } else if ("C2".equals(entry.getKey())) {
                        comments.add("Custom2: "
                                + entry.getValue());
                    } else if ("C3".equals(entry.getKey())) {
                        comments.add("Custom3: "
                                + entry.getValue());
                    } else if ("C4".equals(entry.getKey())) {
                        comments.add("Custom4: "
                                + entry.getValue());
                    } else if ("C5".equals(entry.getKey())) {
                        comments.add("Custom5: "
                                + entry.getValue());
                    } else if ("C6".equals(entry.getKey())) {
                        comments.add("Custom6: "
                                + entry.getValue());
                    } else if ("DE".equals(entry.getKey())) {
                        hm.put(StandardField.ANNOTE, entry
                                .getValue().toString());
                    } else if ("CA".equals(entry.getKey())) {
                        comments.add("Categories: "
                                + entry.getValue());
                    } else if ("TH".equals(entry.getKey())) {
                        comments.add("Short Title: "
                                + entry.getValue());
                    } else if ("SE".equals(entry.getKey())) {
                        hm.put(StandardField.CHAPTER, entry
                                .getValue().toString());
                        // else if (entry.getKey().equals("AC"))
                        //   hm.put("",entry.getValue().toString());
                        // else if (entry.getKey().equals("LP"))
                        //   hm.put("",entry.getValue().toString());
                    }
                }

                EntryType bibtexType = BibEntry.DEFAULT_TYPE;
                // to find type, first check TW, then RT
                for (int i = 1; (i >= 0) && BibEntry.DEFAULT_TYPE.equals(bibtexType); --i) {
                    if (type[i] == null) {
                        continue;
                    }
                    type[i] = type[i].toLowerCase(Locale.ROOT);
                    if (type[i].contains("article")) {
                        bibtexType = StandardEntryType.Article;
                    } else if (type[i].contains("journal")) {
                        bibtexType = StandardEntryType.Article;
                    } else if (type[i].contains("book section")) {
                        bibtexType = StandardEntryType.InBook;
                    } else if (type[i].contains("book")) {
                        bibtexType = StandardEntryType.Book;
                    } else if (type[i].contains("conference")) {
                        bibtexType = StandardEntryType.InProceedings;
                    } else if (type[i].contains("proceedings")) {
                        bibtexType = StandardEntryType.InProceedings;
                    } else if (type[i].contains("report")) {
                        bibtexType = StandardEntryType.TechReport;
                    } else if (type[i].contains("thesis")
                            && type[i].contains("master")) {
                        bibtexType = StandardEntryType.MastersThesis;
                    } else if (type[i].contains("thesis")) {
                        bibtexType = StandardEntryType.PhdThesis;
                    }
                }

                // depending on bibtexType, decide where to place the titleRT and
                // titleTI
                if (bibtexType.equals(StandardEntryType.Article)) {
                    if (titleST != null) {
                        hm.put(StandardField.JOURNAL, titleST);
                    }
                    if (titleTI != null) {
                        hm.put(StandardField.TITLE, titleTI);
                    }
                } else if (bibtexType.equals(StandardEntryType.InBook)) {
                    if (titleST != null) {
                        hm.put(StandardField.BOOKTITLE, titleST);
                    }
                    if (titleTI != null) {
                        hm.put(StandardField.TITLE, titleTI);
                    }
                } else {
                    if (titleST != null) {
                        hm.put(StandardField.BOOKTITLE, titleST);
                    }
                    if (titleTI != null) {
                        hm.put(StandardField.TITLE, titleTI);
                    }
                }

                // concatenate pages
                if ((pages[0] != null) || (pages[1] != null)) {
                    hm.put(StandardField.PAGES, (pages[0] == null ? "" : pages[0]) + (pages[1] == null ? "" : "--" + pages[1]));
                }

                // concatenate address and country
                if (address != null) {
                    hm.put(StandardField.ADDRESS, address + (country == null ? "" : ", " + country));
                }

                if (!comments.isEmpty()) { // set comment if present
                    hm.put(StandardField.COMMENT, String.join(";", comments));
                }
                BibEntry b = new BibEntry(bibtexType);
                b.setField(hm);
                bibItems.add(b);

                hm.clear();
                lines.clear();
                previousLine = null;

                continue;
            }
            // new key
            if (line.startsWith("--") && (line.length() >= 7)
                    && "-- ".equals(line.substring(4, 7))) {
                previousLine = new StringBuilder(line.substring(7));
                lines.put(line.substring(2, 4), previousLine);
                continue;
            }
            // continuation (folding) of previous line
            if (previousLine == null) {
                return new ParserResult();
            }
            previousLine.append(line.trim());
        }

        return new ParserResult(bibItems);
    }
}
