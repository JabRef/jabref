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
import net.sf.jabref.bibtex.EntryTypes;

/**
 * Imports a Biblioscape Tag File. The format is described on
 * http://www.biblioscape.com/manual_bsp/Biblioscape_Tag_File.htm Several
 * Biblioscape field types are ignored. Others are only included in the BibTeX
 * field "comment".
 */
public class ScifinderImporter extends ImportFormat {

    /**
     * Return the name of this import format.
     */
    @Override
    public String getFormatName() {
        return "Scifinder";
    }

    /*
     *  (non-Javadoc)
     * @see net.sf.jabref.imports.ImportFormat#getCLIId()
     */
    @Override
    public String getCLIId() {
        return "scifinder";
    }

    /**
     * Check whether the source is in the correct format for this importer.
     */
    @Override
    public boolean isRecognizedFormat(InputStream stream) throws IOException {

        BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));
        String str;
        int i = 0;
        while (((str = in.readLine()) != null) && (i < 50)) {

            if ("START_RECORD".equals(str.trim())) {
                return true;
            }

            i++;
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
        BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));
        String str;
        String number = "";
        String country = "";
        String kindcode = "";
        while ((str = in.readLine()) != null) {
            sb.append(str);
        }

        String[] entries = sb.toString().split("START_RECORD");
        HashMap<String, String> hm = new HashMap<>();
        for (int i = 1; i < entries.length; i++) {
            String[] fields = entries[i].split("FIELD ");
            String journal = null;
            String Type = "";
            hm.clear(); // reset
            for (String field : fields) {
                if (field.contains(":")) {
                    String[] tmp = new String[2];
                    tmp[0] = field.substring(0, field.indexOf(":"));
                    tmp[1] = field.substring(field.indexOf(":") + 1).trim();
                    if (tmp.length > 1) {//==2
                        if ("Author".equals(tmp[0])) {
                            hm.put("author", AuthorList.fixAuthor_lastNameFirst(tmp[1].replaceAll(";", " and ")));
                        } else if ("Title".equals(tmp[0])) {
                            hm.put("title", tmp[1]);
                        } else if ("Journal Title".equals(tmp[0])) {
                            journal = tmp[1];
                        } else if ("Volume".equals(tmp[0])) {
                            hm.put("volume", tmp[1]);
                        } else if ("Page".equals(tmp[0])) {
                            hm.put("pages", tmp[1]);
                        } else if ("Publication Year".equals(tmp[0])) {
                            hm.put("year", tmp[1]);
                        } else if ("Abstract".equals(tmp[0])) {
                            hm.put("abstract", tmp[1]);
                        } else if ("Supplementary Terms".equals(tmp[0])) {
                            hm.put("keywords",
                                    tmp[1]);
                        } else if ("Inventor Name".equals(tmp[0]) && !tmp[1].trim().isEmpty()) {
                            hm.put("author", AuthorList.fixAuthor_lastNameFirst(tmp[1].replaceAll(";", " and ")));
                        } else if ("Patent Assignee".equals(tmp[0])) {
                            hm.put("institution", tmp[1]);
                        } else if ("Patent Kind Code".equals(tmp[0])) {
                            kindcode = " " + tmp[1];
                        } else if ("Patent Country".equals(tmp[0])) {
                            country = tmp[1] + " ";
                        } else if ("Patent Number".equals(tmp[0])) {
                            number = tmp[1];
                        } else if ("Priority Application Date".equals(tmp[0])) {
                            hm.put("number", country + number + kindcode);
                        } else if ("Document Type".equals(tmp[0])) {
                            if (tmp[1].startsWith("Journal") || tmp[1].startsWith("Review")) {
                                Type = "article";
                            } else if ("Dissertation".equals(tmp[1])) {
                                Type = "phdthesis";
                            } else if ("Patent".equals(tmp[1])) {
                                Type = "patent";
                            } else if (tmp[1].startsWith("Conference")) {
                                Type = "conference";
                            } else {
                                Type = tmp[1];
                            }
                        }
                    }
                }
            }

            BibEntry b = new BibEntry(DEFAULT_BIBTEXENTRY_ID, EntryTypes
                    .getTypeOrDefault(Type)); // id assumes an existing database so don't
            // create one here
            b.setField(hm);
            if (journal != null) {
                if ("conference".equals(Type)) {
                    b.setField("booktitle", journal);
                } else {
                    b.setField("journal", journal);
                }
            }
            bibitems.add(b);

        }
        return bibitems;
    }
}
