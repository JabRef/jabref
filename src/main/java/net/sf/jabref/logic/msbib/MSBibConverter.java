package net.sf.jabref.logic.msbib;

import java.util.ArrayList;
import java.util.List;

import net.sf.jabref.logic.layout.format.LatexToUnicodeFormatter;
import net.sf.jabref.logic.layout.format.RemoveBrackets;
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
            String unicodeField = removeLaTeX(entry.getField(field));

            if (MSBibMapping.getMSBibField(field) != null) {
                result.fields.put(MSBibMapping.getMSBibField(field), unicodeField);
            }
        }

        // Duplicate: also added as BookTitle
        if (entry.hasField("booktitle")) {
            result.conferenceName = entry.getField("booktitle");
        }

        if (entry.hasField("pages")) {
            result.pages = new PageNumbers(entry.getField("pages"));
        }

        if (entry.hasField(MSBIB_PREFIX + "accessed")) {
            result.dateAccessed = entry.getField(MSBIB_PREFIX + "accessed");
        }

        // TODO: currently this can never happen
        if ("SoundRecording".equals(msbibType) && (entry.hasField("title"))) {
            result.albumTitle = entry.getField("title");
        }

        // TODO: currently this can never happen
        if ("Interview".equals(msbibType) && (entry.hasField("title"))) {
            result.broadcastTitle = entry.getField("title");
        }

        // Value must be converted
        if (entry.hasField("language")) {
            result.fields.put("LCID", String.valueOf(MSBibMapping.getLCID(entry.getField("language"))));
        }

        result.standardNumber = "";
        if (entry.hasField(FieldName.ISBN_FIELD)) {
            result.standardNumber += " ISBN: " + entry.getField(FieldName.ISBN_FIELD);
        }
        if (entry.hasField(FieldName.ISSN_FIELD)) {
            result.standardNumber += " ISSN: " + entry.getField(FieldName.ISSN_FIELD);
        }
        if (entry.hasField("lccn")) {
            result.standardNumber += " LCCN: " + entry.getField("lccn");
        }
        if (entry.hasField("mrnumber")) {
            result.standardNumber += " MRN: " + entry.getField("mrnumber");
        }
        if (entry.hasField(FieldName.DOI_FIELD)) {
            result.standardNumber += " DOI: " + entry.getField(FieldName.DOI_FIELD);
        }
        if (result.standardNumber.isEmpty()) {
            result.standardNumber = null;
        }

        if (entry.hasField("address")) {
            result.address = entry.getField("address");
        }

        if (entry.hasField("type")) {
            result.thesisType = entry.getField("type");
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
                && (entry.hasField("title"))) {
            result.internetSiteTitle = entry.getField("title");
        }

        // TODO: currently only Misc can happen
        if (("ElectronicSource".equals(msbibType) || "Art".equals(msbibType) || "Misc".equals(msbibType))
                && (entry.hasField("title"))) {
            result.publicationTitle = entry.getField("title");
        }

        if (entry.hasField(FieldName.AUTHOR_FIELD)) {
            result.authors = getAuthors(entry.getField(FieldName.AUTHOR_FIELD));
        }
        if (entry.hasField(FieldName.EDITOR_FIELD)) {
            result.editors = getAuthors(entry.getField(FieldName.EDITOR_FIELD));
        }

        return result;
    }

    private static List<PersonName> getAuthors(String authors) {
        List<PersonName> result = new ArrayList<>();

        // TODO: case-insensitive?!
        if (authors.contains(" and ")) {
            String[] names = authors.split(" and ");
            for (String name : names) {
                result.add(new PersonName(name));
            }
        } else {
            result.add(new PersonName(authors));
        }
        return result;
    }

    private static String removeLaTeX(String text) {
        // TODO: just use latex free version everywhere in the future
        String result = new RemoveBrackets().format(text);
        result = new LatexToUnicodeFormatter().format(result);

        return result;
    }
}
