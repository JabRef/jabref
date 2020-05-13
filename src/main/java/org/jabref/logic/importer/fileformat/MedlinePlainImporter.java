package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.util.OS;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

/**
 * Importer for the MEDLINE Plain format.
 * <p>
 * check here for details on the format http://www.nlm.nih.gov/bsd/mms/medlineelements.html
 */
public class MedlinePlainImporter extends Importer {

    private static final Pattern PMID_PATTERN = Pattern.compile("PMID.*-.*");
    private static final Pattern PMC_PATTERN = Pattern.compile("PMC.*-.*");
    private static final Pattern PMCR_PATTERN = Pattern.compile("PMCR.*-.*");
    private static final Pattern CREATE_DATE_PATTERN = Pattern.compile("\\d{4}/[0123]?\\d/\\s?[012]\\d:[0-5]\\d");
    private static final Pattern COMPLETE_DATE_PATTERN = Pattern.compile("\\d{8}");

    @Override
    public String getName() {
        return "Medline/PubMed Plain";
    }

    @Override
    public StandardFileType getFileType() {
        return StandardFileType.MEDLINE_PLAIN;
    }

    @Override
    public String getDescription() {
        return "Importer for the MedlinePlain format.";
    }

    @Override
    public String getId() {
        return "medlineplain";
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader reader) throws IOException {

        // Our strategy is to look for the "PMID  - *", "PMC.*-.*", or "PMCR.*-.*" line
        // (i.e., PubMed Unique Identifier, PubMed Central Identifier, PubMed Central Release)
        String str;
        while ((str = reader.readLine()) != null) {
            if (PMID_PATTERN.matcher(str).find() || PMC_PATTERN.matcher(str).find()
                    || PMCR_PATTERN.matcher(str).find()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ParserResult importDatabase(BufferedReader reader) throws IOException {
        List<BibEntry> bibitems = new ArrayList<>();

        // use optional here, so that no exception will be thrown if the file is empty
        String linesAsString = reader.lines().reduce((line, nextline) -> line + "\n" + nextline).orElse("");

        String[] entries = linesAsString.replace("\u2013", "-").replace("\u2014", "--").replace("\u2015", "--")
                                        .split("\\n\\n");

        for (String entry1 : entries) {

            if (entry1.trim().isEmpty() || !entry1.contains("-")) {
                continue;
            }

            EntryType type = BibEntry.DEFAULT_TYPE;
            String author = "";
            String editor = "";
            String comment = "";
            Map<Field, String> fieldConversionMap = new HashMap<>();

            String[] lines = entry1.split("\n");

            for (int j = 0; j < lines.length; j++) {

                StringBuilder current = new StringBuilder(lines[j]);
                boolean done = false;

                while (!done && (j < (lines.length - 1))) {
                    if (lines[j + 1].length() <= 4) {
                        j++;
                        continue;
                    }
                    if (lines[j + 1].charAt(4) != '-') {
                        if ((current.length() > 0) && !Character.isWhitespace(current.charAt(current.length() - 1))) {
                            current.append(' ');
                        }
                        current.append(lines[j + 1].trim());
                        j++;
                    } else {
                        done = true;
                    }
                }
                String entry = current.toString();
                if (!checkLineValidity(entry)) {
                    continue;
                }

                String label = entry.substring(0, entry.indexOf('-')).trim();
                String value = entry.substring(entry.indexOf('-') + 1).trim();

                if ("PT".equals(label)) {
                    type = addSourceType(value, type);
                }
                addDates(fieldConversionMap, label, value);
                addAbstract(fieldConversionMap, label, value);
                addTitles(fieldConversionMap, label, value, type);
                addIDs(fieldConversionMap, label, value);
                addStandardNumber(fieldConversionMap, label, value);

                if ("FAU".equals(label)) {
                    if ("".equals(author)) {
                        author = value;
                    } else {
                        author += " and " + value;
                    }
                } else if ("FED".equals(label)) {
                    if ("".equals(editor)) {
                        editor = value;
                    } else {
                        editor += " and " + value;
                    }
                }

                // store the fields in a map
                Map<String, Field> hashMap = new HashMap<>();
                hashMap.put("PG", StandardField.PAGES);
                hashMap.put("PL", StandardField.ADDRESS);
                hashMap.put("PHST", new UnknownField("history"));
                hashMap.put("PST", new UnknownField("publication-status"));
                hashMap.put("VI", StandardField.VOLUME);
                hashMap.put("LA", StandardField.LANGUAGE);
                hashMap.put("PUBM", new UnknownField("model"));
                hashMap.put("RN", new UnknownField("registry-number"));
                hashMap.put("NM", new UnknownField("substance-name"));
                hashMap.put("OCI", new UnknownField("copyright-owner"));
                hashMap.put("CN", new UnknownField("corporate"));
                hashMap.put("IP", StandardField.ISSUE);
                hashMap.put("EN", StandardField.EDITION);
                hashMap.put("GS", new UnknownField("gene-symbol"));
                hashMap.put("GN", StandardField.NOTE);
                hashMap.put("GR", new UnknownField("grantno"));
                hashMap.put("SO", new UnknownField("source"));
                hashMap.put("NR", new UnknownField("number-of-references"));
                hashMap.put("SFM", new UnknownField("space-flight-mission"));
                hashMap.put("STAT", new UnknownField("status"));
                hashMap.put("SB", new UnknownField("subset"));
                hashMap.put("OTO", new UnknownField("termowner"));
                hashMap.put("OWN", StandardField.OWNER);

                // add the fields to hm
                for (Map.Entry<String, Field> mapEntry : hashMap.entrySet()) {
                    String medlineKey = mapEntry.getKey();
                    Field bibtexKey = mapEntry.getValue();
                    if (medlineKey.equals(label)) {
                        fieldConversionMap.put(bibtexKey, value);
                    }
                }

                if ("IRAD".equals(label) || "IR".equals(label) || "FIR".equals(label)) {
                    String oldInvestigator = fieldConversionMap.get(new UnknownField("investigator"));
                    if (oldInvestigator == null) {
                        fieldConversionMap.put(new UnknownField("investigator"), value);
                    } else {
                        fieldConversionMap.put(new UnknownField("investigator"), oldInvestigator + ", " + value);
                    }
                } else if ("MH".equals(label) || "OT".equals(label)) {
                    if (!fieldConversionMap.containsKey(StandardField.KEYWORDS)) {
                        fieldConversionMap.put(StandardField.KEYWORDS, value);
                    } else {
                        String kw = fieldConversionMap.get(StandardField.KEYWORDS);
                        fieldConversionMap.put(StandardField.KEYWORDS, kw + ", " + value);
                    }
                } else if ("CON".equals(label) || "CIN".equals(label) || "EIN".equals(label) || "EFR".equals(label)
                        || "CRI".equals(label) || "CRF".equals(label) || "PRIN".equals(label) || "PROF".equals(label)
                        || "RPI".equals(label) || "RPF".equals(label) || "RIN".equals(label) || "ROF".equals(label)
                        || "UIN".equals(label) || "UOF".equals(label) || "SPIN".equals(label) || "ORI".equals(label)) {
                    if (!comment.isEmpty()) {
                        comment = comment + "\n";
                    }
                    comment = comment + value;
                }
            }
            fixAuthors(fieldConversionMap, author, StandardField.AUTHOR);
            fixAuthors(fieldConversionMap, editor, StandardField.EDITOR);
            if (!comment.isEmpty()) {
                fieldConversionMap.put(StandardField.COMMENT, comment);
            }

            BibEntry b = new BibEntry(type);

            // create one here
            b.setField(fieldConversionMap);
            bibitems.add(b);
        }

        return new ParserResult(bibitems);
    }

    private boolean checkLineValidity(String line) {
        return (line.length() >= 5) && (line.charAt(4) == '-');
    }

    private EntryType addSourceType(String value, EntryType type) {
        String val = value.toLowerCase(Locale.ENGLISH);
        switch (val) {
            case "book":
                return StandardEntryType.Book;
            case "journal article":
            case "classical article":
            case "corrected and republished article":
            case "historical article":
            case "introductory journal article":
            case "newspaper article":
                return StandardEntryType.Article;
            case "clinical conference":
            case "consensus development conference":
            case "consensus development conference, nih":
                return StandardEntryType.Conference;
            case "technical report":
                return StandardEntryType.TechReport;
            case "editorial":
                return StandardEntryType.InProceedings;
            case "overall":
                return StandardEntryType.Proceedings;
            default:
                return type;
        }
    }

    private void addStandardNumber(Map<Field, String> hm, String lab, String value) {
        if ("IS".equals(lab)) {
            Field key = StandardField.ISSN;
            // it is possible to have two issn, one for electronic and for print
            // if there are two then it comes at the end in brackets (electronic) or (print)
            // so search for the brackets
            if (value.indexOf('(') > 0) {
                int keyStart = value.indexOf('(');
                int keyEnd = value.indexOf(')');
                key = new UnknownField(value.substring(keyStart + 1, keyEnd) + "-" + key);
                String numberValue = value.substring(0, keyStart - 1);
                hm.put(key, numberValue);
            } else {
                hm.put(key, value);
            }
        } else if ("ISBN".equals(lab)) {
            hm.put(StandardField.ISBN, value);
        }
    }

    private void fixAuthors(Map<Field, String> hm, String author, Field field) {
        if (!author.isEmpty()) {
            String fixedAuthor = AuthorList.fixAuthorLastNameFirst(author);
            hm.put(field, fixedAuthor);
        }
    }

    private void addIDs(Map<Field, String> hm, String lab, String value) {
        if ("AID".equals(lab)) {
            Field key = new UnknownField("article-id");
            String idValue = value;
            if (value.startsWith("doi:")) {
                idValue = idValue.replaceAll("(?i)doi:", "").trim();
                key = StandardField.DOI;
            } else if (value.indexOf('[') > 0) {
                int startOfIdentifier = value.indexOf('[');
                int endOfIdentifier = value.indexOf(']');
                key = new UnknownField("article-" + value.substring(startOfIdentifier + 1, endOfIdentifier));
                idValue = value.substring(0, startOfIdentifier - 1);
            }
            hm.put(key, idValue);
        } else if ("LID".equals(lab)) {
            hm.put(new UnknownField("location-id"), value);
        } else if ("MID".equals(lab)) {
            hm.put(new UnknownField("manuscript-id"), value);
        } else if ("JID".equals(lab)) {
            hm.put(new UnknownField("nlm-unique-id"), value);
        } else if ("OID".equals(lab)) {
            hm.put(new UnknownField("other-id"), value);
        } else if ("SI".equals(lab)) {
            hm.put(new UnknownField("second-id"), value);
        }
    }

    private void addTitles(Map<Field, String> hm, String lab, String val, EntryType type) {
        if ("TI".equals(lab)) {
            String oldVal = hm.get(StandardField.TITLE);
            if (oldVal == null) {
                hm.put(StandardField.TITLE, val);
            } else {
                if (oldVal.endsWith(":") || oldVal.endsWith(".") || oldVal.endsWith("?")) {
                    hm.put(StandardField.TITLE, oldVal + " " + val);
                } else {
                    hm.put(StandardField.TITLE, oldVal + ": " + val);
                }
            }
        } else if ("BTI".equals(lab) || "CTI".equals(lab)) {
            hm.put(StandardField.BOOKTITLE, val);
        } else if ("JT".equals(lab)) {
            if (type.equals(StandardEntryType.InProceedings)) {
                hm.put(StandardField.BOOKTITLE, val);
            } else {
                hm.put(StandardField.JOURNAL, val);
            }
        } else if ("CTI".equals(lab)) {
            hm.put(new UnknownField("collection-title"), val);
        } else if ("TA".equals(lab)) {
            hm.put(new UnknownField("title-abbreviation"), val);
        } else if ("TT".equals(lab)) {
            hm.put(new UnknownField("transliterated-title"), val);
        } else if ("VTI".equals(lab)) {
            hm.put(new UnknownField("volume-title"), val);
        }
    }

    private void addAbstract(Map<Field, String> hm, String lab, String value) {
        String abstractValue = "";
        if ("AB".equals(lab)) {
            // adds copyright information that comes at the end of an abstract
            if (value.contains("Copyright")) {
                int copyrightIndex = value.lastIndexOf("Copyright");
                // remove the copyright from the field since the name of the field is copyright
                String copyrightInfo = value.substring(copyrightIndex).replaceAll("Copyright ", "");
                hm.put(new UnknownField("copyright"), copyrightInfo);
                abstractValue = value.substring(0, copyrightIndex);
            } else {
                abstractValue = value;
            }
            String oldAb = hm.get(StandardField.ABSTRACT);
            if (oldAb == null) {
                hm.put(StandardField.ABSTRACT, abstractValue);
            } else {
                hm.put(StandardField.ABSTRACT, oldAb + OS.NEWLINE + abstractValue);
            }
        } else if ("OAB".equals(lab) || "OABL".equals(lab)) {
            hm.put(new UnknownField("other-abstract"), value);
        }
    }

    private void addDates(Map<Field, String> hm, String lab, String val) {
        if ("CRDT".equals(lab) && isCreateDateFormat(val)) {
            hm.put(new UnknownField("create-date"), val);
        } else if ("DEP".equals(lab) && isDateFormat(val)) {
            hm.put(new UnknownField("electronic-publication"), val);
        } else if ("DA".equals(lab) && isDateFormat(val)) {
            hm.put(new UnknownField("date-created"), val);
        } else if ("DCOM".equals(lab) && isDateFormat(val)) {
            hm.put(new UnknownField("completed"), val);
        } else if ("LR".equals(lab) && isDateFormat(val)) {
            hm.put(new UnknownField("revised"), val);
        } else if ("DP".equals(lab)) {
            String[] parts = val.split(" ");
            hm.put(StandardField.YEAR, parts[0]);
            if ((parts.length > 1) && !parts[1].isEmpty()) {
                hm.put(StandardField.MONTH, parts[1]);
            }
        } else if ("EDAT".equals(lab) && isCreateDateFormat(val)) {
            hm.put(new UnknownField("publication"), val);
        } else if ("MHDA".equals(lab) && isCreateDateFormat(val)) {
            hm.put(new UnknownField("mesh-date"), val);
        }
    }

    private boolean isCreateDateFormat(String value) {
        return CREATE_DATE_PATTERN.matcher(value).matches();
    }

    private boolean isDateFormat(String value) {
        return COMPLETE_DATE_PATTERN.matcher(value).matches();
    }
}
