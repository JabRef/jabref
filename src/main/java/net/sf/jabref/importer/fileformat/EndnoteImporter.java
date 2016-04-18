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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import net.sf.jabref.importer.ImportFormatReader;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.logic.labelpattern.LabelPatternUtil;
import net.sf.jabref.model.entry.AuthorList;
import net.sf.jabref.model.entry.BibEntry;

/**
 * Importer for the Refer/Endnote format.
 * modified to use article number for pages if pages are missing (some
 * journals, e.g., Physical Review Letters, don't use pages anymore)
 *
 * check here for details on the format
 * http://www.ecst.csuchico.edu/~jacobsd/bib/formats/endnote.html
 */
public class EndnoteImporter extends ImportFormat {

    private static final String ENDOFRECORD = "__EOREOR__";

    private static final Pattern A_PATTERN = Pattern.compile("%A .*");
    private static final Pattern E_PATTERN = Pattern.compile("%E .*");


    /**
     * Return the name of this import format.
     */
    @Override
    public String getFormatName() {
        return "Refer/Endnote";
    }

    /*
     *  (non-Javadoc)
     * @see net.sf.jabref.imports.ImportFormat#getCLIId()
     */
    @Override
    public String getCLIId() {
        return "refer";
    }

    /**
     * Check whether the source is in the correct format for this importer.
     */
    @Override
    public boolean isRecognizedFormat(InputStream stream) throws IOException {

        // Our strategy is to look for the "%A *" line.
        try (BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream))) {
            String str;
            while ((str = in.readLine()) != null) {
                if (A_PATTERN.matcher(str).matches() || E_PATTERN.matcher(str).matches()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Parse the entries in the source, and return a List of BibEntry
     * objects.
     */
    @Override
    public List<BibEntry> importEntries(InputStream stream, OutputPrinter status) throws IOException {
        List<BibEntry> bibitems = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream))) {

            String str;
            boolean first = true;
            while ((str = in.readLine()) != null) {
                str = str.trim();
                if (str.indexOf("%0") == 0) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(ENDOFRECORD);
                    }
                    sb.append(str);
                } else {
                    sb.append(str);
                }
                sb.append('\n');
            }
        }

        String[] entries = sb.toString().split(ENDOFRECORD);
        Map<String, String> hm = new HashMap<>();
        String author;
        String type;
        String editor;
        String artnum;
        for (String entry : entries) {
            hm.clear();
            author = "";
            type = "misc";
            editor = "";
            artnum = "";

            boolean isEditedBook = false;
            String[] fields = entry.trim().substring(1).split("\n%");
            for (String field : fields) {

                if (field.length() < 3) {
                    continue;
                }

                /*
                 * Details of Refer format for Journal Article and Book:
                 *
                 * Generic Ref Journal Article Book Code Author %A Author Author Year %D
                 * Year Year Title %T Title Title Secondary Author %E Series Editor
                 * Secondary Title %B Journal Series Title Place Published %C City
                 * Publisher %I Publisher Volume %V Volume Volume Number of Volumes %6
                 * Number of Volumes Number %N Issue Pages %P Pages Number of Pages
                 * Edition %7 Edition Subsidiary Author %? Translator Alternate Title %J
                 * Alternate Journal Label %F Label Label Keywords %K Keywords Keywords
                 * Abstract %X Abstract Abstract Notes %O Notes Notes
                 */

                String prefix = field.substring(0, 1);

                String val = field.substring(2);

                if ("A".equals(prefix)) {
                    if ("".equals(author)) {
                        author = val;
                    } else {
                        author += " and " + val;
                    }
                } else if ("E".equals(prefix)) {
                    if ("".equals(editor)) {
                        editor = val;
                    } else {
                        editor += " and " + val;
                    }
                } else if ("T".equals(prefix)) {
                    hm.put("title", val);
                } else if ("0".equals(prefix)) {
                    if (val.indexOf("Journal") == 0) {
                        type = "article";
                    } else if (val.indexOf("Book Section") == 0) {
                        type = "incollection";
                    } else if (val.indexOf("Book") == 0) {
                        type = "book";
                    } else if (val.indexOf("Edited Book") == 0) {
                        type = "book";
                        isEditedBook = true;
                    } else if (val.indexOf("Conference") == 0) {
                        type = "inproceedings";
                    } else if (val.indexOf("Report") == 0) {
                        type = "techreport";
                    } else if (val.indexOf("Review") == 0) {
                        type = "article";
                    } else if (val.indexOf("Thesis") == 0) {
                        type = "phdthesis";
                    } else {
                        type = "misc"; //
                    }
                } else if ("7".equals(prefix)) {
                    hm.put("edition", val);
                } else if ("C".equals(prefix)) {
                    hm.put("address", val);
                } else if ("D".equals(prefix)) {
                    hm.put("year", val);
                } else if ("8".equals(prefix)) {
                    hm.put("date", val);
                } else if ("J".equals(prefix)) {
                    // "Alternate journal. Let's set it only if no journal
                    // has been set with %B.
                    if (hm.get("journal") == null) {
                        hm.put("journal", val);
                    }
                } else if ("B".equals(prefix)) {
                    // This prefix stands for "journal" in a journal entry, and
                    // "series" in a book entry.
                    if ("article".equals(type)) {
                        hm.put("journal", val);
                    } else if ("book".equals(type) || "inbook".equals(type)) {
                        hm.put("series", val);
                    } else {
                        /* type = inproceedings */
                        hm.put("booktitle", val);
                    }
                } else if ("I".equals(prefix)) {
                    if ("phdthesis".equals(type)) {
                        hm.put("school", val);
                    } else {
                        hm.put("publisher", val);
                    }
                }
                // replace single dash page ranges (23-45) with double dashes (23--45):
                else if ("P".equals(prefix)) {
                    hm.put("pages", val.replaceAll("([0-9]) *- *([0-9])", "$1--$2"));
                } else if ("V".equals(prefix)) {
                    hm.put("volume", val);
                } else if ("N".equals(prefix)) {
                    hm.put("number", val);
                } else if ("U".equals(prefix)) {
                    hm.put("url", val);
                } else if ("R".equals(prefix)) {
                    String doi = val;
                    if (doi.startsWith("doi:")) {
                        doi = doi.substring(4);
                    }
                    hm.put("doi", doi);
                } else if ("O".equals(prefix)) {
                    // Notes may contain Article number
                    if (val.startsWith("Artn")) {
                        String[] tokens = val.split("\\s");
                        artnum = tokens[1];
                    } else {
                        hm.put("note", val);
                    }
                } else if ("K".equals(prefix)) {
                    hm.put("keywords", val);
                } else if ("X".equals(prefix)) {
                    hm.put("abstract", val);
                } else if ("9".equals(prefix)) {
                    if (val.indexOf("Ph.D.") == 0) {
                        type = "phdthesis";
                    }
                    if (val.indexOf("Masters") == 0) {
                        type = "mastersthesis";
                    }
                } else if ("F".equals(prefix)) {
                    hm.put(BibEntry.KEY_FIELD, LabelPatternUtil.checkLegalKey(val));
                }
            }

            // For Edited Book, EndNote puts the editors in the author field.
            // We want them in the editor field so that bibtex knows it's an edited book
            if (isEditedBook && "".equals(editor)) {
                editor = author;
                author = "";
            }

            //fixauthorscomma
            if (!"".equals(author)) {
                hm.put("author", fixAuthor(author));
            }
            if (!"".equals(editor)) {
                hm.put("editor", fixAuthor(editor));
            }
            //if pages missing and article number given, use the article number
            if (((hm.get("pages") == null) || "-".equals(hm.get("pages"))) && !"".equals(artnum)) {
                hm.put("pages", artnum);
            }

            BibEntry b = new BibEntry(DEFAULT_BIBTEXENTRY_ID, type); // id assumes an existing database so don't
            // create one here
            b.setField(hm);
            if (!b.getFieldNames().isEmpty()) {
                bibitems.add(b);
            }

        }

        return bibitems;

    }

    /**
     * We must be careful about the author names, since they can be presented differently
     * by different sources. Normally each %A tag brings one name, and we get the authors
     * separated by " and ". This is the correct behaviour.
     * One source lists the names separated by comma, with a comma at the end. We can detect
     * this format and fix it.
     * @param s The author string
     * @return The fixed author string
     */
    private static String fixAuthor(String s) {
        int index = s.indexOf(" and ");
        if (index >= 0) {
            return AuthorList.fixAuthorLastNameFirst(s);
        }
        // Look for the comma at the end:
        index = s.lastIndexOf(',');
        if (index == (s.length() - 1)) {
            String mod = s.substring(0, s.length() - 1).replace(", ", " and ");
            return AuthorList.fixAuthorLastNameFirst(mod);
        } else {
            return AuthorList.fixAuthorLastNameFirst(s);
        }
    }

}
