/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jabref.importer.ImportFormatReader;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.logic.formatter.casechanger.TitleCaseFormatter;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.MonthUtil;

/**
 * Importer for the ISI Web of Science, INSPEC and Medline format.
 * <p>
 * Documentation about ISI WOS format:
 * <p>
 * <ul>
 * <li>http://wos.isitrial.com/help/helpprn.html</li>
 * </ul>
 * <p>
 * <ul>
 * <li>Check compatibility with other ISI2Bib tools like:
 * http://www-lab.imr.tohoku.ac.jp/~t-nissie/computer/software/isi/ or
 * http://www.tug.org/tex-archive/biblio/bibtex/utils/isi2bibtex/isi2bibtex or
 * http://web.mit.edu/emilio/www/utils.html</li>
 * <li>Deal with capitalization correctly</li>
 * </ul>
 */
public class IsiImporter extends ImportFormat {

    private static final Pattern SUB_SUP_PATTERN = Pattern.compile("/(sub|sup)\\s+(.*?)\\s*/");

    // 2006.09.05: Modified pattern to avoid false positives for other files due to an
    // extra | at the end:
    private static final Pattern ISI_PATTERN = Pattern.compile("FN ISI Export Format|VR 1.|PY \\d{4}");


    /**
     * Return the name of this import format.
     */
    @Override
    public String getFormatName() {
        return "ISI";
    }

    /*
     * (non-Javadoc)
     *
     * @see net.sf.jabref.imports.ImportFormat#getCLIId()
     */
    @Override
    public String getCLIId() {
        return "isi";
    }



    /**
     * Check whether the source is in the correct format for this importer.
     */
    @Override
    public boolean isRecognizedFormat(InputStream stream) throws IOException {

        try (BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream))) {

            String str;
            int i = 0;
            while (((str = in.readLine()) != null) && (i < 50)) {

                /**
                 * The following line gives false positives for RIS files, so it
                 * should not be uncommented. The hypen is a characteristic of the
                 * RIS format.
                 *
                 * str = str.replace(" - ", "")
                 */
                if (IsiImporter.ISI_PATTERN.matcher(str).find()) {
                    return true;
                }

                i++;
            }
        }
        return false;
    }



    public static void processSubSup(Map<String, String> map) {

        String[] subsup = {"title", "abstract", "review", "notes"};

        for (String aSubsup : subsup) {
            if (map.containsKey(aSubsup)) {

                Matcher m = IsiImporter.SUB_SUP_PATTERN.matcher(map.get(aSubsup));
                StringBuffer sb = new StringBuffer();

                while (m.find()) {

                    String group2 = m.group(2);
                    group2 = group2.replaceAll("\\$", "\\\\\\\\\\\\\\$"); // Escaping
                    // insanity!
                    // :-)
                    if (group2.length() > 1) {
                        group2 = "{" + group2 + "}";
                    }
                    if ("sub".equals(m.group(1))) {
                        m.appendReplacement(sb, "\\$_" + group2 + "\\$");
                    } else {
                        m.appendReplacement(sb, "\\$^" + group2 + "\\$");
                    }
                }
                m.appendTail(sb);
                map.put(aSubsup, sb.toString());
            }
        }
    }

    private static void processCapitalization(Map<String, String> map) {

        String[] subsup = {"title", "journal", "publisher"};

        for (String aSubsup : subsup) {

            if (map.containsKey(aSubsup)) {

                String s = map.get(aSubsup);
                if (s.toUpperCase().equals(s)) {
                    s = new TitleCaseFormatter().format(s);
                    map.put(aSubsup, s);
                }
            }
        }
    }

    /**
     * Parse the entries in the source, and return a List of BibEntry
     * objects.
     */
    @Override
    public List<BibEntry> importEntries(InputStream stream, OutputPrinter status) throws IOException {
        if (stream == null) {
            throw new IOException("No stream given.");
        }

        List<BibEntry> bibitems = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        try (BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream))) {
            // Pattern fieldPattern = Pattern.compile("^AU |^TI |^SO |^DT |^C1 |^AB
            // |^ID |^BP |^PY |^SE |^PY |^VL |^IS ");
            String str;

            while ((str = in.readLine()) != null) {
                if (str.length() < 3) {
                    continue;
                }

                // beginning of a new item
                if ("PT ".equals(str.substring(0, 3))) {
                    sb.append("::").append(str);
                } else {
                    String beg = str.substring(0, 3).trim();

                    // I could have used the fieldPattern regular expression instead
                    // however this seems to be
                    // quick and dirty and it works!
                    if (beg.length() == 2) {
                        sb.append(" ## "); // mark the beginning of each field
                        sb.append(str);
                    } else {
                        sb.append("EOLEOL"); // mark the end of each line
                        sb.append(str.trim()); // remove the initial spaces
                    }
                }
            }
        }

        String[] entries = sb.toString().split("::");

        Map<String, String> hm = new HashMap<>();

        // skip the first entry as it is either empty or has document header
        for (String entry : entries) {
            String[] fields = entry.split(" ## ");

            if (fields.length == 0) {
                fields = entry.split("\n");
            }

            String Type = "";
            String PT = "";
            String pages = "";
            hm.clear();

            for (String field : fields) {
                // empty field don't do anything
                if (field.length() <= 2) {
                    continue;
                }

                String beg = field.substring(0, 2);
                String value = field.substring(3);
                if (value.startsWith(" - ")) {
                    value = value.substring(3);
                }
                value = value.trim();

                if ("PT".equals(beg)) {
                    if (value.startsWith("J")) {
                        PT = "article";
                    } else {
                        PT = value;
                    }
                    Type = "article"; // make all of them PT?
                } else if ("TY".equals(beg)) {
                    if ("JOUR".equals(value)) {
                        Type = "article";
                    } else if ("CONF".equals(value)) {
                        Type = "inproceedings";
                    }
                } else if ("JO".equals(beg)) {
                    hm.put("booktitle", value);
                } else if ("AU".equals(beg)) {
                    String author = IsiImporter.isiAuthorsConvert(value.replace("EOLEOL", " and "));

                    // if there is already someone there then append with "and"
                    if (hm.get("author") != null) {
                        author = hm.get("author") + " and " + author;
                    }

                    hm.put("author", author);
                } else if ("TI".equals(beg)) {
                    hm.put("title", value.replace("EOLEOL", " "));
                } else if ("SO".equals(beg) || "JA".equals(beg)) {
                    hm.put("journal", value.replace("EOLEOL", " "));
                } else if ("ID".equals(beg) || "KW".equals(beg)) {

                    value = value.replace("EOLEOL", " ");
                    String existingKeywords = hm.get("keywords");
                    if ((existingKeywords == null) || existingKeywords.contains(value)) {
                        existingKeywords = value;
                    } else {
                        existingKeywords += ", " + value;
                    }
                    hm.put("keywords", existingKeywords);

                } else if ("AB".equals(beg)) {
                    hm.put("abstract", value.replace("EOLEOL", " "));
                } else if ("BP".equals(beg) || "BR".equals(beg) || "SP".equals(beg)) {
                    pages = value;
                } else if ("EP".equals(beg)) {
                    int detpos = value.indexOf(' ');

                    // tweak for IEEE Explore
                    if ((detpos != -1) && !value.substring(0, detpos).trim().isEmpty()) {
                        value = value.substring(0, detpos);
                    }

                    pages = pages + "--" + value;
                } else if ("PS".equals(beg)) {
                    pages = IsiImporter.parsePages(value);
                } else if ("AR".equals(beg)) {
                    pages = value;
                } else if ("IS".equals(beg)) {
                    hm.put("number", value);
                } else if ("PY".equals(beg)) {
                    hm.put("year", value);
                } else if ("VL".equals(beg)) {
                    hm.put("volume", value);
                } else if ("PU".equals(beg)) {
                    hm.put("publisher", value);
                } else if ("DI".equals(beg)) {
                    hm.put("doi", value);
                } else if ("PD".equals(beg)) {

                    String month = IsiImporter.parseMonth(value);
                    if (month != null) {
                        hm.put("month", month);
                    }

                } else if ("DT".equals(beg)) {
                    Type = value;
                    if ("Review".equals(Type)) {
                        Type = "article"; // set "Review" in Note/Comment?
                    } else if (Type.startsWith("Article") || Type.startsWith("Journal") || "article".equals(PT)) {
                        Type = "article";
                    } else {
                        Type = "misc";
                    }
                } else if ("CR".equals(beg)) {
                    hm.put("CitedReferences", value.replace("EOLEOL", " ; ").trim());
                } else {
                    // Preserve all other entries except
                    if ("ER".equals(beg) || "EF".equals(beg) || "VR".equals(beg) || "FN".equals(beg)) {
                        continue;
                    }
                    hm.put(beg.toLowerCase(), value);
                }
            }

            if (!"".equals(pages)) {
                hm.put("pages", pages);
            }

            // Skip empty entries
            if (hm.isEmpty()) {
                continue;
            }

            BibEntry b = new BibEntry(DEFAULT_BIBTEXENTRY_ID, Type);
            // id assumes an existing database so don't

            // Remove empty fields:
            List<Object> toRemove = new ArrayList<>();
            for (Map.Entry<String, String> field : hm.entrySet()) {
                String content = field.getValue();
                if ((content == null) || content.trim().isEmpty()) {
                    toRemove.add(field.getKey());
                }
            }
            for (Object aToRemove : toRemove) {
                hm.remove(aToRemove);

            }

            // Polish entries
            IsiImporter.processSubSup(hm);
            IsiImporter.processCapitalization(hm);

            b.setField(hm);

            bibitems.add(b);
        }
        return bibitems;
    }

    private static String parsePages(String value) {
        int lastDash = value.lastIndexOf('-');
        return value.substring(0, lastDash) + "--" + value.substring(lastDash + 1);
    }

    public static String parseMonth(String value) {

        String[] parts = value.split("\\s|\\-");
        for (String part1 : parts) {
            MonthUtil.Month month = MonthUtil.getMonthByShortName(part1.toLowerCase());
            if (month.isValid()) {
                return month.bibtexFormat;
            }
        }

        // Try two digit month
        for (String part : parts) {
            try {
                int number = Integer.parseInt(part);
                MonthUtil.Month month = MonthUtil.getMonthByNumber(number);
                if (month.isValid()) {
                    return month.bibtexFormat;
                }
            } catch (NumberFormatException ignored) {
                // Ignored
            }
        }
        return null;
    }

    /**
     * Will expand ISI first names.
     * <p>
     * Fixed bug from:
     * http://sourceforge.net/tracker/index.php?func=detail&aid=1542552&group_id=92314&atid=600306
     */
    public static String isiAuthorConvert(String author) {

        String[] s = author.split(",");
        if (s.length != 2) {
            return author;
        }

        StringBuilder sb = new StringBuilder();

        String last = s[0].trim();
        sb.append(last).append(", ");

        String first = s[1].trim();

        String[] firstParts = first.split("\\s+");

        for (int i = 0; i < firstParts.length; i++) {

            first = firstParts[i];

            // Do we have only uppercase chars?
            if (first.toUpperCase().equals(first)) {
                first = first.replace(".", "");
                for (int j = 0; j < first.length(); j++) {
                    sb.append(first.charAt(j)).append('.');

                    if (j < (first.length() - 1)) {
                        sb.append(' ');
                    }
                }
            } else {
                sb.append(first);
            }
            if (i < (firstParts.length - 1)) {
                sb.append(' ');
            }
        }
        return sb.toString();

    }

    private static String[] isiAuthorsConvert(String[] authors) {

        String[] result = new String[authors.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = IsiImporter.isiAuthorConvert(authors[i]);
        }
        return result;
    }

    public static String isiAuthorsConvert(String authors) {
        String[] s = IsiImporter.isiAuthorsConvert(authors.split(" and |;"));
        return String.join(" and ", s);
    }

}
