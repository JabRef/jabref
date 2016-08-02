/*  Copyright (C) 2003-2016 JabRef contributors.
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.model.entry.AuthorList;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.entry.MonthUtil;

/**
 * Imports a Biblioscape Tag File. The format is described on
 * http://www.biblioscape.com/manual_bsp/Biblioscape_Tag_File.htm
 * Several Biblioscape field types are ignored. Others are only included in the BibTeX
 * field "comment".
 */
public class RisImporter extends ImportFormat {

    private static final Pattern RECOGNIZED_FORMAT_PATTERN = Pattern.compile("TY  - .*");

    @Override
    public String getFormatName() {
        return "RIS";
    }

    @Override
    public List<String> getExtensions() {
        return Collections.singletonList(".ris");
    }

    @Override
    public String getDescription() {
        return "Imports a Biblioscape Tag File.";
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader reader) throws IOException {
        // Our strategy is to look for the "AU  - *" line.
        String str;
        while ((str = reader.readLine()) != null) {
            if (RECOGNIZED_FORMAT_PATTERN.matcher(str).find()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ParserResult importDatabase(BufferedReader reader) throws IOException {
        List<BibEntry> bibitems = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
            sb.append('\n');
        }

        String[] entries = sb.toString().replace("\u2013", "-").replace("\u2014", "--").replace("\u2015", "--")
                .split("ER  -.*\\n");

        for (String entry1 : entries) {

            String type = "";
            String author = "";
            String editor = "";
            String startPage = "";
            String endPage = "";
            String comment = "";
            Map<String, String> hm = new HashMap<>();

            String[] fields = entry1.split("\n");

            for (int j = 0; j < fields.length; j++) {
                StringBuilder current = new StringBuilder(fields[j]);
                boolean done = false;
                while (!done && (j < (fields.length - 1))) {
                    if ((fields[j + 1].length() >= 6) && !"  - ".equals(fields[j + 1].substring(2, 6))) {
                        if ((current.length() > 0)
                                && !Character.isWhitespace(current.charAt(current.length() - 1))
                                && !Character.isWhitespace(fields[j + 1].charAt(0))) {
                            current.append(' ');
                        }
                        current.append(fields[j + 1]);
                        j++;
                    } else {
                        done = true;
                    }
                }
                String entry = current.toString();
                if (entry.length() < 6) {
                    continue;
                } else {
                    String lab = entry.substring(0, 2);
                    String val = entry.substring(6).trim();
                    if ("TY".equals(lab)) {
                        if ("BOOK".equals(val)) {
                            type = "book";
                        } else if ("JOUR".equals(val) || "MGZN".equals(val)) {
                            type = "article";
                        } else if ("THES".equals(val)) {
                            type = "phdthesis";
                        } else if ("UNPB".equals(val)) {
                            type = "unpublished";
                        } else if ("RPRT".equals(val)) {
                            type = "techreport";
                        } else if ("CONF".equals(val)) {
                            type = "inproceedings";
                        } else if ("CHAP".equals(val)) {
                            type = "incollection";//"inbook";
                        } else {
                            type = "other";
                        }
                    } else if ("T1".equals(lab) || "TI".equals(lab)) {
                        String oldVal = hm.get(FieldName.TITLE);
                        if (oldVal == null) {
                            hm.put(FieldName.TITLE, val);
                        } else {
                            if (oldVal.endsWith(":") || oldVal.endsWith(".") || oldVal.endsWith("?")) {
                                hm.put(FieldName.TITLE, oldVal + " " + val);
                            } else {
                                hm.put(FieldName.TITLE, oldVal + ": " + val);
                            }
                        }
                        hm.put(FieldName.TITLE, hm.get(FieldName.TITLE).replaceAll("\\s+", " ")); // Normalize whitespaces
                    } else if ("T2".equals(lab) || "BT".equals(lab)) {
                        hm.put(FieldName.BOOKTITLE, val);
                    } else if ("T3".equals(lab)) {
                        hm.put(FieldName.SERIES, val);
                    } else if ("AU".equals(lab) || "A1".equals(lab)) {
                        if ("".equals(author)) {
                            author = val;
                        } else {
                            author += " and " + val;
                        }
                    } else if ("A2".equals(lab)) {
                        if ("".equals(editor)) {
                            editor = val;
                        } else {
                            editor += " and " + val;
                        }
                    } else if ("JA".equals(lab) || "JF".equals(lab) || "JO".equals(lab)) {
                        if ("inproceedings".equals(type)) {
                            hm.put(FieldName.BOOKTITLE, val);
                        } else {
                            hm.put(FieldName.JOURNAL, val);
                        }
                    } else if ("SP".equals(lab)) {
                        startPage = val;
                    } else if ("PB".equals(lab)) {
                        if ("phdthesis".equals(type)) {
                            hm.put(FieldName.SCHOOL, val);
                        } else {
                            hm.put(FieldName.PUBLISHER, val);
                        }
                    } else if ("AD".equals(lab) || "CY".equals(lab)) {
                        hm.put(FieldName.ADDRESS, val);
                    } else if ("EP".equals(lab)) {
                        endPage = val;
                        if (!endPage.isEmpty()) {
                            endPage = "--" + endPage;
                        }
                    } else if ("SN".equals(lab)) {
                        hm.put(FieldName.ISSN, val);
                    } else if ("VL".equals(lab)) {
                        hm.put(FieldName.VOLUME, val);
                    } else if ("IS".equals(lab)) {
                        hm.put(FieldName.NUMBER, val);
                    } else if ("N2".equals(lab) || "AB".equals(lab)) {
                        String oldAb = hm.get(FieldName.ABSTRACT);
                        if (oldAb == null) {
                            hm.put(FieldName.ABSTRACT, val);
                        } else {
                            hm.put(FieldName.ABSTRACT, oldAb + OS.NEWLINE + val);
                        }
                    } else if ("UR".equals(lab)) {
                        hm.put(FieldName.URL, val);
                    } else if (("Y1".equals(lab) || "PY".equals(lab)) && (val.length() >= 4)) {
                        String[] parts = val.split("/");
                        hm.put(FieldName.YEAR, parts[0]);
                        if ((parts.length > 1) && !parts[1].isEmpty()) {
                            try {

                                int monthNumber = Integer.parseInt(parts[1]);
                                MonthUtil.Month month = MonthUtil.getMonthByNumber(monthNumber);
                                if (month.isValid()) {
                                    hm.put(FieldName.MONTH, month.bibtexFormat);
                                }
                            } catch (NumberFormatException ex) {
                                // The month part is unparseable, so we ignore it.
                            }
                        }
                    } else if ("KW".equals(lab)) {
                        if (hm.containsKey(FieldName.KEYWORDS)) {
                            String kw = hm.get(FieldName.KEYWORDS);
                            hm.put(FieldName.KEYWORDS, kw + ", " + val);
                        } else {
                            hm.put(FieldName.KEYWORDS, val);
                        }
                    } else if ("U1".equals(lab) || "U2".equals(lab) || "N1".equals(lab)) {
                        if (!comment.isEmpty()) {
                            comment = comment + " ";
                        }
                        comment = comment + val;
                    }
                    // Added ID import 2005.12.01, Morten Alver:
                    else if ("ID".equals(lab)) {
                        hm.put("refid", val);
                    } else if ("M3".equals(lab)) {
                        String doi = val;
                        if (doi.startsWith("doi:")) {
                            doi = doi.replaceAll("(?i)doi:", "").trim();
                            hm.put(FieldName.DOI, doi);
                        }
                    }
                }
                // fix authors
                if (!author.isEmpty()) {
                    author = AuthorList.fixAuthorLastNameFirst(author);
                    hm.put(FieldName.AUTHOR, author);
                }
                if (!editor.isEmpty()) {
                    editor = AuthorList.fixAuthorLastNameFirst(editor);
                    hm.put(FieldName.EDITOR, editor);
                }
                if (!comment.isEmpty()) {
                    hm.put("comment", comment);
                }

                hm.put(FieldName.PAGES, startPage + endPage);
            }
            BibEntry b = new BibEntry(DEFAULT_BIBTEXENTRY_ID, type); // id assumes an existing database so don't

            // Remove empty fields:
            List<String> toRemove = new ArrayList<>();
            for (Map.Entry<String, String> key : hm.entrySet()) {
                String content = key.getValue();
                if ((content == null) || content.trim().isEmpty()) {
                    toRemove.add(key.getKey());
                }
            }
            for (String aToRemove : toRemove) {
                hm.remove(aToRemove);

            }

            // create one here
            b.setField(hm);
            bibitems.add(b);

        }

        return new ParserResult(bibitems);

    }
}
