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
import net.sf.jabref.bibtex.EntryTypes;
import net.sf.jabref.model.entry.EntryType;
import net.sf.jabref.model.entry.IdGenerator;
import net.sf.jabref.model.entry.BibEntry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Imports a Biblioscape Tag File. The format is described on
 * http://www.biblioscape.com/manual_bsp/Biblioscape_Tag_File.htm Several
 * Biblioscape field types are ignored. Others are only included in the BibTeX
 * field "comment".
 */
public class SixpackImporter extends ImportFormat {

    private final String SEPARATOR = new String(new char[] {0, 48});

    private static final Log LOGGER = LogFactory.getLog(SixpackImporter.class);


    /**
     * Return the name of this import format.
     */
    @Override
    public String getFormatName() {
        return "Sixpack";
    }

    /*
     *  (non-Javadoc)
     * @see net.sf.jabref.imports.ImportFormat#getCLIId()
     */
    @Override
    public String getCLIId() {
        return "sixpack";
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

            if (str.contains(SEPARATOR)) {
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

        HashMap<String, String> fI = new HashMap<>();
        fI.put("id", "bibtexkey");
        fI.put("au", "author");
        fI.put("ti", "title");
        fI.put("jo", "journal");
        fI.put("vo", "volume");
        fI.put("nu", "number");
        fI.put("pa", "pages");
        fI.put("mo", "month");
        fI.put("yr", "year");
        fI.put("kw", "keywords");
        fI.put("ab", "abstract");
        fI.put("no", "note");
        fI.put("ed", "editor");
        fI.put("pu", "publisher");
        fI.put("se", "series");
        fI.put("ad", "address");
        fI.put("en", "edition");
        fI.put("ch", "chapter");
        fI.put("hp", "howpublished");
        fI.put("tb", "booktitle");
        fI.put("or", "organization");
        fI.put("sc", "school");
        fI.put("in", "institution");
        fI.put("ty", "type");
        fI.put("url", "url");
        fI.put("cr", "crossref");
        fI.put("fi", "file");

        BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));
        in.readLine();
        String ln = in.readLine();
        if (ln == null) {
            return null;
        }
        String[] fieldDef = ln.split(",");

        List<BibEntry> bibitems = new ArrayList<>();

        String s;
        BibEntry entry;
        while ((s = in.readLine()) != null) {
            try {
                s = s.replaceAll("<par>", ""); // What is <par> ????
                String[] fields = s.split(SEPARATOR);
                // Check type and create entry:
                if (fields.length < 2)
                {
                    continue; // Avoid ArrayIndexOutOfBoundsException
                }
                EntryType typ = EntryTypes.getType(fields[1].toLowerCase());
                if (typ == null) {
                    String type = "";
                    if ("Masterthesis".equals(fields[1])) {
                        type = "mastersthesis";
                    }
                    if ("PhD-Thesis".equals(fields[1])) {
                        type = "phdthesis";
                    }
                    if ("miscellaneous".equals(fields[1])) {
                        type = "misc";
                    }
                    if ("Conference".equals(fields[1])) {
                        type = "proceedings";
                    }
                    typ = EntryTypes.getType(type.toLowerCase());
                }
                entry = new BibEntry(IdGenerator.next(), typ);
                String fld;
                for (int i = 0; i < Math.min(fieldDef.length, fields.length); i++) {
                    fld = fI.get(fieldDef[i]);
                    if (fld != null) {
                        if ("author".equals(fld) || "editor".equals(fld)) {
                            ImportFormatReader.setIfNecessary(entry,
                                    fld, fields[i].replaceAll(" and ", ", ").replaceAll(", ",
                                            " and "));
                        } else if ("pages".equals(fld)) {
                            ImportFormatReader.setIfNecessary(entry, fld, fields[i]
                                    .replaceAll("-", "--"));
                        } else if ("file".equals(fld)) {
                            String fieldName = "pdf"; // We set pdf as default.
                            if (fields[i].endsWith("ps") || fields[i].endsWith("ps.gz")) {
                                fieldName = "ps";
                            } else if (fields[i].endsWith("html")) {
                                fieldName = "url";
                            }
                            ImportFormatReader.setIfNecessary(entry, fieldName, fields[i]);
                        } else {
                            ImportFormatReader.setIfNecessary(entry, fld, fields[i]);
                        }
                    }
                }
                bibitems.add(entry);
            } catch (NullPointerException ex) {
                LOGGER.info("Problem parsing Sixpack entry, ignoring entry.", ex);
            }
        }

        return bibitems;
    }
}
