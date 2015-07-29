/*  Copyright (C) 2003-2011 JabRef contributors.
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
package net.sf.jabref.imports;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import net.sf.jabref.*;
import net.sf.jabref.util.Util;

/**
 * Importer for the Refer/Endnote format.
 * modified to use article number for pages if pages are missing (some
 * journals, e.g., Physical Review Letters, don't use pages anymore)
 *
 * check here for details on the format
 * http://www.ecst.csuchico.edu/~jacobsd/bib/formats/endnote.html
 */
public class EndnoteImporter extends ImportFormat {

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
        BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));
        Pattern pat1 = Pattern.compile("%A .*");
        Pattern pat2 = Pattern.compile("%E .*");
        String str;
        while ((str = in.readLine()) != null) {
            if (pat1.matcher(str).matches() || pat2.matcher(str).matches()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parse the entries in the source, and return a List of BibtexEntry
     * objects.
     */
    @Override
    public List<BibtexEntry> importEntries(InputStream stream, OutputPrinter status) throws IOException {
        ArrayList<BibtexEntry> bibitems = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));
        String ENDOFRECORD = "__EOREOR__";

        String str;
        boolean first = true;
        while ((str = in.readLine()) != null) {
            str = str.trim();
            // if(str.equals("")) continue;
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
            sb.append("\n");
        }

        String[] entries = sb.toString().split(ENDOFRECORD);
        HashMap<String, String> hm = new HashMap<>();
        String author;
        String Type;
        String editor;
        String artnum;
        for (String entry : entries) {
            hm.clear();
            author = "";
            Type = "";
            editor = "";
            artnum = "";

            boolean IsEditedBook = false;
            String[] fields = entry.trim().substring(1).split("\n%");
            //String lastPrefix = "";
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

                switch (prefix) {
                case "A":
                    if (author.equals("")) {
                        author = val;
                    } else {
                        author += " and " + val;
                    }
                    break;
                case "E":
                    if (editor.equals("")) {
                        editor = val;
                    } else {
                        editor += " and " + val;
                    }
                    break;
                case "T":
                    hm.put("title", val);
                    break;
                case "0":
                    if (val.indexOf("Journal") == 0) {
                        Type = "article";
                    } else if (val.indexOf("Book Section") == 0) {
                        Type = "incollection";
                    } else if (val.indexOf("Book") == 0) {
                        Type = "book";
                    } else if (val.indexOf("Edited Book") == 0) {
                        Type = "book";
                        IsEditedBook = true;
                    } else if (val.indexOf("Conference") == 0) {
                        Type = "inproceedings";
                    } else if (val.indexOf("Report") == 0) {
                        Type = "techreport";
                    } else if (val.indexOf("Review") == 0) {
                        Type = "article";
                    } else if (val.indexOf("Thesis") == 0) {
                        Type = "phdthesis";
                    } else {
                        Type = "misc"; //
                    }
                    break;
                case "7":
                    hm.put("edition", val);
                    break;
                case "C":
                    hm.put("address", val);
                    break;
                case "D":
                    hm.put("year", val);
                    break;
                case "8":
                    hm.put("date", val);
                    break;
                case "J":
                    // "Alternate journal. Let's set it only if no journal
                    // has been set with %B.
                    if (hm.get("journal") == null) {
                        hm.put("journal", val);
                    }
                    break;
                case "B":
                    // This prefix stands for "journal" in a journal entry, and
                    // "series" in a book entry.
                    switch (Type) {
                    case "article":
                        hm.put("journal", val);
                        break;
                    case "book":
                    case "inbook":
                        hm.put(
                                "series", val);
                        break;
                    default:
                        /* if (Type.equals("inproceedings")) */
                        hm.put("booktitle", val);
                        break;
                    }
                    break;
                case "I":
                    if (Type.equals("phdthesis")) {
                        hm.put("school", val);
                    } else {
                        hm.put("publisher", val);
                    }
                    break;
                // replace single dash page ranges (23-45) with double dashes (23--45):
                case "P":
                    hm.put("pages", val.replaceAll("([0-9]) *- *([0-9])", "$1--$2"));
                    break;
                case "V":
                    hm.put("volume", val);
                    break;
                case "N":
                    hm.put("number", val);
                    break;
                case "U":
                    hm.put("url", val);
                    break;
                case "R":
                    String doi = val;
                    if (doi.startsWith("doi:")) {
                        doi = doi.substring(4);
                    }
                    hm.put("doi", doi);
                    break;
                case "O":
                    // Notes may contain Article number
                    if (val.startsWith("Artn")) {
                        String[] tokens = val.split("\\s");
                        artnum = tokens[1];
                    } else {
                        hm.put("note", val);
                    }
                    break;
                case "K":
                    hm.put("keywords", val);
                    break;
                case "X":
                    hm.put("abstract", val);
                    break;
                case "9":
                    //Util.pr(val);
                    if (val.indexOf("Ph.D.") == 0) {
                        Type = "phdthesis";
                    }
                    if (val.indexOf("Masters") == 0) {
                        Type = "mastersthesis";
                    }
                    break;
                case "F":
                    hm.put(BibtexFields.KEY_FIELD, Util
                            .checkLegalKey(val));
                    break;
                }
            }

            // For Edited Book, EndNote puts the editors in the author field.
            // We want them in the editor field so that bibtex knows it's an edited book
            if (IsEditedBook && editor.equals("")) {
                editor = author;
                author = "";
            }

            //fixauthorscomma
            if (!author.equals("")) {
                hm.put("author", fixAuthor(author));
            }
            if (!editor.equals("")) {
                hm.put("editor", fixAuthor(editor));
            }
            //if pages missing and article number given, use the article number
            if ((hm.get("pages") == null || hm.get("pages").equals("-")) && !artnum.equals("")) {
                hm.put("pages", artnum);
            }

            BibtexEntry b = new BibtexEntry(BibtexFields.DEFAULT_BIBTEXENTRY_ID, Globals
                    .getEntryType(Type)); // id assumes an existing database so don't
            // create one here
            b.setField(hm);
            //if (hm.isEmpty())
            if (!b.getAllFields().isEmpty()) {
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
    private String fixAuthor(String s) {
        int index = s.indexOf(" and ");
        if (index >= 0) {
            return AuthorList.fixAuthor_lastNameFirst(s);
        }
        // Look for the comma at the end:
        index = s.lastIndexOf(",");
        if (index == s.length() - 1) {
            String mod = s.substring(0, s.length() - 1).replaceAll(", ", " and ");
            return AuthorList.fixAuthor_lastNameFirst(mod);
        } else {
            return AuthorList.fixAuthor_lastNameFirst(s);
        }
    }

}
