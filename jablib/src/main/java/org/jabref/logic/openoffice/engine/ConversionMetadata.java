package org.jabref.logic.openoffice.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.openoffice.ZoteroReferenceMark;
import org.jabref.logic.openoffice.engine.storage.UndoableDocumentProperties;
import org.jabref.logic.openoffice.uno.UnoUserDefinedProperty;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.Citation;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationGroupId;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextDocument;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Lossless conversion state, keyed by canonical uid and indexed by Zotero identity on reads.
/// Native JSON is preserved opaquely, including its own display properties, never used as a render cache.
/// An external edit invalidates the snapshot: refusing conversion is safer than restoring stale state.
@NullMarked
record ConversionMetadata(String canonicalName, List<String> pageInfos, String zoteroName) {
    static final String PREFIX = "JabRef_citation_conversion:";
    private static final Logger LOGGER = LoggerFactory.getLogger(ConversionMetadata.class);

    ConversionMetadata {
        pageInfos = List.copyOf(pageInfos);
    }

    static ConversionMetadata capture(CitationGroup group, String zoteroName) throws JabRefMarkCodecException {
        return new ConversionMetadata(JabRefMarkCodec.encode(new JabRefMarkData(
                group.citationsInStorageOrder.stream().map(citation -> citation.citationKey).toList(),
                group.groupId.citationGroupIdAsString(), group.citationType)),
                group.citationsInStorageOrder.stream().map(citation -> citation.getPageInfo().map(text -> text.toString()).orElse("")).toList(), zoteroName);
    }

    CitationGroup group() throws JabRefMarkCodecException {
        JabRefMarkData data = JabRefMarkCodec.decode(canonicalName);
        List<Citation> citations = new ArrayList<>();
        for (int index = 0; index < data.citationKeys().size(); index++) {
            Citation citation = new Citation(data.citationKeys().get(index));
            String page = pageInfos.get(index);
            if (!page.isEmpty()) {
                citation.setPageInfo(Optional.of(OOText.fromString(page)));
            }
            citations.add(citation);
        }
        return new CitationGroup(new CitationGroupId(data.uniqueId()), data.citationType(), citations, Optional.of(canonicalName));
    }

    String encode() {
        JsonObject json = new JsonObject();
        json.addProperty("version", 1);
        json.addProperty("canonicalName", canonicalName);
        JsonArray pages = new JsonArray();
        pageInfos.forEach(pages::add);
        json.add("pageInfos", pages);
        json.addProperty("zoteroName", zoteroName);
        return json.toString();
    }

    static ConversionMetadata decode(String value) throws RenderException {
        try {
            JsonObject json = JsonParser.parseString(value).getAsJsonObject();
            if (!json.has("version") || json.get("version").getAsInt() != 1
                    || !json.has("canonicalName") || !json.has("pageInfos") || !json.has("zoteroName")
                    || !json.get("canonicalName").isJsonPrimitive() || !json.getAsJsonPrimitive("canonicalName").isString()
                    || !json.get("zoteroName").isJsonPrimitive() || !json.getAsJsonPrimitive("zoteroName").isString()
                    || !json.get("pageInfos").isJsonArray()) {
                throw CitationFormatConverter.invalidMetadata();
            }
            for (JsonElement page : json.getAsJsonArray("pageInfos")) {
                if (!page.isJsonPrimitive() || !page.getAsJsonPrimitive().isString()) {
                    throw CitationFormatConverter.invalidMetadata();
                }
            }
            ConversionMetadata metadata = new ConversionMetadata(json.get("canonicalName").getAsString(),
                    json.getAsJsonArray("pageInfos").asList().stream().map(element -> element.getAsString()).toList(),
                    json.get("zoteroName").getAsString());
            JabRefMarkData canonical = JabRefMarkCodec.decode(metadata.canonicalName());
            ZoteroReferenceMark nativeMark = ZoteroReferenceMark.parse(metadata.zoteroName()).orElseThrow(CitationFormatConverter::invalidMetadata);
            if (canonical.citationKeys().size() != metadata.pageInfos().size() || !canonical.citationKeys().equals(nativeMark.citationKeys())) {
                throw CitationFormatConverter.invalidMetadata();
            }
            identity(metadata.zoteroName());
            return metadata;
        } catch (JsonParseException | IllegalStateException | UnsupportedOperationException | ClassCastException | NumberFormatException | JabRefMarkCodecException exception) {
            throw CitationFormatConverter.invalidMetadata(exception);
        }
    }

    static Optional<ConversionMetadata> forCanonical(XTextDocument document, CitationGroup group) throws WrappedTargetException, RenderException {
        Optional<String> value = UnoUserDefinedProperty.getStringValue(document, PREFIX + group.groupId.citationGroupIdAsString());
        if (value.isEmpty()) {
            return Optional.empty();
        }
        ConversionMetadata metadata = decode(value.orElseThrow());
        try {
            JabRefMarkData stored = JabRefMarkCodec.decode(metadata.canonicalName());
            if (!stored.uniqueId().equals(group.groupId.citationGroupIdAsString()) || !stored.citationKeys().equals(
                    group.citationsInStorageOrder.stream().map(citation -> citation.citationKey).toList())) {
                throw CitationFormatConverter.invalidMetadata();
            }
        } catch (JabRefMarkCodecException exception) {
            throw CitationFormatConverter.invalidMetadata(exception);
        }
        return Optional.of(metadata);
    }

    static Optional<ConversionMetadata> forZotero(XTextDocument document, String name) throws WrappedTargetException, RenderException {
        return readIndex(document).forZotero(name);
    }

    static String identity(String name) throws RenderException {
        JsonObject json = JsonParser.parseString(ZoteroReferenceMark.getCSLJson(name)).getAsJsonObject();
        // The JSON citationID survives Zotero name updates; never use BibEntry's transient id.
        if (json.has("citationID") && json.get("citationID").isJsonPrimitive() && json.getAsJsonPrimitive("citationID").isString()
                && !json.get("citationID").getAsString().isBlank()) {
            return json.get("citationID").getAsString();
        }
        String suffix = ZoteroReferenceMark.parse(name).orElseThrow(CitationFormatConverter::invalidMetadata).uniqueId();
        if (suffix.isBlank()) {
            throw CitationFormatConverter.invalidMetadata();
        }
        return suffix;
    }

    /// Native edits do not require conversion state. Only an exact, unambiguous snapshot may follow
    /// our planned URI rename; stale or corrupt state must remain untouched for strict conversion reads.
    static Optional<ConversionMetadata> prepareRelink(XTextDocument document, String before, String after) throws com.sun.star.uno.Exception {
        return readIndex(document).prepareRelink(document, before, after);
    }

    /// Read once per action, before looking up native citations. Invalid records are retained as a
    /// strict-read failure, but do not prevent tolerant URI-link planning for other exact snapshots.
    static Index readIndex(XTextDocument document) throws WrappedTargetException {
        Index index = new Index();
        for (String property : UnoUserDefinedProperty.getListOfNames(document)) {
            if (!property.startsWith(PREFIX)) {
                continue;
            }
            Optional<String> value = UnoUserDefinedProperty.getStringValue(document, property);
            try {
                ConversionMetadata metadata = decode(value.orElseThrow(CitationFormatConverter::invalidMetadata));
                if (!property.equals(PREFIX + JabRefMarkCodec.decode(metadata.canonicalName()).uniqueId())) {
                    throw CitationFormatConverter.invalidMetadata();
                }
                String citationIdentity = identity(metadata.zoteroName());
                String suffix = ZoteroReferenceMark.parse(metadata.zoteroName()).orElseThrow().uniqueId();
                index.byIdentity.computeIfAbsent(citationIdentity, _ -> new ArrayList<>()).add(metadata);
                if (!suffix.isBlank()) {
                    index.bySuffix.computeIfAbsent(suffix, _ -> new ArrayList<>()).add(metadata);
                }
                index.byExactName.computeIfAbsent(metadata.zoteroName(), _ -> new ArrayList<>()).add(metadata);
            } catch (RenderException | JabRefMarkCodecException exception) {
                index.invalid = Optional.of(CitationFormatConverter.invalidMetadata(exception));
                LOGGER.debug("Invalid conversion metadata excluded from native URI-link planning: {}", property, exception);
            }
        }
        return index;
    }

    @NullMarked
    static final class Index {
        private final Map<String, List<ConversionMetadata>> byIdentity = new HashMap<>();
        private final Map<String, List<ConversionMetadata>> bySuffix = new HashMap<>();
        private final Map<String, List<ConversionMetadata>> byExactName = new HashMap<>();
        private Optional<RenderException> invalid = Optional.empty();

        private Index() {
        }

        Optional<ConversionMetadata> forZotero(String name) throws RenderException {
            if (invalid.isPresent()) {
                throw invalid.orElseThrow();
            }
            String suffix = ZoteroReferenceMark.parse(name).orElseThrow(CitationFormatConverter::invalidMetadata).uniqueId();
            Set<ConversionMetadata> matches = new HashSet<>(byIdentity.getOrDefault(identity(name), List.of()));
            if (!suffix.isBlank()) {
                matches.addAll(bySuffix.getOrDefault(suffix, List.of()));
            }
            if (matches.isEmpty()) {
                return Optional.empty();
            }
            if (matches.size() != 1) {
                throw CitationFormatConverter.invalidMetadata();
            }
            ConversionMetadata metadata = matches.iterator().next();
            if (!metadata.zoteroName().equals(name)) {
                throw CitationFormatConverter.invalidMetadata();
            }
            return Optional.of(metadata);
        }

        Optional<ConversionMetadata> prepareRelink(XTextDocument document, String before, String after) throws com.sun.star.uno.Exception {
            List<ConversionMetadata> matches = byExactName.getOrDefault(before, List.of());
            if (matches.size() != 1) {
                return Optional.empty();
            }
            ConversionMetadata metadata = matches.getFirst();
            ConversionMetadata updated = new ConversionMetadata(metadata.canonicalName(), metadata.pageInfos(), after);
            String encoded = updated.encode();
            String property;
            try {
                decode(encoded);
                property = PREFIX + JabRefMarkCodec.decode(metadata.canonicalName()).uniqueId();
            } catch (RenderException | JabRefMarkCodecException exception) {
                LOGGER.debug("Native URI linking cannot preserve the stored conversion key layout", exception);
                return Optional.empty();
            }
            UndoableDocumentProperties.preflight(document, Map.of(property, Optional.of(encoded)));
            return Optional.of(updated);
        }
    }

    void store(XTextDocument document) throws com.sun.star.uno.Exception, JabRefMarkCodecException {
        UndoableDocumentProperties.set(document, PREFIX + JabRefMarkCodec.decode(canonicalName).uniqueId(), Optional.of(encode()));
    }
}
