package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.FileType;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NonNull;

/**
 * This is BibIX variant of Refer.
 * There is hardly any official document so fields are added taking standard refer type.
 * Originally number of fields were less and overtime some of these modified or added by various management systems.
 */
public class ReferImporter extends Importer {

    private static final Pattern Z_PATTERN = Pattern.compile("%0 .*");
    private static final String ENDOFRECORD = "__EOREOR__";

    @Override
    public String getId() {
        return "refer-bibIX";
    }

    @Override
    public String getName() {
        return "Refer/BibIX";
    }

    @Override
    public FileType getFileType() {
        return StandardFileType.TXT;
    }

    @Override
    public String getDescription() {
        return Localization.lang("Import for the Refer/BibIX file.");
    }

    @Override
    public boolean isRecognizedFormat(@NonNull BufferedReader reader) throws IOException {
        // look for the "%0 *" line;
        String str;
        while ((str = reader.readLine()) != null) {
            if (Z_PATTERN.matcher(str).matches()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ParserResult importDatabase(@NonNull BufferedReader reader) throws IOException {
        List<BibEntry> bibEntryList = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        String str;
        boolean first = true;

        // add all entry/s present in the file
        while ((str = reader.readLine()) != null) {
            if (str.indexOf("%0") == 0) {
                if (!first) {
                    sb.append(ENDOFRECORD);
                }
                first = false;
            }
            sb.append(str).append('\n');
        }

        List<String> allEntries = new ArrayList<>(List.of(sb.toString().split(ENDOFRECORD)));
        stringToBibEntry(bibEntryList, allEntries);

        return new ParserResult(bibEntryList);
    }

    private void stringToBibEntry(List<BibEntry> bibEntryList, List<String> allEntries) {
        Map<Field, String> fieldMap = new HashMap<>();
        EntryType type;
        StringBuilder author;
        StringBuilder editor;
        AtomicBoolean isEdited;

        for (String entry : allEntries) {
            List<String> fields = new ArrayList<>(List.of(entry.trim().substring(1).split("\n%")));
            type = BibEntry.DEFAULT_TYPE;
            author = new StringBuilder();
            editor = new StringBuilder();
            isEdited = new AtomicBoolean(false);

            for (String field : fields) {
                if (field.length() < 3) {
                    continue;
                }
                String tag = field.substring(0, 1);
                String val = field.substring(2);

                switch (tag) {
                    case "0" ->
                            type = getType(val, isEdited);
                    case "7" ->
                            fieldMap.put(StandardField.EDITION, val);
                    case "A" ->
                            addAuthor(author, val);
                    case "B" ->
                            addTag(fieldMap, type, val, "B");
                    case "C" ->
                            fieldMap.put(StandardField.ADDRESS, val);
                    case "D" ->
                            fieldMap.put(StandardField.YEAR, val);
                    case "E" ->
                            addEditor(editor, val);
                    case "F" ->
                            fieldMap.put(InternalField.KEY_FIELD, CitationKeyGenerator.cleanKey(val, ""));
                    case "G" ->
                            fieldMap.put(StandardField.LANGUAGE, val);
                    case "I" ->
                            addTag(fieldMap, type, val, "I");
                    case "J" ->
                            fieldMap.putIfAbsent(StandardField.JOURNAL, val);
                    case "K" ->
                            fieldMap.put(StandardField.KEYWORDS, val);
                    case "N" ->
                            fieldMap.put(StandardField.ISSUE, val);
                    case "O" ->
                            fieldMap.put(StandardField.NOTE, val);
                    case "P" ->
                            fieldMap.put(StandardField.PAGES, val.replaceAll("([0-9]) *- *([0-9])", "$1--$2"));
                    case "R" ->
                            addTag(fieldMap, type, val, "R");
                    case "S" ->
                            fieldMap.put(StandardField.SERIES, val);
                    case "T" ->
                            fieldMap.put(StandardField.TITLE, val);
                    case "U" ->
                            fieldMap.put(StandardField.URL, val);
                    case "V" ->
                            fieldMap.put(StandardField.VOLUME, val);
                    case "X" ->
                            fieldMap.put(StandardField.ABSTRACT, val);
                    case "?" ->
                            fieldMap.put(StandardField.TRANSLATOR, val);
                    case "@" ->
                            fieldMap.put(StandardField.ISBN, val);
                    default ->
                            addTag(fieldMap, type, val, "default");
                }
            }

            postFix(fieldMap, author, editor, isEdited);

            BibEntry singleBibEntry = new BibEntry(type);
            singleBibEntry.setField(fieldMap);
            if (!entry.isEmpty()) {
                bibEntryList.add(singleBibEntry);
            }
            fieldMap.clear();
        }
    }

    private EntryType getType(String val, AtomicBoolean isEdited) {
        EntryType type;
        if (val.indexOf("Journal") == 0) {
            type = StandardEntryType.Article;
        } else if (val.indexOf("Book Section") == 0) {
            type = StandardEntryType.InCollection;
        } else if (val.indexOf("Book") == 0) {
            type = StandardEntryType.Book;
        } else if (val.indexOf("Edited Book") == 0) {
            isEdited.set(true);
            type = StandardEntryType.Book;
        } else if (val.indexOf("Conference") == 0) {
            type = StandardEntryType.InProceedings;
        } else if (val.indexOf("Report") == 0) {
            type = StandardEntryType.TechReport;
        } else if (val.indexOf("Review") == 0) {
            type = StandardEntryType.Article;
        } else if (val.indexOf("Thesis") == 0) {
            type = StandardEntryType.PhdThesis;
        } else {
            type = BibEntry.DEFAULT_TYPE;
        }
        return type;
    }

    private void addAuthor(StringBuilder auth, String val) {
        if (auth.isEmpty()) {
            auth.append(val);
        } else {
            auth.append(" and ").append(val);
        }
    }

    private void addEditor(StringBuilder edt, String val) {
        if (edt.isEmpty()) {
            edt.append(val);
        } else {
            edt.append(" and ").append(val);
        }
    }

    private void addTag(Map<Field, String> m, EntryType type, String val, String tag) {
        switch (tag) {
            case "B" -> {
                if (type.equals(StandardEntryType.Article)) {
                    m.put(StandardField.JOURNAL, val);
                } else if (type.equals(StandardEntryType.Book) || type.equals(StandardEntryType.InBook)) {
                    m.put(StandardField.SERIES, val);
                } else {
                    /* type = inproceedings */
                    m.put(StandardField.BOOKTITLE, val);
                }
            }
            case "I" -> {
                if (type.equals(StandardEntryType.PhdThesis)) {
                    m.put(StandardField.SCHOOL, val);
                } else {
                    m.put(StandardField.PUBLISHER, val);
                }
            }
            case "R" -> {
                // note: R can be type in thesis but format is unknown
                String doi = val;
                if (doi.startsWith("doi:")) {
                    doi = doi.substring(4);
                }
                m.put(StandardField.DOI, doi);
            }
            default -> {
                // other fields e.g. header(if any), rights, table of content, government ordering, call number, price, location of archive/conference etc.
                if (m.containsKey(StandardField.NOTE)) {
                    String oldValue = m.get(StandardField.NOTE);
                    String newValue = (oldValue == null ? "" : oldValue + "; ") + val;
                    m.put(StandardField.NOTE, newValue);
                } else {
                    m.put(StandardField.NOTE, val);
                }
            }
        }
    }

    private void postFix(Map<Field, String> hm, StringBuilder author, StringBuilder editor, AtomicBoolean isEditedBook) {
        // In some of the documentation editor name can be found in place of author name
        if (isEditedBook.get() && editor.toString().isEmpty()) {
            editor = new StringBuilder(author.toString());
            author = new StringBuilder();
        }

        // fix authors/editor comma
        if (!"".contentEquals(author)) {
            hm.put(StandardField.AUTHOR, fixAuthor(author.toString()));
        }
        if (!"".contentEquals(editor)) {
            hm.put(StandardField.EDITOR, fixAuthor(editor.toString()));
        }
    }

    /**
     * We must be careful about the author names, since they can be presented differently
     * by different sources. Normally each %A tag brings one name, and we get the authors
     * separated by " and ". This is the correct behaviour.
     * One source lists the names separated by comma, with a comma at the end. We can detect
     * this format and fix it.
     *
     * @param s The author string
     * @return The fixed author string
     */
    private static String fixAuthor(String s) {
        int index = s.indexOf(" and ");
        if (index >= 0) {
            return AuthorList.fixAuthorLastNameFirst(s);
        }
        // Look for the comma at the end:
        index = s.lastIndexOf(',');
        if (index == (s.length() - 1)) {
            String mod = s.substring(0, s.length() - 1).replace(", ", " and ");
            return AuthorList.fixAuthorLastNameFirst(mod);
        } else {
            return AuthorList.fixAuthorLastNameFirst(s);
        }
    }
}
