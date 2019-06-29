package org.jabref.logic.msbib;

import java.util.ArrayList;
import java.util.List;

import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;

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

        for (Field field : entry.getFieldNames()) {
            // clean field
            String unicodeField = entry.getLatexFreeField(field).orElse("");

            if (MSBibMapping.getMSBibField(field) != null) {
                result.fields.put(MSBibMapping.getMSBibField(field), unicodeField);
            }
        }

        // Duplicate: also added as BookTitle
        entry.getLatexFreeField(StandardField.BOOKTITLE).ifPresent(booktitle -> result.conferenceName = booktitle);
        entry.getLatexFreeField(StandardField.PAGES).ifPresent(pages -> result.pages = new PageNumbers(pages));
        entry.getLatexFreeField(new UnknownField(MSBIB_PREFIX + "accessed")).ifPresent(accesed -> result.dateAccessed = accesed);

        // TODO: currently this can never happen
        if ("SoundRecording".equals(msbibType)) {
            result.albumTitle = entry.getLatexFreeField(StandardField.TITLE).orElse(null);
        }

        // TODO: currently this can never happen
        if ("Interview".equals(msbibType)) {
            result.broadcastTitle = entry.getLatexFreeField(StandardField.TITLE).orElse(null);
        }

        result.number = entry.getLatexFreeField(StandardField.NUMBER).orElse(null);

        if ("Patent".equalsIgnoreCase(entry.getType())) {
            result.patentNumber = entry.getLatexFreeField(StandardField.NUMBER).orElse(null);
            result.number = null;
        }

        result.day = entry.getFieldOrAliasLatexFree(StandardField.DAY).orElse(null);
        result.month = entry.getMonth().map(Month::getNumber).map(Object::toString).orElse(null);

        if (!entry.getLatexFreeField(StandardField.YEAR).isPresent()) {
            result.year = entry.getFieldOrAliasLatexFree(StandardField.YEAR).orElse(null);
        }
        result.journalName = entry.getFieldOrAliasLatexFree(StandardField.JOURNAL).orElse(null);

        // Value must be converted
        //Currently only english is supported
        entry.getLatexFreeField(StandardField.LANGUAGE)
             .ifPresent(lang -> result.fields.put("LCID", String.valueOf(MSBibMapping.getLCID(lang))));
        StringBuilder sbNumber = new StringBuilder();
        entry.getLatexFreeField(StandardField.ISBN).ifPresent(isbn -> sbNumber.append(" ISBN: " + isbn));
        entry.getLatexFreeField(StandardField.ISSN).ifPresent(issn -> sbNumber.append(" ISSN: " + issn));
        entry.getLatexFreeField(new UnknownField("lccn")).ifPresent(lccn -> sbNumber.append("LCCN: " + lccn));
        entry.getLatexFreeField(StandardField.MR_NUMBER).ifPresent(mrnumber -> sbNumber.append(" MRN: " + mrnumber));

        result.standardNumber = sbNumber.toString();
        if (result.standardNumber.isEmpty()) {
            result.standardNumber = null;
        }

        result.address = entry.getFieldOrAliasLatexFree(StandardField.ADDRESS).orElse(null);

        if (entry.getLatexFreeField(StandardField.TYPE).isPresent()) {
            result.thesisType = entry.getLatexFreeField(StandardField.TYPE).get();

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
            result.internetSiteTitle = entry.getLatexFreeField(StandardField.TITLE).orElse(null);
        }

        // TODO: currently only Misc can happen
        if ("ElectronicSource".equals(msbibType) || "Art".equals(msbibType) || "Misc".equals(msbibType)) {
            result.publicationTitle = entry.getLatexFreeField(StandardField.TITLE).orElse(null);
        }

        entry.getField(StandardField.AUTHOR).ifPresent(authors -> result.authors = getAuthors(entry, authors, StandardField.AUTHOR));
        entry.getField(StandardField.EDITOR).ifPresent(editors -> result.editors = getAuthors(entry, editors, StandardField.EDITOR));
        entry.getField(StandardField.TRANSLATOR).ifPresent(translator -> result.translators = getAuthors(entry, translator, StandardField.EDITOR));

        return result;
    }

    private static List<MsBibAuthor> getAuthors(BibEntry entry, String authors, Field field) {
        List<MsBibAuthor> result = new ArrayList<>();
        boolean corporate = false;
        //Only one corporate author is supported
        //We have the possible rare case that are multiple authors which start and end with latex , this is currently not considered
        if (authors.startsWith("{") && authors.endsWith("}")) {
            corporate = true;
        }
        //FIXME: #4152 This is an ugly hack because the latex2unicode formatter kills of all curly braces, so no more corporate author parsing possible
        String authorLatexFree = entry.getLatexFreeField(field).orElse("");
        if (corporate) {
            authorLatexFree = "{" + authorLatexFree + "}";
        }

        AuthorList authorList = AuthorList.parse(authorLatexFree);

        for (Author author : authorList.getAuthors()) {
            result.add(new MsBibAuthor(author, corporate));
        }

        return result;
    }

}
