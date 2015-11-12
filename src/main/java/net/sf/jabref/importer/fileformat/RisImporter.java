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

import java.util.regex.Pattern;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import net.sf.jabref.importer.ImportFormatReader;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.model.entry.AuthorList;
import net.sf.jabref.model.entry.MonthUtil;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;

/**
 * Imports a Biblioscape Tag File. The format is described on
 * http://www.biblioscape.com/manual_bsp/Biblioscape_Tag_File.htm Several
 * Biblioscape field types are ignored. Others are only included in the BibTeX
 * field "comment".
 */
public class RisImporter extends ImportFormat {

    /**
     * Return the name of this import format.
     */
    @Override
    public String getFormatName() {
        return "RIS";
    }

    /*
     *  (non-Javadoc)
     * @see net.sf.jabref.imports.ImportFormat#getCLIId()
     */
    @Override
    public String getCLIId() {
        return "ris";
    }

    /**
     * Check whether the source is in the correct format for this importer.
     */
    @Override
    public boolean isRecognizedFormat(InputStream stream) throws IOException {

        // Our strategy is to look for the "AU  - *" line.
        BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));
        Pattern pat1 = Pattern.compile("TY  - .*");

        String str;
        while ((str = in.readLine()) != null) {
            if (pat1.matcher(str).find()) {
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
        String str;
        while ((str = in.readLine()) != null) {
            sb.append(str);
            sb.append("\n");
        }
        String[] entries = sb.toString().replaceAll("\u2013", "-").replaceAll("\u2014", "--").replaceAll("\u2015", "--").split("ER  -.*\\n");

        for (String entry1 : entries) {

            if (entry1.trim().isEmpty()) {
                continue;
            }

            String type = "";
            String author = "";
            String editor = "";
            String startPage = "";
            String endPage = "";
            String comment = "";
            HashMap<String, String> hm = new HashMap<>();

            String[] fields = entry1.split("\n");

            for (int j = 0; j < fields.length; j++) {
                StringBuilder current = new StringBuilder(fields[j]);
                boolean done = false;
                while (!done && (j < (fields.length - 1))) {
                    if ((fields[j + 1].length() >= 6) && !fields[j + 1].substring(2, 6).equals("  - ")) {
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
                    if (lab.equals("TY")) {
                        if (val.equals("BOOK")) {
                            type = "book";
                        } else if (val.equals("JOUR") || val.equals("MGZN")) {
                            type = "article";
                        } else if (val.equals("THES")) {
                            type = "phdthesis";
                        } else if (val.equals("UNPB")) {
                            type = "unpublished";
                        } else if (val.equals("RPRT")) {
                            type = "techreport";
                        } else if (val.equals("CONF")) {
                            type = "inproceedings";
                        } else if (val.equals("CHAP")) {
                            type = "incollection";//"inbook";
                        } else {
                            type = "other";
                        }
                    } else if (lab.equals("T1") || lab.equals("TI")) {
                        String oldVal = hm.get("title");
                        if (oldVal == null) {
                            hm.put("title", val);
                        } else {
                            if (oldVal.endsWith(":") || oldVal.endsWith(".") || oldVal.endsWith("?")) {
                                hm.put("title", oldVal + " " + val);
                            } else {
                                hm.put("title", oldVal + ": " + val);
                            }
                        }
                    }
                    // =
                    // val;
                    else if (lab.equals("T2") || lab.equals("T3") || lab.equals("BT")) {
                        hm.put("booktitle", val);
                    } else if (lab.equals("AU") || lab.equals("A1")) {
                        if (author.equals("")) {
                            author = val;
                        } else {
                            author += " and " + val;
                        }
                    } else if (lab.equals("A2")) {
                        if (editor.equals("")) {
                            editor = val;
                        } else {
                            editor += " and " + val;
                        }
                    } else if (lab.equals("JA") || lab.equals("JF") || lab.equals("JO")) {
                        if (type.equals("inproceedings")) {
                            hm.put("booktitle", val);
                        } else {
                            hm.put("journal", val);
                        }
                    } else if (lab.equals("SP")) {
                        startPage = val;
                    } else if (lab.equals("PB")) {
                        if (type.equals("phdthesis")) {
                            hm.put("school", val);
                        } else {
                            hm.put("publisher", val);
                        }
                    } else if (lab.equals("AD") || lab.equals("CY")) {
                        hm.put("address", val);
                    } else if (lab.equals("EP")) {
                        endPage = val;
                    } else if (lab.equals("SN")) {
                        hm.put("issn", val);
                    } else if (lab.equals("VL")) {
                        hm.put("volume", val);
                    } else if (lab.equals("IS")) {
                        hm.put("number", val);
                    } else if (lab.equals("N2") || lab.equals("AB")) {
                        String oldAb = hm.get("abstract");
                        if (oldAb == null) {
                            hm.put("abstract", val);
                        } else {
                            hm.put("abstract", oldAb + "\n" + val);
                        }
                    } else if (lab.equals("UR")) {
                        hm.put("url", val);
                    } else if ((lab.equals("Y1") || lab.equals("PY")) && (val.length() >= 4)) {
                        String[] parts = val.split("/");
                        hm.put("year", parts[0]);
                        if ((parts.length > 1) && !parts[1].isEmpty()) {
                            try {

                                int monthNumber = Integer.parseInt(parts[1]);
                                MonthUtil.Month month = MonthUtil.getMonthByNumber(monthNumber);
                                if (month.isValid()) {
                                    hm.put("month", month.bibtexFormat);
                                }
                            } catch (NumberFormatException ex) {
                                // The month part is unparseable, so we ignore it.
                            }
                        }
                    } else if (lab.equals("KW")) {
                        if (!hm.containsKey("keywords")) {
                            hm.put("keywords", val);
                        } else {
                            String kw = hm.get("keywords");
                            hm.put("keywords", kw + ", " + val);
                        }
                    } else if (lab.equals("U1") || lab.equals("U2") || lab.equals("N1")) {
                        if (!comment.isEmpty()) {
                            comment = comment + "\n";
                        }
                        comment = comment + val;
                    }
                    // Added ID import 2005.12.01, Morten Alver:
                    else if (lab.equals("ID")) {
                        hm.put("refid", val);
                    } else if (lab.equals("M3")) {
                        String doi = val;
                        if (doi.startsWith("doi:")) {
                            doi = doi.replaceAll("(?i)doi:", "").trim();
                            hm.put("doi", doi);
                        }
                    }
                }
                // fix authors
                if (!author.isEmpty()) {
                    author = AuthorList.fixAuthor_lastNameFirst(author);
                    hm.put("author", author);
                }
                if (!editor.isEmpty()) {
                    editor = AuthorList.fixAuthor_lastNameFirst(editor);
                    hm.put("editor", editor);
                }
                if (!comment.isEmpty()) {
                    hm.put("comment", comment);
                }

                hm.put("pages", startPage + "--" + endPage);
            }
            BibtexEntry b = new BibtexEntry(DEFAULT_BIBTEXENTRY_ID, BibtexEntryTypes
                    .getEntryType(type)); // id assumes an existing database so don't

            // Remove empty fields:
            ArrayList<Object> toRemove = new ArrayList<>();
            for (String key : hm.keySet()) {
                String content = hm.get(key);
                if ((content == null) || content.trim().isEmpty()) {
                    toRemove.add(key);
                }
            }
            for (Object aToRemove : toRemove) {
                hm.remove(aToRemove);

            }

            // create one here
            b.setField(hm);
            bibitems.add(b);

        }

        return bibitems;

    }
}
