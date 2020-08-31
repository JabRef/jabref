package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.formatter.casechanger.TitleCaseFormatter;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

/**
 * Importer for the ISI Web of Science, INSPEC and Medline format.
 * <p>
 * Documentation about ISI WOS format:
 * <p>
 * <ul>
 * <li>https://web.archive.org/web/20131031052339/http://wos.isitrial.com/help/helpprn.html</li>
 * </ul>
 * <p>
 * <ul>
 * <li>Deal with capitalization correctly</li>
 * </ul>
 */
public class IsiImporter extends Importer {

    private static final Pattern SUB_SUP_PATTERN = Pattern.compile("/(sub|sup)\\s+(.*?)\\s*/");

    // 2006.09.05: Modified pattern to avoid false positives for other files due to an
    // extra | at the end:
    private static final Pattern ISI_PATTERN = Pattern.compile("FN ISI Export Format|VR 1.|PY \\d{4}");

    private static final String EOL = "EOLEOL";
    private static final Pattern EOL_PATTERN = Pattern.compile(EOL);

    @Override
    public String getName() {
        return "ISI";
    }

    @Override
    public StandardFileType getFileType() {
        return StandardFileType.ISI;
    }

    @Override
    public String getId() {
        return "isi";
    }

    @Override
    public String getDescription() {
        return "Importer for the ISI Web of Science, INSPEC and Medline format.";
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader reader) throws IOException {
        String str;
        int i = 0;
        while (((str = reader.readLine()) != null) && (i < 50)) {

            /*
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
        return false;
    }

    public static void processSubSup(Map<Field, String> map) {

        Field[] subsup = {StandardField.TITLE, StandardField.ABSTRACT, StandardField.REVIEW, new UnknownField("notes")};

        for (Field aSubsup : subsup) {
            if (map.containsKey(aSubsup)) {

                Matcher m = IsiImporter.SUB_SUP_PATTERN.matcher(map.get(aSubsup));
                StringBuilder sb = new StringBuilder();

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

    private static void processCapitalization(Map<Field, String> map) {

        Field[] subsup = {StandardField.TITLE, StandardField.JOURNAL, StandardField.PUBLISHER};

        for (Field aSubsup : subsup) {

            if (map.containsKey(aSubsup)) {

                String s = map.get(aSubsup);
                if (s.toUpperCase(Locale.ROOT).equals(s)) {
                    s = new TitleCaseFormatter().format(s);
                    map.put(aSubsup, s);
                }
            }
        }
    }

    @Override
    public ParserResult importDatabase(BufferedReader reader) throws IOException {
        Objects.requireNonNull(reader);

        List<BibEntry> bibEntries = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        // Pattern fieldPattern = Pattern.compile("^AU |^TI |^SO |^DT |^C1 |^AB
        // |^ID |^BP |^PY |^SE |^PY |^VL |^IS ");
        String str;

        while ((str = reader.readLine()) != null) {
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
                    sb.append(EOL); // mark the end of each line
                    sb.append(str.trim()); // remove the initial spaces
                }
            }
        }

        String[] entries = sb.toString().split("::");

        Map<Field, String> hm = new HashMap<>();

        // skip the first entry as it is either empty or has document header
        for (String entry : entries) {
            String[] fields = entry.split(" ## ");

            if (fields.length == 0) {
                fields = entry.split("\n");
            }

            EntryType type = BibEntry.DEFAULT_TYPE;
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
                    type = StandardEntryType.Article; // make all of them PT?
                } else if ("TY".equals(beg)) {
                    if ("JOUR".equals(value)) {
                        type = StandardEntryType.Article;
                    } else if ("CONF".equals(value)) {
                        type = StandardEntryType.InProceedings;
                    }
                } else if ("JO".equals(beg)) {
                    hm.put(StandardField.BOOKTITLE, value);
                } else if ("AU".equals(beg)) {
                    String author = IsiImporter.isiAuthorsConvert(EOL_PATTERN.matcher(value).replaceAll(" and "));

                    // if there is already someone there then append with "and"
                    if (hm.get(StandardField.AUTHOR) != null) {
                        author = hm.get(StandardField.AUTHOR) + " and " + author;
                    }

                    hm.put(StandardField.AUTHOR, author);
                } else if ("TI".equals(beg)) {
                    hm.put(StandardField.TITLE, EOL_PATTERN.matcher(value).replaceAll(" "));
                } else if ("SO".equals(beg) || "JA".equals(beg)) {
                    hm.put(StandardField.JOURNAL, EOL_PATTERN.matcher(value).replaceAll(" "));
                } else if ("ID".equals(beg) || "KW".equals(beg)) {
                    value = EOL_PATTERN.matcher(value).replaceAll(" ");
                    String existingKeywords = hm.get(StandardField.KEYWORDS);
                    if ((existingKeywords == null) || existingKeywords.contains(value)) {
                        existingKeywords = value;
                    } else {
                        existingKeywords += ", " + value;
                    }
                    hm.put(StandardField.KEYWORDS, existingKeywords);
                } else if ("AB".equals(beg)) {
                    hm.put(StandardField.ABSTRACT, EOL_PATTERN.matcher(value).replaceAll(" "));
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
                    hm.put(StandardField.NUMBER, value);
                } else if ("PY".equals(beg)) {
                    hm.put(StandardField.YEAR, value);
                } else if ("VL".equals(beg)) {
                    hm.put(StandardField.VOLUME, value);
                } else if ("PU".equals(beg)) {
                    hm.put(StandardField.PUBLISHER, value);
                } else if ("DI".equals(beg)) {
                    hm.put(StandardField.DOI, value);
                } else if ("PD".equals(beg)) {
                    String month = IsiImporter.parseMonth(value);
                    if (month != null) {
                        hm.put(StandardField.MONTH, month);
                    }
                } else if ("DT".equals(beg)) {
                    if ("Review".equals(value)) {
                        type = StandardEntryType.Article; // set "Review" in Note/Comment?
                    } else if (value.startsWith("Article") || value.startsWith("Journal") || "article".equals(PT)) {
                        type = StandardEntryType.Article;
                    } else {
                        type = BibEntry.DEFAULT_TYPE;
                    }
                } else if ("CR".equals(beg)) {
                    hm.put(new UnknownField("CitedReferences"), EOL_PATTERN.matcher(value).replaceAll(" ; ").trim());
                } else {
                    // Preserve all other entries except
                    if ("ER".equals(beg) || "EF".equals(beg) || "VR".equals(beg) || "FN".equals(beg)) {
                        continue;
                    }
                    hm.put(FieldFactory.parseField(beg), value);
                }
            }

            if (!"".equals(pages)) {
                hm.put(StandardField.PAGES, pages);
            }

            // Skip empty entries
            if (hm.isEmpty()) {
                continue;
            }

            BibEntry b = new BibEntry(type);
            // id assumes an existing database so don't

            // Remove empty fields:
            List<Object> toRemove = new ArrayList<>();
            for (Map.Entry<Field, String> field : hm.entrySet()) {
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

            bibEntries.add(b);
        }
        return new ParserResult(bibEntries);
    }

    private static String parsePages(String value) {
        return value.replace("-", "--");
    }

    /**
     * Parses the month and returns it in the JabRef format
     */
    static String parseMonth(String value) {
        String[] parts = value.split("\\s|\\-");
        for (String part1 : parts) {
            Optional<Month> month = Month.getMonthByShortName(part1.toLowerCase(Locale.ROOT));
            if (month.isPresent()) {
                return month.get().getJabRefFormat();
            }
        }

        // Try two digit month
        for (String part : parts) {
            try {
                int number = Integer.parseInt(part);
                Optional<Month> month = Month.getMonthByNumber(number);
                if (month.isPresent()) {
                    return month.get().getJabRefFormat();
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
     * Fixed bug from: http://sourceforge.net/tracker/index.php?func=detail&aid=1542552&group_id=92314&atid=600306
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
            if (first.toUpperCase(Locale.ROOT).equals(first)) {
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
