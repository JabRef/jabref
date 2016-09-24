package net.sf.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import net.sf.jabref.logic.importer.Importer;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.util.FileExtensions;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.model.entry.AuthorList;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.entry.MonthUtil;

/**
 * Imports a Biblioscape Tag File. The format is described on
 * http://www.biblioscape.com/manual_bsp/Biblioscape_Tag_File.htm
 * Several Biblioscape field types are ignored. Others are only included in the BibTeX
 * field "comment".
 */
public class RisImporter extends Importer {

    private static final Pattern RECOGNIZED_FORMAT_PATTERN = Pattern.compile("TY  - .*");


    @Override
    public String getName() {
        return "RIS";
    }

    @Override
    public FileExtensions getExtensions() {
        return FileExtensions.RIS;
    }

    @Override
    public String getDescription() {
        return "Imports a Biblioscape Tag File.";
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader reader) throws IOException {
        // Our strategy is to look for the "TY  - *" line.
        return reader.lines().anyMatch(line -> RECOGNIZED_FORMAT_PATTERN.matcher(line).find());
    }

    @Override
    public ParserResult importDatabase(BufferedReader reader) throws IOException {
        List<BibEntry> bibitems = new ArrayList<>();

        //use optional here, so that no exception will be thrown if the file is empty
        Optional<String> OptionalLines = reader.lines().reduce((line, nextline) -> line + "\n" + nextline);
        String linesAsString = OptionalLines.isPresent() ? OptionalLines.get() : "";

        String[] entries = linesAsString.replace("\u2013", "-").replace("\u2014", "--").replace("\u2015", "--")
                .split("ER  -.*\\n");

        for (String entry1 : entries) {

            String type = "";
            String author = "";
            String editor = "";
            String startPage = "";
            String endPage = "";
            String comment = "";
            Map<String, String> fields = new HashMap<>();

            String[] lines = entry1.split("\n");

            for (int j = 0; j < lines.length; j++) {
                StringBuilder current = new StringBuilder(lines[j]);
                boolean done = false;
                while (!done && (j < (lines.length - 1))) {
                    if ((lines[j + 1].length() >= 6) && !"  - ".equals(lines[j + 1].substring(2, 6))) {
                        if ((current.length() > 0) && !Character.isWhitespace(current.charAt(current.length() - 1))
                                && !Character.isWhitespace(lines[j + 1].charAt(0))) {
                            current.append(' ');
                        }
                        current.append(lines[j + 1]);
                        j++;
                    } else {
                        done = true;
                    }
                }
                String entry = current.toString();
                if (entry.length() < 6) {
                    continue;
                } else {
                    String tag = entry.substring(0, 2);
                    String value = entry.substring(6).trim();
                    if ("TY".equals(tag)) {
                        if ("BOOK".equals(value)) {
                            type = "book";
                        } else if ("JOUR".equals(value) || "MGZN".equals(value)) {
                            type = "article";
                        } else if ("THES".equals(value)) {
                            type = "phdthesis";
                        } else if ("UNPB".equals(value)) {
                            type = "unpublished";
                        } else if ("RPRT".equals(value)) {
                            type = "techreport";
                        } else if ("CONF".equals(value)) {
                            type = "inproceedings";
                        } else if ("CHAP".equals(value)) {
                            type = "incollection";//"inbook";
                        } else if ("PAT".equals(value)) {
                            type = "patent";
                        } else {
                            type = "other";
                        }
                    } else if ("T1".equals(tag) || "TI".equals(tag)) {
                        String oldVal = fields.get(FieldName.TITLE);
                        if (oldVal == null) {
                            fields.put(FieldName.TITLE, value);
                        } else {
                            if (oldVal.endsWith(":") || oldVal.endsWith(".") || oldVal.endsWith("?")) {
                                fields.put(FieldName.TITLE, oldVal + " " + value);
                            } else {
                                fields.put(FieldName.TITLE, oldVal + ": " + value);
                            }
                        }
                        fields.put(FieldName.TITLE, fields.get(FieldName.TITLE).replaceAll("\\s+", " ")); // Normalize whitespaces
                    } else if ("BT".equals(tag)) {
                        fields.put(FieldName.BOOKTITLE, value);
                    } else if ("T2".equals(tag) || "JO".equals(tag)) {
                        fields.put(FieldName.JOURNAL, value);
                    } else if ("T3".equals(tag)) {
                        fields.put(FieldName.SERIES, value);
                    } else if ("AU".equals(tag) || "A1".equals(tag)) {
                        if ("".equals(author)) {
                            author = value;
                        } else {
                            author += " and " + value;
                        }
                    } else if ("A2".equals(tag) || "A3".equals(tag) || "A4".equals(tag)) {
                        if (editor.isEmpty()) {
                            editor = value;
                        } else {
                            editor += " and " + value;
                        }
                    } else if ("JA".equals(tag) || "JF".equals(tag)) {
                        if ("inproceedings".equals(type)) {
                            fields.put(FieldName.BOOKTITLE, value);
                        } else {
                            fields.put(FieldName.JOURNAL, value);
                        }
                    } else if ("LA".equals(tag)) {
                        fields.put(FieldName.LANGUAGE, value);
                    } else if ("CA".equals(tag)) {
                        fields.put("caption", value);
                    } else if ("DB".equals(tag)) {
                        fields.put("database", value);
                    } else if ("IS".equals(tag)) {
                        fields.put(FieldName.NUMBER, value);
                    } else if ("SP".equals(tag)) {
                        startPage = value;
                    } else if ("PB".equals(tag)) {
                        if ("phdthesis".equals(type)) {
                            fields.put(FieldName.SCHOOL, value);
                        } else {
                            fields.put(FieldName.PUBLISHER, value);
                        }
                    } else if ("AD".equals(tag) || "CY".equals(tag)) {
                        fields.put(FieldName.ADDRESS, value);
                    } else if ("EP".equals(tag)) {
                        endPage = value;
                        if (!endPage.isEmpty()) {
                            endPage = "--" + endPage;
                        }
                    } else if ("ET".equals(tag)) {
                        fields.put(FieldName.EDITION, value);
                    } else if ("SN".equals(tag)) {
                        fields.put(FieldName.ISSN, value);
                    } else if ("VL".equals(tag)) {
                        fields.put(FieldName.VOLUME, value);
                    } else if ("N2".equals(tag) || "AB".equals(tag)) {
                        String oldAb = fields.get(FieldName.ABSTRACT);
                        if (oldAb == null) {
                            fields.put(FieldName.ABSTRACT, value);
                        } else {
                            fields.put(FieldName.ABSTRACT, oldAb + OS.NEWLINE + value);
                        }
                    } else if ("UR".equals(tag)) {
                        fields.put(FieldName.URL, value);
                    } else if (("Y1".equals(tag) || "PY".equals(tag) || "DA".equals(tag)) && (value.length() >= 4)) {
                        fields.put(FieldName.YEAR, value.substring(0, 4));
                        String[] parts = value.split("/");
                        if ((parts.length > 1) && !parts[1].isEmpty()) {
                            try {
                                int monthNumber = Integer.parseInt(parts[1]);
                                MonthUtil.Month month = MonthUtil.getMonthByNumber(monthNumber);
                                if (month.isValid()) {
                                    fields.put(FieldName.MONTH, month.bibtexFormat);
                                }
                            } catch (NumberFormatException ex) {
                                // The month part is unparseable, so we ignore it.
                            }
                        }
                    } else if ("KW".equals(tag)) {
                        if (fields.containsKey(FieldName.KEYWORDS)) {
                            String kw = fields.get(FieldName.KEYWORDS);
                            fields.put(FieldName.KEYWORDS, kw + ", " + value);
                        } else {
                            fields.put(FieldName.KEYWORDS, value);
                        }
                    } else if ("U1".equals(tag) || "U2".equals(tag) || "N1".equals(tag)) {
                        if (!comment.isEmpty()) {
                            comment = comment + " ";
                        }
                        comment = comment + value;
                    }
                    // Added ID import 2005.12.01, Morten Alver:
                    else if ("ID".equals(tag)) {
                        fields.put("refid", value);
                    } else if ("M3".equals(tag) || "DO".equals(tag)) {
                        addDoi(fields, value);
                    }
                }
                // fix authors
                if (!author.isEmpty()) {
                    author = AuthorList.fixAuthorLastNameFirst(author);
                    fields.put(FieldName.AUTHOR, author);
                }
                if (!editor.isEmpty()) {
                    editor = AuthorList.fixAuthorLastNameFirst(editor);
                    fields.put(FieldName.EDITOR, editor);
                }
                if (!comment.isEmpty()) {
                    fields.put("comment", comment);
                }

                fields.put(FieldName.PAGES, startPage + endPage);
            }
            BibEntry b = new BibEntry(DEFAULT_BIBTEXENTRY_ID, type); // id assumes an existing database so don't

            // Remove empty fields:
            fields.entrySet().removeIf(key -> (key.getValue() == null) || key.getValue().trim().isEmpty());

            // create one here
            b.setField(fields);
            bibitems.add(b);

        }
        return new ParserResult(bibitems);

    }

    private void addDoi(Map<String, String> hm, String val) {
        String doi = val.toLowerCase(Locale.ENGLISH);
        if (doi.startsWith("doi:")) {
            doi = doi.replaceAll("(?i)doi:", "").trim();
            hm.put(FieldName.DOI, doi);
        }
    }
}
