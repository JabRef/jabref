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
import org.jabref.logic.openoffice.engine.storage.CitationMarkerWriter;
import org.jabref.logic.openoffice.engine.storage.DocumentStyleStore;
import org.jabref.logic.openoffice.engine.storage.MarkStore;
import org.jabref.logic.openoffice.engine.storage.UndoableDocumentProperties;
import org.jabref.logic.openoffice.frontend.RangeForOverlapCheck;
import org.jabref.logic.openoffice.rangesort.RangeSort;
import org.jabref.logic.openoffice.rangesort.RangeSortEntry;
import org.jabref.logic.openoffice.rangesort.RangeSortVisual;
import org.jabref.logic.openoffice.rangesort.RangeSortable;
import org.jabref.logic.openoffice.rangesort.RestorableTextViewCursor;
import org.jabref.logic.openoffice.style.BstStyle;
import org.jabref.logic.openoffice.style.JStyle;
import org.jabref.logic.openoffice.style.OOStyle;
import org.jabref.logic.openoffice.uno.OOTextWriter;
import org.jabref.logic.openoffice.uno.UnoTextRange;
import org.jabref.logic.openoffice.uno.UnoUserDefinedProperty;
import org.jabref.logic.openoffice.uno.WriterDocumentAccessException;
import org.jabref.logic.openoffice.uno.WriterOperationException;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.Citation;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationGroupId;
import org.jabref.model.openoffice.style.CitationType;
import org.jabref.model.openoffice.style.PageInfo;
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

/// Prepares insertion-triggered conversion of canonical, native, or legacy JStyle citations.
/// Rendering finishes before document mutation; only legacy JStyle migration permits unresolved keys.
/// Application consumes the prepared plan inside the engine's document write session.
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

    static boolean required(List<String> names, OOStyle style, boolean zoteroMode) {
        boolean targetZotero = style instanceof CitationStyle && zoteroMode;
        return names.stream()
                    .filter(name -> JabRefMarkCodec.isCanonicalMarkName(name)
                            || LegacyJStyleMark.isLegacyMarkName(name)
                            || ZoteroReferenceMark.isZoteroReferenceMarkName(name))
                    .findFirst()
                    .map(name -> {
                        if (targetZotero) {
                            return JabRefMarkCodec.isCanonicalMarkName(name);
                        }
                        return LegacyJStyleMark.isLegacyMarkName(name)
                                || (style instanceof CitationStyle && ZoteroReferenceMark.isZoteroReferenceMarkName(name));
                    })
                    .orElse(false);
    }

    PreparedInsertion prepare(List<BibEntry> entries, List<BibDatabase> databases, OOStyle style,
                              CitationType requestedType, String pageInfo, XTextCursor cursor, boolean synchronizeBibliography)
            throws WriterDocumentAccessException, com.sun.star.uno.Exception, RenderException, JabRefMarkCodecException, IOException,
            ZoteroCitationStrategy.StrategyException {
        boolean targetZotero = style instanceof CitationStyle && preferences.getZoteroCompatibilityMode();
        CitationRenderer renderer = new RendererFactory(entryTypes, preferences.getPandocPath()).create(style);

        boolean rebuildBibliography = synchronizeBibliography && renderer.capabilities().hasBibliography();
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
        ReadPlan source = read(selected, style instanceof CitationStyle, !targetZotero);
        List<PlannedCitation> citations = source.citations();
        boolean legacyMigration = !targetZotero && citations.stream().findFirst().map(citation -> LegacyJStyleMark.isLegacyMarkName(citation.oldName())).orElse(false);
        zotero.validateRanges(citations.stream().map(citation -> new RangeForOverlapCheck<>(citation.anchor(), citation.oldName(),
                        RangeForOverlapCheck.REFERENCE_MARK_KIND, citation.oldName())).toList(), Optional.of(cursor),
                rebuildBibliography ? Set.of(targetZotero ? ZoteroCitationStrategy.BIBLIOGRAPHY_PREFIX : BibliographySection.NAME) : Set.of());
        for (PlannedCitation citation : citations) {
            if (JabRefMarkCodec.isCanonicalMarkName(citation.oldName())) {
                String raw = citation.anchor().getString();
                if (!raw.startsWith("\u200b") || !raw.endsWith("\u200b") || raw.length() < 2) {
                    throw invalidReferenceMark();
                }
            }
        }

        CitationGroup inserted = group(new JabRefMarkData(keys, CitationIds.newId(), renderer.capabilities().coerceForInsert(requestedType)));
        if (!pageInfo.isEmpty()) {
            inserted.citationsInStorageOrder.getLast().setPageInfo(Optional.of(OOText.fromString(pageInfo)));
        }
        XTextCursor position = cursor.getText().createTextCursorByRange(cursor.getEnd());
        position.collapseToEnd();
        citations.add(new PlannedCitation(inserted, "", position, ""));
        citations = order(citations);
        RenderOptions options = new RenderOptions(preferences.getBstCitationFormat(), rebuildBibliography,
                targetZotero || style instanceof JStyle ? "" : preferences.getCslBibliographyTitle(), preferences.getCslBibliographyHeaderFormat(),
                preferences.getCslBibliographyBodyFormat(), !targetZotero && preferences.getAlwaysAddCitedOnPages());
        RenderRequest request = new RenderRequest(citations.stream().map(PlannedCitation::group).toList(), selected, options);
        CitationKeyResolution resolved = new CitationKeyResolution(request);
        if (!legacyMigration && !resolved.unresolvedKeys().isEmpty()) {
            throw new ConversionException("Conversion has unresolved keys: " + resolved.unresolvedKeys(),
                    Localization.lang("Cannot convert citations because these citation keys could not be resolved: %0. Select the matching libraries and try again.", String.join(", ", resolved.unresolvedKeys())));
        }
        Optional<BibDatabaseContext> nativeSources = targetZotero ? Optional.of(prepareNativeSources(citations, resolved)) : Optional.empty();
        Map<String, String> uris = source.uris();
        Set<String> usedItemIds = targetZotero ? usedItemIds(citations) : Set.of();
        List<PlannedCitation> complete = new ArrayList<>();
        int itemId = 1;
        Set<String> plannedNames = new HashSet<>();
        for (PlannedCitation citation : citations) {
            List<String> groupKeys = citation.group().citationsInStorageOrder.stream().map(cited -> cited.citationKey).toList();
            String name;
            if (targetZotero) {
                if (ZoteroReferenceMark.isZoteroReferenceMarkName(citation.newName())) {
                    name = citation.newName();
                } else {
                    BibDatabaseContext sources = nativeSources.orElseThrow();
                    List<BibEntry> groupEntries = new ArrayList<>();
                    for (String key : groupKeys) {
                        groupEntries.add(sources.getDatabase().getEntryByCitationKey(key).orElseThrow());
                    }
                    itemId = reserveItemIds(usedItemIds, itemId, groupKeys.size());
                    name = ZoteroReferenceMark.buildReferenceMark(groupEntries, groupKeys, itemId,
                            citation.group().citationType, sources, entryTypes, uris).name();
                }
            } else {
                name = JabRefMarkCodec.encode(new JabRefMarkData(groupKeys, citation.group().groupId.citationGroupIdAsString(), citation.group().citationType));
            }
            if (!plannedNames.add(name)) {
                throw invalidReferenceMark();
            }
            complete.add(new PlannedCitation(citation.group(), citation.oldName(), citation.anchor(), name));
        }
        Map<String, Optional<String>> styleProperties = targetZotero && style instanceof CitationStyle csl
                                                        ? ZoteroDocumentPreferences.prepareCitationStyle(document, csl) : Map.of();
        Map<String, Optional<String>> propertyChanges = propertyChanges(complete, styleProperties, targetZotero, style);
        try {
            UndoableDocumentProperties.preflight(document, propertyChanges);
        } catch (com.sun.star.beans.IllegalTypeException | com.sun.star.beans.PropertyVetoException | com.sun.star.beans.NotRemoveableException exception) {
            ConversionException failure = new ConversionException("Cannot safely write conversion properties",
                    Localization.lang("Cannot convert citations because a required document property is not a writable string or cannot be removed. Check the document's custom properties and try again."));
            failure.initCause(exception);
            throw failure;
        }
        RenderResult rendered = targetZotero && renderer instanceof CslRenderer csl ? csl.renderNative(request) : renderer.render(request);
        for (PlannedCitation citation : complete) {
            if (!rendered.markers().containsKey(citation.group().groupId)) {
                throw new RenderException("Renderer omitted a prepared conversion group");
            }
        }
        return new PreparedInsertion(List.copyOf(complete), rendered, targetZotero,
                Collections.unmodifiableMap(new LinkedHashMap<>(propertyChanges)));
    }

    private static BibDatabaseContext prepareNativeSources(List<PlannedCitation> citations, CitationKeyResolution resolved) throws RenderException {
        List<String> keys = citations.stream()
                                     .filter(citation -> !ZoteroReferenceMark.isZoteroReferenceMarkName(citation.newName()))
                                     .flatMap(citation -> citation.group().citationsInStorageOrder.stream().map(Citation::getCitationKey))
                                     .distinct().toList();
        return new BibDatabaseContext(new BibDatabase(resolved.flattenedEntries(keys)));
    }

    // [impl->req~openoffice.zotero-conversion-modifier-compatibility~1]
    private ReadPlan read(List<BibDatabase> selected, boolean includeZotero, boolean includeLegacy)
            throws WriterDocumentAccessException, WrappedTargetException, RenderException, JabRefMarkCodecException {
        List<PlannedCitation> result = new ArrayList<>();
        MarkStore store = new MarkStore();
        Set<String> ids = new HashSet<>();
        for (String name : ranges.names(document)) {
            if (JabRefMarkCodec.isCanonicalMarkName(name)) {
                CitationGroup group = store.readCitationGroupFromDocumentOrThrow(document, name);
                if (!ids.add(group.groupId.citationGroupIdAsString())) {
                    throw invalidReferenceMark();
                }
                WriterReferenceMark mark = store.referenceMark(group);
                result.add(new PlannedCitation(group, name, mark.getMarkRange(document).orElseThrow(), name));
            } else if (includeLegacy && LegacyJStyleMark.isLegacyMarkName(name)) {
                CitationGroup group = readLegacyGroup(name);
                if (!ids.add(group.groupId.citationGroupIdAsString())) {
                    throw invalidReferenceMark();
                }
                XTextRange anchor = WriterReferenceMarks.getAnchor(document, name)
                                                        .orElseThrow(() -> new WriterDocumentAccessException("Missing legacy JStyle citation range"));
                result.add(new PlannedCitation(group, name, anchor, ""));
            } else if (includeZotero && ZoteroReferenceMark.isZoteroReferenceMarkName(name)) {
                ZoteroReferenceMark mark = ZoteroReferenceMark.parse(name).orElseThrow(CitationFormatConverter::invalidReferenceMark);
                if (JsonParser.parseString(ZoteroReferenceMark.getCSLJson(name)).getAsJsonObject().getAsJsonArray("citationItems").size() != mark.citationKeys().size()) {
                    throw invalidReferenceMark();
                }
            }
        }
        if (!includeZotero) {
            return new ReadPlan(result, Map.of());
        }
        List<ZoteroCitationStrategy.StoredCitation> original = zotero.read();
        if (original.isEmpty()) {
            return new ReadPlan(result, Map.of());
        }
        LinkPlan links = zotero.planLinks(original, selected);
        List<ZoteroCitationStrategy.StoredCitation> linked = ZoteroCitationStrategy.prepareLinks(original, links);
        for (ZoteroCitationStrategy.StoredCitation citation : linked) {
            CitationGroup group = group(new JabRefMarkData(citation.mark().citationKeys(), CitationIds.newId(), citation.group().citationType));
            if (!ids.add(group.groupId.citationGroupIdAsString())) {
                throw invalidReferenceMark();
            }
            result.add(new PlannedCitation(group, citation.originalName(), citation.anchor(), citation.mark().name()));
        }
        return new ReadPlan(result, links.zoteroUriByCitationKey());
    }

    // [impl->req~openoffice.legacy-jstyle-migration~1]
    private CitationGroup readLegacyGroup(String name) throws JabRefMarkCodecException, WrappedTargetException {
        LegacyJStyleMark mark = LegacyJStyleMark.decode(name);
        CitationGroup group = group(new JabRefMarkData(mark.citationKeys(), CitationIds.newId(), mark.citationType()));
        group.citationsInStorageOrder.getLast().setPageInfo(PageInfo.normalizePageInfo(
                UnoUserDefinedProperty.getStringValue(document, name).map(OOText::fromString)));
        return group;
    }

    private static Map<String, Optional<String>> propertyChanges(List<PlannedCitation> citations, Map<String, Optional<String>> styles,
                                                                 boolean targetZotero, OOStyle style)
            throws JabRefMarkCodecException, RenderException {
        Map<String, Optional<String>> changes = new LinkedHashMap<>();
        for (PlannedCitation citation : citations) {
            if (LegacyJStyleMark.isLegacyMarkName(citation.oldName())) {
                changes.put(citation.oldName(), Optional.empty());
            }
            if (targetZotero && JabRefMarkCodec.isCanonicalMarkName(citation.oldName())) {
                JabRefMarkData old = JabRefMarkCodec.decode(citation.oldName());
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
        String styleState = switch (style) {
            case CitationStyle csl ->
                    DocumentStyleStore.Family.CSL.name() + "\n" + csl.getStyleId();
            case JStyle jStyle ->
                    DocumentStyleStore.Family.JSTYLE.name() + "\n" + jStyle.getPath();
            case BstStyle bst ->
                    DocumentStyleStore.Family.BST.name() + "\n" + bst.getPath();
            default ->
                    throw new ConversionException("Unsupported citation style", Localization.lang("Cannot generate citation based on selected citation style."));
        };
        changes.put(DocumentStyleStore.PROPERTY_NAME, Optional.of(styleState));
        return changes;
    }

    private static Set<String> usedItemIds(List<PlannedCitation> citations) {
        Set<String> used = new HashSet<>();
        for (PlannedCitation citation : citations) {
            String nativeName = citation.newName();
            if (!ZoteroReferenceMark.isZoteroReferenceMarkName(nativeName)) {
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

    private static CitationGroup group(JabRefMarkData data) throws JabRefMarkCodecException {
        List<Citation> citations = data.citationKeys().stream().map(Citation::new).toList();
        return new CitationGroup(new CitationGroupId(data.uniqueId()), data.citationType(), citations, Optional.of(JabRefMarkCodec.encode(data)));
    }

    private List<PlannedCitation> order(List<PlannedCitation> citations) throws WriterDocumentAccessException {
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
        OOResult<RestorableTextViewCursor, String> view = RestorableTextViewCursor.get(document);
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
    record PlannedCitation(CitationGroup group, String oldName, XTextRange anchor, String newName) {
    }

    @NullMarked
    final class PreparedInsertion {
        private final List<PlannedCitation> citations;
        private final RenderResult rendered;
        private final boolean targetZotero;
        private final Map<String, Optional<String>> propertyChanges;

        private PreparedInsertion(List<PlannedCitation> citations, RenderResult rendered, boolean targetZotero,
                                  Map<String, Optional<String>> propertyChanges) {
            this.citations = citations;
            this.rendered = rendered;
            this.targetZotero = targetZotero;
            this.propertyChanges = propertyChanges;
        }

        RenderResult rendered() {
            return rendered;
        }

        void applyCitations(CitationMarkerWriter writer, XTextCursor insertion) throws WriterDocumentAccessException, WriterOperationException, com.sun.star.uno.Exception, JabRefMarkCodecException {
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
                                                                                .orElseThrow(() -> new WriterOperationException("Missing prepared conversion text")));
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

    private static ConversionException invalidReferenceMark() {
        return new ConversionException("Invalid citation reference mark",
                Localization.lang("Cannot convert citations because a citation reference mark is invalid. Repair the citation and try again."));
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
