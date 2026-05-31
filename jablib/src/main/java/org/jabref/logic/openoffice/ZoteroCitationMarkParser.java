package org.jabref.logic.openoffice;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Date;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZoteroCitationMarkParser {
    private static final Gson GSON = new Gson();
    private static final String CSL_JOURNAL_ARTICLE = "article-journal";

    private static final Logger LOGGER = LoggerFactory.getLogger(ZoteroCitationMarkParser.class);

    private ZoteroCitationMarkParser() {
    }

    public static List<BibEntry> parse(String referenceMarkName) {
        if (!ReferenceMark.isZoteroReferenceMarkName(referenceMarkName)) {
            return List.of();
        }

        Optional<String> cslJSON = extractCSLJSON(referenceMarkName);
        if (cslJSON.isEmpty()) {
            return List.of();
        }

        try {
            ZoteroCitationData citationData = GSON.fromJson(cslJSON.get(), ZoteroCitationData.class);
            List<BibEntry> entries = new ArrayList<>();
            for (ZoteroCitationData.CitationItemData citationItem : citationData.citationItems) {
                toBibEntry(citationItem).ifPresent(entries::add);
            }

            return entries;
        } catch (JsonParseException e) {
            LOGGER.debug("Could not parse Zotero citation mark {}", referenceMarkName, e);
            return List.of();
        }
    }

    private static Optional<String> extractCSLJSON(String referenceMarkName) {
        int jsonStart = referenceMarkName.indexOf('{');
        int jsonEnd = referenceMarkName.lastIndexOf('}');
        if ((jsonStart < 0) || (jsonEnd < jsonStart)) {
            LOGGER.debug("Could not find CSL citation JSON in Zotero mark {}", referenceMarkName);
            return Optional.empty();
        }
        return Optional.of(referenceMarkName.substring(jsonStart, jsonEnd + 1));
    }

    private static Optional<BibEntry> toBibEntry(ZoteroCitationData.CitationItemData citationItem) {
        ZoteroCitationData.ItemData itemData = citationItem.itemData;

        // TODO: Support more entry types
        // These are temporary lines, to only allow "journal articles"
        if (!CSL_JOURNAL_ARTICLE.equals(itemData.type)) {
            return Optional.empty();
        }

        // TODO: Add more fields
        // For now, "Useful" fields (can be seen in preview) are kept
        BibEntry entry = new BibEntry(StandardEntryType.Article);
        entry.withCitationKey("Zotero-" + (citationItem.id));
        setAuthors(itemData.author).ifPresent(authors -> entry.withField(StandardField.AUTHOR, authors));
        setDate(entry, itemData.issued);
        setField(entry, StandardField.TITLE, itemData.title);
        setField(entry, StandardField.JOURNALTITLE, itemData.containerTitle);
        setField(entry, StandardField.JOURNAL, itemData.containerTitle);
        setField(entry, StandardField.VOLUME, itemData.volume);
        setField(entry, StandardField.NUMBER, itemData.issue);
        setField(entry, StandardField.PAGES, itemData.page);
        setField(entry, StandardField.DOI, itemData.doi);
        setField(entry, StandardField.URL, itemData.url);

        return Optional.of(entry);
    }

    private static Optional<String> setAuthors(List<ZoteroCitationData.AuthorData> authorsList) {
        if (authorsList.isEmpty()) {
            return Optional.empty();
        }

        List<Author> authors = new ArrayList<>();
        for (ZoteroCitationData.AuthorData authorData : authorsList) {
            Author author = new Author(authorData.given, "", "", authorData.family, "");
            authors.add(author);
        }

        return Optional.of(AuthorList.of(authors).getAsLastFirstNamesWithAnd(false));
    }

    ///  Zotero supports 6 types of date format:
    ///  y-m-d, y-d, y-m, m-d, m, d
    ///  If the number is less or equal than 12, Zotero thinks it is month, otherwise day.
    private static void setDate(BibEntry entry, ZoteroCitationData.IssuedData issuedData) {
        if (issuedData.dateParts.isEmpty()) {
            return;
        }

        List<String> dateParts = issuedData.dateParts.getFirst();
        if ((dateParts == null) || dateParts.isEmpty()) {
            return;
        }

        String firstDatePart = dateParts.getFirst();
        if (StringUtil.isBlank(firstDatePart)) {
            return;
        }

        if (firstDatePart.length() == 4) {
            setDateWithYear(entry, firstDatePart, dateParts);
        } else {
            setDateWithoutYear(entry, dateParts);
        }
    }

    private static void setDateWithYear(BibEntry entry, String year, List<String> dateParts) {
        if (dateParts.size() == 1) {
            Date.parse(Optional.of(year), Optional.empty(), Optional.empty()).ifPresent(entry::withDate);
            return;
        }

        if (dateParts.size() == 2) {
            String secondDatePart = dateParts.get(1);
            int secondNumber;
            try {
                secondNumber = Integer.parseInt(secondDatePart);
            } catch (NumberFormatException e) {
                LOGGER.debug("Could not parse Zotero date part {}", secondDatePart, e);
                return;
            }

            if (secondNumber <= 12) {
                // y-m
                Date.parse(Optional.of(year), Optional.of(secondDatePart), Optional.empty()).ifPresent(entry::withDate);
            } else {
                // TODO: Support y-d format for Date.parse()
                // y-d
                entry.withField(StandardField.YEAR, year);
                entry.withField(StandardField.DAY, secondDatePart);
            }
            return;
        }

        // y-m-d
        Date.parse(Optional.of(year), Optional.of(dateParts.get(1)), Optional.of(dateParts.get(2))).ifPresent(entry::withDate);
    }

    private static void setDateWithoutYear(BibEntry entry, List<String> dateParts) {
        String firstDatePart = dateParts.getFirst();
        int firstNumber;
        try {
            firstNumber = Integer.parseInt(firstDatePart);
        } catch (NumberFormatException e) {
            LOGGER.debug("Could not parse Zotero date part {}", firstDatePart, e);
            return;
        }

        if (dateParts.size() == 1) {
            if (firstNumber <= 12) {
                // month
                entry.withField(StandardField.MONTH, firstDatePart);
            } else {
                // day
                entry.withField(StandardField.DAY, firstDatePart);
            }
            return;
        }

        // m-d
        entry.withField(StandardField.MONTH, firstDatePart);
        entry.withField(StandardField.DAY, dateParts.get(1));
    }

    private static void setField(BibEntry entry, StandardField field, String value) {
        if (StringUtil.isBlank(value)) {
            return;
        }

        entry.withField(field, value);
    }
}
