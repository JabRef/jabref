package org.jabref.logic.openoffice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.model.openoffice.uno.NoDocumentException;
import org.jabref.model.openoffice.uno.UnoReferenceMark;
import org.jabref.model.openoffice.uno.UnoUserDefinedProperty;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.sun.star.beans.IllegalTypeException;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextDocument;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class CitationEntryTypeMetadata {

    static final String PROPERTY_NAME = "jabref-entrytype";

    private static final Logger LOGGER = LoggerFactory.getLogger(CitationEntryTypeMetadata.class);
    private static final Gson GSON = new Gson();
    private static final int SCHEMA_VERSION = 1;

    private CitationEntryTypeMetadata() {
    }

    public static void storeZoteroEntryTypes(XTextDocument document)
            throws
            IllegalTypeException,
            NoDocumentException,
            PropertyVetoException,
            WrappedTargetException {
        Map<String, String> entryTypesInMetaData = UnoUserDefinedProperty.getStringValue(document, PROPERTY_NAME)
                                                                         .map(CitationEntryTypeMetadata::readEntryTypes)
                                                                         .orElseGet(Map::of);
        List<BibEntry> entries = parseZoteroEntries(UnoReferenceMark.getListOfNames(document), entryTypesInMetaData);
        storeEntryTypes(document, entries);
    }

    public static void storeEntryTypes(XTextDocument document, List<BibEntry> entries)
            throws
            IllegalTypeException,
            PropertyVetoException,
            WrappedTargetException {
        if (entries.isEmpty()) {
            return;
        }

        Optional<String> existingMetadata = UnoUserDefinedProperty.getStringValue(document, PROPERTY_NAME);
        String metadata = mergeEntryTypes(existingMetadata, entries);
        UnoUserDefinedProperty.setStringProperty(document, PROPERTY_NAME, metadata);
    }

    static String mergeEntryTypes(Optional<String> existingMetadata, List<BibEntry> entries) {
        EntryTypeMetadata metadata = existingMetadata
                .flatMap(CitationEntryTypeMetadata::parseMetadata)
                .orElseGet(EntryTypeMetadata::new);

        Map<String, CitationEntryType> citationTypeMap = getCitationTypeMap(metadata);
        metadata.schemaVersion = SCHEMA_VERSION;

        for (BibEntry entry : entries) {
            entry.getCitationKey()
                 .ifPresent(citationKey -> citationTypeMap.put(
                         citationKey,
                         new CitationEntryType(entry.getType().getName())));
        }

        return GSON.toJson(metadata);
    }

    static Map<String, String> readEntryTypes(String metadata) {
        Optional<EntryTypeMetadata> parsedMetadata = parseMetadata(metadata);
        if (parsedMetadata.isEmpty()) {
            return Map.of();
        }

        EntryTypeMetadata entryTypeMetadata = parsedMetadata.get();
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, CitationEntryType> entry : getCitationTypeMap(entryTypeMetadata).entrySet()) {
            Optional.of(entry.getValue())
                    .map(citationEntryType -> citationEntryType.jabrefEntryType)
                    .filter(value -> !StringUtil.isBlank(value))
                    .ifPresent(value -> result.put(entry.getKey(), value));
        }
        return result;
    }

    static List<BibEntry> parseZoteroEntries(List<String> referenceMarkNames, Map<String, String> entryTypesInMetaData) {
        List<BibEntry> entries = new ArrayList<>();
        for (String referenceMarkName : referenceMarkNames) {
            if (ReferenceMark.isZoteroReferenceMarkName(referenceMarkName)) {
                entries.addAll(parseZoteroEntries(referenceMarkName, entryTypesInMetaData));
            }
        }
        return entries;
    }

    private static List<BibEntry> parseZoteroEntries(String referenceMarkName, Map<String, String> entryTypesInMetaData) {
        Optional<List<String>> citationKeys = readZoteroCitationKeys(referenceMarkName);
        if (citationKeys.isEmpty()) {
            return ZoteroCitationMarkParser.parse(referenceMarkName);
        }

        List<BibEntry> entries = new ArrayList<>();
        List<String> missingCitationKeys = new ArrayList<>();
        for (String citationKey : citationKeys.get()) {
            Optional<String> entryType = Optional.ofNullable(entryTypesInMetaData.get(citationKey))
                                                 .filter(value -> !StringUtil.isBlank(value));
            entryType.ifPresentOrElse(
                    value -> entries.add(new BibEntry(EntryTypeFactory.parse(value)).withCitationKey(citationKey)),
                    () -> missingCitationKeys.add(citationKey));
        }

        // All citation keys are in metadata
        if (missingCitationKeys.isEmpty()) {
            return entries;
        }

        // None of the citation keys are in metadata
        if (entries.isEmpty()) {
            return ZoteroCitationMarkParser.parse(referenceMarkName);
        }

        // Part of the citation keys are in metadata
        for (BibEntry entry : ZoteroCitationMarkParser.parse(referenceMarkName)) {
            entry.getCitationKey()
                 .filter(missingCitationKeys::contains)
                 .ifPresent(_ -> entries.add(entry));
        }

        return entries;
    }

    private static Optional<List<String>> readZoteroCitationKeys(String referenceMarkName) {
        Optional<String> cslJson = extractCSLJSON(referenceMarkName);
        if (cslJson.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(GSON.fromJson(cslJson.orElseThrow(), ZoteroCitationData.class))
                           .map(citationData -> Optional.ofNullable(citationData.citationItems)
                                                        .orElseGet(List::of)
                                                        .stream()
                                                        .map(citationItem -> "Zotero-" + citationItem.id)
                                                        .toList());
        } catch (JsonParseException e) {
            LOGGER.debug("Could not parse Zotero citation entry type metadata", e);
            return Optional.empty();
        }
    }

    private static Optional<String> extractCSLJSON(String referenceMarkName) {
        int jsonStart = referenceMarkName.indexOf('{');
        int jsonEnd = referenceMarkName.lastIndexOf('}');
        if ((jsonStart < 0) || (jsonEnd < jsonStart)) {
            return Optional.empty();
        }
        return Optional.of(referenceMarkName.substring(jsonStart, jsonEnd + 1));
    }

    private static Optional<EntryTypeMetadata> parseMetadata(String metadata) {
        if (StringUtil.isBlank(metadata)) {
            return Optional.empty();
        }

        try {
            return Optional.of(GSON.fromJson(metadata, EntryTypeMetadata.class));
        } catch (JsonParseException e) {
            LOGGER.debug("Could not parse citation entry type metadata", e);
            return Optional.empty();
        }
    }

    private static Map<String, CitationEntryType> getCitationTypeMap(EntryTypeMetadata metadata) {
        Map<String, CitationEntryType> citationTypeMap = Optional.ofNullable(metadata.citationTypeMap)
                                                                 .orElseGet(LinkedHashMap::new);
        metadata.citationTypeMap = citationTypeMap;
        return citationTypeMap;
    }

    private static class EntryTypeMetadata {
        int schemaVersion = SCHEMA_VERSION;
        @Nullable Map<String, CitationEntryType> citationTypeMap = new LinkedHashMap<>();
    }

    private static class CitationEntryType {
        @Nullable String jabrefEntryType;

        CitationEntryType(String jabrefEntryType) {
            this.jabrefEntryType = jabrefEntryType;
        }
    }
}
