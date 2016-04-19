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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jabref.importer.ImportFormatReader;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.model.entry.AuthorList;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.IdGenerator;

/**
 * Imports an Ovid file.
 */
public class OvidImporter extends ImportFormat {

    private static final Pattern OVID_SOURCE_PATTERN = Pattern
            .compile("Source ([ \\w&\\-,:]+)\\.[ ]+([0-9]+)\\(([\\w\\-]+)\\):([0-9]+\\-?[0-9]+?)\\,.*([0-9][0-9][0-9][0-9])");

    private static final Pattern OVID_SOURCE_PATTERN_NO_ISSUE = Pattern
            .compile("Source ([ \\w&\\-,:]+)\\.[ ]+([0-9]+):([0-9]+\\-?[0-9]+?)\\,.*([0-9][0-9][0-9][0-9])");

    private static final Pattern OVID_SOURCE_PATTERN_2 = Pattern.compile(
            "([ \\w&\\-,]+)\\. Vol ([0-9]+)\\(([\\w\\-]+)\\) ([A-Za-z]+) ([0-9][0-9][0-9][0-9]), ([0-9]+\\-?[0-9]+)");

    private static final Pattern INCOLLECTION_PATTERN = Pattern.compile(
            "(.+)\\(([0-9][0-9][0-9][0-9])\\)\\. ([ \\w&\\-,:]+)\\.[ ]+\\(pp. ([0-9]+\\-?[0-9]+?)\\).[A-Za-z0-9, ]+pp\\. "
                    + "([\\w, ]+): ([\\w, ]+)");
    private static final Pattern BOOK_PATTERN = Pattern.compile(
            "\\(([0-9][0-9][0-9][0-9])\\)\\. [A-Za-z, ]+([0-9]+) pp\\. ([\\w, ]+): ([\\w, ]+)");

    private static final String OVID_PATTERN_STRING = "<[0-9]+>";
    private static final Pattern OVID_PATTERN = Pattern.compile(OVID_PATTERN_STRING);

    private static final int MAX_ITEMS = 50;
    /**
     * Return the name of this import format.
     */
    @Override
    public String getFormatName() {
        return "Ovid";
    }

    /*
     *  (non-Javadoc)
     * @see net.sf.jabref.imports.ImportFormat#getCLIId()
     */
    @Override
    public String getCLIId() {
        return "ovid";
    }



    /**
     * Check whether the source is in the correct format for this importer.
     */
    @Override
    public boolean isRecognizedFormat(InputStream stream) throws IOException {

        try (BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream))) {
            String str;
            int i = 0;
            while (((str = in.readLine()) != null) && (i < MAX_ITEMS)) {

                if (OvidImporter.OVID_PATTERN.matcher(str).find()) {
                    return true;
                }

                i++;
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
            String line;
            while ((line = in.readLine()) != null) {
                if (!line.isEmpty() && (line.charAt(0) != ' ')) {
                    sb.append("__NEWFIELD__");
                }
                sb.append(line);
                sb.append('\n');
            }
        }

        String[] items = sb.toString().split(OVID_PATTERN_STRING);

        for (int i = 1; i < items.length; i++) {
            Map<String, String> h = new HashMap<>();
            String[] fields = items[i].split("__NEWFIELD__");
            for (String field : fields) {
                int linebreak = field.indexOf('\n');
                String fieldName = field.substring(0, linebreak).trim();
                String content = field.substring(linebreak).trim();

                // Check if this is the author field (due to a minor special treatment for this field):
                boolean isAuthor = (fieldName.indexOf("Author") == 0)
                        && !fieldName.contains("Author Keywords")
                        && !fieldName.contains("Author e-mail");

                // Remove unnecessary dots at the end of lines, unless this is the author field,
                // in which case a dot at the end could be significant:
                if (!isAuthor && content.endsWith(".")) {
                    content = content.substring(0, content.length() - 1);
                }
                if (isAuthor) {

                    h.put("author", content);

                } else if (fieldName.startsWith("Title")) {
                    content = content.replaceAll("\\[.+\\]", "").trim();
                    if (content.endsWith(".")) {
                        content = content.substring(0, content.length() - 1);
                    }
                    h.put("title", content);
                } else if (fieldName.startsWith("Chapter Title")) {
                    h.put("chaptertitle", content);
                } else if (fieldName.startsWith("Source")) {
                    Matcher matcher;
                    if ((matcher = OvidImporter.OVID_SOURCE_PATTERN.matcher(content)).find()) {
                        h.put("journal", matcher.group(1));
                        h.put("volume", matcher.group(2));
                        h.put("issue", matcher.group(3));
                        h.put("pages", matcher.group(4));
                        h.put("year", matcher.group(5));
                    } else if ((matcher = OvidImporter.OVID_SOURCE_PATTERN_NO_ISSUE.matcher(content)).find()) {// may be missing the issue
                        h.put("journal", matcher.group(1));
                        h.put("volume", matcher.group(2));
                        h.put("pages", matcher.group(3));
                        h.put("year", matcher.group(4));
                    } else if ((matcher = OvidImporter.OVID_SOURCE_PATTERN_2.matcher(content)).find()) {

                        h.put("journal", matcher.group(1));
                        h.put("volume", matcher.group(2));
                        h.put("issue", matcher.group(3));
                        h.put("month", matcher.group(4));
                        h.put("year", matcher.group(5));
                        h.put("pages", matcher.group(6));

                    } else if ((matcher = OvidImporter.INCOLLECTION_PATTERN.matcher(content)).find()) {
                        h.put("editor", matcher.group(1).replace(" (Ed)", ""));
                        h.put("year", matcher.group(2));
                        h.put("booktitle", matcher.group(3));
                        h.put("pages", matcher.group(4));
                        h.put("address", matcher.group(5));
                        h.put("publisher", matcher.group(6));
                    } else if ((matcher = OvidImporter.BOOK_PATTERN.matcher(content)).find()) {
                        h.put("year", matcher.group(1));
                        h.put("pages", matcher.group(2));
                        h.put("address", matcher.group(3));
                        h.put("publisher", matcher.group(4));

                    }
                    // Add double hyphens to page ranges:
                    if (h.get("pages") != null) {
                        h.put("pages", h.get("pages").replace("-", "--"));
                    }

                } else if ("Abstract".equals(fieldName)) {
                    h.put("abstract", content);

                } else if ("Publication Type".equals(fieldName)) {
                    if (content.contains("Book")) {
                        h.put("entrytype", "book");
                    } else if (content.contains("Journal")) {
                        h.put("entrytype", "article");
                    } else if (content.contains("Conference Paper")) {
                        h.put("entrytype", "inproceedings");
                    }
                } else if (fieldName.startsWith("Language")) {
                    h.put("language", content);
                } else if (fieldName.startsWith("Author Keywords")) {
                    content = content.replace(";", ",").replace("  ", " ");
                    h.put("keywords", content);
                } else if (fieldName.startsWith("ISSN")) {
                    h.put("issn", content);
                } else if (fieldName.startsWith("DOI Number")) {
                    h.put("doi", content);
                }
            }

            // Now we need to check if a book entry has given editors in the author field;
            // if so, rearrange:
            String auth = h.get("author");
            if ((auth != null) && auth.contains(" [Ed]")) {
                h.remove("author");
                h.put("editor", auth.replace(" [Ed]", ""));
            }

            // Rearrange names properly:
            auth = h.get("author");
            if (auth != null) {
                h.put("author", fixNames(auth));
            }
            auth = h.get("editor");
            if (auth != null) {
                h.put("editor", fixNames(auth));
            }

            // Set the entrytype properly:
            String entryType = h.containsKey("entrytype") ? h.get("entrytype") : "misc";
            h.remove("entrytype");
            if ("book".equals(entryType) && h.containsKey("chaptertitle")) {
                // This means we have an "incollection" entry.
                entryType = "incollection";
                // Move the "chaptertitle" to just "title":
                h.put("title", h.remove("chaptertitle"));
            }
            BibEntry b = new BibEntry(IdGenerator.next(), entryType);
            b.setField(h);

            bibitems.add(b);

        }

        return bibitems;
    }

    /**
     * Convert a string of author names into a BibTeX-compatible format.
     * @param content The name string.
     * @return The formatted names.
     */
    private static String fixNames(String content) {
        String names;
        if (content.indexOf(';') > 0) { //LN FN; [LN FN;]*
            names = content.replaceAll("[^\\.A-Za-z,;\\- ]", "").replace(";", " and");
        } else if (content.indexOf("  ") > 0) {
            String[] sNames = content.split("  ");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < sNames.length; i++) {
                if (i > 0) {
                    sb.append(" and ");
                }
                sb.append(sNames[i].replaceFirst(" ", ", "));
            }
            names = sb.toString();
        } else {
            names = content;
        }
        return AuthorList.fixAuthorLastNameFirst(names);
    }

}
