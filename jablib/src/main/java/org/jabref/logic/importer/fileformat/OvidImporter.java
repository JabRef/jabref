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
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NonNull;

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
    public String getId() {
        return "ovid";
    }

    @Override
    public String getName() {
        return "Ovid";
    }

    @Override
    public StandardFileType getFileType() {
        return StandardFileType.TXT;
    }

    @Override
    public String getDescription() {
        return Localization.lang("Importer for the Ovid format.");
    }

    @Override
    public boolean isRecognizedFormat(@NonNull BufferedReader reader) throws IOException {
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
    public ParserResult importDatabase(@NonNull BufferedReader reader) throws IOException {
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
            Map<Field, String> h = new HashMap<>();
            String[] fields = items[i].split("__NEWFIELD__");
            for (String field : fields) {
                parseField(field, h);
            }
            normalizeContributors(h);
            EntryType entryType = resolveEntryType(h);
            BibEntry b = new BibEntry(entryType);
            b.setField(h);
            bibitems.add(b);
        }
        return new ParserResult(bibitems);
    }

    /**
     * Convert a string of author names into a BibTeX-compatible format.
     *
     * @param content The name string.
     * @return The formatted names.
     */
    private static String fixNames(String content) {
        String names;
        if (content.indexOf(';') > 0) { // LN FN; [LN FN;]*
            names = content.replaceAll("[^\\.A-Za-z,;\\- ]", "").replace(";", " and");
        } else if (content.indexOf("  ") > 0) {
            String[] sNames = content.split(" {2}");
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

    private void parseField(String field, Map<Field, String> fieldMap) {
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
            fieldMap.put(StandardField.AUTHOR, content);
        } else if (fieldName.startsWith("Title")) {
            content = content.replaceAll("\\[.+\\]", "").trim();
            if (content.endsWith(".")) {
                content = content.substring(0, content.length() - 1);
            }
            fieldMap.put(StandardField.TITLE, content);
        } else if (fieldName.startsWith("Chapter Title")) {
            fieldMap.put(new UnknownField("chaptertitle"), content);
        } else if (fieldName.startsWith("Source")) {
            parseSource(content, fieldMap);
        } else if ("Abstract".equals(fieldName)) {
            fieldMap.put(StandardField.ABSTRACT, content);
        } else if ("Publication Type".equals(fieldName)) {
            if (content.contains("Book")) {
                fieldMap.put(InternalField.TYPE_HEADER, "book");
            } else if (content.contains("Journal")) {
                fieldMap.put(InternalField.TYPE_HEADER, "article");
            } else if (content.contains("Conference Paper")) {
                fieldMap.put(InternalField.TYPE_HEADER, "inproceedings");
            }
        } else if (fieldName.startsWith("Language")) {
            fieldMap.put(StandardField.LANGUAGE, content);
        } else if (fieldName.startsWith("Author Keywords")) {
            content = content.replace(";", ",").replace("  ", " ");
            fieldMap.put(StandardField.KEYWORDS, content);
        } else if (fieldName.startsWith("ISSN")) {
            fieldMap.put(StandardField.ISSN, content);
        } else if (fieldName.startsWith("DOI Number")) {
            fieldMap.put(StandardField.DOI, content);
        }
    }

    private void parseSource(String content, Map<Field, String> fieldMap) {
        Matcher matcher;
        if ((matcher = OvidImporter.OVID_SOURCE_PATTERN.matcher(content)).find()) {
            fieldMap.put(StandardField.JOURNAL, matcher.group(1));
            fieldMap.put(StandardField.VOLUME, matcher.group(2));
            fieldMap.put(StandardField.ISSUE, matcher.group(3));
            fieldMap.put(StandardField.PAGES, matcher.group(4));
            fieldMap.put(StandardField.YEAR, matcher.group(5));
        } else if ((matcher = OvidImporter.OVID_SOURCE_PATTERN_NO_ISSUE.matcher(content)).find()) { // may be missing the issue
            fieldMap.put(StandardField.JOURNAL, matcher.group(1));
            fieldMap.put(StandardField.VOLUME, matcher.group(2));
            fieldMap.put(StandardField.PAGES, matcher.group(3));
            fieldMap.put(StandardField.YEAR, matcher.group(4));
        } else if ((matcher = OvidImporter.OVID_SOURCE_PATTERN_2.matcher(content)).find()) {
            fieldMap.put(StandardField.JOURNAL, matcher.group(1));
            fieldMap.put(StandardField.VOLUME, matcher.group(2));
            fieldMap.put(StandardField.ISSUE, matcher.group(3));
            fieldMap.put(StandardField.MONTH, matcher.group(4));
            fieldMap.put(StandardField.YEAR, matcher.group(5));
            fieldMap.put(StandardField.PAGES, matcher.group(6));
        } else if ((matcher = OvidImporter.INCOLLECTION_PATTERN.matcher(content)).find()) {
            fieldMap.put(StandardField.EDITOR, matcher.group(1).replace(" (Ed)", ""));
            fieldMap.put(StandardField.YEAR, matcher.group(2));
            fieldMap.put(StandardField.BOOKTITLE, matcher.group(3));
            fieldMap.put(StandardField.PAGES, matcher.group(4));
            fieldMap.put(StandardField.ADDRESS, matcher.group(5));
            fieldMap.put(StandardField.PUBLISHER, matcher.group(6));
        } else if ((matcher = OvidImporter.BOOK_PATTERN.matcher(content)).find()) {
            fieldMap.put(StandardField.YEAR, matcher.group(1));
            fieldMap.put(StandardField.PAGES, matcher.group(2));
            fieldMap.put(StandardField.ADDRESS, matcher.group(3));
            fieldMap.put(StandardField.PUBLISHER, matcher.group(4));
        }
        // Add double hyphens to page ranges:
        if (fieldMap.get(StandardField.PAGES) != null) {
            fieldMap.put(StandardField.PAGES, fieldMap.get(StandardField.PAGES).replace("-", "--"));
        }
    }

    private void normalizeContributors(Map<Field, String> fieldMap) {
        // Now we need to check if a book entry has given editors in the author field;
        // if so, rearrange:
        String auth = fieldMap.get(StandardField.AUTHOR);
        if ((auth != null) && auth.contains(" [Ed]")) {
            fieldMap.remove(StandardField.AUTHOR);
            fieldMap.put(StandardField.EDITOR, auth.replace(" [Ed]", ""));
        }

        // Rearrange names properly:
        auth = fieldMap.get(StandardField.AUTHOR);
        if (auth != null) {
            fieldMap.put(StandardField.AUTHOR, fixNames(auth));
        }
        auth = fieldMap.get(StandardField.EDITOR);
        if (auth != null) {
            fieldMap.put(StandardField.EDITOR, fixNames(auth));
        }
    }

    private EntryType resolveEntryType(Map<Field, String> fieldMap) {
        // Set the entrytype properly:
        EntryType entryType = fieldMap.containsKey(InternalField.TYPE_HEADER)
                              ? EntryTypeFactory.parse(fieldMap.get(InternalField.TYPE_HEADER))
                              : BibEntry.DEFAULT_TYPE;
        fieldMap.remove(InternalField.TYPE_HEADER);
        if (entryType.equals(StandardEntryType.Book) && fieldMap.containsKey(new UnknownField("chaptertitle"))) {
            // This means we have an "incollection" entry.
            entryType = StandardEntryType.InCollection;
            // Move the "chaptertitle" to just "title":
            fieldMap.put(StandardField.TITLE, fieldMap.remove(new UnknownField("chaptertitle")));
        }
        return entryType;
    }
}
