package net.sf.jabref.logic.msbib;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
            String unicodeField = entry.getLatexFreeField(field).orElse("");

            if (MSBibMapping.getMSBibField(field) != null) {
                result.fields.put(MSBibMapping.getMSBibField(field), unicodeField);
            }
        }

        // Duplicate: also added as BookTitle
        entry.getField(FieldName.BOOKTITLE).ifPresent(booktitle -> result.conferenceName = booktitle);
        entry.getField(FieldName.PAGES).ifPresent(pages -> result.pages = new PageNumbers(pages));
        entry.getField(MSBIB_PREFIX + "accessed").ifPresent(accesed -> result.dateAccessed = accesed);

        // TODO: currently this can never happen
        if ("SoundRecording".equals(msbibType)) {
            result.albumTitle = entry.getField(FieldName.TITLE).orElse(null);
        }

        // TODO: currently this can never happen
        if ("Interview".equals(msbibType)) {
            result.broadcastTitle = entry.getField(FieldName.TITLE).orElse(null);
        }

        if (!entry.getField(FieldName.ISSUE).isPresent()) {
            result.number = entry.getField(FieldName.NUMBER).orElse(null);
        }

        if ("Patent".equalsIgnoreCase(entry.getType())) {
            result.patentNumber = entry.getField(FieldName.NUMBER).orElse(null);
            result.number = null;
        }

        result.journalName = entry.getFieldOrAlias(FieldName.JOURNAL).orElse(null);
        result.month = entry.getFieldOrAlias(FieldName.MONTH).orElse(null);

        if (!entry.getField(FieldName.YEAR).isPresent()) {
            result.year = entry.getFieldOrAlias(FieldName.YEAR).orElse(null);
        }

        // Value must be converted
        //Currently only english is supported
        entry.getField(FieldName.LANGUAGE)
                .ifPresent(lang -> result.fields.put("LCID", String.valueOf(MSBibMapping.getLCID(lang))));
        StringBuilder sbNumber = new StringBuilder();
        entry.getField(FieldName.ISBN).ifPresent(isbn -> sbNumber.append(" ISBN: " + isbn));
        entry.getField(FieldName.ISSN).ifPresent(issn -> sbNumber.append(" ISSN: " + issn));
        entry.getField("lccn").ifPresent(lccn -> sbNumber.append("LCCN: " + lccn));
        entry.getField("mrnumber").ifPresent(mrnumber -> sbNumber.append(" MRN: " + mrnumber));

        result.standardNumber = sbNumber.toString();
        if (result.standardNumber.isEmpty()) {
            result.standardNumber = null;
        }

        result.address = entry.getFieldOrAlias(FieldName.ADDRESS).orElse(null);

        if (entry.getField(FieldName.TYPE).isPresent()) {
            result.thesisType = entry.getField(FieldName.TYPE).get();

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
        if (("InternetSite".equals(msbibType) || "DocumentFromInternetSite".equals(msbibType))) {
            result.internetSiteTitle = entry.getField(FieldName.TITLE).orElse(null);
        }

        // TODO: currently only Misc can happen
        if ("ElectronicSource".equals(msbibType) || "Art".equals(msbibType) || "Misc".equals(msbibType)) {
            result.publicationTitle = entry.getField(FieldName.TITLE).orElse(null);
        }

        entry.getLatexFreeField(FieldName.AUTHOR).ifPresent(authors -> result.authors = getAuthors(authors));
        entry.getLatexFreeField(FieldName.EDITOR).ifPresent(editors -> result.editors = getAuthors(editors));

        return result;
    }

    private static List<PersonName> getAuthors(String authors) {
        List<PersonName> result = new ArrayList<>();

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

}
