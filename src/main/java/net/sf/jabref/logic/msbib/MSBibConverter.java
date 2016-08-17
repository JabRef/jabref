package net.sf.jabref.logic.msbib;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.sf.jabref.logic.layout.format.LatexToUnicodeFormatter;
import net.sf.jabref.logic.mods.PageNumbers;
import net.sf.jabref.logic.mods.PersonName;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

public class MSBibConverter {

    private static final String MSBIB_PREFIX = "msbib-";
    private static final String BIBTEX_PREFIX = "BIBTEX_";


    public static MSBibEntry convert(BibEntry entry) {
        MSBibEntry result = new MSBibEntry();

        // memorize original type
        result.fields.put(BIBTEX_PREFIX + "Entry", entry.getType());
        // define new type
        String msbibType = result.fields.put("SourceType", MSBibMapping.getMSBibEntryType(entry.getType()).name());

        for (String field : entry.getFieldNames()) {
            // clean field
            String unicodeField = removeLaTeX(entry.getFieldOptional(field).orElse(""));

            if (MSBibMapping.getMSBibField(field) != null) {
                result.fields.put(MSBibMapping.getMSBibField(field), unicodeField);
            }
        }

        // Duplicate: also added as BookTitle
        entry.getFieldOptional(FieldName.BOOKTITLE).ifPresent(booktitle -> result.conferenceName = booktitle);
        entry.getFieldOptional(FieldName.PAGES).ifPresent(pages -> result.pages = new PageNumbers(pages));
        entry.getFieldOptional(MSBIB_PREFIX + "accessed").ifPresent(accesed -> result.dateAccessed = accesed);

        // TODO: currently this can never happen
        if ("SoundRecording".equals(msbibType)) {
            result.albumTitle = entry.getFieldOptional(FieldName.TITLE).orElse(null);
        }

        // TODO: currently this can never happen
        if ("Interview".equals(msbibType)) {
            result.broadcastTitle = entry.getFieldOptional(FieldName.TITLE).orElse(null);
        }

        if ("Patent".equalsIgnoreCase(entry.getType())) {
            result.patentNumber = entry.getFieldOptional(FieldName.NUMBER).orElse(null);
        }

        result.journalName = entry.getFieldOrAlias(FieldName.JOURNAL).orElse(null);
        result.month = entry.getFieldOrAlias(FieldName.MONTH).orElse(null);

        if (!entry.getFieldOptional(FieldName.YEAR).isPresent()) {
            result.year = entry.getFieldOrAlias(FieldName.YEAR).orElse(null);
        }

        if (!entry.getFieldOptional(FieldName.ISSUE).isPresent()) {
            result.number = entry.getFieldOptional(FieldName.NUMBER).orElse(null);
        }
        // Value must be converted
        //Currently only english is supported
        entry.getFieldOptional(FieldName.LANGUAGE)
                .ifPresent(lang -> result.fields.put("LCID", String.valueOf(MSBibMapping.getLCID(lang))));
        result.standardNumber = "";
        entry.getFieldOptional(FieldName.ISBN).ifPresent(isbn -> result.standardNumber += " ISBN: " + isbn);
        entry.getFieldOptional(FieldName.ISSN).ifPresent(issn -> result.standardNumber += " ISSN: " + issn);
        entry.getFieldOptional("lccn").ifPresent(lccn -> result.standardNumber += " LCCN: " + lccn);
        entry.getFieldOptional("mrnumber").ifPresent(mrnumber -> result.standardNumber += " MRN: " + mrnumber);

        if (result.standardNumber.isEmpty()) {
            result.standardNumber = null;
        }

        result.address = entry.getFieldOrAlias(FieldName.ADDRESS).orElse(null);

        if (entry.getFieldOptional(FieldName.TYPE).isPresent()) {
            result.thesisType = entry.getFieldOptional(FieldName.TYPE).get();

        } else {
            if ("techreport".equalsIgnoreCase(entry.getType())) {
                result.thesisType = "Tech. rep.";
            } else if ("mastersthesis".equalsIgnoreCase(entry.getType())) {
                result.thesisType = "Master's thesis";
            } else if ("phdthesis".equalsIgnoreCase(entry.getType())) {
                result.thesisType = "Ph.D. dissertation";
            } else if ("unpublished".equalsIgnoreCase(entry.getType())) {
                result.thesisType = "unpublished";
            }
        }

        // TODO: currently this can never happen
        if (("InternetSite".equals(msbibType) || "DocumentFromInternetSite".equals(msbibType))
                && (entry.hasField(FieldName.TITLE))) {
            result.internetSiteTitle = entry.getField(FieldName.TITLE);
        }

        // TODO: currently only Misc can happen
        if ("ElectronicSource".equals(msbibType) || "Art".equals(msbibType) || "Misc".equals(msbibType)) {
            result.publicationTitle = entry.getFieldOptional(FieldName.TITLE).orElse(null);
        }

        entry.getFieldOptional(FieldName.AUTHOR).ifPresent(authors -> result.authors = getAuthors(authors));
        entry.getFieldOptional(FieldName.EDITOR).ifPresent(editors -> result.editors = getAuthors(editors));

        return result;
    }

    private static List<PersonName> getAuthors(String authors) {
        List<PersonName> result = new ArrayList<>();

        authors = removeLaTeX(authors);

        if (authors.toUpperCase(Locale.ENGLISH).contains(" AND ")) {
            String[] names = authors.split(" (?i)and ");
            for (String name : names) {
                result.add(new PersonName(name));
            }
        } else {
            result.add(new PersonName(authors));
        }
        return result;
    }

    private static String removeLaTeX(String text) {
        return new LatexToUnicodeFormatter().format(text);
    }
}
