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

import net.sf.jabref.Globals;
import net.sf.jabref.importer.ImportFormatReader;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.model.entry.AuthorList;
import net.sf.jabref.model.entry.BibEntry;

/**
 * Importer for the MEDLINE Plain format.
 *
 * check here for details on the format
 * http://www.nlm.nih.gov/bsd/mms/medlineelements.html
 *
 * @author vegeziel
 */
public class MedlinePlainImporter extends ImportFormat {

    private static final Pattern PMID_PATTERN = Pattern.compile("PMID.*-.*");
    private static final Pattern PMC_PATTERN = Pattern.compile("PMC.*-.*");
    private static final Pattern PMCR_PATTERN = Pattern.compile("PMCR.*-.*");


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
        try (BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream))) {

            String str;
            while ((str = in.readLine()) != null) {
                if (PMID_PATTERN.matcher(str).find() || PMC_PATTERN.matcher(str).find()
                        || PMCR_PATTERN.matcher(str).find()) {
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
            while ((str = in.readLine()) != null) {
                sb.append(str);
                sb.append('\n');
            }
        }
        String[] entries = sb.toString().replace("\u2013", "-").replace("\u2014", "--").replace("\u2015", "--")
                .split("\\n\\n");

        for (String entry1 : entries) {

            if (entry1.trim().isEmpty() || !entry1.contains("-")) {
                continue;
            }

            String type = "misc";
            String author = "";
            String editor = "";
            String comment = "";
            Map<String, String> hm = new HashMap<>();

            String[] fields = entry1.split("\n");

            for (int j = 0; j < fields.length; j++) {

                StringBuilder current = new StringBuilder(fields[j]);
                boolean done = false;

                while (!done && (j < (fields.length - 1))) {
                    if (fields[j + 1].length() <= 4) {
                        j++;
                        continue;
                    }
                    if (fields[j + 1].charAt(4) != '-') {
                        if ((current.length() > 0)
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
                if ("PT".equals(lab)) {
                    val = val.toLowerCase();
                    if ("book".equals(val)) {
                        type = "book";
                    } else if ("journal article".equals(val)
                            || "classical article".equals(val)
                            || "corrected and republished article".equals(val)
                            || "historical article".equals(val)
                            || "introductory journal article".equals(val)
                            || "newspaper article".equals(val)) {
                        type = "article";
                    } else if ("clinical conference".equals(val)
                            || "consensus development conference".equals(val)
                            || "consensus development conference, nih".equals(val)) {
                        type = "conference";
                    } else if ("technical report".equals(val)) {
                        type = "techreport";
                    } else if ("editorial".equals(val)) {
                        type = "inproceedings";//"incollection";"inbook";
                    } else if ("overall".equals(val)) {
                        type = "proceedings";
                    } else if ("".equals(type)) {
                        type = "other";
                    }

                } else if ("TI".equals(lab)) {
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
                else if ("BTI".equals(lab) || "CTI".equals(lab)) {
                    hm.put("booktitle", val);
                } else if ("FAU".equals(lab)) {
                    if ("".equals(author)) {
                        author = val;
                    } else {
                        author += " and " + val;
                    }
                } else if ("FED".equals(lab)) {
                    if ("".equals(editor)) {
                        editor = val;
                    } else {
                        editor += " and " + val;
                    }
                } else if ("JT".equals(lab)) {
                    if ("inproceedings".equals(type)) {
                        hm.put("booktitle", val);
                    } else {
                        hm.put("journal", val);
                    }
                } else if ("PG".equals(lab)) {
                    hm.put("pages", val);
                } else if ("PL".equals(lab)) {
                    hm.put("address", val);
                } else if ("IS".equals(lab)) {
                    hm.put("issn", val);
                } else if ("VI".equals(lab)) {
                    hm.put("volume", val);
                } else if ("AB".equals(lab)) {
                    String oldAb = hm.get("abstract");
                    if (oldAb == null) {
                        hm.put("abstract", val);
                    } else {
                        hm.put("abstract", oldAb + Globals.NEWLINE + val);
                    }
                } else if ("DP".equals(lab)) {
                    String[] parts = val.split(" ");
                    hm.put("year", parts[0]);
                    if ((parts.length > 1) && !parts[1].isEmpty()) {
                        hm.put("month", parts[1]);
                    }
                } else if ("MH".equals(lab) || "OT".equals(lab)) {
                    if (!hm.containsKey("keywords")) {
                        hm.put("keywords", val);
                    } else {
                        String kw = hm.get("keywords");
                        hm.put("keywords", kw + ", " + val);
                    }
                } else if ("CON".equals(lab) || "CIN".equals(lab) || "EIN".equals(lab)
                        || "EFR".equals(lab) || "CRI".equals(lab) || "CRF".equals(lab)
                        || "PRIN".equals(lab) || "PROF".equals(lab) || "RPI".equals(lab)
                        || "RPF".equals(lab) || "RIN".equals(lab) || "ROF".equals(lab)
                        || "UIN".equals(lab) || "UOF".equals(lab) || "SPIN".equals(lab)
                        || "ORI".equals(lab)) {
                    if (!comment.isEmpty()) {
                        comment = comment + "\n";
                    }
                    comment = comment + val;
                }
                //                // Added ID import 2005.12.01, Morten Alver:
                //                else if (lab.equals("ID"))
                //                    hm.put("refid", val);
                //                    // Added doi import (sciencedirect.com) 2011.01.10, Alexander Hug <alexander@alexanderhug.info>
                else if ("AID".equals(lab)) {
                    String doi = val;
                    if (doi.startsWith("doi:")) {
                        doi = doi.replaceAll("(?i)doi:", "").trim();
                        hm.put("doi", doi);
                    }
                }
            }
            // fix authors
            if (!author.isEmpty()) {
                author = AuthorList.fixAuthorLastNameFirst(author);
                hm.put("author", author);
            }
            if (!editor.isEmpty()) {
                editor = AuthorList.fixAuthorLastNameFirst(editor);
                hm.put("editor", editor);
            }
            if (!comment.isEmpty()) {
                hm.put("comment", comment);
            }

            BibEntry b = new BibEntry(DEFAULT_BIBTEXENTRY_ID, type); // id assumes an existing database so don't

            // Remove empty fields:
            List<Object> toRemove = new ArrayList<>();
            for (Map.Entry<String, String> key : hm.entrySet()) {
                String content = key.getValue();
                // content can never be null so only check if content is empty
                if (content.trim().isEmpty()) {
                    toRemove.add(key.getKey());
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
