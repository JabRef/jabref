/*  Copyright (C) 2003-2015 JabRef contributors.
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
package net.sf.jabref.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.jabref.importer.ImportFormatReader;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.model.entry.BibEntry;

/**
 * Imports a Biblioscape Tag File. The format is described on
 * http://www.biblioscape.com/manual_bsp/Biblioscape_Tag_File.htm Several
 * Biblioscape field types are ignored. Others are only included in the BibTeX
 * field "comment".
 */
public class BiblioscapeImporter extends ImportFormat {

    /**
     * Return the name of this import format.
     */
    @Override
    public String getFormatName() {
        return "Biblioscape";
    }

    /*
     *  (non-Javadoc)
     * @see net.sf.jabref.imports.ImportFormat#getCLIId()
     */
    @Override
    public String getCLIId() {
        return "biblioscape";
    }

    /**
     * Check whether the source is in the correct format for this importer.
     */
    @Override
    public boolean isRecognizedFormat(InputStream in) throws IOException {
        return true;
    }

    /**
     * Parse the entries in the source, and return a List of BibEntry
     * objects.
     */
    @Override
    public List<BibEntry> importEntries(InputStream stream, OutputPrinter status) throws IOException {

        List<BibEntry> bibItems = new ArrayList<>();
        BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));
        String line;
        Map<String, String> hm = new HashMap<>();
        Map<String, StringBuilder> lines = new HashMap<>();
        StringBuilder previousLine = null;
        while ((line = in.readLine()) != null) {
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
                        hm.put("author", entry.getValue()
                                .toString());
                    } else if ("TI".equals(entry.getKey())) {
                        titleTI = entry.getValue()
                                .toString();
                    } else if ("ST".equals(entry.getKey())) {
                        titleST = entry.getValue()
                                .toString();
                    } else if ("YP".equals(entry.getKey())) {
                        hm.put("year", entry
                                .getValue().toString());
                    } else if ("VL".equals(entry.getKey())) {
                        hm.put("volume", entry
                                .getValue().toString());
                    } else if ("NB".equals(entry.getKey())) {
                        hm.put("number", entry
                                .getValue().toString());
                    } else if ("PS".equals(entry.getKey())) {
                        pages[0] = entry.getValue()
                                .toString();
                    } else if ("PE".equals(entry.getKey())) {
                        pages[1] = entry.getValue()
                                .toString();
                    } else if ("KW".equals(entry.getKey())) {
                        hm.put("keywords", entry
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
                        hm.put("note", entry
                                .getValue().toString());
                    } else if ("PB".equals(entry.getKey())) {
                        hm.put("publisher", entry
                                .getValue().toString());
                    } else if ("TA".equals(entry.getKey())) {
                        comments
                        .add("Tertiary Authors: " + entry.getValue());
                    } else if ("TT".equals(entry.getKey())) {
                        comments
                        .add("Tertiary Title: " + entry.getValue());
                    } else if ("ED".equals(entry.getKey())) {
                        hm.put("edition", entry
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
                        hm.put("isbn", entry
                                .getValue().toString());
                    } else if ("AB".equals(entry.getKey())) {
                        hm.put("abstract", entry
                                .getValue().toString());
                    } else if ("AD".equals(entry.getKey())) {
                        address = entry.getValue()
                                .toString();
                    } else if ("LG".equals(entry.getKey())) {
                        hm.put("language", entry
                                .getValue().toString());
                    } else if ("CO".equals(entry.getKey())) {
                        country = entry.getValue()
                                .toString();
                    } else if ("UR".equals(entry.getKey()) || "AT".equals(entry.getKey())) {
                        String s = entry.getValue().toString().trim();
                        hm.put(s.startsWith("http://") || s.startsWith("ftp://") ? "url"
                                : "pdf", entry.getValue().toString());
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
                        hm.put("annote", entry
                                .getValue().toString());
                    } else if ("CA".equals(entry.getKey())) {
                        comments.add("Categories: "
                                + entry.getValue());
                    } else if ("TH".equals(entry.getKey())) {
                        comments.add("Short Title: "
                                + entry.getValue());
                    } else if ("SE".equals(entry.getKey()))
                    {
                        hm.put("chapter", entry
                                .getValue().toString());
                        //else if (entry.getKey().equals("AC"))
                        // hm.put("",entry.getValue().toString());
                        //else if (entry.getKey().equals("LP"))
                        // hm.put("",entry.getValue().toString());
                    }
                }

                String bibtexType = "misc";
                // to find type, first check TW, then RT
                for (int i = 1; (i >= 0) && "misc".equals(bibtexType); --i) {
                    if (type[i] == null) {
                        continue;
                    }
                    type[i] = type[i].toLowerCase();
                    if (type[i].contains("article")) {
                        bibtexType = "article";
                    } else if (type[i].contains("journal")) {
                        bibtexType = "article";
                    } else if (type[i].contains("book section")) {
                        bibtexType = "inbook";
                    } else if (type[i].contains("book")) {
                        bibtexType = "book";
                    } else if (type[i].contains("conference")) {
                        bibtexType = "inproceedings";
                    } else if (type[i].contains("proceedings")) {
                        bibtexType = "inproceedings";
                    } else if (type[i].contains("report")) {
                        bibtexType = "techreport";
                    } else if (type[i].contains("thesis")
                            && type[i].contains("master")) {
                        bibtexType = "mastersthesis";
                    } else if (type[i].contains("thesis")) {
                        bibtexType = "phdthesis";
                    }
                }

                // depending on bibtexType, decide where to place the titleRT and
                // titleTI
                if ("article".equals(bibtexType)) {
                    if (titleST != null) {
                        hm.put("journal", titleST);
                    }
                    if (titleTI != null) {
                        hm.put("title", titleTI);
                    }
                } else if ("inbook".equals(bibtexType)) {
                    if (titleST != null) {
                        hm.put("booktitle", titleST);
                    }
                    if (titleTI != null) {
                        hm.put("title", titleTI);
                    }
                } else {
                    if (titleST != null) {
                        hm.put("booktitle", titleST); // should not
                    }
                    // happen, I
                    // think
                    if (titleTI != null) {
                        hm.put("title", titleTI);
                    }
                }

                // concatenate pages
                if ((pages[0] != null) || (pages[1] != null)) {
                    hm.put("pages", (pages[0] == null ? "" : pages[0]) + (pages[1] == null ? "" : "--" + pages[1]));
                }

                // concatenate address and country
                if (address != null) {
                    hm.put("address", address + (country == null ? "" : ", " + country));
                }

                if (!comments.isEmpty()) { // set comment if present
                    hm.put("comment", String.join(";", comments));
                }
                BibEntry b = new BibEntry(DEFAULT_BIBTEXENTRY_ID, bibtexType);
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
                return Collections.emptyList();
            }
            previousLine.append(line.trim());
        }

        return bibItems;
    }

}
