package org.jabref.logic.openoffice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Date;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZoteroCitationMarkParser {
    private static final Gson GSON = new Gson();

    private static final Logger LOGGER = LoggerFactory.getLogger(ZoteroCitationMarkParser.class);

    private ZoteroCitationMarkParser() {
    }

    public static List<BibEntry> parseCslCitationJson(String cslJson) {
        if (StringUtil.isBlank(cslJson)) {
            return List.of();
        }

        try {
            ZoteroCitationData citationData = GSON.fromJson(cslJson, ZoteroCitationData.class);
            List<BibEntry> entries = new ArrayList<>();
            for (ZoteroCitationData.CitationItemData citationItem : Optional.ofNullable(citationData.citationItems).orElse(List.of())) {
                toBibEntry(citationItem).ifPresent(entries::add);
            }

            return entries;
        } catch (JsonParseException | NumberFormatException | NoSuchElementException e) {
            LOGGER.debug("Could not parse Zotero CSL citation JSON", e);
            return List.of();
        }
    }

    /// Parses a bare CSL-JSON payload (a single CSL item object, or an array of them) into
    /// [BibEntry] instances, reusing the citation-js-based CSL item-type/field mapping (ADR 0064).
    ///
    /// Unlike [#parse(String)] this expects the CSL items directly (as produced by a Zotero /
    /// citation-js CSL-JSON export), not a Zotero reference-mark wrapper. The produced entries have
    /// no citation key, so callers importing them let JabRef generate keys from its pattern.
    ///
    /// @return the parsed list of entries or an empty list when the input is blank or not parsable CSL JSON.
    public static List<BibEntry> parseCslJsonItems(String cslJson) {
        if (StringUtil.isBlank(cslJson)) {
            return List.of();
        }

        try {
            JsonElement root = JsonParser.parseString(cslJson);
            List<ZoteroCitationData.ItemData> items = new ArrayList<>();
            // The isJsonArray/isJsonObject guards ensure Gson returns a non-null result here.
            if (root.isJsonArray()) {
                Collections.addAll(items, GSON.fromJson(root, ZoteroCitationData.ItemData[].class));
            } else if (root.isJsonObject()) {
                items.add(GSON.fromJson(root, ZoteroCitationData.ItemData.class));
            }

            List<BibEntry> entries = new ArrayList<>();
            for (ZoteroCitationData.ItemData itemData : items) {
                toBibEntry(itemData).ifPresent(entries::add);
            }
            return entries;
        } catch (JsonParseException | NumberFormatException | NoSuchElementException e) {
            LOGGER.debug("Could not parse CSL JSON items", e);
            return List.of();
        }
    }

    public static Optional<BibEntry> toBibEntry(ZoteroCitationData.CitationItemData citationItem) {
        return toBibEntry(citationItem.itemData)
                .map(entry -> ZoteroReferenceMark.getCitationKey(citationItem)
                                                 .map(entry::withCitationKey)
                                                 .orElse(entry));
    }

    private static Optional<BibEntry> toBibEntry(ZoteroCitationData.ItemData itemData) {
        // Gson replaces the field defaults with null when the JSON sets these keys explicitly to
        // null (e.g. "type": null, "author": null, "issued": null), so normalise before use. The
        // CSL mapping tables are immutable maps, which reject a null key lookup with an NPE.
        String type = itemData.type == null ? "" : itemData.type;
        BibEntry entry = new BibEntry(CSLItemTypeDefinitions.getEntryType(type));
        List<ZoteroCitationData.AuthorData> authors = itemData.author == null ? List.of() : itemData.author;
        setAuthors(authors).ifPresent(value -> entry.withField(StandardField.AUTHOR, value));
        if (itemData.issued != null) {
            setDate(entry, itemData.issued);
        }
        for (Map.Entry<String, Field> fieldMapping : CSLItemTypeDefinitions.getFieldMappings(type, itemData).entrySet()) {
            setField(entry, fieldMapping.getValue(), itemData.getFieldValue(fieldMapping.getKey()));
        }

        return Optional.of(entry);
    }

    private static Optional<String> setAuthors(List<ZoteroCitationData.AuthorData> authorsList) {
        if (authorsList.isEmpty()) {
            return Optional.empty();
        }

        List<Author> authors = new ArrayList<>();
        for (ZoteroCitationData.AuthorData authorData : authorsList) {
            String given = Optional.ofNullable(authorData.given).orElse("");
            String family = Optional.ofNullable(authorData.family).orElse("");
            Author author = new Author(given, "", "", family, "");
            authors.add(author);
        }

        return Optional.of(AuthorList.of(authors).getAsLastFirstNamesWithAnd(false));
    }

    private static void setDate(BibEntry entry, ZoteroCitationData.IssuedData issuedData) {
        if ((issuedData.dateParts == null) || issuedData.dateParts.isEmpty()) {
            String rawDate = issuedData.raw;
            if (rawDate != null && !StringUtil.isBlank(rawDate)) {
                Date.parse(rawDate).ifPresent(entry::withDate);
            }
            return;
        }

        List<Object> dateParts = issuedData.dateParts.getFirst();
        if ((dateParts == null) || dateParts.isEmpty()) {
            String rawDate = issuedData.raw;
            if (rawDate != null && !StringUtil.isBlank(rawDate)) {
                Date.parse(rawDate).ifPresent(entry::withDate);
            }
            return;
        }

        List<String> datePartStrings = new ArrayList<>();
        for (Object datePart : dateParts) {
            if (datePart instanceof Number number) {
                datePartStrings.add(Integer.toString(number.intValue()));
            } else {
                datePartStrings.add(datePart.toString());
            }
        }

        String dateString = String.join("-", datePartStrings);
        Date.parse(dateString).ifPresent(entry::withDate);
    }

    private static void setField(BibEntry entry, Field field, String value) {
        if (StringUtil.isBlank(value)) {
            return;
        }

        entry.withField(field, value);
    }
}
