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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jabref.importer.ImportFormatReader;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.logic.util.strings.CaseChangers;
import net.sf.jabref.logic.util.date.MonthUtil;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import net.sf.jabref.util.Util;

/**
 * Importer for the ISI Web of Science, INSPEC and Medline format.
 *
 * Documentation about ISI WOS format:
 *
 * <ul>
 * <li>http://wos.isitrial.com/help/helpprn.html</li>
 * </ul>
 *
 * <ul>
 * <li>Check compatibility with other ISI2Bib tools like:
 * http://www-lab.imr.tohoku.ac.jp/~t-nissie/computer/software/isi/ or
 * http://www.tug.org/tex-archive/biblio/bibtex/utils/isi2bibtex/isi2bibtex or
 * http://web.mit.edu/emilio/www/utils.html</li>
 * <li>Deal with capitalization correctly</li>
 * </ul>
 */
public class IsiImporter extends ImportFormat {

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


    // 2006.09.05: Modified pattern to avoid false positives for other files due to an
    // extra | at the end:
    private static final Pattern isiPattern = Pattern.compile("FN ISI Export Format|VR 1.|PY \\d{4}");


    /**
     * Check whether the source is in the correct format for this importer.
     */
    @Override
    public boolean isRecognizedFormat(InputStream stream) throws IOException {

        BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));

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
            if (IsiImporter.isiPattern.matcher(str).find()) {
                return true;
            }

            i++;
        }

        return false;
    }


    private static final Pattern subsupPattern = Pattern.compile("/(sub|sup)\\s+(.*?)\\s*/");


    public static void processSubSup(HashMap<String, String> map) {

        String[] subsup = {"title", "abstract", "review", "notes"};

        for (String aSubsup : subsup) {
            if (map.containsKey(aSubsup)) {

                Matcher m = IsiImporter.subsupPattern.matcher(map.get(aSubsup));
                StringBuffer sb = new StringBuffer();

                while (m.find()) {

                    String group2 = m.group(2);
                    group2 = group2.replaceAll("\\$", "\\\\\\\\\\\\\\$"); // Escaping
                    // insanity!
                    // :-)
                    if (group2.length() > 1) {
                        group2 = "{" + group2 + "}";
                    }
                    if (m.group(1).equals("sub")) {
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

    private static void processCapitalization(HashMap<String, String> map) {

        String[] subsup = {"title", "journal", "publisher"};

        for (String aSubsup : subsup) {

            if (map.containsKey(aSubsup)) {

                String s = map.get(aSubsup);
                if (s.toUpperCase().equals(s)) {
                    s = CaseChangers.TITLE.changeCase(s);
                    map.put(aSubsup, s);
                }
            }
        }
    }

    /**
     * Parse the entries in the source, and return a List of BibtexEntry
     * objects.
     */
    @Override
    public List<BibtexEntry> importEntries(InputStream stream, OutputPrinter status) throws IOException {
        if (stream == null) {
            throw new IOException("No stream given.");
        }

        ArrayList<BibtexEntry> bibitems = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        BufferedReader in = new BufferedReader(ImportFormatReader.getReaderDefaultEncoding(stream));

        // Pattern fieldPattern = Pattern.compile("^AU |^TI |^SO |^DT |^C1 |^AB
        // |^ID |^BP |^PY |^SE |^PY |^VL |^IS ");
        String str;

        while ((str = in.readLine()) != null) {
            if (str.length() < 3) {
                continue;
            }

            // begining of a new item
            if (str.substring(0, 3).equals("PT ")) {
                sb.append("::").append(str);
            } else {
                String beg = str.substring(0, 3).trim();

                // I could have used the fieldPattern regular expression instead
                // however this seems to be
                // quick and dirty and it works!
                if (beg.length() == 2) {
                    sb.append(" ## "); // mark the begining of each field
                    sb.append(str);
                } else {
                    sb.append("EOLEOL"); // mark the end of each line
                    sb.append(str.trim()); // remove the initial spaces
                }
            }
        }

        String[] entries = sb.toString().split("::");

        HashMap<String, String> hm = new HashMap<>();

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

                if (beg.equals("PT")) {
                    if (value.startsWith("J")) {
                        PT = "article";
                    } else {
                        PT = value;
                    }
                    Type = "article"; // make all of them PT?
                } else if (beg.equals("TY")) {
                    if ("JOUR".equals(value)) {
                        Type = "article";
                    } else if ("CONF".equals(value)) {
                        Type = "inproceedings";
                    }
                } else if (beg.equals("JO")) {
                    hm.put("booktitle", value);
                } else if (beg.equals("AU")) {
                    String author = IsiImporter.isiAuthorsConvert(value.replaceAll("EOLEOL", " and "));

                    // if there is already someone there then append with "and"
                    if (hm.get("author") != null) {
                        author = hm.get("author") + " and " + author;
                    }

                    hm.put("author", author);
                } else if (beg.equals("TI")) {
                    hm.put("title", value.replaceAll("EOLEOL", " "));
                } else if (beg.equals("SO") || beg.equals("JA")) {
                    hm.put("journal", value.replaceAll("EOLEOL", " "));
                } else if (beg.equals("ID") || beg.equals("KW")) {

                    value = value.replaceAll("EOLEOL", " ");
                    String existingKeywords = hm.get("keywords");
                    if ((existingKeywords != null) && !existingKeywords.contains(value)) {
                        existingKeywords += ", " + value;
                    } else {
                        existingKeywords = value;
                    }
                    hm.put("keywords", existingKeywords);

                } else if (beg.equals("AB")) {
                    hm.put("abstract", value.replaceAll("EOLEOL", " "));
                } else if (beg.equals("BP") || beg.equals("BR") || beg.equals("SP")) {
                    pages = value;
                } else if (beg.equals("EP")) {
                    int detpos = value.indexOf(' ');

                    // tweak for IEEE Explore
                    if ((detpos != -1) && !value.substring(0, detpos).trim().isEmpty()) {
                        value = value.substring(0, detpos);
                    }

                    pages = pages + "--" + value;
                } else if (beg.equals("PS")) {
                    pages = IsiImporter.parsePages(value);
                } else if (beg.equals("AR")) {
                    pages = value;
                } else if (beg.equals("IS")) {
                    hm.put("number", value);
                } else if (beg.equals("PY")) {
                    hm.put("year", value);
                } else if (beg.equals("VL")) {
                    hm.put("volume", value);
                } else if (beg.equals("PU")) {
                    hm.put("publisher", value);
                } else if (beg.equals("DI")) {
                    hm.put("doi", value);
                } else if (beg.equals("PD")) {

                    String month = IsiImporter.parseMonth(value);
                    if (month != null) {
                        hm.put("month", month);
                    }

                } else if (beg.equals("DT")) {
                    Type = value;
                    if (Type.equals("Review")) {
                        Type = "article"; // set "Review" in Note/Comment?
                    } else if (Type.startsWith("Article") || Type.startsWith("Journal")
                            || PT.equals("article")) {
                        Type = "article";
                    } else {
                        Type = "misc";
                    }
                } else if (beg.equals("CR")) {
                    hm.put("CitedReferences", value.replaceAll("EOLEOL", " ; ").trim());
                } else {
                    // Preserve all other entries except
                    if (beg.equals("ER") || beg.equals("EF") || beg.equals("VR")
                            || beg.equals("FN")) {
                        continue;
                    }
                    hm.put(beg, value);
                }
            }

            if (!"".equals(pages)) {
                hm.put("pages", pages);
            }

            // Skip empty entries
            if (hm.isEmpty()) {
                continue;
            }

            BibtexEntry b = new BibtexEntry(DEFAULT_BIBTEXENTRY_ID, BibtexEntryTypes
                    .getEntryType(Type));
            // id assumes an existing database so don't

            // Remove empty fields:
            ArrayList<Object> toRemove = new ArrayList<>();
            for (String key : hm.keySet()) {
                String content = hm.get(key);
                if ((content == null) || content.trim().isEmpty()) {
                    toRemove.add(key);
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
        int lastDash = value.lastIndexOf("-");
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
                int number = Util.intValueOf(part);
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
     *
     * Fixed bug from:
     * http://sourceforge.net/tracker/index.php?func=detail&aid=1542552&group_id=92314&atid=600306
     *
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
                first = first.replaceAll("\\.", "");
                for (int j = 0; j < first.length(); j++) {
                    sb.append(first.charAt(j)).append(".");

                    if (j < (first.length() - 1)) {
                        sb.append(" ");
                    }
                }
            } else {
                sb.append(first);
            }
            if (i < (firstParts.length - 1)) {
                sb.append(" ");
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
        return StringUtil.join(s, " and ");
    }

}
