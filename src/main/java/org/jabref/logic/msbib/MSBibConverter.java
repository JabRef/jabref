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
import org.jabref.model.entry.types.IEEETranEntryType;
import org.jabref.model.entry.types.StandardEntryType;

public class MSBibConverter {

    private static final String MSBIB_PREFIX = "msbib-";
    private static final String BIBTEX_PREFIX = "BIBTEX_";

    private MSBibConverter() {
    }

    public static MSBibEntry convert(BibEntry entry) {
        MSBibEntry result = new MSBibEntry();

        // memorize original type
        result.fields.put(BIBTEX_PREFIX + "Entry", entry.getType().getName());
        // define new type
        String msBibType = MSBibMapping.getMSBibEntryType(entry.getType()).name();
        result.fields.put("SourceType", msBibType);

        for (Field field : entry.getFields()) {
            String msBibField = MSBibMapping.getMSBibField(field);
            if (msBibField != null) {
                String value = entry.getLatexFreeField(field).orElse("");
                result.fields.put(msBibField, value);
            }
        }

        // Duplicate: also added as BookTitle
        entry.getLatexFreeField(StandardField.BOOKTITLE).ifPresent(booktitle -> result.conferenceName = booktitle);
        entry.getLatexFreeField(StandardField.PAGES).ifPresent(pages -> result.pages = new PageNumbers(pages));
        entry.getLatexFreeField(new UnknownField(MSBIB_PREFIX + "accessed")).ifPresent(accesed -> result.dateAccessed = accesed);

        entry.getLatexFreeField(StandardField.URLDATE).ifPresent(acessed -> result.dateAccessed = acessed);

        // TODO: currently this can never happen
        if ("SoundRecording".equals(msBibType)) {
            result.albumTitle = entry.getLatexFreeField(StandardField.TITLE).orElse(null);
        }

        // TODO: currently this can never happen
        if ("Interview".equals(msBibType)) {
            result.broadcastTitle = entry.getLatexFreeField(StandardField.TITLE).orElse(null);
        }

        result.number = entry.getLatexFreeField(StandardField.NUMBER).orElse(null);

        if (entry.getType().equals(IEEETranEntryType.Patent)) {
            result.patentNumber = entry.getLatexFreeField(StandardField.NUMBER).orElse(null);
            result.number = null;
        }

        result.day = entry.getFieldOrAliasLatexFree(StandardField.DAY).orElse(null);
        result.month = entry.getMonth().map(Month::getFullName).orElse(null);

        if (!entry.getLatexFreeField(StandardField.YEAR).isPresent()) {
            result.year = entry.getFieldOrAliasLatexFree(StandardField.YEAR).orElse(null);
        }
        result.journalName = entry.getFieldOrAliasLatexFree(StandardField.JOURNAL).orElse(null);

        // Value must be converted
        // Currently only english is supported
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
            if (entry.getType().equals(StandardEntryType.TechReport)) {
                result.thesisType = "Tech. rep.";
            } else if (entry.getType().equals(StandardEntryType.MastersThesis)) {
                result.thesisType = "Master's thesis";
            } else if (entry.getType().equals(StandardEntryType.PhdThesis)) {
                result.thesisType = "Ph.D. dissertation";
            } else if (entry.getType().equals(StandardEntryType.Unpublished)) {
                result.thesisType = "unpublished";
            }
        }

        // TODO: currently this can never happen
        if (("InternetSite".equals(msBibType) || "DocumentFromInternetSite".equals(msBibType))) {
            result.internetSiteTitle = entry.getLatexFreeField(StandardField.TITLE).orElse(null);
        }

        // TODO: currently only Misc can happen
        if ("ElectronicSource".equals(msBibType) || "Art".equals(msBibType) || "Misc".equals(msBibType)) {
            result.publicationTitle = entry.getLatexFreeField(StandardField.TITLE).orElse(null);
        }

        if (entry.getType().equals(IEEETranEntryType.Patent)) {
            entry.getField(StandardField.AUTHOR).ifPresent(authors -> result.inventors = getAuthors(entry, authors, StandardField.AUTHOR));
        } else {
            entry.getField(StandardField.AUTHOR).ifPresent(authors -> result.authors = getAuthors(entry, authors, StandardField.AUTHOR));
        }
        entry.getField(StandardField.EDITOR).ifPresent(editors -> result.editors = getAuthors(entry, editors, StandardField.EDITOR));
        entry.getField(StandardField.TRANSLATOR).ifPresent(translator -> result.translators = getAuthors(entry, translator, StandardField.EDITOR));

        return result;
    }

    private static List<MsBibAuthor> getAuthors(BibEntry entry, String authors, Field field) {
        List<MsBibAuthor> result = new ArrayList<>();
        boolean corporate = false;
        // Only one corporate author is supported
        // We have the possible rare case that are multiple authors which start and end with latex , this is currently not considered
        if (authors.startsWith("{") && authors.endsWith("}")) {
            corporate = true;
        }
        // FIXME: #4152 This is an ugly hack because the latex2unicode formatter kills of all curly braces, so no more corporate author parsing possible
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
