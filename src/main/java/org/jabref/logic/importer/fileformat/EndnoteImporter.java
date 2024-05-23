package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

/**
 * Importer for the Refer/Endnote format.
 * modified to use article number for pages if pages are missing (some
 * journals, e.g., Physical Review Letters, don't use pages anymore)
 * <br>
 * check here for details on the format
 * <a href="http://libguides.csuchico.edu/c.php?g=414245&p=2822898">...</a>
 */
public class EndnoteImporter extends Importer {

    private static final String ENDOFRECORD = "__EOREOR__";

    private static final Pattern A_PATTERN = Pattern.compile("%A .*");
    private static final Pattern E_PATTERN = Pattern.compile("%E .*");

    @Override
    public String getName() {
        return "Refer/Endnote";
    }

    @Override
    public StandardFileType getFileType() {
        return StandardFileType.ENDNOTE;
    }

    @Override
    public String getId() {
        return "refer";
    }

    @Override
    public String getDescription() {
        return "Importer for the Refer/Endnote format. Modified to use article number for pages if pages are missing.";
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader reader) throws IOException {
        // Our strategy is to look for the "%A *" line.
        String str;
        while ((str = reader.readLine()) != null) {
            if (A_PATTERN.matcher(str).matches() || E_PATTERN.matcher(str).matches()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ParserResult importDatabase(BufferedReader reader) throws IOException {
        List<BibEntry> bibitems = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        String str;
        boolean first = true;
        while ((str = reader.readLine()) != null) {
            str = str.trim();
            if (str.indexOf("%0") == 0) {
                if (first) {
                    first = false;
                } else {
                    sb.append(ENDOFRECORD);
                }
                sb.append(str);
            } else {
                sb.append(str);
            }
            sb.append('\n');
        }

        String[] entries = sb.toString().split(ENDOFRECORD);
        Map<Field, String> hm = new HashMap<>();
        StringBuilder author;
        EntryType type;
        StringBuilder editor;
        String artnum;
        for (String entry : entries) {
            hm.clear();
            author = new StringBuilder();
            type = BibEntry.DEFAULT_TYPE;
            editor = new StringBuilder();
            artnum = "";

            boolean isEditedBook = false;
            String[] fields = entry.trim().substring(1).split("\n%");
            for (String field : fields) {
                if (field.length() < 3) {
                    continue;
                }

                /*
                 * Details of Refer format for Journal Article and Book:
                 *
                 * Generic Ref Journal Article Book Code Author %A Author Author Year %D
                 * Year Year Title %T Title Title Secondary Author %E Series Editor
                 * Secondary Title %B Journal Series Title Place Published %C City
                 * Publisher %I Publisher Volume %V Volume Volume Number of Volumes %6
                 * Number of Volumes Number %N Issue Pages %P Pages Number of Pages
                 * Edition %7 Edition Subsidiary Author %? Translator Alternate Title %J
                 * Alternate Journal Label %F Label Label Keywords %K Keywords Keywords
                 * Abstract %X Abstract Abstract Notes %O Notes Notes
                 */

                String prefix = field.substring(0, 1);

                String val = field.substring(2);

                switch (prefix) {
                    case "A" -> {
                        if (author.isEmpty()) {
                            author = new StringBuilder(val);
                        } else {
                            author.append(" and ").append(val);
                        }
                    }
                    case "E" -> {
                        if (editor.isEmpty()) {
                            editor = new StringBuilder(val);
                        } else {
                            editor.append(" and ").append(val);
                        }
                    }
                    case "T" ->
                            hm.put(StandardField.TITLE, val);
                    case "0" -> {
                        if (val.indexOf("Journal") == 0) {
                            type = StandardEntryType.Article;
                        } else if (val.indexOf("Book Section") == 0) {
                            type = StandardEntryType.InCollection;
                        } else if (val.indexOf("Book") == 0) {
                            type = StandardEntryType.Book;
                        } else if (val.indexOf("Edited Book") == 0) {
                            type = StandardEntryType.Book;
                            isEditedBook = true;
                        } else if (val.indexOf("Conference") == 0) {
                            type = StandardEntryType.InProceedings;
                        } else if (val.indexOf("Report") == 0) {
                            type = StandardEntryType.TechReport;
                        } else if (val.indexOf("Review") == 0) {
                            type = StandardEntryType.Article;
                        } else if (val.indexOf("Thesis") == 0) {
                            type = StandardEntryType.PhdThesis;
                        } else {
                            type = BibEntry.DEFAULT_TYPE; //
                        }
                    }
                    case "7" ->
                            hm.put(StandardField.EDITION, val);
                    case "C" ->
                            hm.put(StandardField.ADDRESS, val);
                    case "D" ->
                            hm.put(StandardField.YEAR, val);
                    case "8" ->
                            hm.put(StandardField.DATE, val);
                    case "J" ->
                        // "Alternate journal. Let's set it only if no journal
                        // has been set with %B.
                            hm.putIfAbsent(StandardField.JOURNAL, val);
                    case "B" -> {
                        // This prefix stands for "journal" in a journal entry, and
                        // "series" in a book entry.
                        if (type.equals(StandardEntryType.Article)) {
                            hm.put(StandardField.JOURNAL, val);
                        } else if (type.equals(StandardEntryType.Book) || type.equals(StandardEntryType.InBook)) {
                            hm.put(StandardField.SERIES, val);
                        } else {
                            /* type = inproceedings */
                            hm.put(StandardField.BOOKTITLE, val);
                        }
                    }
                    case "I" -> {
                        if (type.equals(StandardEntryType.PhdThesis)) {
                            hm.put(StandardField.SCHOOL, val);
                        } else {
                            hm.put(StandardField.PUBLISHER, val);
                        }
                    }
                    case "P" ->
                        // replace single dash page ranges (23-45) with double dashes (23--45):
                            hm.put(StandardField.PAGES, val.replaceAll("([0-9]) *- *([0-9])", "$1--$2"));
                    case "V" ->
                            hm.put(StandardField.VOLUME, val);
                    case "N" ->
                            hm.put(StandardField.NUMBER, val);
                    case "U" ->
                            hm.put(StandardField.URL, val);
                    case "R" -> {
                        String doi = val;
                        if (doi.startsWith("doi:")) {
                            doi = doi.substring(4);
                        }
                        hm.put(StandardField.DOI, doi);
                    }
                    case "O" -> {
                        // Notes may contain Article number
                        if (val.startsWith("Artn")) {
                            String[] tokens = val.split("\\s");
                            artnum = tokens[1];
                        } else {
                            hm.put(StandardField.NOTE, val);
                        }
                    }
                    case "K" ->
                            hm.put(StandardField.KEYWORDS, val);
                    case "X" ->
                            hm.put(StandardField.ABSTRACT, val);
                    case "9" -> {
                        if (val.indexOf("Ph.D.") == 0) {
                            type = StandardEntryType.PhdThesis;
                        }
                        if (val.indexOf("Masters") == 0) {
                            type = StandardEntryType.MastersThesis;
                        }
                    }
                    case "F" ->
                            hm.put(InternalField.KEY_FIELD, CitationKeyGenerator.cleanKey(val, ""));
                }
            }

            // For Edited Book, EndNote puts the editors in the author field.
            // We want them in the editor field so that bibtex knows it's an edited book
            if (isEditedBook && editor.toString().isEmpty()) {
                editor = new StringBuilder(author.toString());
                author = new StringBuilder();
            }

            // fixauthorscomma
            if (!"".contentEquals(author)) {
                hm.put(StandardField.AUTHOR, fixAuthor(author.toString()));
            }
            if (!"".contentEquals(editor)) {
                hm.put(StandardField.EDITOR, fixAuthor(editor.toString()));
            }
            // if pages missing and article number given, use the article number
            if (((hm.get(StandardField.PAGES) == null) || "-".equals(hm.get(StandardField.PAGES))) && !"".equals(artnum)) {
                hm.put(StandardField.PAGES, artnum);
            }

            BibEntry b = new BibEntry(type);
            b.setField(hm);
            if (!b.getFields().isEmpty()) {
                bibitems.add(b);
            }
        }

        return new ParserResult(bibitems);
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
