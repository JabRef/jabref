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

import net.sf.jabref.importer.ImportFormatReader;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.model.entry.BibEntry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import net.sf.jabref.bibtex.EntryTypes;

/**
 * Importer for the ISI Web of Science format.
 */
public class BiomailImporter extends ImportFormat {

    /**
     * Return the name of this import format.
     */
    @Override
    public String getFormatName() {
        return "Biomail";
    }

    /*
     *  (non-Javadoc)
     * @see net.sf.jabref.imports.ImportFormat#getCLIId()
     */
    @Override
    public String getCLIId() {
        return "biomail";
    }

    /**
     * Check whether the source is in the correct format for this importer.
     */
    @Override
    public boolean isRecognizedFormat(InputStream stream)
            throws IOException {
        // Our strategy is to look for the "BioMail" line.
        BufferedReader in =
                new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));
        Pattern pat1 = Pattern.compile("BioMail");

        String str;

        while ((str = in.readLine()) != null) {

            if (pat1.matcher(str).find()) {
                return true;
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
        ArrayList<BibEntry> bibitems = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        BufferedReader in =
                new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));

        String str;

        while ((str = in.readLine()) != null) {
            if (str.length() < 3) {
                continue;
            }

            // begining of a new item
            if ("PMID- ".equals(str.substring(0, 6))) {
                sb.append("::").append(str);
            } else {
                String beg = str.substring(0, 6);

                if (beg.indexOf(" ") > 0) {
                    sb.append(" ## "); // mark the begining of each field
                    sb.append(str);
                } else {
                    sb.append("EOLEOL"); // mark the end of each line
                    sb.append(str.trim());
                }
            }
        }

        String[] entries = sb.toString().split("::");

        // skip the first entry as it is either empty or has document header
        HashMap<String, String> hm = new HashMap<>();

        for (String entry : entries) {
            String[] fields = entry.split(" ## ");

            if (fields.length == 0) {
                fields = entry.split("\n");
            }

            String Type = "";
            String pages = "";
            String shortauthor = "";
            String fullauthor = "";
            hm.clear();

            for (String field : fields) {
                System.out.println(">>>" + field + "<<<");

                //empty field don't do anything
                if (field.length() <= 2) {
                    continue;
                }

                String beg = field.substring(0, 6);
                String value = field.substring(6);
                value = value.trim();

                if ("PT  - ".equals(beg)) {
                    // PT = value.replaceAll("JOURNAL ARTICLE", "article").replaceAll("Journal Article", "article");
                    Type = "article"; //make all of them PT?
                } else if ("TY  - ".equals(beg)) {
                    if ("CONF".equals(value)) {
                        Type = "inproceedings";
                    }
                } else if ("JO  - ".equals(beg)) {
                    hm.put("booktitle", value);
                } else if ("FAU - ".equals(beg)) {
                    String tmpauthor = value.replaceAll("EOLEOL", " and ");

                    // if there is already someone there then append with "and"
                    if (!"".equals(fullauthor)) {
                        fullauthor = fullauthor + " and " + tmpauthor;
                    } else {
                        fullauthor = tmpauthor;
                    }
                } else if ("AU  - ".equals(beg)) {
                    String tmpauthor = value.replaceAll("EOLEOL", " and ").replaceAll(" ", ", ");

                    // if there is already someone there then append with "and"
                    if (!"".equals(shortauthor)) {
                        shortauthor = shortauthor + " and " + tmpauthor;
                    } else {
                        shortauthor = tmpauthor;
                    }
                } else if ("TI  - ".equals(beg)) {
                    hm.put("title", value.replaceAll("EOLEOL", " "));
                } else if ("TA  - ".equals(beg)) {
                    hm.put("journal", value.replaceAll("EOLEOL", " "));
                } else if ("AB  - ".equals(beg)) {
                    hm.put("abstract", value.replaceAll("EOLEOL", " "));
                } else if ("PG  - ".equals(beg)) {
                    pages = value.replaceAll("-", "--");
                } else if ("IP  - ".equals(beg)) {
                    hm.put("number", value);
                } else if ("DP  - ".equals(beg)) {
                    String[] parts = value.split(" "); // sometimes this is just year, sometimes full date
                    hm.put("year", parts[0]);
                } else if ("VI  - ".equals(beg)) {
                    hm.put("volume", value);
                } else if ("AID - ".equals(beg)) {
                    String[] parts = value.split(" ");
                    if ("[doi]".equals(parts[1])) {
                        hm.put("doi", parts[0]);
                        hm.put("url", "http://dx.doi.org/" + parts[0]);
                    }
                }
            }

            if (!"".equals(pages)) {
                hm.put("pages", pages);
            }
            if (!"".equals(fullauthor)) {
                hm.put("author", fullauthor);
            } else if (!"".equals(shortauthor)) {
                hm.put("author", shortauthor);
            }

            BibEntry b =
                    new BibEntry(DEFAULT_BIBTEXENTRY_ID, EntryTypes.getTypeOrDefault(Type)); // id assumes an existing database so don't

            // create one here
            b.setField(hm);

            // the first bibitem is always empty, presumably as a result of trying
            // to parse header informaion. So add only if we have at least author or
            // title fields.
            if ((hm.get("author") != null) || (hm.get("title") != null)) {
                bibitems.add(b);
            }
        }

        return bibitems;
    }

}
