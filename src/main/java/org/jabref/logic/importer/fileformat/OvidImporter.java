package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.util.FileType;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

/**
 * Imports an Ovid file.
 */
public class OvidImporter extends Importer {

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

    @Override
    public String getName() {
        return "Ovid";
    }

    @Override
    public FileType getFileType() {
        return FileType.OVID;
    }

    @Override
    public String getDescription() {
        return "Imports an Ovid file.";
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader reader) throws IOException {
        String str;
        int i = 0;
        while (((str = reader.readLine()) != null) && (i < MAX_ITEMS)) {

            if (OvidImporter.OVID_PATTERN.matcher(str).find()) {
                return true;
            }

            i++;
        }
        return false;
    }

    @Override
    public ParserResult importDatabase(BufferedReader reader) throws IOException {
        List<BibEntry> bibitems = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.isEmpty() && (line.charAt(0) != ' ')) {
                sb.append("__NEWFIELD__");
            }
            sb.append(line);
            sb.append('\n');
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

                    h.put(FieldName.AUTHOR, content);

                } else if (fieldName.startsWith("Title")) {
                    content = content.replaceAll("\\[.+\\]", "").trim();
                    if (content.endsWith(".")) {
                        content = content.substring(0, content.length() - 1);
                    }
                    h.put(FieldName.TITLE, content);
                } else if (fieldName.startsWith("Chapter Title")) {
                    h.put("chaptertitle", content);
                } else if (fieldName.startsWith("Source")) {
                    Matcher matcher;
                    if ((matcher = OvidImporter.OVID_SOURCE_PATTERN.matcher(content)).find()) {
                        h.put(FieldName.JOURNAL, matcher.group(1));
                        h.put(FieldName.VOLUME, matcher.group(2));
                        h.put(FieldName.ISSUE, matcher.group(3));
                        h.put(FieldName.PAGES, matcher.group(4));
                        h.put(FieldName.YEAR, matcher.group(5));
                    } else if ((matcher = OvidImporter.OVID_SOURCE_PATTERN_NO_ISSUE.matcher(content)).find()) { // may be missing the issue
                        h.put(FieldName.JOURNAL, matcher.group(1));
                        h.put(FieldName.VOLUME, matcher.group(2));
                        h.put(FieldName.PAGES, matcher.group(3));
                        h.put(FieldName.YEAR, matcher.group(4));
                    } else if ((matcher = OvidImporter.OVID_SOURCE_PATTERN_2.matcher(content)).find()) {

                        h.put(FieldName.JOURNAL, matcher.group(1));
                        h.put(FieldName.VOLUME, matcher.group(2));
                        h.put(FieldName.ISSUE, matcher.group(3));
                        h.put(FieldName.MONTH, matcher.group(4));
                        h.put(FieldName.YEAR, matcher.group(5));
                        h.put(FieldName.PAGES, matcher.group(6));

                    } else if ((matcher = OvidImporter.INCOLLECTION_PATTERN.matcher(content)).find()) {
                        h.put(FieldName.EDITOR, matcher.group(1).replace(" (Ed)", ""));
                        h.put(FieldName.YEAR, matcher.group(2));
                        h.put(FieldName.BOOKTITLE, matcher.group(3));
                        h.put(FieldName.PAGES, matcher.group(4));
                        h.put(FieldName.ADDRESS, matcher.group(5));
                        h.put(FieldName.PUBLISHER, matcher.group(6));
                    } else if ((matcher = OvidImporter.BOOK_PATTERN.matcher(content)).find()) {
                        h.put(FieldName.YEAR, matcher.group(1));
                        h.put(FieldName.PAGES, matcher.group(2));
                        h.put(FieldName.ADDRESS, matcher.group(3));
                        h.put(FieldName.PUBLISHER, matcher.group(4));

                    }
                    // Add double hyphens to page ranges:
                    if (h.get(FieldName.PAGES) != null) {
                        h.put(FieldName.PAGES, h.get(FieldName.PAGES).replace("-", "--"));
                    }

                } else if ("Abstract".equals(fieldName)) {
                    h.put(FieldName.ABSTRACT, content);

                } else if ("Publication Type".equals(fieldName)) {
                    if (content.contains("Book")) {
                        h.put(BibEntry.TYPE_HEADER, "book");
                    } else if (content.contains("Journal")) {
                        h.put(BibEntry.TYPE_HEADER, "article");
                    } else if (content.contains("Conference Paper")) {
                        h.put(BibEntry.TYPE_HEADER, "inproceedings");
                    }
                } else if (fieldName.startsWith("Language")) {
                    h.put(FieldName.LANGUAGE, content);
                } else if (fieldName.startsWith("Author Keywords")) {
                    content = content.replace(";", ",").replace("  ", " ");
                    h.put(FieldName.KEYWORDS, content);
                } else if (fieldName.startsWith("ISSN")) {
                    h.put(FieldName.ISSN, content);
                } else if (fieldName.startsWith("DOI Number")) {
                    h.put(FieldName.DOI, content);
                }
            }

            // Now we need to check if a book entry has given editors in the author field;
            // if so, rearrange:
            String auth = h.get(FieldName.AUTHOR);
            if ((auth != null) && auth.contains(" [Ed]")) {
                h.remove(FieldName.AUTHOR);
                h.put(FieldName.EDITOR, auth.replace(" [Ed]", ""));
            }

            // Rearrange names properly:
            auth = h.get(FieldName.AUTHOR);
            if (auth != null) {
                h.put(FieldName.AUTHOR, fixNames(auth));
            }
            auth = h.get(FieldName.EDITOR);
            if (auth != null) {
                h.put(FieldName.EDITOR, fixNames(auth));
            }

            // Set the entrytype properly:
            String entryType = h.containsKey(BibEntry.TYPE_HEADER) ? h.get(BibEntry.TYPE_HEADER) : BibEntry.DEFAULT_TYPE;
            h.remove(BibEntry.TYPE_HEADER);
            if ("book".equals(entryType) && h.containsKey("chaptertitle")) {
                // This means we have an "incollection" entry.
                entryType = "incollection";
                // Move the "chaptertitle" to just "title":
                h.put(FieldName.TITLE, h.remove("chaptertitle"));
            }
            BibEntry b = new BibEntry(entryType);
            b.setField(h);

            bibitems.add(b);

        }

        return new ParserResult(bibitems);
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
