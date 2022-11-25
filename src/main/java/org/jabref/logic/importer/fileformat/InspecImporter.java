package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.model.entry.types.StandardEntryType;

/**
 * INSPEC format importer.
 */
public class InspecImporter extends Importer {

    private static final Pattern INSPEC_PATTERN = Pattern.compile("Record.*INSPEC.*");

    @Override
    public String getName() {
        return "INSPEC";
    }

    @Override
    public StandardFileType getFileType() {
        return StandardFileType.TXT;
    }

    @Override
    public String getDescription() {
        return "INSPEC format importer.";
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader reader) throws IOException {
        // Our strategy is to look for the "PY <year>" line.
        String str;
        while ((str = reader.readLine()) != null) {
            if (INSPEC_PATTERN.matcher(str).find()) {
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
        while ((str = reader.readLine()) != null) {
            if (str.length() < 2) {
                continue;
            }
            if (str.indexOf("Record") == 0) {
                sb.append("__::__").append(str);
            } else {
                sb.append("__NEWFIELD__").append(str);
            }
        }
        String[] entries = sb.toString().split("__::__");
        EntryType type = BibEntry.DEFAULT_TYPE;
        Map<Field, String> h = new HashMap<>();
        for (String entry : entries) {
            if (entry.indexOf("Record") != 0) {
                continue;
            }
            h.clear();

            String[] fields = entry.split("__NEWFIELD__");
            for (String s : fields) {
                String f3 = s.substring(0, 2);
                String frest = s.substring(5);
                if ("TI".equals(f3)) {
                    h.put(StandardField.TITLE, frest);
                } else if ("PY".equals(f3)) {
                    h.put(StandardField.YEAR, frest);
                } else if ("AU".equals(f3)) {
                    h.put(StandardField.AUTHOR,
                            AuthorList.fixAuthorLastNameFirst(frest.replace(",-", ", ").replace(";", " and ")));
                } else if ("AB".equals(f3)) {
                    h.put(StandardField.ABSTRACT, frest);
                } else if ("ID".equals(f3)) {
                    h.put(StandardField.KEYWORDS, frest);
                } else if ("SO".equals(f3)) {
                    int m = frest.indexOf('.');
                    if (m >= 0) {
                        String jr = frest.substring(0, m);
                        h.put(StandardField.JOURNAL, jr.replace("-", " "));
                        frest = frest.substring(m);
                        m = frest.indexOf(';');
                        if (m >= 5) {
                            String yr = frest.substring(m - 5, m).trim();
                            h.put(StandardField.YEAR, yr);
                            frest = frest.substring(m);
                            m = frest.indexOf(':');
                            if (m >= 0) {
                                String pg = frest.substring(m + 1).trim();
                                h.put(StandardField.PAGES, pg);
                                String vol = frest.substring(1, m).trim();
                                h.put(StandardField.VOLUME, vol);
                            }
                        }
                    }
                } else if ("RT".equals(f3)) {
                    frest = frest.trim();
                    if ("Journal-Paper".equals(frest)) {
                        type = StandardEntryType.Article;
                    } else if ("Conference-Paper".equals(frest) || "Conference-Paper; Journal-Paper".equals(frest)) {
                        type = StandardEntryType.InProceedings;
                    } else {
                        type = EntryTypeFactory.parse(frest.replace(" ", ""));
                    }
                }
            }
            BibEntry b = new BibEntry(type);
            b.setField(h);

            bibitems.add(b);
        }

        return new ParserResult(bibitems);
    }
}
