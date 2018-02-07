package org.jabref.logic.msbib;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.Month;

public class BibTeXConverter {

    private static final String MSBIB_PREFIX = "msbib-";

    private BibTeXConverter() {
    }

    /**
     * Converts an {@link MSBibEntry} to a {@link BibEntry} for import
     * @param entry The MsBibEntry to convert
     * @return The bib entry
     */
    public static BibEntry convert(MSBibEntry entry) {
        BibEntry result;
        Map<String, String> fieldValues = new HashMap<>();

        String bibTexEntryType = MSBibMapping.getBiblatexEntryType(entry.getType());
        result = new BibEntry(bibTexEntryType);

        // add String fields
        for (Map.Entry<String, String> field : entry.fields.entrySet()) {
            String msField = field.getKey();
            String value = field.getValue();

            if ((value != null) && (MSBibMapping.getBibTeXField(msField) != null)) {
                fieldValues.put(MSBibMapping.getBibTeXField(msField), value);
            }
        }

        // Value must be converted
        if (fieldValues.containsKey(FieldName.LANGUAGE)) {
            int lcid = Integer.valueOf(fieldValues.get(FieldName.LANGUAGE));
            fieldValues.put(FieldName.LANGUAGE, MSBibMapping.getLanguage(lcid));
        }

        addAuthor(fieldValues, FieldName.AUTHOR, entry.authors);
        addAuthor(fieldValues, FieldName.BOOKAUTHOR, entry.bookAuthors);
        addAuthor(fieldValues, FieldName.EDITOR, entry.editors);
        addAuthor(fieldValues, FieldName.TRANSLATOR, entry.translators);
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
            fieldValues.put(FieldName.PAGES, entry.pages.toString("--"));
        }

        parseStandardNumber(entry.standardNumber, fieldValues);

        if (entry.address != null) {
            fieldValues.put(FieldName.LOCATION, entry.address);
        }
        // TODO: ConferenceName is saved as booktitle when converting from MSBIB to BibTeX
        if (entry.conferenceName != null) {
            fieldValues.put(FieldName.ORGANIZATION, entry.conferenceName);
        }

        if (entry.dateAccessed != null) {
            fieldValues.put(MSBIB_PREFIX + "accessed", entry.dateAccessed);
        }

        if (entry.journalName != null) {
            fieldValues.put(FieldName.JOURNAL, entry.journalName);
        }
        if (entry.month != null) {
            Optional<Month> month = Month.parse(entry.month);
            month.ifPresent(parsedMonth ->  result.setMonth(parsedMonth));
        }
        if (entry.number != null) {
            fieldValues.put(FieldName.NUMBER, entry.number);
        }

        // set all fields
        result.setField(fieldValues);

        return result;
    }

    private static void addAuthor(Map<String, String> map, String type, List<MsBibAuthor> authors) {
        if (authors == null) {
            return;
        }
        String allAuthors = authors.stream().map(MsBibAuthor::getLastFirst).collect(Collectors.joining(" and "));

        map.put(type, allAuthors);
    }

    private static void parseSingleStandardNumber(String type, String bibtype, String standardNum,
            Map<String, String> map) {
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
        parseSingleStandardNumber("ISBN", FieldName.ISBN, standardNum, map);
        parseSingleStandardNumber("ISSN", FieldName.ISSN, standardNum, map);
        parseSingleStandardNumber("LCCN", "lccn", standardNum, map);
        parseSingleStandardNumber("MRN", "mrnumber", standardNum, map);
        parseSingleStandardNumber("DOI", FieldName.DOI, standardNum, map);
    }
}
