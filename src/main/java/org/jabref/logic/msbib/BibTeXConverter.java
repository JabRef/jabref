package org.jabref.logic.msbib;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.EntryType;

public class BibTeXConverter {

    private static final String MSBIB_PREFIX = "msbib-";

    private BibTeXConverter() {
    }

    /**
     * Converts an {@link MSBibEntry} to a {@link BibEntry} for import
     *
     * @param entry The MsBibEntry to convert
     * @return The bib entry
     */
    public static BibEntry convert(MSBibEntry entry) {
        BibEntry result;
        Map<Field, String> fieldValues = new HashMap<>();

        EntryType bibTexEntryType = MSBibMapping.getBiblatexEntryType(entry.getType());
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
        if (fieldValues.containsKey(StandardField.LANGUAGE)) {
            int lcid = Integer.valueOf(fieldValues.get(StandardField.LANGUAGE));
            fieldValues.put(StandardField.LANGUAGE, MSBibMapping.getLanguage(lcid));
        }

        addAuthor(fieldValues, StandardField.AUTHOR, entry.authors);
        addAuthor(fieldValues, StandardField.BOOKAUTHOR, entry.bookAuthors);
        addAuthor(fieldValues, StandardField.EDITOR, entry.editors);
        addAuthor(fieldValues, StandardField.TRANSLATOR, entry.translators);
        addAuthor(fieldValues, new UnknownField(MSBIB_PREFIX + "producername"), entry.producerNames);
        addAuthor(fieldValues, new UnknownField(MSBIB_PREFIX + "composer"), entry.composers);
        addAuthor(fieldValues, new UnknownField(MSBIB_PREFIX + "conductor"), entry.conductors);
        addAuthor(fieldValues, new UnknownField(MSBIB_PREFIX + "performer"), entry.performers);
        addAuthor(fieldValues, new UnknownField(MSBIB_PREFIX + "writer"), entry.writers);
        addAuthor(fieldValues, new UnknownField(MSBIB_PREFIX + "director"), entry.directors);
        addAuthor(fieldValues, new UnknownField(MSBIB_PREFIX + "compiler"), entry.compilers);
        addAuthor(fieldValues, new UnknownField(MSBIB_PREFIX + "interviewer"), entry.interviewers);
        addAuthor(fieldValues, new UnknownField(MSBIB_PREFIX + "interviewee"), entry.interviewees);
        addAuthor(fieldValues, new UnknownField(MSBIB_PREFIX + "inventor"), entry.inventors);
        addAuthor(fieldValues, new UnknownField(MSBIB_PREFIX + "counsel"), entry.counsels);

        if (entry.pages != null) {
            fieldValues.put(StandardField.PAGES, entry.pages.toString("--"));
        }

        parseStandardNumber(entry.standardNumber, fieldValues);

        if (entry.address != null) {
            fieldValues.put(StandardField.LOCATION, entry.address);
        }
        // TODO: ConferenceName is saved as booktitle when converting from MSBIB to BibTeX
        if (entry.conferenceName != null) {
            fieldValues.put(StandardField.ORGANIZATION, entry.conferenceName);
        }

        if (entry.dateAccessed != null) {
            fieldValues.put(new UnknownField(MSBIB_PREFIX + "accessed"), entry.dateAccessed);
        }

        if (entry.journalName != null) {
            fieldValues.put(StandardField.JOURNAL, entry.journalName);
        }
        if (entry.month != null) {
            Optional<Month> month = Month.parse(entry.month);
            month.ifPresent(result::setMonth);
        }
        if (entry.number != null) {
            fieldValues.put(StandardField.NUMBER, entry.number);
        }

        // set all fields
        result.setField(fieldValues);

        return result;
    }

    private static void addAuthor(Map<Field, String> map, Field field, List<MsBibAuthor> authors) {
        if (authors == null) {
            return;
        }
        String allAuthors = authors.stream().map(MsBibAuthor::getLastFirst).collect(Collectors.joining(" and "));

        map.put(field, allAuthors);
    }

    private static void parseSingleStandardNumber(String type, Field field, String standardNum, Map<Field, String> map) {
        Pattern pattern = Pattern.compile(':' + type + ":(.[^:]+)");
        Matcher matcher = pattern.matcher(standardNum);
        if (matcher.matches()) {
            map.put(field, matcher.group(1));
        }
    }

    private static void parseStandardNumber(String standardNum, Map<Field, String> map) {
        if (standardNum == null) {
            return;
        }
        parseSingleStandardNumber("ISBN", StandardField.ISBN, standardNum, map);
        parseSingleStandardNumber("ISSN", StandardField.ISSN, standardNum, map);
        parseSingleStandardNumber("LCCN", new UnknownField("lccn"), standardNum, map);
        parseSingleStandardNumber("MRN", StandardField.MR_NUMBER, standardNum, map);
        parseSingleStandardNumber("DOI", StandardField.DOI, standardNum, map);
    }
}
