package org.jabref.logic.openoffice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.openoffice.CitationEntryTypeMetadata.CitationEntryType;
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

import static org.jabref.logic.openoffice.CitationEntryTypeMetadata.getCitationTypeMap;

@NullMarked
public class CitationEntryTypeMetadataManager {

    static final String PROPERTY_NAME = "jabref-entrytype";

    private static final Logger LOGGER = LoggerFactory.getLogger(CitationEntryTypeMetadataManager.class);
    private static final Gson GSON = new Gson();

    private CitationEntryTypeMetadataManager() {
    }

    public static void storeZoteroEntryTypes(XTextDocument document)
            throws
            IllegalTypeException,
            NoDocumentException,
            PropertyVetoException,
            WrappedTargetException {
        Map<String, String> entryTypesInMetaData = UnoUserDefinedProperty.getStringValue(document, PROPERTY_NAME)
                                                                         .map(CitationEntryTypeMetadataManager::readEntryTypes)
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
        CitationEntryTypeMetadata metadata = existingMetadata
                .flatMap(CitationEntryTypeMetadataManager::parseMetadata)
                .orElseGet(CitationEntryTypeMetadata::new);

        Map<String, @Nullable CitationEntryType> citationTypeMap = getCitationTypeMap(metadata);
        metadata.schemaVersion = CitationEntryTypeMetadata.SCHEMA_VERSION;

        for (BibEntry entry : entries) {
            entry.getCitationKey()
                 .ifPresent(citationKey -> citationTypeMap.put(
                         citationKey,
                         new CitationEntryType(entry.getType().getName())));
        }

        return GSON.toJson(metadata);
    }

    static Map<String, String> readEntryTypes(String metadata) {
        Optional<CitationEntryTypeMetadata> parsedMetadata = parseMetadata(metadata);
        if (parsedMetadata.isEmpty()) {
            return Map.of();
        }

        CitationEntryTypeMetadata entryTypeMetadata = parsedMetadata.get();
        Map<String, @Nullable CitationEntryType> citationTypeMap = getCitationTypeMap(entryTypeMetadata);
        Map<String, String> result = new LinkedHashMap<>(citationTypeMap.size());
        for (Map.Entry<String, @Nullable CitationEntryType> entry : citationTypeMap.entrySet()) {
            Optional.ofNullable(entry.getValue())
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
                for (BibEntry entry : ZoteroCitationMarkParser.parse(referenceMarkName)) {
                    applyStoredEntryType(entry, entryTypesInMetaData);
                    entries.add(entry);
                }
            }
        }
        return entries;
    }

    private static void applyStoredEntryType(BibEntry entry, Map<String, String> entryTypesByCitationKey) {
        entry.getCitationKey()
             .map(entryTypesByCitationKey::get)
             .filter(entryType -> !StringUtil.isBlank(entryType))
             .map(EntryTypeFactory::parse)
             .ifPresent(entry::setType);
    }

    private static Optional<CitationEntryTypeMetadata> parseMetadata(String metadata) {
        if (StringUtil.isBlank(metadata)) {
            return Optional.empty();
        }

        try {
            return Optional.of(GSON.fromJson(metadata, CitationEntryTypeMetadata.class));
        } catch (JsonParseException e) {
            LOGGER.debug("Could not parse citation entry type metadata", e);
            return Optional.empty();
        }
    }
}
