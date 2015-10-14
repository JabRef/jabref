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
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.model.entry.AuthorList;
import net.sf.jabref.model.entry.BibtexEntryTypes;

/**
 * Importer for the MEDLINE Plain format.
 * 
 * check here for details on the format
 * http://www.nlm.nih.gov/bsd/mms/medlineelements.html
 * 
 * @author vegeziel
 */
public class MedlinePlainImporter extends ImportFormat {

    /**
     * Return the name of this import format.
     */
    @Override
    public String getFormatName() {
        return "MedlinePlain";
    }

    /*
     *  (non-Javadoc)
     * @see net.sf.jabref.imports.ImportFormat#getCLIId()
     */
    @Override
    public String getCLIId() {
        return "medlineplain";
    }

    /**
     * Check whether the source is in the correct format for this importer.
     */
    @Override
    public boolean isRecognizedFormat(InputStream stream) throws IOException {

        // Our strategy is to look for the "PMID  - *", "PMC.*-.*", or "PMCR.*-.*" line 
        // (i.e., PubMed Unique Identifier, PubMed Central Identifier, PubMed Central Release)
        BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));
        Pattern pat1 = Pattern.compile("PMID.*-.*");
        Pattern pat2 = Pattern.compile("PMC.*-.*");
        Pattern pat3 = Pattern.compile("PMCR.*-.*");

        String str;
        while ((str = in.readLine()) != null) {
            if (pat1.matcher(str).find() || pat2.matcher(str).find() || pat3.matcher(str).find()) {
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
        ArrayList<BibtexEntry> bibitems = new ArrayList<BibtexEntry>();
        StringBuilder sb = new StringBuilder();
        BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));
        String str;
        while ((str = in.readLine()) != null) {
            sb.append(str);
            sb.append("\n");
        }
        String[] entries = sb.toString().replaceAll("\u2013", "-").replaceAll("\u2014", "--").replaceAll("\u2015", "--").split("\\n\\n");

        for (String entry1 : entries) {

            if (entry1.trim().isEmpty()) {
                continue;
            }

            String type = "";
            String author = "";
            String editor = "";
            String comment = "";
            HashMap<String, String> hm = new HashMap<String, String>();

            String[] fields = entry1.split("\n");

            for (int j = 0; j < fields.length; j++) {
                if (fields[j].equals("")) {
                    continue;
                }

                StringBuilder current = new StringBuilder(fields[j]);
                boolean done = false;

                while (!done && j < fields.length - 1) {
                    if (fields[j + 1].length() <= 4) {
                        System.out.println("aaa");
                    }
                    if (fields[j + 1].charAt(4) != '-') {
                        if (current.length() > 0
                                && !Character.isWhitespace(current.charAt(current.length() - 1))) {
                            current.append(' ');
                        }
                        current.append(fields[j + 1].trim());
                        j++;
                    } else {
                        done = true;
                    }
                }
                String entry = current.toString();

                String lab = entry.substring(0, entry.indexOf('-')).trim();
                String val = entry.substring(entry.indexOf('-') + 1).trim();
                if (lab.equals("PT")) {
                    val = val.toLowerCase();
                    if (val.equals("BOOK")) {
                        type = "book";
                    } else if (val.equals("journal article")
                            || val.equals("classical article")
                            || val.equals("corrected and republished article")
                            || val.equals("historical article")
                            || val.equals("introductory journal article")
                            || val.equals("newspaper article")) {
                        type = "article";
                    } else if (val.equals("clinical conference")
                            || val.equals("consensus development conference")
                            || val.equals("consensus development conference, NIH")) {
                        type = "conference";
                    } else if (val.equals("technical report")) {
                        type = "techreport";
                    } else if (val.equals("editorial")) {
                        type = "inproceedings";//"incollection";"inbook";
                    } else if (val.equals("overall")) {
                        type = "proceedings";
                    } else if (type.equals("")) {
                        type = "other";
                    }

                } else if (lab.equals("TI")) {
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
                else if (lab.equals("BTI") || lab.equals("CTI")) {
                    hm.put("booktitle", val);
                } else if (lab.equals("FAU")) {
                    if (author.equals("")) {
                        author = val;
                    } else {
                        author += " and " + val;
                    }
                } else if (lab.equals("FED")) {
                    if (editor.equals("")) {
                        editor = val;
                    } else {
                        editor += " and " + val;
                    }
                } else if (lab.equals("JT")) {
                    if (type.equals("inproceedings")) {
                        hm.put("booktitle", val);
                    } else {
                        hm.put("journal", val);
                    }
                } else if (lab.equals("PG")) {
                    hm.put("pages", val);
                } else if (lab.equals("PL")) {
                    hm.put("address", val);
                } else if (lab.equals("IS")) {
                    hm.put("issn", val);
                } else if (lab.equals("VI")) {
                    hm.put("volume", val);
                } else if (lab.equals("AB")) {
                    String oldAb = hm.get("abstract");
                    if (oldAb == null) {
                        hm.put("abstract", val);
                    } else {
                        hm.put("abstract", oldAb + "\n" + val);
                    }
                } else if (lab.equals("DP")) {
                    String[] parts = val.split(" ");
                    hm.put("year", parts[0]);
                    if (parts.length > 1 && !parts[1].isEmpty()) {
                        hm.put("month", parts[1]);
                    }
                } else if (lab.equals("MH") || lab.equals("OT")) {
                    if (!hm.containsKey("keywords")) {
                        hm.put("keywords", val);
                    } else {
                        String kw = hm.get("keywords");
                        hm.put("keywords", kw + ", " + val);
                    }
                } else if (lab.equals("CON") || lab.equals("CIN") || lab.equals("EIN")
                        || lab.equals("EFR") || lab.equals("CRI") || lab.equals("CRF")
                        || lab.equals("PRIN") || lab.equals("PROF") || lab.equals("RPI")
                        || lab.equals("RPF") || lab.equals("RIN") || lab.equals("ROF")
                        || lab.equals("UIN") || lab.equals("UOF") || lab.equals("SPIN")
                        || lab.equals("ORI")) {
                    if (!comment.isEmpty()) {
                        comment = comment + "\n";
                    }
                    comment = comment + val;
                }
                //                // Added ID import 2005.12.01, Morten Alver:
                //                else if (lab.equals("ID"))
                //                    hm.put("refid", val);
                //                    // Added doi import (sciencedirect.com) 2011.01.10, Alexander Hug <alexander@alexanderhug.info>
                else if (lab.equals("AID")) {
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

            BibtexEntry b = new BibtexEntry(DEFAULT_BIBTEXENTRY_ID, BibtexEntryTypes
                    .getEntryType(type)); // id assumes an existing database so don't

            // Remove empty fields:
            ArrayList<Object> toRemove = new ArrayList<Object>();
            for (String key : hm.keySet()) {
                String content = hm.get(key);
                if (content == null || content.trim().isEmpty()) {
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
