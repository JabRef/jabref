package net.sf.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.util.FileExtensions;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.model.entry.AuthorList;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.entry.MonthUtil;

import org.openjdk.jmh.generators.core.FileSystemDestination;

/**
 * Imports a Biblioscape Tag File. The format is described on
 * http://www.biblioscape.com/manual_bsp/Biblioscape_Tag_File.htm
 * Several Biblioscape field types are ignored. Others are only included in the BibTeX
 * field "comment".
 */
public class RisImporter extends ImportFormat {

    private static final Pattern RECOGNIZED_FORMAT_PATTERN = Pattern.compile("TY  - .*");


    @Override
    public String getFormatName() {
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
        StringBuilder sb = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
            sb.append('\n');
        }

        String[] entries = sb.toString().replace("\u2013", "-").replace("\u2014", "--").replace("\u2015", "--")
                .split("ER  -.*\\n");

        for (String entry1 : entries) {

            String type = "";
            String author = "";
            String editor = "";
            String startPage = "";
            String endPage = "";
            String comment = "";
            Map<String, String> hm = new HashMap<>();

            String[] fields = entry1.split("\n");

            for (int j = 0; j < fields.length; j++) {
                StringBuilder current = new StringBuilder(fields[j]);
                boolean done = false;
                while (!done && (j < (fields.length - 1))) {
                    if ((fields[j + 1].length() >= 6) && !"  - ".equals(fields[j + 1].substring(2, 6))) {
                        if ((current.length() > 0) && !Character.isWhitespace(current.charAt(current.length() - 1))
                                && !Character.isWhitespace(fields[j + 1].charAt(0))) {
                            current.append(' ');
                        }
                        current.append(fields[j + 1]);
                        j++;
                    } else {
                        done = true;
                    }
                }
                String entry = current.toString();
                if (entry.length() < 6) {
                    continue;
                } else {
                    String lab = entry.substring(0, 2);
                    String value = entry.substring(6).trim();
                    if ("TY".equals(lab)) {
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
                        } else if ("PAT".equals(lab)) {
                            type= "patent";
                        } else {
                            type = "other";
                        }
                    } else if ("T1".equals(lab) || "TI".equals(lab)) {
                        String oldVal = hm.get(FieldName.TITLE);
                        if (oldVal == null) {
                            hm.put(FieldName.TITLE, value);
                        } else {
                            if (oldVal.endsWith(":") || oldVal.endsWith(".") || oldVal.endsWith("?")) {
                                hm.put(FieldName.TITLE, oldVal + " " + value);
                            } else {
                                hm.put(FieldName.TITLE, oldVal + ": " + value);
                            }
                        }
                        hm.put(FieldName.TITLE, hm.get(FieldName.TITLE).replaceAll("\\s+", " ")); // Normalize whitespaces
                    } else if ( "BT".equals(lab)) {
                        hm.put(FieldName.BOOKTITLE, value);
                    }else if("T2".equals(lab) ){
                        hm.put(FieldName.JOURNAL, value);
                    } else if ("T3".equals(lab)) {
                        hm.put(FieldName.SERIES, value);
                    } else if ("AU".equals(lab) || "A1".equals(lab)) {
                        if ("".equals(author)) {
                            author = value;
                        } else {
                            author += " and " + value;
                        }
                    } else if ("A2".equals(lab) || "A3".equals(lab) || "A4".equals(lab)) {
                        if (editor.isEmpty()) {
                            editor = value;
                        } else {
                            editor += " and " + value;
                        }
                    } else if ("JA".equals(lab) || "JF".equals(lab) || "JO".equals(lab)) {
                        if ("inproceedings".equals(type)) {
                            hm.put(FieldName.BOOKTITLE, value);
                        } else {
                            hm.put(FieldName.JOURNAL, value);
                        }
                    } else if("CN".equals(lab)){
                        hm.put(FieldName.NUMBER, value);
                    } else if ("SP".equals(lab)) {
                        startPage = value;
                    } else if ("PB".equals(lab)) {
                        if ("phdthesis".equals(type)) {
                            hm.put(FieldName.SCHOOL, value);
                        } else {
                            hm.put(FieldName.PUBLISHER, value);
                        }
                    } else if ("AD".equals(lab) || "CY".equals(lab)) {
                        hm.put(FieldName.ADDRESS, value);
                    } else if ("EP".equals(lab)) {
                        endPage = value;
                        if (!endPage.isEmpty()) {
                            endPage = "--" + endPage;
                        }
                    }else if("ET".equals(lab)) {
                        hm.put(FieldName.EDITION, value);
                    } else if ("SN".equals(lab)) {
                        hm.put(FieldName.ISSN, value);
                    } else if ("VL".equals(lab)) {
                        hm.put(FieldName.VOLUME, value);
                    } else if ("IS".equals(lab)) {
                        hm.put(FieldName.NUMBER, value);
                    } else if ("N2".equals(lab) || "AB".equals(lab)) {
                        String oldAb = hm.get(FieldName.ABSTRACT);
                        if (oldAb == null) {
                            hm.put(FieldName.ABSTRACT, value);
                        } else {
                            hm.put(FieldName.ABSTRACT, oldAb + OS.NEWLINE + value);
                        }
                    } else if ("UR".equals(lab)) {
                        hm.put(FieldName.URL, value);
                    } else if (("Y1".equals(lab) || "PY".equals(lab)) && (value.length() >= 4)) {
                        hm.put(FieldName.YEAR, value);
                    }else if("DA".equals(lab)){
                        String[] parts = value.split("/");
                        if ((parts.length > 1) && !parts[1].isEmpty()) {
                            try {
                                int monthNumber = Integer.parseInt(parts[1]);
                                MonthUtil.Month month = MonthUtil.getMonthByNumber(monthNumber);
                                if (month.isValid()) {
                                    hm.put(FieldName.MONTH, month.bibtexFormat);
                                }
                            } catch (NumberFormatException ex) {
                                // The month part is unparseable, so we ignore it.
                            }
                    } else if ("KW".equals(lab)) {
                        if (hm.containsKey(FieldName.KEYWORDS)) {
                            String kw = hm.get(FieldName.KEYWORDS);
                            hm.put(FieldName.KEYWORDS, kw + ", " + value);
                        } else {
                            hm.put(FieldName.KEYWORDS, value);
                        }
                    } else if ("U1".equals(lab) || "U2".equals(lab) || "N1".equals(lab)) {
                        if (!comment.isEmpty()) {
                            comment = comment + " ";
                        }
                        comment = comment + value;
                    }
                    // Added ID import 2005.12.01, Morten Alver:
                    else if ("ID".equals(lab)) {
                        hm.put("refid", value);
                    } else if ("M3".equals(lab)) {
                        addDoi(hm, value);
                    } else if ("DO".equals(lab)) {
                        addDoi(hm, value);
                    }
                }
                // fix authors
                if (!author.isEmpty()) {
                    author = AuthorList.fixAuthorLastNameFirst(author);
                    hm.put(FieldName.AUTHOR, author);
                }
                if (!editor.isEmpty()) {
                    editor = AuthorList.fixAuthorLastNameFirst(editor);
                    hm.put(FieldName.EDITOR, editor);
                }
                if (!comment.isEmpty()) {
                    hm.put("comment", comment);
                }

                hm.put(FieldName.PAGES, startPage + endPage);
            }
            BibEntry b = new BibEntry(DEFAULT_BIBTEXENTRY_ID, type); // id assumes an existing database so don't

            // Remove empty fields:
            List<String> toRemove = new ArrayList<>();
            for (Map.Entry<String, String> key : hm.entrySet()) {
                String content = key.getValue();
                if ((content == null) || content.trim().isEmpty()) {
                    toRemove.add(key.getKey());
                }
            }
            for (String aToRemove : toRemove) {
                hm.remove(aToRemove);
            }

            // create one here
            b.setField(hm);
            bibitems.add(b);

        }

        return new ParserResult(bibitems);

    }

    private void addDoi(Map<String, String> hm, String val) {
        String doi = val.toLowerCase();
        if (doi.startsWith("doi:")) {
            doi = doi.replaceAll("(?i)doi:", "").trim();
            hm.put(FieldName.DOI, doi);
        }
    }
}
