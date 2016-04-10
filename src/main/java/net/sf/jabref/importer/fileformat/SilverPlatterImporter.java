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

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import net.sf.jabref.importer.ImportFormatReader;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.AuthorList;

import java.util.regex.Pattern;

/**
 * Imports a SilverPlatter exported file. This is a poor format to parse,
 * so it currently doesn't handle everything correctly.
 */
public class SilverPlatterImporter extends ImportFormat {

    private static final Pattern START_PATTERN = Pattern.compile("Record.*INSPEC.*");


    /**
     * Return the name of this import format.
     */
    @Override
    public String getFormatName() {
        return "SilverPlatter";
    }

    /*
     *  (non-Javadoc)
     * @see net.sf.jabref.imports.ImportFormat#getCLIId()
     */
    @Override
    public String getCLIId() {
        return "silverplatter";
    }

    /**
     * Check whether the source is in the correct format for this importer.
     */
    @Override
    public boolean isRecognizedFormat(InputStream stream) throws IOException {
        try (BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream))) {

            // This format is very similar to Inspec, so we have a two-fold strategy:
            // If we see the flag signaling that it is an Inspec file, return false.
            // This flag should appear above the first entry and prevent us from
            // accepting the Inspec format. Then we look for the title entry.
            String str;
            while ((str = in.readLine()) != null) {

                if (START_PATTERN.matcher(str).find()) {
                    return false; // This is an Inspec file, so return false.
                }

                if ((str.length() >= 5) && "TI:  ".equals(str.substring(0, 5))) {
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
        try (BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream))) {
            boolean isChapter = false;
            String str;
            StringBuilder sb = new StringBuilder();
            while ((str = in.readLine()) != null) {
                if (str.length() < 2) {
                    sb.append("__::__").append(str);
                } else {
                    sb.append("__NEWFIELD__").append(str);
                }
            }
            String[] entries = sb.toString().split("__::__");
            String type = "";
            HashMap<String, String> h = new HashMap<>();
            for (String entry : entries) {
                if (entry.trim().length() < 6) {
                    continue;
                }
                h.clear();
                String[] fields = entry.split("__NEWFIELD__");
                for (String field : fields) {
                    if (field.length() < 6) {
                        continue;
                    }
                    String f3 = field.substring(0, 2);
                    String frest = field.substring(5);
                    if ("TI".equals(f3)) {
                        h.put("title", frest);
                    } else if ("AU".equals(f3)) {
                        if (frest.trim().endsWith("(ed)")) {
                            String ed = frest.trim();
                            ed = ed.substring(0, ed.length() - 4);
                            h.put("editor",
                                    AuthorList.fixAuthorLastNameFirst(ed.replace(",-", ", ").replace(";", " and ")));
                        } else {
                            h.put("author",
                                    AuthorList.fixAuthorLastNameFirst(frest.replace(",-", ", ").replace(";", " and ")));
                        }
                    } else if ("AB".equals(f3)) {
                        h.put("abstract", frest);
                    } else if ("DE".equals(f3)) {
                        String kw = frest.replace("-;", ",").toLowerCase();
                        h.put("keywords", kw.substring(0, kw.length() - 1));
                    } else if ("SO".equals(f3)) {
                        int m = frest.indexOf('.');
                        if (m >= 0) {
                            String jr = frest.substring(0, m);
                            h.put("journal", jr.replace("-", " "));
                            frest = frest.substring(m);
                            m = frest.indexOf(';');
                            if (m >= 5) {
                                String yr = frest.substring(m - 5, m).trim();
                                h.put("year", yr);
                                frest = frest.substring(m);
                                m = frest.indexOf(':');
                                int issueIndex = frest.indexOf('(');
                                int endIssueIndex = frest.indexOf(')');
                                if (m >= 0) {
                                    String pg = frest.substring(m + 1).trim();
                                    h.put("pages", pg);
                                    h.put("volume", frest.substring(1, issueIndex).trim());
                                    h.put("issue", frest.substring(issueIndex + 1, endIssueIndex).trim());
                                }
                            }
                        }
                    } else if ("PB".equals(f3)) {
                        int m = frest.indexOf(':');
                        if (m >= 0) {
                            String jr = frest.substring(0, m);
                            h.put("publisher", jr.replace("-", " ").trim());
                            frest = frest.substring(m);
                            m = frest.indexOf(", ");
                            if ((m + 2) < frest.length()) {
                                String yr = frest.substring(m + 2).trim();
                                try {
                                    Integer.parseInt(yr);
                                    h.put("year", yr);
                                } catch (NumberFormatException ex) {
                                    // Let's assume that this wasn't a number, since it
                                    // couldn't be parsed as an integer.
                                }

                            }

                        }
                    } else if ("AF".equals(f3)) {
                        h.put("school", frest.trim());

                    } else if ("DT".equals(f3)) {
                        frest = frest.trim();
                        if ("Monograph".equals(frest)) {
                            type = "book";
                        } else if (frest.startsWith("Dissertation")) {
                            type = "phdthesis";
                        } else if (frest.toLowerCase().contains("journal")) {
                            type = "article";
                        } else if ("Contribution".equals(frest) || "Chapter".equals(frest)) {
                            type = "incollection";
                            // This entry type contains page numbers and booktitle in the
                            // title field.
                            isChapter = true;
                        } else {
                            type = frest.replace(" ", "");
                        }
                    }
                }

                if (isChapter) {
                    Object titleO = h.get("title");
                    if (titleO != null) {
                        String title = ((String) titleO).trim();
                        int inPos = title.indexOf("\" in ");
                        if (inPos > 1) {
                            h.put("title", title.substring(0, inPos));
                        }
                    }

                }

                BibEntry b = new BibEntry(DEFAULT_BIBTEXENTRY_ID, type); // id assumes an existing database so don't
                // create one here
                b.setField(h);

                bibitems.add(b);

            }
        }

        return bibitems;
    }
}
