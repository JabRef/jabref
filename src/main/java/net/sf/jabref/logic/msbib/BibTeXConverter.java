package net.sf.jabref.logic.msbib;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.sf.jabref.importer.fileformat.ImportFormat;
import net.sf.jabref.logic.mods.PersonName;
import net.sf.jabref.model.entry.BibEntry;

public class BibTeXConverter {
    private static final String MSBIB_PREFIX = "msbib-";

    public static BibEntry convert(MSBibEntry entry) {
        BibEntry result;

        if (entry.getCiteKey() == null) {
            result = new BibEntry(ImportFormat.DEFAULT_BIBTEXENTRY_ID, MSBibMapping.getBibTeXEntryType(entry.getType()));
        } else {
            // TODO: the cite key should not be the ID?!
            // id assumes an existing database so don't
            result = new BibEntry(entry.getCiteKey(), MSBibMapping.getBibTeXEntryType(entry.getType()));
        }

        Map<String, String> fieldValues = new HashMap<>();

        // add String fields
        for (Map.Entry<String, String> field : entry.fields.entrySet()) {
            String msField = field.getKey();
            String value = field.getValue();

            if (value != null && MSBibMapping.getBibTeXField(msField) != null) {
                fieldValues.put(MSBibMapping.getBibTeXField(msField), value);
            }
        }

        if (entry.LCID >= 0) {
            fieldValues.put("language", entry.getLanguage(entry.LCID));
        }

        addAuthor(fieldValues, "author", entry.authors);
        addAuthor(fieldValues, MSBIB_PREFIX + "bookauthor", entry.bookAuthors);
        addAuthor(fieldValues, "editor", entry.editors);
        addAuthor(fieldValues, MSBIB_PREFIX + "translator", entry.translators);
        addAuthor(fieldValues, MSBIB_PREFIX + "producername", entry.producerNames);
        addAuthor(fieldValues, MSBIB_PREFIX + "composer", entry.composers);
        addAuthor(fieldValues, MSBIB_PREFIX + "conductor", entry.conductors);
        addAuthor(fieldValues, MSBIB_PREFIX + "performer", entry.performers);
        addAuthor(fieldValues, MSBIB_PREFIX + "writer", entry.writers);
        addAuthor(fieldValues, MSBIB_PREFIX + "director", entry.directors);
        addAuthor(fieldValues, MSBIB_PREFIX + "compiler", entry.compilers);
        addAuthor(fieldValues, MSBIB_PREFIX + "interviewer", entry.interviewers);
        addAuthor(fieldValues, MSBIB_PREFIX + "interviewee", entry.interviewees);
        addAuthor(fieldValues, MSBIB_PREFIX + "inventor", entry.inventors);
        addAuthor(fieldValues, MSBIB_PREFIX + "counsel", entry.counsels);

        if (entry.pages != null) {
            fieldValues.put("pages", entry.pages.toString("--"));
        }
        parseStandardNumber(entry.standardNumber, fieldValues);

        if (entry.address != null) {
            fieldValues.put("address", entry.address);
        }
        if (entry.conferenceName != null) {
            fieldValues.put("organization", entry.conferenceName);
        }

        if (entry.dateAccessed != null) {
            fieldValues.put(MSBIB_PREFIX + "accessed", entry.dateAccessed);
        }

        result.setField(fieldValues);
        return result;
    }

    private static void addAuthor(Map<String, String> map, String type, List<PersonName> authors) {
        if (authors == null) {
            return;
        }
        String allAuthors = authors.stream().map(PersonName::getFullname).collect(Collectors.joining(" and "));

        map.put(type, allAuthors);
    }

    private static void parseSingleStandardNumber(String type, String bibtype, String standardNum, Map<String, String> map) {
        Pattern pattern = Pattern.compile(':' + type + ":(.[^:]+)");
        Matcher matcher = pattern.matcher(standardNum);
        if (matcher.matches()) {
            map.put(bibtype, matcher.group(1));
        }
    }

    private static void parseStandardNumber(String standardNum, Map<String, String> map) {
        if (standardNum == null) {
            return;
        }
        parseSingleStandardNumber("ISBN", "isbn", standardNum, map);
        parseSingleStandardNumber("ISSN", "issn", standardNum, map);
        parseSingleStandardNumber("LCCN", "lccn", standardNum, map);
        parseSingleStandardNumber("MRN", "mrnumber", standardNum, map);
        parseSingleStandardNumber("DOI", "doi", standardNum, map);
    }
}
