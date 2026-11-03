package org.jabref.logic.openoffice.engine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.CitationIds;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.ZoteroCitationLinker.LinkPlan;
import org.jabref.logic.openoffice.ZoteroDocumentPreferences;
import org.jabref.logic.openoffice.ZoteroReferenceMark;
import org.jabref.logic.openoffice.backend.ReferenceMarkTextWriter;
import org.jabref.logic.openoffice.backend.WriterReferenceMark;
import org.jabref.logic.openoffice.backend.WriterReferenceMark.BoundaryPolicy;
import org.jabref.logic.openoffice.backend.WriterReferenceMarks;
import org.jabref.logic.openoffice.engine.storage.DocumentStateStore;
import org.jabref.logic.openoffice.engine.storage.MarkStore;
import org.jabref.logic.openoffice.engine.storage.MarkWriter;
import org.jabref.logic.openoffice.engine.storage.UndoableDocumentProperties;
import org.jabref.logic.openoffice.frontend.RangeForOverlapCheck;
import org.jabref.logic.openoffice.rangesort.FunctionalTextViewCursor;
import org.jabref.logic.openoffice.rangesort.RangeSort;
import org.jabref.logic.openoffice.rangesort.RangeSortEntry;
import org.jabref.logic.openoffice.rangesort.RangeSortVisual;
import org.jabref.logic.openoffice.rangesort.RangeSortable;
import org.jabref.logic.openoffice.uno.CreationException;
import org.jabref.logic.openoffice.uno.NoDocumentException;
import org.jabref.logic.openoffice.uno.OOTextWriter;
import org.jabref.logic.openoffice.uno.UnoTextRange;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.Citation;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationType;
import org.jabref.model.openoffice.util.OOResult;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.XComponentContext;
import org.jspecify.annotations.NullMarked;

/// A next-insertion transaction, not a mode listener or a refresh migration. Prepare reads both
/// supported formats, resolves all keys and renders the complete result once, without changing UNO
/// state. Apply only consumes that plan, inside the engine's existing document session.
@NullMarked
final class CitationFormatConverter {
    private final XTextDocument document;
    private final XComponentContext context;
    private final OpenOfficePreferences preferences;
    private final BibEntryTypesManager entryTypes;
    private final ZoteroCitationStrategy zotero;
    private final WriterReferenceMarks ranges = new WriterReferenceMarks();

    CitationFormatConverter(XTextDocument document, XComponentContext context, OpenOfficePreferences preferences,
                            BibEntryTypesManager entryTypes, ZoteroCitationStrategy zotero) {
        this.document = document;
        this.context = context;
        this.preferences = preferences;
        this.entryTypes = entryTypes;
        this.zotero = zotero;
    }

    static boolean required(List<String> names, boolean targetZotero) {
        return names.stream().anyMatch(targetZotero ? MarkCodec::isCanonicalMarkName : ZoteroReferenceMark::isZoteroReferenceMarkName);
    }

    PreparedInsertion prepare(List<BibEntry> entries, List<BibDatabase> databases, CitationStyle style,
                              CitationType requestedType, String pageInfo, XTextCursor cursor, boolean synchronizeBibliography)
            throws NoDocumentException, com.sun.star.uno.Exception, RenderException, MarkCodecException, IOException,
            ZoteroCitationStrategy.StrategyException {
        boolean targetZotero = preferences.getZoteroCompatibilityMode();
        boolean rebuildBibliography = synchronizeBibliography && style.hasBibliography();
        if (targetZotero) {
            ZoteroCitationStrategy.requireType(requestedType);
            if (!pageInfo.isEmpty()) {
                throw new ConversionException("Cannot add page information in Zotero mode", Localization.lang("Problem modifying citation"));
            }
        }
        if (entries.isEmpty()) {
            throw new ConversionException("No entries to insert", Localization.lang("No entries selected for citation"));
        }
        List<String> keys = new ArrayList<>();
        for (BibEntry entry : entries) {
            keys.add(entry.getCitationKey().filter(key -> !key.isBlank()).orElseThrow(() -> new ConversionException(
                    "A selected entry has no stable citation key", Localization.lang("This operation requires all selected entries to have citation keys defined."))));
        }
        List<BibDatabase> selected = new ArrayList<>(databases);
        selected.add(new BibDatabase(entries.stream().map(BibEntry::new).toList()));
        ReadPlan source = read(selected);
        List<PlannedCitation> citations = source.citations();
        zotero.validateRanges(citations.stream().map(citation -> new RangeForOverlapCheck<>(citation.anchor(), citation.oldName(),
                        RangeForOverlapCheck.REFERENCE_MARK_KIND, citation.oldName())).toList(), Optional.of(cursor),
                rebuildBibliography ? Set.of(targetZotero ? ZoteroCitationStrategy.BIBLIOGRAPHY_PREFIX : BibliographySection.NAME) : Set.of());
        for (PlannedCitation citation : citations) {
            if (MarkCodec.isCanonicalMarkName(citation.oldName())) {
                String raw = citation.anchor().getString();
                if (!raw.startsWith("\u200b") || !raw.endsWith("\u200b") || raw.length() < 2) {
                    throw invalidMetadata();
                }
            }
        }

        CslRenderer renderer = new CslRenderer(style, entryTypes);
        CitationGroup inserted = group(new JabRefMarkData(keys, CitationIds.newId(), renderer.capabilities().coerceForInsert(requestedType)),
                keys.stream().map(_ -> "").toList());
        if (!pageInfo.isEmpty()) {
            inserted.citationsInStorageOrder.getLast().setPageInfo(Optional.of(OOText.fromString(pageInfo)));
        }
        XTextCursor position = cursor.getText().createTextCursorByRange(cursor.getEnd());
        position.collapseToEnd();
        citations.add(new PlannedCitation(inserted, "", position, "", Optional.empty()));
        citations = order(citations);
        RenderOptions options = new RenderOptions(preferences.getBstCitationFormat(), rebuildBibliography,
                targetZotero ? "" : preferences.getCslBibliographyTitle(), preferences.getCslBibliographyHeaderFormat(),
                preferences.getCslBibliographyBodyFormat(), false);
        RenderRequest request = new RenderRequest(citations.stream().map(PlannedCitation::group).toList(), selected, options);
        RenderData resolved = new RenderData(request);
        if (!resolved.unresolvedKeys().isEmpty()) {
            throw new ConversionException("Conversion has unresolved keys: " + resolved.unresolvedKeys(),
                    Localization.lang("Cannot convert citations because these citation keys could not be resolved: %0. Select the matching libraries and try again.", String.join(", ", resolved.unresolvedKeys())));
        }
        BibDatabase flattened = new BibDatabase(resolved.flattenedEntries());
        Map<String, String> uris = source.uris();
        Set<String> usedItemIds = usedItemIds(citations);
        List<PlannedCitation> complete = new ArrayList<>();
        int itemId = 1;
        Set<String> plannedNames = new HashSet<>();
        for (PlannedCitation citation : citations) {
            List<String> groupKeys = citation.group().citationsInStorageOrder.stream().map(cited -> cited.citationKey).toList();
            String name;
            Optional<ConversionMetadata> metadata = citation.metadata();
            if (targetZotero) {
                if (ZoteroReferenceMark.isZoteroReferenceMarkName(citation.newName())) {
                    name = citation.newName();
                } else if (metadata.isPresent()) {
                    ConversionMetadata previous = metadata.orElseThrow();
                    name = previous.zoteroName();
                    if (previous.group().citationType != citation.group().citationType) {
                        name = ZoteroReferenceMark.updateCitationType(name, citation.group().citationType);
                    }
                } else {
                    List<BibEntry> groupEntries = new ArrayList<>();
                    for (String key : groupKeys) {
                        groupEntries.add(flattened.getEntryByCitationKey(key).orElseThrow());
                    }
                    itemId = reserveItemIds(usedItemIds, itemId, groupKeys.size());
                    name = ZoteroReferenceMark.buildReferenceMark(groupEntries, groupKeys, itemId,
                            citation.group().citationType, new BibDatabaseContext(flattened), entryTypes, uris).name();
                }
                if (!citation.oldName().isEmpty() && (MarkCodec.isCanonicalMarkName(citation.oldName()) || metadata.isPresent())) {
                    metadata = Optional.of(ConversionMetadata.capture(citation.group(), name));
                }
            } else {
                name = MarkCodec.encode(new JabRefMarkData(groupKeys, citation.group().groupId.citationGroupIdAsString(), citation.group().citationType));
                if (ZoteroReferenceMark.isZoteroReferenceMarkName(citation.oldName())) {
                    metadata = Optional.of(ConversionMetadata.capture(citation.group(), citation.newName()));
                }
            }
            if (ZoteroReferenceMark.isZoteroReferenceMarkName(name)) {
                ConversionMetadata.requireSupportedModifiers(name);
            }
            if (!plannedNames.add(name)) {
                throw invalidMetadata();
            }
            complete.add(new PlannedCitation(citation.group(), citation.oldName(), citation.anchor(), name, metadata));
        }
        Map<String, Optional<String>> propertyChanges = propertyChanges(complete, ZoteroDocumentPreferences.prepareCitationStyle(document, style),
                targetZotero, style);
        try {
            UndoableDocumentProperties.preflight(document, propertyChanges);
        } catch (com.sun.star.beans.IllegalTypeException | com.sun.star.beans.PropertyVetoException | com.sun.star.beans.NotRemoveableException exception) {
            ConversionException failure = new ConversionException("Cannot safely write conversion properties",
                    Localization.lang("Cannot convert citations because a required document property is not a writable string or cannot be removed. Check the document's custom properties and try again."));
            failure.initCause(exception);
            throw failure;
        }
        RenderResult rendered = renderer.render(request);
        for (PlannedCitation citation : complete) {
            if (!rendered.markers().containsKey(citation.group().groupId)) {
                throw new RenderException("Renderer omitted a prepared conversion group");
            }
        }
        return new PreparedInsertion(List.copyOf(complete), rendered, targetZotero, rebuildBibliography,
                Collections.unmodifiableMap(new LinkedHashMap<>(propertyChanges)));
    }

    private ReadPlan read(List<BibDatabase> selected)
            throws NoDocumentException, WrappedTargetException, RenderException, MarkCodecException {
        List<PlannedCitation> result = new ArrayList<>();
        MarkStore store = new MarkStore();
        Set<String> ids = new HashSet<>();
        Set<String> nativeIds = new HashSet<>();
        for (String name : ranges.names(document)) {
            if (MarkCodec.isCanonicalMarkName(name)) {
                CitationGroup group = store.readCitationGroupFromDocumentOrThrow(document, name);
                if (!ids.add(group.groupId.citationGroupIdAsString())) {
                    throw invalidMetadata();
                }
                WriterReferenceMark mark = store.referenceMark(group);

                Optional<ConversionMetadata> metadata = ConversionMetadata.forCanonical(document, group);
                if (metadata.isPresent()) {
                    ConversionMetadata.requireSupportedModifiers(metadata.orElseThrow().zoteroName());
                }
                result.add(new PlannedCitation(group, name, mark.getMarkRange(document).orElseThrow(), name, metadata));
            } else if (ZoteroReferenceMark.isZoteroReferenceMarkName(name)) {
                ConversionMetadata.requireSupportedModifiers(name);
                ZoteroReferenceMark mark = ZoteroReferenceMark.parse(name).orElseThrow(CitationFormatConverter::invalidMetadata);
                if (!nativeIds.add(ConversionMetadata.identity(name))) {
                    throw invalidMetadata();
                }
                if (JsonParser.parseString(ZoteroReferenceMark.getCSLJson(name)).getAsJsonObject().getAsJsonArray("citationItems").size() != mark.citationKeys().size()) {
                    throw invalidMetadata();
                }
            }
        }
        List<ZoteroCitationStrategy.StoredCitation> original = zotero.read();
        LinkPlan links = zotero.planLinks(original, selected);
        List<ZoteroCitationStrategy.StoredCitation> linked = ZoteroCitationStrategy.prepareLinks(original, links);
        for (ZoteroCitationStrategy.StoredCitation citation : linked) {
            Optional<ConversionMetadata> metadata = ConversionMetadata.forZotero(document, citation.originalName());
            CitationGroup group;
            if (metadata.isPresent()) {
                group = metadata.orElseThrow().group();
                if (!group.citationsInStorageOrder.stream().map(cited -> cited.citationKey).toList().equals(citation.mark().citationKeys())) {
                    throw invalidMetadata();
                }
            } else {
                group = group(new JabRefMarkData(citation.mark().citationKeys(), CitationIds.newId(), citation.group().citationType),
                        citation.mark().citationKeys().stream().map(_ -> "").toList());
            }
            if (!ids.add(group.groupId.citationGroupIdAsString())) {
                throw invalidMetadata();
            }
            result.add(new PlannedCitation(group, citation.originalName(), citation.anchor(), citation.mark().name(), metadata));
        }
        return new ReadPlan(result, links.zoteroUriByCitationKey());
    }

    private static Map<String, Optional<String>> propertyChanges(List<PlannedCitation> citations, Map<String, Optional<String>> styles,
                                                                 boolean targetZotero, CitationStyle style)
            throws MarkCodecException {
        Map<String, Optional<String>> changes = new LinkedHashMap<>();
        for (PlannedCitation citation : citations) {
            if (citation.metadata().isPresent()) {
                changes.put(ConversionMetadata.PREFIX + citation.group().groupId.citationGroupIdAsString(), Optional.of(citation.metadata().orElseThrow().encode()));
            }
            if (targetZotero && MarkCodec.isCanonicalMarkName(citation.oldName())) {
                JabRefMarkData old = MarkCodec.decode(citation.oldName());
                for (int index = 0; index < old.citationKeys().size(); index++) {
                    changes.put(MarkStore.pageInfoProperty(old.uniqueId(), index), Optional.empty());
                }
            } else if (!targetZotero) {
                for (int index = 0; index < citation.group().citationsInStorageOrder.size(); index++) {
                    changes.put(MarkStore.pageInfoProperty(citation.group().groupId.citationGroupIdAsString(), index),
                            citation.group().citationsInStorageOrder.get(index).getPageInfo().map(text -> text.toString()));
                }
            }
        }
        changes.putAll(styles);
        changes.put(DocumentStateStore.PROPERTY_NAME, Optional.of(DocumentStateStore.Family.CSL.name() + "\n" + style.getStyleId()));
        return changes;
    }

    private static Set<String> usedItemIds(List<PlannedCitation> citations) {
        Set<String> used = new HashSet<>();
        for (PlannedCitation citation : citations) {
            String nativeName = ZoteroReferenceMark.isZoteroReferenceMarkName(citation.newName()) ? citation.newName()
                                                                                                  : citation.metadata().map(ConversionMetadata::zoteroName).orElse("");
            if (nativeName.isEmpty()) {
                continue;
            }
            for (JsonElement item : JsonParser.parseString(ZoteroReferenceMark.getCSLJson(nativeName)).getAsJsonObject().getAsJsonArray("citationItems")) {
                JsonObject json = item.getAsJsonObject();
                if (json.has("id")) {
                    used.add(json.get("id").getAsString());
                }
                if (json.has("itemData") && json.get("itemData").isJsonObject() && json.getAsJsonObject("itemData").has("id")) {
                    used.add(json.getAsJsonObject("itemData").get("id").getAsString());
                }
            }
        }
        return used;
    }

    static int reserveItemIds(Set<String> used, int start, int count) throws RenderException {
        if (start < 1 || count < 1 || count - 1 > Integer.MAX_VALUE - start) {
            throw new RenderException("Zotero citation item ids are exhausted");
        }
        Set<Integer> numericIds = new HashSet<>();
        for (String id : used) {
            ZoteroReferenceMark.boundedItemId(id).ifPresent(numericIds::add);
        }
        int candidate = start;
        for (int index = 0; index < count; index++) {
            int itemId = candidate + index;
            if (numericIds.contains(itemId)) {
                if (count > Integer.MAX_VALUE - itemId) {
                    throw new RenderException("Zotero citation item ids are exhausted");
                }
                candidate = itemId + 1;
                index = -1;
            }
        }
        for (int index = 0; index < count; index++) {
            used.add(Integer.toString(candidate + index));
        }
        return candidate;
    }

    @NullMarked
    private record ReadPlan(List<PlannedCitation> citations, Map<String, String> uris) {
    }

    private static CitationGroup group(JabRefMarkData data, List<String> pages) throws MarkCodecException {
        return new ConversionMetadata(MarkCodec.encode(data), pages, "").group();
    }

    private List<PlannedCitation> order(List<PlannedCitation> citations) throws NoDocumentException {
        Map<XText, List<RangeSortEntry<PlannedCitation>>> partitions = new LinkedHashMap<>();
        for (PlannedCitation citation : citations) {
            partitions.computeIfAbsent(citation.anchor().getText(), _ -> new ArrayList<>()).add(new RangeSortEntry<>(citation.anchor(), 0, citation));
        }
        List<RangeSortable<PlannedCitation>> mapped = new ArrayList<>();
        for (List<RangeSortEntry<PlannedCitation>> partition : partitions.values()) {
            RangeSort.sortWithinPartition(partition);
            for (int index = 0; index < partition.size(); index++) {
                RangeSortEntry<PlannedCitation> entry = partition.get(index);
                mapped.add(new RangeSortEntry<>(UnoTextRange.getFootnoteMarkRange(entry.getRange()).orElse(entry.getRange()), index, entry.getContent()));
            }
        }
        OOResult<FunctionalTextViewCursor, String> view = FunctionalTextViewCursor.get(document);
        if (!view.isError()) {
            try {
                return RangeSortVisual.visualSort(mapped, document, view.get()).stream().map(RangeSortable::getContent).toList();
            } finally {
                view.get().restore(document);
            }
        }
        Map<XText, List<RangeSortable<PlannedCitation>>> textOrder = new LinkedHashMap<>();
        for (RangeSortable<PlannedCitation> entry : mapped) {
            textOrder.computeIfAbsent(entry.getRange().getText(), _ -> new ArrayList<>()).add(entry);
        }
        for (List<RangeSortable<PlannedCitation>> partition : textOrder.values()) {
            RangeSort.sortWithinPartition(partition);
        }
        return textOrder.values().stream().flatMap(List::stream).map(RangeSortable::getContent).toList();
    }

    @NullMarked
    record PlannedCitation(CitationGroup group, String oldName, XTextRange anchor, String newName, Optional<ConversionMetadata> metadata) {
    }

    @NullMarked
    final class PreparedInsertion {
        private final List<PlannedCitation> citations;
        private final RenderResult rendered;
        private final boolean targetZotero;
        private final boolean rebuildBibliography;
        private final Map<String, Optional<String>> propertyChanges;

        private PreparedInsertion(List<PlannedCitation> citations, RenderResult rendered, boolean targetZotero,
                                  boolean rebuildBibliography, Map<String, Optional<String>> propertyChanges) {
            this.citations = citations;
            this.rendered = rendered;
            this.targetZotero = targetZotero;
            this.rebuildBibliography = rebuildBibliography;
            this.propertyChanges = propertyChanges;
        }

        List<String> unresolvedKeys() {
            return rendered.unresolvedKeys();
        }

        void apply(MarkWriter writer, XTextCursor insertion) throws NoDocumentException, CreationException, com.sun.star.uno.Exception, MarkCodecException {
            writer.clear();
            MarkStore store = new MarkStore();
            for (PlannedCitation citation : citations) {
                XTextCursor position;
                if (citation.oldName().isEmpty()) {
                    position = insertion;
                } else {
                    position = citation.anchor().getText().createTextCursorByRange(citation.anchor());
                }
                WriterReferenceMark range;
                if (citation.oldName().equals(citation.newName())) {
                    range = ranges.find(document, citation.oldName(), targetZotero ? BoundaryPolicy.TEMPORARY : BoundaryPolicy.PERSISTENT).orElseThrow();
                } else {
                    if (!citation.oldName().isEmpty()) {
                        OOTextWriter.removeEscapementFormatting(position);
                        XTextContent content = WriterReferenceMarks.getAsTextContent(document, citation.oldName()).orElseThrow();
                        citation.anchor().getText().removeTextContent(content);
                        position.collapseToStart();
                    }
                    if (targetZotero) {
                        range = ranges.create(document, context, citation.newName(), position,
                                citation.oldName().isEmpty() && preferences.getAddSpaceBefore() && !hasAdjacentSpace(position, false),
                                citation.oldName().isEmpty() && preferences.getAddSpaceAfter() && !hasAdjacentSpace(position, true), BoundaryPolicy.TEMPORARY);
                    } else {
                        CitationGroup stored = store.createCitationGroup(document, context,
                                citation.group().citationsInStorageOrder.stream().map(cited -> cited.citationKey).toList(),
                                citation.group().citationsInStorageOrder.stream().map(Citation::getPageInfo).toList(), citation.group().citationType,
                                position, citation.oldName().isEmpty() && preferences.getAddSpaceBefore(), citation.oldName().isEmpty() && preferences.getAddSpaceAfter(),
                                citation.group().groupId.citationGroupIdAsString());
                        range = store.referenceMark(stored);
                    }
                }
                ReferenceMarkTextWriter.write(document, context, range, Optional.ofNullable(rendered.markers().get(citation.group().groupId))
                                                                                .orElseThrow(() -> new CreationException("Missing prepared conversion text")));
            }
            if (rebuildBibliography) {
                if (targetZotero) {
                    zotero.rebuildBibliography(rendered.bibliography());
                } else {
                    new BibliographySection().rebuild(document, rendered.bibliography(), false);
                }
            }
            for (Map.Entry<String, Optional<String>> property : propertyChanges.entrySet()) {
                UndoableDocumentProperties.set(document, property.getKey(), property.getValue());
            }
        }
    }

    private static boolean hasAdjacentSpace(XTextCursor cursor, boolean after) {
        XTextCursor adjacent = cursor.getText().createTextCursorByRange(cursor.getEnd());
        boolean moved = after ? adjacent.goRight((short) 1, true) : adjacent.goLeft((short) 1, true);
        return moved && !adjacent.getString().isEmpty() && adjacent.getString().isBlank();
    }

    static ConversionException invalidMetadata() {
        return new ConversionException("Invalid or externally modified citation conversion state",
                Localization.lang("Cannot convert citations because stored conversion information is invalid or a converted citation was edited outside JabRef. Restore the original citation before converting."));
    }

    static ConversionException invalidMetadata(Throwable cause) {
        ConversionException exception = invalidMetadata();
        exception.initCause(cause);
        return exception;
    }

    @NullMarked
    static final class ConversionException extends RenderException {
        private final String localized;

        ConversionException(String message, String localized) {
            super(message);
            this.localized = localized;
        }

        @Override
        public Optional<String> localizedMessage() {
            return Optional.of(localized);
        }
    }
}
