package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.BibtexEntryType;

/**
 * Importer for COPAC format.
 *
 * Documentation can be found online at:
 *
 * http://copac.ac.uk/faq/#format
 */
public class CopacImporter extends Importer {

    private static final Pattern COPAC_PATTERN = Pattern.compile("^\\s*TI- ");

    @Override
    public String getName() {
        return "Copac";
    }

    @Override
    public StandardFileType getFileType() {
        return StandardFileType.TXT;
    }

    @Override
    public String getId() {
        return "cpc";
    }

    @Override
    public String getDescription() {
        return "Importer for COPAC format.";
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader reader) throws IOException {
        String str;
        while ((str = reader.readLine()) != null) {
            if (CopacImporter.COPAC_PATTERN.matcher(str).find()) {
                return true;
            }
        }
        return false;
    }

    private static void setOrAppend(BibEntry b, Field field, String value, String separator) {
        if (b.hasField(field)) {
            b.setField(field, b.getField(field).get() + separator + value);
        } else {
            b.setField(field, value);
        }
    }

    @Override
    public ParserResult importDatabase(BufferedReader reader) throws IOException {
        Objects.requireNonNull(reader);

        List<String> entries = new LinkedList<>();
        StringBuilder sb = new StringBuilder();

        // Preprocess entries
        String str;

        while ((str = reader.readLine()) != null) {

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

        if (sb.length() > 0) {
            entries.add(sb.toString());
        }

        List<BibEntry> results = new LinkedList<>();

        for (String entry : entries) {

            // Copac does not contain enough information on the type of the
            // document. A book is assumed.
            BibEntry b = new BibEntry(BibtexEntryType.Book);

            String[] lines = entry.split("\n");

            for (String line1 : lines) {
                String line = line1.trim();
                if (line.length() < 4) {
                    continue;
                }
                String code = line.substring(0, 4);

                if ("TI- ".equals(code)) {
                    setOrAppend(b, StandardField.TITLE, line.substring(4).trim(), ", ");
                } else if ("AU- ".equals(code)) {
                    setOrAppend(b, StandardField.AUTHOR, line.substring(4).trim(), " and ");
                } else if ("PY- ".equals(code)) {
                    setOrAppend(b, StandardField.YEAR, line.substring(4).trim(), ", ");
                } else if ("PU- ".equals(code)) {
                    setOrAppend(b, StandardField.PUBLISHER, line.substring(4).trim(), ", ");
                } else if ("SE- ".equals(code)) {
                    setOrAppend(b, StandardField.SERIES, line.substring(4).trim(), ", ");
                } else if ("IS- ".equals(code)) {
                    setOrAppend(b, StandardField.ISBN, line.substring(4).trim(), ", ");
                } else if ("KW- ".equals(code)) {
                    setOrAppend(b, StandardField.KEYWORDS, line.substring(4).trim(), ", ");
                } else if ("NT- ".equals(code)) {
                    setOrAppend(b, StandardField.NOTE, line.substring(4).trim(), ", ");
                } else if ("PD- ".equals(code)) {
                    setOrAppend(b, new UnknownField("physicaldimensions"), line.substring(4).trim(), ", ");
                } else if ("DT- ".equals(code)) {
                    setOrAppend(b, new UnknownField("documenttype"), line.substring(4).trim(), ", ");
                } else {
                    setOrAppend(b, FieldFactory.parseField(code.substring(0, 2)), line.substring(4).trim(), ", ");
                }
            }
            results.add(b);
        }

        return new ParserResult(results);
    }
}
