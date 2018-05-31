package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.util.FileType;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

/**
 * Importer for the MEDLINE Plain format.
 *
 * check here for details on the format
 * http://www.nlm.nih.gov/bsd/mms/medlineelements.html
 *
 * @author vegeziel
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
    public FileType getFileType() {
        return FileType.MEDLINE_PLAIN;
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

        //use optional here, so that no exception will be thrown if the file is empty
        Optional<String> OptionalLines = reader.lines().reduce((line, nextline) -> line + "\n" + nextline);
        String linesAsString = OptionalLines.isPresent() ? OptionalLines.get() : "";

        String[] entries = linesAsString.replace("\u2013", "-").replace("\u2014", "--").replace("\u2015", "--")
                .split("\\n\\n");

        for (String entry1 : entries) {

            if (entry1.trim().isEmpty() || !entry1.contains("-")) {
                continue;
            }

            String type = BibEntry.DEFAULT_TYPE;
            String author = "";
            String editor = "";
            String comment = "";
            Map<String, String> fields = new HashMap<>();

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
                addDates(fields, label, value);
                addAbstract(fields, label, value);
                addTitles(fields, label, value, type);
                addIDs(fields, label, value);
                addStandardNumber(fields, label, value);

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

                //store the fields in a map
                Map<String, String> hashMap = new HashMap<>();
                hashMap.put("PG", FieldName.PAGES);
                hashMap.put("PL", FieldName.ADDRESS);
                hashMap.put("PHST", "history");
                hashMap.put("PST", "publication-status");
                hashMap.put("VI", FieldName.VOLUME);
                hashMap.put("LA", FieldName.LANGUAGE);
                hashMap.put("PUBM", "model");
                hashMap.put("RN", "registry-number");
                hashMap.put("NM", "substance-name");
                hashMap.put("OCI", "copyright-owner");
                hashMap.put("CN", "corporate");
                hashMap.put("IP", FieldName.ISSUE);
                hashMap.put("EN", FieldName.EDITION);
                hashMap.put("GS", "gene-symbol");
                hashMap.put("GN", FieldName.NOTE);
                hashMap.put("GR", "grantno");
                hashMap.put("SO", "source");
                hashMap.put("NR", "number-of-references");
                hashMap.put("SFM", "space-flight-mission");
                hashMap.put("STAT", "status");
                hashMap.put("SB", "subset");
                hashMap.put("OTO", "termowner");
                hashMap.put("OWN", FieldName.OWNER);

                //add the fields to hm
                for (Map.Entry<String, String> mapEntry : hashMap.entrySet()) {
                    String medlineKey = mapEntry.getKey();
                    String bibtexKey = mapEntry.getValue();
                    if (medlineKey.equals(label)) {
                        fields.put(bibtexKey, value);
                    }
                }

                if ("IRAD".equals(label) || "IR".equals(label) || "FIR".equals(label)) {
                    String oldInvestigator = fields.get("investigator");
                    if (oldInvestigator == null) {
                        fields.put("investigator", value);
                    } else {
                        fields.put("investigator", oldInvestigator + ", " + value);
                    }
                } else if ("MH".equals(label) || "OT".equals(label)) {
                    if (!fields.containsKey(FieldName.KEYWORDS)) {
                        fields.put(FieldName.KEYWORDS, value);
                    } else {
                        String kw = fields.get(FieldName.KEYWORDS);
                        fields.put(FieldName.KEYWORDS, kw + ", " + value);
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
            fixAuthors(fields, author, FieldName.AUTHOR);
            fixAuthors(fields, editor, FieldName.EDITOR);
            if (!comment.isEmpty()) {
                fields.put(FieldName.COMMENT, comment);
            }

            BibEntry b = new BibEntry(type);

            // Remove empty fields:
            fields.entrySet().stream().filter(n -> n.getValue().trim().isEmpty()).forEach(fields::remove);

            // create one here
            b.setField(fields);
            bibitems.add(b);
        }

        return new ParserResult(bibitems);

    }

    private boolean checkLineValidity(String line) {
        return (line.length() >= 5) && (line.charAt(4) == '-');
    }

    private String addSourceType(String value, String type) {
        String val = value.toLowerCase(Locale.ENGLISH);
        String theType = type;
        switch (val) {
        case "book":
            theType = "book";
            break;
        case "journal article":
        case "classical article":
        case "corrected and republished article":
        case "historical article":
        case "introductory journal article":
        case "newspaper article":
            theType = "article";
            break;
        case "clinical conference":
        case "consensus development conference":
        case "consensus development conference, nih":
            theType = "conference";
            break;
        case "technical report":
            theType = "techreport";
            break;
        case "editorial":
            theType = "inproceedings";
            break;
        case "overall":
            theType = "proceedings";
            break;
        default:
            break;
        }
        if ("".equals(theType)) {
            theType = "other";
        }
        return theType;
    }

    private void addStandardNumber(Map<String, String> hm, String lab, String value) {
        if ("IS".equals(lab)) {
            String key = FieldName.ISSN;
            //it is possible to have two issn, one for electronic and for print
            //if there are two then it comes at the end in brackets (electronic) or (print)
            //so search for the brackets
            if (value.indexOf('(') > 0) {
                int keyStart = value.indexOf('(');
                int keyEnd = value.indexOf(')');
                key = value.substring(keyStart + 1, keyEnd) + "-" + key;
                String numberValue = value.substring(0, keyStart - 1);
                hm.put(key, numberValue);
            } else {
                hm.put(key, value);
            }
        } else if ("ISBN".equals(lab)) {
            hm.put(FieldName.ISBN, value);
        }
    }

    private void fixAuthors(Map<String, String> hm, String author, String field) {
        if (!author.isEmpty()) {
            String fixedAuthor = AuthorList.fixAuthorLastNameFirst(author);
            hm.put(field, fixedAuthor);
        }
    }

    private void addIDs(Map<String, String> hm, String lab, String value) {
        if ("AID".equals(lab)) {
            String key = "article-id";
            String idValue = value;
            if (value.startsWith("doi:")) {
                idValue = idValue.replaceAll("(?i)doi:", "").trim();
                key = FieldName.DOI;
            } else if (value.indexOf('[') > 0) {
                int startOfIdentifier = value.indexOf('[');
                int endOfIdentifier = value.indexOf(']');
                key = "article-" + value.substring(startOfIdentifier + 1, endOfIdentifier);
                idValue = value.substring(0, startOfIdentifier - 1);
            }
            hm.put(key, idValue);

        } else if ("LID".equals(lab)) {
            hm.put("location-id", value);
        } else if ("MID".equals(lab)) {
            hm.put("manuscript-id", value);
        } else if ("JID".equals(lab)) {
            hm.put("nlm-unique-id", value);
        } else if ("OID".equals(lab)) {
            hm.put("other-id", value);
        } else if ("SI".equals(lab)) {
            hm.put("second-id", value);
        }
    }

    private void addTitles(Map<String, String> hm, String lab, String val, String type) {
        if ("TI".equals(lab)) {
            String oldVal = hm.get(FieldName.TITLE);
            if (oldVal == null) {
                hm.put(FieldName.TITLE, val);
            } else {
                if (oldVal.endsWith(":") || oldVal.endsWith(".") || oldVal.endsWith("?")) {
                    hm.put(FieldName.TITLE, oldVal + " " + val);
                } else {
                    hm.put(FieldName.TITLE, oldVal + ": " + val);
                }
            }
        } else if ("BTI".equals(lab) || "CTI".equals(lab)) {
            hm.put(FieldName.BOOKTITLE, val);
        } else if ("JT".equals(lab)) {
            if ("inproceedings".equals(type)) {
                hm.put(FieldName.BOOKTITLE, val);
            } else {
                hm.put(FieldName.JOURNAL, val);
            }
        } else if ("CTI".equals(lab)) {
            hm.put("collection-title", val);
        } else if ("TA".equals(lab)) {
            hm.put("title-abbreviation", val);
        } else if ("TT".equals(lab)) {
            hm.put("transliterated-title", val);
        } else if ("VTI".equals(lab)) {
            hm.put("volume-title", val);
        }
    }

    private void addAbstract(Map<String, String> hm, String lab, String value) {
        String abstractValue = "";
        if ("AB".equals(lab)) {
            //adds copyright information that comes at the end of an abstract
            if (value.contains("Copyright")) {
                int copyrightIndex = value.lastIndexOf("Copyright");
                //remove the copyright from the field since the name of the field is copyright
                String copyrightInfo = value.substring(copyrightIndex, value.length()).replaceAll("Copyright ", "");
                hm.put("copyright", copyrightInfo);
                abstractValue = value.substring(0, copyrightIndex);
            } else {
                abstractValue = value;
            }
            String oldAb = hm.get(FieldName.ABSTRACT);
            if (oldAb == null) {
                hm.put(FieldName.ABSTRACT, abstractValue);
            } else {
                hm.put(FieldName.ABSTRACT, oldAb + OS.NEWLINE + abstractValue);
            }
        } else if ("OAB".equals(lab) || "OABL".equals(lab)) {
            hm.put("other-abstract", value);
        }
    }

    private void addDates(Map<String, String> hm, String lab, String val) {
        if ("CRDT".equals(lab) && isCreateDateFormat(val)) {
            hm.put("create-date", val);
        } else if ("DEP".equals(lab) && isDateFormat(val)) {
            hm.put("electronic-publication", val);
        } else if ("DA".equals(lab) && isDateFormat(val)) {
            hm.put("date-created", val);
        } else if ("DCOM".equals(lab) && isDateFormat(val)) {
            hm.put("completed", val);
        } else if ("LR".equals(lab) && isDateFormat(val)) {
            hm.put("revised", val);
        } else if ("DP".equals(lab)) {
            String[] parts = val.split(" ");
            hm.put(FieldName.YEAR, parts[0]);
            if ((parts.length > 1) && !parts[1].isEmpty()) {
                hm.put(FieldName.MONTH, parts[1]);
            }
        } else if ("EDAT".equals(lab) && isCreateDateFormat(val)) {
            hm.put("publication", val);
        } else if ("MHDA".equals(lab) && isCreateDateFormat(val)) {
            hm.put("mesh-date", val);
        }
    }

    private boolean isCreateDateFormat(String value) {
        return CREATE_DATE_PATTERN.matcher(value).matches();
    }

    private boolean isDateFormat(String value) {
        return COMPLETE_DATE_PATTERN.matcher(value).matches();
    }
}
