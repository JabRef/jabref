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
import org.jabref.model.entry.types.EntryType;

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
        } catch (JsonParseException | NumberFormatException | NoSuchElementException e) {
            LOGGER.debug("Could not parse Zotero citation mark {}", referenceMarkName, e);
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
    /// Returns an empty list when the input is blank or not parseable CSL JSON.
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
        return toBibEntry(citationItem.itemData)
                .map(entry -> entry.withCitationKey("Zotero-" + citationItem.id));
    }

    private static Optional<BibEntry> toBibEntry(ZoteroCitationData.ItemData itemData) {
        EntryType entryType = CSLItemTypeDefinitions.getEntryType(itemData.type);
        BibEntry entry = new BibEntry(entryType);
        setAuthors(itemData.author).ifPresent(authors -> entry.withField(StandardField.AUTHOR, authors));
        setDate(entry, itemData.issued);
        for (Map.Entry<String, Field> fieldMapping : CSLItemTypeDefinitions.getFieldMappings(itemData.type, itemData).entrySet()) {
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
            Author author = new Author(authorData.given, "", "", authorData.family, "");
            authors.add(author);
        }

        return Optional.of(AuthorList.of(authors).getAsLastFirstNamesWithAnd(false));
    }

    private static void setDate(BibEntry entry, ZoteroCitationData.IssuedData issuedData) {
        if (issuedData.dateParts.isEmpty()) {
            return;
        }

        List<String> dateParts = issuedData.dateParts.getFirst();
        if ((dateParts == null) || dateParts.isEmpty()) {
            return;
        }

        String dateString = String.join("-", dateParts);
        Date.parse(dateString).ifPresent(entry::withDate);
    }

    private static void setField(BibEntry entry, Field field, String value) {
        if (StringUtil.isBlank(value)) {
            return;
        }

        entry.withField(field, value);
    }
}
