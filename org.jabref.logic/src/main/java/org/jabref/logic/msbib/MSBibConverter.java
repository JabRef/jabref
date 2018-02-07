package org.jabref.logic.msbib;

import java.util.ArrayList;
import java.util.List;

import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.Month;

public class MSBibConverter {

    private static final String MSBIB_PREFIX = "msbib-";
    private static final String BIBTEX_PREFIX = "BIBTEX_";

    private MSBibConverter() {
    }

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
        entry.getLatexFreeField(FieldName.BOOKTITLE).ifPresent(booktitle -> result.conferenceName = booktitle);
        entry.getLatexFreeField(FieldName.PAGES).ifPresent(pages -> result.pages = new PageNumbers(pages));
        entry.getLatexFreeField(MSBIB_PREFIX + "accessed").ifPresent(accesed -> result.dateAccessed = accesed);

        // TODO: currently this can never happen
        if ("SoundRecording".equals(msbibType)) {
            result.albumTitle = entry.getLatexFreeField(FieldName.TITLE).orElse(null);
        }

        // TODO: currently this can never happen
        if ("Interview".equals(msbibType)) {
            result.broadcastTitle = entry.getLatexFreeField(FieldName.TITLE).orElse(null);
        }

        result.number = entry.getLatexFreeField(FieldName.NUMBER).orElse(null);

        if ("Patent".equalsIgnoreCase(entry.getType())) {
            result.patentNumber = entry.getLatexFreeField(FieldName.NUMBER).orElse(null);
            result.number = null;
        }

        result.day = entry.getFieldOrAliasLatexFree(FieldName.DAY).orElse(null);
        result.month = entry.getMonth().map(Month::getNumber).map(Object::toString).orElse(null);

        if (!entry.getLatexFreeField(FieldName.YEAR).isPresent()) {
            result.year = entry.getFieldOrAliasLatexFree(FieldName.YEAR).orElse(null);
        }
        result.journalName = entry.getFieldOrAliasLatexFree(FieldName.JOURNAL).orElse(null);

        // Value must be converted
        //Currently only english is supported
        entry.getLatexFreeField(FieldName.LANGUAGE)
                .ifPresent(lang -> result.fields.put("LCID", String.valueOf(MSBibMapping.getLCID(lang))));
        StringBuilder sbNumber = new StringBuilder();
        entry.getLatexFreeField(FieldName.ISBN).ifPresent(isbn -> sbNumber.append(" ISBN: " + isbn));
        entry.getLatexFreeField(FieldName.ISSN).ifPresent(issn -> sbNumber.append(" ISSN: " + issn));
        entry.getLatexFreeField("lccn").ifPresent(lccn -> sbNumber.append("LCCN: " + lccn));
        entry.getLatexFreeField("mrnumber").ifPresent(mrnumber -> sbNumber.append(" MRN: " + mrnumber));

        result.standardNumber = sbNumber.toString();
        if (result.standardNumber.isEmpty()) {
            result.standardNumber = null;
        }

        result.address = entry.getFieldOrAliasLatexFree(FieldName.ADDRESS).orElse(null);

        if (entry.getLatexFreeField(FieldName.TYPE).isPresent()) {
            result.thesisType = entry.getLatexFreeField(FieldName.TYPE).get();

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
            result.internetSiteTitle = entry.getLatexFreeField(FieldName.TITLE).orElse(null);
        }

        // TODO: currently only Misc can happen
        if ("ElectronicSource".equals(msbibType) || "Art".equals(msbibType) || "Misc".equals(msbibType)) {
            result.publicationTitle = entry.getLatexFreeField(FieldName.TITLE).orElse(null);
        }

        entry.getLatexFreeField(FieldName.AUTHOR).ifPresent(authors -> result.authors = getAuthors(authors));
        entry.getLatexFreeField(FieldName.EDITOR).ifPresent(editors -> result.editors = getAuthors(editors));
        entry.getLatexFreeField(FieldName.TRANSLATOR).ifPresent(translator -> result.translators = getAuthors(translator));

        return result;
    }

    private static List<MsBibAuthor> getAuthors(String authors) {
        List<MsBibAuthor> result = new ArrayList<>();
        boolean corporate = false;
        //Only one corporate authors is supported
        if (authors.startsWith("{") && authors.endsWith("}")) {
            corporate = true;
        }
        AuthorList authorList = AuthorList.parse(authors);

        for (Author author : authorList.getAuthors()) {
            result.add(new MsBibAuthor(author, corporate));
        }

        return result;
    }

}
