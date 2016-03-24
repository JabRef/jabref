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
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import net.sf.jabref.importer.ImportFormatReader;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.model.entry.BibEntry;

/**
 * Importer for COPAC format.
 *
 * Documentation can be found online at:
 *
 * http://copac.ac.uk/faq/#format
 */
public class CopacImporter extends ImportFormat {

    private static final Pattern COPAC_PATTERN = Pattern.compile("^\\s*TI- ");


    /**
     * Return the name of this import format.
     */
    @Override
    public String getFormatName() {
        return "Copac";
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.jabref.imports.ImportFormat#getCLIId()
     */
    @Override
    public String getCLIId() {
        return "cpc";
    }



    /**
     * Check whether the source is in the correct format for this importer.
     */
    @Override
    public boolean isRecognizedFormat(InputStream stream) throws IOException {

        BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));

        String str;

        while ((str = in.readLine()) != null) {
            if (CopacImporter.COPAC_PATTERN.matcher(str).find()) {
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
        if (stream == null) {
            throw new IOException("No stream given.");
        }

        List<String> entries = new LinkedList<>();
        StringBuilder sb = new StringBuilder();

        try (BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream))) {
            // Preprocess entries
            String str;

            while ((str = in.readLine()) != null) {

                if (str.length() < 4) {
                    continue;
                }

                String code = str.substring(0, 4);

                if ("    ".equals(code)) {
                    sb.append(' ').append(str.trim());
                } else {

                    // begining of a new item
                    if ("TI- ".equals(str.substring(0, 4))) {
                        if (sb.length() > 0) {
                            entries.add(sb.toString());
                        }
                        sb = new StringBuilder();
                    }
                    sb.append('\n').append(str);
                }
            }
        }

        if (sb.length() > 0) {
            entries.add(sb.toString());
        }

        List<BibEntry> results = new LinkedList<>();

        for (String entry : entries) {

            // Copac does not contain enough information on the type of the
            // document. A book is assumed.
            BibEntry b = new BibEntry(DEFAULT_BIBTEXENTRY_ID, "book");

            String[] lines = entry.split("\n");

            for (String line1 : lines) {
                String line = line1.trim();
                if (line.length() < 4) {
                    continue;
                }
                String code = line.substring(0, 4);

                if ("TI- ".equals(code)) {
                    setOrAppend(b, "title", line.substring(4).trim(), ", ");
                } else if ("AU- ".equals(code)) {
                    setOrAppend(b, "author", line.substring(4).trim(), " and ");
                } else if ("PY- ".equals(code)) {
                    setOrAppend(b, "year", line.substring(4).trim(), ", ");
                } else if ("PU- ".equals(code)) {
                    setOrAppend(b, "publisher", line.substring(4).trim(), ", ");
                } else if ("SE- ".equals(code)) {
                    setOrAppend(b, "series", line.substring(4).trim(), ", ");
                } else if ("IS- ".equals(code)) {
                    setOrAppend(b, "isbn", line.substring(4).trim(), ", ");
                } else if ("KW- ".equals(code)) {
                    setOrAppend(b, "keywords", line.substring(4).trim(), ", ");
                } else if ("NT- ".equals(code)) {
                    setOrAppend(b, "note", line.substring(4).trim(), ", ");
                } else if ("PD- ".equals(code)) {
                    setOrAppend(b, "physicaldimensions", line.substring(4).trim(), ", ");
                } else if ("DT- ".equals(code)) {
                    setOrAppend(b, "documenttype", line.substring(4).trim(), ", ");
                } else {
                    setOrAppend(b, code.substring(0, 2), line.substring(4).trim(), ", ");
                }
            }
            results.add(b);
        }

        return results;
    }

    private static void setOrAppend(BibEntry b, String field, String value, String separator) {
        if (b.hasField(field)) {
            b.setField(field, b.getField(field) + separator + value);
        } else {
            b.setField(field, value);
        }
    }
}
