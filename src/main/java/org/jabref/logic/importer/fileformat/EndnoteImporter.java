package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.importer.ImportFormatPreferences;
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
 *
 * check here for details on the format
 * http://libguides.csuchico.edu/c.php?g=414245&p=2822898
 */
public class EndnoteImporter extends Importer {

    private static final String ENDOFRECORD = "__EOREOR__";

    private static final Pattern A_PATTERN = Pattern.compile("%A .*");
    private static final Pattern E_PATTERN = Pattern.compile("%E .*");

    private final ImportFormatPreferences preferences;

    public EndnoteImporter(ImportFormatPreferences preferences) {
        this.preferences = preferences;
    }

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
        String author;
        EntryType type;
        String editor;
        String artnum;
        for (String entry : entries) {
            hm.clear();
            author = "";
            type = BibEntry.DEFAULT_TYPE;
            editor = "";
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

                if ("A".equals(prefix)) {
                    if ("".equals(author)) {
                        author = val;
                    } else {
                        author += " and " + val;
                    }
                } else if ("E".equals(prefix)) {
                    if ("".equals(editor)) {
                        editor = val;
                    } else {
                        editor += " and " + val;
                    }
                } else if ("T".equals(prefix)) {
                    hm.put(StandardField.TITLE, val);
                } else if ("0".equals(prefix)) {
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
                } else if ("7".equals(prefix)) {
                    hm.put(StandardField.EDITION, val);
                } else if ("C".equals(prefix)) {
                    hm.put(StandardField.ADDRESS, val);
                } else if ("D".equals(prefix)) {
                    hm.put(StandardField.YEAR, val);
                } else if ("8".equals(prefix)) {
                    hm.put(StandardField.DATE, val);
                } else if ("J".equals(prefix)) {
                    // "Alternate journal. Let's set it only if no journal
                    // has been set with %B.
                    hm.putIfAbsent(StandardField.JOURNAL, val);
                } else if ("B".equals(prefix)) {
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
                } else if ("I".equals(prefix)) {
                    if (type.equals(StandardEntryType.PhdThesis)) {
                        hm.put(StandardField.SCHOOL, val);
                    } else {
                        hm.put(StandardField.PUBLISHER, val);
                    }
                } else if ("P".equals(prefix)) {
                    // replace single dash page ranges (23-45) with double dashes (23--45):
                    hm.put(StandardField.PAGES, val.replaceAll("([0-9]) *- *([0-9])", "$1--$2"));
                } else if ("V".equals(prefix)) {
                    hm.put(StandardField.VOLUME, val);
                } else if ("N".equals(prefix)) {
                    hm.put(StandardField.NUMBER, val);
                } else if ("U".equals(prefix)) {
                    hm.put(StandardField.URL, val);
                } else if ("R".equals(prefix)) {
                    String doi = val;
                    if (doi.startsWith("doi:")) {
                        doi = doi.substring(4);
                    }
                    hm.put(StandardField.DOI, doi);
                } else if ("O".equals(prefix)) {
                    // Notes may contain Article number
                    if (val.startsWith("Artn")) {
                        String[] tokens = val.split("\\s");
                        artnum = tokens[1];
                    } else {
                        hm.put(StandardField.NOTE, val);
                    }
                } else if ("K".equals(prefix)) {
                    hm.put(StandardField.KEYWORDS, val);
                } else if ("X".equals(prefix)) {
                    hm.put(StandardField.ABSTRACT, val);
                } else if ("9".equals(prefix)) {
                    if (val.indexOf("Ph.D.") == 0) {
                        type = StandardEntryType.PhdThesis;
                    }
                    if (val.indexOf("Masters") == 0) {
                        type = StandardEntryType.MastersThesis;
                    }
                } else if ("F".equals(prefix)) {
                    hm.put(InternalField.KEY_FIELD, CitationKeyGenerator.cleanKey(val, ""));
                }
            }

            // For Edited Book, EndNote puts the editors in the author field.
            // We want them in the editor field so that bibtex knows it's an edited book
            if (isEditedBook && "".equals(editor)) {
                editor = author;
                author = "";
            }

            // fixauthorscomma
            if (!"".equals(author)) {
                hm.put(StandardField.AUTHOR, fixAuthor(author));
            }
            if (!"".equals(editor)) {
                hm.put(StandardField.EDITOR, fixAuthor(editor));
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
