package org.jabref.logic.msbib;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.formatter.bibtexfields.RemoveEnclosingBracesFormatter;
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
    private static final RemoveEnclosingBracesFormatter REMOVE_BRACES_FORMATTER = new RemoveEnclosingBracesFormatter();

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
                String value = entry.getFieldLatexFree(field).orElse("");
                result.fields.put(msBibField, value);
            }
        }

        // Duplicate: also added as BookTitle
        entry.getFieldLatexFree(StandardField.BOOKTITLE).ifPresent(booktitle -> result.conferenceName = booktitle);
        entry.getFieldLatexFree(StandardField.PAGES).ifPresent(pages -> result.pages = new PageNumbers(pages));
        entry.getFieldLatexFree(new UnknownField(MSBIB_PREFIX + "accessed")).ifPresent(accesed -> result.dateAccessed = accesed);

        entry.getFieldLatexFree(StandardField.URLDATE).ifPresent(acessed -> result.dateAccessed = acessed);

        // TODO: currently this can never happen
        if ("SoundRecording".equals(msBibType)) {
            result.albumTitle = entry.getFieldLatexFree(StandardField.TITLE).orElse(null);
        }

        // TODO: currently this can never happen
        if ("Interview".equals(msBibType)) {
            result.broadcastTitle = entry.getFieldLatexFree(StandardField.TITLE).orElse(null);
        }

        result.number = entry.getFieldLatexFree(StandardField.NUMBER).orElse(null);

        if (entry.getType().equals(IEEETranEntryType.Patent)) {
            result.patentNumber = entry.getFieldLatexFree(StandardField.NUMBER).orElse(null);
            result.number = null;
        }

        result.day = entry.getFieldOrAliasLatexFree(StandardField.DAY).orElse(null);
        result.month = entry.getMonth().map(Month::getFullName).orElse(null);

        if (entry.getFieldLatexFree(StandardField.YEAR).isEmpty()) {
            result.year = entry.getFieldOrAliasLatexFree(StandardField.YEAR).orElse(null);
        }
        result.journalName = entry.getFieldOrAliasLatexFree(StandardField.JOURNAL).orElse(null);

        // Value must be converted
        entry.getFieldLatexFree(StandardField.LANGUAGE)
             .ifPresent(lang -> result.fields.put("LCID", String.valueOf(MSBibMapping.getLCID(lang))));
        StringBuilder sbNumber = new StringBuilder();
        entry.getFieldLatexFree(StandardField.ISBN).ifPresent(isbn -> sbNumber.append(" ISBN: ").append(isbn));
        entry.getFieldLatexFree(StandardField.ISSN).ifPresent(issn -> sbNumber.append(" ISSN: ").append(issn));
        entry.getFieldLatexFree(new UnknownField("lccn")).ifPresent(lccn -> sbNumber.append("LCCN: ").append(lccn));
        entry.getFieldLatexFree(StandardField.MR_NUMBER).ifPresent(mrnumber -> sbNumber.append(" MRN: ").append(mrnumber));

        result.standardNumber = sbNumber.toString();
        if (result.standardNumber.isEmpty()) {
            result.standardNumber = null;
        }

        result.address = entry.getFieldOrAliasLatexFree(StandardField.ADDRESS).orElse(null);

        if (entry.getFieldLatexFree(StandardField.TYPE).isPresent()) {
            result.thesisType = entry.getFieldLatexFree(StandardField.TYPE).get();
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
        if ("InternetSite".equals(msBibType) || "DocumentFromInternetSite".equals(msBibType)) {
            result.internetSiteTitle = entry.getFieldLatexFree(StandardField.TITLE).orElse(null);
        }

        // TODO: currently only Misc can happen
        if ("Art".equals(msBibType) || "Misc".equals(msBibType)) {
            result.publicationTitle = entry.getFieldLatexFree(StandardField.TITLE).orElse(null);
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

        // Only one corporate author is supported
        // Heuristics: If the author is surrounded by curly braces, it is a corporate author
        boolean corporate = !REMOVE_BRACES_FORMATTER.format(authors).equals(authors);

        Optional<String> authorLatexFreeOpt = entry.getFieldLatexFree(field);
        if (authorLatexFreeOpt.isEmpty()) {
            return result;
        }
        String authorLatexFree = authorLatexFreeOpt.get();

        // We re-add the curly braces to keep the corporate author as is.
        // See https://github.com/JabRef/jabref-issue-melting-pot/issues/386 for details
        if (corporate) {
            authorLatexFree = "{" + authorLatexFree + "}";
        }

        return AuthorList.parse(authorLatexFree).getAuthors()
                         .stream()
                         .map(author -> new MsBibAuthor(author, corporate))
                         .toList();
    }
}
